package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sourcegraph.langserver.langservice.maven.MavenUtil;
import org.apache.maven.model.Dependency;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by beyang on 3/30/17.
 */
final class GradleUtil {

    /**
     * Converts a dependency parsed from Gradle into a Maven Dependency instance.
     */
    public static Dependency toDependency(String dep, String scope, String type) {
        Matcher m = DEP_MATCHER.matcher(dep);
        if (!m.matches()) {
            throw new IllegalArgumentException("Could not extract $GROUP:$ARTIFACT(:$VERSION)?(:$CLASSIFIER)?(@$EXT)? from " + dep);
        }
        String group = m.group("group");
        String name = m.group("name");
        String version = m.group("version");
        String ext = m.group("ext");
        String classifier = m.group("classifier");

        if (ext == null && isAutoAarDependency(group, name, version)) {
            ext = "aar";
        }
        if (ext == null) {
            ext = type;
        }

        return toDependency(scope, group, name, version, ext, classifier);
    }

    static Dependency toDependency(String scope, String group, String artifact, String version, String type, String classifier) {
        version = gradleToMavenVersion(version);
        Dependency dependency = new Dependency();
        if (group != null) {
            dependency.setGroupId(group);
        } else {
            dependency.setGroupId(MavenUtil.UNKNOWN_GROUP);
        }
        if (artifact != null) {
            dependency.setArtifactId(artifact);
        }
        if (version != null) {
            dependency.setVersion(version);
        } else {
            dependency.setVersion(MavenUtil.UNKNOWN_VERSION);
        }
        if (scope != null) {
            dependency.setScope(scope);
        }
        if (type != null) {
            dependency.setType(type);
        }
        if (classifier != null) {
            dependency.setClassifier(classifier);
        }
        return dependency;
    }

    public static String gradleToMavenVersion(String gradleVersion) {
        String v = gradleVersion;
        if (v == null) {
            return v;
        }
        if (v.startsWith("[") || v.startsWith("(") || v.startsWith("]") || v.startsWith(")")) {
            v = v.substring(1);
        }
        if (v.endsWith("]") || v.endsWith((")")) || v.endsWith("[") || v.endsWith("(")) {
            v = v.substring(0, v.length() - 1);
        }
        String[] parts = v.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].length() > 0) {
                return parts[i];
            }
        }

        // If this fails, fall back to original string
        return gradleVersion;
    }

    /**
     * Returns true if a dependency should be treated as type .aar if no type is explicitly set. (The default assumption
     * is .jar)
     */
    private static boolean isAutoAarDependency(String group, String artifact, String version) {
        if (!AUTO_AAR_GROUPS.contains(group)) {
            return false;
        }
        if (!AUTO_AAR_EXCLUDES.containsKey(group)) {
            return true;
        }
        Map<String, Set<String>> artifactExcludes = AUTO_AAR_EXCLUDES.get(group);
        if (artifactExcludes.containsKey("ALL")) { // null mean "excludes all"
            return false;
        }
        if (!artifactExcludes.containsKey(artifact)) {
            return true;
        }
        Set<String> versionExcludes = artifactExcludes.get(artifact);
        if (versionExcludes.contains("ALL")) {
            return false;
        }
        return !versionExcludes.contains(version);
    }

    private static final Pattern DEP_MATCHER =
            Pattern.compile("^(?<group>[^:@]+)\\:(?<name>[^:@]+)(?:\\:(?<version>[^:@]+)(?:\\:(?<classifier>[^:@]+))?)?(?:@(?<ext>[^:@]+))?$");
    private static final Set<String> AUTO_AAR_GROUPS = new ImmutableSet.Builder<String>()
            .add("com.android.support")
            .add("com.android.support.test")
            .add("com.google.android.support")
            .add("com.google.android.gms")
            .add("com.google.firebase")
            .add("com.android.databinding")
            .build();
    private static final Map<String, Map<String, Set<String>>> AUTO_AAR_EXCLUDES = ImmutableMap.of(
            "com.android.support", ImmutableMap.of(
                    "support-v4", ImmutableSet.of("13.0.0", "18.0.0", "19.0.0", "19.0.1", "19.1.0"),
                    "support-v13", ImmutableSet.of("13.0.0", "18.0.0", "19.0.0", "19.0.1", "19.1.0"),
                    "support-annotations", ImmutableSet.of("ALL")
            ),
            "com.google.android.wearable", ImmutableMap.of(
                    "wearable", ImmutableSet.of("1.0.0", "2.0.0", "2.0.0-alpha3", "2.0.0-beta1", "2.0.0-beta2", "2.0.1")
            ),
            "com.android.databinding", ImmutableMap.of(
                    "adapters", ImmutableSet.of("1.0-rc0", "1.0-rc1")
            )
    );
}
