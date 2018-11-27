package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.Response;

public interface ResponseHandler<R> {
    void handle(Response<R> response);
}
