package com.sourcegraph.lsp;

import com.sourcegraph.lsp.domain.params.MessageType;

/**
 * Messenger is an interface through which to pass displayable (user-visible) messages.
 *
 * Created by beyang on 7/26/17.
 */
public interface Messenger {
    void showMessage(MessageType messageType, String message);
}
