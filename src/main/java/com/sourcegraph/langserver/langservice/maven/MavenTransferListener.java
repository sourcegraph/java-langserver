package com.sourcegraph.langserver.langservice.maven;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenTransferListener extends AbstractTransferListener {

    private static final Logger log = LoggerFactory.getLogger(MavenTransferListener.class);

    @Override
    public void transferSucceeded(TransferEvent event) {
        log.trace(
                "Successfully transferred {} bytes from {} for {}",
                event.getTransferredBytes(),
                event.getResource().getRepositoryUrl(),
                event.getResource().getResourceName()
        );
    }

    @Override
    public void transferFailed(TransferEvent event) {
        // debug log level because it's normal situation:
        // we may try to download A from repos [R1, R2] where R1 does not contain A while R2 does
        // there is no sense to pollute logs with errors
        log.trace(
                "Transfer from {} failed for {}",
                event.getResource().getRepositoryUrl(),
                event.getResource().getResourceName()
        );
    }
}
