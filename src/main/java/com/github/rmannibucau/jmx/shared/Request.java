package com.github.rmannibucau.jmx.shared;

import java.io.Serializable;

public class Request implements Serializable {
    private long id;
    private String name;
    private Object[] params;

    public Request(final long id, final String name, final Object[] params) {
        this.id = id;
        this.name = name;
        this.params = params;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object[] getParams() {
        return params;
    }

    public int paramNumber() {
        if (params == null) {
            return 0;
        }
        return params.length;
    }
}
