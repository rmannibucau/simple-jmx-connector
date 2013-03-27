package com.github.rmannibucau.jmx.server;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleJMXConnectorServer extends JMXConnectorServer {
    private final JMXServiceURL url;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final SimpleServer server;

    public SimpleJMXConnectorServer(final JMXServiceURL serviceURL, final Map<String, ?> environment, final MBeanServer mbeanServer) {
        super(mbeanServer);
        this.url = serviceURL;
        this.server = new SimpleServer(url, environment, mbeanServer, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void start() throws IOException {
        active.set(true);
        server.start();
    }

    @Override
    public void stop() throws IOException {
        server.stop();
        active.set(false);
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public JMXServiceURL getAddress() {
        return url;
    }

    @Override
    public Map<String, ?> getAttributes() {
        return Collections.emptyMap();
    }
}
