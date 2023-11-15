package com.sentrycloud.sentry.collector;

public interface Collector {
    void put(double value);
    void put(double value, long timestamp);
}
