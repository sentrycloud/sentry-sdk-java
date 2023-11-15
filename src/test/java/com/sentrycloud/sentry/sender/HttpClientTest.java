package com.sentrycloud.sentry.sender;

import com.sentrycloud.sentry.collector.DataPoint;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class HttpClientTest {

    private List<DataPoint> prepareDataPoints() {
        Map<String, String> tags = new HashMap<>();
        tags.put("appName", "javaTest");
        DataPoint dataPoint = new DataPoint("java_test_metric", tags, System.currentTimeMillis() / 1000, 10);
        DataPoint dataPoint2 = new DataPoint("java_test_metric2", tags, System.currentTimeMillis() / 1000, 20);

        List<DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(dataPoint);
        dataPoints.add(dataPoint2);
        return dataPoints;
    }

    @Test
    public void sendDataPointsToAgent() {
        List<DataPoint> dataPoints = prepareDataPoints();

        try {
            int responseCode = HttpClient.send(dataPoints);
            Assert.assertEquals(responseCode, 200);
        } catch (Exception ex) {
            Assert.fail("HttpClient.send exception: " + ex.getMessage());
        }
    }

    @Test
    public void sendDataPointsToServer() {
        HttpClient.setReportURL("http://127.0.0.1:51001/server/api/putMetrics");
        List<DataPoint> dataPoints = prepareDataPoints();

        try {
            int responseCode = HttpClient.send(dataPoints);
            Assert.assertEquals(responseCode, 200);
        } catch (Exception ex) {
            Assert.fail("HttpClient.send exception: " + ex.getMessage());
        }
    }
}
