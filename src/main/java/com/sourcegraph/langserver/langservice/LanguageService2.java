package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.compiler.LanguageData;
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
import com.sourcegraph.lsp.domain.params.TextDocumentPositionParams;
import com.sourcegraph.lsp.domain.result.InitializeResult;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.Hover;
import com.sourcegraph.lsp.domain.structures.MarkedString;
import com.sourcegraph.lsp.domain.structures.Position;
import com.sourcegraph.lsp.domain.structures.ServerCapabilities;
import com.sourcegraph.lsp.jsonrpc.Message;
import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.utils.Util;
import com.sun.source.util.TreePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LanguageService2 is the core of the language server. It defines LSP endpoint methods and maintains
 * all internal compiler and build state. It is threadsafe.
 */
public class LanguageService2 {

    private static Logger log = LoggerFactory.getLogger(LanguageService2.class);

    private String remoteRootURI;

    private WorkspaceManager workspaceManager;

    private CompilerService compilerService;

    private FileContentProvider files;

    private LSPConnection lspConn;

    public LanguageService2(LSPConnection lspConn) {
        this.lspConn = lspConn;
    }

    private String convertURI(String remoteURI) {
        try {
            URL remoteRoot = new URL(remoteRootURI);
            URL remote = new URL(remoteURI);
            if (!remoteRoot.getHost().equals(remote.getHost())) {
                throw new Exception("bad remoteURI");
            }
            if (!remote.getPath().startsWith(remoteRoot.getPath())) {
                throw new Exception("bad remoteURI");
            }
            return "file:///" + remote.getPath().substring(remoteRoot.getPath().length());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: add connection later
    public void dispatch(Message message) {
        Map<String, Object> ctx = new ConcurrentHashMap<>();

        try {
            Method method = Method.fromString(message.getMethod());
            switch (method) {
                case INITIALIZE:
                    Request<InitializeParams> initReq = Mapper.convertMessageToRequest(message, InitializeParams.class);
                    InitializeResult result = initialize(initReq.getParams());
                    lspConn.send(new Response<InitializeResult>().withId(initReq.getId()).withResult(result));
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
                Request<TextDocumentPositionParams> hoverReq = Mapper.convertMessageToRequest(message, TextDocumentPositionParams.class);
                hoverReq.getParams().getTextDocument().setUri(
                        convertURI(hoverReq.getParams().getTextDocument().getUri())
                );
                Hover hover = hover(hoverReq.getParams(), ctx);
                lspConn.send(new Response<Hover>()
                        .withId(hoverReq.getId())
                        .withResult(hover));
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
        this.compilerService = new CompilerService(workspaceManager);
        this.files = files;

        return new InitializeResult().withCapabilities(new ServerCapabilities());
    }

    public Hover hover(TextDocumentPositionParams textDocumentPosition, Map<String, Object> ctx) {
        Util.Timer t = Util.timeStartQuiet("textDocument/hover");
        Workspace workspace = workspaceManager.getWorkspaceContainingUri(textDocumentPosition.getTextDocument().getUri());
        Optional<LanguageData> shallowHover = findHover(textDocumentPosition, ctx);
        Optional<LanguageData> deepHover = shallowHover.flatMap(h -> getDefinitionFromHover(h, workspace, ctx));
        List<MarkedString> hoverContents = deepHover.map(Optional::of)
                .orElse(shallowHover)
                .map(LanguageData::getData)
                .orElse(Collections.emptyList());
        t.end();
        return new Hover().withContents(hoverContents);
    }


    private Optional<LanguageData> findHover(TextDocumentPositionParams textDocumentPosition, Map<String, Object> ctx) {
        return findHover(textDocumentPosition.getTextDocument().getUri(), textDocumentPosition.getPosition(), ctx);
    }

    private Optional<LanguageData> findHover(String uri, Position position, Map<String, Object> ctx) {
        return compilerService
                .analyze(uri, ctx)
                .flatMap(compilationResult -> compilationResult.findHover(position));
    }

    private Optional<LanguageData> getDefinitionFromHover(LanguageData hoverData, Workspace workspace, Map<String, Object> ctx) {
        try {
            Element refElement = hoverData.getElement();
            Element defContainer = LanguageUtils.getTopLevelClass(refElement);
            if (defContainer == null) return Optional.empty();
            String defName = LanguageUtils.getQualifiedName(defContainer);

            if (LanguageUtils.isTopLevel(defContainer.getKind())) {
                return compilerService.getDeclaredType(defName)
                        .flatMap(defResult -> defResult.findDefinition(refElement, hoverData.getTypeMirror()));
            } else {
                TreePath defTreePath = workspace.getCompiler().getTrees().getPath(defContainer);
                if (defTreePath == null) return Optional.empty();
                return compilerService
                        .analyze(defTreePath.getCompilationUnit().getSourceFile(), workspace, ctx)
                        .flatMap(defResult -> defResult.findDefinition(defContainer, refElement, hoverData.getTypeMirror()));
            }
        } catch (Exception e) {
            log.error("Error searching for definition");
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
