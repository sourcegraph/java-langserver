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
public class ApacheCommonsIo extends BenchmarkTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheCommonsIo.class);

    private static final String REPO = "commons-io";

    /**
     * IMPORTANT: Test names define tests execution order
     */
    @Test
    public void _warmup() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            harness.doHover("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java",
                    545, 24);
        }
    }

    @Test(timeout = 10000)
    public void a_coldXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java",
                    545, 24,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 315, 23, 315, 35),
                                    SymbolDescriptor.of(ElementKind.METHOD, "closeQuietly", "org.apache.commons.io.IOUtils.closeQuietly(java.io.OutputStream)", "IOUtils", "org.apache.commons.io", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.6-SNAPSHOT", null, null))
                            )
                    )
);
            LOG.info("Cold XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 500)
    public void a_warmXDefinition() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectXDefinition("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java",
                    545, 24,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 315, 23, 315, 35),
                                    SymbolDescriptor.of(ElementKind.METHOD, "closeQuietly", "org.apache.commons.io.IOUtils.closeQuietly(java.io.OutputStream)", "IOUtils", "org.apache.commons.io", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.6-SNAPSHOT", null, null))
                            )
                    )
);
            LOG.info("Warm XDefinition took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 10000)
    public void b_coldSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("FileUtils",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("FileUtils", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 78, 13, 78, 22)),
                            SymbolInformation.of("FileUtils()", SymbolKind.METHOD, "FileUtils", Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 83, 11, 83, 20)),
                            SymbolInformation.of("FileUtilsCleanDirectoryTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsCleanDirectoryTestCase.java", 36, 13, 36, 44)),
                            SymbolInformation.of("FileUtilsCleanSymlinksTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsCleanSymlinksTestCase.java", 35, 13, 35, 43)),
                            SymbolInformation.of("FileUtilsDirectoryContainsTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsDirectoryContainsTestCase.java", 37, 13, 37, 47)),
                            SymbolInformation.of("FileUtilsFileNewerTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsFileNewerTestCase.java", 36, 13, 36, 39)),
                            SymbolInformation.of("FileUtilsListFilesTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsListFilesTestCase.java", 38, 13, 38, 39)),
                            SymbolInformation.of("FileUtilsTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsTestCase.java", 46, 13, 46, 30)),
                            SymbolInformation.of("FileUtilsTestCase()", SymbolKind.METHOD, "FileUtilsTestCase", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsTestCase.java", 81, 11, 81, 28)),
                            SymbolInformation.of("FileUtilsWaitForTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsWaitForTestCase.java", 31, 13, 31, 37))
                    )
            );
            LOG.info("Cold Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 500)
    public void b_warmSymbol() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectWorkspaceSymbol("FileUtils",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("FileUtils", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 78, 13, 78, 22)),
                            SymbolInformation.of("FileUtils()", SymbolKind.METHOD, "FileUtils", Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 83, 11, 83, 20)),
                            SymbolInformation.of("FileUtilsCleanDirectoryTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsCleanDirectoryTestCase.java", 36, 13, 36, 44)),
                            SymbolInformation.of("FileUtilsCleanSymlinksTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsCleanSymlinksTestCase.java", 35, 13, 35, 43)),
                            SymbolInformation.of("FileUtilsDirectoryContainsTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsDirectoryContainsTestCase.java", 37, 13, 37, 47)),
                            SymbolInformation.of("FileUtilsFileNewerTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsFileNewerTestCase.java", 36, 13, 36, 39)),
                            SymbolInformation.of("FileUtilsListFilesTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsListFilesTestCase.java", 38, 13, 38, 39)),
                            SymbolInformation.of("FileUtilsTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsTestCase.java", 46, 13, 46, 30)),
                            SymbolInformation.of("FileUtilsTestCase()", SymbolKind.METHOD, "FileUtilsTestCase", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsTestCase.java", 81, 11, 81, 28)),
                            SymbolInformation.of("FileUtilsWaitForTestCase", SymbolKind.CLASS, "org.apache.commons.io", Location.of("file:///src/test/java/org/apache/commons/io/FileUtilsWaitForTestCase.java", 31, 13, 31, 37))
                    )
            );
            LOG.info("Warm Symbol took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 10000)
    public void c_coldReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///src/main/java/org/apache/commons/io/IOUtils.java", 314, 4, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 110, 1, 110, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 151, 5, 151, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 243, 5, 243, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 286, 5, 286, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 336, 5, 336, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 36, 1, 36, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 54, 5, 54, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 70, 5, 70, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 85, 5, 85, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 101, 5, 101, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 114, 5, 114, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 142, 5, 142, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 140, 5, 140, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 171, 5, 171, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 202, 5, 202, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 220, 5, 220, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 240, 5, 240, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1765, 5, 1765, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1826, 5, 1826, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1973, 5, 1973, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1989, 5, 1989, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 2003, 5, 2003, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 2019, 5, 2019, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOExceptionWithCause.java", 27, 1, 27, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 218, 5, 218, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 249, 5, 249, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 281, 5, 281, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 314, 5, 314, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 358, 5, 358, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 416, 5, 416, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 453, 5, 453, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 491, 5, 491, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 529, 5, 529, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 846, 5, 846, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 908, 5, 908, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 977, 5, 977, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1061, 5, 1061, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1139, 5, 1139, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1182, 5, 1182, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1228, 5, 1228, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1394, 5, 1394, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1582, 5, 1582, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1631, 5, 1631, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1730, 5, 1730, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1838, 5, 1838, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1922, 5, 1922, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2003, 5, 2003, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2064, 5, 2064, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2086, 5, 2086, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2111, 5, 2111, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2135, 5, 2135, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2415, 5, 2415, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2632, 5, 2632, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/LineIterator.java", 188, 5, 188, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/FileFilterUtils.java", 388, 5, 388, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/FileFilterUtils.java", 403, 5, 403, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/WildcardFilter.java", 50, 1, 50, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/input/ReaderInputStream.java", 183, 5, 183, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/input/ReversedLinesFileReader.java", 62, 5, 62, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/ByteArrayOutputStream.java", 388, 5, 388, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 5, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/WriterOutputStream.java", 200, 5, 200, 15)
                    ));
            LOG.info("Cold References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore // TODO: do this in a way that doesn't send messages to a server that's received a shutdown request
    @Test(timeout = 2000)
    public void c_warmReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            harness.expectReferences("file:///src/main/java/org/apache/commons/io/IOUtils.java", 314, 4, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 110, 1, 110, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 151, 5, 151, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 243, 5, 243, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 286, 5, 286, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/CopyUtils.java", 336, 5, 336, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 36, 1, 36, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 54, 5, 54, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 70, 5, 70, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 85, 5, 85, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 101, 5, 101, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 114, 5, 114, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileCleaner.java", 142, 5, 142, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 140, 5, 140, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 171, 5, 171, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 202, 5, 202, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 220, 5, 220, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileSystemUtils.java", 240, 5, 240, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1765, 5, 1765, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1826, 5, 1826, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1973, 5, 1973, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 1989, 5, 1989, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 2003, 5, 2003, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/FileUtils.java", 2019, 5, 2019, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOExceptionWithCause.java", 27, 1, 27, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 218, 5, 218, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 249, 5, 249, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 281, 5, 281, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 314, 5, 314, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 358, 5, 358, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 416, 5, 416, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 453, 5, 453, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 491, 5, 491, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 529, 5, 529, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 846, 5, 846, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 908, 5, 908, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 977, 5, 977, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1061, 5, 1061, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1139, 5, 1139, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1182, 5, 1182, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1228, 5, 1228, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1394, 5, 1394, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1582, 5, 1582, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1631, 5, 1631, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1730, 5, 1730, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1838, 5, 1838, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 1922, 5, 1922, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2003, 5, 2003, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2064, 5, 2064, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2086, 5, 2086, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2111, 5, 2111, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2135, 5, 2135, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2415, 5, 2415, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 2632, 5, 2632, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/LineIterator.java", 188, 5, 188, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/FileFilterUtils.java", 388, 5, 388, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/FileFilterUtils.java", 403, 5, 403, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/filefilter/WildcardFilter.java", 50, 1, 50, 11),
                            Location.of("file:///src/main/java/org/apache/commons/io/input/ReaderInputStream.java", 183, 5, 183, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/input/ReversedLinesFileReader.java", 62, 5, 62, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/ByteArrayOutputStream.java", 388, 5, 388, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 4, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/LockableFileWriter.java", 130, 5, 130, 15),
                            Location.of("file:///src/main/java/org/apache/commons/io/output/WriterOutputStream.java", 200, 5, 200, 15)
                    ));
            LOG.info("Warm References took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Test(timeout = 10000)
    public void d_coldXReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.makeHarness(REPO)) {
            long start = System.currentTimeMillis();
            SymbolDescriptor descriptor = SymbolDescriptor.of(
                    ElementKind.CLASS,
                    "AssertionFailedError",
                    "junit.framework.AssertionFailedError",
                    "AssertionFailedError",
                    "junit.framework",
                    null
            );
            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 18, 23, 18, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 178, 22, 178, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 189, 22, 189, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnCloseInputStream.java", 21, 23, 21, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnCloseInputStream.java", 40, 18, 40, 38), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 21, 23, 21, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 48, 22, 48, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 57, 22, 57, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null))
                    )
            );
            LOG.info("Cold XReferences took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    @Ignore
    @Test(timeout = 2000)
    public void d_warmXReferences() throws Exception {
        try (Harness harness = BenchmarkTestBase.getHarness()) {
            long start = System.currentTimeMillis();
            SymbolDescriptor descriptor = SymbolDescriptor.of(
                    ElementKind.CLASS,
                    "AssertionFailedError",
                    "junit.framework.AssertionFailedError",
                    "AssertionFailedError",
                    "junit.framework",
                    null
            );
            harness.expectXReferences(descriptor, Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 18, 23, 18, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 178, 22, 178, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/TestUtils.java", 189, 22, 189, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnCloseInputStream.java", 21, 23, 21, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnCloseInputStream.java", 40, 18, 40, 38), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 21, 23, 21, 43), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 48, 22, 48, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null)),
                            ReferenceInformation.of(Location.of("file:///src/test/java/org/apache/commons/io/testtools/YellOnFlushAndCloseOutputStream.java", 57, 22, 57, 42), SymbolDescriptor.of(ElementKind.CLASS, "AssertionFailedError", "junit.framework.AssertionFailedError", "AssertionFailedError", "junit.framework", null))
                    )
            );
            LOG.info("Warm XReferences took {}", (System.currentTimeMillis() - start) / 1000.0);
        }
    }
}
