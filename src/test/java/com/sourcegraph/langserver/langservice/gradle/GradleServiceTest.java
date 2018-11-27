package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.sourcegraph.langserver.langservice.files.FileSystemFileProvider;
import com.sourcegraph.langserver.langservice.files.OverlayContentProvider;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.langserver.langservice.files.ResourceFileProvider;
import com.sourcegraph.langserver.langservice.maven.MavenUtil;
import com.sourcegraph.langserver.langservice.maven.MavenWorkspaceModelResolver;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.utils.LanguageUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by beyang on 3/6/17.
 */
public class GradleServiceTest {

    @Test
    public void testMockito() throws Exception {
        test("mockito");
    }

    @Test
    public void testPicasso() throws Exception {
        test("picasso");
    }

    @Test
    public void testPriam() throws Exception {
        test("priam");
    }

    @Test
    public void testJsonPatch() throws Exception {
        test("json-patch");
    }

    @Test
    public void testLeakCanary() throws Exception {
        test ("leakcanary");
    }

    @Test
    public void testIon() throws Exception {
        test("ion");
    }

    @Test
    public void testAndroidSupport() throws Exception {
        test("platform_frameworks_support");
    }

    @Test
    public void testElasticSearch() throws Exception {
        test("elasticsearch");
    }

    @Test
    public void testMPAndroidChart() throws Exception {
        test("MPAndroidChart");
    }

    @Test
    public void testTinker() throws Exception {
        test("tinker");
    }

    @Test
    public void testEventBus() throws Exception {
        test("EventBus");
    }

    @Test
    public void testGradleCustomRepo() throws Exception {
        test("gradle_custom_repo");
    }

    /**
     * test runs Gradle -> POM conversion for a given project and compares the output with its expected value.
     * The expected value is ready from the GRADLE2POM subdirectory of the resources directory.
     */
    private static void test(String projectName) throws Exception {

        Path root = Paths.get(Resources.getResource(projectName).toURI());
        FileContentProvider fs = new FileSystemFileProvider(root);
        Set<TextDocumentIdentifier> allFileUris = fs.listFilesRecursively("file:///").stream().collect(Collectors.toSet());

        Optional<ResourceFileProvider> overlayMaybe = ResourceFileProvider.forGradleWorkspace(allFileUris);
        if (overlayMaybe.isPresent()) {
            ResourceFileProvider overlay = overlayMaybe.get();
            List<TextDocumentIdentifier> overlayFiles = overlay.listFilesRecursively("file:///");
            allFileUris.addAll(overlayFiles);
            fs = new OverlayContentProvider(fs, overlay);
        }

        List<String> uris = allFileUris.stream().map(TextDocumentIdentifier::getUri).collect(Collectors.toList());
        HashSet<String> gradleUris = uris.stream()
                .filter(uri -> uri.endsWith(".gradle"))
                .collect(Collectors.toCollection(HashSet::new));
        Path pomFsRoot = Paths.get(Resources.getResource(Paths.get("GRADLE2POM").resolve(projectName).toString()).toURI());
        FileSystemFileProvider pomFs = new FileSystemFileProvider(pomFsRoot);
        List<String> expPomUris = pomFs.listFilesRecursively("file:///").stream().map(TextDocumentIdentifier::getUri).collect(Collectors.toList());
        Map<String, String> expPomContent = Maps.newHashMap();
        for (String expPomUri : expPomUris) {
            if (expPomContent.containsKey(expPomUri)) {
                throw new IllegalStateException("expPomContent already contains key " + expPomUri);
            }
            expPomContent.put(expPomUri, IOUtils.toString(pomFs.readContent(expPomUri), "UTF-8"));
        }

        GradleHierarchy gh = new GradleHierarchy(gradleUris, uris, fs, new MessageAggregator(null));
        Map<String, MavenWorkspaceModelResolver.PomInfo> pomInfos = gh.computeInferredPoms();
        Map<String, String> pomContent = Maps.newHashMap();
        Map<String, Model> pomModels = Maps.newHashMap();
        for (Map.Entry<String, MavenWorkspaceModelResolver.PomInfo> e : pomInfos.entrySet()) {
            String pomUri = LanguageUtils.pathToUri(LanguageUtils.uriToPath(e.getKey()).resolve("pom.xml").toString());
            pomContent.put(pomUri, MavenUtil.pomToString(e.getValue().rawModel));
            pomModels.put(pomUri, e.getValue().rawModel);
        }

        Path pomFsOutRoot = pomFsRoot.getParent().getParent().resolve("GRADLE2POM_actual").resolve(projectName);
        writePoms(pomFsOutRoot, pomModels);
        String matchMsg = String.join("\n", "expPomContent != pomContent", "",
                String.format("Run `diff -r ./src/test/resources/GRADLE2POM/%s ./target/test-classes/GRADLE2POM_actual/%s` to see the difference", projectName, projectName),
                String.format("To update expected contents, run `rsync -avh --delete-after ./target/test-classes/GRADLE2POM_actual/%s/ ./src/test/resources/GRADLE2POM/%s`", projectName, projectName),
                "", "");
        assertEquals(matchMsg, expPomContent, pomContent);
    }

    public static void writePoms(Path rootPath, Map<String, Model> poms) throws IOException {
        for (Map.Entry<String, Model> e : poms.entrySet()) {
            Model pom = e.getValue();
            String uri = e.getKey();
            Path path = LanguageUtils.uriToPath(uri);

            Path pomPath = rootPath.resolve(Paths.get("/").relativize(path));
            Path pomDirPath = pomPath.getParent();
            pomDirPath.toFile().mkdirs();
            pomPath.toFile().createNewFile();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(pomPath.toFile()))) {
                MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                try {
                    pomWriter.write(w, pom);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
    }
}
