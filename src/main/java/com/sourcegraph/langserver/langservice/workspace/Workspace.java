package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sourcegraph.lsp.domain.structures.PackageInformation;
import org.apache.commons.lang3.StringUtils;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Workspace encapsulates the raw file contents of a build unit. It is the primary interface that stands between a build
 * system (and build-system-adjacent classes) and the rest of the language service.
 *
 * It serves two purposes:
 * - Provide access to the file objects in a given build unit (as requested by the language service and compiler).
 *   This includes fetching dependencies if necessary.
 * - Provide metadata about the build unit (e.g., artifact ID)
 */
public interface Workspace {
    String getRootURI();
    Set<String> getSourceUris() throws Exception;
    Set<JavaFileObject> getSourceFiles();
    Set<JavaFileObject> getPackageSourceFileObjects(String packageName) throws Exception;
    Set<JavaFileObject> getPackageFileObjects(String packageName) throws IOException;
    Set<JavaFileObject> getJARPackageFileObjects(String packageName);


    /**
     * All source file access should go through this method. Implementations should fetch the contents at most
     * once if necessary, so this function can be called anytime, as many times as desired without concern
     * for performance.
     *
     * @param uri The uri of the desired source file
     * @return the source file
     */
    JavaFileObject getSourceFile(String uri);

    boolean containsSourceFile(String uri) throws Exception;

    PackageIdentifier getArtifactIdentifier(URI fileObjectUri);
    PackageInformation getThisArtifactInformation();
    Set<PackageIdentifier> getDependencies();

    void setWorkspaceManager(WorkspaceManager w);
    WorkspaceManager getWorkspaceManager();

    JavacHolder getCompiler();

    class Utils {
        public static String classFileToPackageName(String classFileName) {
            return StringUtils.substringBeforeLast(classFileName, "/").replace('/', '.');
        }
    }
}
