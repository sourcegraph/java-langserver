package com.sourcegraph.langserver.langservice.files;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.utils.LanguageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ResourceFileProvider is a FileContentProvider implementation that uses a resource directory as the backing source of
 * file contents.
 */
public class ResourceFileProvider implements FileContentProvider {

    private static final Logger log = LoggerFactory.getLogger(ResourceFileProvider.class);

    private String type;
    private String id;

    /**
     * Mapping from sentinel files to IDs. Each ID should correspond to a directory in the resources/fileOverlay directory.
     */
    private static final ImmutableMap<String, String> GRADLE_SENTINEL_FILES_TO_ID = ImmutableMap.of(
            "file:///src/main/java/org/dom4j/dom/DOMElement.java", "org.dom4j/dom4j",
            "file:///src/main/java/org/testng/TestNG.java", "org.testng/testng",
            "file:///spring-core/src/main/java/org/springframework/core/SpringVersion.java", "org.springframework",
            "file:///core/src/main/java/org/elasticsearch/ElasticsearchWrapperException.java", "org.elasticsearch/elasticsearch",
            "file:///TEST_ARTIFACT_SENTINEL", "test-artifact"
    );

    /**
     * Mapping from sentinel files to IDs. Each ID should correspond to a directory in the resources/fileOverlay directory.
     */
    private static final ImmutableMap<String, String> ANT_SENTINEL_FILES_TO_ID = ImmutableMap.of(
            "file:///src/core/lombok/Lombok.java", "org.projectlombok/lombok",
            "file:///java/org/apache/catalina/Server.java", "org.apache.tomcat",
            "file:///TEST_ARTIFACT_SENTINEL", "test-artifact/test-artifact",
            "file:///core/src/java/org/jdom2/transform/JDOMSource.java", "org.jdom/jdom2",
            "file:///JSONPointerException.java", "org.json/json"
    );

    private Set<String> allFilePaths; // lazily initialized to concurrency-safe value

    public static Optional<ResourceFileProvider> forMavenArtifact(String id) {
        try {
            Resources.getResource(Paths.get("fileOverlay", PackageIdentifier.Type.MAVEN.toString(), id).toString()); // make sure resource exists
            return Optional.of(new ResourceFileProvider(PackageIdentifier.Type.MAVEN, id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<ResourceFileProvider> forGradleWorkspace(Set<TextDocumentIdentifier> files) {
        return bySentinel(files, GRADLE_SENTINEL_FILES_TO_ID, PackageIdentifier.Type.GRADLE);
    }

    public static Optional<ResourceFileProvider> forAntWorkspace(Set<TextDocumentIdentifier> files) {
        return bySentinel(files, ANT_SENTINEL_FILES_TO_ID, PackageIdentifier.Type.ANT);
    }

    /**
     * @param files         input files
     * @param sentinelFiles map of sentinel files used to detect if there is an overlay
     * @param type          package descriptor type
     * @return optional resource file provider, not empty if at least one input file matches sentinel one
     */
    protected static Optional<ResourceFileProvider> bySentinel(Set<TextDocumentIdentifier> files,
                                                               Map<String, String> sentinelFiles,
                                                               PackageIdentifier.Type type) {
        for (Map.Entry<String, String> e : sentinelFiles.entrySet()) {
            String sentinel = e.getKey();
            if (!files.contains(new TextDocumentIdentifier().withUri(sentinel))) {
                continue;
            }
            String id = e.getValue();
            try {
                Resources.getResource(Paths.get("fileOverlay", type.toString(), id).toString()); // make sure resource exists
                return Optional.of(new ResourceFileProvider(type, id));
            } catch (Exception exception) {
                log.error("Expected overlays for id {}, but failed to load them: {}", id, exception);
            }
        }
        return Optional.empty();
    }

    private ResourceFileProvider(PackageIdentifier.Type type, String id) {
        this.type = type.toString();
        this.id = id;
    }

    private synchronized void ensureInitAllFiles() throws IOException {
        if (allFilePaths != null) {
            return;
        }
        allFilePaths = ConcurrentHashMap.newKeySet();
        try {
            allFilePaths.addAll(Resources.readLines(
                    Resources.getResource(Paths.get("fileOverlay", type, id, "files.txt").toString()),
                    Charsets.UTF_8
            ).stream().filter(p -> !p.isEmpty()).collect(Collectors.toSet()));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream readContent(String uri) throws IOException {
        ensureInitAllFiles();
        String path = LanguageUtils.vfsPath(LanguageUtils.uriToPath(uri));
        if (!allFilePaths.contains(path)) {
            throw new IOException(String.format("file %s not found", uri));
        }

        try {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Path resourcePath = Paths.get("fileOverlay", type, id, "content").resolve(path);
            URL resourceUrl = Resources.getResource(LanguageUtils.vfsPath(resourcePath));
            return resourceUrl.openStream();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws IOException {
        ensureInitAllFiles();
        String basePath = LanguageUtils.vfsPath(LanguageUtils.uriToPath(baseUri));
        return allFilePaths.stream().filter(p -> p.startsWith(basePath))
                .map(p -> new TextDocumentIdentifier().withUri(String.format("file://%s", p)))
                .collect(Collectors.toList());
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
