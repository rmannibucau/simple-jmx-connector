package com.github.rmannibucau.jmx.client;

import com.github.rmannibucau.jmx.shared.Request;
import com.github.rmannibucau.jmx.shared.Response;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleClientHandler implements InvocationHandler {
    private final AtomicLong id = new AtomicLong(1); // 0 is the credential
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final AnswerReader answerReader;
    private final ConcurrentMap<Long, CountDownLatch> latches = new ConcurrentHashMap<Long, CountDownLatch>();
    private final ConcurrentMap<Long, Response> responses = new ConcurrentHashMap<Long, Response>();
    private final long timeout;

    public SimpleClientHandler(final String id, long responseTimeout, final ObjectInputStream in, final ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.timeout = responseTimeout;

        answerReader = new AnswerReader();
        answerReader.setName("Answer Reader #" + id);
        answerReader.setDaemon(true);
        answerReader.start();
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (!MBeanServerConnection.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }

        // basic filtering
        final String methodName = method.getName();
        if (methodName.endsWith("NotificationListener") || "unregisterMBean".equals(methodName)) {
            return null; // not yet supported
        }
        if ("createMBean".equals(methodName)) {
            throw new UnsupportedOperationException();
        }

        // generate an id for the request, send the request "synchronizedly" to let the server get correct input by socket
        // then wait the returned value
        final long currentId = id.incrementAndGet();
        final CountDownLatch latch = new CountDownLatch(1);
        latches.put(currentId, latch);
        synchronized (out) {
            out.writeObject(new Request(currentId, methodName, args));
        }

        if (timeout > 0) {
            if (!latch.await(timeout, TimeUnit.SECONDS)) { // cleanup if time elapsed
                latches.remove(currentId);
            }
        } else {
            latch.await();
        }

        final Response response = responses.remove(currentId);
        if (response != null && response.isException()) {
            throw Throwable.class.cast(response.getValue());
        }
        return response.getValue();
    }

    public void forceStop() {
        for (final CountDownLatch latch : latches.values()) {
            latch.countDown();
        }
        answerReader.done();
    }

    private class AnswerReader extends Thread {
        private AtomicBoolean done = new AtomicBoolean(false);
        @Override
        public void run() {
            while (!done.get()) {
                if (done.get()) {
                    break;
                }

                try {
                    final Response response = Response.class.cast(in.readObject());
                    final CountDownLatch latch = latches.remove(response.getId());
                    if (latch != null) {
                        responses.putIfAbsent(response.getId(), response);
                        latch.countDown();
                    }
                } catch (final IOException e) {
                    break;
                } catch (final ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        public void done() {
            done.set(true);
        }
    }
}
