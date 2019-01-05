package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.compiler.LanguageData;
import com.sourcegraph.langserver.langservice.files.RemoteFileContentProvider;
import com.sourcegraph.langserver.langservice.workspace.Workspace;
import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.langserver.langservice.workspace.Workspaces;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.NoopMessenger;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.utils.Util;
import com.sun.source.util.TreePath;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class JavacLanguageServer implements LanguageServer, WorkspaceService, TextDocumentService, LanguageClientAware {

    private static Logger log = LoggerFactory.getLogger(JavacLanguageServer.class);

    private String remoteRootURI;

    private WorkspaceManager workspaceManager;

    private CompilerService compilerService;

    private FileContentProvider files;

    private LanguageClient client;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams p) {
        remoteRootURI = p.getRootUri();

        try {
            // TODO(beyang): get cache container from params
            File cacheRoot = new File("/tmp/eclipse.jdt.ls.cache");
            cacheRoot.mkdirs();

            FileContentProvider files = new RemoteFileContentProvider(remoteRootURI, cacheRoot);
            // TODO(beyang): keep?
            ArrayList<WorkspaceConfigurationServersResult.Server> servers = new ArrayList<>();
            List<Workspace> workspaces = Workspaces.fromFiles(remoteRootURI, files, new NoopMessenger(), servers);
            WorkspaceManager workspaceManager = new WorkspaceManager(workspaces, files);

            this.workspaceManager = workspaceManager;
            this.compilerService = new CompilerService(workspaceManager);
            this.files = files;

            return CompletableFuture.completedFuture(new InitializeResult(new ServerCapabilities()));
        } catch (Exception e) {
            CompletableFuture<InitializeResult> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    private static com.sourcegraph.lsp.domain.params.TextDocumentPositionParams toLegacyTextDocumentPositionParams(TextDocumentPositionParams p) {
        return new com.sourcegraph.lsp.domain.params.TextDocumentPositionParams()
                .withPosition(com.sourcegraph.lsp.domain.structures.Position.of(p.getPosition().getLine(), p.getPosition().getCharacter()))
                .withTextDocument(com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier.of(p.getTextDocument().getUri()));
    }

    private static Location fromLegacyLocation(com.sourcegraph.lsp.domain.structures.Location l) {
        Location l2 = new Location();
        l2.setRange(fromLegacyRange(l.getRange()));
        l2.setUri(l.getUri());
        return l2;
    }

    private static Range fromLegacyRange(com.sourcegraph.lsp.domain.structures.Range r) {
        return new Range(fromLegacyPosition(r.getStart()), fromLegacyPosition(r.getEnd()));
    }

    private static Position fromLegacyPosition(com.sourcegraph.lsp.domain.structures.Position p) {
        return new Position(p.getLine(), p.getCharacter());
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
        Util.Timer t = Util.timeStartQuiet("textDocument/hover");
        Workspace workspace = workspaceManager.getWorkspaceContainingUri(position.getTextDocument().getUri());

        // TODO(beyang): remove ctx param from all methods
        Map<String, Object> ctx = new HashMap<>();
        Optional<LanguageData> shallowHover = findHover(toLegacyTextDocumentPositionParams(position), ctx);
        Optional<LanguageData> deepHover = shallowHover.flatMap(h -> getDefinitionFromHover(h, workspace, ctx));
        List<com.sourcegraph.lsp.domain.structures.MarkedString> legacyHoverContents = deepHover.map(Optional::of)
                .orElse(shallowHover)
                .map(LanguageData::getData)
                .orElse(Collections.emptyList());
        t.end();

        List<Either<String, MarkedString>> hoverContents = legacyHoverContents.stream()
                .map(s -> Either.<String, MarkedString>forRight(new MarkedString(s.getLanguage(), s.getValue())))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(new Hover(hoverContents));
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams p) {
        Util.Timer t = Util.timeStartQuiet("textDocument/definition");
        Workspace workspace = workspaceManager.getWorkspaceContainingUri(p.getTextDocument().getUri());
        Map<String, Object> ctx = new HashMap<>();
        List<Location> locations = findHover(toLegacyTextDocumentPositionParams(p), ctx)
                .flatMap(h -> getDefinitionFromHover(h, workspace, ctx))
                .map(LanguageData::getLocation)
                .map(JavacLanguageServer::fromLegacyLocation)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        t.end();
        return CompletableFuture.completedFuture(locations);
    }

    private Optional<LanguageData> findHover(com.sourcegraph.lsp.domain.params.TextDocumentPositionParams textDocumentPosition, Map<String, Object> ctx) {
        return findHover(textDocumentPosition.getTextDocument().getUri(), textDocumentPosition.getPosition(), ctx);
    }

    private Optional<LanguageData> findHover(String uri, com.sourcegraph.lsp.domain.structures.Position position, Map<String, Object> ctx) {
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

    @Override
    public CompletableFuture<Object> shutdown() {
        return null;
    }

    @Override
    public void exit() {

    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {

    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {

    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {

    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }
}
