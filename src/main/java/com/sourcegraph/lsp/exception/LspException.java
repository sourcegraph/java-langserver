package com.sourcegraph.lsp.exception;

/**
 * Created by beyang on 1/27/17.
 */
public class LspException extends Exception {
    public LspException(String reason) {
        super(reason);
    }

    public String toString() {
        return String.format("LspException: %s", super.getMessage());
    }
}
