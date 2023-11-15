package com.sentrycloud.sentry;

import com.sentrycloud.sentry.collector.Collector;
import com.sentrycloud.sentry.collector.CollectorManager;
import com.sentrycloud.sentry.collector.CollectorType;
import com.sentrycloud.sentry.jvm.JvmCollectors;
import com.sentrycloud.sentry.sender.HttpClient;

import java.util.Map;

public class SentryClient {
    public static Collector getCollector(String metric, Map<String, String> tags, CollectorType collectorType, int timeInterval) {
        return CollectorManager.getInstance().getCollector(metric, tags, collectorType, timeInterval);
    }

    // metric are send to local sentry_agent by default, call this API to send to sentry_server or even other sentry_agent
    public static void setReportURL(String newReportURL) {
        HttpClient.setReportURL(newReportURL);
    }

    public static void startJvmCollectors(String appName, int collectTimeInterval) {
        JvmCollectors.getInstance().start(appName, collectTimeInterval);
    }

    public static void stopJvmCollectors() {
        JvmCollectors.getInstance().stop();
    }
}
