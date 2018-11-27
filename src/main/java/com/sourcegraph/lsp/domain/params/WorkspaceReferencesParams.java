package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourcegraph.lsp.domain.structures.SymbolDescriptor;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceReferencesParams {
    /**
     * Metadata about the symbol that is being searched for.
     */
    SymbolDescriptor query;

    /**
     * An optional set of hints that can be used to optimize the search.
     */
    Map<String, String> hints;

    Integer limit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkspaceReferencesParams that = (WorkspaceReferencesParams) o;

        if (query != null ? !query.equals(that.query) : that.query != null) return false;
        if (hints != null ? !hints.equals(that.hints) : that.hints != null) return false;
        return limit == null ? that.limit == null : that.limit != null && limit.equals(that.limit);
    }

    @Override
    public int hashCode() {
        int result = query != null ? query.hashCode() : 0;
        result = 31 * result + (hints != null ? hints.hashCode() : 0);
        result = 31 * result + (limit != null ? limit : 0);
        return result;
    }

    public SymbolDescriptor getQuery() {
        return query;
    }

    public void setQuery(SymbolDescriptor query) {
        this.query = query;
    }

    public Map<String, String> getHints() {
        return hints;
    }

    public void setHints(Map<String, String> hints) {
        this.hints = hints;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public static WorkspaceReferencesParams of(SymbolDescriptor query, Map<String, String> hints, Integer limit) {
        WorkspaceReferencesParams params = new WorkspaceReferencesParams();
        params.setQuery(query);
        params.setHints(hints);
        params.setLimit(limit);
        return params;
    }
}
