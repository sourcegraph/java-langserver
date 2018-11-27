package com.sourcegraph.langserver.langservice.workspace.standardlibs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.langserver.langservice.workspace.JarEntryFile;
import com.sourcegraph.langserver.langservice.javaconfigjson.Project;
import com.sourcegraph.lsp.domain.structures.PackageIdentifier;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.file.ZipFileIndexArchive;
import org.apache.commons.io.FilenameUtils;

import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.sourcegraph.lsp.domain.structures.PackageIdentifier.Type.STDLIB;

/**
 * OpenJDK8 langtools standard library
 */
public class OpenJDKLangTools implements StandardLibrary {

    private static final String ID = "jdk8-langtools";

    public static final PackageIdentifier PACKAGE_IDENTIFIER = PackageIdentifier.of(STDLIB,
            ID,
            null,
            "git://github.com/jdkmirrors/openjdk8-langtools",
            null);

    private static final Predicate<String> STDLIB_PATH_MATCHER = Pattern.compile("/(?:java-[0-9]+-oracle|jdk[0-9]+\\.[0-9]+\\.[0-9]+(?:_[0-9]+)?)/lib/tools.jar").asPredicate();
    private static final Predicate<String> STDLIB_OPENJDK_PATH_MATCHER = Pattern.compile("/(?:java-[0-9.]+-openjdk-\\w+)/lib/tools.jar").asPredicate();

    @Override
    public PackageIdentifier getPackageIdentifier() {
        return PACKAGE_IDENTIFIER;
    }

    @Override
    public boolean matches(JavaFileObject object, Symbol symbol) {
        String zipName;
        if (object instanceof ZipFileIndexArchive.ZipFileIndexFileObject) {
            zipName = FilenameUtils.separatorsToUnix(object.getName());
        } else if (object instanceof JarEntryFile) {
            zipName = FilenameUtils.separatorsToUnix(((JarEntryFile) object).getJarName());
        } else {
            return false;
        }
        return STDLIB_PATH_MATCHER.test(zipName) ||
                STDLIB_OPENJDK_PATH_MATCHER.test(zipName) ||
                OpenJDK.belongsToLangTools(symbol);
    }

    @Override
    public Collection<String> getSentinelUris() {
        return ImmutableSet.of(
                "file:///src/share/classes/javax/tools/JavaCompiler.java"
        );
    }

    @Override
    public Project getConfiguration() {
        Project p = new Project();
        p.setSourceDirectories(ImmutableList.of("src/share/classes"));
        return p;
    }

    public boolean isUsed(List<CompilerOption> compilerOptions) {
        // NOTE: This means we never record any dependencies on the OpenJDK lang tools, which means we never
        // display global usage examples for that repository.
        return false;
    }
}
