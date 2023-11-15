package com.sentrycloud.sentry.collector;

import com.sentrycloud.sentry.sender.HttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

// a singleton of collector manager
public class CollectorManager {
    private static final int DATA_POINTS_BLOCKING_QUEUE_SIZE = 8192;
    private static final int DATA_POINTS_BATCH_SIZE = 30;

    private final Map<String, BaseCollector> collectors;
    private final LinkedBlockingQueue<List<DataPoint>> dataPointsBlockingQueue;

    private CollectorManager() {
        collectors = new ConcurrentHashMap<>();
        dataPointsBlockingQueue = new LinkedBlockingQueue<>(DATA_POINTS_BLOCKING_QUEUE_SIZE);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new AggregateThread(), 1, 1, TimeUnit.SECONDS);

        ExecutorService senderExecuteService = Executors.newSingleThreadExecutor();
        senderExecuteService.submit(new SenderThread());
    }

    private static class CollectorManagerHolder {
        public static  CollectorManager instance = new CollectorManager();
    }

    public static CollectorManager getInstance() {
        return CollectorManagerHolder.instance;
    }

    public Collector getCollector(String metric, Map<String, String> tags, CollectorType collectorType, int timeInterval) {
        String key = serializeKey(metric, tags);
        BaseCollector collector = collectors.get(key);
        if (collector != null) {
            if (collector.getTimeInterval() != timeInterval) {
                throw new RuntimeException("collector exist with another time interval");
            }

            if (collector.getCollectorType() != collectorType) {
                throw new RuntimeException("collector exist with another collector type");
            }

            return collector;
        }

        synchronized (collectors) {
            switch (collectorType) {
                case SUM:
                    collector = new SumCollector(metric, tags, collectorType, timeInterval);
                    break;
                case AVG:
                    collector = new AvgCollector(metric, tags, collectorType, timeInterval);
                    break;
                case MAX:
                    collector = new MaxCollector(metric, tags, collectorType, timeInterval);
                    break;
                case MIN:
                    collector = new MinCollector(metric, tags, collectorType, timeInterval);
                    break;
                default:
                    throw new RuntimeException("no such collector type");
            }

            collectors.put(key, collector);
        }

        return collector;
    }

    private String serializeKey(String metric, Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return metric;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(metric);
        sb.append("@");
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            sb.append(entry.getKey());
            sb.append(":");
            sb.append(entry.getValue());
            sb.append(",");
        }

        return sb.toString();
    }

    class AggregateThread implements Runnable {
        public void run() {
            long now = System.currentTimeMillis() / 1000;
            List<DataPoint> batchDps = new ArrayList<>();
            for (Map.Entry<String, BaseCollector> entry : collectors.entrySet()) {
                List<DataPoint> dps = entry.getValue().aggregate(now);
                if (dps != null && !dps.isEmpty()) {
                    batchDps.addAll(dps);
                }

                if (batchDps.size() >= DATA_POINTS_BATCH_SIZE) {
                    dataPointsBlockingQueue.offer(batchDps);
                    batchDps = new ArrayList<>();
                }
            }

            if (!batchDps.isEmpty()) {
                dataPointsBlockingQueue.offer(batchDps);
            }
        }
    }

    class SenderThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<DataPoint> dps = dataPointsBlockingQueue.poll(1, TimeUnit.SECONDS);
                    if (dps != null && !dps.isEmpty()) {
                        HttpClient.send(dps);
                    }
                } catch (InterruptedException ex) {
                    System.out.println("thread is interrupted " + ex.getMessage());
                    break;
                } catch (Exception ex) {
                    System.out.println("normal exception");
                }
            }
        }
    }
}
