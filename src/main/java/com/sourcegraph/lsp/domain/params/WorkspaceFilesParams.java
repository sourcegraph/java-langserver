package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceFilesParams {

    private String base = null;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public WorkspaceFilesParams withBase(String base) {
        this.base = base;
        return this;
    }
}
