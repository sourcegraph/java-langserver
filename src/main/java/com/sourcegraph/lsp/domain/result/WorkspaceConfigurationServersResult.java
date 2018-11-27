package com.sourcegraph.lsp.domain.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceConfigurationServersResult extends ArrayList<ArrayList<WorkspaceConfigurationServersResult.Server>> {
    public static class Server {
        private String id;
        private String username;
        private String password;

        private String defaultUrl; // special field, used to support backcompat for PRIVATE_REPOO_URL env var

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDefaultUrl() {
            return defaultUrl;
        }

        public void setDefaultUrl(String defaultUrl) {
            this.defaultUrl = defaultUrl;
        }
    }
}
