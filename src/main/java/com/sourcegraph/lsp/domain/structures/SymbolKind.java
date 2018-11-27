package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SymbolKind {

    FILE(1),
    MODULE(2),
    NAMESPACE(3),
    PACKAGE(4),
    CLASS(5),
    METHOD(6),
    PROPERTY(7),
    FIELD(8),
    CONSTRUCTOR(9),
    ENUM(10),
    INTERFACE(11),
    FUNCTION(12),
    VARIABLE(13),
    CONSTANT(14),
    STRING(15),
    NUMBER(16),
    BOOLEAN(17),
    ARRAY(18);

    private int value;

    SymbolKind(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static SymbolKind fromValue(int value) {
        for (SymbolKind symbolKind : SymbolKind.values()) {
            if (symbolKind.getValue() == value) return symbolKind;
        }
        throw new IllegalArgumentException("Unknown symbol kind " + value);
    }
}
