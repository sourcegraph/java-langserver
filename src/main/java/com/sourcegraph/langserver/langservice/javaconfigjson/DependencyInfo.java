package com.sourcegraph.langserver.langservice.javaconfigjson;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class DependencyInfo {

    private String groupId;

    private String artifactId;

    private String version;

    private String scope;

    private String packaging;

    private String systemPath;

    /**
     * This field is no longer used but leaving it to support old javaconfig.json
     */
    private List<String> packages;

    public DependencyInfo() {
    }

    public DependencyInfo(String groupId, String artifactId, String version, String scope, String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.packaging = packaging;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getSystemPath() {
        return systemPath;
    }

    public void setSystemPath(String systemPath) {
        this.systemPath = systemPath;
    }

    @JsonIgnore
    public String getCoordinates() {
        return String.join(":", groupId, artifactId, version);
    }


    @JsonIgnore
    @Deprecated
    public List<String> getPackages() {
        return packages;
    }

    @JsonIgnore
    @Deprecated
    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    /**
     * Allow hashing and equality comparisons so that we can store these in sets and hash-maps in order to ensure
     * uniqueness.
     */
    @JsonIgnore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyInfo that = (DependencyInfo) o;

        if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) return false;
        if (artifactId != null ? !artifactId.equals(that.artifactId) : that.artifactId != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (scope != null ? !scope.equals(that.scope) : that.scope != null) return false;
        if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null) return false;
        if (systemPath != null ? !systemPath.equals(that.systemPath) : that.systemPath != null) return false;
        return packages != null ? packages.equals(that.packages) : that.packages == null;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
        result = 31 * result + (systemPath != null ? systemPath.hashCode() : 0);
        result = 31 * result + (packages != null ? packages.hashCode() : 0);
        return result;
    }
}
