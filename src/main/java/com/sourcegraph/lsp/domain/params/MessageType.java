package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by beyang on 7/26/17.
 */
public enum MessageType {
    ERROR(1),
    WARNING(2),
    INFO(3),
    LOG(4);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
