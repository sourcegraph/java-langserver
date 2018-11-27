package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.params.*;

import java.util.Map;

public interface MessageHandlerService {
    // General
    void initialize(Map<String, Object> ctx, Request<InitializeParams> request);
    void shutdown(Map<String, Object> ctx, Request<Void> request);
    void exit(Map<String, Object> ctx, Request<Void> request);
    void cancelRequest(Map<String, Object> ctx, Request<CancelParams> request);
    // Workspace
    void workspaceSymbol(Map<String, Object> ctx, Request<WorkspaceSymbolParams> request);
    void workspaceFiles(Map<String, Object> ctx, Request<WorkspaceFilesParams> request);
    void workspaceXPackages(Map<String, Object> ctx, Request<Void> request);
    void workspaceXDependencies(Map<String, Object> ctx, Request<Void> request);
    void workspaceXReferences(Map<String, Object> ctx, Request<WorkspaceReferencesParams> request);
    // Document
    void textDocumentDidClose(Map<String, Object> ctx, Request<DidCloseTextDocumentParams> request);
    void textDocumentDidOpen(Map<String, Object> ctx, Request<DidOpenTextDocumentParams> request);
    void textDocumentHover(Map<String, Object> ctx, Request<TextDocumentPositionParams> request);
    void textDocumentReferences(Map<String, Object> ctx, Request<ReferenceParams> request);
    void textDocumentDocumentSymbol(Map<String, Object> ctx, Request<DocumentSymbolParams> request);
    void textDocumentDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request);
    void textDocumentXDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request);
    void textDocumentContent(Map<String, Object> ctx, Request<TextDocumentContentParams> request);
    // TODO: add the rest
}
