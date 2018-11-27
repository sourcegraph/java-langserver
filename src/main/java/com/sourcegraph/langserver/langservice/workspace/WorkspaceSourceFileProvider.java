package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.utils.ExecutorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WorkspaceSourceFileProvider provides efficient access to source files (via pre-fetching).
 */
public class WorkspaceSourceFileProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSourceFileProvider.class);

    private ConfigProvider configProvider;

    private FileContentProvider files;

    private String rootDir;

    /**
     * sourceFileFutures is a map from file URI to CompletableFuture resolving to a JavaFileObject made from the file
     * contents. This is lazily initialized on first file fetch.
     */
    private ConcurrentHashMap<String, CompletableFuture<JavaFileObject>> sourceFileFutures;
    private Object sourceFileFuturesInitLock = new Object();


    // cached list of all source URIs covered by this workspace
    private Set<String> sourceUris;
    private Object sourceUrisInitLock = new Object();

    // map from JavaFileObject.toURI() to PackageIdentifier (threadsafe)
    private Set<URI> fetchedSourceFileUris;

    public WorkspaceSourceFileProvider(FileContentProvider files, String rootDir, ConfigProvider configProvider) {
        this.files = files;
        this.rootDir = rootDir;
        this.configProvider = configProvider;

        this.fetchedSourceFileUris = ConcurrentHashMap.newKeySet();
    }


    public Set<String> getSourceUris() {
        if (sourceUris != null) {
            return sourceUris;
        }

        synchronized(sourceUrisInitLock) {
            if (sourceUris != null) {
                return sourceUris;
            }

            String rootURI = "file://" + rootDir;
            List<TextDocumentIdentifier> allFiles;
            try {
                allFiles = files.listFilesRecursively("file:///");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Set<String> newSourceUris = new HashSet<>();

            List<String> roots = new ArrayList<>();
            Project cfg = configProvider.getConfig();
            roots.addAll(cfg.getSourceDirectories());
            roots.addAll(cfg.getTestSourceDirectories());
            for (TextDocumentIdentifier identifier : allFiles) {
                String uri = identifier.getUri();
                if (!LanguageUtils.isRelevantFile(uri)) {
                    continue;
                }

                for (String root : roots) {
                    String sourceDir = LanguageUtils.concatPath(rootURI, root);
                    if (LanguageUtils.uriContainsOrEquals(sourceDir, uri)) {
                        newSourceUris.add(uri);
                        break;
                    }
                }
            }
            sourceUris = newSourceUris;
            return sourceUris;
        }
    }


    public Set<JavaFileObject> getSourceFiles() {
        Collection<CompletableFuture<JavaFileObject>> fileFutures = getSourceFileFutures().values();
        return CompletableFuture.allOf(fileFutures.toArray(new CompletableFuture[fileFutures.size()]))
                .thenApply(__ -> fileFutures.stream().map(f -> f.join()).collect(Collectors.toSet()))
                .join();
    }

    public JavaFileObject getSourceFile(String uri) {
        try {
            return getSourceFileFutures().get(uri).join();
        } catch (Exception e) {
            log.warn("Missing source file {}", uri, e);
            return null;
        }
    }

    /**
     * getSourceFileFutures returns a map from URI to CompletableFutures resolving to JavaFileObjects. when it is
     * first called, it creates the map and adds future for every source file URI, thereby kicking off a
     * background fetch for all file content.
     */
    private ConcurrentHashMap<String, CompletableFuture<JavaFileObject>> getSourceFileFutures() {
        if (sourceFileFutures != null) {
            return sourceFileFutures;
        }
        synchronized (sourceFileFuturesInitLock) {
            if (sourceFileFutures != null) {
                return sourceFileFutures;
            }
            ConcurrentHashMap<String, CompletableFuture<JavaFileObject>> newSourceFileFutures = new ConcurrentHashMap<>();
            for (String uri : getSourceUris()) {
                newSourceFileFutures.put(uri, CompletableFuture.supplyAsync(() -> {
                    try {
                        SourceFile sourceFile = fetchSourceFile(uri);
                        fetchedSourceFileUris.add(sourceFile.toUri());
                        return sourceFile;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, ExecutorUtils.getFileFetcherExecutorService()));
            }
            sourceFileFutures = newSourceFileFutures;
        }
        return sourceFileFutures;
    }

    public Set<URI> getFetchedSourceFileUris() {
        return fetchedSourceFileUris;
    }

    /**
     * fetchSourceFile actually fetches the source file from the files provider. This should only be called from
     * getSourceFileFutures.
     */
    private SourceFile fetchSourceFile(String uri) throws Exception {
        Path path = LanguageUtils.uriToPath(uri);
        String content = IOUtils.toString(files.readContent(uri), StandardCharsets.UTF_8);
        String vfsPath = LanguageUtils.vfsPath(path);
        SourceFile sourceFile = new SourceFile(
                vfsPath,
                SourceFile.pathToBinaryName(relPath(path)),
                content);
        return sourceFile;
    }

    public String relPath(Path path) throws Exception {
        String p = LanguageUtils.vfsPath(path);
        List<Path> sourcePaths = new ArrayList<>();
        for (String d : configProvider.getConfig().getSourceDirectories()) {
            sourcePaths.add(Paths.get(rootDir, d));
        }
        for (String d : configProvider.getConfig().getTestSourceDirectories()) {
            sourcePaths.add(Paths.get(rootDir, d));
        }

        String candidate = relPath(p, sourcePaths);
        if (candidate != null) {
            return candidate;
        }
        throw new Exception("could not relativize path " + path.toString());
    }

    private static String relPath(String path, List<Path> prefixes) {
        for (Path prefix : prefixes) {
            String root = StringUtils.appendIfMissing(LanguageUtils.vfsPath(prefix), "/");
            if (path.startsWith(root)) {
                return StringUtils.removeStart(path, root);
            }
        }
        return null;
    }

}
