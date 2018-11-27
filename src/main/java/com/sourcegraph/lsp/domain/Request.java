package com.sourcegraph.lsp.domain;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request<Params> {

    private String jsonrpc = "2.0";

    private Method method;

    private Params params;

    private Object id = null;

    private Map<String, String> meta;

    @JsonIgnore
    private Class<Params> paramsClass;

    public Request() {
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Method getMethod() {
        return method;
    }

    @JsonProperty("method")
    public String getMethodAsString() {
        return method.toString();
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @JsonProperty("method")
    public void setMethodAsString(String methodName) {
        this.method = Method.fromString(methodName);
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Class<Params> getParamsClass() {
        return paramsClass;
    }

    public Request<Params> withMethod(Method method) {
        this.method = method;
        return this;
    }

    public Request<Params> withParams(Params params) {
        this.params = params;
        return this;
    }

    public Request<Params> withId(Object id) {
        this.id = id;
        return this;
    }

    public static <P> Request<P> of(Class<P> paramsClass) {
        return new Request<>(paramsClass);
    }

    private Request(Class<Params> paramsClass) {
        this.paramsClass = paramsClass;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public Request<Params> withMeta(Map<String, String> meta) {
        this.meta = meta;
        return this;
    }


}
