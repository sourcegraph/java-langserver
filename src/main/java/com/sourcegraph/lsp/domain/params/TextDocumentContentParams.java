package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentContentParams {

    TextDocumentIdentifier textDocument;

    public TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }

    public TextDocumentContentParams withTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
        return this;
    }
}
