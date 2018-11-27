package com.sourcegraph.lsp.domain.result;

import com.sourcegraph.lsp.domain.structures.ServerCapabilities;

public class InitializeResult {

    private ServerCapabilities capabilities;

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public InitializeResult withCapabilities(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }
}
