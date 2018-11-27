package com.sourcegraph.langserver.langservice.workspace.standardlibs;

import com.google.common.collect.ImmutableList;
import com.sourcegraph.langserver.langservice.compiler.CompilerOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds all known standard libraries
 */
public final class StandardLibraries {

    private static final StandardLibraries instance = new StandardLibraries();

    public static StandardLibraries getInstance() {
        return instance;
    }

    private final Collection<StandardLibrary> libraries = new LinkedList<>();

    private StandardLibraries() {
        add(new OpenJDK());
        add(new OpenJDKLangTools());
        add(new AndroidSDK());
    }

    public void add(StandardLibrary library) {
        libraries.add(library);
    }

    public Collection<StandardLibrary> getLibraries() {
        return ImmutableList.copyOf(libraries);
    }

    public Collection<StandardLibrary> getLibrariesInUse(List<CompilerOption> compilerOptions) {
        ArrayList<StandardLibrary> inUse = new ArrayList<>();
        for (StandardLibrary lib : getLibraries()) {
            if (lib.isUsed(compilerOptions)) {
                inUse.add(lib);
            }
        }
        return inUse;
    }

    public static boolean isStandardLibraryPackage(String packageName) {
        if (packageName.startsWith("java.") || packageName.equals("java")) {
            return true;
        }
        if (packageName.equals("android")) {
            return true;
        }
        if (packageName.startsWith("android.")) {
            return !packageName.startsWith("android.support.") && !packageName.equals("android.support");
        }
        if (packageName.startsWith("com.android.")) {
            return !packageName.startsWith("com.android.support.") && !packageName.equals("com.android.support");
        }
        return false;
    }
}
