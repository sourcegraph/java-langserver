package com.sourcegraph.langserver.langservice.workspace;

import com.google.common.collect.Lists;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.StandardLibraries;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sourcegraph.utils.ExecutorUtils;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import org.apache.commons.validator.routines.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * SHIM (Simple Hashed In-Memory) File Manager
 * ---
 * Javac invokes file manager methods in order to fetch class files from packages, as well as open files for input
 * and output. We can override that logic here in order to fetch dependencies on-demand and provide them in-memory.
 * Although there are no hash tables in this particular class (despite its name), rest assured that there are plenty
 * of hash tables in the classes that it relies on.
 *
 * Note that each workspace gets its own file manager, and each file manager has a reference to its workspace. This
 * allows a file manager to ask its workspace for all (and only) those files that are implemented or imported by that
 * workspace (or any of the workspaces that it depends on transitively, because we can compute a workspace's internal
 * dependencies).
 */
public class ShimFileManager extends JavacFileManager {

    private static final boolean doLog = System.getenv("LOG_ShimFileManager2") != null;
    private static final Logger log = LoggerFactory.getLogger(ShimFileManager.class);

    private static final DomainValidator domainValidator = DomainValidator.getInstance();

    private final Workspace workspace;

    public ShimFileManager(Context context, Workspace workspace) {
        super(context, true, null);
        this.workspace = workspace;
    }

    @Override
    public Iterable<JavaFileObject> list(
            Location location,
            String packageName,
            Set<JavaFileObject.Kind> kinds,
            boolean recurse
    ) throws IOException {
        Iterable<JavaFileObject> files;

        long start = 0;
        if (doLog) {
            start = System.currentTimeMillis();
        }

        // If the requested package belongs to a standard library, bypass the workspace and delegate the request to
        // the system file manager (which will return class files directly from platform jars). This greatly speeds up
        // performance for things like OpenJDK and Android-SDK, which contain several thousand source files.
        if (location.getName().equals("CLASS_PATH")
                && !domainValidator.isValidTld(packageName)
                && !StandardLibraries.isStandardLibraryPackage(packageName)) {
            files = getTransitivePackageFileObjects(packageName);
        } else {
            files = super.list(location, packageName, kinds, recurse);
        }

        if (doLog) {
            List<JavaFileObject> fileList = Lists.newArrayList(files);
            files = fileList;
            log.info("list({}, {}, {}, {}) yielded {} JavaFileObjects ({}ms)", location, packageName, kinds, recurse, fileList.size(), System.currentTimeMillis() - start);
        }

        return files;
    }

    private List<JavaFileObject> getTransitivePackageFileObjects(String packageName) {

        // Get my source files
        List<JavaFileObject> files;
        try {
            files = Lists.newArrayList(workspace.getPackageSourceFileObjects(packageName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Get source files from local dependency workspaces
        PackageIdentifier workspaceId = workspace.getThisArtifactInformation().getPackage().getIdentifier();
        Set<Workspace> workspaceDeps = workspace.getWorkspaceManager().getInternalDependencies(workspaceId);
        workspaceDeps.stream()
                .map(depWorkspace -> {
                    try {
                        return depWorkspace.getPackageSourceFileObjects(packageName);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Collection::stream)
                .forEach(files::add);

        // Do the same for class files, but after the source files -- for packages that are implemented by this
        // project, this gives javac a chance to compile the source files rather than use external class files, which
        // allows us to provide correct code intelligence.
        ArrayList<CompletableFuture<Set<JavaFileObject>>> futurePackages = new ArrayList<>();
        futurePackages.add(
                CompletableFuture.supplyAsync(
                        () -> workspace.getJARPackageFileObjects(packageName),
                        ExecutorUtils.getArtifactsFetcherExecutorService()
                )
        );

        workspaceDeps.stream()
                .map(depWorkspace-> (Supplier<Set<JavaFileObject>>) () -> depWorkspace.getJARPackageFileObjects(packageName))
                .map(packageSupplier -> CompletableFuture.supplyAsync(packageSupplier, ExecutorUtils.getArtifactsFetcherExecutorService()))
                .forEach(futurePackages::add);
        futurePackages.forEach(futurePackage -> files.addAll(futurePackage.join()));

        return files;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (location.getName().equals("PLATFORM_CLASS_PATH")) {
            return super.inferBinaryName(location, file);
        } else if (file instanceof WorkspaceFile) {
            return ((WorkspaceFile) file).getBinaryName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }
}
