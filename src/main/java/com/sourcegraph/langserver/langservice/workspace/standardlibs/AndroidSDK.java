package com.sourcegraph.langserver.langservice.workspace.standardlibs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sourcegraph.common.Config;
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
 * Created by beyang on 3/17/17.
 */
public class AndroidSDK implements StandardLibrary {

    private static final Predicate<String> PATH_MATCHER = Pattern.compile("/android\\.jar\\((?:[\\-_A-Za-z0-9\\./])*\\)").asPredicate();
    private static final String GROUP = "com.google.android";
    private static final String ARTIFACT = "android";
    public static final PackageIdentifier PACKAGE_IDENTIFIER = PackageIdentifier.of(STDLIB, GROUP+":"+ARTIFACT, "25", "git://github.com/androidmirrors/android-sdk", null);

    @Override
    public boolean matches(JavaFileObject object, Symbol symbol) {
        if (!(object instanceof ZipFileIndexArchive.ZipFileIndexFileObject)) {
            return false;
        }
        String zipName = FilenameUtils.separatorsToUnix(object.getName());
        return PATH_MATCHER.test(zipName);
    }

    @Override
    public Collection<String> getSentinelUris() {
        return ImmutableSet.of(
                "file:///sdk/android/app/Activity.java",
                "file:///sdk/android/view/View.java"
        );
    }

    @Override
    public PackageIdentifier getPackageIdentifier() {
        return PACKAGE_IDENTIFIER;
    }

    @Override
    public Project getConfiguration() {
        Project p = new Project();
        p.setSourceDirectories(ImmutableList.of("sdk"));
        if (Config.ANDROID_JAR_PATH != null) {
            p.setCompilerOptions(Lists.newArrayList(new CompilerOption("-bootclasspath", Config.ANDROID_JAR_PATH)));
        }
        p.setArtifactId(ARTIFACT);
        p.setGroupId(GROUP);
        return p;
    }

    @Override
    public boolean isUsed(List<CompilerOption> compilerOptions) {
        for (CompilerOption opt : compilerOptions) {
            if ("-bootclasspath".equals(opt.getName()) && opt.getValue() != null && opt.getValue().endsWith("/android.jar")) {
                return true;
            }
        }
        return false;
    }
}
