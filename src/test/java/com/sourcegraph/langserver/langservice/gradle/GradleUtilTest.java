package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.model.Dependency;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

/**
 * Created by beyang on 3/30/17.
 */
public class GradleUtilTest {
    @Test
    public void testToDependency() {
        Map<String, Map<String, Dependency>> exp = ImmutableMap.of(
                "compile", ImmutableMap.<String, Dependency>builder()
                        .put("net.bytebuddy:byte-buddy:1.6.11", GradleUtil.toDependency("compile", "net.bytebuddy", "byte-buddy", "1.6.11", "jar", null))
                        .put("net.bytebuddy:byte-buddy2:1.6.11@aar", GradleUtil.toDependency("compile", "net.bytebuddy", "byte-buddy2", "1.6.11", "aar", null))
                        .put("net.bytebuddy:byte-buddy3:1.6.11:jdk15@aar", GradleUtil.toDependency("compile", "net.bytebuddy", "byte-buddy3", "1.6.11", "aar", "jdk15"))
                        .put("net.bytebuddy:byte-buddy4@aar", GradleUtil.toDependency("compile", "net.bytebuddy", "byte-buddy4", null, "aar", null))
                        .put("net.bytebuddy:byte-buddy5", GradleUtil.toDependency("compile", "net.bytebuddy", "byte-buddy5", null, "jar", null))

                        // Gradle support libraries
                        .put("com.android.support:support-annotations:25.0.1", GradleUtil.toDependency("compile", "com.android.support", "support-annotations", "25.0.1", "jar", null))
                        .put("com.android.support:appcompat-v7:25.0.1", GradleUtil.toDependency("compile", "com.android.support", "appcompat-v7", "25.0.1", "aar", null))
                        .put("com.google.android.gms:play-services-cast-framework:9.4.0", GradleUtil.toDependency("compile", "com.google.android.gms", "play-services-cast-framework", "9.4.0", "aar", null))
                        .put("com.google.android.support:wearable:1.3.0", GradleUtil.toDependency("compile", "com.google.android.support", "wearable", "1.3.0", "aar", null))
                        .put("com.google.android.wearable:wearable:2.0.0", GradleUtil.toDependency("compile", "com.google.android.wearable", "wearable", "2.0.0", "jar", null))
                        .put("com.android.support:cardview-v7:23.4.0", GradleUtil.toDependency("compile", "com.android.support", "cardview-v7", "23.4.0", "aar", null))
                        .put("com.android.support:mediarouter-v7:23.4.0", GradleUtil.toDependency("compile", "com.android.support", "mediarouter-v7", "23.4.0", "aar", null))
                        .put("com.android.support:leanback-v17:23.4.0", GradleUtil.toDependency("compile", "com.android.support", "leanback-v17", "23.4.0", "aar", null))
                        .put("com.android.support.test:runner:0.4.1", GradleUtil.toDependency("compile", "com.android.support.test", "runner", "0.4.1", "aar", null))
                        .build(),
                "provided", ImmutableMap.<String, Dependency>builder()
                        .put("junit:junit:4.12", GradleUtil.toDependency("provided", "junit", "junit", "4.12", "jar", null))
                        .build()
        );
        for (Map.Entry<String, Map<String, Dependency>> e : exp.entrySet()) {
            String scope = e.getKey();
            for (Map.Entry<String, Dependency> e2 : e.getValue().entrySet()) {
                String depStr = e2.getKey();
                Dependency expDep = e2.getValue();
                assertEqual(expDep, GradleUtil.toDependency(depStr, scope, null));
            }
        }
    }

    private static void assertEqual(Dependency exp, Dependency actual) {
        Assert.assertTrue(String.format("artifactId: %s != %s", exp.getArtifactId(), actual.getArtifactId()), Objects.equals(exp.getArtifactId(), actual.getArtifactId()));
        Assert.assertTrue(String.format("groupId: %s != %s (artifactId was %s)", exp.getGroupId(), actual.getGroupId(), exp.getArtifactId()), Objects.equals(exp.getGroupId(), actual.getGroupId()));
        Assert.assertTrue(String.format("scope: %s != %s (artifactId was %s)", exp.getScope(), actual.getScope(), exp.getArtifactId()), Objects.equals(exp.getScope(), actual.getScope()));
        Assert.assertTrue(String.format("version: %s != %s (artifactId was %s)", exp.getVersion(), actual.getVersion(), exp.getArtifactId()), Objects.equals(exp.getVersion(), actual.getVersion()));
        Assert.assertTrue(String.format("type: %s != %s (artifactId was %s)", exp.getType(), actual.getType(), exp.getArtifactId()), Objects.equals(exp.getType(), actual.getType()));
        Assert.assertTrue(String.format("classifer: %s != %s (artifactId was %s)", exp.getClassifier(), actual.getClassifier(), exp.getArtifactId()), Objects.equals(exp.getType(), actual.getType()));
    }
}
