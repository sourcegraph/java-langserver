package com.sourcegraph.langserver.langservice.maven;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.MessageAggregator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Our custom implementation of Maven's WorkspaceModelResolver. This class keeps track of every POM in the workspace
 * hierarchy and is passed to each instance of {@link EffectivePom} to enable it to resolve references to other POMs
 * in the local repository when it builds the effective POM. (Otherwise, it would attempt to treat every dependency
 * artifact as a remote dependency.)
 *
 * This class is threadsafe.
 *
 * TODO(beyang): this class may be unnecessary now (what purpose does it serve other than to contain pomInfos?)
 */
public class MavenWorkspaceModelResolver implements WorkspaceModelResolver {

    private static final Logger log = LoggerFactory.getLogger(MavenWorkspaceModelResolver.class);

    public static MavenWorkspaceModelResolver newResolver(Collection<String> pomUris, FileContentProvider fileProvider, MessageAggregator messages) {
        Set<PomInfo> pomInfos = Sets.newHashSet();
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        for (String uri : pomUris) {
            log.trace("Processing pom.xml from {}", uri);
            String pomContent;
            try {
                pomContent = IOUtils.toString(fileProvider.readContent(uri), StandardCharsets.UTF_8);
            } catch (Exception e) {
                messages.warn("Failed to read POM content from {}: {}", uri, e.getMessage());
                continue;
            }
            try {
                Model rawModel = mavenReader.read(new StringReader(pomContent));
                // ensure that raw model passing Maven validation
                MavenUtil.dehydrate(rawModel);
                pomInfos.add(new PomInfo(StringUtils.removeEnd(uri, "pom.xml"), rawModel, ImmutableList.of()));
            } catch (IOException | XmlPullParserException e) {
                messages.warn("Failed to parse POM content from {}: {}", uri, e.getMessage());
            }
        }
        return new MavenWorkspaceModelResolver(pomInfos);
    }

    private ConcurrentHashMap<String, PomInfo> pomInfos;

    public MavenWorkspaceModelResolver(Collection<PomInfo> pomInfos) {
        this.pomInfos = new ConcurrentHashMap<>();
        for (PomInfo pomInfo : pomInfos) {
            String groupId = pomInfo.rawModel.getGroupId();
            if (groupId == null && pomInfo.rawModel.getParent() != null) {
                groupId = pomInfo.rawModel.getParent().getGroupId();
            }
            this.pomInfos.put(String.format("%s:%s", groupId, pomInfo.rawModel.getArtifactId()), pomInfo);
        }
    }

    public ImmutableMap<String, PomInfo> getPomInfos() {
        return ImmutableMap.copyOf(pomInfos);
    }

    @Override
    public Model resolveRawModel(String groupId, String artifactId, String versionConstraint) throws UnresolvableModelException {
        // TODO(beyang): deal with version resolution
        PomInfo pomInfo = pomInfos.get(MavenUtil.getModelId(groupId, artifactId));
        if (pomInfo == null) {
            return null;
        }
        pomInfo.rawModel.setPomFile(new File("DUMMY"));
        return pomInfo.rawModel;
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint) throws UnresolvableModelException {
        return null;
    }

    public static class PomInfo {
        public String workspaceUri; // uri to directory containing pom.xml
        public Model rawModel;
        public List<CompilerOption> compilerOptions; // extra compiler options if they exist

        public PomInfo(String workspaceUri, Model rawModel, List<CompilerOption> compilerOptions) {
            this.workspaceUri = workspaceUri;
            this.rawModel = rawModel;
            this.compilerOptions = compilerOptions;
        }
    }


}
