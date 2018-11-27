package com.sourcegraph.langserver.langservice.workspace;

import javax.tools.JavaFileObject;
import java.nio.file.Path;

public interface WorkspaceFile extends JavaFileObject {
    String getBinaryName();
    Path getSourcePath();
}
