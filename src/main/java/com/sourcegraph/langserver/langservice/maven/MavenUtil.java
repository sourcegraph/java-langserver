package com.sourcegraph.langserver.langservice.maven;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sourcegraph.common.Config;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.langserver.langservice.javaconfigjson.RepositoryInfo;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.*;
import org.apache.maven.model.building.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility functions for manipulating Maven-related data
 *
 * Created by beyang on 3/12/17.
 */
public final class MavenUtil {
    public static final String UNKNOWN_GROUP = "UNKNOWN_GROUP";
    public static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";

    public static Model javaConfigProjectToRawModel(Project project) {
        Model rawModel = new Model();
        rawModel.setArtifactId(project.getArtifactId());
        rawModel.setGroupId(project.getGroupId());
        rawModel.setVersion(project.getVersion());
        Build build = new Build();
        build.setSourceDirectory(String.join(",", project.getSourceDirectories())); // TODO(beyang): test more than one source directory
        build.setTestSourceDirectory(String.join(",", project.getTestSourceDirectories())); // TODO(beyang): test more than one test source directory
        rawModel.setBuild(build);

        List<Dependency> deps = new ArrayList<>();
        for (Project.Dep dep : project.getDependencies()) {
            Dependency dependency = new Dependency();
            dependency.setGroupId(dep.getGroupId());
            dependency.setArtifactId(dep.getArtifactId());
            dependency.setVersion(dep.getVersion());
            dependency.setClassifier(dep.getClassifier());
            deps.add(dependency);
        }
        rawModel.setDependencies(deps);

        List<Repository> repositories = new ArrayList<>();
        for (Project.Repo r : project.getRepositories()) {
            Repository repository = new Repository();
            repository.setId(r.getId());
            repository.setName(r.getName());
            repository.setUrl(r.getUrl());
            repository.setLayout(r.getLayout());
            repositories.add(repository);
        }
        rawModel.setRepositories(repositories);

        rawModel.setModelVersion("4.0.0");

        return rawModel;
    }

    public static String pomToString(Model pom) {
        StringWriter w = new StringWriter();
        try {
            new MavenXpp3Writer().write(w, pom);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return w.toString();
    }

    @SuppressWarnings("unused") // debug utility
    public static String pomsToString(Map<String, MavenWorkspaceModelResolver.PomInfo> poms) {
        String s = "";
        for (Map.Entry<String, MavenWorkspaceModelResolver.PomInfo> e : poms.entrySet()) {
            s += e.getKey() + "\n" + pomToString(e.getValue().rawModel) + "\n\n";
        }
        return s;
    }

    public static List<RemoteRepository> convertRepositories(Collection<RepositoryInfo> repos, List<WorkspaceConfigurationServersResult.Server> servers) {
        Map<String, List<WorkspaceConfigurationServersResult.Server>> serversById = servers.stream().collect(Collectors.groupingBy(s -> s.getId()));
        ArrayList<RemoteRepository> remoteRepos = new ArrayList<>(repos.size());
        HashSet<String> seenIds = new HashSet<>();
        for (RepositoryInfo repo : repos) {
            seenIds.add(repo.getId());
            WorkspaceConfigurationServersResult.Server server = serversById.containsKey(repo.getId()) ? serversById.get(repo.getId()).get(0) : null;
            if (server != null) {
                remoteRepos.add(newRemoteRepository(repo.getId(), repo.getLayout(), repo.getUrl(), server.getUsername(), server.getPassword()));
            } else {
                remoteRepos.add(newRemoteRepository(repo.getId(), repo.getLayout(), repo.getUrl(), null, null));
            }
        }

        // Add mandatory servers. A server is mandatory if it has a defaultUrl field.
        // We do this mainly to maintain backward compatibility with the old PRIVATE_REPO_ID/PRIVATE_REPO_URL env vars.
        List<WorkspaceConfigurationServersResult.Server> mandatoryServers = servers.stream().filter(s -> s.getDefaultUrl() != null && !s.getDefaultUrl().isEmpty()).collect(Collectors.toList());
        for (WorkspaceConfigurationServersResult.Server s : mandatoryServers) {
            if (seenIds.contains(s.getId())) {
                continue;
            }
            remoteRepos.add(newRemoteRepository(s.getId(), "default", s.getDefaultUrl(), s.getUsername(), s.getPassword()));
        }

        return remoteRepos;
    }

    private static RemoteRepository newRemoteRepository(String id, String layout, String url, String username, String password) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, layout, url);
        if (username != null && password != null) {
            builder.setAuthentication(new AuthenticationBuilder()
                    .addUsername(username)
                    .addPassword(password)
                    .build());
        }
        return builder.build();
    }

    /**
     * Builds effective model from a raw one. Before building ensures that raw model passes Maven validation,
     * then invokes effective model building, and finally restores temporary changed data
     */
    public static Model buildEffectiveModel(Model rawModel,
                                            String projectBaseDir,
                                            MavenWorkspaceModelResolver mavenWorkspaceModelResolver,
                                            RepositorySystem repositorySystem,
                                            RepositorySystemSession repositorySystemSession,
                                            List<WorkspaceConfigurationServersResult.Server> servers) throws ModelBuildingException {

        Map<Object, List<WorkspaceConfigurationServersResult.Server>> serversById = servers.stream().collect(Collectors.groupingBy(s -> s.getId()));

        Set<String> tmpRemoteRepoUrls = new HashSet<>();
        List<RemoteRepository> tmpRemoteRepos = new ArrayList<>();
        if (rawModel.getRepositories() != null) {
            for (Repository repository : rawModel.getRepositories()) {
                // ignoring file repositories, we don't want to provide access to the local FS
                if (repository.getUrl().startsWith("file:")) {
                    continue;
                }
                log.debug("Adding Maven repository `{}` at {}", repository.getId(), repository.getUrl());
                RemoteRepository.Builder remoteRepoBuilder = new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl());
                WorkspaceConfigurationServersResult.Server server = serversById.containsKey(repository.getId()) ? serversById.get(repository.getId()).get(0) : null;
                if (server != null) {
                    remoteRepoBuilder.setAuthentication(new AuthenticationBuilder()
                            .addUsername(server.getUsername())
                            .addPassword(server.getPassword()).build());
                }
                RemoteRepository remoteRepo = remoteRepoBuilder.build();
                tmpRemoteRepos.add(remoteRepo);
                tmpRemoteRepoUrls.add(repository.getUrl());
            }
        }
        for (Repository r : CANONICAL_REPOSITORIES) {
            if (!tmpRemoteRepoUrls.contains(r.getUrl())) {
                RemoteRepository.Builder builder = new RemoteRepository.Builder(r.getId(), "default", r.getUrl());
                tmpRemoteRepos.add(builder.build());
            }
        }

        Properties properties = new Properties();
        properties.setProperty("project.basedir", projectBaseDir);
        // The following properties are deprecated per http://maven.apache.org/ref/3-LATEST/maven-model-builder/
        // but still being used :(
        properties.setProperty("pom.basedir", projectBaseDir);
        properties.setProperty("basedir", projectBaseDir);

        MavenModelResolver modelResolver = new MavenModelResolver(repositorySystemSession,
                repositorySystem,
                tmpRemoteRepos,
                mavenWorkspaceModelResolver);
        ModelBuildingRequest request = new DefaultModelBuildingRequest()
                .setWorkspaceModelResolver(mavenWorkspaceModelResolver)
                .setRawModel(rawModel)
                .setSystemProperties(System.getProperties())
                .setUserProperties(properties)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setModelResolver(modelResolver)
                .setTwoPhaseBuilding(false);

        Model effectiveModel;
        try {
            ModelBuildingResult result = new DefaultModelBuilderFactory().newInstance().build(request);
            effectiveModel = result.getEffectiveModel();
        } catch (ModelBuildingException ex) {
            effectiveModel = ex.getResult().getEffectiveModel();
            if (effectiveModel == null) {
                throw ex;
            }
            log.warn("There were some problems building effective model for {}: {}", projectBaseDir, ex.getMessage());
        }
        if (effectiveModel == null) {
            throw new RuntimeException("Unable to build effective model");
        }
        MavenUtil.saturate(effectiveModel);

        Set<String> repositoryUrls = Sets.newHashSet();
        for (Repository r : effectiveModel.getRepositories()) {
            repositoryUrls.add(r.getUrl());
        }
        for (Repository c : CANONICAL_REPOSITORIES) {
            if (!repositoryUrls.contains(c.getUrl())) {
                effectiveModel.addRepository(c);
            }
        }

        return effectiveModel;
    }

    /**
     * Saturates Maven model from dehydrated one
     * - removes fake prefixes from system dependencies
     *
     * @param model model to saturate
     */
    public static void saturate(Model model) {
        model.getDependencies().forEach(MavenUtil::saturate);
        DependencyManagement management = model.getDependencyManagement();
        if (management != null) {
            management.getDependencies().forEach(MavenUtil::saturate);
        }
    }

    /**
     * Makes Maven model pass Maven validation. For example,
     * Maven expects system dependencies point to absolute path after interpolation
     * however project-based libraries won't be in case of VFS.
     * - adds fake prefixes to system dependencies
     *
     * @param model model to dehydrate
     */
    static void dehydrate(Model model) {
        model.getDependencies().forEach(MavenUtil::dehydrate);
        DependencyManagement management = model.getDependencyManagement();
        if (management != null) {
            management.getDependencies().forEach(MavenUtil::dehydrate);
        }
    }

    private static void dehydrate(org.apache.maven.model.Dependency dependency) {
        if ("system".equals(dependency.getScope()) && dependency.getSystemPath() != null) {
            dependency.setSystemPath(wrapSystemPath(dependency.getSystemPath()));
        }
    }

    private static void saturate(org.apache.maven.model.Dependency dependency) {
        if ("system".equals(dependency.getScope()) && dependency.getSystemPath() != null) {
            dependency.setSystemPath(unwrapSystemPath(dependency.getSystemPath()));
        }
    }

    /**
     * Making VFS path absolute to make Maven validator happy
     */
    private static String wrapSystemPath(String systemPath) {
        return FileUtils.getUserDirectoryPath() + systemPath;
    }

    /**
     * Stripping fake absolute path prefix from the VFS path
     */
    private static String unwrapSystemPath(String systemPath) {
        return StringUtils.substringAfter(systemPath, FileUtils.getUserDirectoryPath());
    }

    // TODO: handle versions someday
    public static String getModelId(String groupId, String artifactId) {
        return String.format("%s:%s", groupId, artifactId);
    }

    /**
     * CANONICAL_REPOSITORIES are Maven repositories that are *always* included in the generated POM. This may
     * technical not be correct (a POM that declares explicit <repositories> may not technically include the
     * standard repositories), but in practice, including these does not hurt correctness and in fact may help in
     * cases where there is an implicit reliance on standard repositories.
     */
    private static final List<Repository> CANONICAL_REPOSITORIES;
    static {
        Repository central = new Repository();
        central.setId("maven_central_autoinclude");
        central.setName("central");
        central.setUrl("https://repo.maven.apache.org/maven2");
        // turn off snapshot-checking
//        org.apache.maven.model.RepositoryPolicy mavenCentralPolicy = new org.apache.maven.model.RepositoryPolicy();
//        mavenCentralPolicy.setEnabled(false);
//        mavenCentralPolicy.setUpdatePolicy("never");
//        central.setSnapshots(mavenCentralPolicy);

        Repository jcenter = new Repository();
        jcenter.setId("jcenter_autoinclude");
        jcenter.setName("jcenter");
        jcenter.setUrl("https://jcenter.bintray.com");
        // turn off snapshot-checking
//        org.apache.maven.model.RepositoryPolicy jcenterPolicy = new org.apache.maven.model.RepositoryPolicy();
//        jcenterPolicy.setEnabled(false);
//        jcenterPolicy.setUpdatePolicy("never");
//        jcenter.setSnapshots(jcenterPolicy);

        CANONICAL_REPOSITORIES = ImmutableList.of(central, jcenter);
    }

    private static final Logger log = LoggerFactory.getLogger(MavenUtil.class);
}
