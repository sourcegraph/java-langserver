package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourcegraph.lsp.domain.structures.SymbolDescriptor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceSymbolParams {

    public static WorkspaceSymbolParams of(String query, SymbolDescriptor symbol) {
        WorkspaceSymbolParams params = new WorkspaceSymbolParams();
        params.setQuery(query);
        params.setSymbol(symbol);
        return params;
    }

    private String query;

    private SymbolDescriptor symbol;

    public SymbolDescriptor getSymbol() {
        return symbol;
    }

    public void setSymbol(SymbolDescriptor symbol) {
        this.symbol = symbol;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public WorkspaceSymbolParams withQuery(String query) {
        this.setQuery(query);
        return this;
    }
}
