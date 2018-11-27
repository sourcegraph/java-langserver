package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * PackageDescriptor wraps PackageIdentifier with additional, non-identifying properties. Note that whereas
 * PackageIdentifier implements semantic equals/hashCode and can be relied on as a quasi-unique identifier,
 * PackageDescriptor should not be used to identify a build package. Two PackageDescriptor instances derived from the
 * same underlying package may have different property values.
 *
 * Created by beyang on 2/22/17.
 */
public class PackageDescriptor implements SourceGenerable {

    public static PackageDescriptor ofMaven(String groupId, String artifactId, String version, String baseDir) {
        return new PackageDescriptor(PackageIdentifier.ofMaven(groupId, artifactId, version), baseDir);
    }

    public static PackageDescriptor of(PackageIdentifier.Type type, String id, String version, String url, String commit, String baseDir) {
        return new PackageDescriptor(PackageIdentifier.of(type, id, version, url, commit), baseDir);
    }

    public PackageDescriptor(PackageIdentifier identifier, String baseDir) {
        this.identifier = identifier;
        this.baseDir = baseDir;
    }

    @SuppressWarnings("unused") // Private no-args constructor for JSON deserialization
    private PackageDescriptor() {
        this.identifier = new PackageIdentifier();
    }

    @Nonnull
    private PackageIdentifier identifier;

    @Nullable
    private String baseDir;

    @JsonIgnore
    public PackageIdentifier getIdentifier() {
        return identifier;
    }

    @JsonIgnore
    public void setIdentifier(PackageIdentifier identifier) {
        this.identifier = identifier;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
    
    public String getId() {
        return identifier.getId();
    }

    public void setId(String id) {
        identifier.setId(id);
    }

    public String getVersion() {
        return identifier.getVersion();
    }

    public void setVersion(String version) {
        identifier.setVersion(version);
    }

    public String getRepoURL() {
        return identifier.getRepoURL();
    }

    public void setRepoURL(String url) {
        identifier.setRepoURL(url);
    }

    public String getCommit() {
        return identifier.getCommit();
    }

    public void setCommit(String commit) {
        identifier.setCommit(commit);
    }

    @NonNull
    public PackageIdentifier.Type getType() {
        return identifier.getType();
    }

    public void setType(PackageIdentifier.Type type) {
        identifier.setType(type);
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s, %s, %s, %s, %s)", getClass().getSimpleName(),
                SourceGenerable.generateSource(identifier.getType()),
                SourceGenerable.q(identifier.getId()),
                SourceGenerable.q(identifier.getVersion()),
                SourceGenerable.q(identifier.getRepoURL()),
                SourceGenerable.q(identifier.getCommit()),
                SourceGenerable.q(baseDir));
    }
}
