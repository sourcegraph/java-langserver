package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.params.MessageType;

public class NoopMessenger implements Messenger {
    @Override
    public void showMessage(MessageType messageType, String message) {

    }
}
