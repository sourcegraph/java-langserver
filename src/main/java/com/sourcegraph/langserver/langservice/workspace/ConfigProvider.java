package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.langserver.langservice.javaconfigjson.Project;

public interface ConfigProvider {
    Project getConfig();
}
