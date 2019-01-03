package com.sourcegraph.langserver.langservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sourcegraph.langserver.langservice.compiler.CompilationResult;
import com.sourcegraph.langserver.langservice.compiler.LanguageData;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.langserver.langservice.filters.ReferenceFilterUtils;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibraries;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibrary;
import com.sourcegraph.langserver.langservice.workspace.Workspace;
import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.lsp.PartialResultStreamer;
import com.sourcegraph.lsp.Tracing;
import com.sourcegraph.lsp.domain.params.*;
import com.sourcegraph.lsp.domain.structures.*;
import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.utils.Util;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import io.opentracing.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LanguageService {

    private static Logger log = LoggerFactory.getLogger(LanguageService.class);

    // The front-end doesn't seem to be sending the new xlimit parameter, so let's cap it here too so that we don't
    // do unnecessary work.
    private static int REFERENCES_LIMIT = 200;

    private WorkspaceManager workspaceManager;

    private CompilerService compilerService;

    private FileContentProvider fileContentProvider;

    private PartialResultStreamer partialResultStreamer;

    public LanguageService(FileContentProvider fileProvider, PartialResultStreamer partialResultStreamer, WorkspaceManager workspaceManager) {
        this.fileContentProvider = fileProvider;
        this.workspaceManager = workspaceManager;
        this.compilerService = new CompilerService(workspaceManager);
        this.partialResultStreamer = partialResultStreamer;
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

    public List<Location> definition(TextDocumentPositionParams textDocumentPosition, Map<String, Object> ctx) {
        Util.Timer t = Util.timeStartQuiet("textDocument/definition");
        Workspace workspace = workspaceManager.getWorkspaceContainingUri(textDocumentPosition.getTextDocument().getUri());
        List<Location> locations = findHover(textDocumentPosition, ctx)
                .flatMap(h -> getDefinitionFromHover(h, workspace, ctx))
                .map(LanguageData::getLocation)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        t.end();
        return locations;
    }

    // TODO(aaron): remove these logs once the x-def flakiness has been debugged
    private ObjectWriter writer = new ObjectMapper().writer();

    public List<SymbolLocationInformation> xDefinition(TextDocumentPositionParams textDocumentPosition, Map<String, Object> ctx) {
        Util.Timer t;
        try {
            t = Util.timeStart("workspace/xdefinition", "params", writer.writeValueAsString(textDocumentPosition));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Workspace workspace = workspaceManager.getWorkspaceContainingUri(textDocumentPosition.getTextDocument().getUri());

        List<SymbolLocationInformation> symbolLocations = this.findHover(textDocumentPosition, ctx)
                .map((hover) -> {
                    Optional<LanguageData> def = this.getDefinitionFromHover(hover, workspace, ctx);
                    Location location = null;
                    if (def.isPresent()) {
                        location = def.get().getLocation();
                    }
                    SymbolDescriptor symbolDescriptor = null;
                    if (hover.getElement() instanceof Symbol) {
                        Symbol sym = (Symbol) hover.getElement();
                        JavaFileObject fileObject = getSymbolFileObject(sym);
                        if (fileObject != null) {
                            PackageIdentifier packageIdentifier = null;
                            for (StandardLibrary library : StandardLibraries.getInstance().getLibraries()) {
                                if (library.matches(fileObject, sym)) {
                                    packageIdentifier = library.getPackageIdentifier();
                                    break;
                                }
                            }
                            URI fileUri = fileObject.toUri();
                            if (packageIdentifier == null) {
                                packageIdentifier = workspace.getArtifactIdentifier(fileUri);
                            }
                            if (packageIdentifier == null) {
                                packageIdentifier = workspaceManager.getArtifactorIdentifier(fileUri);
                            }
                            if (location != null) {
                                // this is an internal symbol, so get the default fully-analyzed signature
                                symbolDescriptor = SymbolDescriptor.of(hover.getSignature(), packageIdentifier);
                            } else {
                                // this is an external symbol, so get a simpler signature that matches workspace/symbol
                                symbolDescriptor = SymbolDescriptor.of(hover.getCrossRepoSignature(), packageIdentifier);
                            }
                        }
                    }
                    if (symbolDescriptor != null || location != null) {
                        return SymbolLocationInformation.of(location, symbolDescriptor);
                    }
                    return null;
                })
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());

        t.end("numResults", symbolLocations.size());
        return symbolLocations;
    }


    public List<Location> references(ReferenceParams referenceParams, Map<String, Object> ctx) throws Exception {
        return references(referenceParams, null, ctx);
    }

    public List<Location> references(ReferenceParams referenceParams, Object requestId, Map<String, Object> ctx) throws Exception {
        Util.Timer t = Util.timeStart("workspace/references");

        Optional<LanguageData> hover = findHover(referenceParams.getTextDocument().getUri(), referenceParams.getPosition(), ctx);

        if (!hover.isPresent()) {
            return Collections.emptyList();
        }
        LanguageData hoverData = hover.get();

        ReferenceContext refCtx = referenceParams.getContext();
        boolean includeDeclaration = refCtx != null && refCtx.isIncludeDeclaration();
        ReferencesFilter defaultReferencesFilter = new ReferencesFilter();

        ArrayList<Location> accumulator = new ArrayList<>();
        int limit = refCtx != null && refCtx.getXlimit() != null ? refCtx.getXlimit() : REFERENCES_LIMIT;

        // if there's no request id, then apply the identity function to each partial result, otherwise apply
        // this::streamPartialResults
        Function<List<LanguageData>, List<LanguageData>> streamingFunction;
        if (requestId == null) {
            streamingFunction = x -> x;
        } else {
            LongAdder counter = new LongAdder();

            // initialize the streaming results if we plan on sending any
            JsonPatch streamingInitPatch = new JsonPatch();
            streamingInitPatch.add(JsonPatchOperation.of("add", "", new ArrayList()));
            partialResultStreamer.sendPartialResult(requestId, streamingInitPatch);

            streamingFunction = someReferences -> streamPartialReferences(
                    someReferences,
                    requestId,
                    counter,
                    limit
            );
        }

        // note that these loops can be interrupted; the intention is that we cancel any long-running computations if
        // the session is shut down
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            t.debug("workspace/references starting workspace", "workspace", workspace.getRootURI());
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
                List<LanguageData> streamedReferences = streamingFunction.apply(someReferences);
                for (LanguageData streamedReference : streamedReferences) {
                    accumulator.add(streamedReference.getLocation());
                }
            }
            t.log("workspace/references finished workspace", "workspace", workspace.getRootURI());
        }

        // we can't sort streaming refs, so only sort if they're non-streaming
        if (requestId == null) {
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
        }
        t.end();
        return accumulator;
    }

    private List<LanguageData> streamPartialReferences(
            List<LanguageData> partialResults,
            Object requestId,
            LongAdder counter,
            int limit
    ) {

        if (partialResults.isEmpty()) return partialResults;
        if (counter.intValue() >= limit) return Collections.emptyList();

        int remaining = limit - counter.intValue();
        if (remaining < 0) {
            remaining = 0;
        }

        if (remaining < partialResults.size()) {
            partialResults = partialResults.subList(0, remaining);
        }

        JsonPatch patch = partialResults.stream()
                .map(LanguageData::getLocation)
                .map(location -> JsonPatchOperation.of("add", "/-", location))
                .collect(Collectors.toCollection(JsonPatch::new));

        if (!patch.isEmpty()) {
            counter.add(patch.size());
            partialResultStreamer.sendPartialResult(requestId, patch);
        }

        return partialResults;
    }

    private JavaFileObject getSymbolFileObject(Symbol sym) {
        for (Symbol n = sym; n != null; n = n.owner) {
            if (n instanceof Symbol.ClassSymbol) {
                JavaFileObject classfile = ((Symbol.ClassSymbol) n).classfile;
                if (classfile != null) {
                    return classfile;
                }
                return ((Symbol.ClassSymbol) n).sourcefile;
            } else if (n instanceof Symbol.PackageSymbol) {
                Symbol.PackageSymbol psn = (Symbol.PackageSymbol) n;
                Symbol.ClassSymbol package_info = psn.package_info;
                if (package_info != null) {
                    JavaFileObject classfile = package_info.classfile;
                    if (classfile != null) {
                        return classfile;
                    }
                    return ((Symbol.PackageSymbol) n).package_info.sourcefile;
                }
            }
        }
        return null;
    }

    public List<SymbolInformation> workspaceSymbol(WorkspaceSymbolParams params, Map<String, Object> ctx) throws Exception {
        Util.Timer t = Util.timeStart("workspace/symbol");
        SymbolDescriptor symbolQuery = params.getSymbol();
        String textQuery = params.getQuery();

        if (symbolQuery == null && textQuery == null) {
            return Collections.emptyList();
        }

        // Set the limit on the number of results we'll return
        int limit;
        if (symbolQuery != null) {
            limit = 3;
        } else if (textQuery.length() > 4) {
            limit = 10;
        } else {
            limit = 5;
        }

        final String simpleQuery = (symbolQuery != null) ? symbolQuery.getSimpleName() : textQuery;
        if (simpleQuery == null || simpleQuery.isEmpty()) return Collections.emptyList();

        long before = System.currentTimeMillis();

        if (symbolQuery != null) {
            symbolQuery.setPackage(null); // TODO(beyang): KLUDGE. Note: if this is removed, we still need to do `symbolQuery.getPackage().setBaseDir(null)`
        }

        // if we have a full symbol descriptor in the request, then go straight to the relevant files first because
        // it's probably part of an xDefinition request
        if (symbolQuery != null
                && symbolQuery.getSimpleName() != null
                && symbolQuery.getOutermostContainerName() != null
                && symbolQuery.getElementKind() != null) {

            Span tsSpan = Tracing.startSpanFromContext(ctx, "computing targetedSymbols");
            List<SymbolInformation> targetedSymbols = initializedTargetedSymbols(symbolQuery);
            // TODO: add back uninitialized case?

            Tracing.endSpan(tsSpan, "number of symbols", targetedSymbols.size());
            if (!targetedSymbols.isEmpty()) {
                t.log("Found structured workspace symbols", "numResults", targetedSymbols.size(), "kind", symbolQuery.getElementKind(), "query", simpleQuery);
                return targetedSymbols.stream()
                        .map(symbol -> Pair.of(-1 * symbolScore(symbol, simpleQuery), symbol))
                        .sorted(Comparator.comparing(Pair::getLeft))
                        .limit(limit)
                        .map(Pair::getRight)
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }


        // even if we're doing a raw text search, create a symbol out of it because the symbol-filtering visitor is
        // faster (ignores non-definitions)
        final SymbolDescriptor symbolFromText = SymbolDescriptor.of(null, simpleQuery, null, null, null, null);

        // raw text searches are interactive, so go ahead and wait for workspace initialization
        ArrayList<SymbolInformation> symbols = workspaceManager.getWorkspaces().stream()
                .flatMap(workspace -> {

                    Collection<JavaFileObject> files;
                    try {
                        if (symbolQuery != null && symbolQuery.getPackageName() != null) {
                            files = workspace.getPackageSourceFileObjects(symbolQuery.getPackageName());
                        } else {
                            files = workspace.getSourceFiles();
                        }
                    } catch (Exception e) {
                        // TODO: convert to for-loop
                        throw new RuntimeException(e);
                    }

                    return files.stream()
                            .map(file -> compilerService.parse(file, workspace.getCompiler()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(symbolQuery == null
                                    ? compilationResult -> compilationResult.containsPartialSymbol(symbolFromText)
                                    : compilationResult -> compilationResult.containsExactSymbol(symbolQuery));
                })
                .map(symbolQuery == null
                        ? compilationResult -> compilationResult.findSymbols(simpleQuery)
                        : compilationResult -> compilationResult.findSymbols(symbolQuery))
                .flatMap(Collection::stream)
                .map(symbol -> Pair.of(-1 * symbolScore(symbol, simpleQuery), symbol))
                .sorted(Comparator.comparing(Pair::getLeft))
                .limit(limit)
                .map(Pair::getRight)
                .collect(Collectors.toCollection(ArrayList::new));

        t.end("query", simpleQuery);
        return symbols;
    }

    private List<SymbolInformation> initializedTargetedSymbols(SymbolDescriptor symbolQuery) throws Exception {

        ArrayList<SymbolInformation> targetedSymbols = new ArrayList<>();

        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            for (String uri : workspace.getSourceUris()) {
                String baseFileName = StringUtils.substringAfterLast(uri, "/");
                baseFileName = StringUtils.substringBeforeLast(baseFileName, ".");
                if (baseFileName.equals(symbolQuery.getOutermostContainerName())) {
                    JavaFileObject source = workspace.getSourceFile(uri);
                    if (source == null) {
                        continue;
                    }
                    compilerService
                            .parse(source, workspace.getCompiler())
                            .ifPresent(compilationResult -> {
                                if (compilationResult.containsExactSymbol(symbolQuery)) {
                                    targetedSymbols.addAll(compilationResult.findSymbols(symbolQuery));
                                }
                            });
                }
            }
        }

        return targetedSymbols;
    }

    private double symbolScore(SymbolInformation symbol, String rawQuery) {
        double score = 0.0;
        String simpleName = symbol.getName();
        if (simpleName.equals(rawQuery)) {
            score += 1000;
        } else if (simpleName.toLowerCase().equals(rawQuery.toLowerCase())) {
            score += 900;
        } else if (simpleName.toLowerCase().startsWith(rawQuery.toLowerCase())) {
            score += 50;
        }

        score += 10 * ((double) rawQuery.length()) / simpleName.length();

        score += (double) 10 / symbol.getLocation().getUri().length();

        if (symbol.getKind() != null) {
            switch (symbol.getKind()) {
                case CLASS:
                case INTERFACE:
                case PACKAGE:
                    score += 200;
            }
        }
        return score;
    }

    public List<SymbolInformation> documentSymbol(DocumentSymbolParams documentSymbolParams, Map<String, Object> ctx) {

        // we want all symbols in the specified document, so just query for the empty string

        long before = System.currentTimeMillis();

        String uri = documentSymbolParams.getTextDocument().getUri();

        Workspace workspace = workspaceManager.getWorkspaceContainingUri(uri);
        JavaFileObject file = workspace.getSourceFile(uri);

        ArrayList<SymbolInformation> symbols = new ArrayList<>();

        compilerService.parse(file, workspace.getCompiler())
                .ifPresent(compilationResult -> symbols.addAll(compilationResult.findSymbols("")));

        // don't sort them; since it's for the current file, it'll be more intuitive to keep them in document order

        log.trace("Document symbol (file: {}) took {} ms", uri, System.currentTimeMillis() - before);

        return symbols;
    }


    // note that these loops can be interrupted; the intention is that we cancel any long-running computations if
    // the session is shut down
    public List<ReferenceInformation> xReferences(WorkspaceReferencesParams params, Object requestId, Map<String, Object> ctx) throws Exception {

        String queryName = Optional.ofNullable(params.getQuery())
                .map(SymbolDescriptor::getSimpleName)
                .flatMap(Optional::ofNullable)
                .orElse("");
        PackageIdentifier pkg = params.getQuery() != null ? params.getQuery().getPackage() : null;

        if (queryName.isEmpty()) return Collections.emptyList();

        int limit = params.getLimit() == null ? REFERENCES_LIMIT : params.getLimit();

        long before = System.currentTimeMillis();

        // if there's no request id, then apply the identity function to each partial result, otherwise apply
        // this::streamPartialResults
        Function<List<LanguageData>, List<LanguageData>> streamingFunction;
        if (requestId == null) {
            streamingFunction = x -> x;
        } else {
            LongAdder counter = new LongAdder();

            // initialize the streaming results if we plan on sending any
            JsonPatch streamingInitPatch = new JsonPatch();
            streamingInitPatch.add(JsonPatchOperation.of("add", "", new ArrayList()));
            partialResultStreamer.sendPartialResult(requestId, streamingInitPatch);

            streamingFunction = someReferences -> streamPartialXReferences(
                    someReferences,
                    pkg,
                    requestId,
                    counter,
                    limit
            );
        }
        List<Workspace> workspaces = workspaceManager.getWorkspaces();

        List<ReferenceInformation> accumulator = new ArrayList<>();

        for (Workspace workspace : workspaces) {
            if (Thread.currentThread().isInterrupted()) {
                return Collections.emptyList();
            }

            boolean skipThisWorkspace = true;
            // Limit our search to workspaces that explicitly depend on the query package (if it is specified).
            // This may leave some references out (transitive dependencies), but we do so for performance.
            if (pkg == null || pkg.getId() == null) {
                skipThisWorkspace = false;
            } else {
                PackageInformation wsPkgInfo = workspace.getThisArtifactInformation();
                if (wsPkgInfo != null) {
                    PackageDescriptor wsPkgDesc = wsPkgInfo.getPackage();
                    if (wsPkgDesc != null) {
                        PackageIdentifier wsPkg = wsPkgDesc.getIdentifier();
                        if (wsPkg != null) {
                            // Check if this is the source package
                            if (pkg.getId().equals(wsPkg.getId())) {
                                skipThisWorkspace = false;
                            }
                            // Check if this package depends on the query package
                            for (DependencyReference dep : wsPkgInfo.getDependencies()) {
                                if (pkg.getId().equals(dep.getAttributes().getId())) {
                                    skipThisWorkspace = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (skipThisWorkspace) continue;

            for (String uri : workspace.getSourceUris()) {
                if (Thread.currentThread().isInterrupted()) {
                    return Collections.emptyList();
                }
                if (accumulator.size() >= limit) {
                    break;
                }
                JavaFileObject sourceFile = workspace.getSourceFile(uri);
                if (sourceFile.getKind() != JavaFileObject.Kind.SOURCE) {
                    continue;
                }
                Optional<CompilationResult> optionalResult = compilerService.parse(sourceFile, workspace.getCompiler());
                if (!optionalResult.isPresent()) {
                    continue;
                }
                CompilationResult compilationResult = optionalResult.get();
                // quick search for element in a tree
                if (!compilationResult.containsExactSymbol(queryName)) {
                    continue;
                }
                optionalResult = compilerService.analyze(sourceFile, workspace, ctx);
                if (!optionalResult.isPresent()) {
                    continue;
                }
                compilationResult = optionalResult.get();
                List<LanguageData> someReferences = compilationResult.findReferences(params.getQuery().toSignature());
                if (someReferences.isEmpty()) {
                    continue;
                }
                // stream partial results or not, depending on request id
                List<LanguageData> streamedReferences = streamingFunction.apply(someReferences);
                for (LanguageData streamedReference : streamedReferences) {
                    accumulator.add(ReferenceInformation.of(streamedReference.getLocation(), SymbolDescriptor.of(streamedReference.getSignature(), pkg)));
                }
            }
        }

        // we can't sort streaming refs, so only sort if they're non-streaming
        if (requestId == null) {
            accumulator.sort((r1, r2) -> { // stable ordering
                Location l1 = r1.getReference();
                Location l2 = r2.getReference();
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
        }

        return accumulator;
    }

    private List<LanguageData> streamPartialXReferences(List<LanguageData> partialResults,
                                                        PackageIdentifier packageIdentifier,
                                                        Object requestId,
                                                        LongAdder counter,
                                                        int limit) {

        if (partialResults.isEmpty()) return partialResults;
        if (counter.intValue() >= limit) return Collections.emptyList();

        int remaining = limit - counter.intValue();
        if (remaining < 0) {
            remaining = 0;
        }

        if (remaining < partialResults.size()) {
            partialResults = partialResults.subList(0, remaining);
        }

        JsonPatch patch = partialResults.stream()
                .filter(data -> data.getSignature() != null)
                .map(data -> ReferenceInformation.of(data.getLocation(), SymbolDescriptor.of(data.getSignature(), packageIdentifier)))
                .map(reference -> JsonPatchOperation.of("add", "/-", reference))
                .collect(Collectors.toCollection(JsonPatch::new));

        if (!patch.isEmpty()) {
            counter.add(patch.size());
            partialResultStreamer.sendPartialResult(requestId, patch);
        }

        return partialResults;
    }

    public List<PackageInformation> xPackages(Map<String, Object> ctx) {

        return workspaceManager.getWorkspaces().stream()
                .map(Workspace::getThisArtifactInformation)
                .filter(packageInformation -> packageInformation != null && packageInformation.getPackage() != null)
                .distinct()
                .sorted(PackageInformation.comparator)
                .collect(Collectors.toList());
    }

    public List<DependencyReference> xDependencies(Map<String, Object> ctx) {
        return xPackages(ctx).stream()
                .map(PackageInformation::getDependencies)
                .flatMap(Collection::stream)
                .distinct()
                .sorted(DependencyReference.comparator)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private class ReferencesFilter implements Predicate<LanguageData> {

        private Location definition;

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
            Location location = candidate.getLocation();
            return location == null || !definition.equals(location);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        log.trace("Garbage collecting language service");
        super.finalize();
    }
}
