package com.sourcegraph.langserver.langservice.maven;

import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Project model resolver that looks first in the workspace for model source.
 * We can't properly implement MavenWorkspaceModelResolver::resolveEffectiveModel
 * because sub-projects may be processed asynchronously so Maven will use custom project resolver to build effective
 * models of POM dependencies
 */
public class MavenModelResolver extends ProjectModelResolver {

    private RepositorySystem repositorySystem;
    private RepositorySystemSession repositorySystemSession;
    private List<RemoteRepository> remoteRepositoryList;
    private MavenWorkspaceModelResolver mavenWorkspaceModelResolver;

    MavenModelResolver(RepositorySystemSession repositorySystemSession,
                       RepositorySystem repositorySystem,
                       List<RemoteRepository> remoteRepositoryList,
                       MavenWorkspaceModelResolver mavenWorkspaceModelResolver) {
        super(repositorySystemSession,
                null,
                repositorySystem,
                new DefaultRemoteRepositoryManager(),
                remoteRepositoryList,
                ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT,
                null);
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteRepositoryList = remoteRepositoryList;
        this.mavenWorkspaceModelResolver = mavenWorkspaceModelResolver;
    }

    private MavenModelResolver(MavenModelResolver source) {
        this(source.repositorySystemSession,
                source.repositorySystem,
                source.remoteRepositoryList,
                source.mavenWorkspaceModelResolver);
    }

    @Override
    public ModelResolver newCopy() {
        return new MavenModelResolver(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        MavenWorkspaceModelResolver.PomInfo info = mavenWorkspaceModelResolver
                .getPomInfos()
                .get(MavenUtil.getModelId(groupId, artifactId));
        if (info != null) {
            StringWriter writer = new StringWriter();
            ModelWriter modelWriter = new DefaultModelWriter();
            try {
                modelWriter.write(writer, null, info.rawModel);
            } catch (IOException e) {
                return super.resolveModel(groupId, artifactId, version);
            }
            return new StringModelSource(writer.toString(), info.workspaceUri);
        }
        return super.resolveModel(groupId, artifactId, version);
    }

}
