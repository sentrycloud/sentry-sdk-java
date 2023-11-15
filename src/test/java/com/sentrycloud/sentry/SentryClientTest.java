package com.sentrycloud.sentry;

import com.sentrycloud.sentry.collector.Collector;
import com.sentrycloud.sentry.collector.CollectorType;
import org.junit.Test;

public class SentryClientTest {

    @Test
    public void TestSentryClient() throws Exception {
        int timeInterval = 10;
        Collector sumCollector = SentryClient.getCollector("sentry_java_client_metric_sum", null, CollectorType.SUM, timeInterval);
        Collector avgCollector = SentryClient.getCollector("sentry_java_client_metric_avg", null, CollectorType.AVG, timeInterval);
        Collector maxCollector = SentryClient.getCollector("sentry_java_client_metric_max", null, CollectorType.MAX, timeInterval);
        Collector minCollector = SentryClient.getCollector("sentry_java_client_metric_min", null, CollectorType.MIN, timeInterval);

        int cycleNum = 80; // the number must greater than MAX_ACCUMULATOR_COUNT to test reset is take effect
        for (int i = 0; i < cycleNum; i++) {
            // expect aggregate value 6
            sumCollector.put(1);
            sumCollector.put(2);
            sumCollector.put(3);

            // expect aggregate value 2
            avgCollector.put(1);
            avgCollector.put(2);
            avgCollector.put(3);

            // expect aggregate value 3
            maxCollector.put(1);
            maxCollector.put(2);
            maxCollector.put(3);

            // expect aggregate value 1
            minCollector.put(1);
            minCollector.put(2);
            minCollector.put(3);

            System.out.printf("test cycle %d\n", i);
            Thread.sleep(timeInterval * 1000);
        }

        Thread.sleep(timeInterval * 1000);
    }
}
