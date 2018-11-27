package com.sourcegraph.lsp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sourcegraph.langserver.JavaLspHandlerService;
import com.sourcegraph.langserver.JavaLspHandlerServiceFactory;
import com.sourcegraph.langserver.langservice.files.FileSystemFileProvider;
import com.sourcegraph.lsp.domain.Method;
import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.Response;
import com.sourcegraph.lsp.domain.comparators.Comparators;
import com.sourcegraph.lsp.domain.params.*;
import com.sourcegraph.lsp.domain.result.*;
import com.sourcegraph.lsp.domain.structures.*;
import com.sourcegraph.lsp.exception.LspException;
import io.opentracing.mock.MockTracer;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Harness is a harness that test code can use to check behavior of LSP endpoints in the server.
 */
public class Harness implements AutoCloseable {

    /**
     * client is the client used to access the language server
     */
    public Controller client;

    public FileContentProvider fileContentProvider;

    public List<AssertionError> testErrors;

    private Harness(Controller client, FileContentProvider fileContentProvider) {
        this.client = client;
        this.fileContentProvider = fileContentProvider;
        this.testErrors = Lists.newArrayList();
    }

    public void expectDefinition(String uri, int line, int col, TextDocumentDefinitionResult expRes) throws LspException {
        checkJsonEqual(expRes, doDefinition(uri, line, col));
    }

    public TextDocumentDefinitionResult doDefinition(String uri, int line, int col) throws LspException {
        Request<TextDocumentPositionParams> req = new Request<TextDocumentPositionParams>()
                .withMethod(Method.TEXT_DOCUMENT_DEFINITION)
                .withParams(new TextDocumentPositionParams()
                        .withTextDocument(new TextDocumentIdentifier().withUri(uri))
                        .withPosition(new Position().withLine(line).withCharacter(col)));
        TextDocumentDefinitionResult res = this.client.getResponse(req, TextDocumentDefinitionResult.class);
        res.sort(Comparators.LOCATION);
        return res;
    }

    public void expectXDefinition(String uri, int line, int col, TextDocumentXDefinitionResult expRes) throws LspException {
        Request<TextDocumentPositionParams> req = new Request<TextDocumentPositionParams>()
                .withMethod(Method.TEXT_DOCUMENT_XDEFINITION)
                .withParams(new TextDocumentPositionParams()
                        .withTextDocument(new TextDocumentIdentifier().withUri(uri))
                        .withPosition(new Position().withLine(line).withCharacter(col)));
        TextDocumentXDefinitionResult res = this.client.getResponse(req, TextDocumentXDefinitionResult.class);
        res.sort(Comparators.SYMBOL_LOCATION_INFORMATION);
        checkJsonEqual(expRes, res);
    }

    public void expectXPackages(WorkspaceXPackagesResult expRes) throws LspException {
        Request<Void> req = new Request<Void>().withMethod(Method.WORKSPACE_XPACKAGES);
        WorkspaceXPackagesResult res = this.client.getResponse(req, WorkspaceXPackagesResult.class);
        res.sort(Comparators.PACKAGE_INFORMATION);
        checkJsonEqual(expRes, res);
    }

    public void expectXDependencies(WorkspaceXDependenciesResult expRes) throws LspException {
        Request<Void> req = new Request<Void>().withMethod(Method.WORKSPACE_XDEPENDENCIES);
        WorkspaceXDependenciesResult res = this.client.getResponse(req, WorkspaceXDependenciesResult.class);
        res.sort(Comparators.DEPENDENCY_REFERENCE);
        checkJsonEqual(expRes, res);
    }

    public Hover doHover(String uri, int line, int col) throws LspException {
        Request<TextDocumentPositionParams> req = new Request<TextDocumentPositionParams>()
                .withMethod(Method.TEXT_DOCUMENT_HOVER)
                .withParams(new TextDocumentPositionParams()
                        .withTextDocument(new TextDocumentIdentifier().withUri(uri))
                        .withPosition(new Position().withLine(line).withCharacter(col)));
        return this.client.getResponse(req, Hover.class);
    }

    public void expectHover(String uri, int line, int col, Hover expHover) throws LspException {
        checkJsonEqual(expHover, doHover(uri, line, col));
    }

    public void expectWorkspaceSymbol(String query, WorkspaceSymbolResult expRes) throws LspException {
        checkJsonEqual(expRes, doWorkspaceSymbol(query));
    }

    public void expectDocumentSymbol(String uri, DocumentSymbolResult expRes) throws LspException {
        checkJsonEqual(expRes, doDocumentSymbol(uri));
    }

    public WorkspaceSymbolResult doWorkspaceSymbol(String query) throws LspException {
        Request<WorkspaceSymbolParams> req = new Request<WorkspaceSymbolParams>()
                .withMethod(Method.WORKSPACE_SYMBOL)
                .withParams(new WorkspaceSymbolParams().withQuery(query));
        WorkspaceSymbolResult res = this.client.getResponse(req, WorkspaceSymbolResult.class);
        res.sort(Comparators.SYMBOL_INFORMATION);
        return res;
    }

    public DocumentSymbolResult doDocumentSymbol(String uri) throws LspException {
        Request<DocumentSymbolParams> req = new Request<DocumentSymbolParams>()
                .withMethod(Method.TEXT_DOCUMENT_DOCUMENT_SYMBOL)
                .withParams(new DocumentSymbolParams()
                        .withTextDocument(new TextDocumentIdentifier()
                                .withUri(uri)));
        DocumentSymbolResult res = this.client.getResponse(req, DocumentSymbolResult.class);
        return res;
    }

    public void expectWorkspaceSymbol(SymbolDescriptor symbol, WorkspaceSymbolResult expRes) throws LspException {
        Request<WorkspaceSymbolParams> req = new Request<WorkspaceSymbolParams>()
                .withMethod(Method.WORKSPACE_SYMBOL)
                .withParams(WorkspaceSymbolParams.of(null, symbol));
        WorkspaceSymbolResult res = this.client.getResponse(req, WorkspaceSymbolResult.class);
        res.sort(Comparators.SYMBOL_INFORMATION);
        checkJsonEqual(expRes, res);
    }

    public void expectReferences(String uri, int line, int col, boolean includeDeclaration, TextDocumentReferencesResult expRes) throws LspException {
        expectReferences(uri, line, col, includeDeclaration, null, expRes);
    }

    public void expectReferences(String uri, int line, int col, boolean includeDeclaration, Integer limit, TextDocumentReferencesResult expRes) throws LspException {
        checkJsonEqual(expRes, doReferences(uri, line, col, includeDeclaration, limit));
    }

    public TextDocumentReferencesResult doReferences(String uri, int line, int col, boolean includeDeclaration, Integer limit) throws LspException {
        Request<ReferenceParams> req = new Request<ReferenceParams>()
                .withMethod(Method.TEXT_DOCUMENT_REFERENCES)
                .withParams(new ReferenceParams()
                        .withContext(new ReferenceContext().withIncludeDeclaration(includeDeclaration).withXlimit(limit))
                        .withTextDocument(new TextDocumentIdentifier().withUri(uri))
                        .withPosition(new Position().withLine(line).withCharacter(col)));
        TextDocumentReferencesResult res = this.client.getResponse(req, TextDocumentReferencesResult.class);
        res.sort(Comparators.LOCATION);
        return res;
    }

    public void expectXReferences(SymbolDescriptor query,
                                  Map<String, String> hints,
                                  WorkspaceXReferencesResult expRes) throws LspException {
        expectXReferences(query, hints, null, expRes);

    }

    public void expectXReferences(SymbolDescriptor query,
                                  Map<String, String> hints,
                                  Integer limit,
                                  WorkspaceXReferencesResult expRes) throws LspException {
        Request<WorkspaceReferencesParams> req = new Request<WorkspaceReferencesParams>()
                .withMethod(Method.WORKSPACE_XREFERENCES)
                .withParams(WorkspaceReferencesParams.of(query, hints, limit));
        WorkspaceXReferencesResult res = this.client.getResponse(req, WorkspaceXReferencesResult.class);
        res.sort(Comparators.REFERENCE_INFORMATION);
        checkJsonEqual(expRes, res);
    }

    public void close() throws Exception {
        this.client.send(new Request<>().withMethod(Method.SHUTDOWN));
        this.client.send(new Request<>().withMethod(Method.EXIT));
        if (testErrors.size() > 0) {
            System.err.println("Test errors:");
            for (AssertionError e : testErrors) {
                e.printStackTrace(System.err);
            }
            fail(String.format("%d test assertions failed", testErrors.size()));
        }
    }

    /**
     * checkJsonEqual compares two objects on the basis of JSON equality.
     */
    private <T> boolean checkJsonEqual(T exp, T actual) {
        DiffMatchPatch differ = new DiffMatchPatch();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String exp_ = mapper.writeValueAsString(exp);
            String actual_ = mapper.writeValueAsString(actual);
            boolean isEqual = exp_.equals(actual_);
            if (!isEqual) {
                StringBuilder msg = new StringBuilder();
                LinkedList<DiffMatchPatch.Diff> diff = differ.diffMain(exp_, actual_, false);
                if (diff.size() > 0) {
                    msg.append(String.format("Expected:\n%s\nbut got:\n%s\n", exp_, actual_));
                } else {
                    for (DiffMatchPatch.Diff patch : diff) {
                        switch (patch.operation) {
                            case DELETE:
                                msg.append(String.format("[[[[[-%s]]]]]", patch.text));
                                break;
                            case INSERT:
                                msg.append(String.format("<<<<<+%s>>>>>", patch.text));
                                break;
                            case EQUAL:
                            default:
                                msg.append(patch.text);
                        }
                    }
                    msg.append("\n");
                }
                if (actual instanceof SourceGenerable) {
                    msg.append("To update expected to actual, use:\n");
                    String codegen = ((SourceGenerable) actual).generateSource("");
                    msg.append(codegen + "\n");
                } else {
                    msg.append(String.format("(Result is not codegen-able, so no suggested Java code is available. Consider making %s implement Codegenable to make your life easier.)", actual.getClass().getSimpleName()));
                }
                this.testErrors.add(new AssertionError(msg.toString()));
            }
            return isEqual;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * currentService is a singleton list that always contains the currently running LSP Handler (assuming test cases run serially).
     */
    public static final List<JavaLspHandlerService> currentService = Lists.newArrayList(); // single handler instance running at a given time
    private static boolean serverListening = false;
    private static Object serverListeningMu = new Object();

    public static MockTracer tracer;

    public static Harness newHarness(String testFilesRoot) throws Exception {
        return newHarness(Paths.get(Resources.getResource(testFilesRoot).toURI()));
    }

    public static synchronized Harness newHarness(Path root) throws Exception {
        synchronized (serverListeningMu) {
            if (!serverListening) {
                JavaLspHandlerServiceFactory javaLspHandlerServiceFactory = new JavaLspHandlerServiceFactory(true);
                Controller.serve(9090, (controller) -> {
                    currentService.clear();
                    currentService.add(javaLspHandlerServiceFactory.newHandlerService(controller));
                    return currentService.get(0);
                }, controller -> {
                    tracer = new MockTracer();
                    return tracer;
                }, false);
                serverListening = true;
            }
        }

        FileContentProvider fileContentProvider = new FileSystemFileProvider(root);
        Controller client = Controller.connect(9090, (controller) ->
                new MockClientMessageHandlerService(controller, fileContentProvider));
        InitializeParams initParams = new InitializeParams();
        initParams.setCapabilities(new ClientCapabilities());
        initParams.setRootUri("file:///");
        Request<InitializeParams> initRequest = new Request<InitializeParams>()
                .withMethod(Method.INITIALIZE)
                .withParams(initParams);
        Response<InitializeResult> initResponse = client.sendBlockingRequest(initRequest, InitializeResult.class);
        if (initResponse == null || initResponse.getError() != null) {
            throw new Exception("Initialization error: " + (initResponse == null ? "null response" : initResponse.getError().toString()));
        }
        return new Harness(client, fileContentProvider);
    }
}
