package com.sourcegraph.langserver.langservice.maven;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class MavenRepositoryListener extends AbstractRepositoryListener {

    private static final Logger log = LoggerFactory.getLogger(MavenRepositoryListener.class);

    @Override
    public void artifactDownloaded(RepositoryEvent event) {
        if (event.getExceptions().isEmpty()) {
            log.trace("Downloaded artifact {}", event.getArtifact());
        } else {
            // debug log level because it's normal situation:
            // we may try to download A from repos [R1, R2] where R1 does not contain A while R2 does
            // there is no sense to pollute logs with errors
            log.trace("Failed to download artifact {}: {}",
                    event.getArtifact(),
                    event.getExceptions()
                            .stream()
                            .map(Throwable::getMessage)
                            .collect(Collectors.toCollection(LinkedList::new)));
        }
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        log.trace("Installed artifact {}", event.getArtifact());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        log.trace("Resolved artifact {}", event.getArtifact());
    }
}
