package com.sourcegraph.langserver.langservice.workspace.standardlibs;

import com.google.common.collect.ImmutableSet;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
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
 * OpenJDK8 standard library
 */
public class OpenJDK implements StandardLibrary {

    private static final String GROUP = "STANDARD_LIB";
    private static final String ARTIFACT = "jdk8";
    public static final PackageIdentifier PACKAGE_IDENTIFIER = PackageIdentifier.of(STDLIB, GROUP+":"+ARTIFACT, null, "git://github.com/jdkmirrors/openjdk8", null);

    private static final Predicate<String> STDLIB_PATH_MATCHER = Pattern.compile("/(?:java-[0-9]+-oracle|jdk[0-9]+\\.[0-9]+\\.[0-9]+(?:_[0-9]+)?)/lib/ct.sym").asPredicate();
    private static final Predicate<String> STDLIB_PATH_MATCHER_MACOS = Pattern.compile("/jdk[0-9]+\\.[0-9]+\\.[0-9]+(?:_[0-9]+)?\\.jdk/").asPredicate();
    private static final Predicate<String> STDLIB_OPENJDK_PATH_MATCHER = Pattern.compile("/(?:java-[0-9.]+-openjdk-\\w+)/lib/ct.sym").asPredicate();

    /**
     * Some symbols from langtools belong to ct.sym jar file so we need to filter them out
     */
    private static Collection<String> LANGTOOLS_PACKAGES = ImmutableSet.of(
            "javax.tools.",
            "javax.annotation.processing.",
            "javax.lang.model.",
            "com.sun.javadoc.",
            "com.sun.source.",
            "com.sun.tools.",
            "com.sun.tools.classfile.",
            "com.sun.tools.doclets.",
            "com.sun.tools.doclint.",
            "com.sun.tools.javac.",
            "com.sun.tools.javadoc.",
            "com.sun.tools.javah.",
            "com.sun.tools.javap.",
            "com.sun.tools.jdeps.",
            "com.sun.tools.sjavac.",
            "jdk."
    );

    @Override
    public boolean matches(JavaFileObject object, Symbol symbol) {
        if (!(object instanceof ZipFileIndexArchive.ZipFileIndexFileObject)) {
            return false;
        }
        String zipName = FilenameUtils.separatorsToUnix(object.getName());
        // some symbols return false positive because they belong to langtools so we'll double check them
        return (STDLIB_PATH_MATCHER.test(zipName) ||
                STDLIB_PATH_MATCHER_MACOS.test(zipName) ||
                STDLIB_OPENJDK_PATH_MATCHER.test(zipName)) && !belongsToLangTools(symbol);
    }

    @Override
    public Collection<String> getSentinelUris() {
        return ImmutableSet.of(
                "file:///src/share/classes/java/io/OutputStream.java",
                "file:///src/share/classes/java/util/Comparator.java"
        );
    }

    @Override
    public PackageIdentifier getPackageIdentifier() {
        return PACKAGE_IDENTIFIER;
    }

    @Override
    public Project getConfiguration() {
        // We expect the OpenJDK repository (github.com/jdkmirrors/openjdk8) to provide the config files
        return null;
    }

    @Override
    public boolean isUsed(List<CompilerOption> compilerOptions) {
        for (CompilerOption opt : compilerOptions) {
            if ("-bootclasspath".equals(opt.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param symbol symbol to check
     * @return true if symbol belongs to langtools package
     */
    static boolean belongsToLangTools(Symbol symbol) {
        String name = symbol.toString();
        for (String packageName : LANGTOOLS_PACKAGES) {
            if (name.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

}
