package com.sentrycloud.sentry.collector;

import java.util.Map;

public class AvgCollector extends BaseCollector {
    public AvgCollector(String metric, Map<String, String> tags, CollectorType aggregator, int timeInterval) {
        super(metric, tags, aggregator, timeInterval);
    }

    public void put(double value, long timestamp) {
        putForSum(value, timestamp);
    }
}
