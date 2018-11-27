package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.structures.Position;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentPositionParams {

    private TextDocumentIdentifier textDocument;

    private Position position;

    public TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public TextDocumentPositionParams withTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
        return this;
    }

    public TextDocumentPositionParams withPosition(Position position) {
        this.position = position;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextDocumentPositionParams that = (TextDocumentPositionParams) o;

        if (textDocument != null ? !textDocument.equals(that.textDocument) : that.textDocument != null) return false;
        return position != null ? position.equals(that.position) : that.position == null;
    }

    @Override
    public int hashCode() {
        int result = textDocument != null ? textDocument.hashCode() : 0;
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }
}
