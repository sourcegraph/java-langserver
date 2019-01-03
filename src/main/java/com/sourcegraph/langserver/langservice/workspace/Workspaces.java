package com.sourcegraph.langserver.langservice.workspace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sourcegraph.langserver.langservice.gradle.GradleHierarchy;
import com.sourcegraph.langserver.langservice.maven.MavenUtil;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.langserver.langservice.javaconfigjson.Config;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibraries;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibrary;
import com.sourcegraph.langserver.langservice.maven.MavenWorkspaceModelResolver;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.lsp.Messenger;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.langserver.langservice.maven.EffectivePom;
import com.sourcegraph.utils.LanguageUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workspaces is a container class for utility functions that deal with workspaces. It should never be instantiated.
 * For the class that describes a collection of workspaces, see WorkspaceManager.
 */
public class Workspaces {

    private static final Logger log = LoggerFactory.getLogger(Workspaces.class);

    public static List<Workspace> fromFiles(String rootUri, FileContentProvider files, Messenger messenger, List<WorkspaceConfigurationServersResult.Server> servers) throws Exception {
        List<Workspace> ws = null;
        try {
            ws = fromJavaConfig(rootUri, files, messenger, servers);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                log.info("A javaconfig.json file was not found.");
            } else {
                log.warn("Unexpected error attempting to read javaconfig.json: {}", e);
            }
        } catch (IOException e) {
            log.info("A javaconfig.json file was not found.");
        } catch (Exception e) {
            log.warn("Unexpected error attempting to read javaconfig.json: {}", e);
        }

        List<TextDocumentIdentifier> allFiles = files.listFilesRecursively(rootUri);
        List<String> allUris = Lists.newArrayListWithCapacity(allFiles.size());
        List<String> pomUris = Lists.newArrayList();
        List<String> gradleUris = Lists.newArrayList();
        boolean hasStandardLayout = false;
        for (TextDocumentIdentifier id : allFiles) {
            String uri = id.getUri();
            allUris.add(uri);
            if (uri.endsWith("/pom.xml")) {
                pomUris.add(uri);
            }
            if (uri.startsWith("file:///src/main/java/")) {
                hasStandardLayout = true;
            }
            if (uri.endsWith(".gradle")) {
                gradleUris.add(uri);
            }
        }

        if (ws == null || ws.size() == 0) {
            ws = fromMaven(rootUri, pomUris, files, messenger, servers);
        }
        if (ws == null || ws.size() == 0) {
            ws = fromGradle(rootUri, gradleUris, allUris, files, messenger, servers);
        }
        if (ws == null || ws.size() == 0) {
            ws = standardLibraryWorkspace(rootUri, files);
        }
        if (ws == null || ws.size() == 0) {
            ws = defaultWorkspace(rootUri, hasStandardLayout, files);
        }
        if (ws == null) {
            return new ArrayList<>();
        }
        return ws;
    }

    public static List<Workspace> fromJavaConfig(String rootUri, FileContentProvider files, Messenger messenger, List<WorkspaceConfigurationServersResult.Server> servers) throws Exception {
        MessageAggregator msgs = new MessageAggregator(messenger, "Javaconfig: ");

        String configFile = LanguageUtils.concatPath(rootUri, "javaconfig.json");
        String configContents = IOUtils.toString(files.readContent(configFile), StandardCharsets.UTF_8);
        if (configContents == null || configContents.length() == 0) {
            return null;
        }
        Config config = Config.fromJavaConfig(configContents);

        Map<String, MavenWorkspaceModelResolver.PomInfo> pomInfos = new HashMap<>();
        Map<String, Model> rawModels = new HashMap<>();
        for (Map.Entry<String, Project> e : config.getProjects().entrySet()) {
            String wsDir = e.getKey();
            String wsUri = LanguageUtils.pathToUri(wsDir);
            Project project = e.getValue();
            Model rawModel = MavenUtil.javaConfigProjectToRawModel(project);
            MavenWorkspaceModelResolver.PomInfo pomInfo = new MavenWorkspaceModelResolver.PomInfo(wsUri, rawModel, project.getCompilerOptions());
            pomInfos.put(wsDir, pomInfo);
            rawModels.put(wsDir, rawModel);
        }
        MavenWorkspaceModelResolver mavenWorkspaceModelResolver = new MavenWorkspaceModelResolver(pomInfos.values());

        Map<String, EffectivePom> effectivePoms = new HashMap<>();
        for (Map.Entry<String, Project> e : config.getProjects().entrySet()) {
            String wsDir = e.getKey();
            Project project = e.getValue();
            EffectivePom effectivePom = EffectivePom.createAndResolve(wsDir, rawModels.get(wsDir), mavenWorkspaceModelResolver, project.getCompilerOptions(), msgs, servers);
            effectivePoms.put(wsDir, effectivePom);
        }

        List<Workspace> workspaces = new ArrayList<>();
        for (Map.Entry<String, EffectivePom> e : effectivePoms.entrySet()) {
            String wsDir = e.getKey();
            workspaces.add(new MavenWorkspace(files, wsDir, e.getValue(), effectivePoms.values()));
        }
        return workspaces;
    }

    public static List<Workspace> fromMaven(String rootUri, List<String> pomUris, FileContentProvider files, Messenger messenger, List<WorkspaceConfigurationServersResult.Server> servers) throws Exception {
        MessageAggregator msgs = new MessageAggregator(messenger, "Maven: ");
        MavenWorkspaceModelResolver mavenWorkspaceModelResolver = MavenWorkspaceModelResolver.newResolver(pomUris, files, msgs);

        Map<String, EffectivePom> effectivePoms = new HashMap<>();
        ImmutableMap<String, MavenWorkspaceModelResolver.PomInfo> pomInfos = mavenWorkspaceModelResolver.getPomInfos();
        for (Map.Entry<String, MavenWorkspaceModelResolver.PomInfo> e : pomInfos.entrySet()) {
            String workspaceUri = e.getValue().workspaceUri;
            String baseDir = LanguageUtils.relativePath(rootUri, workspaceUri);
            if (baseDir == null) {
                log.error("Could not resolve effective POM at {}: could not relativize workspace URI", workspaceUri);
                continue;
            }
            try {
                EffectivePom effectivePom = EffectivePom.createAndResolve(baseDir, e.getValue().rawModel, mavenWorkspaceModelResolver, e.getValue().compilerOptions, msgs, servers);
                effectivePoms.put(workspaceUri, effectivePom);
            } catch (Exception exc) {
                log.error("Could not resolve effective POM at {}: {}", workspaceUri, exc);
            }
        }

        List<Workspace> workspaces = new ArrayList<>();
        for (Map.Entry<String, EffectivePom> e : effectivePoms.entrySet()) {
            String baseDir = LanguageUtils.relativePath(rootUri, e.getKey());
            workspaces.add(new MavenWorkspace(files, baseDir, e.getValue(), effectivePoms.values()));
        }
        return workspaces;
    }

    public static List<Workspace> fromGradle(String rootUri, List<String> gradleUris, List<String> allUris, FileContentProvider files, Messenger messenger, List<WorkspaceConfigurationServersResult.Server> servers) throws Exception {
        if (gradleUris.size() == 0) {
            return null;
        }

        MessageAggregator msgs = new MessageAggregator(messenger, "Gradle: ");
        GradleHierarchy gradleHierarchy = new GradleHierarchy(gradleUris, allUris, files, msgs);
        Map<String, MavenWorkspaceModelResolver.PomInfo> inferredPoms = gradleHierarchy.computeInferredPoms();

        Map<String, EffectivePom> effectivePoms = new HashMap<>();
        MavenWorkspaceModelResolver mavenWorkspaceModelResolver = new MavenWorkspaceModelResolver(inferredPoms.values());
        ImmutableMap<String, MavenWorkspaceModelResolver.PomInfo> pomInfos = mavenWorkspaceModelResolver.getPomInfos();
        for (Map.Entry<String, MavenWorkspaceModelResolver.PomInfo> e : pomInfos.entrySet()) {
            String baseDir = LanguageUtils.uriToPath(e.getValue().workspaceUri).toString();
            EffectivePom effectivePom = EffectivePom.createAndResolve(baseDir, e.getValue().rawModel, mavenWorkspaceModelResolver, e.getValue().compilerOptions, msgs, servers);
            effectivePoms.put(e.getValue().workspaceUri, effectivePom);
        }

        List<Workspace> workspaces = new ArrayList<>();
        for (Map.Entry<String, EffectivePom> e : effectivePoms.entrySet()) {
            String baseDir = LanguageUtils.uriToPath(e.getKey()).toString();
            workspaces.add(new MavenWorkspace(files, baseDir, e.getValue(), effectivePoms.values()));
        }

        return workspaces;
    }

    public static List<Workspace> standardLibraryWorkspace(String rootUri, FileContentProvider files) {
        StandardLibrary standardLibrary = null;
        for (StandardLibrary library : StandardLibraries.getInstance().getLibraries()) {
            try {
                for (String uri: library.getSentinelUris()) {
                    files.readContent(uri);
                }
                standardLibrary = library;
            } catch (Exception e) {
                // ignore
            }
        }
        if (standardLibrary == null) {
            return null;
        }

        Project configuration = standardLibrary.getConfiguration();
        if (configuration == null) {
            return null;
        }
        JavaConfigWorkspace ws = new JavaConfigWorkspace(files, configuration, LanguageUtils.uriToPath(rootUri).toString());
        return Lists.newArrayList(ws);
    }

    public static List<Workspace> defaultWorkspace(String rootUri, boolean hasStandardLayout, FileContentProvider files) throws Exception {
        if (!hasStandardLayout) {
            return ImmutableList.of();
        }
        return ImmutableList.of(new JavaConfigWorkspace(files, Project.newDefaultProject(), "/"));
    }

}
