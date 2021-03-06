package com.github.rmannibucau.jmx.server;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleServer {
    private static final Logger LOGGER = Logger.getLogger(SimpleServer.class.getName());

    private final ServerThread serverThread;

    public SimpleServer(final JMXServiceURL url, final Map<String, ?> envrt,
                        final SimpleJMXConnectorServer server, final ClassLoader contextClassLoader) {
        serverThread = new ServerThread(url, envrt, server, contextClassLoader);
        serverThread.setName("JMX Server");
    }

    public void start() {
        if (serverThread.isAlive()) {
            stop();
        }
        serverThread.start();
    }

    public void stop() {
        serverThread.close();
    }

    private class ServerThread extends Thread {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final Map<String, ?> environment;
        private final JMXServiceURL url;
        private final ExecutorService es;
        private final JMXAuthenticator authenticator;
        private final SimpleJMXConnectorServer mbeanServerProvider;
        private ServerSocket server;

        public ServerThread(final JMXServiceURL url, final Map<String, ?> envrt,
                            final SimpleJMXConnectorServer mbeanServerProvider, final ClassLoader classloader) {
            this.url = url;
            this.environment = envrt;
            this.mbeanServerProvider = mbeanServerProvider;

            int poolSize = 10;
            if (envrt.containsKey("poolSize")) {
                poolSize =  Number.class.cast(envrt.get("poolSize")).intValue();
            }

            this.es = Executors.newFixedThreadPool(poolSize, new ServletThreadFactory(classloader));
            this.authenticator = JMXAuthenticator.class.cast(environment.get(JMXConnectorServer.AUTHENTICATOR));
        }

        @Override
        public void run() {
            try {
                server = new ServerSocket(url.getPort());
            } catch (final IOException e) {
                throw new JMXServerException(e);
            }

            while (!done.get()) {
                try {
                    final Socket socket = server.accept();
                    if (!done.get() && !es.isShutdown()) {
                        //socket.setSoTimeout(soTimeout);
                        try {
                            es.submit(new SimpleServerHandler(authenticator, mbeanServerProvider, socket));
                        } catch (final Exception se) {
                            socket.close();
                        }
                    } else {
                        socket.close();
                    }
                } catch (final IOException e) {
                    if (server.isClosed()) {
                        throw new JMXServerException(e);
                    }
                } catch (final RejectedExecutionException ree) {
                    if (server.isClosed()) {
                        throw new JMXServerException(ree);
                    }
                    LOGGER.log(Level.WARNING, "Too much connections", ree);
                }
            }
        }

        public void close() {
            done.set(true);

            es.shutdown();

            try { // force iteration un run()
                new Socket("127.0.0.1", url.getPort()).close();
            } catch (final IOException e) {
                // no-op
            }

            try {
                es.awaitTermination(1, TimeUnit.MINUTES);
            } catch (final InterruptedException e) {
                // no-op
            }

            if (server == null) {
                return;
            }

            try {
                server.close();
            } catch (final IOException e) {
                // no-op
            }
        }
    }

    private static class ServletThreadFactory implements ThreadFactory {
        private static final AtomicInteger ID = new AtomicInteger();
        private final ClassLoader loader;

        public ServletThreadFactory(final ClassLoader classloader) {
            loader = classloader;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r);
            thread.setContextClassLoader(loader);
            thread.setName("JMX Server Handler #" + ID.incrementAndGet());
            return thread;
        }
    }
}
