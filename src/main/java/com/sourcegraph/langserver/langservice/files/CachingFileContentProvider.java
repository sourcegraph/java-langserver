package com.sourcegraph.langserver.langservice.files;

import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CachingFileContentProvider implements FileContentProvider {

    FileContentProvider actualProvider;

    ConcurrentHashMap<String, String> documentCache;

    ConcurrentHashMap<String, List<TextDocumentIdentifier>> uriCache;

    public CachingFileContentProvider(FileContentProvider actualProvider) {
        this.actualProvider = actualProvider;
        this.documentCache = new ConcurrentHashMap<>();
        this.uriCache = new ConcurrentHashMap<>();
    }

    @Override
    public InputStream readContent(String uri) throws Exception {
        String content = documentCache.computeIfAbsent(uri, __ -> {
            try {
                return IOUtils.toString(actualProvider.readContent(uri), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws Exception {
        return uriCache.computeIfAbsent(baseUri, __ -> {
            try {
                return actualProvider.listFilesRecursively(baseUri);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
