package com.sentrycloud.sentry.collector;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseCollector implements Collector {
    public final int MAX_ACCUMULATOR_COUNT = 64;
    public static final int MAX_TRY_COUNT = 3;

    protected String metric;
    protected Map<String, String> tags;
    protected CollectorType collectorType;
    protected int timeInterval;
    protected long lastAggregateTime;

    protected AtomicDouble[] doubleAdders;
    protected AtomicInteger[] doubleAdderCounters;

    public BaseCollector(String metric, Map<String, String> tags, CollectorType collectorType, int timeInterval) {
        this.metric = metric;
        this.tags = new HashMap<>();
        if (tags != null) {
            for (Map.Entry<String, String> kv : tags.entrySet()) {
                String key = kv.getKey();
                String value = kv.getValue();
                if (!key.isEmpty() && !value.isEmpty()) {
                    // filter empty key or value in tags
                    this.tags.put(key, value);
                }
            }
        }

        this.collectorType = collectorType;
        this.timeInterval = timeInterval;

        long currentTimestamp = System.currentTimeMillis() / 1000;
        this.lastAggregateTime =  currentTimestamp - currentTimestamp % timeInterval;

        this.doubleAdders = new AtomicDouble[MAX_ACCUMULATOR_COUNT];
        for (int i = 0; i < MAX_ACCUMULATOR_COUNT; i++) {
            this.doubleAdders[i] = new AtomicDouble();
        }
        this.doubleAdderCounters = new AtomicInteger[MAX_ACCUMULATOR_COUNT];
        for (int i = 0; i < MAX_ACCUMULATOR_COUNT; i++) {
            this.doubleAdderCounters[i] = new AtomicInteger();
        }
    }

    public CollectorType getCollectorType() {
        return collectorType;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    protected int calculateIndex(long timestamp) {
        return (int) (timestamp / this.timeInterval) % MAX_ACCUMULATOR_COUNT; // calculate which slot to put the value
    }

    protected boolean isOldTimestamp(long timestamp) {
        return  timestamp - timestamp % timeInterval < lastAggregateTime;
    }

    public void put(double value) {
        put(value, System.currentTimeMillis() / 1000);
    }

    protected void putForSum(double value, long timestamp) {
        if (isOldTimestamp(timestamp)) {
            return;
        }

        int index = calculateIndex(timestamp);
        doubleAdders[index].addAndGet(value);
        doubleAdderCounters[index].incrementAndGet();
    }

    protected void putForMaxMin(double value, long timestamp, boolean isForMax) {
        if (isOldTimestamp(timestamp)) {
            return;
        }

        int index = calculateIndex(timestamp);
        double currentValue = doubleAdders[index].doubleValue();

        if (isCurrentValueFit(isForMax, currentValue, value)) {
            return;
        }

        for (int i = 0; i < MAX_TRY_COUNT; i++) {
            if (doubleAdders[index].compareAndSet(currentValue, value)) {
                doubleAdderCounters[index].incrementAndGet();
                break;
            } else {
                currentValue = doubleAdders[index].doubleValue();
                if (isCurrentValueFit(isForMax, currentValue, value)) {
                    break;
                }
            }

        }
    }

    protected boolean isCurrentValueFit(boolean isForMax, double currentValue, double value) {
        if (isForMax) {
            return currentValue >= value;
        } else {
            return currentValue < value;
        }
    }

    public List<DataPoint> aggregate(long now) {
        if (lastAggregateTime + timeInterval >= now) {
            return null;
        }

        List<DataPoint> dps = new ArrayList<>();
        while (lastAggregateTime + (long)timeInterval < now) {
            int index = calculateIndex(lastAggregateTime);
            if (doubleAdderCounters[index].get() > 0) {
                DataPoint dp;
                switch (collectorType) {
                    case SUM:
                        dp = new DataPoint(metric, tags, lastAggregateTime, doubleAdders[index].doubleValue());
                        dps.add(dp);
                        doubleAdders[index].set(0);
                        break;
                    case AVG:
                        dp = new DataPoint(metric, tags, lastAggregateTime, doubleAdders[index].doubleValue() / doubleAdderCounters[index].get());
                        dps.add(dp);
                        doubleAdders[index].set(0);
                        break;
                    case MAX:
                        dp = new DataPoint(metric, tags, lastAggregateTime, doubleAdders[index].doubleValue());
                        dps.add(dp);
                        doubleAdders[index].set(Double.MIN_VALUE);
                        break;
                    case MIN:
                        dp = new DataPoint(metric, tags, lastAggregateTime, doubleAdders[index].doubleValue());
                        dps.add(dp);
                        doubleAdders[index].set(Double.MAX_VALUE);
                        break;
                }
            }

            lastAggregateTime += timeInterval;
            doubleAdderCounters[index].set(0);
        }
        return dps;
    }
}
