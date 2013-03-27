package com.github.rmannibucau.jmx.shared;

import java.io.Serializable;

public class Response implements Serializable {
    private long id;
    private boolean exception;
    private Object value;

    public Response(final long id, final boolean error, final Object value) {
        this.id = id;
        this.exception = error;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public boolean isException() {
        return exception;
    }

    public void setException(final boolean exception) {
        this.exception = exception;
    }
}
