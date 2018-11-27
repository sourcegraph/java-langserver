package com.sourcegraph.lsp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sourcegraph.common.Config;
import com.sourcegraph.langserver.SlowTestCategory;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.OpenJDK;
import com.sourcegraph.langserver.langservice.workspace.standardlibs.OpenJDKLangTools;
import com.sourcegraph.lsp.domain.result.*;
import com.sourcegraph.lsp.domain.structures.*;
import io.opentracing.mock.MockSpan;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.lang.model.element.ElementKind;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * ControllerTest contains integration tests for the Java Language Server.
 * <p>
 * Created by Beyang on 1/26/17.
 */
public class ControllerTest {

    /**
     * testConfig verifies the proper config is set for tests. Some tests may fail or be unreliable indicators
     * of correctness unless certain configuration parameters (e.g., paths to Android JARs, disabling
     * the dependency resolution cache) are set.
     */
    @Test
    public void testConfig() {
        assertNotNull("$ANDROID_JAR_PATH not set. Set it or Android-dependent tests will fail. To set it, install the Android SDK via Android Studio and set $ANDROID_JAR_PATH to the path to android.jar", Config.ANDROID_JAR_PATH);
        assertTrue("$NO_CACHE should be true for tests", Config.IGNORE_DEPENDENCY_RESOLUTION_CACHE);
    }

    @Test
    public void testLightstepIncludeSensitiveSet() {
        assertTrue("LIGHTSTEP_INCLUDE_SENSITIVE not set or disabled. Set it or tracing tests will fail", Config.LIGHTSTEP_INCLUDE_SENSITIVE);
    }

    @Test
    public void testTraceHover() throws Exception {
        try (Harness h = Harness.newHarness("java-simple")) {
            h.doHover("file:///src/main/java/com/foo/A.java", 5, 6);
            // giving some time to finish spans
            Thread.sleep(1000);
            List<MockSpan> spans = Harness.tracer.finishedSpans();
            List<Map<String, Object>> expSpanProps = Lists.newArrayList(
                    ImmutableMap.of(
                            "operationName", "LSP lang server: initialize",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"rootUri\":\"file:///\",\"capabilities\":{\"streaming\":false}}"
                            )
                    ),
                    ImmutableMap.of(
                            "operationName", "analyze",
                            "tags", ImmutableMap.of("filename", "/src/main/java/com/foo/A.java")
                    ),
                    ImmutableMap.of(
                            "operationName", "LSP lang server: textDocument/hover",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"textDocument\":{\"uri\":\"file:///src/main/java/com/foo/A.java\"},\"position\":{\"line\":5,\"character\":6}}"
                            )
                    )
            );
            assertTraces(spans, expSpanProps);
        }
    }

    @Test
    public void testTraceDefinition() throws Exception {
        try (Harness h = Harness.newHarness("java-simple")) {
            h.doDefinition("file:///src/main/java/com/foo/A.java", 5, 6);
            // giving some time to finish spans
            Thread.sleep(1000);
            List<MockSpan> spans = Harness.tracer.finishedSpans();
            List<Map<String, Object>> expSpanProps = Lists.newArrayList(
                    ImmutableMap.of(
                            "operationName", "LSP lang server: initialize",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"rootUri\":\"file:///\",\"capabilities\":{\"streaming\":false}}"
                            )
                    ),
                    ImmutableMap.of(
                            "operationName", "analyze",
                            "tags", ImmutableMap.of("filename", "/src/main/java/com/foo/A.java")
                    ),
                    ImmutableMap.of(
                            "operationName", "LSP lang server: textDocument/definition",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"textDocument\":{\"uri\":\"file:///src/main/java/com/foo/A.java\"},\"position\":{\"line\":5,\"character\":6}}"
                            )
                    )
            );
            assertTraces(spans, expSpanProps);
        }
    }

    @Test
    public void testTraceSymbol() throws Exception {
        try (Harness h = Harness.newHarness("java-simple")) {
            h.doWorkspaceSymbol("test");
            // giving some time to finish spans
            Thread.sleep(1000);
            List<MockSpan> spans = Harness.tracer.finishedSpans();
            List<Map<String, Object>> expSpanProps = Lists.newArrayList(
                    ImmutableMap.of(
                            "operationName", "LSP lang server: initialize",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"rootUri\":\"file:///\",\"capabilities\":{\"streaming\":false}}"
                            )
                    ),
                    ImmutableMap.of(
                            "operationName", "LSP lang server: workspace/symbol",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"query\":\"test\"}"
                            )
                    )
            );
            assertTraces(spans, expSpanProps);
        }
    }

    @Test
    public void testTraceReferences() throws Exception {
        try (Harness h = Harness.newHarness("java-simple")) {
            h.doReferences("file:///src/main/java/com/foo/A.java", 5, 13, true, null);
            // giving some time to finish spans
            Thread.sleep(1000);
            List<MockSpan> spans = Harness.tracer.finishedSpans();
            List<Map<String, Object>> expSpanProps = Lists.newArrayList(
                    ImmutableMap.of(
                            "operationName", "LSP lang server: initialize",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"rootUri\":\"file:///\",\"capabilities\":{\"streaming\":false}}"
                            )
                    ),
                    ImmutableMap.of(
                            "operationName", "analyze",
                            "tags", ImmutableMap.of("filename", "/src/main/java/com/foo/A.java")
                    ),
                    ImmutableMap.of(
                            "operationName", "analyze",
                            "tags", ImmutableMap.of("filename", "/src/main/java/com/foo/A.java")
                    ),
                    ImmutableMap.of(
                            "operationName", "analyze",
                            "tags", ImmutableMap.of("filename", "/src/main/java/com/foo/B.java")
                    ),
                    ImmutableMap.of(
                            "operationName", "LSP lang server: textDocument/references",
                            "tags", ImmutableMap.of(
                                    "mode", "java",
                                    "params", "{\"textDocument\":{\"uri\":\"file:///src/main/java/com/foo/A.java\"},\"position\":{\"line\":5,\"character\":13},\"context\":{\"includeDeclaration\":true,\"xlimit\":null}}"
                            )
                    )
            );
            assertTraces(spans, expSpanProps);
        }
    }

    /**
     * helper method for comparing sets of spans with expected span properties in order-agnostic manner.
     */
    private static void assertTraces(List<MockSpan> spans, List<Map<String, Object>> expSpanProps) {
        List<Map<String, Object>> gotSpanProps = spans.stream().map(s -> ImmutableMap.of(
                "operationName", s.operationName(),
                "tags", s.tags()
        )).collect(Collectors.toList());
        expSpanProps.sort(Comparator.comparing(p -> (String) p.get("operationName")));
        spans.sort(Comparator.comparing(MockSpan::operationName));
        assertEquals("Unexpected number of spans. Got:\n" +
                        gotSpanProps.toString() +
                        "\nwhile expected:\n" +
                        expSpanProps.toString(),
                spans.size(),
                expSpanProps.size());
        for (int i = 0; i < spans.size(); i++) {
            assertEquals("Unexpected operation name", expSpanProps.get(i).get("operationName"), spans.get(i).operationName());
            assertEquals("Unexpected tags", expSpanProps.get(i).get("tags"), spans.get(i).tags());
        }
    }

    @Test
    public void testSimple() {
        try (Harness h = Harness.newHarness("java-simple")) {
            h.expectHover("file:///src/main/java/com/foo/A.java", 5, 13, Hover.of(
                    MarkedString.of(Hover.LANGUAGE_JAVA, "public class com.foo.A"),
                    MarkedString.of(Hover.LANGUAGE_MARKDOWN, " This is a javadoc for A\n"))
            );
            h.expectHover("file:///src/main/java/com/foo/B.java", 15, 10, Hover.of(
                    MarkedString.of(Hover.LANGUAGE_JAVA, "public void doSomething()"),
                    MarkedString.of(Hover.LANGUAGE_MARKDOWN, " Do something!\n")
            ));
            h.expectDefinition("file:///src/main/java/com/foo/B.java", 15, 10,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/foo/A.java", 15, 16, 15, 27)
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/foo/B.java", 15, 10,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/foo/A.java", 15, 16, 15, 27),
                                    SymbolDescriptor.of(ElementKind.METHOD, "doSomething", "com.foo.A.doSomething()", "A", "com.foo", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "__inferred__:__inferred__", "0.0.0", null, null))
                            )
                    )
            );
            h.expectWorkspaceSymbol("A",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("A", SymbolKind.CLASS, "com.foo", Location.of("file:///src/main/java/com/foo/A.java", 5, 13, 5, 14)),
                            SymbolInformation.of("A(String x)", SymbolKind.METHOD, "A", Location.of("file:///src/main/java/com/foo/A.java", 8, 11, 8, 12)),
                            SymbolInformation.of("a", SymbolKind.FIELD, "B", Location.of("file:///src/main/java/com/foo/B.java", 4, 14, 4, 15)),
                            SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32)),
                            SymbolInformation.of("getA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 10, 13, 10, 17))
                    )
            );
            h.expectWorkspaceSymbol("doSomethingWithA", WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectWorkspaceSymbol(SymbolDescriptor.of(null, "doSomethingWithA", null, null, null, null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.METHOD, "doSomethingWithA", "com.foo.B.doSomethingWithA()", "B", "com.foo", null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectReferences("file:///src/main/java/com/foo/A.java", 5, 13, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/foo/A.java", 5, 13, 5, 14),
                            Location.of("file:///src/main/java/com/foo/B.java", 4, 12, 4, 13),
                            Location.of("file:///src/main/java/com/foo/B.java", 7, 21, 7, 22),
                            Location.of("file:///src/main/java/com/foo/B.java", 10, 11, 10, 12)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testSimpleJavaConfig() {
        try (Harness h = Harness.newHarness("java-simple-javaconfig")) {
            h.expectHover("file:///src/main/java/com/foo/A.java", 5, 13, Hover.of(
                    MarkedString.of(Hover.LANGUAGE_JAVA, "public class com.foo.A"),
                    MarkedString.of(Hover.LANGUAGE_MARKDOWN, " This is a javadoc for A\n"))
            );
            h.expectHover("file:///src/main/java/com/foo/B.java", 15, 10, Hover.of(
                    MarkedString.of(Hover.LANGUAGE_JAVA, "public void doSomething()"),
                    MarkedString.of(Hover.LANGUAGE_MARKDOWN, " Do something!\n")
            ));
            h.expectDefinition("file:///src/main/java/com/foo/B.java", 15, 10,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/foo/A.java", 15, 16, 15, 27)
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/foo/B.java", 15, 10,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/foo/A.java", 15, 16, 15, 27),
                                    SymbolDescriptor.of(ElementKind.METHOD, "doSomething", "com.foo.A.doSomething()", "A", "com.foo", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "myGroup:myId", "1.0.0", null, null))
                            )
                    )
            );
            h.expectWorkspaceSymbol("A",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("A", SymbolKind.CLASS, "com.foo", Location.of("file:///src/main/java/com/foo/A.java", 5, 13, 5, 14)),
                            SymbolInformation.of("A(String x)", SymbolKind.METHOD, "A", Location.of("file:///src/main/java/com/foo/A.java", 8, 11, 8, 12)),
                            SymbolInformation.of("a", SymbolKind.FIELD, "B", Location.of("file:///src/main/java/com/foo/B.java", 4, 14, 4, 15)),
                            SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32)),
                            SymbolInformation.of("getA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 10, 13, 10, 17))
                    )
            );
            h.expectWorkspaceSymbol("doSomethingWithA", WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectWorkspaceSymbol(SymbolDescriptor.of(null, "doSomethingWithA", null, null, null, null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.METHOD, "doSomethingWithA", "com.foo.B.doSomethingWithA()", "B", "com.foo", null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomethingWithA()", SymbolKind.METHOD, "B", Location.of("file:///src/main/java/com/foo/B.java", 14, 16, 14, 32))
            ));
            h.expectReferences("file:///src/main/java/com/foo/A.java", 5, 13, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/foo/A.java", 5, 13, 5, 14),
                            Location.of("file:///src/main/java/com/foo/B.java", 4, 12, 4, 13),
                            Location.of("file:///src/main/java/com/foo/B.java", 7, 21, 7, 22),
                            Location.of("file:///src/main/java/com/foo/B.java", 10, 11, 10, 12)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testLanguageFeatures() {
        try (Harness h = Harness.newHarness("cup-of-joe")) {
            // hover over a variable containing an instantiation of a generic
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    17,
                    20,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "com.sourcegraph.cup.Cup<java.lang.Double> floatingCup")
                    )
            );

            // hover over an instantiated type -- should retrieve the generic definition and doc comment
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    17,
                    8,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public class com.sourcegraph.cup.Cup<T>"),
                            MarkedString.of(Hover.LANGUAGE_MARKDOWN, " A cup of T\n @param <T> (whatever you want)\n")
                    )
            );

            // hover over a "normal" element
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    19,
                    8,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "private static final org.slf4j.Logger log")
                    )
            );

            // hover over an element (of a different type) that shadows the preceding element
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    22,
                    19,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "java.lang.String log")
                    )
            );

            // hover over a later occurrence of the previously shadowed element
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    30,
                    8,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "private static final org.slf4j.Logger log")
                    )
            );

            // hover over a call to a generic method -- should retrieve the generic definition and doc comment
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    26,
                    20,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public void <U> refill(U moreStuff)"),
                            MarkedString.of(Hover.LANGUAGE_MARKDOWN, " Refill the cup with U (which had better be castable to T!)\n @param moreStuff\n @param <U> (will be cast to T)\n")
                    )
            );

            // hover over a call to another generic method
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    28,
                    41,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public T pour()")
                    )
            );

            // hover over a call to a standard library function
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    37,
                    31,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public void println(java.lang.String arg0)")
                    )
            );

            // hover over an annotation
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/bowl/Bowl.java",
                    12,
                    6,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public abstract @interface com.fasterxml.jackson.annotation.JsonInclude")
                    )
            );

            // hover over an anonymous class instantiation should return the class data, not the constructor
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    45, 12,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public class com.sourcegraph.cup.Cup<T>"),
                            MarkedString.of(Hover.LANGUAGE_MARKDOWN, " A cup of T\n @param <T> (whatever you want)\n")
                    )
            );

            // hover over a consructor
            h.expectHover(
                    "file:///src/main/java/com/sourcegraph/bowl/Bowl.java",
                    26,
                    11,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public Bowl(T stuff)")
                    )
            );

            // definition of an instantiation of a generic class should jump to the generic class declaration
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    17,
                    8,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 13, 12, 16)
                    )
            );

            // definition of a use of a generic method should jump to the generic method declaration
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    26,
                    20,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 44, 20, 44, 26)
                    )
            );

            // definition of a local variable (of an instantiation of a generic type)
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    26,
                    8,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 20, 17, 31)
                    )
            );

            // definition of a local variable that shadows an outer variable should jump to the shadowing declaration
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    23,
                    31,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 22, 19, 22, 22)
                    )
            );

            // definition of a local variable that was shadowed in preceding code should jump to the shadowed declaration
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    30,
                    8,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 8, 32, 8, 35)
                    )
            );

            // definition of an enhanced for-loop iteration variable in a static initializer block
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                    21,
                    22,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 20, 20, 20, 24)
                    )
            );

            // definition of a reference to a parameter in a method body
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                    48,
                    53,
                    TextDocumentDefinitionResult.of(
                            Location.of(
                                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                                    44,
                                    29,
                                    44,
                                    38
                            )
                    )
            );

            // definition of a reference to a parameter in a method body
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                    49,
                    12,
                    TextDocumentDefinitionResult.of(
                            Location.of(
                                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                                    47,
                                    27,
                                    47,
                                    28
                            )
                    )
            );

            // definition of a class instantiation should jump to the constructor
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    17,
                    38,
                    TextDocumentDefinitionResult.of(
                            Location.of(
                                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                                    27,
                                    11,
                                    27,
                                    14
                            )
                    )
            );

            // definition of an anonymous class instantiation should jump to the class, not the constructor
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java", 45, 12,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 13, 12, 16)
                    )
            );

            // definition of a method call chained to an anonymous class instantiation
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java", 45, 42,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 44, 20, 44, 26)
                    )
            );

            h.expectReferences(
                    "file:///src/main/java/com/sourcegraph/App.java",
                    17,
                    20,
                    true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 20, 17, 31),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 19, 43, 19, 54),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 26, 8, 26, 19),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 28, 29, 28, 40)
                    )
            );

            h.expectReferences(
                    "file:///src/main/java/com/sourcegraph/cup/Cup.java",
                    44,
                    20,
                    true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 26, 20, 26, 26),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 45, 42, 45, 48),
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 44, 20, 44, 26)
                    )
            );

            h.expectReferences(
                    "file:///src/main/java/com/sourcegraph/bowl/Bowl.java",
                    12,
                    4,
                    true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 43, 5, 43, 16),
                            Location.of("file:///src/main/java/com/sourcegraph/bowl/Bowl.java", 9, 1, 9, 12),
                            Location.of("file:///src/main/java/com/sourcegraph/bowl/Bowl.java", 12, 5, 12, 16),
                            Location.of("file:///src/main/java/com/sourcegraph/bowl/Bowl.java", 34, 5, 34, 16)
                    )
            );

            // also make sure we can find definitions and references for annotations
            h.expectDefinition(
                    "file:///src/main/java/com/sourcegraph/App.java", 48, 5,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/Shiny.java", 8, 18, 8, 23)
                    )
            );

            h.expectReferences(
                    "file:///src/main/java/com/sourcegraph/Shiny.java", 8, 18, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 48, 5, 48, 10),
                            Location.of("file:///src/main/java/com/sourcegraph/Shiny.java", 7, 0, 7, 24),
                            Location.of("file:///src/main/java/com/sourcegraph/Shiny.java", 7, 0, 7, 24)
                    )
            );

            // synthetic constructor hover
            h.expectHover("file:///src/main/java/com/sourcegraph/App.java",
                    11,
                    23,
                    Hover.of(
                            MarkedString.of("java", "public class com.sourcegraph.App")
                    )
            );
            // synthetic constructor xdefinition
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/App.java",
                    11,
                    23,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/App.java", 6, 13, 6, 16),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "App", "com.sourcegraph.App.App()", "App", "com.sourcegraph", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))
                            )
                    )
            );
            // synthetic constructor definition
            h.expectDefinition("file:///src/main/java/com/sourcegraph/App.java",
                    11,
                    23,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 6, 13, 6, 16)
                    )
            );
            // synthetic constructor calls should be included into class's references
            h.expectReferences("file:///src/main/java/com/sourcegraph/App.java",
                    6,
                    13,
                    false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 8, 62, 8, 65),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 11, 8, 11, 11),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 11, 22, 11, 25)
                    ));

            // constructor's references
            h.expectReferences("file:///src/main/java/com/sourcegraph/cup/Cup.java", 27, 11, false, TextDocumentReferencesResult.of(
                    Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 34, 17, 49)
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testLanguageFeaturesDocumentSymbol() {
        try (Harness h = Harness.newHarness("cup-of-joe")) {
            // document symbols
            h.expectDocumentSymbol("file:///src/main/java/com/sourcegraph/cup/Cup.java",
                    DocumentSymbolResult.of(
                            SymbolInformation.of("Cup", SymbolKind.CLASS, "com.sourcegraph.cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 13, 12, 16)),
                            SymbolInformation.of("log", SymbolKind.FIELD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 14, 32, 14, 35)),
                            SymbolInformation.of("SIZES", SymbolKind.FIELD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 16, 31, 16, 36)),
                            SymbolInformation.of("stuff", SymbolKind.FIELD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 25, 14, 25, 19)),
                            SymbolInformation.of("Cup(T stuff)", SymbolKind.METHOD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 27, 11, 27, 14)),
                            SymbolInformation.of("pour()", SymbolKind.METHOD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 31, 13, 31, 17)),
                            SymbolInformation.of("toString()", SymbolKind.METHOD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 35, 18, 35, 26)),
                            SymbolInformation.of("<U>refill(U moreStuff)", SymbolKind.METHOD, "Cup", Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 44, 20, 44, 26))
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSimplePom() {
        try (Harness h = Harness.newHarness("java-simple-pom")) {
            // Stdlib hovers
            h.expectHover("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 8,
                    Hover.of(MarkedString.of(Hover.LANGUAGE_JAVA, "public final class java.lang.System")));
            h.expectHover("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 15,
                    Hover.of(MarkedString.of(Hover.LANGUAGE_JAVA, "public static final java.io.PrintStream out")));
            h.expectHover("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 22,
                    Hover.of(MarkedString.of(Hover.LANGUAGE_JAVA, "public void println(java.lang.String arg0)")));
            h.expectHover("file:///src/main/java/com/sourcegraph/simple/Main.java", 3, 52,
                    Hover.of(MarkedString.of(Hover.LANGUAGE_JAVA, "public class java.lang.Exception")));

            // Stdlib xdefinition
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 8, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(null,
                            SymbolDescriptor.of(ElementKind.CLASS, "System", "java.lang.System", "System", "java.lang",
                                    OpenJDK.PACKAGE_IDENTIFIER))
            ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 15, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(null,
                            SymbolDescriptor.of(ElementKind.FIELD, "out", "java.lang.System.out", "System", "java.lang",
                                    OpenJDK.PACKAGE_IDENTIFIER))
            ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 22,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.METHOD, "println", "java.io.PrintStream.println(String)", "PrintStream", "java.io", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null))
                            )
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 3, 52, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(null,
                            SymbolDescriptor.of(ElementKind.CLASS, "Exception", "java.lang.Exception", "Exception", "java.lang",
                                    OpenJDK.PACKAGE_IDENTIFIER))
            ));

            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/B.java", 8, 16, TextDocumentDefinitionResult.of(
                    Location.of("file:///src/main/java/com/sourcegraph/simple/A.java", 12, 16, 12, 27)
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/B.java", 8, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/A.java", 12, 16, 12, 27),
                                    SymbolDescriptor.of(ElementKind.METHOD, "doSomething", "com.sourcegraph.simple.A.doSomething()", "A", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    )
            );
            h.expectWorkspaceSymbol("specialmap",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("SpecialMap", SymbolKind.CLASS, "com.sourcegraph.simple", Location.of("file:///src/main/java/com/sourcegraph/simple/SpecialMap.java", 7, 13, 7, 23))
                    )
            );
            h.expectWorkspaceSymbol("SpecialMap", WorkspaceSymbolResult.of(
                    SymbolInformation.of("SpecialMap", SymbolKind.CLASS, "com.sourcegraph.simple", Location.of("file:///src/main/java/com/sourcegraph/simple/SpecialMap.java", 7, 13, 7, 23))
            ));
            h.expectWorkspaceSymbol("Special", WorkspaceSymbolResult.of(
                    SymbolInformation.of("SpecialMap", SymbolKind.CLASS, "com.sourcegraph.simple", Location.of("file:///src/main/java/com/sourcegraph/simple/SpecialMap.java", 7, 13, 7, 23))
            ));
            h.expectWorkspaceSymbol("resources", WorkspaceSymbolResult.of(
                    SymbolInformation.of("Resources", SymbolKind.CLASS, "com.sourcegraph.resources", Location.of("file:///src/main/java/com/sourcegraph/resources/Resources.java", 2, 19, 2, 28))
            ));

            // workspace/symbol with symbol query whose simple name will match other classes
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.CLASS, "Map", null, null, null, null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("Map", SymbolKind.CLASS, "com.sourcegraph.simple", Location.of("file:///src/main/java/com/sourcegraph/simple/Map.java", 5, 13, 5, 16))
            ));
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.METHOD, "doSomething", "com.sourcegraph.simple.A.doSomething()", "A", "com.sourcegraph.simple", null), WorkspaceSymbolResult.of(
                    SymbolInformation.of("doSomething()", SymbolKind.METHOD, "A", Location.of("file:///src/main/java/com/sourcegraph/simple/A.java", 12, 16, 12, 27))
            ));

            // reference to internal class
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/SpecialMap.java", 7, 32, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Maps.java", 7, 24, 7, 30),
                            SymbolDescriptor.of(
                                    ElementKind.CLASS, "FooMap", "com.sourcegraph.simple.Maps.FooMap", "Maps", "com.sourcegraph.simple",
                                    PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)
                            ))));
            // overloaded ctors and methods
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 4, 11, 12),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.lang.String)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 4, 15, 12),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 4, 19, 12),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(T,java.util.List<T>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 4, 23, 12),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.util.Collection<? extends java.lang.Double>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 15,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 13, 26, 16),
                                    SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 14, 30, 17),
                                    SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(T)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 15, 34, 18),
                                    SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 15,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 13, 38, 16),
                                    SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(java.lang.Long)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))
                            )
                    )
            );
            h.expectXReferences(SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 12, 8, 12, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 16, 8, 16, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 43, 8, 43, 22), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 48, 29, 48, 51), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    )
            );
            h.expectXReferences(SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.lang.String)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 4, 11, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.lang.String)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 45, 8, 45, 26), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.lang.String)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 4, 15, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 20, 8, 20, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 44, 8, 44, 24), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(T,java.util.List<T>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 4, 19, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(T,java.util.List<T>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 46, 8, 46, 58), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(T,java.util.List<T>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.util.Collection<? extends java.lang.Double>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 4, 23, 12), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.util.Collection<? extends java.lang.Double>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 47, 8, 47, 45), SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "Overload", "com.sourcegraph.simple.Overload.Overload(java.util.Collection<? extends java.lang.Double>)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 13, 26, 16), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 49, 10, 49, 13), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo()", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(T)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 14, 30, 17), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(T)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 50, 10, 50, 13), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(T)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 15, 34, 18), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 52, 17, 52, 20), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(int)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    ));
            h.expectXReferences(SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(java.lang.Long)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 13, 38, 16), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(java.lang.Long)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 51, 10, 51, 13), SymbolDescriptor.of(ElementKind.METHOD, "foo", "com.sourcegraph.simple.Overload.foo(java.lang.Long)", "Overload", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0", null, null)))
                    )
            );
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 12, 11,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 20, 11,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 4, 15, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 43, 15,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 44, 15,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 4, 15, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 45, 15,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 4, 11, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 46, 15,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 4, 19, 12)
                    )
            );
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 47, 15,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 4, 23, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 48, 40,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12)
                    ));
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 49, 12,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 13, 26, 16)
                    )
            );
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 50, 12,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 14, 30, 17)
                    )
            );
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 51, 12,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 13, 38, 16)
                    )
            );
            h.expectDefinition("file:///src/main/java/com/sourcegraph/simple/Overload.java", 52, 18,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 15, 34, 18)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 5, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 12, 8, 12, 12),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 16, 8, 16, 12),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 43, 8, 43, 22),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 48, 29, 48, 51)
                    ));
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 5, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 45, 8, 45, 26))
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 5, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 20, 8, 20, 12),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 44, 8, 44, 24)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 5, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 46, 8, 46, 58)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 5, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 47, 8, 47, 45)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 14, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 49, 10, 49, 13)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 15, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 50, 10, 50, 13)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 16, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 52, 17, 52, 20)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 14, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 51, 10, 51, 13)
                    )
            );
            // ctor search
            h.expectWorkspaceSymbol("overload",
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Overload", SymbolKind.CLASS, "com.sourcegraph.simple", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 6, 13, 6, 21)),
                            SymbolInformation.of("Overload()", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 8, 4, 8, 12)),
                            SymbolInformation.of("Overload(Collection<? extends Double> collection)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 23, 4, 23, 12)),
                            SymbolInformation.of("Overload(String value)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 11, 4, 11, 12)),
                            SymbolInformation.of("Overload(T first, List<T> second)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 19, 4, 19, 12)),
                            SymbolInformation.of("Overload(int value)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 15, 4, 15, 12))
                    )
            );
            // overloaded method search
            h.expectWorkspaceSymbol("foo", WorkspaceSymbolResult.of(
                    SymbolInformation.of("FooMap", SymbolKind.CLASS, "Maps", Location.of("file:///src/main/java/com/sourcegraph/simple/Maps.java", 7, 24, 7, 30)),
                    SymbolInformation.of("foo()", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 26, 13, 26, 16)),
                    SymbolInformation.of("foo(Long bar)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 38, 13, 38, 16)),
                    SymbolInformation.of("foo(T src)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 30, 14, 30, 17)),
                    SymbolInformation.of("foo(int bar)", SymbolKind.METHOD, "Overload", Location.of("file:///src/main/java/com/sourcegraph/simple/Overload.java", 34, 15, 34, 18))
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPomWithDep() {
        try (Harness h = Harness.newHarness("java-pom-with-deps")) {
            testSimpleJavaDepsCase(h);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testJavaconfigWithDep() {
        try (Harness h = Harness.newHarness("java-pom-with-deps-javaconfig")) {
            testSimpleJavaDepsCase(h);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper function to test the simple Java dependencies case. This is used to test both the "java-pom-with-deps" and
     * "java-pom-with-deps-javaconfig" repositories, which are identical, except the the former uses pom.xml and the
     * latter uses javaconfig.json.
     */
    private static void testSimpleJavaDepsCase(Harness h) throws Exception {
        h.expectHover("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 14, 17, Hover.of(
                MarkedString.of(Hover.LANGUAGE_JAVA, "public static boolean isNullOrEmpty(java.lang.String arg0)")
        ));
        h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 14, 17,
                TextDocumentXDefinitionResult.of(
                        SymbolLocationInformation.of(
                                null,
                                SymbolDescriptor.of(ElementKind.METHOD, "isNullOrEmpty", "com.google.common.base.Strings.isNullOrEmpty(String)", "Strings", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "21.0", null, null))
                        )
                )
        );
        h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 13, 19,
                TextDocumentXDefinitionResult.of(
                        SymbolLocationInformation.of(
                                Location.of("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 13, 16, 13, 27),
                                SymbolDescriptor.of(ElementKind.METHOD, "callStrings", "com.sourcegraph.depuser.GuavaUser.callStrings()", "GuavaUser", "com.sourcegraph.depuser", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:depuser", "1", null, null))
                        )
                )
        );

        // xdefinition on a symbol that is also pulled in by a dependency (Dagger). Check that we mark this as Guava and not from the Dagger artifact.
        h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 38, 37, TextDocumentXDefinitionResult.of(
                SymbolLocationInformation.of(null,
                        SymbolDescriptor.of(ElementKind.CLASS, "Maps", "com.google.common.collect.Maps", "Maps", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "21.0", null, null))
                )
        ));
        h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/JsonLibUser.java", 9, 19, TextDocumentXDefinitionResult.of(
                SymbolLocationInformation.of(null,
                        SymbolDescriptor.of(ElementKind.CLASS, "JSONObject", "net.sf.json.JSONObject", "JSONObject", "net.sf.json", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "net.sf.json-lib:json-lib", "2.4", null, null))
                )
        ));
        h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/MetricsGuiceUser.java", 9, 9, TextDocumentXDefinitionResult.of(
                SymbolLocationInformation.of(null,
                        SymbolDescriptor.of(ElementKind.CLASS, "MetricsInstrumentationModule", "com.palominolabs.metrics.guice.MetricsInstrumentationModule", "MetricsInstrumentationModule", "com.palominolabs.metrics.guice", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.palominolabs.metrics:metrics-guice", "3.2.0", null, null))
                )
        ));
        h.expectXDependencies(WorkspaceXDependenciesResult.of(
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0.1", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.dagger:dagger-compiler", "2.10-rc1", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.glassfish.jersey.ext.rx:jersey-rx-client", "2.23", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.glassfish.jersey.ext.rx:jersey-rx-client-guava", "2.23", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "net.sf.json-lib:json-lib", "2.4", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.5", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.apache.logging.log4j:log4j-core", "2.8", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "21.0", null, null), ImmutableMap.of()),
                DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.palominolabs.metrics:metrics-guice", "3.2.0", null, null), ImmutableMap.of())
        ));
        h.expectXPackages(WorkspaceXPackagesResult.of(
                PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:depuser", "1", null, null, "/"), Lists.newArrayList(
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.dagger:dagger-compiler", "2.10-rc1", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "21.0", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.palominolabs.metrics:metrics-guice", "3.2.0", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:simple", "1.0.1", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.5", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "net.sf.json-lib:json-lib", "2.4", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.apache.logging.log4j:log4j-core", "2.8", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.glassfish.jersey.ext.rx:jersey-rx-client", "2.23", null, null), ImmutableMap.of()),
                        DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.glassfish.jersey.ext.rx:jersey-rx-client-guava", "2.23", null, null), ImmutableMap.of())
                ))
        ));
        h.expectXReferences(SymbolDescriptor.of(
                ElementKind.METHOD, "isNullOrEmpty", "com.google.common.base.Strings.isNullOrEmpty(java.lang.String)", "Strings", "com.google.common.base", null
                ),
                Maps.newHashMap(),
                WorkspaceXReferencesResult.of(
                        ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 14, 16, 14, 29), SymbolDescriptor.of(ElementKind.METHOD, "isNullOrEmpty", "com.google.common.base.Strings.isNullOrEmpty(java.lang.String)", "Strings", "com.google.common.base", null))
                )
        );
        h.expectWorkspaceSymbol(
                SymbolDescriptor.of(
                        ElementKind.METHOD, "callStrings", "com.sourcegraph.depuser.GuavaUser.callStrings()", "GuavaUser", "com.sourcegraph.depuser", null
                ),
                WorkspaceSymbolResult.of(
                        SymbolInformation.of("callStrings()", SymbolKind.METHOD, "GuavaUser", Location.of("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 13, 16, 13, 27))
                )
        );
    }

    @Test
    public void testXReferencesGuavaUser() {
        try (Harness h = Harness.newHarness("java-pom-with-deps")) {
            h.expectXReferences(SymbolDescriptor.of(
                    ElementKind.CLASS, "Maps", "com.google.common.collect.Maps", "Maps", "com.google.common.collect",
                    PackageIdentifier.ofMaven("com.google.guava", "guava", null)
            ), ImmutableMap.of(), WorkspaceXReferencesResult.of(
                    ReferenceInformation.of(
                            Location.of("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 38, 36, 38, 40),
                            SymbolDescriptor.of(ElementKind.CLASS, "Maps", "com.google.common.collect.Maps", "Maps", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", null, null, null))
                    )
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPomWithParent() {
        try (Harness h = Harness.newHarness("java-parent-pom")) {
            h.expectXDefinition("file:///child/src/main/java/com/sourcegraph/Dog.java", 2, 28,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///child/src/main/java/com/sourcegraph/Animal.java", 2, 17, 2, 23),
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "Animal", "com.sourcegraph.Animal", "Animal", "com.sourcegraph", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:child", "1.0.0-SNAPSHOT", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFakeGuava() {
        try (Harness h = Harness.newHarness("java-fake-guava")) {
            h.expectXDefinition("file:///guava/src/com/google/common/collect/AbstractMultimap.java", 2, 50,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///guava/src/com/google/common/collect/Multimap.java", 2, 17, 2, 25),
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "Multimap", "com.google.common.collect.Multimap", "Multimap", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGuava() throws Exception {
        try (Harness h = Harness.newHarness("guava")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.METHOD, "transform", "com.google.common.collect.Iterables.<F,T>transform(Iterable<F>,Function<? super F,? extends T>)", "Iterables", "com.google.common.collect", null),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("<F, T>transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function)", SymbolKind.METHOD, "Iterables", Location.of("file:///guava/src/com/google/common/collect/Iterables.java", 735, 35, 735, 44))
                    )
            );
            // targeted search for a method of an inner class should fall back to an exhaustive search (and still return some results)
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.METHOD, "build", "com.google.common.collect.ImmutableMap.Builder.build()", "ImmutableMap", "com.google.common.collect", null),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("build()", SymbolKind.METHOD, "Builder", Location.of("file:///guava/src/com/google/common/collect/ImmutableMap.java", 335, 30, 335, 35))
                    )
            );
        }
    }

    @Test
    public void testJava7() {
        try (Harness h = Harness.newHarness("java-7")) {
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/Main.java", 7, 10,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/Overloaded.java", 3, 16, 3, 22),
                                    SymbolDescriptor.of(ElementKind.METHOD, "append", "com.sourcegraph.Overloaded.append(java.lang.Object)", "Overloaded", "com.sourcegraph", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:java-7-app", "1", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAnnotationArgument() throws Exception {
        try (Harness h = Harness.newHarness("java-annotation-arguments")) {
            // annotation argument: enum constant
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 8, 20, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(null,
                            SymbolDescriptor.of(ElementKind.ENUM_CONSTANT, "METHOD", "java.lang.annotation.ElementType.METHOD", "ElementType", "java.lang.annotation",
                                    OpenJDK.PACKAGE_IDENTIFIER))
            ));
            // annotation argument: enum
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 8, 10, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.ENUM, "ElementType", "java.lang.annotation.ElementType", "ElementType", "java.lang.annotation", OpenJDK.PACKAGE_IDENTIFIER)
                    )
            ));
            // using annotation: annotation name
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 8, 5, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(null,
                            SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "Target", "java.lang.annotation.Target", "Target", "java.lang.annotation",
                                    OpenJDK.PACKAGE_IDENTIFIER))
            ));
            // annotation declaration
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18, 9, 28),
                                    SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "AfterClass", "com.sourcegraph.simple.AfterClass", "AfterClass", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:annotation-arguments", "1.0", null, null))
                            )
                    )
            );
            // using annotation: annotation name
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 5,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18, 9, 28),
                                    SymbolDescriptor.of(ElementKind.ANNOTATION_TYPE, "AfterClass", "com.sourcegraph.simple.AfterClass", "AfterClass", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:annotation-arguments", "1.0", null, null))
                            )
                    )
            );
            // using annotation: annotation method
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 16,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 10, 8, 10, 12),
                                    SymbolDescriptor.of(ElementKind.METHOD, "name", "com.sourcegraph.simple.AfterClass.name()", "AfterClass", "com.sourcegraph.simple", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:annotation-arguments", "1.0", null, null))
                            )
                    )
            );
            // references to annotation
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18, false, TextDocumentReferencesResult.of(
                    Location.of("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 2, 4, 12)
            ));
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 9, 18, 9, 28),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 2, 4, 12)
                    )
            );

            // references to annotation's method
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 10, 11, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 13, 4, 17)
                    ));
            h.expectReferences("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 10, 11, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/simple/AfterClass.java", 10, 8, 10, 12),
                            Location.of("file:///src/main/java/com/sourcegraph/simple/Main.java", 4, 13, 4, 17)
                    )
            );
        }
    }

    @Test
    public void testOpenJDK8() {
        try (Harness h = Harness.newHarness("openjdk8")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(
                            ElementKind.CLASS, "System", "java.lang.System", "System", "java.lang",
                            OpenJDK.PACKAGE_IDENTIFIER
                    ),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("System", SymbolKind.CLASS, "java.lang", Location.of("file:///jdk/src/share/classes/java/lang/System.java", 58, 19, 58, 25))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(
                            ElementKind.FIELD, "out", "java.lang.System.out", "System", "java.lang",
                            OpenJDK.PACKAGE_IDENTIFIER
                    ),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("out", SymbolKind.FIELD, "System", Location.of("file:///jdk/src/share/classes/java/lang/System.java", 109, 36, 109, 39))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(
                            ElementKind.METHOD, "println", "java.io.PrintStream.println()", "PrintStream", "java.io",
                            OpenJDK.PACKAGE_IDENTIFIER
                    ),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("println()", SymbolKind.METHOD, "PrintStream", Location.of("file:///jdk/src/share/classes/java/io/PrintStream.java", 694, 16, 694, 23))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(
                            ElementKind.CLASS, "Exception", "java.lang.Exception", "Exception", "java.lang",
                            OpenJDK.PACKAGE_IDENTIFIER
                    ),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Exception", SymbolKind.CLASS, "java.lang", Location.of("file:///jdk/src/share/classes/java/lang/Exception.java", 44, 13, 44, 22))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(
                            ElementKind.CLASS, "ParserConfigurationException", "javax.xml.parsers.ParserConfigurationException", "ParserConfigurationException", "javax.xml.parsers",
                            OpenJDK.PACKAGE_IDENTIFIER
                    ),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("ParserConfigurationException", SymbolKind.CLASS, "javax.xml.parsers", Location.of("file:///jaxp/src/javax/xml/parsers/ParserConfigurationException.java", 33, 13, 33, 41))
                    )
            );
            // enum constant test
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.ENUM_CONSTANT,
                    "RUNTIME",
                    "java.lang.annotation.RetentionPolicy.RUNTIME",
                    "RetentionPolicy",
                    "java.lang.annotation",
                    OpenJDK.PACKAGE_IDENTIFIER),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("RUNTIME",
                                    null,
                                    "RetentionPolicy",
                                    Location.of("file:///jdk/src/share/classes/java/lang/annotation/RetentionPolicy.java",
                                            55, 4,
                                            55, 11))
                    ));
            // constructor symbol search test
            h.expectWorkspaceSymbol(SymbolDescriptor.of(ElementKind.CONSTRUCTOR,
                    "ArrayList",
                    "java.util.ArrayList.ArrayList()",
                    "ArrayList",
                    "java.util",
                    PackageIdentifier.of(PackageIdentifier.Type.STDLIB,
                            "jdk8",
                            null,
                            "git://github.com/jdkmirrors/openjdk8",
                            null)),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("ArrayList()", SymbolKind.CONSTRUCTOR, "ArrayList", Location.of("file:///jdk/src/share/classes/java/util/ArrayList.java", 153, 11, 153, 20))
                    )
            );

            // Proper class name span, see https://github.com/sourcegraph/java-langserver/issues/148
            h.expectXDefinition("file:///jdk/src/share/classes/java/util/Map.java", 470, 38, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            Location.of("file:///jdk/src/share/classes/java/util/Map.java", 374, 14, 374, 19),
                            SymbolDescriptor.of(ElementKind.INTERFACE, "Entry", "java.util.Map.Entry", "Map", "java.util", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null))
                    )
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAndroid25() throws Exception {
        try (Harness h = Harness.newHarness("android-sdk")) {
            h.expectXDefinition("file:///sdk/android/app/IntentService.java", 54, 46, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "Service", "android.app.Service", "Service", "android.app", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null))
                    )
            ));
        }
    }

    @Test
    public void testLombok() {
        try (Harness h = Harness.newHarness("lombok")) {
            h.expectHover("file:///src/core/lombok/Lombok.java", 26, 18, Hover.of(
                    MarkedString.of(Hover.LANGUAGE_JAVA, "public class lombok.Lombok"),
                    MarkedString.of(Hover.LANGUAGE_MARKDOWN, " Useful utility methods to manipulate lombok-generated code.\n")
            ));
            h.expectDefinition("file:///src/core/lombok/Lombok.java", 26, 18,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/core/lombok/Lombok.java", 26, 13, 26, 19)
                    )
            );
            h.expectReferences("file:///src/core/lombok/Builder.java", 108, 20, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/core/lombok/eclipse/handlers/HandleBuilder.java", 89, 60, 89, 67),
                            Location.of("file:///src/core/lombok/eclipse/handlers/HandleBuilder.java", 129, 47, 129, 54),
                            Location.of("file:///src/core/lombok/eclipse/handlers/HandleBuilder.java", 132, 2, 132, 9),
                            Location.of("file:///src/core/lombok/eclipse/handlers/HandleConstructor.java", 237, 45, 237, 52),
                            Location.of("file:///src/core/lombok/javac/handlers/HandleBuilder.java", 76, 58, 76, 65),
                            Location.of("file:///src/core/lombok/javac/handlers/HandleBuilder.java", 94, 47, 94, 54),
                            Location.of("file:///src/core/lombok/javac/handlers/HandleBuilder.java", 95, 2, 95, 9),
                            Location.of("file:///src/core/lombok/javac/handlers/HandleBuilder.java", 120, 47, 120, 54),
                            Location.of("file:///src/core/lombok/javac/handlers/HandleConstructor.java", 213, 45, 213, 52)
                    )
            );
            h.expectXDefinition("file:///src/core/lombok/eclipse/EclipseAnnotationHandler.java", 60, 100, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS,
                                    "Annotation",
                                    "org.eclipse.jdt.internal.compiler.ast.Annotation",
                                    "Annotation",
                                    "org.eclipse.jdt.internal.compiler.ast",
                                    PackageIdentifier.of(PackageIdentifier.Type.MAVEN,
                                            "org.eclipse.jdt:org.eclipse.jdt.core",
                                            "3.10.0.v20140604-1726",
                                            null,
                                            null))
                    )
            ));
            h.expectXPackages(
                    WorkspaceXPackagesResult.of(
                            PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "org.projectlombok:eclipse-compiler-test", "1.0-SNAPSHOT", null, null, "/website/setup"), Lists.newArrayList(
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.projectlombok:lombok", "1.16.8", null, null), ImmutableMap.of())
                            )),
                            PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "org.projectlombok:lombok", "1.16.16", null, null, "/"), Lists.newArrayList(
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.eclipse.core:jobs", "3.5.300-v20130429-1813", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.eclipse.core:org.eclipse.core.resources", "3.8.101.v20130717-0806", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.eclipse.core:org.eclipse.core.runtime", "3.9.0.v20130326-1255", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.eclipse.equinox:org.eclipse.equinox.common", "3.6.200.v20130402-1505", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.eclipse.jdt:org.eclipse.jdt.core", "3.10.0.v20140604-1726", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.mangosdk.spi:spi", "0.2.4", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.osgi:org.osgi.core", "4.0.0", null, null), ImmutableMap.of()),
                                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.projectlombok:lombok-patcher", "0.22", null, null), ImmutableMap.of())
                            ))
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Priam is a Gradle-based repository with Nebula-based conventions
     */
    @Test
    public void testPriam() {
        try (Harness h = Harness.newHarness("priam")) {
            // Normal jump-to-def
            h.expectDefinition("file:///priam/src/main/java/com/netflix/priam/AbstractConfigSource.java", 13, 55, TextDocumentDefinitionResult.of(
                    Location.of("file:///priam/src/main/java/com/netflix/priam/IConfigSource.java", 11, 17, 11, 30)
            ));

            // Jump from subproject to root project
            h.expectDefinition("file:///priam-dse-extensions/src/main/java/com/netflix/priam/dse/DseProcessManager.java", 6, 37, TextDocumentDefinitionResult.of(
                    Location.of("file:///priam/src/main/java/com/netflix/priam/defaultimpl/CassandraProcessManager.java", 22, 13, 22, 36)
                    )
            );

            h.expectXDefinition("file:///priam-dse-extensions/src/main/java/com/netflix/priam/dse/DseProcessManager.java", 6, 37,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///priam/src/main/java/com/netflix/priam/defaultimpl/CassandraProcessManager.java", 22, 13, 22, 36),
                                    SymbolDescriptor.of(ElementKind.CLASS, "CassandraProcessManager", "com.netflix.priam.defaultimpl.CassandraProcessManager", "CassandraProcessManager", "com.netflix.priam.defaultimpl", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.netflix.priam:priam", "2.0.1", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mockito is a Gradle-based repository with peculiar patterns
     */
    @Test
    public void testMockito() {
        try (Harness h = Harness.newHarness("mockito")) {
            h.expectWorkspaceSymbol("DefaultMockingDetails", WorkspaceSymbolResult.of(
                    SymbolInformation.of("DefaultMockingDetails", SymbolKind.CLASS, "org.mockito.internal.util", Location.of("file:///src/main/java/org/mockito/internal/util/DefaultMockingDetails.java", 25, 13, 25, 34)),
                    SymbolInformation.of("DefaultMockingDetails(Object toInspect)", SymbolKind.METHOD, "DefaultMockingDetails", Location.of("file:///src/main/java/org/mockito/internal/util/DefaultMockingDetails.java", 29, 11, 29, 32)),
                    SymbolInformation.of("DefaultMockingDetailsTest", SymbolKind.CLASS, "org.mockito.internal.util", Location.of("file:///src/test/java/org/mockito/internal/util/DefaultMockingDetailsTest.java", 26, 13, 26, 38))
            ));
            // Jump from subproject to root project
            h.expectDefinition("file:///subprojects/testng/src/main/java/org/mockito/testng/MockitoBeforeTestNGMethod.java", 48, 102, TextDocumentDefinitionResult.of(
                    Location.of("file:///src/main/java/org/mockito/internal/configuration/CaptorAnnotationProcessor.java", 16, 13, 16, 38)
            ));
            h.expectXDefinition("file:///subprojects/testng/src/main/java/org/mockito/testng/MockitoBeforeTestNGMethod.java", 48, 102,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/org/mockito/internal/configuration/CaptorAnnotationProcessor.java", 16, 13, 16, 38),
                                    SymbolDescriptor.of(ElementKind.CONSTRUCTOR, "CaptorAnnotationProcessor", "org.mockito.internal.configuration.CaptorAnnotationProcessor.CaptorAnnotationProcessor()", "CaptorAnnotationProcessor", "org.mockito.internal.configuration", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.mockito:mockito-core", "2.7.16", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test Mockito, but with a javaconfig.json generated from the Javaconfig Gradle plugin.
     */
    @Test
    public void testMockitoJavaconfig() {
        try (Harness h = Harness.newHarness("mockito-javaconfig")) {
            h.expectWorkspaceSymbol("DefaultMockingDetails", WorkspaceSymbolResult.of(
                    SymbolInformation.of("DefaultMockingDetails", SymbolKind.CLASS, "org.mockito.internal.util", Location.of("file:///src/main/java/org/mockito/internal/util/DefaultMockingDetails.java", 24, 13, 24, 34)),
                    SymbolInformation.of("DefaultMockingDetails(Object toInspect)", SymbolKind.METHOD, "DefaultMockingDetails", Location.of("file:///src/main/java/org/mockito/internal/util/DefaultMockingDetails.java", 28, 11, 28, 32)),
                    SymbolInformation.of("DefaultMockingDetailsTest", SymbolKind.CLASS, "org.mockito.internal.util", Location.of("file:///src/test/java/org/mockito/internal/util/DefaultMockingDetailsTest.java", 31, 13, 31, 38))
            ));
            // Jump from subproject to root project
            h.expectDefinition("file:///subprojects/testng/src/main/java/org/mockito/testng/MockitoBeforeTestNGMethod.java", 8, 42, TextDocumentDefinitionResult.of(
                    Location.of("file:///src/main/java/org/mockito/internal/configuration/CaptorAnnotationProcessor.java", 16, 13, 16, 38)
            ));
            h.expectXDefinition("file:///subprojects/testng/src/main/java/org/mockito/testng/MockitoBeforeTestNGMethod.java", 8, 42,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/org/mockito/internal/configuration/CaptorAnnotationProcessor.java", 16, 13, 16, 38),
                                    SymbolDescriptor.of(ElementKind.CLASS, "CaptorAnnotationProcessor", "org.mockito.internal.configuration.CaptorAnnotationProcessor", "CaptorAnnotationProcessor", "org.mockito.internal.configuration", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.mockito:mockito", "2.18.6", null, null))
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * LeakCanary has an Android dependency
     */
    @Test
    public void testLeakCanary() throws Exception {
        try (Harness h = Harness.newHarness("leakcanary")) {
            h.expectXDefinition("file:///leakcanary-android/src/main/java/com/squareup/leakcanary/AbstractAnalysisResultService.java", 17, 20, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "IntentService", "android.app.IntentService", "IntentService", "android.app", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null))
                    )
            ));
            h.expectXDefinition("file:///leakcanary-sample/src/main/java/com/example/leakcanary/ExampleApplication.java", 17, 20, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "Application", "android.app.Application", "Application", "android.app", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null))
                    )
            ));
            h.expectXPackages(WorkspaceXPackagesResult.of(
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-analyzer", "1.6-SNAPSHOT", null, null, "/leakcanary-analyzer"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.haha:haha", "2.0.3", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-watcher", "1.6-SNAPSHOT", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.assertj:assertj-core", "1.7.0", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-android", "1.6-SNAPSHOT", null, null, "/leakcanary-android"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-analyzer", "1.6-SNAPSHOT", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-android-no-op", "1.6-SNAPSHOT", null, null, "/leakcanary-android-no-op"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-watcher", "1.6-SNAPSHOT", null, null, "/leakcanary-watcher"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:UNKNOWN_ARTIFACT_2", "UNKNOWN_VERSION", null, null, "/"), Lists.newArrayList(

                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:leakcanary-sample", "UNKNOWN_VERSION", null, null, "/leakcanary-sample"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of())
                    ))
            ));
            h.expectXDependencies(WorkspaceXDependenciesResult.of(
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.haha:haha", "2.0.3", null, null), ImmutableMap.of()),
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-analyzer", "1.6-SNAPSHOT", null, null), ImmutableMap.of()),
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.squareup.leakcanary:leakcanary-watcher", "1.6-SNAPSHOT", null, null), ImmutableMap.of()),
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of()),
                    DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.assertj:assertj-core", "1.7.0", null, null), ImmutableMap.of())
            ));
        }
    }

    /**
     * Test that we're tolerant to spaces in file paths. (apache/log4j has a file path
     * "file:///contribs/Jamie Tsao/JMSQueueAppender.java" that contains a space.
     */
    @Test
    public void testSpaceInFilename() throws Exception {
        try (Harness h = Harness.newHarness("log4j")) {
            h.expectDefinition("file:///src/main/java/org/apache/log4j/Logger.java", 29, 14,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/org/apache/log4j/Logger.java", 29, 13, 29, 19)
                    )
            );
        }
    }

    @Test
    public void testDropwizardSnippets() throws Exception {
        try (Harness h = Harness.newHarness("dropwizard-snippets")) {
            // generic interface type bounds
            h.expectXDefinition("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    6, 55,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.INTERFACE,
                                            "DeferredProcessingAware",
                                            "ch.qos.logback.core.spi.DeferredProcessingAware",
                                            "DeferredProcessingAware",
                                            "ch.qos.logback.core.spi",
                                            PackageIdentifier.of(PackageIdentifier.Type.MAVEN,
                                                    "ch.qos.logback:logback-core",
                                                    "1.1.7",
                                                    null,
                                                    null))
                            )
                    ));
            // generic interface type parameter
            h.expectXDefinition("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    6, 33,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java", 6, 33, 6, 66),
                                    SymbolDescriptor.of(ElementKind.TYPE_PARAMETER, "E", "io.dropwizard.logging.AppenderFactory.E", "AppenderFactory", "io.dropwizard.logging", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "io.dropwizard:dropwizard-logging", "1.2.0-SNAPSHOT", null, null))
                            )
                    ));
            // annotation fields and methods
            h.expectXDefinition("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    5, 15,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.METHOD, "use", "com.fasterxml.jackson.annotation.JsonTypeInfo.use()", "JsonTypeInfo", "com.fasterxml.jackson.annotation", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.fasterxml.jackson.core:jackson-annotations", "2.8.6", null, null))
                            )
                    ));
            h.expectHover("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    5, 15, Hover.of(
                            MarkedString.of("java", "public abstract com.fasterxml.jackson.annotation.JsonTypeInfo.Id use()")
                    ));

            // annotation fields and methods
            h.expectXDefinition("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    5, 46,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.METHOD, "property", "com.fasterxml.jackson.annotation.JsonTypeInfo.property()", "JsonTypeInfo", "com.fasterxml.jackson.annotation", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.fasterxml.jackson.core:jackson-annotations", "2.8.6", null, null))
                            )
                    ));
            h.expectHover("file:///dropwizard-logging/src/main/java/io/dropwizard/logging/AppenderFactory.java",
                    5, 46, Hover.of(
                            MarkedString.of("java", "public abstract java.lang.String property()")
                    ));
        }
    }


    /**
     * @link https://github.com/sourcegraph/sourcegraph/issues/4447
     */
    @Ignore // TODO: ad-hoc build structure (no root config; mix of Maven and Eclipse or something?)
    @Test
    public void testXrefsNullPointerException() throws Exception {
        try (Harness h = Harness.newHarness("hackthon")) {
            h.expectXReferences(
                    SymbolDescriptor.of(
                            ElementKind.METHOD,
                            "checkNotNull",
                            "com.google.common.base.Preconditions.<T>checkNotNull(T,java.lang.Object)",
                            "Preconditions",
                            "com.google.common.base",
                            null
                    ),
                    Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///FeatureExtractor/src/io/derek/hackathon/feature/WordVectorizer.java", 48, 16, 48, 28), SymbolDescriptor.of(ElementKind.METHOD, "checkNotNull", "com.google.common.base.Preconditions.<T>checkNotNull(T,java.lang.Object)", "Preconditions", "com.google.common.base", null)),
                            ReferenceInformation.of(Location.of("file:///analyzer/src/main/java/io/derek/hackathon/feature/WordVectorizer.java", 48, 16, 48, 28), SymbolDescriptor.of(ElementKind.METHOD, "checkNotNull", "com.google.common.base.Preconditions.<T>checkNotNull(T,java.lang.Object)", "Preconditions", "com.google.common.base", null))
                    )
            );
        }
    }

    @Test
    public void textXReferencesWithPartialPackageInfo() throws Exception {
        try (Harness h = Harness.newHarness("guava")) {
            h.expectXReferences(
                    SymbolDescriptor.of(
                            ElementKind.METHOD,
                            "compose",
                            "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)",
                            "Predicates",
                            "com.google.common.base",
                            PackageIdentifier.of(
                                    PackageIdentifier.Type.MAVEN,
                                    "com.google.guava:guava",
                                    "22.0-SNAPSHOT",
                                    null,
                                    null
                            )
                    ),
                    Maps.newHashMap(),
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/base/Predicates.java", 245, 36, 245, 43), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/collect/Maps.java", 2342, 11, 2342, 18), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/collect/Maps.java", 2346, 11, 2346, 18), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/collect/TreeRangeMap.java", 616, 33, 616, 40), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/collect/TreeRangeMap.java", 683, 33, 683, 40), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///guava/src/com/google/common/collect/TreeRangeMap.java", 688, 33, 688, 40), SymbolDescriptor.of(ElementKind.METHOD, "compose", "com.google.common.base.Predicates.<A,B>compose(com.google.common.base.Predicate<B>,com.google.common.base.Function<A,? extends B>)", "Predicates", "com.google.common.base", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "22.0-SNAPSHOT", null, null)))
                    )
            );
        }
    }

    @Test
    public void testStaticImports() throws Exception {
        try (Harness h = Harness.newHarness("commons-io")) {
            h.expectHover("file:///src/main/java/org/apache/commons/io/EndianUtils.java", 18, 46,
                    Hover.of(
                            MarkedString.of(Hover.LANGUAGE_JAVA, "public static final int EOF"),
                            MarkedString.of(Hover.LANGUAGE_MARKDOWN, " Represents the end-of-file (or stream).\n @since 2.5 (made public)\n")
                    )
            );
            h.expectDefinition("file:///src/main/java/org/apache/commons/io/EndianUtils.java", 18, 46,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/main/java/org/apache/commons/io/IOUtils.java", 107, 28, 107, 31)
                    )
            );
        }
    }

    /**
     * Testing refs limits
     */
    @Test
    public void testRefLimits() throws Exception {
        try (Harness h = Harness.newHarness("cup-of-joe")) {
            h.expectReferences("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 15, false,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 8, 17, 11),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 38, 17, 41),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 45, 12, 45, 15),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 45, 12, 45, 15),
                            Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 14, 62, 14, 65)
                    )
            );
            h.expectReferences("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 15, false, 2,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 8, 17, 11),
                            Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 38, 17, 41)
                    )
            );
            h.expectXReferences(
                    SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null)),
                    null,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 2, 27, 2, 30), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 8, 17, 11), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 38, 17, 41), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 45, 12, 45, 15), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 45, 12, 45, 15), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 12, 13, 12, 16), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/cup/Cup.java", 14, 62, 14, 65), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null)))
                    )
            );
            h.expectXReferences(
                    SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null)),
                    null,
                    2,
                    WorkspaceXReferencesResult.of(
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 2, 27, 2, 30), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null))),
                            ReferenceInformation.of(Location.of("file:///src/main/java/com/sourcegraph/App.java", 17, 8, 17, 11), SymbolDescriptor.of(ElementKind.CLASS, "Cup", "com.sourcegraph.cup.Cup", "Cup", "com.sourcegraph.cup", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.sourcegraph:cup-of-joe", "1.0-SNAPSHOT", null, null)))
                    )
            );
        }
    }

    /**
     * Make sure we can resolve xdefinitions to openjdk8-langtools
     *
     * @throws Exception
     */
    @Test
    public void testUsingOpenJDK8LangTools() throws Exception {
        try (Harness h = Harness.newHarness("java-with-langtools")) {
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/langtools/Main.java",
                    7, 38,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "TreeVisitor", "com.sun.source.tree.TreeVisitor", "TreeVisitor", "com.sun.source.tree", OpenJDKLangTools.PACKAGE_IDENTIFIER)
                            )
                    )
            );
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/langtools/Main.java",
                    11, 11,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.CLASS, "Trees", "com.sun.source.util.Trees", "Trees", "com.sun.source.util", OpenJDKLangTools.PACKAGE_IDENTIFIER)
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/langtools/Main.java",
                    12, 14,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "Element", "javax.lang.model.element.Element", "Element", "javax.lang.model.element", OpenJDKLangTools.PACKAGE_IDENTIFIER)
                            )
                    ));
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/langtools/Main.java",
                    16, 43,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "AnnotationTree", "com.sun.source.tree.AnnotationTree", "AnnotationTree", "com.sun.source.tree", OpenJDKLangTools.PACKAGE_IDENTIFIER)
                            )
                    ));
        }
    }

    /**
     * Make sure we can index openjdk8-langtools repo
     *
     * @throws Exception
     */
    @Test
    public void testOpenJDK8LangTools() throws Exception {
        try (Harness h = Harness.newHarness("openjdk8-langtools")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.INTERFACE, "TreeVisitor", "com.sun.source.tree.TreeVisitor", "TreeVisitor", "com.sun.source.tree", OpenJDKLangTools.PACKAGE_IDENTIFIER),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("TreeVisitor", SymbolKind.INTERFACE, "com.sun.source.tree", Location.of("file:///src/share/classes/com/sun/source/tree/TreeVisitor.java", 59, 17, 59, 28))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.CLASS, "Trees", "com.sun.source.util.Trees", "Trees", "com.sun.source.util", OpenJDKLangTools.PACKAGE_IDENTIFIER),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Trees", SymbolKind.CLASS, "com.sun.source.util", Location.of("file:///src/share/classes/com/sun/source/util/Trees.java", 54, 22, 54, 27))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.INTERFACE, "Element", "javax.lang.model.element.Element", "Element", "javax.lang.model.element", OpenJDKLangTools.PACKAGE_IDENTIFIER),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("Element", SymbolKind.INTERFACE, "javax.lang.model.element", Location.of("file:///src/share/classes/javax/lang/model/element/Element.java", 62, 17, 62, 24))
                    )
            );
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.INTERFACE, "AnnotationTree", "com.sun.source.tree.AnnotationTree", "AnnotationTree", "com.sun.source.tree", OpenJDKLangTools.PACKAGE_IDENTIFIER),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AnnotationTree", SymbolKind.INTERFACE, "com.sun.source.tree", Location.of("file:///src/share/classes/com/sun/source/tree/AnnotationTree.java", 45, 17, 45, 31))
                    )
            );

        }
    }

    /**
     * Analyzing a file in one module, then fetching a file in a different module (but still in the same package)
     * should correctly resolve the second module's dependencies.
     *
     * @throws Exception
     */
    @Test
    public void testSiblingDependencies() throws Exception {
        try (Harness h = Harness.newHarness("multi-module")) {
            h.expectHover("file:///a/src/main/java/com/sourcegraph/A.java", 3, 16,
                    Hover.of(
                            MarkedString.of("java", "public void foo()")
                    )
            );
            h.expectHover("file:///b/src/main/java/com/sourcegraph/B.java", 6, 42,
                    Hover.of(
                            MarkedString.of("java", "public java.lang.String toString()")
                    )
            );
        }
    }

    /**
     * Ensures we properly calculating span for variables and enum constants
     *
     * @link https://github.com/sourcegraph/java-langserver/issues/163
     */
    @Test
    public void testCommonsLangEnumConstantSpan() throws Exception {
        try (Harness h = Harness.newHarness("commons-lang")) {
            h.expectDefinition("file:///src/test/java/org/apache/commons/lang3/EnumUtilsTest.java", 50, 31,
                    TextDocumentDefinitionResult.of(
                            Location.of("file:///src/test/java/org/apache/commons/lang3/EnumUtilsTest.java", 413, 4, 413, 7)
                    )
            );
        }
    }

    /**
     * Ensures that if model contains project-based system dependencies which we do not support for now
     * we still can build an effective model and extract as much data as possible
     */
    @Test
    public void testPomWithSystemDependencies() throws Exception {
        try (Harness h = Harness.newHarness("java-pom-with-system-dependencies")) {
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 6, 28,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.FIELD, "DIR_SEPARATOR", "org.apache.commons.io.IOUtils.DIR_SEPARATOR", "IOUtils", "org.apache.commons.io", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.5", null, null))
                            )
                    )
            );
        }
    }

    /**
     * Ensures that even if model contains some errors
     * we still can build a partial effective model and extract as much data as possible
     */
    @Test
    public void testPomWithBrokenDependencies() throws Exception {
        try (Harness h = Harness.newHarness("java-pom-with-broken-dependencies")) {
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/simple/Main.java", 6, 28,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.FIELD, "DIR_SEPARATOR", "org.apache.commons.io.IOUtils.DIR_SEPARATOR", "IOUtils", "org.apache.commons.io", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "commons-io:commons-io", "2.5", null, null))
                            )
                    )
            );
        }
    }

    /**
     * Ensures we can extract dependencies in "label: value, ..." form
     * i.e. provided(group: "com.google.code.findbugs", name: "jsr305", version: "2.0.1")
     */
    @Test
    public void testGradleOnJsonPatch() throws Exception {
        try (Harness h = Harness.newHarness("json-patch")) {
            h.expectXDefinition("file:///src/main/java/com/github/fge/jsonpatch/ReplaceOperation.java", 42, 63, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "JsonPointer", "com.github.fge.jackson.jsonpointer.JsonPointer", "JsonPointer", "com.github.fge.jackson.jsonpointer", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.github.fge:jackson-coreutils", "1.6", null, null))
                    )
            ));
        }
    }

    /**
     * Make sure that sub-projects with colons in their names work. Also make sure that we don't evaluate certain gradle
     * blocks such as `afterEvaluate` and `uploadArchives` because they might change the names that we're trying to
     * infer for our pom files.
     *
     * @throws Exception
     */
    @Test
    public void testGradleWithColonsInNamesAndInconvenientBlocks() throws Exception {
        try (Harness h = Harness.newHarness("glide")) {
            h.expectXDefinition("file:///library/src/main/java/com/bumptech/glide/manager/Lifecycle.java", 9, 19,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///library/src/main/java/com/bumptech/glide/manager/LifecycleListener.java", 6, 17, 6, 34),
                                    SymbolDescriptor.of(ElementKind.INTERFACE, "LifecycleListener", "com.bumptech.glide.manager.LifecycleListener", "LifecycleListener", "com.bumptech.glide.manager", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.github.bumptech.glide:glide", "4.0.0-SNAPSHOT", null, null))
                            )
                    )
            );
            h.expectReferences("file:///library/src/main/java/com/bumptech/glide/manager/LifecycleListener.java", 6, 17, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///library/src/main/java/com/bumptech/glide/RequestManager.java", 46, 39, 46, 56),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 12, 20, 12, 37),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 13, 48, 13, 65),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 29, 26, 29, 43),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 42, 29, 42, 46),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 48, 9, 48, 26),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 55, 9, 55, 26),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ActivityFragmentLifecycle.java", 62, 9, 62, 26),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ApplicationLifecycle.java", 11, 26, 11, 43),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ApplicationLifecycle.java", 16, 29, 16, 46),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/ConnectivityMonitor.java", 5, 45, 5, 62),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/Lifecycle.java", 9, 19, 9, 36),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/Lifecycle.java", 18, 22, 18, 39),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/LifecycleListener.java", 6, 17, 6, 34),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/manager/TargetTracker.java", 14, 44, 14, 61),
                            Location.of("file:///library/src/main/java/com/bumptech/glide/request/target/Target.java", 22, 35, 22, 52),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 19, 10, 19, 27),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 24, 20, 24, 37),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 109, 9, 109, 26),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 111, 25, 111, 42),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 113, 9, 113, 26),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/LifecycleTest.java", 118, 9, 118, 26),
                            Location.of("file:///library/src/test/java/com/bumptech/glide/manager/RequestManagerFragmentTest.java", 63, 71, 63, 88)
                    )
            );
        }
    }

    /**
     * Make sure we honor the `srcDirs` variables when they're set by Gradle scripts.
     *
     * @throws Exception
     */
    @Test
    public void testGradleWithSrcDirs() throws Exception {
        try (Harness h = Harness.newHarness("EventBus")) {
            h.expectXDefinition("file:///EventBusTest/src/org/greenrobot/eventbus/AbstractEventBusTest.java", 23, 31,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 40, 13, 40, 21),
                                    SymbolDescriptor.of(ElementKind.CLASS, "EventBus", "org.greenrobot.eventbus.EventBus", "EventBus", "org.greenrobot.eventbus", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.greenrobot:eventbus", "3.0.0", null, null))
                            )
                    )
            );
            h.expectReferences("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 40, 13, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/AsyncPoster.java", 26, 18, 26, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/AsyncPoster.java", 28, 16, 28, 24),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/BackgroundPoster.java", 27, 18, 27, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/BackgroundPoster.java", 31, 21, 31, 29),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 40, 13, 40, 21),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 45, 20, 45, 28),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 77, 18, 77, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 79, 26, 79, 34),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBus.java", 81, 42, 81, 50),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 145, 11, 145, 19),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 146, 22, 146, 30),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 147, 16, 147, 24),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 151, 12, 151, 20),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 152, 19, 152, 27),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 157, 11, 157, 19),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/EventBusBuilder.java", 158, 19, 158, 27),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/HandlerPoster.java", 26, 18, 26, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/HandlerPoster.java", 29, 18, 29, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/NoSubscriberEvent.java", 24, 17, 24, 25),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/NoSubscriberEvent.java", 29, 29, 29, 37),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/SubscriberExceptionEvent.java", 24, 17, 24, 25),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/SubscriberExceptionEvent.java", 35, 36, 35, 44),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 37, 16, 37, 24),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 52, 32, 52, 40),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 67, 27, 67, 35),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 94, 18, 94, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 97, 47, 97, 55),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/AsyncExecutor.java", 121, 30, 121, 38),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogConfig.java", 29, 4, 29, 12),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogConfig.java", 52, 18, 52, 26),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogConfig.java", 73, 28, 73, 36),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogConfig.java", 78, 4, 78, 12),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogConfig.java", 79, 42, 79, 50),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogFragments.java", 57, 12, 57, 20),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogManager.java", 50, 16, 50, 24),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogManager.java", 121, 16, 121, 24),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ErrorDialogManager.java", 244, 22, 244, 30),
                            Location.of("file:///EventBus/src/org/greenrobot/eventbus/util/ExceptionToResourceMapping.java", 54, 26, 54, 34),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/TestRunner.java", 32, 18, 32, 26),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/TestRunner.java", 34, 62, 34, 70),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/TestRunnerActivity.java", 37, 12, 37, 20),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/TestRunnerActivity.java", 45, 25, 45, 33),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/testsubject/PerfTestEventBus.java", 35, 18, 35, 26),
                            Location.of("file:///EventBusPerformance/src/org/greenrobot/eventbusperf/testsubject/PerfTestEventBus.java", 43, 19, 43, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/AbstractEventBusTest.java", 43, 14, 43, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/AbstractEventBusTest.java", 67, 8, 67, 16),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/AbstractEventBusTest.java", 68, 23, 68, 31),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBasicTest.java", 45, 14, 45, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBasicTest.java", 56, 23, 56, 31),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBasicTest.java", 69, 14, 69, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBasicTest.java", 144, 14, 144, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBasicTest.java", 190, 12, 190, 20),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 29, 19, 29, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 42, 19, 42, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 51, 19, 51, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 59, 34, 59, 42),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 63, 35, 63, 43),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusBuilderTest.java", 75, 19, 75, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusIndexTest.java", 43, 8, 43, 16),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusIndexTest.java", 43, 28, 43, 36),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusInheritanceDisabledTest.java", 31, 14, 31, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusInheritanceDisabledTest.java", 41, 19, 41, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusInheritanceTest.java", 26, 14, 26, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusInheritanceTest.java", 36, 23, 36, 31),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusMultithreadedTest.java", 105, 14, 105, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusMultithreadedTest.java", 135, 14, 135, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusMultithreadedTest.java", 221, 22, 221, 30),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusNoSubscriberEventTest.java", 48, 19, 48, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusOrderedSubscriptionsTest.java", 139, 18, 139, 26),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusOrderedSubscriptionsTest.java", 208, 18, 208, 26),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusSubscriberExceptionTest.java", 29, 19, 29, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusSubscriberExceptionTest.java", 42, 19, 42, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusSubscriberInJarTest.java", 27, 14, 27, 22),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/EventBusSubscriberInJarTest.java", 27, 34, 27, 42),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/indexed/EventBusInheritanceDisabledTestWithIndex.java", 27, 19, 27, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/indexed/EventBusSubscriberInJarTestWithIndex.java", 26, 19, 26, 27),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/indexed/Indexed.java", 22, 11, 22, 19),
                            Location.of("file:///EventBusTest/src/org/greenrobot/eventbus/indexed/Indexed.java", 23, 15, 23, 23)
                    )
            );
        }
    }

    /**
     * Make sure we honor `srcDirs` only when they're inside a `java` block, otherwise we might set them to irrelevant
     * paths like resources and such.
     *
     * @throws Exception
     */
    @Test
    public void testGradleWithScopedSrcDirs() throws Exception {
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectXDefinition("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/decoder/ApkDecoder.java", 62, 25,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/util/TypedValue.java", 21, 13, 21, 23),
                                    SymbolDescriptor.of(ElementKind.CLASS, "TypedValue", "com.tencent.tinker.build.util.TypedValue", "TypedValue", "com.tencent.tinker.build.util", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-patch-lib", "1.7.7", null, null))
                            )
                    )
            );
            h.expectReferences("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/util/TypedValue.java", 28, 31, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/decoder/ApkDecoder.java", 71, 41, 71, 49),
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/patch/Configuration.java", 285, 44, 285, 52),
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/patch/Configuration.java", 292, 44, 292, 52),
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/patch/Configuration.java", 298, 83, 298, 91),
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/patch/Configuration.java", 301, 83, 301, 91),
                            Location.of("file:///tinker-build/tinker-patch-lib/src/main/java/com/tencent/tinker/build/util/TypedValue.java", 28, 31, 28, 39)
                    )
            );
        }
    }

    /**
     * Make sure we initialize Gradle projects properly even when the settings.gradle file isn't in the root folder.
     *
     * @throws Exception
     */
    @Ignore // TODO: this seems to be primarily an Ant project, actually
    @Test
    public void testGradleWithNonRootSettings() throws Exception {
        try (Harness h = Harness.newHarness("ion")) {
            h.expectXDefinition("file:///ion-sample/src/com/koushikdutta/ion/sample/KenBurnsSample.java", 31, 8,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///ion/src/com/koushikdutta/ion/Ion.java", 58, 13, 58, 16),
                                    SymbolDescriptor.of(ElementKind.CLASS, "Ion", "com.koushikdutta.ion.Ion", "Ion", "com.koushikdutta.ion", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:UNKNOWN_ARTIFACT_0", "UNKNOWN_VERSION", null, null))
                            )
                    )
            );
        }
    }

    @Test
    public void testAndroidSDKJavaxReference() throws Exception {
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectXPackages(WorkspaceXPackagesResult.of(
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:aosp-dexutils", "1.7.7", null, null, "/third-party/aosp-dexutils"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:bsdiff-util", "1.7.7", null, null, "/third-party/bsdiff-util"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-android-anno", "1.7.7", null, null, "/tinker-android/tinker-android-anno"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-android-lib", "1.7.7", null, null, "/tinker-android/tinker-android-lib"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-commons", "1.7.7", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-android-loader", "1.7.7", null, null, "/tinker-android/tinker-android-loader"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-commons", "1.7.7", null, null, "/tinker-commons"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-patch-cli", "1.7.7", null, null, "/tinker-build/tinker-patch-cli"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-patch-gradle-plugin", "1.7.7", null, null, "/tinker-build/tinker-patch-gradle-plugin"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.tools.build:gradle", "2.1.0", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.gradle:osdetector-gradle-plugin", "1.2.1", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-patch-lib", "1.7.7", null, null, "/tinker-build/tinker-patch-lib"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "11.0.2", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.mm:apk-parser-lib", "1.0.0", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-commons", "1.7.7", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.ow2.asm:asm", "5.0.3", null, null), ImmutableMap.of())
                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:UNKNOWN_ARTIFACT_1", "UNKNOWN_VERSION", null, null, "/tinker-sample-android"), Lists.newArrayList(

                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:UNKNOWN_ARTIFACT_10", "UNKNOWN_VERSION", null, null, "/"), Lists.newArrayList(

                    )),
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "UNKNOWN_GROUP:app", "UNKNOWN_VERSION", null, null, "/tinker-sample-android/app"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "23.1.1", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:multidex", "1.0.1", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-android-anno", "1.7.7", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.tencent.tinker:tinker-android-lib", "1.7.7", null, null), ImmutableMap.of()),
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "junit:junit", "4.12", null, null), ImmutableMap.of())
                    ))
            ));
        }
    }

    /**
     * Make sure we initialize Gradle projects properly even when they have multiple settings.gradle
     *
     * @throws Exception
     */
    @Test
    public void testGradleWithNestedSettings() throws Exception {
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectReferences("file:///tinker-android/tinker-android-lib/src/main/java/com/tencent/tinker/lib/tinker/TinkerInstaller.java", 31, 13, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///tinker-android/tinker-android-lib/src/main/java/com/tencent/tinker/lib/tinker/TinkerInstaller.java", 31, 13, 31, 28),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/app/MainActivity.java", 58, 16, 58, 31),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/app/SampleApplicationLike.java", 91, 8, 91, 23),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/reporter/SampleLoadReporter.java", 110, 24, 110, 39),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/util/TinkerManager.java", 71, 8, 71, 23),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/util/TinkerManager.java", 96, 8, 96, 23),
                            Location.of("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/util/UpgradePatchRetry.java", 107, 8, 107, 23)
                    )
            );
        }
    }

    @Test
    public void testJumpToAndroidSDK() throws Exception {
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectXDefinition("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/app/MainActivity.java", 22, 18,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.CLASS, "Environment", "android.os.Environment", "Environment", "android.os", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null))
                            )
                    )
            );
        }
    }

    @Test
    public void testTinker() throws Exception {
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectXDefinition("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/app/MainActivity.java", 23, 31, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "23.1.1", null, null))
                    )
            ));
        }
        try (Harness h = Harness.newHarness("platform_frameworks_support")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "23.1.1", null, null)),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AppCompatActivity", SymbolKind.CLASS, "android.support.v7.app", Location.of("file:///v7/appcompat/src/android/support/v7/app/AppCompatActivity.java", 60, 13, 60, 30))
                    )
            );
        }
    }

    @Test
    public void testJumpToAndroidSupportLibrary() throws Exception {
        // Cross-repo J2D instance 1
        try (Harness h = Harness.newHarness("tinker")) {
            h.expectXDefinition("file:///tinker-sample-android/app/src/main/java/tinker/sample/android/app/MainActivity.java", 23, 31, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "23.1.1", null, null))
                    )
            ));
        }
        try (Harness h = Harness.newHarness("platform_frameworks_support")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "23.1.1", null, null)),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AppCompatActivity", SymbolKind.CLASS, "android.support.v7.app", Location.of("file:///v7/appcompat/src/android/support/v7/app/AppCompatActivity.java", 60, 13, 60, 30))
                    )
            );
        }

        // Cross-repo J2D instance 2
        try (Harness h = Harness.newHarness("AppIntro")) {
            h.expectXDefinition("file:///example/src/main/java/com/amqtech/opensource/appintroexample/ui/MainActivity.java", 6, 31, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "25.0.1", null, null))
                    )
            ));
        }
        try (Harness h = Harness.newHarness("platform_frameworks_support")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.CLASS, "AppCompatActivity", "android.support.v7.app.AppCompatActivity", "AppCompatActivity", "android.support.v7.app", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:appcompat-v7", "25.0.1", null, null)),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("AppCompatActivity", SymbolKind.CLASS, "android.support.v7.app", Location.of("file:///v7/appcompat/src/android/support/v7/app/AppCompatActivity.java", 60, 13, 60, 30))
                    )
            );
        }
    }

    @Test
    public void testJumpToAndroidSupportLibraryTransitiveDep() throws Exception {
        try (Harness h = Harness.newHarness("AppIntro")) {
            h.expectXDefinition("file:///library/src/main/java/com/github/paolorotolo/appintro/AppIntroViewPager.java", 5, 32, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            null,
                            SymbolDescriptor.of(ElementKind.CLASS, "ViewPager", "android.support.v4.view.ViewPager", "ViewPager", "android.support.v4.view", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:support-core-ui", "25.0.1", null, null))
                    )
            ));
        }
        try (Harness h = Harness.newHarness("platform_frameworks_support")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.CLASS, "ViewPager", "android.support.v4.view.ViewPager", "ViewPager", "android.support.v4.view", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.android.support:support-core-ui", "25.0.1", null, null)),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("ViewPager", SymbolKind.CLASS, "android.support.v4.view", Location.of("file:///core-ui/java/android/support/v4/view/ViewPager.java", 106, 13, 106, 22))
                    )
            );
        }
    }

    /**
     * Make sure our overlay for the Elastic Search repo works (its settings.gradle puts all its imports into a list, then
     * turns the list to an array of new strings and includes that, and its core/build.gradle sets srcDirs in a conditional)
     *
     * @throws Exception
     */
    @Category(SlowTestCategory.class)
    @Test
    public void testElasticSearch() throws Exception {
        try (Harness h = Harness.newHarness("elasticsearch")) {
            h.expectXDefinition("file:///core/src/main/java/org/elasticsearch/common/settings/Settings.java", 73, 58,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///core/src/main/java/org/elasticsearch/common/unit/ByteSizeValue.java", 131, 32, 131, 51),
                                    SymbolDescriptor.of(ElementKind.METHOD, "parseBytesSizeValue", "org.elasticsearch.common.unit.ByteSizeValue.parseBytesSizeValue(java.lang.String,java.lang.String)", "ByteSizeValue", "org.elasticsearch.common.unit", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.elasticsearch:elasticsearch", "4.4.5", null, null))
                            )
                    )
            );
            h.expectReferences("file:///core/src/main/java/org/elasticsearch/Build.java", 73, 26, true,
                    TextDocumentReferencesResult.of(
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 73, 26, 73, 36),
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 88, 13, 88, 23),
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 113, 15, 113, 25),
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 132, 12, 132, 22),
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 132, 32, 132, 42),
                            Location.of("file:///core/src/main/java/org/elasticsearch/Build.java", 144, 22, 144, 32)
                    )
            );
            h.expectXDefinition("file:///modules/percolator/src/main/java/org/elasticsearch/percolator/PercolateQueryBuilder.java", 72, 13,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///modules/percolator/src/main/java/org/elasticsearch/percolator/PercolateQueryBuilder.java", 72, 13, 72, 34),
                                    SymbolDescriptor.of(ElementKind.CLASS, "PercolateQueryBuilder", "org.elasticsearch.percolator.PercolateQueryBuilder", "PercolateQueryBuilder", "org.elasticsearch.percolator", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.elasticsearch:percolator", "4.4.5", null, null))
                            )
                    )
            );
        }
    }

    @Test
    public void testServletAPI() throws Exception {
        try (Harness h = Harness.newHarness("servlet-api")) {
            h.expectXDefinition("file:///java/javax/servlet/http/HttpServlet.java", 120, 43,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///java/javax/servlet/GenericServlet.java", 88, 22, 88, 36),
                                    SymbolDescriptor.of(ElementKind.CLASS, "GenericServlet", "javax.servlet.GenericServlet", "GenericServlet", "javax.servlet", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "javax.servlet:servlet-api", "3.0-alpha-1", null, null))
                            )
                    )
            );
        }
    }

    @Test
    public void testCrossRepoJumpToOverloadedMethod() throws Exception {

        // The following cases come in pairs -- the first of each pair returns a descriptor for an externally defined
        // symbol, and the second of each pair should take that same symbol and return its location.

        String ANDROID_TARGET_SYMBOL = "android.animation.ObjectAnimator.ofFloat(Object,String,float[])";
        try (Harness h = Harness.newHarness("AndroidViewAnimations")) {
            h.expectXDefinition("file:///library/src/main/java/com/daimajia/androidanimations/library/zooming_exits/ZoomOutAnimator.java", 37, 32,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.METHOD, "ofFloat", ANDROID_TARGET_SYMBOL, "ObjectAnimator", "android.animation", PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "com.google.android:android", "25", "git://github.com/androidmirrors/android-sdk", null))
                            )
                    )
            );
        }
        try (Harness h = Harness.newHarness("android-sdk")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.METHOD, "ofFloat", ANDROID_TARGET_SYMBOL, "ObjectAnimator", "android.animation", null),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("ofFloat(Object target, String propertyName, float... values)", SymbolKind.METHOD, "ObjectAnimator", Location.of("file:///sdk/android/animation/ObjectAnimator.java", 441, 33, 441, 40))
                    )
            );
        }


        String GUAVA_TARGET_SYMBOL = "com.google.common.collect.Iterables.<F,T>transform(Iterable<F>,Function<? super F,? extends T>)";
        try (Harness h = Harness.newHarness("java-pom-with-deps")) {
            h.expectXDefinition("file:///src/main/java/com/sourcegraph/depuser/GuavaUser.java", 24, 49,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    null,
                                    SymbolDescriptor.of(ElementKind.METHOD, "transform", GUAVA_TARGET_SYMBOL, "Iterables", "com.google.common.collect", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "com.google.guava:guava", "21.0", null, null))
                            )
                    )
            );
        }
        try (Harness h = Harness.newHarness("guava")) {
            h.expectWorkspaceSymbol(
                    SymbolDescriptor.of(ElementKind.METHOD, "transform", GUAVA_TARGET_SYMBOL, "Iterables", "com.google.common.collect", null),
                    WorkspaceSymbolResult.of(
                            SymbolInformation.of("<F, T>transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function)", SymbolKind.METHOD, "Iterables", Location.of("file:///guava/src/com/google/common/collect/Iterables.java", 735, 35, 735, 44))
                    )
            );
        }
    }

    @Test
    public void testDefaultMethods() throws Exception {
        try (Harness h = Harness.newHarness("mockito")) {
            h.expectHover("file:///src/main/java/org/mockito/Mock.java", 68, 37,
                    Hover.of(
                            MarkedString.of("java", "public static final org.mockito.Answers RETURNS_DEFAULTS"),
                            MarkedString.of("markdown", " The default configured answer of every mock.\n\n <p>Please see the {@link org.mockito.Mockito#RETURNS_DEFAULTS} documentation for more details.</p>\n\n @see org.mockito.Mockito#RETURNS_DEFAULTS\n")
                    )
            );
            h.expectXDefinition("file:///src/main/java/org/mockito/Mock.java", 68, 37,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///src/main/java/org/mockito/Answers.java", 34, 4, 34, 20),
                                    SymbolDescriptor.of(ElementKind.ENUM_CONSTANT, "RETURNS_DEFAULTS", "org.mockito.Answers.RETURNS_DEFAULTS", "Answers", "org.mockito", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.mockito:mockito-core", "2.7.16", null, null))
                            )
                    )
            );
        }
    }

    @Test
    public void testAxis2Java() throws Exception {
        try (Harness h = Harness.newHarness("axis2-java")) {
            h.expectXDefinition("file:///modules/adb/src/org/apache/axis2/databinding/types/Id.java", 27, 13,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///modules/adb/src/org/apache/axis2/databinding/types/Id.java", 27, 13, 27, 15),
                                    SymbolDescriptor.of(ElementKind.CLASS, "Id", "org.apache.axis2.databinding.types.Id", "Id", "org.apache.axis2.databinding.types", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.apache.axis2:axis2-adb", "1.8.0-SNAPSHOT", null, null))
                            )
                    )
            );
        }
    }

    /**
     * JDOM2 (Ant overlay)
     */
    @Test
    public void testJDOM() throws Exception {
        try (Harness h = Harness.newHarness("jdom")) {
            h.expectXDefinition("file:///core/src/java/org/jdom2/xpath/jaxen/JDOMXPath.java", 83, 14,
                    TextDocumentXDefinitionResult.of(
                            SymbolLocationInformation.of(
                                    Location.of("file:///core/src/java/org/jdom2/xpath/jaxen/JDOMXPath.java", 83, 13, 83, 22),
                                    SymbolDescriptor.of(ElementKind.CLASS, "JDOMXPath", "org.jdom2.xpath.jaxen.JDOMXPath", "JDOMXPath", "org.jdom2.xpath.jaxen", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.jdom:jdom2", "2.0.6", null, null))
                            )
                    )
            );
        }
    }

    @Test
    public void testJSONJava() throws Exception {
        try (Harness h = Harness.newHarness("JSON-java")) {
            h.expectXPackages(WorkspaceXPackagesResult.of(
                    PackageInformation.of(PackageDescriptor.of(PackageIdentifier.Type.MAVEN, "org.json:json", "20160810", null, null, "/"), Lists.newArrayList(
                            DependencyReference.of(PackageIdentifier.of(PackageIdentifier.Type.STDLIB, "STANDARD_LIB:jdk8", null, "git://github.com/jdkmirrors/openjdk8", null), ImmutableMap.of())
                    ))
            ));
            h.expectXDefinition("file:///JSONObject.java", 97, 13, TextDocumentXDefinitionResult.of(
                    SymbolLocationInformation.of(
                            Location.of("file:///JSONObject.java", 97, 13, 97, 23),
                            SymbolDescriptor.of(ElementKind.CLASS, "JSONObject", "org.json.JSONObject", "JSONObject", "org.json", PackageIdentifier.of(PackageIdentifier.Type.MAVEN, "org.json:json", "20160810", null, null))
                    )
            ));
        }
    }
}
