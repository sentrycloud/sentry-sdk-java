package com.sentrycloud.sentry.collector;

public enum CollectorType {
    SUM(0), AVG(1), MAX(2), MIN(3);

    private int type;

    CollectorType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }
}
