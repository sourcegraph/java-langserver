package com.sourcegraph.langserver.langservice.workspace.standardlibs;

import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sun.tools.javac.code.Symbol;

import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.List;

/**
 * Standard library represent core or some standard java library, for example OpenJDK, langtools, Android SDK and so on.
 * This interface defines methods that allows to:
 * - identify if a given symbol comes from some known library
 * - identify if workspace contains source code of some known library
 * - identify compilation settings to build library data
 */
public interface StandardLibrary {

    /**
     * @return this library package identifier
     */
    PackageIdentifier getPackageIdentifier();

    /**
     * @param object java file object to check
     * @param symbol symbol to check
     * @return true if given object (and symbol) belong to current library
     */
    boolean matches(JavaFileObject object, Symbol symbol);

    /**
     * Returns true if this StandardLibrary is used by a blob of code compiled by the given compiler options.
     */
    boolean isUsed(List<CompilerOption> compilerOptions);

    /**
     * @return URIs present in the given library to determine if workspace matches given standard library
     */
    Collection<String> getSentinelUris();

    /**
     * Returns JavaConfig object for the standard library, which will be used if no config is already present in the
     * repository. If null is returned, then the caller should just use the config present in the repository.
     */
    Project getConfiguration();
}
