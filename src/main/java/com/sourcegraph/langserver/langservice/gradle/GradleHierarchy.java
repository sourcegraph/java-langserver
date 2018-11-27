package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.langserver.langservice.maven.MavenUtil;
import com.sourcegraph.utils.LanguageUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import com.sourcegraph.langserver.langservice.maven.MavenWorkspaceModelResolver;

/**
 * A hierarchy of Gradle scripts (typically one per WorkspaceHierarchy), rooted at a settings.gradle file.
 * <p>
 * Created by beyang on 3/7/17.
 */
public class GradleHierarchy {

    private static Logger log = LoggerFactory.getLogger(Fradle.class);

    private static final ImmutableList<String> sourceSourceSets = ImmutableList.of("main.java.srcDirs", "main.java.srcDir");
    private static final ImmutableList<String> testSourceSets = ImmutableList.of("test.java.srcDirs", "test.java.srcDir", "androidTest.java.srcDir", "androidTest.java.srcDirs");

    private HashSet<String> gradleUris;
    private HashSet<String> allUris;
    private FileContentProvider fileProvider;
    private MessageAggregator messages;

    public GradleHierarchy(Collection<String> gradleUris, Collection<String> allUris, FileContentProvider fileProvider, MessageAggregator messages) {
        this.gradleUris = Sets.newHashSet(gradleUris);
        this.allUris = Sets.newHashSet(allUris);
        this.fileProvider = fileProvider;
        this.messages = messages;
    }

    public static class FatalGradleException extends Exception {
        public FatalGradleException(Throwable e) {
            super(e);
        }
    }

    /**
     * computeInferredPomModels returns a map from workspace URI to inferred POM
     */
    public Map<String, MavenWorkspaceModelResolver.PomInfo> computeInferredPoms() {
        // Find all potential Gradle hierarchy roots (indicated by presence of `settings.gradle` for multi-project hierarchies or
        // `build.gradle` for single projects).
        List<String> rootsMaybe = gradleUris
                .stream()
                .filter(path -> path.endsWith("/build.gradle") || path.endsWith("/settings.gradle"))
                .map(path -> StringUtils.substringBeforeLast(path, "/"))
                .sorted(Comparator.comparing(path -> StringUtils.countMatches(path, '/')))
                .collect(Collectors.toList());

        // Filter down to actual roots (don't count as a root any directory that's a descendant of another root.
        // Note: this is a heuristic; there may be peculiar projects with multiple settings.gradle's that overlap in
        // directory hierarchy.
        Set<String> roots = new HashSet<>();
        for (String rootMaybe : rootsMaybe) {
            // if folder was already seen, all its subfolders are considered seen.
            boolean seen = false;
            String current = rootMaybe;
            while (true) {
                if (roots.contains(current)) {
                    seen = true;
                    break;
                }
                int pos = current.lastIndexOf('/');
                if (pos < 0) {
                    break;
                }
                current = current.substring(0, pos);
            }
            if (!seen) {
                roots.add(rootMaybe);
            }
        }

        Map<String, MavenWorkspaceModelResolver.PomInfo> poms = Maps.newHashMap();
        for (String root : roots) {
            poms.putAll(computeInferredPomsAtRoot(root));
        }
        return poms;
    }

    /**
     * Computes inferred POM(s) from a given Gradle hierarchy root.
     *
     * @param rootUri     workpace subtree root (directory) that contains root Gradle file(s)
     */
    private Map<String, MavenWorkspaceModelResolver.PomInfo> computeInferredPomsAtRoot(String rootUri) {
        Map<String, MavenWorkspaceModelResolver.PomInfo> poms = Maps.newHashMap();

        List<Fradle.Globals> globalEnvironments = collectProjectRoots(rootUri);

        int projectNumber = 0;
        for (Fradle.Globals globals : globalEnvironments) {
            // Transform to poms
            ArrayList<Project> projects = Lists.newArrayList(globals.rootProject);
            projects.addAll(globals.subProjects.values());
            for (Project project : projects) {
                Model model = new Model();
                model.setModelVersion("4.0.0");

                if (project.depManagement.size() > 0) {
                    DependencyManagement depMgt = new DependencyManagement();
                    for (String depString : project.depManagement) {
                        Dependency dep;
                        try {
                            dep = GradleUtil.toDependency(depString, "import", "pom");
                        } catch (Exception e) {
                            log.error("failed to parse dependency management dependency {} due to error", depString, e);
                            continue;
                        }
                        depMgt.addDependency(dep);
                    }
                    model.setDependencyManagement(depMgt);
                }

                Map<String, List<String>> projDeps = project.getDependencies();
                collectDependencies(projDeps.get("compile"), "compile", model);
                collectDependencies(projDeps.get("testCompile"), "test", model);
                collectDependencies(projDeps.get("androidTestCompile"), "test", model);
                collectDependencies(projDeps.get("provided"),"compile", model);

                for (String repo : project.getRepositories()) {
                    Repository repository = new Repository();
                    repository.setId(repo);
                    repository.setName(repo);
                    repository.setUrl(repo);
                    model.addRepository(repository);
                }

                Map<String, String> projIdentifier = project.getIdentifier();
                model.setGroupId(projIdentifier.get("group"));
                model.setArtifactId(projIdentifier.get("artifact"));
                model.setVersion(projIdentifier.get("version"));

                {
                    List<String> sourceDirs = Lists.newArrayList();
                    List<String> testSourceDirs = Lists.newArrayList();

                    for (String sourceDir : sourceSourceSets) {
                        if (project.getAttribute(sourceDir) instanceof List) {
                            for (Object o : (List) project.getAttribute(sourceDir)) {
                                sourceDirs.add(o.toString());
                            }
                        }
                    }
                    for (String testDir : testSourceSets) {
                        if (project.getAttribute(testDir) instanceof List) {
                            for (Object o : (List) project.getAttribute(testDir)) {
                                testSourceDirs.add(o.toString());
                            }
                        }
                    }

                    if (sourceDirs.size() > 0 || testSourceDirs.size() > 0) {
                        Build build = new Build();
                        if (sourceDirs.size() > 0) {
                            build.setSourceDirectory(String.join(",", sourceDirs));
                        }
                        if (testSourceDirs.size() > 0) {
                            build.setTestSourceDirectory(String.join(",", testSourceDirs));
                        }
                        model.setBuild(build);
                    }
                }

                String projectRootDir = project.getAttribute("rootDir").toString();
                Path projectRootPath = Paths.get(projectRootDir);

                Path projectPath = projectRootPath.resolve(project.getAttribute("projectDir").toString());
                if (Paths.get("/").relativize(projectPath).normalize().startsWith("../")) {
                    // ignore paths that point outside repository
                    continue;
                }
                String projectUri = LanguageUtils.pathToUri(projectPath.normalize().toString());
                poms.put(projectUri, new MavenWorkspaceModelResolver.PomInfo(projectUri, model, project.getCompilerOptions()));

                // set the desired Java source and target versions, if specified in the Gradle project
                String sourceVersion = null;
                if (project.getAttribute("sourceCompatibility") != null) {
                    sourceVersion = project.getAttribute("sourceCompatibility").toString();
                }
                String targetVersion = null;
                if (project.getAttribute("targetCompatibility") != null) {
                    targetVersion = project.getAttribute("targetCompatibility").toString();
                }
                if (sourceVersion != null || targetVersion != null) {
                    Properties modelProperties = new Properties();
                    if (sourceVersion != null) {
                        modelProperties.setProperty("maven.compiler.source", sourceVersion);
                    }
                    if (targetVersion != null) {
                        modelProperties.setProperty("maven.compiler.target", targetVersion);
                    }
                    model.setProperties(modelProperties);
                }
            }

            // Final fill-in of missing data
            for (MavenWorkspaceModelResolver.PomInfo pom : poms.values()) {
                Model model = pom.rawModel;
                if (model.getGroupId() == null && model.getArtifactId() != null && model.getArtifactId().contains(":")) {
                    String groupId = StringUtils.substringBefore(model.getArtifactId(), ":");
                    String artifactId = StringUtils.substringAfter(model.getArtifactId(), ":");
                    model.setArtifactId(artifactId);
                    model.setGroupId(groupId);
                }
                if (model.getArtifactId() == null) {
                    model.setArtifactId("UNKNOWN_ARTIFACT_" + projectNumber);
                }
                if (model.getVersion() == null) {
                    model.setVersion(MavenUtil.UNKNOWN_VERSION);
                }
                if (model.getGroupId() == null) {
                    model.setGroupId(MavenUtil.UNKNOWN_GROUP);
                }
                projectNumber++;
            }
        }

        return poms;
    }

    /**
     * Fills model with the dependencies extracted
     *
     * @param dependencies list of dependency identifiers
     * @param scope        dependency scope (compile, test, etc)
     * @param model        model to fill with the dependencies collected
     */
    private void collectDependencies(List<String> dependencies, String scope, Model model) {
        for (String depString : dependencies) {
            if (depString == null) continue;
            Dependency dep;
            try {
                dep = GradleUtil.toDependency(depString, scope, null);
            } catch (Exception e) {
                log.error("failed to parse dependency {} due to error", depString, e);
                continue;
            }
            if (dep.getArtifactId() == null || dep.getGroupId() == null) {
                log.error("ignoring incomplete dependency {}", depString);
                continue;
            }
            model.addDependency(dep);
        }
    }

    /**
     * Reads properties from a gradle.properties (or other *.properties) file.
     */
    private Map<String, Object> readProperties(String propertiesUri) throws Exception {
        if (!allUris.contains(propertiesUri)) {
            return Maps.newHashMap();
        }
        Map<String, Object> props = Maps.newHashMap();
        String[] lines = IOUtils.toString(fileProvider.readContent(propertiesUri), "UTF-8").split("\n");
        for (String line : lines) {
            if (line.indexOf("=") == -1) {
                continue;
            }
            String k = line.substring(0, line.indexOf("="));
            if (k.startsWith("#")) {
                continue;
            }
            String v = line.substring(line.indexOf("=") + 1);
            props.put(k, v);
        }
        return props;
    }

    /**
     * Collects all Gradle hierarchy roots under a given path -- a path is considered a root if it either contains a
     * settings.gradle file, or if it contains an "orphan" build.gradle file that isn't under any settings.gradle file.
     * @param rootUri
     */
    private List<Fradle.Globals> collectProjectRoots(String rootUri) {

        HashSet<String> projectRoots = gradleUris.stream()
                .map(LanguageUtils::uriToPath)
                .filter(path -> path.getFileName().toString().equals("settings.gradle"))
                .map(path -> path.getParent().toString())
                .collect(Collectors.toCollection(HashSet::new));

        HashSet<String> moreRoots = new HashSet<>();

        for (String gradleUri : gradleUris) {
            boolean orphan = true;
            Path gradlePath = LanguageUtils.uriToPath(gradleUri);
            for (String projectRoot : projectRoots) {
                if (gradlePath.startsWith(Paths.get(projectRoot))) {
                    orphan = false;
                    break;
                }
            }
            if (orphan) {
                moreRoots.add(gradlePath.getParent().toString());
            }
        }

        projectRoots.addAll(moreRoots);

        return projectRoots.stream()
                .map(LanguageUtils::pathToUri)
                .map(subProjectRoot -> {
                    try {
                        return collectProjects(subProjectRoot);
                    } catch (FatalGradleException e) {
                        log.error("Error initializing Gradle sub-project under {}", subProjectRoot, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Collects all project metadata in a given Gradle script hierarchy.
     */
    private Fradle.Globals collectProjects(String rootUri) throws FatalGradleException {
        // Map out hierarchy from settings.gradle
        String rootPath = LanguageUtils.uriToPath(rootUri).toString();
        Fradle.Globals globals = new Fradle.Globals(rootPath);
        Fradle.Scope scope = Fradle.Scope.of(globals);

        String settingsUri = LanguageUtils.pathToUri(LanguageUtils.joinPath(LanguageUtils.uriToPath(rootUri).toString(), "settings.gradle"));
        if (gradleUris.contains(settingsUri)) {
            try {
                Fradle.runGradle(settingsUri, fileProvider, globals, scope, messages);
            } catch (Exception e) {
                throw new FatalGradleException(e);
            }
        }
        for (Project proj : globals.subProjects.values()) {
            proj.parent = globals.rootProject;
        }

        // Property attribute default vals initialization
        for (Project proj : globals.getAllProjects()) {
            // for a complete list of Project properties, see the "Properties" section of
            // https://docs.gradle.org/current/dsl/org.gradle.api.Project.html
            proj.setAttribute("project", proj);
            proj.setAttribute("rootProject", globals.rootProject);
            proj.setAttribute("rootDir", rootPath);  // relative to Gradle hierarchy root dir
        }

        // Load properties from gradle.properties
        String rootPropsUri = LanguageUtils.pathToUri(LanguageUtils.joinPath(
                rootPath,
                Objects.toString(globals.rootProject.getAttribute("projectDir")),
                "gradle.properties"
        ));
        try {
            Map<String, Object> rootProps = readProperties(rootPropsUri);
            globals.rootProject.setAllProperties(rootProps);
        } catch (Exception e) {
            log.error("failed to read properties from " + rootPropsUri);
        }
        for (Project subProject : globals.subProjects.values()) {
            String propsUri = LanguageUtils.pathToUri(LanguageUtils.joinPath(
                    rootPath,
                    Objects.toString(subProject.getAttribute("projectDir")),
                    "gradle.properties"
            ));
            try {
                subProject.setAllProperties(readProperties(propsUri));
            } catch (Exception e) {
                log.error("failed to read properties from " + propsUri);
            }
        }

        // Other *.properties files set default values on projects (NOTE: this is a heuristic and may not be 100% accurate)
        try {
            String versionPropsUri = LanguageUtils.pathToUri(LanguageUtils.uriToPath(rootUri).resolve("version.properties").toString());
            Map<String, Object> versionProps = readProperties(versionPropsUri);
            if (versionProps.containsKey("version")) {
                String version = versionProps.get("version").toString();
                for (Project proj : globals.getAllProjects()) {
                    proj.setAttribute("version", version);
                }
            }
        } catch (Exception e) {/* ignore if "version.properties" doesn't exist */}

        // Run root project script
        String buildUri = LanguageUtils.pathToUri(LanguageUtils.joinPath(
                rootPath,
                Objects.toString(globals.rootProject.getAttribute("projectDir")),
                Objects.toString(globals.rootProject.getAttribute("buildFileName"))
        ));
        try {
            Fradle.runGradle(buildUri, fileProvider, globals, scope, messages);
        } catch (Exception e) {
            throw new FatalGradleException(e);
        }

        // Run subproject scripts
        for (Project subProject : globals.subProjects.values()) {
            String subBuildUri = LanguageUtils.pathToUri(LanguageUtils.joinPath(
                    rootPath,
                    Objects.toString(subProject.getAttribute("projectDir")),
                    Objects.toString(subProject.getAttribute("buildFileName"))
            ));
            HashMap<String, Object> context = Maps.newHashMap();
            context.put("project", subProject);
            scope.push(Fradle.TOP_LEVEL_SCOPE, Lists.newArrayList(context));
            try {
                Fradle.runGradle(subBuildUri, fileProvider, globals, scope, messages);
            } catch (Exception e) {
                log.error("failed to run Gradle for subProject " + subProject.getAttribute("name") + ": " + e.getMessage());
            }
            scope.pop();
        }

        return globals;
    }
}
