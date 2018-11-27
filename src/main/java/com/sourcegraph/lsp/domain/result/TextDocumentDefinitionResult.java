package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;
import com.sourcegraph.lsp.domain.structures.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDocumentDefinitionResult extends ArrayList<Location> implements SourceGenerable {
    public static TextDocumentDefinitionResult of(Location... locs) {
        Arrays.sort(locs, Comparators.LOCATION);
        TextDocumentDefinitionResult r = new TextDocumentDefinitionResult();
        for (Location loc: locs) {
            r.add(loc);
        }
        return r;
    }

    @Override
    public String generateSource(String linePrefix) {
        String linePrefix2 = linePrefix + "  ";
        String children = String.join(",\n" + linePrefix2, this.stream().map(c -> c.generateSource(linePrefix2)).collect(Collectors.toList()));
        return String.format("%s.of(\n%s%s\n%s)", this.getClass().getSimpleName(), linePrefix2, children, linePrefix);
    }
}
