package com.sourcegraph.lsp.benchmarks;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ApacheCommonsIo.class,
        Dropwizard.class,
        GoogleGuava.class,
        JavaDesignPatterns.class
})
public class BenchmarkSuite {
}
