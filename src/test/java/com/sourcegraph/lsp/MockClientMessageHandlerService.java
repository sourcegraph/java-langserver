package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.Response;
import com.sourcegraph.lsp.domain.params.*;
import com.sourcegraph.lsp.domain.result.WorkspaceFilesResult;
import com.sourcegraph.lsp.domain.structures.TextDocumentItem;
import com.sourcegraph.lsp.jsonrpc.Error;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by beyang on 1/26/17.
 */
public class MockClientMessageHandlerService implements MessageHandlerService {

    private Controller controller;
    private FileContentProvider fileContentProvider;

    public MockClientMessageHandlerService(Controller controller, FileContentProvider fileContentProvider) {
        this.fileContentProvider = fileContentProvider;
        this.controller = controller;
    }

    @Override
    public void initialize(Map<String, Object> ctx, Request<InitializeParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown(Map<String, Object> ctx, Request<Void> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exit(Map<String, Object> ctx, Request<Void> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelRequest(Map<String, Object> ctx, Request<CancelParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void workspaceSymbol(Map<String, Object> ctx, Request<WorkspaceSymbolParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentDidClose(Map<String, Object> ctx, Request<DidCloseTextDocumentParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentDidOpen(Map<String, Object> ctx, Request<DidOpenTextDocumentParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentHover(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentReferences(Map<String, Object> ctx, Request<ReferenceParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentDocumentSymbol(Map<String, Object> ctx, Request<DocumentSymbolParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void workspaceXReferences(Map<String, Object> ctx, Request<WorkspaceReferencesParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void textDocumentXDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void workspaceXPackages(Map<String, Object> ctx, Request<Void> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void workspaceXDependencies(Map<String, Object> ctx, Request<Void> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void workspaceFiles(Map<String, Object> ctx, Request<WorkspaceFilesParams> request) {
        try {
            WorkspaceFilesResult res = new WorkspaceFilesResult();
            res.addAll(fileContentProvider.listFilesRecursively(request.getParams().getBase()));
            Response<WorkspaceFilesResult> filesResponse = new Response<WorkspaceFilesResult>()
                    .withId(request.getId())
                    .withResult(res);
            this.controller.send(filesResponse);
        } catch (Exception e) {
            this.controller.send(new Response<WorkspaceFilesResult>().withId(request.getId()).withError(
                    new Error().withCode(Error.Code.RESOURCE_NOT_FOUND).withMessage(e.getMessage())));
        }
    }

    @Override
    public void textDocumentContent(Map<String, Object> ctx, Request<TextDocumentContentParams> request) {
        String uri = request.getParams().getTextDocument().getUri();
        try (InputStream is = fileContentProvider.readContent(uri)) {
            Response<TextDocumentItem> resp = new Response<TextDocumentItem>().withId(request.getId()).withResult(
                    new TextDocumentItem()
                            .withLanguageId("java")
                            .withUri(uri)
                            .withVersion(1)
                            .withText(IOUtils.toString(is, StandardCharsets.UTF_8))
            );
            this.controller.send(resp);
        } catch (Exception e) {
            this.controller.send(new Response<TextDocumentItem>().withId(request.getId()).withError(
                    new Error().withCode(Error.Code.RESOURCE_NOT_FOUND).withMessage(String.format("content at uri %s not found", uri))
            ));
        }
    }
}
