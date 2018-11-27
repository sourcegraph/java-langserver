package com.sourcegraph.langserver.langservice.javaconfigjson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.maven.model.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Project configuration
 *
 * Created by beyang on 12/21/17.
 */
public class Project {
    public static class Jar {
        // File on disk of the JAR. This is non-null only if the JAR has been resolved/downloaded.
        public String file;

        // The URL from which to download the JAR.
        public String url;
    }

    public static class Dep {
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;

        public Dep() {
            // For JSON serialization
        }

        public Dep(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
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

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }
    }

    public static class Repo {
        private String id;
        private String name;
        private String url;
        private String layout;

        public Repo() {
            // For JSON serialization
        }

        public Repo(String id, String name, String url, String layout) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.layout = layout;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLayout() {
            return layout;
        }

        public void setLayout(String layout) {
            this.layout = layout;
        }
    }

    public static Project newDefaultProject() {
        Project p = new Project();
        p.artifactId = "__inferred__";
        p.groupId = "__inferred__";
        p.version = "0.0.0";
        p.sourceDirectories = Lists.newArrayList("src/main/java");
        p.testSourceDirectories = Lists.newArrayList("src/main/test");
        p.jars = Lists.newArrayList();
        p.compilerOptions = Lists.newArrayList();
        return p;
    }

    private String artifactId;

    private String groupId;

    private String version;

    // List of source directories relative to the project root dir
    private Collection<String> sourceDirectories;

    // List of test source directories, relative to the project root dir
    private Collection<String> testSourceDirectories;

    // List of JAR file paths
    private List<Jar> jars;

    // List of dependencies identified by (groupId, artifactId, version)
    private List<Dep> dependencies;

    // List of artifact repositories to use for fetching dependencies
    private List<Repo> repositories;

    // List of additional javac compiler options
    private List<CompilerOption> compilerOptions;

    public String getArtifactId() {
        return artifactId == null ? "" : artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId == null ? "" : groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version == null ? "" : version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    // Returns source directories of a project (relative to the project root)
    public Collection<String> getSourceDirectories() {
        return sourceDirectories == null ? ImmutableList.of() : sourceDirectories;
    }

    // These should be relative to the project root
    public void setSourceDirectories(Collection<String> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    // Returns test directories of a project (relative to the project root).
    public Collection<String> getTestSourceDirectories() {
        return testSourceDirectories == null ? ImmutableList.of() : testSourceDirectories;
    }

    // These should be relative to the project root
    public void setTestSourceDirectories(Collection<String> testSourceDirectories) {
        this.testSourceDirectories = testSourceDirectories;
    }

    public List<Jar> getJars() {
        return jars == null ? ImmutableList.of() : jars;
    }

    public void setJars(List<Jar> jars) {
        this.jars = jars;
    }

    public List<Dep> getDependencies() {
        return dependencies == null ? ImmutableList.of() : dependencies;
    }

    public void setDependencies(List<Dep> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Repo> getRepositories() {
        return repositories == null ? ImmutableList.of() : repositories;
    }

    public void setRepositories(List<Repo> repositories) {
        this.repositories = repositories;
    }

    public List<CompilerOption> getCompilerOptions() {
        return compilerOptions == null ? ImmutableList.of() : compilerOptions;
    }

    public void setCompilerOptions(List<CompilerOption> compilerOptions) {
        this.compilerOptions = compilerOptions;
    }
}
