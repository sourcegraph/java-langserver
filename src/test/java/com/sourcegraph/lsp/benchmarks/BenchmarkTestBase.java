package com.sourcegraph.lsp.benchmarks;

import com.sourcegraph.lsp.Harness;

/**
 * Benchmark test base
 */
class BenchmarkTestBase {

    private static Harness harness;

    static Harness makeHarness(String repo) throws Exception {
        harness = Harness.newHarness(repo);
        return harness;
    }

    public static Harness getHarness() throws Exception {
        return harness;
    }
}
