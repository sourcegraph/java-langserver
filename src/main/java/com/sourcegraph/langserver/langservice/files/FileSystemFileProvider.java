package com.sourcegraph.langserver.langservice.files;

import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.utils.LanguageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * FileSystemFileProvider is a FileContentProvider implementation
 * that serves file contents from the local filesystem.
 */
public class FileSystemFileProvider implements FileContentProvider {

    private Path root;

    /**
     * @param root root directory from which to serve files.
     */
    public FileSystemFileProvider(Path root) {
        this.root = root;
    }

    @Override
    public InputStream readContent(String uri) throws IOException {
        try {
            return resolve((uri)).toUri().toURL().openStream();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws IOException {
        List<TextDocumentIdentifier> results = new LinkedList<>();
        Path base = resolve(baseUri);
        File baseFile = base.toFile();
        if (!baseFile.exists()) {
            throw new IOException(baseUri + " does not exist");
        }
        if (!baseFile.isDirectory()) {
            throw new IOException(baseUri + " is not a directory");
        }
        collect(base, base, results);
        return results;
    }

    private void collect(Path base, Path current, List<TextDocumentIdentifier> results) {
        File file = current.toFile();
        if (file.isFile()) {
            results.add(new TextDocumentIdentifier().
                    withUri(LanguageUtils.pathToUri(base.relativize(current).toString())));
            return;
        }
        File children[] = current.toFile().listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            collect(base, child.toPath(), results);
        }
    }

    private Path resolve(String uri) {
        String path = LanguageUtils.vfsPath(LanguageUtils.uriToPath(uri));
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return root.resolve(path);
    }


}
