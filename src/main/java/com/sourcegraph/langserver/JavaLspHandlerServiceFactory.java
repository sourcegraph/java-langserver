package com.sourcegraph.langserver;

import com.sourcegraph.lsp.Controller;

public class JavaLspHandlerServiceFactory {

    private boolean vfs;

    public JavaLspHandlerServiceFactory(boolean vfs) {
        this.vfs = vfs;
    }

    public JavaLspHandlerService newHandlerService(Controller controller) {
        return new JavaLspHandlerService(controller, vfs);
    }
}
