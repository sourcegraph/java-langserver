package com.sourcegraph.langserver;

import com.google.common.collect.Lists;
import com.sourcegraph.common.Config;
import com.sourcegraph.langserver.langservice.*;
import com.sourcegraph.langserver.langservice.files.CachingFileContentProvider;
import com.sourcegraph.langserver.langservice.files.OverlayContentProvider;
import com.sourcegraph.langserver.langservice.workspace.Workspaces;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.langserver.langservice.workspace.Workspace;
import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.lsp.Controller;
import com.sourcegraph.lsp.MessageHandlerService;
import com.sourcegraph.lsp.domain.Method;
import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.Response;
import com.sourcegraph.lsp.domain.params.*;
import com.sourcegraph.lsp.domain.result.InitializeResult;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.*;
import com.sourcegraph.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaLspHandlerService implements MessageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(JavaLspHandlerService.class);

    private final Controller controller;

    // TODO: guarantee that these are set before they're used -- we assume we'll receive an init request before anything else
    private LanguageService languageService;
    private WorkspaceManager workspaceManager;

    private final AtomicBoolean receivedShutdownRequest = new AtomicBoolean(false);

    private final boolean vfs;

    private Object isInitializedMu = new Object();
    private CompletableFuture<Void> isInitialized;

    private Util.Timer start;

    public JavaLspHandlerService(Controller controller, boolean vfs) {
        this.controller = controller;
        this.vfs = vfs;
        this.workspaceManager = null;
        this.languageService = null;
    }

    @Override
    public void initialize(Map<String, Object> ctx, Request<InitializeParams> request) {
        String rootUri = request.getParams().getRootUri();
        String originalRootUri = request.getParams().getOriginalRootUri();
        if (rootUri == null) {
            // Fallback to rootPath for older clients
            rootUri = request.getParams().getRootPath();
            if (rootUri != null && !rootUri.startsWith("file://")) {
                rootUri = "file://" + rootUri;
            }
        }
        if (originalRootUri == null) {
            originalRootUri = "NO_PATH";
        }
        final String finalRootUri = rootUri;
        final String finalOriginalRootUri = originalRootUri;
        Util.Timer t = Util.timeStart("initialize", "originalRootUri", originalRootUri);

        if (!vfs) {
            throw new RuntimeException("Running in non-VFS mode unsupported");
        }
        if (rootUri == null) {
            throw new RuntimeException("rootUri was not specified");
        }

        final List<WorkspaceConfigurationServersResult.Server> servers = new ArrayList<>();
        servers.addAll(getServers());
        InitializeParams.InitializationOptions initOpts = request.getParams().getInitializationOptions();
        if (initOpts != null && initOpts.getServers() != null) {
            servers.addAll(initOpts.getServers());
        }

        final String r = originalRootUri;
        synchronized(isInitializedMu) {
            if (this.isInitialized == null) {
                this.isInitialized = CompletableFuture.supplyAsync(() -> {
                    try {
                        FileContentProvider files = OverlayContentProvider.withOverlays(
                                new CachingFileContentProvider(controller),
                                finalRootUri,
                                controller
                        );
                        List<Workspace> workspaces = Workspaces.fromFiles(finalRootUri, files, controller, servers);
                        if (workspaces.size() == 0) {
                            log.warn("No workspaces detected in {}", r);
                        } else {
                            log.info("{} Workspaces detected in {}", workspaces.size(), r);
                        }
                        this.workspaceManager = new WorkspaceManager(workspaces, files);
                        this.languageService = new LanguageService(files, controller, workspaceManager);
                        return null;
                    } catch (Exception e) {
                        log.error("Initialization error for {}: {}", finalOriginalRootUri, e);
                        this.terminate();
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        Response<InitializeResult> response = new Response<InitializeResult>()
                .withId(request.getId())
                .withResult(new InitializeResult().withCapabilities(new ServerCapabilities()));
        controller.send(response);
        t.end();
        start = Util.timeStartQuiet("JavaLspHandlerService", "originalRootUri", originalRootUri);
    }

    @Override
    public void shutdown(Map<String, Object> ctx, Request<Void> request) {
        if (start != null) {
            start.end();
        } else {
            log.error("Received shutdown before initialize start time was set");
        }
        ensureReadyOrThrow("shutdown");
        receivedShutdownRequest.set(true);
        controller.send(new Response<Void>().withId(request.getId()));
        this.terminate();
    }

    /**
     * terminate closes the connection associated with this handler, and effectively shuts it down.
     */
    private void terminate() {
        controller.shutdown();
        workspaceManager = null;
        languageService = null;
    }

    @Override
    public void exit(Map<String, Object> ctx, Request<Void> request) {
        if (receivedShutdownRequest.get()) {
            log.trace("Exit");
        } else {
            log.error("Received `exit` before `shutdown`");
        }
    }

    @Override
    public void cancelRequest(Map<String, Object> ctx, Request<CancelParams> request) {
        // ignore
    }

    // Workspace method handlers

    @Override
    public void workspaceXReferences(Map<String, Object> ctx, Request<WorkspaceReferencesParams> request) {
        ensureReadyOrThrow("workspace/xreferences");
        List<ReferenceInformation> refs;
        try {
            refs = languageService.xReferences(request.getParams(), request.getId(), ctx);
        } catch (Exception e) {
            log.error("Error on workspace/xreferences: {}", e);
            refs = Collections.emptyList();
        }
        Response<List<ReferenceInformation>> response = new Response<List<ReferenceInformation>>()
                .withResult(refs)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void workspaceSymbol(Map<String, Object> ctx, Request<WorkspaceSymbolParams> request) {
        ensureReadyOrThrow("workspace/symbol");
        List<SymbolInformation> symbols;
        try {
            symbols = languageService.workspaceSymbol(request.getParams(), ctx);
        } catch (Exception e) {
            log.error("Error on workspace/symbol: {}", e);
            symbols = Collections.emptyList();
        }
        Response<List<SymbolInformation>> response = new Response<List<SymbolInformation>>()
                .withResult(symbols)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void workspaceXPackages(Map<String, Object> ctx, Request<Void> request) {
        ensureReadyOrThrow("workspace/xpackages");
        List<PackageInformation> pkgInfo = languageService.xPackages(ctx);
        Response<List<PackageInformation>> response = new Response<List<PackageInformation>>()
                .withResult(pkgInfo)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void workspaceXDependencies(Map<String, Object> ctx, Request<Void> request) {
        ensureReadyOrThrow("workspace/xdependencies");
        List<DependencyReference> deps = languageService.xDependencies(ctx);
        Response<List<DependencyReference>> response = new Response<List<DependencyReference>>()
                .withResult(deps)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void workspaceFiles(Map<String, Object> ctx, Request<WorkspaceFilesParams> request) {
        // not expecting this request from the client
        // TODO: send dummy response
    }

    // Document method handlers

    @Override
    public void textDocumentDidClose(Map<String, Object> ctx, Request<DidCloseTextDocumentParams> request) {
        // not yet implemented
    }

    @Override
    public void textDocumentDidOpen(Map<String, Object> ctx, Request<DidOpenTextDocumentParams> request) {
        // not yet implemented
    }

    @Override
    public void textDocumentHover(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        ensureReadyOrThrow("textDocument/hover");
        Hover hover = languageService.hover(request.getParams(), ctx);
        Response<Hover> response = new Response<Hover>()
                .withResult(hover)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void textDocumentReferences(Map<String, Object> ctx, Request<ReferenceParams> request) {
        ensureReadyOrThrow("textDocument/references");
        List<Location> locations;
        try {
            // TODO: check the client capabilities flag once it's being passed in
            locations = languageService.references(request.getParams(), request.getId(), ctx);
        } catch (Exception e) {
            log.warn("Exception while collecting references: {}", e);
            locations = Collections.emptyList();
        }

        Response<List<Location>> response = new Response<List<Location>>()
                .withResult(locations)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void textDocumentDocumentSymbol(Map<String, Object> ctx, Request<DocumentSymbolParams> request) {
        ensureReadyOrThrow("textDocument/symbols");
        List<SymbolInformation> symbols = languageService.documentSymbol(request.getParams(), ctx);
        Response<List<SymbolInformation>> response = new Response<List<SymbolInformation>>()
                .withResult(symbols)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void textDocumentDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        ensureReadyOrThrow("textDocument/definition");
        List<Location> locations = languageService.definition(request.getParams(), ctx);
        Response<List<Location>> response = new Response<List<Location>>()
                .withResult(locations)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void textDocumentXDefinition(Map<String, Object> ctx, Request<TextDocumentPositionParams> request) {
        ensureReadyOrThrow("textDocument/xdefinition");
        List<SymbolLocationInformation> symbols = languageService.xDefinition(request.getParams(), ctx);
        Response<List<SymbolLocationInformation>> response = new Response<List<SymbolLocationInformation>>()
                .withResult(symbols)
                .withId(request.getId());
        controller.send(response);
    }

    @Override
    public void textDocumentContent(Map<String, Object> ctx, Request<TextDocumentContentParams> request) {
        // not expecting this request from the client
        // TODO: send dummy response
    }

    private void ensureReadyOrThrow(String requestMethod) {
        if (isInitialized == null) {
            throw new RuntimeException("Received LSP request " + requestMethod + " before initialize call");
        }
        try {
            isInitialized.get();
        } catch (Exception e) {
            throw new RuntimeException("Received LSP request " + requestMethod + " after initialization error: " + e.getMessage());
        }
        if (receivedShutdownRequest.get()) {
            throw new RuntimeException("Received additional LSP requests (other than `exit`) after receiving a shutdown request");
        }
    }

    /**
     * getServers fetches Maven servers via workspace/configuration request
     */
    private List<WorkspaceConfigurationServersResult.Server> getServers() {
        try {
            WorkspaceConfigurationParams configParams = new WorkspaceConfigurationParams();
            List<WorkspaceConfigurationParams.Item> items = Lists.newArrayList(
                    new WorkspaceConfigurationParams.Item(null, "java.servers")
            );
            configParams.setItems(items);
            Request<WorkspaceConfigurationParams> configReq = new Request<WorkspaceConfigurationParams>()
                    .withMethod(Method.WORKSPACE_CONFIG)
                    .withParams(configParams)
                    .withId(Controller.generateId());
            Response<WorkspaceConfigurationServersResult> configResult =
                    controller.sendBlockingRequest(configReq, WorkspaceConfigurationServersResult.class);
            ArrayList<WorkspaceConfigurationServersResult.Server> servers = null;
            if (configResult.getResult() != null && configResult.getResult().size() > 0) {
                servers = configResult.getResult().get(0);
            }
            if (servers == null) {
                servers = new ArrayList<>();
            }
            servers.addAll(getServersFromEnv());
            return servers;
        } catch (Exception e) {
            log.error("Fetching servers via workspace/configuration failed: {}", e);
            return new ArrayList<>();
        }
    }

    private List<WorkspaceConfigurationServersResult.Server> getServersFromEnv() {
        if (Config.PRIVATE_REPO_ID == null) {
            return new ArrayList<>();
        }
        WorkspaceConfigurationServersResult.Server s = new WorkspaceConfigurationServersResult.Server();
        s.setId(Config.PRIVATE_REPO_ID);
        s.setUsername(Config.PRIVATE_REPO_USERNAME);
        s.setPassword(Config.PRIVATE_REPO_PASSWORD);
        s.setDefaultUrl(Config.PRIVATE_REPO_URL);
        return Lists.newArrayList(s);
    }

    @Override
    protected void finalize() throws Throwable {
        log.trace("Garbage collecting LSP message handler for controller {}", controller != null ? controller.getId() : "[unknown]");
        super.finalize();
    }
}
