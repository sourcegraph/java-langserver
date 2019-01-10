package com.sourcegraph.langserver.langservice.files;

import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * RemoteFileContentProvider serves file contents from the Sourcegraph raw file API.
 */
public class RemoteFileContentProvider implements FileContentProvider {

    private static final Logger log = LoggerFactory.getLogger(RemoteFileContentProvider.class);

    /**
     * fetchedTrees is a map from remoteUri to local cache path. It is used to ensure remote workspaces are fetched
     * exactly once.
     */
    private static ConcurrentHashMap<String, CompletableFuture<Void>> fetchedTrees = new ConcurrentHashMap();

    /**
     * remoteRootURI is the remote root URI, e.g.,
     * "https://${TOKEN}@sourcegraph.com/github.com/apache/commons-io@4daab02fb7d967a39eb15fe33f0d5350fc548a98/-/raw/"
     */
    private URL remoteRootURI;

    private File cacheContainer;

    private String authToken;

    public RemoteFileContentProvider(String remoteRootURI, File cacheContainer, String authToken) throws Exception {
        try {
            this.remoteRootURI = new URL(remoteRootURI);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        this.cacheContainer = cacheContainer;
        this.authToken = authToken;

        ensureTreeFetched(remoteRootURI);
    }

    @Override
    public InputStream readContent(String uri) throws Exception {
        URL parsedURI = new URL(uri);
        if (!parsedURI.getHost().equals(remoteRootURI.getHost())) {
            throw new IllegalArgumentException("requested URI was not a sub-URI");
        }
        if (!(parsedURI.getPath().equals(remoteRootURI.getPath()) || parsedURI.getPath().startsWith(remoteRootURI.getPath()))) {
            throw new IllegalArgumentException("requested URI was not a sub-URI");
        }
        String path = uriToCachePath(uri);
        return new FileInputStream(path);
    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws Exception {
        String cachePath = uriToCachePath(baseUri);
        return Files.walk(Paths.get(cachePath))
                .filter(Files::isRegularFile)
                .map(u -> new TextDocumentIdentifier().withUri(cachePathToUri(u.toString())))
                .collect(Collectors.toList());
    }

    /**
     * uriToCachePath maps a remote file URI to its corresponding local filesystem cache path.
     */
    private String uriToCachePath(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            URL url = new URL(uri);
            return Paths.get(cacheRootDir(), url.getProtocol(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * cachePathToUri maps a local filesystem cache path to the corresponding remote file URI.
     */
    private String cachePathToUri(String cachePath) {
        try {
            String rootPath = uriToCachePath(remoteRootURI.toString());
            Path rel = Paths.get(rootPath).relativize(Paths.get(cachePath));
            return new URL(remoteRootURI, rel.toString()).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String cacheTmpDir() {
        return Paths.get(cacheContainer.toString(), "tmp").toString();
    }

    private String cacheRootDir() {
        return Paths.get(cacheContainer.toString(), "root").toString();
    }

    private void ensureTreeFetched(String remoteUri) throws Exception {
        String localPath = uriToCachePath(remoteUri);
        fetchedTrees.computeIfAbsent(remoteUri, u -> CompletableFuture.supplyAsync(() -> {
            if (Files.exists(Paths.get(localPath))) {
                log.info("Cache path for {} already exists, not refetching", remoteUri);
                return null;
            }

            Path tmpDir = null;
            try {
                // Fetch from remote
                Map<String, String> headers = new HashMap<>();
                if (authToken != null) {
                    headers.put("Authorization", "token " + authToken);
                }
                headers.put("Accept", "application/zip");
                InputStream respBody = HTTPUtil.httpGet(remoteUri, headers);
                new File(cacheTmpDir()).mkdirs();
                tmpDir = Files.createTempDirectory(Paths.get(cacheTmpDir()), Paths.get(localPath).getFileName().toString());

                ZipInputStream zipIn = new ZipInputStream(respBody);
                byte[] buffer = new byte[1024];
                for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                    if (entry.isDirectory()) {
                        new File(tmpDir + File.separator + entry.getName()).mkdirs();
                        continue;
                    }
                    File newFile = new File(tmpDir + File.separator + entry.getName());
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zipIn.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                // Create cache dir parent dir
                new File(new File(localPath).getParent()).mkdirs();

                // Atomically move from temp dir to cache dir
                Files.move(tmpDir, Paths.get(localPath));
            } catch (IOException e) {
                // Clean up temp directory
                if (tmpDir != null && Files.exists(tmpDir)) {
                    try {
                        FileUtils.deleteDirectory(tmpDir.toFile());
                    } catch (IOException e2) {
                        throw new RuntimeException(e2);
                    }
                }
                // Clean up cache directory
                if (localPath != null && Files.exists(Paths.get(localPath))) {
                    try {
                        FileUtils.deleteDirectory(new File(localPath));
                    } catch (IOException e2) {
                        throw new RuntimeException(e2);
                    }
                }
                // Complete exceptionally
                throw new RuntimeException(e);
            }
            return null; // End of CompletableFuture supplier
        }));
        fetchedTrees.get(remoteUri).get();
    }
}
