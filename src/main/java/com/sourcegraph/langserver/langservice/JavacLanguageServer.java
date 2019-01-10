package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.compiler.CompilationResult;
import com.sourcegraph.langserver.langservice.compiler.LanguageData;
import com.sourcegraph.langserver.langservice.filters.ReferenceFilterUtils;
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
import javax.tools.JavaFileObject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A javac-based implementation of LanguageServer.
 */
public class JavacLanguageServer implements LanguageServer, WorkspaceService, TextDocumentService, LanguageClientAware {

    private static Logger log = LoggerFactory.getLogger(JavacLanguageServer.class);

    /**
     * Fields set in constructor
     */
    private Function<String, FileContentProvider> filesProvider;

    /**
     * Fields set on initialization.
     */

    private String remoteRootURI;

    private WorkspaceManager workspaceManager;

    private CompilerService compilerService;

    /**
     * Other fields (may be null)
     */
    private LanguageClient client;


    public JavacLanguageServer(Function<String, FileContentProvider> filesProvider) {
        this.filesProvider = filesProvider;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams p) {
        remoteRootURI = p.getRootUri();

        try {
            FileContentProvider files = filesProvider.apply(remoteRootURI);
            ArrayList<WorkspaceConfigurationServersResult.Server> servers = new ArrayList<>();
            List<Workspace> workspaces = Workspaces.fromFiles(remoteRootURI, files, new NoopMessenger(), servers);
            WorkspaceManager workspaceManager = new WorkspaceManager(workspaces, files);

            this.workspaceManager = workspaceManager;
            this.compilerService = new CompilerService(workspaceManager);

            return CompletableFuture.completedFuture(new InitializeResult(new ServerCapabilities()));
        } catch (Exception e) {
            CompletableFuture<InitializeResult> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {}

    private static com.sourcegraph.lsp.domain.params.TextDocumentPositionParams toLegacyTextDocumentPositionParams(TextDocumentPositionParams p) {
        return new com.sourcegraph.lsp.domain.params.TextDocumentPositionParams()
                .withPosition(toLegacyPosition(p.getPosition()))
                .withTextDocument(toLegacyTextDocumentIdentifier(p.getTextDocument()));
    }

    private static com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier toLegacyTextDocumentIdentifier(TextDocumentIdentifier t) {
        return com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier.of(t.getUri());
    }

    private static com.sourcegraph.lsp.domain.structures.Position toLegacyPosition(Position p) {
        return com.sourcegraph.lsp.domain.structures.Position.of(p.getLine(), p.getCharacter());
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

    private static com.sourcegraph.lsp.domain.params.ReferenceParams toLegacyReferenceParams(ReferenceParams p) {
        return new com.sourcegraph.lsp.domain.params.ReferenceParams()
                .withContext(toLegacyReferenceContext(p.getContext()))
                .withPosition(toLegacyPosition(p.getPosition()))
                .withTextDocument(toLegacyTextDocumentIdentifier(p.getTextDocument()));
    }

    private static com.sourcegraph.lsp.domain.structures.ReferenceContext toLegacyReferenceContext(ReferenceContext c) {
        return new com.sourcegraph.lsp.domain.structures.ReferenceContext()
                .withIncludeDeclaration(c.isIncludeDeclaration());
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

    private class ReferencesFilter implements Predicate<LanguageData> {

        private com.sourcegraph.lsp.domain.structures.Location definition;

        /**
         * Matches all candidates
         */
        ReferencesFilter() {
        }

        /**
         * Matches candidates with the same location
         *
         * @param sample    hover data to extract definition from
         * @param workspace workspace manager to get compiler options
         */
        ReferencesFilter(LanguageData sample, Workspace workspace, Map<String, Object> ctx) {
            Optional<LanguageData> definition = getDefinitionFromHover(sample, workspace, ctx);
            definition.ifPresent(languageData -> this.definition = languageData.getLocation());
        }

        @Override
        public boolean test(LanguageData candidate) {
            if (this.definition == null) {
                return true;
            }
            com.sourcegraph.lsp.domain.structures.Location location = candidate.getLocation();
            return location == null || !definition.equals(location);
        }
    }

    // The front-end doesn't seem to be sending the new xlimit parameter, so let's cap it here too so that we don't
    // do unnecessary work.
    private static int REFERENCES_LIMIT = 200;


    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams p) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doReferences(p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<? extends Location> doReferences(ReferenceParams origParams) throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        com.sourcegraph.lsp.domain.params.ReferenceParams params = toLegacyReferenceParams(origParams);

        Optional<LanguageData> hover = findHover(params.getTextDocument().getUri(), params.getPosition(), ctx);

        if (!hover.isPresent()) {
            return Collections.emptyList();
        }
        LanguageData hoverData = hover.get();

        com.sourcegraph.lsp.domain.structures.ReferenceContext refCtx = params.getContext();
        boolean includeDeclaration = refCtx != null && refCtx.isIncludeDeclaration();
        ReferencesFilter defaultReferencesFilter = new ReferencesFilter();

        ArrayList<com.sourcegraph.lsp.domain.structures.Location> accumulator = new ArrayList<>();
        int limit = refCtx != null && refCtx.getXlimit() != null ? refCtx.getXlimit() : REFERENCES_LIMIT;

        // note that these loops can be interrupted; the intention is that we cancel any long-running computations if
        // the session is shut down
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            if (Thread.currentThread().isInterrupted()) {
                return Collections.emptyList();
            }
            // excludes or not declaration from the list of references
            ReferencesFilter filterDefinitions = !includeDeclaration
                    ? new ReferencesFilter(hoverData, workspace, ctx)
                    : defaultReferencesFilter;
            Optional<LanguageData> def = getDefinitionFromHover(hoverData, workspace, ctx);
            Predicate<JavaFileObject> referencesScopeFilter;
            // If we found a reference's definition element we may reduce search scope by leaving
            // only same file or files from the same package if our target element has reduced visibility.
            // For example, if we are looking for local variable there is no reason to process all the files;
            // one is enough. Another case is when target element has package level access
            // Fallback to include all the files
            referencesScopeFilter = def
                    .map(defData -> ReferenceFilterUtils.getFilter(hoverData.getElement(), defData.getFileName()))
                    .orElse(__ -> true);

            for (String uri : workspace.getSourceUris()) {
                if (Thread.currentThread().isInterrupted()) {
                    return Collections.emptyList();
                }
                if (accumulator.size() >= limit) {
                    break;
                }
                JavaFileObject sourceFile = workspace.getSourceFile(uri);
                // is there a chance to find ref there based on visibility?
                if (!referencesScopeFilter.test(sourceFile)) {
                    continue;
                }
                Optional<CompilationResult> optionalResult = compilerService.parse(sourceFile, workspace.getCompiler());
                if (!optionalResult.isPresent()) {
                    continue;
                }
                CompilationResult compilationResult = optionalResult.get();
                // quick search for element in a tree
                if (!compilationResult.containsExactSymbol(hoverData.getElement())) {
                    continue;
                }
                optionalResult = compilerService.analyze(sourceFile, workspace, ctx);
                if (!optionalResult.isPresent()) {
                    continue;
                }
                compilationResult = optionalResult.get();
                List<LanguageData> someReferences = compilationResult.findReferences(hoverData).stream()
                        .filter(filterDefinitions) // exclude declaration if needed
                        .collect(Collectors.toCollection(ArrayList::new));
                if (someReferences.isEmpty()) {
                    continue;
                }
                // stream partial results or not, depending on request id
                List<LanguageData> streamedReferences = someReferences;
                for (LanguageData streamedReference : streamedReferences) {
                    accumulator.add(streamedReference.getLocation());
                }
            }
        }

        accumulator.sort((l1, l2) -> { // stable ordering
            int a = l1.getUri().compareTo(l2.getUri());
            if (a != 0) {
                return a;
            }
            int b = l1.getRange().getStart().getLine() - l2.getRange().getStart().getLine();
            if (b != 0) {
                return b;
            }
            int c = l1.getRange().getStart().getCharacter() - l2.getRange().getStart().getCharacter();
            if (c != 0) {
                return c;
            }
            int d = l1.getRange().getEnd().getLine() - l2.getRange().getEnd().getLine();
            if (d != 0) {
                return d;
            }
            return l1.getRange().getEnd().getCharacter() - l2.getRange().getEnd().getCharacter();
        });

        return accumulator.stream().map(JavacLanguageServer::fromLegacyLocation).collect(Collectors.toList());
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
