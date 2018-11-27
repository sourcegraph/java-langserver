package com.sourcegraph.langserver.langservice.workspace;

import com.google.common.collect.Lists;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by beyang on 12/22/17.
 */
public class WorkspaceManager {

    private static Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    // Map from workspace root directory (where "/" is the repository root directory) to Workspace.
    private Map<PackageIdentifier, Workspace> workspaces;
    private FileContentProvider files;

    public WorkspaceManager(List<Workspace> workspaces, FileContentProvider files) {
        this.workspaces = new HashMap<>();
        for (Workspace ws : workspaces) {
            PackageIdentifier id = ws.getThisArtifactInformation().getPackage().getIdentifier();
            if (this.workspaces.containsKey(id)) {
                log.error("Duplicate package identifier {} found. Ignoring.", id);
                continue;
            }
            this.workspaces.put(id, ws);
            ws.setWorkspaceManager(this);
        }
        this.files = files;
    }

    public Workspace getWorkspaceContainingUri(String uri) {
        for (Workspace workspace : workspaces.values()) {
            try {
                if (workspace.containsSourceFile(uri)) {
                    return workspace;
                }
            } catch (Exception e) {
                log.error("Exception getting workspace for uri {}: {}", uri, e);
                continue;
            }
        }
        return null;
    }

    public List<Workspace> getWorkspaces() {
        return Lists.newArrayList(workspaces.values());
    }

    public PackageIdentifier getArtifactorIdentifier(URI fileObjectUri) {
        for (Workspace ws : workspaces.values()) {
            PackageIdentifier id = ws.getArtifactIdentifier(fileObjectUri);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private Map<PackageIdentifier, Set<PackageIdentifier>> localDependencyTree = new ConcurrentHashMap<>();

    public Set<Workspace> getInternalDependencies(PackageIdentifier workspaceIdentifier) {
        Set<Workspace> deps = new HashSet<>();
        for (PackageIdentifier p : getInternalDependencyIds(workspaceIdentifier)) {
            if (workspaces.containsKey(p)) {
                deps.add(workspaces.get(p));
            }
        }
        return deps;
    }

    private Set<PackageIdentifier> getInternalDependencyIds(PackageIdentifier workspaceIdentifier) {
        if (localDependencyTree.containsKey(workspaceIdentifier)) {
            return localDependencyTree.get(workspaceIdentifier);
        }
        return computeInternalDependencies(workspaceIdentifier, new HashSet<>());
    }

    /**
     * Helper function for the above; this does the actual work by recursively traversing workspaces and their
     * coordinates, and keeps track of visited workspaces with an extra parameter, in case the dependency graph
     * contains cycles.
     *
     * @param workspaceCoordinates The coordinates of the workspace for which to construct the transitive dependency list.
     * @param visited A set of packages indicating which workspace have been visited already.
     * @return A set of coordinates of workspaces that the given workspace transitively depends upon.
     */
    private Set<PackageIdentifier> computeInternalDependencies(
            PackageIdentifier workspaceCoordinates,
            Set<PackageIdentifier> visited
    ) {
        if (workspaceCoordinates == null || visited.contains(workspaceCoordinates)) {
            return Collections.emptySet();
        }

        visited.add(workspaceCoordinates);

        Workspace workspace = workspaces.get(workspaceCoordinates);

        if (workspace == null) {
            return Collections.emptySet();
        }

        // get the immediate dependencies of this workspace, and keep only the internal ones
        Set<PackageIdentifier> dependencyCoordinates = new HashSet<>(workspace.getDependencies());
        dependencyCoordinates.retainAll(workspaces.keySet());

        // recursively get the transitive dependencies of the aforementioned immediate internal dependencies
        Set<PackageIdentifier> transitiveDependencyCoordinates = dependencyCoordinates.stream()
                .map(coordinates -> computeInternalDependencies(coordinates, visited))
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(HashSet::new));

        // return the union
        dependencyCoordinates.addAll(transitiveDependencyCoordinates);

        localDependencyTree.putIfAbsent(workspaceCoordinates, dependencyCoordinates);

        return dependencyCoordinates;
    }

}
