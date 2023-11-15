package com.sentrycloud.sentry.collector;

import java.util.Map;

public class DataPoint {
    private String metric;
    private Map<String, String> tags;
    long timestamp;
    double value;

    public DataPoint(String metric, Map<String, String> tags, long timestamp, double value) {
        this.metric = metric;
        this.tags = tags;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getMetric() {
        return metric;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }
}
