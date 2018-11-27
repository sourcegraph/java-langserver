package com.sourcegraph.langserver.langservice.javaconfigjson;

public class RepositoryInfo {

    private String id;

    private String url;

    private String layout;

    @SuppressWarnings("unused") // for JSON deserialization
    public RepositoryInfo() {}

    public RepositoryInfo(String id, String url, String layout) {
        this.id = id;
        this.url = url;
        this.layout = layout;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLayout() {
        if (layout == null) {
            return "default";
        }
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }
}
