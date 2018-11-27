package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.DependencyReference;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceXDependenciesResult extends ArrayList<DependencyReference> implements SourceGenerable {
    public static WorkspaceXDependenciesResult of(DependencyReference... deps) {
        Arrays.sort(deps, Comparators.DEPENDENCY_REFERENCE);
        WorkspaceXDependenciesResult r = new WorkspaceXDependenciesResult();
        for (DependencyReference dep: deps) {
            r.add(dep);
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
