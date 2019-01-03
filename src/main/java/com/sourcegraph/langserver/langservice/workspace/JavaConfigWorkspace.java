package com.sourcegraph.langserver.langservice.workspace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sourcegraph.langserver.langservice.compiler.JarSource;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibraries;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibrary;
import com.sourcegraph.lsp.domain.structures.DependencyReference;
import com.sourcegraph.lsp.domain.structures.PackageDescriptor;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sourcegraph.lsp.domain.structures.PackageInformation;
import com.sourcegraph.utils.LanguageUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * JavaConfigWorkspace
 */
public class JavaConfigWorkspace implements Workspace, ConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(JavaConfigWorkspace.class);

    // An absolute path that indicates the root directory of this workspace
    // from the root of the entire repository.
    private String rootDir;
    private WorkspaceSourceFileProvider fileProvider;
    private Project config;
    private JavacHolder compiler;

    // cached map of package name to file URIs
    private ConcurrentHashMap<String, Set<String>> packageUris;

    // cached map of package name to JAR classes
    private ConcurrentHashMap<String, Set<JavaFileObject>> jarClasses;

    // map from JavaFileObject.toURI() to PackageIdentifier
    private ConcurrentHashMap<URI, PackageIdentifier> jarClassUriToPackageIdentifier;

    private WorkspaceManager workspaceManager;

    public JavaConfigWorkspace(
            FileContentProvider files,
            Project config,
            String rootDir
    ) {
        this.config = config;
        this.rootDir = rootDir;
        this.jarClassUriToPackageIdentifier = new ConcurrentHashMap<>();
        this.fileProvider = new WorkspaceSourceFileProvider(files, rootDir, this);
    }

    public void setWorkspaceManager(WorkspaceManager w) {
        workspaceManager = w;
    }

    public WorkspaceManager getWorkspaceManager() { return workspaceManager; }

    public String getRootURI() {
        return rootDir;
    }

    public Set<JavaFileObject> getPackageSourceFileObjects(String packageName) throws Exception {
        Set<String> packageUris = getPackageUris(packageName);
        HashSet<JavaFileObject> fileObjects = packageUris.stream()
                .map(this::getSourceFile)
                .collect(Collectors.toCollection(HashSet::new));
        return fileObjects;
    }

    public Set<JavaFileObject> getPackageFileObjects(String packageName) throws IOException {
        try {
            Set<JavaFileObject> fileObjects = getPackageSourceFileObjects(packageName);

            Set<JavaFileObject> jarFileObjects = getJARPackageFileObjects(packageName);
            fileObjects.addAll(jarFileObjects);

            return fileObjects;
        } catch (Exception e) {
            log.error("Error getting FileObjects for package: {}", e);
            throw new IOException(e);
        }
    }

    public Set<JavaFileObject> getJARPackageFileObjects(String packageName) {
        if (jarClasses != null) {
            return jarClasses.getOrDefault(packageName, ImmutableSet.of());
        }
        ConcurrentHashMap<String, Set<JavaFileObject>> packageClasses = new ConcurrentHashMap<>();
        for (Map.Entry<PackageIdentifier, JarSource> e : getAllDeps().entrySet()) {
            PackageIdentifier pkgID = e.getKey();
            JarSource jar = e.getValue();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (!jarEntry.getName().endsWith(".class")) {
                    continue;
                }
                JarEntryFile classFile = new JarEntryFile(jarEntry.getName(), jar, jarEntry);
                String filePackageName = Workspace.Utils.classFileToPackageName(classFile.getName());
                packageClasses.computeIfAbsent(filePackageName, __ -> new HashSet<>()).add(classFile);
                jarClassUriToPackageIdentifier.put(classFile.toUri(), pkgID);
            }
        }
        jarClasses = packageClasses;
        return jarClasses.getOrDefault(packageName, ImmutableSet.of());
    }

    private PackageIdentifier getThisArtifactIdentifier() {
        Project cfg = getConfig();
        if (cfg.getArtifactId() != null) {
            return PackageIdentifier.ofMaven(cfg.getGroupId(), cfg.getArtifactId(), cfg.getVersion());
        }
        return null;
    }

    public PackageIdentifier getArtifactIdentifier(URI fileObjectUri) {
        if (fileProvider.getFetchedSourceFileUris().contains(fileObjectUri)) {
            return getThisArtifactIdentifier();
        }
        return jarClassUriToPackageIdentifier.get(fileObjectUri);
    }

    public PackageInformation getThisArtifactInformation() {
        List<DependencyReference> deps = new ArrayList<>();
        for (PackageIdentifier depId : getDirectDeps()) {
            deps.add(DependencyReference.of(depId, ImmutableMap.of()));
        }

        Set<String> sourceUris;
        try {
            sourceUris = getSourceUris();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Add implicit dependencies on standard libraries
        if (sourceUris.size() > 0) {
            Collection<StandardLibrary> stdlibs = StandardLibraries.getInstance().getLibrariesInUse(getCompiler().getOptions());
            for (StandardLibrary stdlib : stdlibs) {
                if (!Objects.equals(stdlib.getPackageIdentifier(), getThisArtifactIdentifier())) {
                    deps.add(DependencyReference.of(stdlib.getPackageIdentifier(), ImmutableMap.of()));
                }
            }
        }

        deps.sort(DependencyReference.comparator);
        return PackageInformation.of(new PackageDescriptor(getThisArtifactIdentifier(), rootDir), deps);
    }

    public Set<PackageIdentifier> getDependencies() {
        return ImmutableSet.of();
    }

    /**
     * All source file access should go through this method -- it relies on `ConcurrentHashMap::computeIfAbsent` to
     * fetch the contents at most once if necessary, so this function can be called anytime, as many times as desired.
     *
     * @param uri The uri of the desired source file
     * @return the source file
     */
    public JavaFileObject getSourceFile(String uri) {
        return fileProvider.getSourceFile(uri);
    }

    public Set<JavaFileObject> getSourceFiles() {
        return fileProvider.getSourceFiles();
    }

    public boolean containsSourceFile(String uri) throws Exception {
        // Shortcut that doesn't trigger a resolution of source URIs.
        String path = LanguageUtils.uriToPath(uri).toString();
        if (!path.equals(rootDir) && !path.startsWith(rootDir)) {
            return false;
        }

        return getSourceUris().contains(uri);
    }

    /**
     * Returns the Java source files in a given Java package.
     */
    private Set<String> getPackageUris(String packageName) throws Exception {
        if (packageUris != null) {
            return packageUris.getOrDefault(packageName, ImmutableSet.of());
        }
        ConcurrentHashMap<String, Set<String>> newPackageUris = new ConcurrentHashMap<>();
        for (String uri : getSourceUris()) {
            if (!uri.endsWith(".java")) {
                continue;
            }
            Path path = LanguageUtils.uriToPath(uri);
            String pname = sourcePathToPackageName(path);
            newPackageUris.computeIfAbsent(pname, p -> new HashSet<>()).add(uri);
        }

        packageUris = newPackageUris;
        return packageUris.getOrDefault(packageName, ImmutableSet.of());
    }

    public Set<String> getSourceUris() throws Exception {
        return fileProvider.getSourceUris();
    }

    private String sourcePathToPackageName(Path path) throws Exception {
        String relPath = fileProvider.relPath(path);
        int pos = relPath.lastIndexOf('/');
        if (pos < 0) {
            return StringUtils.EMPTY;
        }
        return relPath.substring(0, pos).replace('/', '.');
    }

    private Map<PackageIdentifier, JarSource> allDeps;

    synchronized private Map<PackageIdentifier, JarSource> getAllDeps() {
        if (allDeps == null) {
            allDeps = Maps.newHashMap();
        }
        return allDeps;
    }

    private List<PackageIdentifier> directDeps;

    synchronized private List<PackageIdentifier> getDirectDeps() {
        if (directDeps == null) {
            directDeps = new ArrayList<>();
        }
        return directDeps;
    }

    synchronized public Project getConfig() {
        return config;
    }

    synchronized public JavacHolder getCompiler() {
        if (compiler == null) {
            compiler = new JavacHolder(this, getConfig().getCompilerOptions());
        }
        return compiler;
    }

}
