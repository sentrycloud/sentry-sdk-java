package com.sentrycloud.sentry.jvm;

import com.sentrycloud.sentry.collector.DataPoint;
import com.sentrycloud.sentry.sender.HttpClient;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JvmCollectors {
    private ClassLoadingMXBean classLoaderMXBean;
    private ThreadMXBean threadMXBean;
    private MemoryMXBean memoryMXBean;
    private List<MemoryPoolMXBean> memoryPoolMXBeans;
    private List<GarbageCollectorMXBean> gcMXBeans;

    private String appName;
    private ScheduledExecutorService scheduledExecutorService;

    private JvmCollectors() {
        classLoaderMXBean = ManagementFactory.getClassLoadingMXBean();
        threadMXBean = ManagementFactory.getThreadMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    private static class JvmCollectorsHolder {
        public static final JvmCollectors instance = new JvmCollectors();
    }

    public static JvmCollectors getInstance() {
        return JvmCollectorsHolder.instance;
    }

    public void start(String appName, int timeInterval) {
        this.appName = appName;
        if (this.appName == null || this.appName.isEmpty()) {
            this.appName = "defaultAppName";
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new CollectJvmThread(), 0, timeInterval, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }

    protected void test() {
        System.out.printf("totalClassCount=%d, loadedClassCount=%d, unloadedClassCount=%d\n",
                classLoaderMXBean.getTotalLoadedClassCount(), classLoaderMXBean.getLoadedClassCount(), classLoaderMXBean.getUnloadedClassCount());
        System.out.printf("threadCount=%d\n", threadMXBean.getThreadCount());
        System.out.printf("heapMemoryUsage.used=%d, nonHeapMemoryUsage.used=%d\n",
                memoryMXBean.getHeapMemoryUsage().getUsed(), memoryMXBean.getNonHeapMemoryUsage().getUsed());

        System.out.println("memory pool usage:");
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            System.out.printf("\tname=%s, used=%d\n", poolMXBean.getName(), poolMXBean.getUsage().getUsed());
        }

        System.out.println("garbage collector info:");
        for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
            System.out.printf("\tname=%s, collectTime=%d, collectCount=%d\n",
                    gcMXBean.getName(), gcMXBean.getCollectionTime(), gcMXBean.getCollectionCount());
        }
    }

    class CollectJvmThread implements Runnable {
        private Map<String, Long> prevGcCollectionTimeMap;
        private Map<String, Long> prevGcCollectionCountMap;

        public CollectJvmThread() {
            prevGcCollectionTimeMap = new HashMap<>();
            prevGcCollectionCountMap = new HashMap<>();
        }

        public void run() {
            // no need to aggregate data for JVM information, send data directly to agent or server
            List<DataPoint> dataPoints = new ArrayList<>(16);
            long timestamp = System.currentTimeMillis() / 1000;

            Map<String, String> tags = newTagsWithAppName();
            DataPoint dp = new DataPoint("sentry_jvm_load_class_count", tags, timestamp, classLoaderMXBean.getLoadedClassCount());
            dataPoints.add(dp);

            tags = newTagsWithAppName();
            tags.put("daemon", "false");
            dp = new DataPoint("sentry_jvm_thread_count", tags, timestamp, threadMXBean.getThreadCount());
            dataPoints.add(dp);

            tags = newTagsWithAppName();
            tags.put("daemon", "true");
            dp = new DataPoint("sentry_jvm_thread_count", tags, timestamp, threadMXBean.getDaemonThreadCount());
            dataPoints.add(dp);

            tags = newTagsWithAppName();
            tags.put("name", "Heap");
            dp = new DataPoint("sentry_jvm_used_memory", tags, timestamp, memoryMXBean.getHeapMemoryUsage().getUsed());
            dataPoints.add(dp);

            tags = newTagsWithAppName();
            tags.put("name", "Non Heap");
            dp = new DataPoint("sentry_jvm_used_memory", tags, timestamp, memoryMXBean.getNonHeapMemoryUsage().getUsed());
            dataPoints.add(dp);

            for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
                tags = newTagsWithAppName();
                tags.put("name", poolMXBean.getName());
                dp = new DataPoint("sentry_jvm_used_memory", tags, timestamp, poolMXBean.getUsage().getUsed());
                dataPoints.add(dp);
            }

            for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
                String gcName = gcMXBean.getName();
                Long prevGcTime = prevGcCollectionTimeMap.get(gcName);
                if (prevGcTime != null) {
                    tags = newTagsWithAppName();
                    tags.put("name", gcName);
                    dp = new DataPoint("sentry_jvm_gc_time", tags, timestamp, gcMXBean.getCollectionTime() - prevGcTime);
                    dataPoints.add(dp);
                }

                Long prevGcCount = prevGcCollectionCountMap.get(gcName);
                if (prevGcCount != null) {
                    tags = newTagsWithAppName();
                    tags.put("name", gcName);
                    dp = new DataPoint("sentry_jvm_gc_count", tags, timestamp, gcMXBean.getCollectionCount() - prevGcCount);
                    dataPoints.add(dp);
                }

                prevGcCollectionTimeMap.put(gcName, gcMXBean.getCollectionTime());
                prevGcCollectionCountMap.put(gcName, gcMXBean.getCollectionCount());
            }

            try {
                HttpClient.send(dataPoints);
            } catch (Exception ex) {
                System.out.println("send jvm metrics failed: " + ex.getMessage());
            }
        }

        private Map<String, String> newTagsWithAppName() {
            Map<String, String> tags = new HashMap<>();
            tags.put("appName", appName);
            return tags;
        }
    }
}
