package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceConfigurationParams {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private String scopeUri;
        private String section;

        public Item(String scopeUri, String section) {
            this.scopeUri = scopeUri;
            this.section = section;
        }

        public String getScopeUri() {
            return scopeUri;
        }

        public void setScopeUri(String scopeUri) {
            this.scopeUri = scopeUri;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }
    }

    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
