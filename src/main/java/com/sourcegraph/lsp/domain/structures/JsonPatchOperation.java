package com.sourcegraph.lsp.domain.structures;

public class JsonPatchOperation {

    private String op;

    private String path;

    private Object value;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static JsonPatchOperation of(String op, String path, Object value) {
        JsonPatchOperation patch = new JsonPatchOperation();
        patch.op = op;
        patch.path = path;
        patch.value = value;
        return patch;
    }
}
