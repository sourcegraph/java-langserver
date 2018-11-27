package com.sourcegraph.lsp.jsonrpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {

    private Code code;

    private String message;

    private Object data = null;

    public Code getCode() {
        return code;
    }

    @JsonProperty("code")
    public int getNumericCode() {
        return code.getValue();
    }

    public void setCode(Code code) {
        this.code = code;
    }

    @JsonProperty("code")
    public void setNumericCode(int value) {
        this.code = Code.fromInt(value);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("Code: %s, Message: %s", this.getCode().toString(), this.getMessage());
    }

    public Error withCode(Code code) {
        this.code = code;
        return this;
    }

    public Error withMessage(String message) {
        this.message = message;
        return this;
    }

    public Error withData(Object data) {
        this.data = data;
        return this;
    }

    public enum Code {

        PARSE_ERROR(-32700),
        INVALID_REQUEST(-32600),
        METHOD_NOT_FOUND(-32601),
        INVALID_PARAMS(-32602),
        INTERNAL_ERROR(-32603),
        RESOURCE_NOT_FOUND(-32604);
        // add more custom error constants if needed, with codes between -32000 to -32099

        private int value;

        Code(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Code fromInt(int value) {
            for (Code code : Code.values()) {
                if (code.getValue() == value) {
                    return code;
                }
            }
            return INTERNAL_ERROR;
        }
    }

}
