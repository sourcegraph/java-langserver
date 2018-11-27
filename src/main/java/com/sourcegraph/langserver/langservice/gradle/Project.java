package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sourcegraph.common.Config;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.utils.AndroidUtil;

import java.util.*;

/**
 * Project represents a Gradle project (see https://docs.gradle.org/current/dsl/org.gradle.api.Project.html).
 */
public class Project {

    public Project parent;
    private Map<String, Object> attributes = Maps.newHashMap();
    private Map<String, Object> properties = Maps.newHashMap();
    public Map<String, List<Object>> deps = ImmutableMap.of(
            "compile", Lists.newArrayList(),
            "testCompile", Lists.newArrayList(),
            "androidTestCompile", Lists.newArrayList(),
            "provided", Lists.newArrayList()
    );
    public List<String> repos = Lists.newArrayList();
    public List<String> plugins = Lists.newArrayList();
    public List<String> depManagement = Lists.newArrayList();

    public void addDependency(String scope, Object projectOrString) {
        if (!(projectOrString instanceof String || projectOrString instanceof Project)) {
            throw new IllegalArgumentException("Project.addDependency: projectOrString must be Project or String");
        }
        if (!deps.containsKey(scope)) {
            throw new IllegalArgumentException("scope unrecognized (" + scope + ")");
        }
        deps.get(scope).add(projectOrString);
    }

    public void addRepository(String url) {
        this.repos.add(url);
    }

    public List<CompilerOption> getCompilerOptions() {
        for (String plugin : plugins) {
            if ("com.android.library".equals(plugin) || "com.android.application".equals(plugin)) {
                if (Config.ANDROID_JAR_PATH != null) {
                    return Lists.newArrayList(new CompilerOption("-bootclasspath", Config.ANDROID_JAR_PATH));
                }
            }
        }
        return Lists.newArrayList();
    }

    public Map<String, List<String>> getDependencies() {
        HashMap<String, List<String>> stringifiedDeps = Maps.newHashMapWithExpectedSize(deps.size());
        collectExternalDependencies(this, stringifiedDeps, new HashSet<>());
        return stringifiedDeps;
    }

    public ImmutableList<String> getRepositories() {
        return ImmutableList.copyOf(repos);
    }

    /**
     * Collects external (non-project) dependencies for a given project
     * @param project project to process
     * @param accum accumulator to store collected dependencies
     * @param visited set to track visited projects to avoid cycles
     */
    private static void collectExternalDependencies(Project project, Map<String, List<String>> accum, Set<String> visited) {
        String id = project.getIdentifierString();
        if (!visited.add(id)) {
            return;
        }
        for (Map.Entry<String, List<Object>> e : project.deps.entrySet()) {
            String configuration = e.getKey();
            List<String> dependencies = accum.get(configuration);
            if (dependencies == null) {
                dependencies = new LinkedList<>();
                accum.put(configuration, dependencies);
            }
            for (Object o : e.getValue()) {
                if (o instanceof String) {
                    dependencies.add((String) o);
                } else if (o instanceof Project) {
                    dependencies.add(((Project) o).getIdentifierString());
                } else {
                    throw new IllegalStateException("dependency was neither Project nor String " + o.getClass());
                }
            }
        }
    }

    /**
     * getIdentifierString returns a string in the form $GROUP:$ARTIFACT or $GROUP:$ARTIFACT:$VERSION
     */
    public String getIdentifierString() {
        Map<String, String> identifier = getIdentifier();
        ArrayList<String> cmps = Lists.newArrayListWithCapacity(3);
        cmps.add(identifier.get("group"));
        cmps.add(identifier.get("artifact"));
        if (identifier.get("version") != null) {
            cmps.add(identifier.get("version"));
        }
        return String.join(":", cmps);
    }

    /**
     * getIdentifier returns a map with no more than 3 elements with keys "group", "artifact", and "version"
     */
    public Map<String, String> getIdentifier() {
        HashMap<String, String> identifier = Maps.newHashMap();

        // Scan exact properties all the way up the project chain
        for (Project proj = this; proj != null; proj = proj.parent) {
            HashMap<String, Object> attrsAndProps = Maps.newHashMap(attributes);
            attrsAndProps.putAll(properties);
            if (!identifier.containsKey("artifact") && attrsAndProps.containsKey("pom.artifactId")) {
                identifier.put("artifact", attrsAndProps.get("pom.artifactId").toString());
            }
            if (!identifier.containsKey("artifact") && attrsAndProps.containsKey("artifactName")) {
                identifier.put("artifact", attrsAndProps.get("artifactName").toString());
            }
            if (!identifier.containsKey("artifact") && attrsAndProps.containsKey("archivesBaseName")) {
                identifier.put("artifact", attrsAndProps.get("archivesBaseName").toString());
            }
            if (!identifier.containsKey("artifact") && attrsAndProps.containsKey("name")) {
                identifier.put("artifact", attrsAndProps.get("name").toString());
            }
            if (!identifier.containsKey("group") && attrsAndProps.containsKey("pom.groupId")) {
                identifier.put("group", attrsAndProps.get("pom.groupId").toString());
            }
            if (!identifier.containsKey("group") && attrsAndProps.containsKey("group")) {
                identifier.put("group", attrsAndProps.get("group").toString());
            }
            if (!identifier.containsKey("version") && attrsAndProps.containsKey("pom.version")) {
                identifier.put("version", attrsAndProps.get("pom.version").toString());
            }
            if (!identifier.containsKey("version") && attrsAndProps.containsKey("version")) {
                identifier.put("version", attrsAndProps.get("version").toString());
            }
        }

        // Scan "best-effort" properties
        HashMap<String, Object> attrsAndProps = Maps.newHashMap(getAttributes());
        attrsAndProps.putAll(getProperties());
        for (Map.Entry<String, Object> e : attrsAndProps.entrySet()) {
            if (!identifier.containsKey("artifact") && (e.getKey().contains("artifact") || "name".equals(e.getKey()))) {
                identifier.put("artifact", e.getValue().toString());
            } else if (!identifier.containsKey("group") && e.getKey().contains("group")) {
                identifier.put("group", e.getValue().toString());
            } else if (!identifier.containsKey("version") && e.getKey().contains("version")) {
                identifier.put("version", e.getValue().toString());
            }
        }

        // Special-case kludges
        if (AndroidUtil.groupToDir.containsKey(identifier.get("group")) && identifier.get("version") == null) {
            if (attrsAndProps.get("supportedVersion") != null) {
                identifier.put("version", attrsAndProps.get("supportVersion").toString());
            }
        }

        return identifier;
    }

    public Object getAttribute(String k) {
        if (k.startsWith("ext.")) {
            k = k.substring("ext.".length());
        }
        if (attributes.containsKey(k)) {
            return attributes.get(k);
        }
        if (parent != null) {
            return parent.getAttribute(k);
        }
        return null;
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> attrs = Maps.newHashMap();
        if (parent != null) {
            attrs.putAll(parent.getAttributes());
        }
        attrs.putAll(attributes);
        return ImmutableMap.copyOf(attrs);
    }

    public void setAttribute(String k, Object v) {
        attributes.put(k, v);
    }

    public Object getProperty(String k) {
        if (properties.containsKey(k)) {
            return properties.get(k);
        }
        if (parent != null) {
            return parent.getProperty(k);
        }
        return null;
    }

    public void setProperty(String k, Object v) {
        properties.put(k, v);
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> props = Maps.newHashMap();
        if (parent != null) {
            props.putAll(parent.getProperties());
        }
        props.putAll(properties);
        return ImmutableMap.copyOf(props);
    }

    public void setAllProperties(Map<String, Object> newProps) {
        properties.putAll(newProps);
    }
}
