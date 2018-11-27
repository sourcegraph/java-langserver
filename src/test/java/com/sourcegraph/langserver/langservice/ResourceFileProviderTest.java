package com.sourcegraph.langserver.langservice;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sourcegraph.langserver.langservice.files.ResourceFileProvider;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Created by beyang on 2/6/17.
 */
public class ResourceFileProviderTest {
    @Test
    public void testFoundResourceFileProviderMaven() throws Exception {
        Optional<ResourceFileProvider> fileProviderMaybe = ResourceFileProvider.forMavenArtifact("test-artifact");
        Assert.assertTrue("no resource overlay found for test-artifact", fileProviderMaybe.isPresent());
        ResourceFileProvider fileProvider = fileProviderMaybe.get();

        Set<TextDocumentIdentifier> files = Sets.newHashSet(fileProvider.listFilesRecursively("file:///"));
        Set<TextDocumentIdentifier> expectedFiles = Sets.newHashSet(
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/util/Util.java"),
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/main/Main.java")
        );
        Assert.assertEquals(String.format("expected %s, but got %s", expectedFiles, files), expectedFiles, files);

        ImmutableMap<String, String> expectedContents = ImmutableMap.of(
                "file:///src/main/java/com/sourcegraph/main/Main.java", "Main.java contents",
                "file:///src/main/java/com/sourcegraph/util/Util.java", "Util.java contents"
        );

        for (TextDocumentIdentifier file : files) {
            String expContent = expectedContents.get(file.getUri());
            String content = IOUtils.toString(fileProvider.readContent(file.getUri()), StandardCharsets.UTF_8);
            Assert.assertEquals(String.format("expected \"%s\", but got \"%s\"", expContent, content), expContent, content);
        }
    }

    @Test
    public void testResourceFileProviderGradle() throws Exception {
        Optional<ResourceFileProvider> fileProviderMaybe = ResourceFileProvider.forGradleWorkspace(ImmutableSet.of(new TextDocumentIdentifier().withUri("file:///TEST_ARTIFACT_SENTINEL")));
        Assert.assertTrue("no resource overlay found for test-artifact", fileProviderMaybe.isPresent());
        ResourceFileProvider fileProvider = fileProviderMaybe.get();

        Set<TextDocumentIdentifier> files = Sets.newHashSet(fileProvider.listFilesRecursively("file:///"));
        Set<TextDocumentIdentifier> expectedFiles = Sets.newHashSet(
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/util/Util.java"),
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/main/Main.java")
        );
        Assert.assertEquals(String.format("expected %s, but got %s", expectedFiles, files), expectedFiles, files);

        ImmutableMap<String, String> expectedContents = ImmutableMap.of(
                "file:///src/main/java/com/sourcegraph/main/Main.java", "Main.java contents",
                "file:///src/main/java/com/sourcegraph/util/Util.java", "Util.java contents"
        );

        for (TextDocumentIdentifier file : files) {
            String expContent = expectedContents.get(file.getUri());
            String content = IOUtils.toString(fileProvider.readContent(file.getUri()), StandardCharsets.UTF_8);
            Assert.assertEquals(String.format("expected \"%s\", but got \"%s\"", expContent, content), expContent, content);
        }
    }

    @Test
    public void testResourceFileProviderAnt() throws Exception {
        Optional<ResourceFileProvider> fileProviderMaybe = ResourceFileProvider.forAntWorkspace(ImmutableSet.of(new TextDocumentIdentifier().withUri("file:///TEST_ARTIFACT_SENTINEL")));
        Assert.assertTrue("no resource overlay found for test-artifact", fileProviderMaybe.isPresent());
        ResourceFileProvider fileProvider = fileProviderMaybe.get();

        Set<TextDocumentIdentifier> files = Sets.newHashSet(fileProvider.listFilesRecursively("file:///"));
        Set<TextDocumentIdentifier> expectedFiles = Sets.newHashSet(
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/util/Util.java"),
                new TextDocumentIdentifier().withUri("file:///src/main/java/com/sourcegraph/main/Main.java")
        );
        Assert.assertEquals(String.format("expected %s, but got %s", expectedFiles, files), expectedFiles, files);

        ImmutableMap<String, String> expectedContents = ImmutableMap.of(
                "file:///src/main/java/com/sourcegraph/main/Main.java", "Main.java contents",
                "file:///src/main/java/com/sourcegraph/util/Util.java", "Util.java contents"
        );

        for (TextDocumentIdentifier file : files) {
            String expContent = expectedContents.get(file.getUri());
            String content = IOUtils.toString(fileProvider.readContent(file.getUri()), StandardCharsets.UTF_8);
            Assert.assertEquals(String.format("expected \"%s\", but got \"%s\"", expContent, content), expContent, content);
        }
    }

    @Test
    public void testRepoFileProviderNotExist() {
        Optional<ResourceFileProvider> providerMaybe = ResourceFileProvider.forMavenArtifact("non-existent-artifact");
        Assert.assertTrue("ResourceFileProvider instance should not have been returned for non-existent-artifact", !providerMaybe.isPresent());
    }
}
