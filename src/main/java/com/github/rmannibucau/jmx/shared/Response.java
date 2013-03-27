package com.github.rmannibucau.jmx.shared;

import java.io.Serializable;

public class Response implements Serializable {
    private long id;
    private Object value;

    public Response(final long id, final Object value) {
        this.id = id;
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
}
