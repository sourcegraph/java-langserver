package com.sourcegraph.lsp.domain.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import com.sourcegraph.lsp.domain.structures.ClientCapabilities;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializeParams {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InitializationOptions {
        private List<WorkspaceConfigurationServersResult.Server> servers;

        public List<WorkspaceConfigurationServersResult.Server> getServers() {
            return servers;
        }

        public void setServers(List<WorkspaceConfigurationServersResult.Server> servers) {
            this.servers = servers;
        }
    }

    private String rootPath = null;

    private String rootUri = null;

    private String originalRootUri = null;

    private ClientCapabilities capabilities;

    private InitializationOptions initializationOptions;

    public String getRootPath() {
        return rootPath;
    }

    @SuppressWarnings("unused") // JSON deserialization
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootUri() {
        return rootUri;
    }

    public void setRootUri(String rootUri) {
        this.rootUri = rootUri;
    }

    public String getOriginalRootUri() {
        return originalRootUri;
    }

    @SuppressWarnings("unused") // JSON deserialization
    public void setOriginalRootUri(String originalRootUri) {
        this.originalRootUri = originalRootUri;
    }

    public ClientCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public InitializationOptions getInitializationOptions() {
        return initializationOptions;
    }

    public void setInitializationOptions(InitializationOptions initializationOptions) {
        this.initializationOptions = initializationOptions;
    }
}
