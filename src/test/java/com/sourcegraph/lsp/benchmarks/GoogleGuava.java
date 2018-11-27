package com.sourcegraph.lsp.benchmarks;

import com.google.common.collect.Maps;
import com.sourcegraph.lsp.Harness;
import com.sourcegraph.lsp.domain.result.TextDocumentReferencesResult;
import com.sourcegraph.lsp.domain.result.TextDocumentXDefinitionResult;
import com.sourcegraph.lsp.domain.result.WorkspaceSymbolResult;
import com.sourcegraph.lsp.domain.result.WorkspaceXReferencesResult;
import com.sourcegraph.lsp.domain.structures.*;
import org.junit.Ignore;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ElementKind;

/**
 * Created by alexsaveliev on 15.02.2017.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GoogleGuava extends BenchmarkTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGuava.class);


    private static String REPO = "guava";

    /**
     * IMPORTANT: Test names define tests execution order
     */
    @Test
    public void _warmup() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            harness.doHover("file:///guava/src/com/google/common/collect/AbstractMapBasedMultimap.java",
                    1294, 33);
        }
    }

    @Test(timeout = 20000)
    public void a_coldXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///guava/src/com/google/common/collect/AbstractMapBasedMultimap.java",
                    1293, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///guava/src/com/google/common/collect/AbstractMapBasedMultimap.java", 1293, 16, 1293, 21),
                                    SymbolDescriptor.of(ElementKind.CLASS, "AsMap", "com.google.common.collect.AbstractMapBasedMultimap.AsMap", "AbstractMapBasedMultimap", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))
                            )
                    )
            );
            LOG.info("Cold XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 1000)
    public void a_warmXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///guava/src/com/google/common/collect/AbstractMapBasedMultimap.java",
                    1293, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///guava/src/com/google/common/collect/AbstractMapBasedMultimap.java", 1293, 16, 1293, 21),
                                    SymbolDescriptor.of(ElementKind.CLASS, "AsMap", "com.google.common.collect.AbstractMapBasedMultimap.AsMap", "AbstractMapBasedMultimap", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))
                            )
                    )
            );
            LOG.info("Warm XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 120000)
    public void b_coldSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("Escapers",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Escapers", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava/src/com/google/common/escape/Escapers.java", 34, 19, 34, 27)),
                            SymbolInformation.of("EscapersTest", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava-tests/test/com/google/common/escape/EscapersTest.java", 28, 13, 28, 25)),
                            SymbolInformation.of("EscapersTest_gwt", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava-gwt/test/com/google/common/escape/EscapersTest_gwt.java", 16, 13, 16, 29)),
                            SymbolInformation.of("HtmlEscapers", SymbolKind.CLASS, "com.google.common.html", Location.of("file:///guava/src/com/google/common/html/HtmlEscapers.java", 39, 19, 39, 31)),
                            SymbolInformation.of("HtmlEscapersTest", SymbolKind.CLASS, "com.google.common.html", Location.of("file:///guava-tests/test/com/google/common/html/HtmlEscapersTest.java", 27, 13, 27, 29)),
                            SymbolInformation.of("UrlEscapers", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava/src/com/google/common/net/UrlEscapers.java", 32, 19, 32, 30)),
                            SymbolInformation.of("UrlEscapersTest", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava-tests/test/com/google/common/net/UrlEscapersTest.java", 35, 13, 35, 28)),
                            SymbolInformation.of("UrlEscapersTest_gwt", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava-gwt/test/com/google/common/net/UrlEscapersTest_gwt.java", 16, 13, 16, 32)),
                            SymbolInformation.of("XmlEscapers", SymbolKind.CLASS, "com.google.common.xml", Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 43, 13, 43, 24)),
                            SymbolInformation.of("XmlEscapersTest", SymbolKind.CLASS, "com.google.common.xml", Location.of("file:///guava-tests/test/com/google/common/xml/XmlEscapersTest.java", 32, 13, 32, 28))
                    )
            );
            LOG.info("Cold Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 1000)
    public void b_warmSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("Escapers",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Escapers", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava/src/com/google/common/escape/Escapers.java", 34, 19, 34, 27)),
                            SymbolInformation.of("EscapersTest", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava-tests/test/com/google/common/escape/EscapersTest.java", 28, 13, 28, 25)),
                            SymbolInformation.of("EscapersTest_gwt", SymbolKind.CLASS, "com.google.common.escape", Location.of("file:///guava-gwt/test/com/google/common/escape/EscapersTest_gwt.java", 16, 13, 16, 29)),
                            SymbolInformation.of("HtmlEscapers", SymbolKind.CLASS, "com.google.common.html", Location.of("file:///guava/src/com/google/common/html/HtmlEscapers.java", 39, 19, 39, 31)),
                            SymbolInformation.of("HtmlEscapersTest", SymbolKind.CLASS, "com.google.common.html", Location.of("file:///guava-tests/test/com/google/common/html/HtmlEscapersTest.java", 27, 13, 27, 29)),
                            SymbolInformation.of("UrlEscapers", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava/src/com/google/common/net/UrlEscapers.java", 32, 19, 32, 30)),
                            SymbolInformation.of("UrlEscapersTest", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava-tests/test/com/google/common/net/UrlEscapersTest.java", 35, 13, 35, 28)),
                            SymbolInformation.of("UrlEscapersTest_gwt", SymbolKind.CLASS, "com.google.common.net", Location.of("file:///guava-gwt/test/com/google/common/net/UrlEscapersTest_gwt.java", 16, 13, 16, 32)),
                            SymbolInformation.of("XmlEscapers", SymbolKind.CLASS, "com.google.common.xml", Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 43, 13, 43, 24)),
                            SymbolInformation.of("XmlEscapersTest", SymbolKind.CLASS, "com.google.common.xml", Location.of("file:///guava-tests/test/com/google/common/xml/XmlEscapersTest.java", 32, 13, 32, 28))
                    )
            );
            LOG.info("Warm Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 30000)
    public void c_coldReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///guava/src/com/google/common/xml/XmlEscapers.java",
                    104, 48, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 99, 11, 99, 32),
                            Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 142, 4, 142, 25)
                    ));
            LOG.info("Cold References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 1000)
    public void c_warmReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///guava/src/com/google/common/xml/XmlEscapers.java",
                    104, 48, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 99, 11, 99, 32),
                            Location.of("file:///guava/src/com/google/common/xml/XmlEscapers.java", 142, 4, 142, 25)
                    ));
            LOG.info("Warm References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 20000)
    public void d_coldXReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            SymbolDescriptor descriptor = SymbolDescriptor.of(
                    ElementKind.ANNOTATION_TYPE,
                    "IgnoreJRERequirement",
                    "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement",
                    "IgnoreJRERequirement",
                    "org.codehaus.mojo.animal_sniffer",
                    PackageIdentifier.ofMaven("org.codehaus.mojo", "animal-sniffer-annotations", null)
            );
            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/util/concurrent/FuturesGetChecked.java", 38, 40, 38, 60), SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement", "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.codehaus.mojo:animal-sniffer-annotations", null, null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/util/concurrent/FuturesGetChecked.java", 123, 5, 123, 25), SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement", "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.codehaus.mojo:animal-sniffer-annotations", null, null, null)))
                    )
            );
            LOG.info("Cold XReferences took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 1000)
    public void d_warmXReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            SymbolDescriptor descriptor = SymbolDescriptor.of(
                    ElementKind.ANNOTATION_TYPE,
                    "IgnoreJRERequirement",
                    "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement",
                    "IgnoreJRERequirement",
                    "org.codehaus.mojo.animal_sniffer",
                    PackageIdentifier.ofMaven("org.codehaus.mojo", "animal-sniffer-annotations", null)
            );
            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/util/concurrent/FuturesGetChecked.java", 38, 40, 38, 60), SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement", "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.codehaus.mojo:animal-sniffer-annotations", null, null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/util/concurrent/FuturesGetChecked.java", 123, 5, 123, 25), SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement", "IgnoreJRERequirement", "org.codehaus.mojo.animal_sniffer", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.codehaus.mojo:animal-sniffer-annotations", null, null, null)))
                    )
            );
            LOG.info("Warm XReferences took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }
}
