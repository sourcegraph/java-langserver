package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.structures.Position;
import com.sourcegraph.lsp.domain.structures.ReferenceContext;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceParams extends TextDocumentPositionParams {

    private ReferenceContext context;

    public ReferenceContext getContext() {
        return context;
    }

    public void setContext(ReferenceContext context) {
        this.context = context;
    }

    public ReferenceParams withContext(ReferenceContext context) {
        this.setContext(context);
        return this;
    }

    @Override
    public ReferenceParams withTextDocument(TextDocumentIdentifier textDocument) {
        this.setTextDocument(textDocument);
        return this;
    }

    @Override
    public ReferenceParams withPosition(Position position) {
        this.setPosition(position);
        return this;
    }

}
