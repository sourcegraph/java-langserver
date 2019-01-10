package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentIdentifier {

    public static TextDocumentIdentifier of(String uri) {
        return new TextDocumentIdentifier().withUri(uri);
    }

    private String uri;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public TextDocumentIdentifier withUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextDocumentIdentifier that = (TextDocumentIdentifier) o;

        return uri != null ? uri.equals(that.uri) : that.uri == null;
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), uri);
    }
}
