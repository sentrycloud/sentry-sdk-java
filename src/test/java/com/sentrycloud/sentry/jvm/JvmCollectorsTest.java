package com.sentrycloud.sentry.jvm;

import org.junit.Test;

public class JvmCollectorsTest {

    @Test
    public void TestMXBeans() {
        JvmCollectors.getInstance().test();
    }

    @Test
    public void TestJvmCollectors() throws Exception {
        JvmCollectors jvmCollectors = JvmCollectors.getInstance();

        System.out.println("start jvm collect");
        jvmCollectors.start("jvmTestApp", 10);
        Thread.sleep(60 * 1000);

        System.out.println("stop jvm collect");
        jvmCollectors.stop();
        Thread.sleep(30 * 1000);

        System.out.println("start jvm collect again");
        jvmCollectors.start("jvmTestApp", 10);
        Thread.sleep(60 * 1000);

        System.out.println("complete jvm collect");
    }
}
