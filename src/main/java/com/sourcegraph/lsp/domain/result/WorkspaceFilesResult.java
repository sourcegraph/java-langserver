package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;

import java.util.ArrayList;

// Create a concrete subclass here so that we can pass its type to the deserializer more conveniently.
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceFilesResult extends ArrayList<TextDocumentIdentifier> {
}
