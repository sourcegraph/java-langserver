package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.PackageInformation;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;

import java.util.ArrayList;
import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceXPackagesResult extends ArrayList<PackageInformation> implements SourceGenerable {
    public static WorkspaceXPackagesResult of(PackageInformation... pkgs) {
        Arrays.sort(pkgs, Comparators.PACKAGE_INFORMATION);
        WorkspaceXPackagesResult r = new WorkspaceXPackagesResult();
        for (PackageInformation sym: pkgs) {
            r.add(sym);
        }
        return r;
    }

    @Override
    public String generateSource(String linePrefix) {
        return SourceGenerable.q(this, linePrefix, getClass().getSimpleName() + ".of");
    }
}
