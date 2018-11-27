package com.sourcegraph.common;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.VM;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {

    public static final boolean LIGHTSTEP_INCLUDE_SENSITIVE =
            Boolean.valueOf(System.getenv("LIGHTSTEP_INCLUDE_SENSITIVE"));
    public static final String LIGHTSTEP_PROJECT = System.getenv("LIGHTSTEP_PROJECT");
    public static final String LIGHTSTEP_TOKEN = System.getenv("LIGHTSTEP_ACCESS_TOKEN");

    /**
     * Path to android.jar file that provides the Android SDK. This assumes a single version of the SDK. If we want to
     * support including other versions of the SDK, we'll need to extend this to include multiple possible SDK jars.
     */
    public static final String ANDROID_JAR_PATH = System.getenv().get("ANDROID_JAR_PATH");

    /**
     * If set, causes the dependency fetcher to ignore the Maven dependency resolution cache.
     */
    public static final boolean IGNORE_DEPENDENCY_RESOLUTION_CACHE = System.getenv().get("NO_CACHE") != null;

    /**
     * Controls how long the LSP controller will wait for a response to a blocking request.
     */
    public static final int LSP_TIMEOUT;

    static {
        String timeout = System.getenv().get("LSP_TIMEOUT");
        if (timeout != null) {
            LSP_TIMEOUT = Integer.valueOf(timeout);
        } else {
            LSP_TIMEOUT = 20;
        }
    }

    /**
     * Root directory to keep all the files in
     */
    private static final String LANGSERVER_ROOT = System.getenv().getOrDefault("LANGSERVER_ROOT", new File(FileUtils.getUserDirectory(), ".java-langserver").toString());

    // -------------------------------------------------------------------------------
    // Derived fields
    // -------------------------------------------------------------------------------

    /**
     * Local artifacts repository
     */
    public static final File LOCAL_REPOSITORY = new File(LANGSERVER_ROOT, "artifacts");

    /**
     * Comma-delimited original root paths for which we run the Gradle plugin, rather than Fradle, to extract
     * Gradle metadata. Note that these should include projects that we trust with arbitrary code execution.
     *
     * Note: This is currently misnamed, as it is used to match against the root URI, instead of the no-longer-used
     * root path initialize parameter.
     */
    private static final ImmutableSet<String> EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS;
    static {
        String v = System.getenv().get("EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS");
        if (v != null && !v.isEmpty()) {
            EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS = ImmutableSet.copyOf(v.split(","));
        } else {
            EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS = ImmutableSet.of();
        }
    }

    /**
     * shouldExecuteGradle returns true when we should run the Gradle plugin to extract build metadata (instead of
     * running Fradle). This is determined by the value of the environment variable EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS.
     */
    public static boolean shouldExecuteGradle(String originalRootUri) {
        if (originalRootUri == null) {
            return false;
        }
        for (String rootPathPattern : EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS) {
            if (rootPathPattern.endsWith("%")) {
                if (originalRootUri.startsWith(rootPathPattern.substring(0, rootPathPattern.length() - 1))) {
                    return true;
                }
            } else {
                if (rootPathPattern.equals(originalRootUri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Authentication info for private repositories -- currently we support BasicAuth via three environments variables
     * for username, password, and the id of the private repo.
     */
    public static final String PRIVATE_REPO_ID = System.getenv("PRIVATE_ARTIFACT_REPO_ID");
    public static final String PRIVATE_REPO_URL = System.getenv("PRIVATE_ARTIFACT_REPO_URL");
    public static final String PRIVATE_REPO_USERNAME = System.getenv("PRIVATE_ARTIFACT_REPO_USERNAME");
    public static final String PRIVATE_REPO_PASSWORD = System.getenv("PRIVATE_ARTIFACT_REPO_PASSWORD");

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static void checkEnv() throws ConfigException {
        System.out.println("Configuration:");
        System.out.printf("\t%s:\t%s\n", "EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS", EXECUTE_GRADLE_ORIGINAL_ROOT_PATHS);
        System.out.printf("\t%s:\t%s\n", "LIGHTSTEP_INCLUDE_SENSITIVE", LIGHTSTEP_INCLUDE_SENSITIVE);
        System.out.printf("\t%s:\t%s\n", "LIGHTSTEP_PROJECT", LIGHTSTEP_PROJECT);
        System.out.printf("\t%s:\t%s\n", "LIGHTSTEP_TOKEN", LIGHTSTEP_TOKEN != null ? "<redacted>" : null);
        System.out.printf("\t%s:\t%s\n", "ANDROID_JAR_PATH", ANDROID_JAR_PATH);
        System.out.printf("\t%s:\t%s\n", "IGNORE_DEPENDENCY_RESOLUTION_CACHE", IGNORE_DEPENDENCY_RESOLUTION_CACHE);
        System.out.printf("\t%s:\t%s\n", "LANGSERVER_ROOT", LANGSERVER_ROOT);
        System.out.printf("\t%s:\t%d\n", "LSP_TIMEOUT", LSP_TIMEOUT);
        System.out.printf("\t%s:\t%s\n", "PRIVATE_ARTIFACT_REPO_ID", PRIVATE_REPO_ID);
        System.out.printf("\t%s:\t%s\n", "PRIVATE_ARTIFACT_REPO_USERNAME", PRIVATE_REPO_USERNAME);
        System.out.printf("\t%s:\t%s\n", "PRIVATE_ARTIFACT_REPO_PASSWORD", PRIVATE_REPO_PASSWORD != null ? "<redacted>" : null);

        Path p = Paths.get(LOCAL_REPOSITORY.toString(), "com/android/support/appcompat-v7/25.3.0/appcompat-v7-25.3.0.aar");
        if (!Files.exists(p)) {
            log.error("It appears Android support libraries are not installed in LANGSERVER_ROOT. Use the Android Maven SDK deployer script to add them or else Android support libraries will not be resolved.");
        }

        String disableMemoryMapping = VM.getSavedProperty("sun.zip.disableMemoryMapping");
        if (!"true".equals(disableMemoryMapping)) {
            System.err.println("!!! WARNING: it is highly recommend you pass -Dsun.zip.disableMemoryMapping=true as a JVM argument to avoid fatal crashes. See: https://bugs.openjdk.java.net/browse/JDK-8142508.");
        }
    }

    public static class ConfigException extends java.lang.Exception {}
}
