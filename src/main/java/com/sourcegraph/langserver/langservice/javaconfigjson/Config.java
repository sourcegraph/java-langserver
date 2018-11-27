package com.sourcegraph.langserver.langservice.javaconfigjson;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by beyang on 12/21/17.
 */
public class Config {

    private static ObjectMapper mapper = new ObjectMapper();

    public Map<String, Project> projects;

    public Config() {
        this.projects = new HashMap<>();
    }

    public Map<String, Project> getProjects() {
        return projects;
    }

    public void setProjects(Map<String, Project> projects) {
        this.projects = projects;
    }

    public static Config fromJavaConfig(String javaconfigJSON) throws Exception {
        return mapper.readValue(javaconfigJSON, Config.class);
    }
}
