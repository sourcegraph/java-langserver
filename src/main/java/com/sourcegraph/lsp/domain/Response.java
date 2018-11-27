package com.sourcegraph.lsp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourcegraph.lsp.jsonrpc.Error;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response<Result> {

    private String jsonrpc = "2.0";

    private Result result = null;

    private Error error = null;

    private Object id = null;

    @JsonIgnore
    private Class<Result> resultClass;

    public Response() {
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
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

    public Class<Result> getResultClass() {
        return resultClass;
    }

    public Response<Result> withJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
        return this;
    }

    public Response<Result> withResult(Result result) {
        this.result = result;
        return this;
    }

    public Response<Result> withError(Error error) {
        this.error = error;
        return this;
    }

    public Response<Result> withId(Object id) {
        this.id = id;
        return this;
    }

    public static <R> Response<R> of(Class<R> resultClass) {
        return new Response<>(resultClass);
    }

    private Response(Class<Result> resultClass) {
        this.resultClass = resultClass;
    }
}
