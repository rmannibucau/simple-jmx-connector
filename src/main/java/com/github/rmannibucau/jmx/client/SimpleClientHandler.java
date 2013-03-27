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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleClientHandler implements InvocationHandler {
    private final AtomicLong id = new AtomicLong(0);
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final AnswerReader answerReader;
    private final ConcurrentMap<Long, CountDownLatch> latches = new ConcurrentHashMap<Long, CountDownLatch>();
    private final ConcurrentMap<Long, Object> responses = new ConcurrentHashMap<Long, Object>();

    public SimpleClientHandler(String id, final ObjectInputStream in, final ObjectOutputStream out) {
        this.in = in;
        this.out = out;

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
            return null;
        }

        if ("createMBean".equals(methodName)) {
            throw new UnsupportedOperationException();
        }

        final long currentId = id.incrementAndGet();
        final CountDownLatch latch = new CountDownLatch(1);
        latches.put(currentId, latch);
        synchronized (out) {
            out.writeObject(new Request(currentId, methodName, args));
        }
        latch.await();
        return responses.remove(currentId);
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
                    responses.putIfAbsent(response.getId(), response.getValue());
                    latches.remove(response.getId()).countDown();
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
