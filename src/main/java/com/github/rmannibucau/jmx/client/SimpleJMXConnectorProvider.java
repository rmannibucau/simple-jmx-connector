package com.github.rmannibucau.jmx.client;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

public class SimpleJMXConnectorProvider implements JMXConnectorProvider {
    @Override
    public JMXConnector newJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) throws IOException {
        if (!serviceURL.getProtocol().equals("simple")) {
            throw new MalformedURLException("Protocol not simple: " + serviceURL.getProtocol());
        }

        return new SimpleClientConnector(serviceURL, environment);
    }
}
