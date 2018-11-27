package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by beyang on 2/2/17.
 */
public interface FileContentProvider {

    /**
     * Reads content from the resource specified by the given URI
     * @param uri URI that denotes content's location
     * @return binary data
     * @throws IOException
     */
    InputStream readContent(String uri) throws Exception;

    List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws Exception;
}
