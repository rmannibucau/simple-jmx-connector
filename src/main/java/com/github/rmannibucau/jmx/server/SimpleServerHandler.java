package com.github.rmannibucau.jmx.server;

import com.github.rmannibucau.jmx.shared.Request;
import com.github.rmannibucau.jmx.shared.Response;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.lang.reflect.Method;
import java.net.Socket;

public class SimpleServerHandler implements Runnable {
    private final ObjectInputStream input;
    private final ObjectOutputStream output;
    private final Socket socket;
    private final MBeanServer server;
    private final Subject subject;

    public SimpleServerHandler(final JMXAuthenticator authenticator, final MBeanServer mbeanServer, final Socket socket) {
        this.socket = socket;
        this.server = mbeanServer;

        try {
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
        } catch (final IOException se) {
            throw new IllegalArgumentException(se);
        }

        Subject s;
        try {
            final Object credentials = input.readObject();
            if (authenticator != null) {
                s = authenticator.authenticate(credentials);
            } else {
                s = null;
            }
        } catch (final OptionalDataException ode) {
            s = null;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Can't get credentials", e);
        }
        subject = s;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final Request request = Request.class.cast(input.readObject());
                final Method method = findMethod(request.getName(), request.paramNumber());
                // TODO: bind subject
                final Object result = method.invoke(server, request.getParams());
                output.writeObject(new Response(request.getId(), result));
            }
        } catch (final IOException end) {
            // no-op: communication end
        } catch (final Exception e) {
            e.printStackTrace();
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
}
