package com.sourcegraph.lsp.domain.structures;

import lombok.NonNull;
import org.apache.maven.model.Dependency;

import java.util.Comparator;
import java.util.Objects;

import static com.sourcegraph.lsp.domain.structures.PackageIdentifier.Type.MAVEN;

/**
 * PackageIdentifier identifies a build-level package (e.g., Maven artifact) by a list of properties. It can be used
 * as a quasi-unique identifier if a complete set of properties (i.e., either type + id + version OR repoURL + commit)
 * is set. Note, however, that PackageIdentifier can also be used as a *partial* identifier (where only a subset of one
 * of the aforementioned property sets is populated). It is up to the client to distinguish between these two use cases
 * (quasi-unique identifier or partial identifier).
 *
 * Created by beyang on 1/29/17.
 */
public class PackageIdentifier implements SourceGenerable {

    public static PackageIdentifier ofMavenDep(Dependency mavenDep) {
        PackageIdentifier pd = PackageIdentifier.ofMaven(mavenDep.getGroupId(), mavenDep.getArtifactId(), mavenDep.getVersion());
        return pd;
    }

    public static PackageIdentifier ofMaven(String groupId, String artifactId, String version) {
        return new PackageIdentifier().withId(String.join(":", groupId, artifactId)).withVersion(version).withType(MAVEN);
    }

    public static PackageIdentifier of(Type type, String id, String version, String url, String commit) {
        return new PackageIdentifier().withType(type).withId(id).withVersion(version).withUrl(url).withCommit(commit);
    }

    public static Comparator<PackageIdentifier> comparator = Comparator.nullsFirst(
            Comparator.<PackageIdentifier>comparingInt(p -> p.getType().ordinal())
            .thenComparing(PackageIdentifier::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
            .thenComparing(PackageIdentifier::getVersion, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PackageIdentifier::getRepoURL, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PackageIdentifier::getCommit, Comparator.nullsFirst(Comparator.naturalOrder()));

    public enum Type {
        STDLIB,
        MAVEN,
        GRADLE,
        ANT
    }

    @NonNull
    private Type type;
    private String id;
    private String version;

    private String repoURL; // needs to be named precisely
    private String commit;

    public boolean queryMatches(PackageIdentifier p) {
        if (type != null && !Objects.equals(type, p.type)) {
            return false;
        }
        if (id != null && !Objects.equals(id, p.id)) {
            return false;
        }
        if (version != null && !Objects.equals(version, p.version)) {
            return false;
        }
        if (repoURL != null && !Objects.equals(repoURL, p.repoURL)) {
            return false;
        }
        if (commit != null && !Objects.equals(commit, p.commit)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageIdentifier that = (PackageIdentifier) o;

        if (type != that.type) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (repoURL != null ? !repoURL.equals(that.repoURL) : that.repoURL != null) return false;
        return commit != null ? commit.equals(that.commit) : that.commit == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (repoURL != null ? repoURL.hashCode() : 0);
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        return result;
    }

    public PackageIdentifier withType(Type type) {
        this.type = type;
        return this;
    }

    public PackageIdentifier withId(String id) {
        this.id = id;
        return this;
    }

    public PackageIdentifier withVersion(String version) {
        this.version = version;
        return this;
    }

    public PackageIdentifier withUrl(String url) {
        this.repoURL = url;
        return this;
    }

    public PackageIdentifier withCommit(String commit) {
        this.commit = commit;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String url) {
        this.repoURL = url;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s, %s, %s, %s)", getClass().getSimpleName(),
                SourceGenerable.generateSource(type),
                SourceGenerable.q(id),
                SourceGenerable.q(version),
                SourceGenerable.q(repoURL),
                SourceGenerable.q(commit));
    }
}
