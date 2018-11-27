package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.Request;

import java.util.Map;

public interface RequestHandler<P> {
    void handle(Map<String, Object> ctx, Request<P> request);
}
