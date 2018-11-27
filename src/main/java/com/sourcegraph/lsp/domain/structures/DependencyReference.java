package com.sourcegraph.lsp.domain.structures;

import com.google.common.collect.Maps;
import com.sourcegraph.langserver.langservice.javaconfigjson.DependencyInfo;
import org.apache.maven.model.Dependency;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by beyang on 1/29/17.
 */
public class DependencyReference implements SourceGenerable {

    private PackageIdentifier attributes;
    private Map<String, String> hints;

    public static Comparator<DependencyReference> comparator = Comparator.nullsFirst((a, b) ->
        PackageIdentifier.comparator.compare(a.getAttributes(), b.getAttributes())
    );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyReference that = (DependencyReference) o;

        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        return hints != null ? hints.equals(that.hints) : that.hints == null;
    }

    @Override
    public int hashCode() {
        int result = attributes != null ? attributes.hashCode() : 0;
        result = 31 * result + (hints != null ? hints.hashCode() : 0);
        return result;
    }

    public PackageIdentifier getAttributes() {
        return attributes;
    }

    public void setAttributes(PackageIdentifier attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getHints() {
        return hints;
    }

    public void setHints(Map<String, String> hints) {
        this.hints = hints;
    }

    public static DependencyReference of(PackageIdentifier attributes, Map<String, String> hints) {
        DependencyReference dep = new DependencyReference();
        dep.setAttributes(attributes);
        dep.setHints(hints);
        return dep;
    }

    public static DependencyReference ofDependencyInfo(DependencyInfo depInfo) {
        DependencyReference dep = new DependencyReference();
        dep.setAttributes(PackageIdentifier.ofMaven(depInfo.getGroupId(), depInfo.getArtifactId(), depInfo.getVersion()));
        dep.setHints(Maps.newHashMap());
        return dep;
    }

    public static DependencyReference ofMavenDependency(Dependency mavenDep) {
        DependencyReference dep = new DependencyReference();
        dep.setAttributes(PackageIdentifier.ofMaven(mavenDep.getGroupId(), mavenDep.getArtifactId(), mavenDep.getVersion()));
        dep.setHints(Maps.newHashMap());
        return dep;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s)", getClass().getSimpleName(),
                SourceGenerable.q(attributes, linePrefix),
                SourceGenerable.q(hints, linePrefix)
        );
    }
}