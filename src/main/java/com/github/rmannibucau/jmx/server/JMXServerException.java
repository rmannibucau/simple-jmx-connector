package com.github.rmannibucau.jmx.server;

public class JMXServerException extends RuntimeException {
    public JMXServerException(final Exception e) {
        super(e);
    }
}
