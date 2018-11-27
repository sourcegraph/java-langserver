package com.sourcegraph.lsp.domain;

import java.util.HashSet;
import java.util.Set;

public enum Method {
    // General
    INITIALIZE("initialize"),
    SHUTDOWN("shutdown"),
    EXIT("exit"),
    CANCEL_REQUEST("$/cancelRequest"),
    PARTIAL_RESULT("$/partialResult"),
    // Workspace
    WORKSPACE_SYMBOL("workspace/symbol"),
    WORKSPACE_XPACKAGES("workspace/xpackages"),
    WORKSPACE_XDEPENDENCIES("workspace/xdependencies"),
    WORKSPACE_XREFERENCES("workspace/xreferences"),
    WORKSPACE_FILES("workspace/xfiles"),
    // Workspace server-to-client
    WORKSPACE_CONFIG("workspace/configuration"),
    // Document
    TEXT_DOCUMENT_DID_CLOSE("textDocument/didClose"),
    TEXT_DOCUMENT_DID_OPEN("textDocument/didOpen"),
    TEXT_DOCUMENT_DID_CHANGE("textDocument/didChange"),
    TEXT_DOCUMENT_DID_SAVE("textDocument/didSave"),
    TEXT_DOCUMENT_HOVER("textDocument/hover"),
    TEXT_DOCUMENT_REFERENCES("textDocument/references"),
    TEXT_DOCUMENT_DOCUMENT_SYMBOL("textDocument/documentSymbol"),
    TEXT_DOCUMENT_DEFINITION("textDocument/definition"),
    TEXT_DOCUMENT_XDEFINITION("textDocument/xdefinition"),
    TEXT_DOCUMENT_CONTENT("textDocument/xcontent"),
    // Window
    WINDOW_SHOW_MESSAGE("window/showMessage"),
    // Cache extension
    CACHE_GET("xcache/get"),
    CACHE_SET("xcache/set"),
    // Catch-all for unsupported methods
    UNKNOWN(null);

    private static Set<Method> FILE_SYSTEM_REQUEST_METHODS = new HashSet<>();

    static {
        FILE_SYSTEM_REQUEST_METHODS.add(TEXT_DOCUMENT_DID_OPEN);
        FILE_SYSTEM_REQUEST_METHODS.add(TEXT_DOCUMENT_DID_CLOSE);
        FILE_SYSTEM_REQUEST_METHODS.add(TEXT_DOCUMENT_DID_CHANGE);
        FILE_SYSTEM_REQUEST_METHODS.add(TEXT_DOCUMENT_DID_SAVE);
    }
    private String name;

    Method(String name) {
        this.name = name;
    }

    public static Method fromString(String name) {
        if (name == null) return null;
        for (Method method : Method.values()) {
            if (name.equals(method.toString())) {
                return method;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isFileSystemMethod(Method method) {
        return FILE_SYSTEM_REQUEST_METHODS.contains(method);
    }

}
