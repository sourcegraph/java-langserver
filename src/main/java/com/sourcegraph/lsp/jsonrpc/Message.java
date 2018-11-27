package com.sourcegraph.lsp.jsonrpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private String jsonrpc = "2.0";

    private String method = null;

    private Object params = null;

    private Object result = null;

    private Error error = null;

    private Object id = null;

    private Map<String, String> meta;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    // not exclusive with isNotification, since notifications are a subtype of requests
    public boolean isRequest() {
        return method != null;
    }

    public boolean isNotification() {
        return isRequest() && id == null;
    }

    // not exclusive with isError, since errors are a subtype of responses
    public boolean isResponse() {
        return !isRequest() && id != null;
    }

    public boolean isError() {
        return isResponse() && error != null;
    }

    public Message withJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
        return this;
    }
}
