package com.github.rmannibucau.jmx.server;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

public class SimpleJMXConnectorServerProvider implements JMXConnectorServerProvider {
    @Override
    public JMXConnectorServer newJMXConnectorServer(final JMXServiceURL serviceURL,
                                                    final Map<String, ?> environment,
                                                    final MBeanServer mbeanServer) throws IOException {
        if (!serviceURL.getProtocol().equals("simple")) {
            throw new MalformedURLException("Protocol not simple: " + serviceURL.getProtocol());
        }
        return new SimpleJMXConnectorServer(serviceURL, environment, mbeanServer);
    }
}
