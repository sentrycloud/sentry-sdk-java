package com.sentrycloud.sentry.sender;

import com.google.gson.Gson;
import com.sentrycloud.sentry.collector.DataPoint;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HttpClient {
    private static final int CONN_TIMEOUT = 10000;
    private static String reportURL = "http://127.0.0.1:50001/agent/api/putMetrics";

    public static void setReportURL(String newReportUrl) {
        reportURL = newReportUrl;
    }

    public static int send(List<DataPoint> dataPoints) throws Exception {
        URL url = new URL(reportURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(CONN_TIMEOUT);
        conn.setReadTimeout(CONN_TIMEOUT);
        conn.setDoOutput(true);
        conn.connect();

        Gson gson = new Gson();
        String jsonStr = gson.toJson(dataPoints);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        writer.write(jsonStr);
        writer.close();

        return conn.getResponseCode();
    }
}
