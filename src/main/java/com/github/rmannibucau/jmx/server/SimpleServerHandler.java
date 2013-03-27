package com.github.rmannibucau.jmx.server;

import com.github.rmannibucau.jmx.server.security.JMXSubjetCombiner;
import com.github.rmannibucau.jmx.shared.Request;
import com.github.rmannibucau.jmx.shared.Response;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class SimpleServerHandler implements Runnable {
    private final ObjectInputStream input;
    private final ObjectOutputStream output;
    private final Socket socket;
    private final SimpleJMXConnectorServer server;
    private final JMXSubjetCombiner jmxSubjetCombiner;

    public SimpleServerHandler(final JMXAuthenticator authenticator, final SimpleJMXConnectorServer mbeanServerProvider, final Socket socket) throws Exception {
        this.socket = socket;
        this.server = mbeanServerProvider;

        try {
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
        } catch (final IOException se) {
            throw new IllegalArgumentException(se);
        }

        final Request credentials = Request.class.cast(input.readObject());
        if (credentials.getId() != 0) {
            throw new IllegalStateException("creadential id should be 0");
        }
        if (authenticator != null) {
            try {
                final Subject subject = authenticator.authenticate(credentials.getParams()[0]);
                jmxSubjetCombiner = new JMXSubjetCombiner(subject);
            } catch (final Exception e) {
                if (socket.isConnected()) {
                    output.writeObject(new Response(0, true, e));
                }
                throw e;
            }
        } else {
            jmxSubjetCombiner = null;
        }
        output.writeObject(new Response(0, false, null)); // ACK for credentials
    }

    @Override
    public void run() {
        try {
            while (true) {
                Throwable error = null;
                Request request = null;
                try {
                    request = Request.class.cast(input.readObject());
                } catch (final ClassNotFoundException e) {
                    error = e;
                }

                Object result = null;
                if (request != null) {
                    final Method method = findMethod(request.getName(), request.paramNumber());
                    final Object[] params = request.getParams();
                    final MBeanServer mBeanServer = server.getMBeanServer();

                    if (jmxSubjetCombiner != null) {
                        try {
                            result = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                                @Override
                                public Object run() {
                                    try {
                                        return method.invoke(mBeanServer, params);
                                    } catch (final IllegalAccessException e) {
                                        throw new UnwrappableException(e);
                                    } catch (final InvocationTargetException e) {
                                        throw new UnwrappableException(e.getCause());
                                    }
                                }
                            }, new AccessControlContext(AccessController.getContext(), jmxSubjetCombiner));
                        } catch (final UnwrappableException re) {
                            error = Exception.class.cast(re.getCause());
                        }
                    } else {
                        try {
                            result = method.invoke(mBeanServer, params);
                        } catch (final IllegalAccessException e) {
                            error = e;
                        } catch (final InvocationTargetException e) {
                            error = e.getCause();
                        }
                    }
                } else{
                    result = error;
                }

                if (error != null) {
                    result = error;
                }

                output.writeObject(new Response(request.getId(), error != null, result));
            }
        } catch (final IOException end) {
            // no-op: communication end
        } finally {
            close(input);
            close(output);
            if (socket != null) {
                try {
                    socket.close();
                } catch (final IOException e) {
                    // no-op
                }
            }
        }
    }

    private void close(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                // no-op
            }
        }
    }

    private static Method findMethod(final String methodName, final int paramNumber) {
        for (final Method m : MBeanServer.class.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length == paramNumber) {
                return m;
            }
        }
        throw new IllegalArgumentException("Method " + methodName + " not found with " + paramNumber + " parameters");
    }

    private static class UnwrappableException extends RuntimeException {
        private UnwrappableException(final Throwable e) {
            super(e);
        }
    }
}
