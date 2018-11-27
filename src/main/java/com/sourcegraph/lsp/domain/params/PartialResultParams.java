package com.sourcegraph.lsp.domain.params;

public class PartialResultParams {

    Object id;

    // using Object here is fine for serializing; we'll worry about deserialization if it ever comes up
    Object patch;

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getPatch() {
        return patch;
    }

    public void setPatch(Object patch) {
        this.patch = patch;
    }

    public static PartialResultParams of(Object id, Object patch) {
        PartialResultParams params = new PartialResultParams();
        params.id = id;
        params.patch = patch;
        return params;
    }
}
