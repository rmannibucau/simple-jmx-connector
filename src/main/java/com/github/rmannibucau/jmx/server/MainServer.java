package com.github.rmannibucau.jmx.server;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public final class MainServer {
    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("Usage: <run> service:jmx:simple://<host>:<port>");
        }

        final JMXServiceURL serviceURL = new JMXServiceURL(args[0]);
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, Collections.<String, Object>emptyMap(), mBeanServer).start();
        new CountDownLatch(1).await();
    }
}
