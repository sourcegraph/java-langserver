package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.structures.JsonPatch;

public interface PartialResultStreamer {
    void sendPartialResult(Object requestId, JsonPatch jsonPatch);
}
