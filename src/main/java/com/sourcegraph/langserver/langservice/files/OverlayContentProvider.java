package com.sourcegraph.langserver.langservice.files;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.Messenger;
import com.sourcegraph.lsp.domain.params.MessageType;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * OverlayContentProvider overlays files in the resources directory on top of a base FileContentProvider
 */
public class OverlayContentProvider implements FileContentProvider {

    private static final Logger log = LoggerFactory.getLogger(OverlayContentProvider.class);

    /**
     * ROOT_BUILD_GRADLE_URI and ROOT_POM_XML_URI are the conventional locations of the root configuration files for
     * Gradle and Maven projects, respectively.
     */
    private static final TextDocumentIdentifier ROOT_BUILD_GRADLE_URI = new TextDocumentIdentifier().withUri("file:///build.gradle");
    private static final TextDocumentIdentifier ROOT_POM_XML_URI = new TextDocumentIdentifier().withUri("file:///pom.xml");

    public static FileContentProvider withOverlays(FileContentProvider fileProvider, String rootUri, Messenger msgr) throws Exception {
        Set<TextDocumentIdentifier> allFileIdentifiers = new HashSet<>(fileProvider.listFilesRecursively(rootUri));
        Optional<ResourceFileProvider> optionalOverlay = Optional.empty();

        if (allFileIdentifiers.contains(ROOT_POM_XML_URI)) {
            // Maven overrides
            try {
                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                Model rawModel = mavenReader.read(fileProvider.readContent("file:///pom.xml"));
                String id = String.format("%s/%s", rawModel.getGroupId(), rawModel.getArtifactId());
                optionalOverlay = ResourceFileProvider.forMavenArtifact(id);
            } catch (IOException | XmlPullParserException e) {
                msgr.showMessage(MessageType.WARNING, "We failed to parse the POM in this project, so some code navigation may fail. To fix, add a javaconfig.json file to your project (https://sourcegraph.com/help/languages).");
                log.warn("Failed to parse POM content from file:///pom.xml: {}", e.getMessage());
            }
        } else {
            if (allFileIdentifiers.contains(ROOT_BUILD_GRADLE_URI)) {
                // Gradle overrides
                optionalOverlay = ResourceFileProvider.forGradleWorkspace(allFileIdentifiers);
            } else {
                // Other overrides (including Ant)
                optionalOverlay = ResourceFileProvider.forAntWorkspace(allFileIdentifiers);
            }
        }

        if (optionalOverlay.isPresent()) {
            ResourceFileProvider overlay = optionalOverlay.get();
            log.info("Using overlay {}:{}", overlay.getType(), overlay.getId());
            List<TextDocumentIdentifier> overlayFiles = overlay.listFilesRecursively(rootUri);
            allFileIdentifiers.addAll(overlayFiles);
            fileProvider = new OverlayContentProvider(fileProvider, overlay);
        }

        return fileProvider;
    }

    private List<FileContentProvider> overlays;
    private FileContentProvider base;

    public OverlayContentProvider(FileContentProvider base, FileContentProvider... overlays) {
        this.base = base;
        this.overlays = Lists.newArrayList(overlays);
    }

    @Override
    public InputStream readContent(String uri) throws Exception {
        for (FileContentProvider overlay : overlays) {
            try {
                return overlay.readContent(uri);
            } catch (Exception e) {}
        }
        return base.readContent(uri);
    }


    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String uri) throws Exception {
        HashSet<TextDocumentIdentifier> ret = Sets.newHashSet(base.listFilesRecursively(uri));
        for (FileContentProvider overlay : overlays) {
            try {
                ret.addAll(overlay.listFilesRecursively(uri));
            } catch (Exception e) {}
        }
        return Lists.newArrayList(ret);
    }
}
