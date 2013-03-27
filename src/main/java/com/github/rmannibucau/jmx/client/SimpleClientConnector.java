package com.github.rmannibucau.jmx.client;

import com.github.rmannibucau.jmx.shared.Request;
import com.github.rmannibucau.jmx.shared.Response;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleClientConnector implements JMXConnector {
    private Collection<SimpleClientHandler> handlers = new CopyOnWriteArrayList<SimpleClientHandler>();
    private final Map<String, ?> envrt;
    private final JMXServiceURL url;
    private final NotificationBroadcasterSupport connectionBroadcaster;
    private String id = null;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SimpleClientConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) {
        this.url = serviceURL;
        this.envrt = environment;
        this.connectionBroadcaster = new NotificationBroadcasterSupport();
    }

    @Override
    public void connect() throws IOException {
        connect(envrt);
    }

    @Override
    public void connect(final Map<String, ?> env) throws IOException {
        final Map<String, Object> map = new HashMap<String, Object>();
        if (envrt != null) {
            map.putAll(envrt);
        }
        if (env != null) {
            map.putAll(env);
        }


        if (socket != null) {
            socket.close();
        }

        socket = new Socket(url.getHost(), url.getPort());
        id = System.nanoTime() + "." + System.identityHashCode(socket);

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        final Object credentials = map.get(CREDENTIALS);
        out.writeObject(new Request(0, null, new Object[] { credentials }));
        try {
            final Response response = Response.class.cast(in.readObject());
            if (response.isException()) {
                final Throwable cause = Throwable.class.cast(response.getValue());
                if (RuntimeException.class.isInstance(cause)) {
                    throw RuntimeException.class.cast(cause);
                }
                throw new IOException("Can't authenticate", cause);
            }
        } catch (final ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        return getMBeanServerConnection(null);
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
        final SimpleClientHandler handler = new SimpleClientHandler(id, in, out);
        handlers.add(handler);
        return MBeanServerConnection.class.cast(Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(), new Class<?>[] { MBeanServerConnection.class }, handler));
    }

    @Override
    public void close() throws IOException {
        for (final SimpleClientHandler handler : handlers) {
            handler.forceStop();
        }

        if (socket != null) {
            try {
                socket.getOutputStream().close();
                socket.close();
            } catch (final Exception e) {
                // no-op
            }
        }
    }

    public void addConnectionNotificationListener(final NotificationListener listener,
                                      final NotificationFilter filter,
                                      final Object handback) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        connectionBroadcaster.addNotificationListener(listener, filter, handback);
    }

    public void removeConnectionNotificationListener(final NotificationListener listener)
            throws ListenerNotFoundException {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        connectionBroadcaster.removeNotificationListener(listener);
    }

    public void removeConnectionNotificationListener(final NotificationListener listener,
                                         final NotificationFilter filter,
                                         final Object handback) throws ListenerNotFoundException {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        connectionBroadcaster.removeNotificationListener(listener, filter, handback);
    }

    @Override
    public String getConnectionId() throws IOException {
        return id;
    }
}
