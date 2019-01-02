package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.files.RemoteFileContentProvider;
import com.sourcegraph.langserver.langservice.workspace.Workspace;
import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.langserver.langservice.workspace.Workspaces;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.LSPConnection;
import com.sourcegraph.lsp.NoopMessenger;
import com.sourcegraph.lsp.domain.Mapper;
import com.sourcegraph.lsp.domain.Method;
import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.Response;
import com.sourcegraph.lsp.domain.params.InitializeParams;
import com.sourcegraph.lsp.domain.result.InitializeResult;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.ServerCapabilities;
import com.sourcegraph.lsp.jsonrpc.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LanguageService2 is the core of the language server. It defines LSP endpoint methods and maintains
 * all internal compiler and build state. It is threadsafe.
 */
public class LanguageService2 {

    private static Logger log = LoggerFactory.getLogger(LanguageService2.class);

    private String remoteRootURI;

    private WorkspaceManager workspaceManager;

    private CompilerService compiler;

    private FileContentProvider files;

    private LSPConnection lspConn;

    public LanguageService2(LSPConnection lspConn) {
        this.lspConn = lspConn;
    }

    // TODO: add connection later
    public void dispatch(Message message) {
        try {
            Method method = Method.fromString(message.getMethod());
            switch (method) {
                case INITIALIZE:
                    Request<InitializeParams> req = Mapper.convertMessageToRequest(message, InitializeParams.class);
                    InitializeResult result = initialize(req.getParams());
                    lspConn.send(new Response<InitializeResult>().withId(req.getId()).withResult(result));
                    break;
//            case SHUTDOWN:
//                handleRequest(messageHandlers::shutdown, Void.class, message);
//                break;
//            case EXIT:
//                handleRequest(messageHandlers::exit, Void.class, message);
//                break;
//            case CANCEL_REQUEST:
//                handleRequest(messageHandlers::cancelRequest, CancelParams.class, message);
//                break;
//            case WORKSPACE_SYMBOL:
//                handleRequest(messageHandlers::workspaceSymbol, WorkspaceSymbolParams.class, message);
//                break;
//            case WORKSPACE_XPACKAGES:
//                handleRequest(messageHandlers::workspaceXPackages, Void.class, message);
//                break;
//            case WORKSPACE_XDEPENDENCIES:
//                handleRequest(messageHandlers::workspaceXDependencies, Void.class, message);
//                break;
//            case WORKSPACE_XREFERENCES:
//                handleRequest(messageHandlers::workspaceXReferences, WorkspaceReferencesParams.class, message);
//                break;
//            case WORKSPACE_FILES:
//                handleRequest(messageHandlers::workspaceFiles, WorkspaceFilesParams.class, message);
//                break;
//            case TEXT_DOCUMENT_DID_CLOSE:
//                handleRequest(messageHandlers::textDocumentDidClose, DidCloseTextDocumentParams.class, message);
//                break;
//            case TEXT_DOCUMENT_DID_OPEN:
//                handleRequest(messageHandlers::textDocumentDidOpen, DidOpenTextDocumentParams.class, message);
//                break;
            case TEXT_DOCUMENT_HOVER:
                System.out.println("# HOVER");
//                handleRequest(messageHandlers::textDocumentHover, TextDocumentPositionParams.class, message);
                break;
//            case TEXT_DOCUMENT_REFERENCES:
//                handleRequest(messageHandlers::textDocumentReferences, ReferenceParams.class, message);
//                break;
//            case TEXT_DOCUMENT_DOCUMENT_SYMBOL:
//                handleRequest(messageHandlers::textDocumentDocumentSymbol, DocumentSymbolParams.class, message);
//                break;
//            case TEXT_DOCUMENT_DEFINITION:
//                handleRequest(messageHandlers::textDocumentDefinition, TextDocumentPositionParams.class, message);
//                break;
//            case TEXT_DOCUMENT_XDEFINITION:
//                handleRequest(messageHandlers::textDocumentXDefinition, TextDocumentPositionParams.class, message);
//                break;
//            case TEXT_DOCUMENT_CONTENT:
//                handleRequest(messageHandlers::textDocumentContent, TextDocumentContentParams.class, message);
//                break;
//            case PARTIAL_RESULT: // neither the mock client nor the server use these, so just drop them
//                break;
                case UNKNOWN:
                default:
                    handleUnknownMethod(message);
                    break;
            }
        } catch (Exception e) {
            // TODO(beyang)
            throw new RuntimeException(e);
        }
    }

    public void handleUnknownMethod(Message message) {
        // TODO
    }

    public InitializeResult initialize(InitializeParams p) throws Exception {
        remoteRootURI = p.getRootUri();

        // TODO(beyang): get cache container from params
        File cacheRoot = new File("/tmp/eclipse.jdt.ls.cache");
        cacheRoot.mkdirs();

        String fakeRootURI = "file:///";
        FileContentProvider files = new RemoteFileContentProvider(remoteRootURI, cacheRoot);
        // TODO(beyang): keep?
        ArrayList<WorkspaceConfigurationServersResult.Server> servers = new ArrayList<>();
        List<Workspace> workspaces = Workspaces.fromFiles(fakeRootURI, files, new NoopMessenger(), servers);
        WorkspaceManager workspaceManager = new WorkspaceManager(workspaces, files);

        this.workspaceManager = workspaceManager;
        this.compiler = new CompilerService(workspaceManager);
        this.files = files;

        return new InitializeResult().withCapabilities(new ServerCapabilities());
    }

//    public

}
