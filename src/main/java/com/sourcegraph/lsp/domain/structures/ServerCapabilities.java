package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerCapabilities {

    // TODO: update these when more methods are supported
    private boolean hoverProvider = true;

    private boolean definitionProvider = true;

    private boolean xdefinitionProvider = true;

    private boolean referencesProvider = true;

    private boolean xworkspaceReferencesProvider = true;

    private boolean workspaceSymbolProvider = false;

    private boolean streaming = true;

    public boolean isHoverProvider() {
        return hoverProvider;
    }

    public void setHoverProvider(boolean hoverProvider) {
        this.hoverProvider = hoverProvider;
    }

    public boolean isDefinitionProvider() {
        return definitionProvider;
    }

    public void setDefinitionProvider(boolean definitionProvider) {
        this.definitionProvider = definitionProvider;
    }

    public boolean isXdefinitionProvider() { return xdefinitionProvider; }

    public void setXdefinitionProvider(boolean xdefinitionProvider) { this.xdefinitionProvider = xdefinitionProvider; }

    public boolean isReferencesProvider() {
        return referencesProvider;
    }

    public void setReferencesProvider(boolean referencesProvider) {
        this.referencesProvider = referencesProvider;
    }

    public boolean isXworkspaceReferencesProvider() { return this.xworkspaceReferencesProvider; }

    public void setXworkspaceReferencesProvider(boolean xworkspaceReferencesProvider) {
        this.xworkspaceReferencesProvider = xworkspaceReferencesProvider;
    }

    public boolean isWorkspaceSymbolProvider() {
        return workspaceSymbolProvider;
    }

    public void setWorkspaceSymbolProvider(boolean workspaceSymbolProvider) {
        this.workspaceSymbolProvider = workspaceSymbolProvider;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }
}
