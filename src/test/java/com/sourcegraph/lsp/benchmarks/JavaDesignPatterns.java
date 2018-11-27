package com.sourcegraph.lsp.benchmarks;

import com.sourcegraph.lsp.Harness;
import com.sourcegraph.lsp.domain.structures.Hover;
import com.sourcegraph.lsp.domain.structures.MarkedString;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * java-design-patterns contains a multi-level pom hierarchy. Some of the pom.xml's
 * do not contain an explicit groupId and instead inherit the parent pom.xml's groupId.
 *
 * Created by beyang on 2/24/17.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JavaDesignPatterns extends BenchmarkTestBase {

    private static String REPO = "java-design-patterns";

    /**
     * IMPORTANT: Test names define tests execution order
     */
    @Test
    public void _warmup() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            harness.doHover("file:///caching/src/main/java/com/iluwatar/caching/AppManager.java", 35, 20);
        }
    }

    @Test(timeout = 40000)
    public void firstHover() throws Exception {
        try (Harness h = BenchmarkTestBase.makeHarness(REPO)) {
            h.expectHover("file:///callback/src/main/java/com/iluwatar/callback/App.java", 36, 30, Hover.of(
                    MarkedString.of("java", "private static final org.slf4j.Logger LOGGER")
            ));
        }
    }

}
