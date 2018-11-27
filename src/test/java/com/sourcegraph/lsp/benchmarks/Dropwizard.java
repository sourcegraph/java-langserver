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
public class Dropwizard extends BenchmarkTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(Dropwizard.class);

    private static String REPO = "dropwizard";

    /**
     * IMPORTANT: Test names define tests execution order
     */
    @Test
    public void _warmup() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            harness.doHover("file:///dropwizard-metrics/src/main/java/io/dropwizard/metrics/MetricsFactory.java", 83, 45);
        }
    }

    @Test(timeout = 15000)
    public void a_coldXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///dropwizard-metrics/src/main/java/io/dropwizard/metrics/MetricsFactory.java",
                    83, 45,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///dropwizard-metrics/src/main/java/io/dropwizard/metrics/MetricsFactory.java", 48, 43, 48, 52),
                                    SymbolDescriptor.of(ElementKind.FIELD, "reporters", "io.dropwizard.metrics.MetricsFactory.reporters", "MetricsFactory", "io.dropwizard.metrics", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "io.dropwizard:dropwizard-metrics", "1.2.0-SNAPSHOT", null, null))
                            )
                    )
            );
            LOG.info("Cold XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 100)
    public void a_warmXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///dropwizard-metrics/src/main/java/io/dropwizard/metrics/MetricsFactory.java",
                    83, 45,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///dropwizard-metrics/src/main/java/io/dropwizard/metrics/MetricsFactory.java", 48, 43, 48, 52),
                                    SymbolDescriptor.of(ElementKind.FIELD, "reporters", "io.dropwizard.metrics.MetricsFactory.reporters", "MetricsFactory", "io.dropwizard.metrics", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "io.dropwizard:dropwizard-metrics", "1.2.0-SNAPSHOT", null, null))
                            )
                    )
            );
            LOG.info("Warm XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 15000)
    public void b_coldSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("AssetsBundle",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AssetsBundle", SymbolKind.CLASS, "io.dropwizard.assets", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 16, 13, 16, 25)),
                            SymbolInformation.of("AssetsBundle()", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 34, 11, 34, 23)),
                            SymbolInformation.of("AssetsBundle(String path)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 47, 11, 47, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 61, 11, 61, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath, String indexFile)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 76, 11, 76, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath, String indexFile, String assetsName)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 92, 11, 92, 23)),
                            SymbolInformation.of("AssetsBundleTest", SymbolKind.CLASS, "io.dropwizard.assets", Location.of("file:///dropwizard-assets/src/test/java/io/dropwizard/assets/AssetsBundleTest.java", 22, 13, 22, 29)),
                            SymbolInformation.of("canSupportDiffrentAssetsBundleName()", SymbolKind.METHOD, "AssetsBundleTest", Location.of("file:///dropwizard-assets/src/test/java/io/dropwizard/assets/AssetsBundleTest.java", 86, 16, 86, 50))
                    )

            );
            LOG.info("Cold Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 300)
    public void b_warmSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("AssetsBundle",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AssetsBundle", SymbolKind.CLASS, "io.dropwizard.assets", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 16, 13, 16, 25)),
                            SymbolInformation.of("AssetsBundle()", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 34, 11, 34, 23)),
                            SymbolInformation.of("AssetsBundle(String path)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 47, 11, 47, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 61, 11, 61, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath, String indexFile)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 76, 11, 76, 23)),
                            SymbolInformation.of("AssetsBundle(String resourcePath, String uriPath, String indexFile, String assetsName)", SymbolKind.METHOD, "AssetsBundle", Location.of("file:///dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java", 92, 11, 92, 23)),
                            SymbolInformation.of("AssetsBundleTest", SymbolKind.CLASS, "io.dropwizard.assets", Location.of("file:///dropwizard-assets/src/test/java/io/dropwizard/assets/AssetsBundleTest.java", 22, 13, 22, 29)),
                            SymbolInformation.of("canSupportDiffrentAssetsBundleName()", SymbolKind.METHOD, "AssetsBundleTest", Location.of("file:///dropwizard-assets/src/test/java/io/dropwizard/assets/AssetsBundleTest.java", 86, 16, 86, 50))
                    )
            );
            LOG.info("Warm Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 90000)
    public void c_coldReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///dropwizard-core/src/main/java/io/dropwizard/Configuration.java",
                    61, 13,
                    false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 60, 42, 60, 55),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 227, 60, 227, 73),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 233, 24, 233, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 25, 42, 25, 55),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 90, 60, 90, 73),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 96, 24, 96, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 52, 63, 52, 76),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 58, 24, 58, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 64, 42, 64, 55),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/Application.java", 23, 44, 23, 57),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/Application.java", 43, 53, 43, 66),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/CheckCommand.java", 14, 36, 14, 49),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ConfiguredCommand.java", 25, 50, 25, 63),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ConfiguredCommand.java", 41, 53, 41, 66),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/EnvironmentCommand.java", 14, 51, 14, 64),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ServerCommand.java", 17, 37, 17, 50),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/setup/Bootstrap.java", 40, 33, 40, 46),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ApplicationTest.java", 11, 51, 11, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 15, 18, 15, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 15, 52, 15, 65),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 52, 14, 52, 27),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 52, 57, 52, 70),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 14, 59, 14, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 16, 24, 16, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 21, 31, 21, 44),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 24, 28, 24, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 26, 18, 26, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 26, 53, 26, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 30, 31, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 67, 31, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 67, 31, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 33, 24, 33, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 36, 28, 36, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 39, 31, 39, 44),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 60, 86, 60, 99),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 189, 85, 189, 98),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 214, 85, 214, 98),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 295, 69, 295, 82),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 30, 35, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 67, 35, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 67, 35, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 37, 24, 37, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 49, 24, 49, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 16, 63, 16, 76),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 22, 37, 22, 50),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 22, 84, 22, 97),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 27, 59, 27, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 29, 24, 29, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 35, 28, 35, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 42, 29, 42, 42),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 28, 58, 28, 71),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 30, 47, 30, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 35, 59, 35, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 37, 47, 37, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 43, 24, 43, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 54, 18, 54, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 54, 53, 54, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 104, 24, 104, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 108, 51, 108, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 108, 51, 108, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 111, 47, 111, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 111, 106, 111, 119),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 21, 59, 21, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 23, 24, 23, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 28, 32, 28, 45),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 43, 18, 43, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 43, 53, 43, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 30, 29, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 75, 29, 88),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 75, 29, 88),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 31, 24, 31, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 34, 22, 34, 35),
                            Location.of("file:///dropwizard-db/src/main/java/io/dropwizard/db/DatabaseConfiguration.java", 4, 49, 4, 62),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 16, 38, 16, 51),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 18, 37, 18, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 23, 20, 23, 33),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 8, 42, 8, 55),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 10, 37, 10, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 15, 20, 15, 33),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 8, 46, 8, 59),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 10, 37, 10, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 15, 20, 15, 33),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/app1/App1Test.java", 23, 42, 23, 55),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/forms/FormsAppTest.java", 25, 42, 25, 55),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 30, 60, 30, 73),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 36, 24, 36, 37),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 60, 29, 60, 42),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/sslreload/SslReloadAppTest.java", 55, 35, 55, 48),
                            Location.of("file:///dropwizard-example/src/main/java/com/example/helloworld/HelloWorldConfiguration.java", 14, 45, 14, 58),
                            Location.of("file:///dropwizard-hibernate/src/main/java/io/dropwizard/hibernate/HibernateBundle.java", 14, 48, 14, 61),
                            Location.of("file:///dropwizard-hibernate/src/main/java/io/dropwizard/hibernate/ScanningHibernateBundle.java", 15, 56, 15, 69),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 31, 18, 31, 31),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 31, 53, 31, 66),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 34, 35, 47),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 78, 35, 91),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 78, 35, 91),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 37, 54, 37, 67),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 30, 106, 43),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 80, 106, 93),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 80, 106, 93),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 108, 58, 108, 71),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/LazyLoadingTest.java", 42, 50, 42, 63),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/FakeApplication.java", 12, 49, 12, 62),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/FakeApplication.java", 17, 20, 17, 33),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/Http2CIntegrationTest.java", 26, 29, 26, 42),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/Http2IntegrationTest.java", 27, 35, 27, 48),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/AbstractLiquibaseCommand.java", 21, 57, 21, 70),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbCalculateChecksumCommand.java", 14, 50, 14, 63),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbClearChecksumsCommand.java", 7, 47, 7, 60),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbCommand.java", 11, 33, 11, 46),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbDropAllCommand.java", 9, 40, 9, 53),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbDumpCommand.java", 41, 37, 41, 50),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbFastForwardCommand.java", 16, 44, 16, 57),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbGenerateDocsCommand.java", 8, 45, 8, 58),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbLocksCommand.java", 14, 38, 14, 51),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbMigrateCommand.java", 17, 40, 17, 53),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbPrepareRollbackCommand.java", 16, 48, 16, 61),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbRollbackCommand.java", 21, 41, 21, 54),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbStatusCommand.java", 17, 39, 17, 52),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbTagCommand.java", 8, 36, 8, 49),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbTestCommand.java", 12, 37, 12, 50),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/MigrationsBundle.java", 8, 49, 8, 62),
                            Location.of("file:///dropwizard-migrations/src/test/java/io/dropwizard/migrations/TestMigrationConfiguration.java", 5, 48, 5, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java", 40, 45, 40, 58),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java", 268, 59, 268, 72),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/POJOConfigurationFactory.java", 9, 48, 9, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 69, 41, 69, 54),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 201, 59, 201, 72),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 56, 40, 56, 53),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 59, 48, 59, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 59, 48, 59, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 61, 31, 61, 44),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 97, 54, 97, 67),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 99, 24, 99, 37),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/DropwizardTestSupportTest.java", 115, 50, 115, 63),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 21, 42, 21, 55),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 33, 60, 33, 73),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 35, 24, 35, 37),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/TestConfiguration.java", 6, 39, 6, 52),
                            Location.of("file:///dropwizard-views/src/main/java/io/dropwizard/views/ViewBundle.java", 91, 34, 91, 47),
                            Location.of("file:///dropwizard-views/src/main/java/io/dropwizard/views/ViewConfigurable.java", 6, 44, 6, 57),
                            Location.of("file:///dropwizard-views/src/test/java/io/dropwizard/views/ViewBundleTest.java", 37, 49, 37, 62)
                    )
            );
            LOG.info("Cold References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 1500)
    public void c_warmReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///dropwizard-core/src/main/java/io/dropwizard/Configuration.java",
                    61, 13,
                    false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 60, 42, 60, 55),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 227, 60, 227, 73),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/DropwizardApacheConnectorTest.java", 233, 24, 233, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 25, 42, 25, 55),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 90, 60, 90, 73),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/JerseyIgnoreRequestUserAgentHeaderFilterTest.java", 96, 24, 96, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 52, 63, 52, 76),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 58, 24, 58, 37),
                            Location.of("file:///dropwizard-client/src/test/java/io/dropwizard/client/ssl/DropwizardSSLConnectionSocketFactoryTest.java", 64, 42, 64, 55),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/Application.java", 23, 44, 23, 57),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/Application.java", 43, 53, 43, 66),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/CheckCommand.java", 14, 36, 14, 49),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ConfiguredCommand.java", 25, 50, 25, 63),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ConfiguredCommand.java", 41, 53, 41, 66),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/EnvironmentCommand.java", 14, 51, 14, 64),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/cli/ServerCommand.java", 17, 37, 17, 50),
                            Location.of("file:///dropwizard-core/src/main/java/io/dropwizard/setup/Bootstrap.java", 40, 33, 40, 46),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ApplicationTest.java", 11, 51, 11, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 15, 18, 15, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 15, 52, 15, 65),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 52, 14, 52, 27),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/ConfigurationTest.java", 52, 57, 52, 70),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 14, 59, 14, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 16, 24, 16, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 21, 31, 21, 44),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 24, 28, 24, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 26, 18, 26, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CheckCommandTest.java", 26, 53, 26, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 30, 31, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 67, 31, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 31, 67, 31, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 33, 24, 33, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 36, 28, 36, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 39, 31, 39, 44),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 60, 86, 60, 99),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 189, 85, 189, 98),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 214, 85, 214, 98),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CliTest.java", 295, 69, 295, 82),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 30, 35, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 67, 35, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 35, 67, 35, 80),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 37, 24, 37, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/CommandTest.java", 49, 24, 49, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 16, 63, 16, 76),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 22, 37, 22, 50),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 22, 84, 22, 97),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 27, 59, 27, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 29, 24, 29, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 35, 28, 35, 41),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ConfiguredCommandTest.java", 42, 29, 42, 42),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 28, 58, 28, 71),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 30, 47, 30, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 35, 59, 35, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 37, 47, 37, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 43, 24, 43, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 54, 18, 54, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 54, 53, 54, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 104, 24, 104, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 108, 51, 108, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 108, 51, 108, 64),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 111, 47, 111, 60),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/InheritedServerCommandTest.java", 111, 106, 111, 119),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 21, 59, 21, 72),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 23, 24, 23, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 28, 32, 28, 45),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 43, 18, 43, 31),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/cli/ServerCommandTest.java", 43, 53, 43, 66),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 30, 29, 43),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 75, 29, 88),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 29, 75, 29, 88),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 31, 24, 31, 37),
                            Location.of("file:///dropwizard-core/src/test/java/io/dropwizard/setup/BootstrapTest.java", 34, 22, 34, 35),
                            Location.of("file:///dropwizard-db/src/main/java/io/dropwizard/db/DatabaseConfiguration.java", 4, 49, 4, 62),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 16, 38, 16, 51),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 18, 37, 18, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/app1/App1.java", 23, 20, 23, 33),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 8, 42, 8, 55),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 10, 37, 10, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/forms/FormsApp.java", 15, 20, 15, 33),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 8, 46, 8, 59),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 10, 37, 10, 50),
                            Location.of("file:///dropwizard-e2e/src/main/java/com/example/sslreload/SslReloadApp.java", 15, 20, 15, 33),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/app1/App1Test.java", 23, 42, 23, 55),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/forms/FormsAppTest.java", 25, 42, 25, 55),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 30, 60, 30, 73),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 36, 24, 36, 37),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/request_log/AbstractRequestLogPatternIntegrationTest.java", 60, 29, 60, 42),
                            Location.of("file:///dropwizard-e2e/src/test/java/com/example/sslreload/SslReloadAppTest.java", 55, 35, 55, 48),
                            Location.of("file:///dropwizard-example/src/main/java/com/example/helloworld/HelloWorldConfiguration.java", 14, 45, 14, 58),
                            Location.of("file:///dropwizard-hibernate/src/main/java/io/dropwizard/hibernate/HibernateBundle.java", 14, 48, 14, 61),
                            Location.of("file:///dropwizard-hibernate/src/main/java/io/dropwizard/hibernate/ScanningHibernateBundle.java", 15, 56, 15, 69),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 31, 18, 31, 31),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 31, 53, 31, 66),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 34, 35, 47),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 78, 35, 91),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 35, 78, 35, 91),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 37, 54, 37, 67),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 30, 106, 43),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 80, 106, 93),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 106, 80, 106, 93),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/HibernateBundleTest.java", 108, 58, 108, 71),
                            Location.of("file:///dropwizard-hibernate/src/test/java/io/dropwizard/hibernate/LazyLoadingTest.java", 42, 50, 42, 63),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/FakeApplication.java", 12, 49, 12, 62),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/FakeApplication.java", 17, 20, 17, 33),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/Http2CIntegrationTest.java", 26, 29, 26, 42),
                            Location.of("file:///dropwizard-http2/src/test/java/io/dropwizard/http2/Http2IntegrationTest.java", 27, 35, 27, 48),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/AbstractLiquibaseCommand.java", 21, 57, 21, 70),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbCalculateChecksumCommand.java", 14, 50, 14, 63),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbClearChecksumsCommand.java", 7, 47, 7, 60),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbCommand.java", 11, 33, 11, 46),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbDropAllCommand.java", 9, 40, 9, 53),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbDumpCommand.java", 41, 37, 41, 50),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbFastForwardCommand.java", 16, 44, 16, 57),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbGenerateDocsCommand.java", 8, 45, 8, 58),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbLocksCommand.java", 14, 38, 14, 51),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbMigrateCommand.java", 17, 40, 17, 53),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbPrepareRollbackCommand.java", 16, 48, 16, 61),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbRollbackCommand.java", 21, 41, 21, 54),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbStatusCommand.java", 17, 39, 17, 52),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbTagCommand.java", 8, 36, 8, 49),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/DbTestCommand.java", 12, 37, 12, 50),
                            Location.of("file:///dropwizard-migrations/src/main/java/io/dropwizard/migrations/MigrationsBundle.java", 8, 49, 8, 62),
                            Location.of("file:///dropwizard-migrations/src/test/java/io/dropwizard/migrations/TestMigrationConfiguration.java", 5, 48, 5, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java", 40, 45, 40, 58),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java", 268, 59, 268, 72),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/POJOConfigurationFactory.java", 9, 48, 9, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 69, 41, 69, 54),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 201, 59, 201, 72),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 56, 40, 56, 53),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 59, 48, 59, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 59, 48, 59, 61),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 61, 31, 61, 44),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 97, 54, 97, 67),
                            Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 99, 24, 99, 37),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/DropwizardTestSupportTest.java", 115, 50, 115, 63),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 21, 42, 21, 55),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 33, 60, 33, 73),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/DropwizardAppRuleWithoutConfigTest.java", 35, 24, 35, 37),
                            Location.of("file:///dropwizard-testing/src/test/java/io/dropwizard/testing/junit/TestConfiguration.java", 6, 39, 6, 52),
                            Location.of("file:///dropwizard-views/src/main/java/io/dropwizard/views/ViewBundle.java", 91, 34, 91, 47),
                            Location.of("file:///dropwizard-views/src/main/java/io/dropwizard/views/ViewConfigurable.java", 6, 44, 6, 57),
                            Location.of("file:///dropwizard-views/src/test/java/io/dropwizard/views/ViewBundleTest.java", 37, 49, 37, 62)
                    )
            );
            LOG.info("Warm References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 60000)
    public void d_coldXReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            SymbolDescriptor descriptor = SymbolDescriptor.of(
                    ElementKind.CLASS,
                    "ExternalResource",
                    "org.junit.rules.ExternalResource",
                    "ExternalResource",
                    "org.junit.rules",
                    null
            );

            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DBIClient.java", 9, 23, 9, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DBIClient.java", 19, 31, 19, 47), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DatabaseInTimeZone.java", 3, 23, 3, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DatabaseInTimeZone.java", 13, 40, 13, 56), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DAOTestRule.java", 10, 23, 10, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DAOTestRule.java", 47, 33, 47, 49), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 13, 23, 13, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 69, 64, 69, 80), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 10, 23, 10, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 54, 42, 54, 58), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null))
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
                    ElementKind.CLASS,
                    "ExternalResource",
                    "org.junit.rules.ExternalResource",
                    "ExternalResource",
                    "org.junit.rules",
                    null
            );

            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DBIClient.java", 9, 23, 9, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DBIClient.java", 19, 31, 19, 47), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DatabaseInTimeZone.java", 3, 23, 3, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-jdbi/src/test/java/io/dropwizard/jdbi/timestamps/DatabaseInTimeZone.java", 13, 40, 13, 56), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DAOTestRule.java", 10, 23, 10, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DAOTestRule.java", 47, 33, 47, 49), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 13, 23, 13, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardAppRule.java", 69, 64, 69, 80), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 10, 23, 10, 39), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null)),
                            ReferenceInformation.of(Location.of("file:///dropwizard-testing/src/main/java/io/dropwizard/testing/junit/DropwizardClientRule.java", 54, 42, 54, 58), SymbolDescriptor.of(ElementKind.CLASS, "ExternalResource", "org.junit.rules.ExternalResource", "ExternalResource", "org.junit.rules", null))
                    )
            );
            LOG.info("Warm XReferences took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 12000)
    public void e_ReferencesToLocalVar() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java",
                    42, 21,
                    false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 37, 13, 37, 23),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 42, 13, 42, 23),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 57, 68, 57, 78),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 58, 83, 58, 93),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 81, 12, 81, 22),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 82, 29, 82, 39)
                    )
            );
            LOG.info("Cold References (local var) took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 12000)
    public void f_ReferencesToPackageLevelElement() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/WebApplicationExceptionCatchingFilter.java",
                    14, 37,
                    false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/AuthDynamicFeature.java", 58, 45, 58, 82),
                            Location.of("file:///dropwizard-auth/src/main/java/io/dropwizard/auth/PolymorphicAuthDynamicFeature.java", 50, 45, 50, 82)

                    )
            );
            LOG.info("Cold References (local var) took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

}
