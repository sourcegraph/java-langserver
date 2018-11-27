package com.sourcegraph.lsp;

public interface MessageHandlerServiceFactory {
    MessageHandlerService getMessageHandlerServiceForController(Controller controller);
}
