package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.Location;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentReferencesResult extends ArrayList<Location> implements SourceGenerable {
    public static TextDocumentReferencesResult of(Location... locations) {
        Arrays.sort(locations, Comparators.LOCATION);
        TextDocumentReferencesResult result = new TextDocumentReferencesResult();
        result.addAll(Arrays.asList(locations));
        return result;
    }

    public String generateSource(String linePrefix) {
        String prefix2 = linePrefix + "  ";
        String children = String.join(",\n" + prefix2, this.stream().map(c -> c.generateSource(prefix2)).collect(Collectors.toList()));
        return String.format("TextDocumentReferencesResult.of(\n%s%s\n%s)", prefix2, children, linePrefix);
    }
}
