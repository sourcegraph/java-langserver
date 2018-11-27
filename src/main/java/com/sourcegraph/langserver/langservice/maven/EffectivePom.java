package com.sourcegraph.langserver.langservice.maven;

import com.google.common.collect.*;
import com.sourcegraph.common.Config;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.langserver.langservice.compiler.JarFileSource;
import com.sourcegraph.langserver.langservice.compiler.JarSource;
import com.sourcegraph.langserver.langservice.javaconfigjson.RepositoryInfo;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.lsp.Tracing;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sourcegraph.utils.AsyncUtils;
import com.sourcegraph.utils.ExecutorUtils;
import com.sourcegraph.utils.Util;
import io.opentracing.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * EffectivePom is a wrapper class that represents an effective POM and is responsible for resolving and fetching
 * POM dependencies.
 *
 * Note that this class is not very fault tolerant. If there's any issue in building the pom (e.g., dependency not
 * found), then the constructor may throw an exception.
 */
public class EffectivePom {

    private static final Logger log = LoggerFactory.getLogger(EffectivePom.class);

    private String groupId;
    private String artifactId;
    private String version;
    private HashMap<String, org.apache.maven.model.Dependency> dependencies;
    private List<PackageIdentifier> transitiveDependencies;
    private ArrayList<Path> sourcePath;
    private ArrayList<Path> testSourcePath;
    private RepositorySystem repositorySystem;
    private RepositorySystemSession repositorySystemSession;
    private List<CompilerOption> compilerOptions;
    private List<org.apache.maven.model.Dependency> dependencyManagement;

    private Model effectiveModel;

    // System path -> system jar file
    private ConcurrentHashMap<String, Optional<JarFile>> systemArtifacts;

    // Remote repositories
    private List<RemoteRepository> repositories;

    // will be true iff transitive dependencies have been resolved and fetched
    private final AtomicBoolean isCompletelyResolved;

    private final MessageAggregator messages;

    public static EffectivePom createAndResolve(String projectBaseDir, Model rawModel, MavenWorkspaceModelResolver modelResolver, List<CompilerOption> compilerOptions, MessageAggregator messages, List<WorkspaceConfigurationServersResult.Server> servers) throws ModelBuildingException {
        return new EffectivePom(projectBaseDir, rawModel, modelResolver, compilerOptions, messages, servers);
    }

    private EffectivePom(String projectBaseDir, Model rawModel, MavenWorkspaceModelResolver modelResolver, List<CompilerOption> compilerOptions, MessageAggregator messages, List<WorkspaceConfigurationServersResult.Server> servers) throws ModelBuildingException {
        dependencies = new HashMap<>();
        transitiveDependencies = new ArrayList<>();
        dependencyManagement = new ArrayList<>();
        systemArtifacts = new ConcurrentHashMap<>();
        repositorySystem = getRepositorySystem();
        repositorySystemSession = getRepositorySystemSession(repositorySystem);
        repositories = new ArrayList<>();
        isCompletelyResolved = new AtomicBoolean(false);
        this.messages = messages;
        this.effectiveModel = MavenUtil.buildEffectiveModel(rawModel, projectBaseDir, modelResolver, repositorySystem, repositorySystemSession, servers);

        // Note: we support multiple comma-separated source directories here (not part of the official Maven spec)
        // This lets us handle Gradle-derived builds with multiple source sets.
        String srcdir = effectiveModel.getBuild().getSourceDirectory();
        if (srcdir == null) {
            sourcePath = Lists.newArrayList(Paths.get("src/main/java"));
        } else {
            sourcePath = Lists.newArrayList(srcdir.split(",")).stream()
                    .map(p -> {
                        Path path = Paths.get(p.trim());
                        if (path.isAbsolute()) {
                            path = Paths.get(projectBaseDir).relativize(path);
                        }
                        return path;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        String testSrcdir = effectiveModel.getBuild().getTestSourceDirectory();
        if (testSrcdir == null) {
            testSourcePath = Lists.newArrayList(Paths.get("src/test/java"));
        } else {
            testSourcePath = Lists.newArrayList(testSrcdir.split(",")).stream()
                    .map(p -> {
                        Path path = Paths.get(p.trim());
                        if (path.isAbsolute()) {
                            path = Paths.get(projectBaseDir).relativize(path);
                        }
                        return path;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        effectiveModel.getDependencies().forEach(dep -> {
            if ("system".equals(dep.getScope()) && dep.getSystemPath() != null) {
                String systemDepKey = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + "@" + dep.getType();
                systemArtifacts.computeIfAbsent(systemDepKey, (__) -> this.fetchSystemArtifact(dep));
            } else {
                String groupAndArtifact = String.join(":", dep.getGroupId(), dep.getArtifactId());
                dependencies.put(groupAndArtifact, dep);
            }
        });

        repositories.addAll(
                MavenUtil.convertRepositories(effectiveModel.getRepositories().stream()
                        .filter(rep -> rep.getId() != null && rep.getLayout() != null && rep.getUrl() != null)
                        .map(rep -> new RepositoryInfo(rep.getId(), rep.getUrl(), rep.getLayout()))
                        .collect(Collectors.toList()),
                        servers)
        );

        DependencyManagement depMan = effectiveModel.getDependencyManagement();
        if (depMan != null && depMan.getDependencies() != null) {
            dependencyManagement = depMan.getDependencies();
        } else {
            dependencyManagement = new ArrayList<>();
        }

        reconcileDependencyVersions();

        this.groupId = effectiveModel.getGroupId();
        this.artifactId = effectiveModel.getArtifactId();
        this.version = effectiveModel.getVersion();

        this.compilerOptions = new ArrayList<>();
        if (compilerOptions != null) {
            this.compilerOptions.addAll(compilerOptions);
        }
        String compilerSourceVersion = effectiveModel.getProperties().getProperty("maven.compiler.source");
        if (compilerSourceVersion != null) {
            this.compilerOptions.add(new CompilerOption("-source", compilerSourceVersion));
        }
        String compilerTargetVersion = effectiveModel.getProperties().getProperty("maven.compiler.target");
        if (compilerTargetVersion != null) {
            this.compilerOptions.add(new CompilerOption("-target", compilerTargetVersion));
        }
    }

    public Model getEffectiveModel() {
        return effectiveModel;
    }

    public ImmutableList<Path> getSourcePath() {
        return ImmutableList.copyOf(sourcePath);
    }

    public ImmutableList<Path> getTestSourcePath() {
        return ImmutableList.copyOf(testSourcePath);
    }

    public ImmutableMap<String, org.apache.maven.model.Dependency> getDependencies() {
        return ImmutableMap.copyOf(dependencies);
    }

    public Map<PackageIdentifier, List<JarSource>> resolveAndFetchTransitiveDependencies(Collection<PackageIdentifier> excludes, Map<String, Object> ctx, Span parent) {

        // Read transitive dependencies from cache if possible
        Map<PackageIdentifier, List<JarSource>> groupedDeps;
        if (Config.IGNORE_DEPENDENCY_RESOLUTION_CACHE) {
            groupedDeps = new HashMap<>();
            log.info("Dependency resolution cache ignored, resolving dependencies for {}:{}:{}", groupId, artifactId, version);
        } else {
            Span cachedDepsSpan = Tracing.startSpanFromContext(ctx, "resolution cache", parent);
            cachedDepsSpan.setTag("groupId", groupId);
            cachedDepsSpan.setTag("artifactId", artifactId);
            groupedDeps = loadTransitiveDependencyList(dependencies, excludes);
            if (groupedDeps != null) {
                Tracing.endSpan(cachedDepsSpan);
                setTransitiveDependencies(groupedDeps);
                return groupedDeps;
            } else {
                Tracing.endSpan(cachedDepsSpan);
                groupedDeps = new HashMap<>();
                log.info("Dependency resolution cache miss, resolving dependencies for {}:{}:{}", groupId, artifactId, version);
            }
        }

        Span fetchFromMavenSpan = Tracing.startSpanFromContext(ctx, "Maven resolution", parent);
        fetchFromMavenSpan.setTag("groupId", groupId);
        fetchFromMavenSpan.setTag("artifactId", artifactId);
        List<Map<PackageIdentifier, List<JarSource>>> rawGroupedDeps = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch l = new CountDownLatch(dependencies.size());
        for (org.apache.maven.model.Dependency dep : dependencies.values()) {
            AsyncUtils.runAsync(() -> {
                try {
                    if (excludes.contains(PackageIdentifier.ofMavenDep(dep))) {
                        return;
                    }
                    rawGroupedDeps.add(this.fetchTransitiveDeps(dep, excludes));
                } finally {
                    l.countDown();
                }
            }, ExecutorUtils.getArtifactsFetcherExecutorService());
        }
        Util.waitFor(l, "could not fetch transitive dependencies");
        Tracing.endSpan(fetchFromMavenSpan);

        Set<String> seenJars = Sets.newHashSet();
        for (Map<PackageIdentifier, List<JarSource>> pkgJars : rawGroupedDeps) {
            for (Map.Entry<PackageIdentifier, List<JarSource>> e : pkgJars.entrySet()) {
                PackageIdentifier pkg = e.getKey();
                List<JarSource> jars = e.getValue();
                for (JarSource jar : jars) {
                    if (!seenJars.add(jar.getFileName())) {
                        continue;
                    }
                    if (!groupedDeps.containsKey(pkg)) {
                        groupedDeps.put(pkg, Lists.newArrayList());
                    }
                    groupedDeps.get(pkg).add(jar);
                }
            }
        }

        // Write to cache
        saveTransitiveDependencyList(groupedDeps);
        setTransitiveDependencies(groupedDeps);
        return groupedDeps;
    }

    public List<JarSource> getSystemDependencies() {
        return systemArtifacts.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(JarFileSource::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public List<CompilerOption> getCompilerOptions() {
        return compilerOptions;
    }

    private Artifact fetchMavenArtifact(String coordinates) {
        Artifact desiredArtifact = new DefaultArtifact(coordinates);

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(desiredArtifact);
        request.setRepositories(repositories);

        try {
            return repositorySystem.resolveArtifact(repositorySystemSession, request).getArtifact();
        } catch (Exception exception) {
            messages.error("Unable to resolve artifact {}", coordinates);
            return null;
        }
    }

    /**
     * Fetches the set of transitive dependencies rooted at the given dependency, excluding the artifacts specified in
     * the `excludes` parameter.
     *
     * TODO: this is known to be incorrect (i.e., its behavior diverges from `mvn dependency:tree && mvn dependency:resolve`) in 2 ways:
     *   1) It may resolve dependencies to the incorrect version, which may cause compiler errors if the fetched version is incompatible with the correct dependency version
     *   2) Local dependencies may be incorrectly resolved to remote external dependencies. This may lead to compiler errors. Even if there are no compiler errors,
     *      this may still result in jump-to-def jumping to the wrong location (the location in the external dep, rather than the local one).
     */
    private Map<PackageIdentifier, List<JarSource>> fetchTransitiveDeps(org.apache.maven.model.Dependency dependency,
                                                                        Collection<PackageIdentifier> excludes) {
        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());

        Dependency aetherDep = new Dependency(artifact, JavaScopes.COMPILE, Boolean.valueOf(dependency.getOptional()));

        List<Exclusion> aetherExclusions = new ArrayList<>();

        Optional.ofNullable(dependency.getExclusions()).ifPresent(exclusions -> {
            for (org.apache.maven.model.Exclusion exclusion : exclusions) {
                Exclusion aetherExcl = new Exclusion(
                        exclusion.getGroupId(),
                        exclusion.getArtifactId(),
                        StringUtils.EMPTY,
                        "jar"
                );
                aetherExclusions.add(aetherExcl);
            }
        });
        for (PackageIdentifier exclude : excludes) {
            if (exclude == null || exclude.getId() == null) {
                continue;
            }
            String parts[] = StringUtils.split(exclude.getId(), ':');
            if (parts.length < 2) {
                log.warn("Not excluding package '{}' from Maven dependency fetching, because it did not have the requisite ID parts", exclude.getId());
                continue;
            }
            Exclusion aetherExcl = new Exclusion(
                    parts[0],
                    parts[1],
                    StringUtils.EMPTY,
                    "jar"
            );
            aetherExclusions.add(aetherExcl);
        }
        aetherDep = aetherDep.setExclusions(aetherExclusions);

        DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(aetherDep);
        collectRequest.setRepositories(repositories);

        CollectResult collectResult;
        try {
            collectResult = repositorySystem.collectDependencies(repositorySystemSession, collectRequest);
        } catch (DependencyCollectionException exception) {
            messages.error("Unable to collect transitive dependencies for {}", dependency, exception);
            collectResult = exception.getResult();
            if (collectResult == null) {
                return ImmutableMap.of();
            }
        } catch (Exception exception) {
            messages.error("Unable to collect transitive dependencies for {}", dependency, exception);
            return ImmutableMap.of();
        }

        if (collectResult.getRoot() == null) {
            return ImmutableMap.of();
        }

        DependencyRequest dependencyRequest = new DependencyRequest(collectResult.getRoot(), classpathFilter);

        try {
            DependencyResult dependencyResult;
            try {
                dependencyResult = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
            } catch (DependencyResolutionException exception) {
                dependencyResult = exception.getResult();
                if (dependencyResult == null) {
                    messages.error("Failed to resolve any transitive dependencies for {}: {}", dependency, exception);
                    return ImmutableMap.of();
                }
                messages.warn("Failed to resolve some transitive dependencies for {}: {}", dependency, exception.getMessage());
            }

            Map<PackageIdentifier, List<Artifact>> m = dependencyResult
                    .getArtifactResults()
                    .stream()
                    .map(ArtifactResult::getArtifact)
                    .filter(Objects::nonNull)
                    .map(this::replaceWithStipulatedVersion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(a -> PackageIdentifier.ofMaven(a.getGroupId(), a.getArtifactId(), a.getBaseVersion() != null ? a.getBaseVersion() : a.getVersion())));
            Map<PackageIdentifier, List<JarSource>> files = Maps.newHashMap();
            for (Map.Entry<PackageIdentifier, List<Artifact>> e : m.entrySet()) {
                ArrayList<JarSource> jarFiles = e.getValue().stream()
                        .map(Artifact::getFile)
                        .map(File::toString)
                        .map(JarSource::fromFileOrNull)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));
                files.put(e.getKey(), jarFiles);
            }

            return files;
        } catch (Exception exception) {
            messages.error("Unknown fatal exception resolving transitive dependencies for {}", dependency, exception);
            return ImmutableMap.of();
        }
    }

    private void setTransitiveDependencies(Map<PackageIdentifier, List<JarSource>> groupedDeps) {
        synchronized (isCompletelyResolved) {
            transitiveDependencies.addAll(groupedDeps.keySet());
            isCompletelyResolved.set(true);
        }
    }

    private void saveTransitiveDependencyList(Map<PackageIdentifier, List<JarSource>> groupedJars) {
        List<String> lines = new ArrayList<>();

        List<PackageIdentifier> unfetchables = new ArrayList<>();
        for (org.apache.maven.model.Dependency dependency : dependencies.values()) {
            PackageIdentifier packageIdentifier = PackageIdentifier.ofMavenDep(dependency);
            if (!groupedJars.containsKey(packageIdentifier)) {
                unfetchables.add(packageIdentifier);
            }
        }

        Path localRepoPath = Paths.get(Config.LOCAL_REPOSITORY.getAbsolutePath());

        groupedJars.forEach((pkgId, jars) -> {
            for (JarSource jar : jars) {
                String id = String.join(":", pkgId.getId(), pkgId.getVersion());
                String relPath = localRepoPath.relativize(Paths.get(jar.getFileName())).toString();
                String line = id + "\t" + relPath;
                lines.add(line);
            }
        });

        for (PackageIdentifier unfetchable : unfetchables) {
            String id = String.join(":", unfetchable.getId(), unfetchable.getVersion());
            lines.add(id + "\t");
        }

        String thisArtifact = String.join("_", groupId, artifactId, version);
        try {
            Path depListPath = Paths.get(Config.LOCAL_REPOSITORY.getAbsolutePath(), thisArtifact);
            Files.deleteIfExists(depListPath);
            Path depPath = Files.createFile(depListPath);
            Files.write(depPath, lines);
        } catch (IOException e) {
            messages.warn("Couldn't write dependencies file for {}: {}", thisArtifact, e);
        }
    }

    private Map<PackageIdentifier, List<JarSource>> loadTransitiveDependencyList(
            Map<String, org.apache.maven.model.Dependency> dependencies,
            Collection<PackageIdentifier> exclusions
    ) {
        String thisArtifact = String.join("_", groupId, artifactId, version);
        Path depList = Paths.get(Config.LOCAL_REPOSITORY.getAbsolutePath(), thisArtifact);
        if (!Files.exists(depList)) {
            return null;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(depList);
        } catch (IOException e) {
            messages.error("Error reading cached dependency list for {}; {}", thisArtifact, e);
            return null;
        }


        Set<PackageIdentifier> cachedPackageIds = new HashSet<>();
        for (String line : lines) {
            String pkgId = StringUtils.substringBefore(line, "\t");
            String[] pkgComponents = StringUtils.split(pkgId, ':');
            PackageIdentifier packageIdentifier = PackageIdentifier.ofMaven(pkgComponents[0], pkgComponents[1], pkgComponents[2]);
            cachedPackageIds.add(packageIdentifier);
        }
        // make sure each declared dependency is in the cached list, unless it's been excluded
        for (org.apache.maven.model.Dependency dependency : dependencies.values()) {
            PackageIdentifier depId = PackageIdentifier.ofMaven(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            if (!cachedPackageIds.contains(depId) && !exclusions.contains(depId)) {
                messages.warn("Cached list for {}:{}:{} is out of date; doesn't contain {}", groupId, artifactId, version, dependency);
                return null;
            }
        }

        // actually a concurrent hash map of pkgIds -> synchronized lists
        ConcurrentHashMap<PackageIdentifier, List<JarSource>> result = new ConcurrentHashMap<>();
        CountDownLatch jarsLoaded = new CountDownLatch(lines.size());
        for (String line : lines) {
            AsyncUtils.runAsync(() -> {
                try {
                    String pkgId = StringUtils.substringBefore(line, "\t");
                    String jarLoc = StringUtils.substringAfter(line, "\t");

                    String[] pkgComponents = StringUtils.split(pkgId, ':');
                    PackageIdentifier packageIdentifier = PackageIdentifier.ofMaven(pkgComponents[0], pkgComponents[1], pkgComponents[2]);
//                    cachedPackageIds.putIfAbsent(packageIdentifier, true);
                    if (jarLoc.trim().isEmpty()) {
                        return;
                    }
                    Path jarPath = Paths.get(Config.LOCAL_REPOSITORY.getAbsolutePath(), jarLoc);
                    JarSource jarFile = null;
                    if (Files.exists(jarPath)) {
                        try {
                            jarFile = JarSource.fromFile(jarPath.toString());
                        } catch (IOException e) {
                            messages.error("Error loading cached jar file {}; {}", jarPath, e);
                        }
                    } else {
                        Artifact artifact = fetchMavenArtifact(pkgId);
                        if (artifact != null) {
                            artifact = replaceWithStipulatedVersion(artifact);
                        }
                        if (artifact != null && artifact.getFile() != null) {
                            try {
                                jarFile = JarSource.fromFile(artifact.getFile().toString());
                            } catch (IOException e) {
                                messages.error("Error loading fetched jar file {}; {}", jarPath, e);
                            }
                        } else {
                            messages.warn("Couldn't fetch artifact {} from Maven", pkgId);
                        }
                    }
                    if (jarFile != null) {
                        result.computeIfAbsent(packageIdentifier, __ -> Collections.synchronizedList(new ArrayList<>())).add(jarFile);
                    }
                } finally {
                    jarsLoaded.countDown();
                }
            }, ExecutorUtils.getReasonableFetcherExecutorService());
        }

        Util.waitFor(jarsLoaded, "Interrupted while waiting for cached jars to be loaded");

        return result;
    }

    private void reconcileDependencyVersions() {
        dependencyManagement.forEach(managedDependency -> {
            String groupAndArtifact = String.join(":", managedDependency.getGroupId(), managedDependency.getArtifactId());
            org.apache.maven.model.Dependency dependency = dependencies.get(groupAndArtifact);
            if (dependency != null) {
                Optional.ofNullable(managedDependency.getVersion()).ifPresent(dependency::setVersion);
                Optional.ofNullable(managedDependency.getExclusions()).ifPresent(dependency::setExclusions);
                Optional.ofNullable(managedDependency.getOptional()).ifPresent(dependency::setOptional);
                Optional.ofNullable(managedDependency.getScope()).ifPresent(dependency::setScope);
                Optional.ofNullable(managedDependency.getSystemPath()).ifPresent(dependency::setSystemPath);
            }
        });
    }

    private Artifact replaceWithStipulatedVersion(Artifact artifact) {
        String groupAndArtifact = String.join(":", artifact.getGroupId(), artifact.getArtifactId());
        org.apache.maven.model.Dependency stipulatedDependency = dependencies.get(groupAndArtifact);
        if (stipulatedDependency != null && !stipulatedDependency.getVersion().equals(artifact.getVersion())) {
            log.debug(
                    "Conflicting dependency {}:{}; replacing with version {}",
                    groupAndArtifact,
                    artifact.getVersion(),
                    stipulatedDependency.getVersion()
            );
            return fetchMavenArtifact(String.join(":", groupAndArtifact, stipulatedDependency.getVersion()));
        } else {
            return artifact;
        }
    }

    private Optional<JarFile> fetchSystemArtifact(org.apache.maven.model.Dependency dep) {
        try {
            JarFile jarFile = new JarFile(Paths.get(dep.getSystemPath()).normalize().toString());
            log.debug("Loaded system dependency {}", jarFile.getName());
            return Optional.of(jarFile);
        } catch (IOException exception) {
            messages.warn("Error loading system dependency {}, systemPath was \"{}\"", dep, dep.getSystemPath());
            return Optional.empty();
        }
    }

    private RepositorySystem getRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                messages.error("Couldn't initialize Maven service");
                throw new RuntimeException(exception);
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession getRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(Config.LOCAL_REPOSITORY);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new MavenTransferListener());
        session.setRepositoryListener(new MavenRepositoryListener());
        return session;
    }

}
