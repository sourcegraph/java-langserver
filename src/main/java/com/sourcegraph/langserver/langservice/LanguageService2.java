package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.lsp.domain.params.InitializeParams;

/**
 * LanguageService2 is the core of the language server. It defines LSP endpoint methods and maintains
 * all internal compiler and build state. It is threadsafe.
 */
public class LanguageService2 {
    private WorkspaceManager workspaceManager;

    public LanguageService2() {
    }

    public void initialize(InitializeParams p) {
        // TODO: initialize WorkspaceManager
    }
}
