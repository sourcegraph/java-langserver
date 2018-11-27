package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.structures.TextDocumentItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DidOpenTextDocumentParams {

    private TextDocumentItem textDocument;

    public TextDocumentItem getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(TextDocumentItem textDocument) {
        this.textDocument = textDocument;
    }
}
