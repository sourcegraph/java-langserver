package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.lsp.Messenger;
import com.sourcegraph.lsp.domain.params.MessageType;

/**
 * Created by beyang on 7/26/17.
 */
public class TestMessenger implements Messenger {
    @Override
    public void showMessage(MessageType messageType, String message) {
        // no-op
    }
}
