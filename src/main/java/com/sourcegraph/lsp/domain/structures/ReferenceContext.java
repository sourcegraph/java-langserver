package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceContext {

    private boolean includeDeclaration;

    private Integer xlimit;

    public boolean isIncludeDeclaration() {
        return includeDeclaration;
    }

    public void setIncludeDeclaration(boolean includeDeclaration) {
        this.includeDeclaration = includeDeclaration;
    }

    public ReferenceContext withIncludeDeclaration(boolean includeDeclaration) {
        this.setIncludeDeclaration(includeDeclaration);
        return this;
    }

    public ReferenceContext withXlimit(Integer xlimit) {
        this.setXlimit(xlimit);
        return this;
    }

    public Integer getXlimit() {
        return xlimit;
    }

    public void setXlimit(Integer xlimit) {
        this.xlimit = xlimit;
    }
}
