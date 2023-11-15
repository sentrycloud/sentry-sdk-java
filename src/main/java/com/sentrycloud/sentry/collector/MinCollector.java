package com.sentrycloud.sentry.collector;

import java.util.Map;

public class MinCollector extends BaseCollector {

    public MinCollector(String metric, Map<String, String> tags, CollectorType aggregator, int timeInterval) {
        super(metric, tags, aggregator, timeInterval);

        for (int i = 0; i < MAX_ACCUMULATOR_COUNT; i++) {
            doubleAdders[i].set(Double.MAX_VALUE);
        }
    }

    public void put(double value, long timestamp) {
        putForMaxMin(value, timestamp, false);
    }
}
