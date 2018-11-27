package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.structures.ReferenceInformation;
import com.sourcegraph.lsp.domain.structures.SourceGenerable;

import java.util.ArrayList;
import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceXReferencesResult extends ArrayList<ReferenceInformation> implements SourceGenerable {
    public static WorkspaceXReferencesResult of(ReferenceInformation... refs) {
        WorkspaceXReferencesResult r = new WorkspaceXReferencesResult();
        Arrays.sort(refs, Comparators.REFERENCE_INFORMATION);
        for (ReferenceInformation ref : refs) {
            r.add(ref);
        }
        return r;
    }

    @Override
    public String generateSource(String linePrefix) {
        return SourceGenerable.q(this, linePrefix, getClass().getSimpleName() + ".of");
    }
}
