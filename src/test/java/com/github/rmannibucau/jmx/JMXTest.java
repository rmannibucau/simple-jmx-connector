package com.github.rmannibucau.jmx;

import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JMXTest {
    @Test
    public void ensureServerStart() throws Exception {
        final JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:simple://localhost:1234");
        final JMXConnectorServer server = createServer(serviceURL);
        server.start();
        server.stop();
    }

    @Test
    public void clientServer() throws Exception {
        final JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:simple://localhost:1234");
        final JMXConnectorServer server = createServer(serviceURL);
        server.start();

        boolean found = false;
        {
            final JMXConnector client = JMXConnectorFactory.connect(serviceURL);
            final MBeanServerConnection connection = client.getMBeanServerConnection();

            assertNotNull(connection);

            for (final ObjectName on : connection.queryNames(null, null)) {
                if (on.getCanonicalName().equals("java.lang:type=Compilation")) {
                    found = true;
                    break;
                }
            }

            client.close();
        }

        server.stop();

        assertTrue(found);
    }

    private JMXConnectorServer createServer(final JMXServiceURL serviceURL) throws IOException {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        return JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, Collections.<String, Object>emptyMap(), mBeanServer);
    }
}
