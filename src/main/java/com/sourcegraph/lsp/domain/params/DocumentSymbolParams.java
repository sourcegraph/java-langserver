package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentSymbolParams {

    private TextDocumentIdentifier textDocument;

    public TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }

    public DocumentSymbolParams withTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
        return this;
    }

    public static DocumentSymbolParams of(TextDocumentIdentifier textDocument) {
        DocumentSymbolParams params = new DocumentSymbolParams();
        params.setTextDocument(textDocument);
        return params;
    }
}
