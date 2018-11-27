package com.sourcegraph.lsp.domain.params;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheGetParams {

    private String key;

    public static CacheGetParams of(String key) {
        CacheGetParams p = new CacheGetParams();
        p.key = key;
        return p;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
