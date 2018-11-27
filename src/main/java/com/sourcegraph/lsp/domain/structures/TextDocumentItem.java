package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentItem {

    private String uri;

    private String languageId = "java";

    private int version = 0;

    private String text;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TextDocumentItem withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public TextDocumentItem withLanguageId(String languageId) {
        this.languageId = languageId;
        return this;
    }

    public TextDocumentItem withVersion(int version) {
        this.version = version;
        return this;
    }

    public TextDocumentItem withText(String text) {
        this.text = text;
        return this;
    }

}
