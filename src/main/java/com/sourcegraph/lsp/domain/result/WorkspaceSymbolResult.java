package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;
import com.sourcegraph.lsp.domain.structures.SymbolInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceSymbolResult extends ArrayList<SymbolInformation> implements SourceGenerable {
    public static WorkspaceSymbolResult of(SymbolInformation... syms) {
        Arrays.sort(syms, Comparators.SYMBOL_INFORMATION);
        WorkspaceSymbolResult r = new WorkspaceSymbolResult();
        for (SymbolInformation sym: syms) {
            r.add(sym);
        }
        return r;
    }

    @Override
    public String generateSource(String linePrefix) {
        String linePrefix2 = linePrefix + "  ";
        String children = String.join(",\n" + linePrefix2, stream().map(c -> c.generateSource(linePrefix2)).collect(Collectors.toList()));
        return String.format("%s.of(\n%s%s\n%s)", getClass().getSimpleName(), linePrefix2, children, linePrefix);
    }
}
