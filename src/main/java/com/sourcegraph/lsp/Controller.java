package com.sourcegraph.lsp;

import com.sourcegraph.common.Config;
import com.sourcegraph.lsp.domain.Mapper;
import com.sourcegraph.lsp.domain.Method;
import com.sourcegraph.lsp.domain.Request;
import com.sourcegraph.lsp.domain.Response;
import com.sourcegraph.lsp.domain.params.*;
import com.sourcegraph.lsp.domain.result.WorkspaceFilesResult;
import com.sourcegraph.lsp.domain.structures.JsonPatch;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import com.sourcegraph.lsp.domain.structures.TextDocumentItem;
import com.sourcegraph.lsp.exception.LspConnectionClosed;
import com.sourcegraph.lsp.exception.LspException;
import com.sourcegraph.lsp.jsonrpc.*;
import com.sourcegraph.lsp.jsonrpc.Error;
import com.sourcegraph.utils.ExecutorUtils;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Controller controls one end of one LSP connection. It is associated with exactly one connection.
 */
public class Controller implements FileContentProvider, PartialResultStreamer, Messenger, Cache {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private static Broker jsonRpcBroker = null;

    private String controllerId = generateId();

    private Observable<Message> incomingResponses;

    private Observer<Object> outgoingMessages;

    private MessageHandlerService messageHandlers;

    // Keep track of pending responses from the client that we're blocking on so that we can cancel them if the session
    // is shut down -- typically these will be pending file content responses. Keeping track of each Future in this way
    // and removing them as they complete is relatively clean and feasible because we block on them, and it allows us
    // to use a shared ExecutorService.
    private final ConcurrentHashMap<Object, Future> pendingResponses;

    // Create our own executor for request handling -- we don't block on language analysis (each analysis Future does
    // its work and sends its results to the client asynchronously), so it's messy to keep track of each Future and
    // remove them when they're done; it's much easier to give the controller its own executor so that it can cancel
    // the whole thing on shutdown.
    private final ExecutorService requestHandlingExecutor;

    private Tracer tracer;

    private Socket socket;

    private boolean logLsp;

    public Controller() {
        this(false);
    }

    public Controller(boolean logLsp) {
        this.logLsp = logLsp;
        this.pendingResponses = new ConcurrentHashMap<>();
        this.requestHandlingExecutor = Executors.newCachedThreadPool();
    }

    public static void serve(int port,
                             Function<Controller, MessageHandlerService> handlerSupplier,
                             Function<Controller, Tracer> tracerSupplier,
                             boolean logRequests) {
        if (jsonRpcBroker == null) {
            jsonRpcBroker = new Broker(port);
        }
        jsonRpcBroker.getConnections()
                .subscribe(connection -> {
                    Controller controller = new Controller(logRequests);
                    log.trace("Spawning new LSP controller instance {}", controller.getId());
                    controller.messageHandlers = handlerSupplier.apply(controller);
                    controller.tracer = tracerSupplier.apply(controller);
                    controller.handleConnection(connection);
                });
    }

    public static Controller connect(int port, Function<Controller, MessageHandlerService> handlerSupplier) throws IOException {
        if (jsonRpcBroker == null) {
            jsonRpcBroker = new Broker(port);
        }
        Socket socket = new Socket("127.0.0.1", port);
        Controller controller = new Controller();
        controller.messageHandlers = handlerSupplier.apply(controller);
        controller.handleConnection(socket);
        return controller;
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public String getId() {
        return controllerId;
    }

    public synchronized void send(Object object) {
        if (logLsp) {
            if (object instanceof Request) {
                log.trace("LSP request sent: " + Mapper.writeValueAsString(object));
            } else if (object instanceof Response) {
                log.trace("LSP response: " + Mapper.writeValueAsString(object));
            }
        }
        outgoingMessages.onNext(object);
    }

    public <P, R> Response<R> sendBlockingRequest(Request<P> request, Class<R> resultClass) throws LspException {
        // If ID is null, set it
        if (request.getId() == null) {
            request.setId(generateId());
        }

        BlockingQueue<Response<R>> inbox = new ArrayBlockingQueue<>(1);

        addResponseHandler(request.getId(), resultClass, response -> {
            try {
                inbox.put(response);
            } catch (InterruptedException exception) {
                log.error("Error enqueueing response for blocking request {}", request.getId());
            }
        });

        send(request);

        Future<Response<R>> futureResponse = ExecutorUtils.getExecutorService().submit(() -> {
            Response<R> response;
            try {
                response = inbox.poll(Config.LSP_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                log.warn("Interrupted while awaiting response for blocking request {}", Mapper.writeValueAsString(request));
                throw exception;
            }
            if (response == null) {
                log.warn("Timed out while awaiting response for blocking request {}", Mapper.writeValueAsString(request));
                throw new TimeoutException();
            }
            return response;
        });
        pendingResponses.put(request.getId().toString(), futureResponse);

        try {
            Response<R> response = futureResponse.get(Config.LSP_TIMEOUT, TimeUnit.SECONDS);
            if (response == null) {
                throw new TimeoutException();
            }
            return response;
        } catch (TimeoutException e) {
            String msg = "Timed out getting response for blocking request " + Mapper.writeValueAsString(request);
            log.warn(msg);
            throw new LspException(msg);
        } catch (InterruptedException e) {
            String msg = "Interrupted while getting response for blocking request " + Mapper.writeValueAsString(request);
            log.warn(msg);
            throw new LspException(msg);
        } catch (ExecutionException e) {
            String msg = "Error getting response for blocking request " + Mapper.writeValueAsString(request);
            log.warn(msg);
            throw new LspException(msg);
        } catch (CancellationException e) {
            String msg = "Task cancelled while getting response for blocking request " + Mapper.writeValueAsString(request);
            log.warn(msg);
            throw new LspException(msg);
        } finally {
            pendingResponses.remove(request.getId());
        }
    }

    // used solely by the test harness
    public <P, R> R getResponse(Request<P> request, Class<R> resultClass) throws LspException {
        Response<R> resp = this.sendBlockingRequest(request, resultClass);
        if (resp.getError() != null) {
            throw new LspException(resp.getError().toString());
        }
        return resp.getResult();
    }

    public <R> void addResponseHandler(Object id, Class<R> resultClass, ResponseHandler<R> handler) {
        incomingResponses
                .filter(message -> message.getId().equals(id))
                .map(message -> Mapper.convertMessageToResponse(message, resultClass))
                .firstOrError()
                .subscribe(handler::handle, this::onError);
    }

    private void handleUnknownMethod(Message message) {

        String unknownMethodName = message.getMethod();
        Object id = message.getId();

        if (id == null) {
            log.warn("Ignoring unknown method `{}` in notification", unknownMethodName);
        } else {
            log.warn("Ignoring unknown method `{}` in request {}", unknownMethodName, id);
        }

        Error error = new Error()
                .withCode(Error.Code.METHOD_NOT_FOUND)
                .withMessage("Method `" + unknownMethodName + "` not supported");

        Response<Void> errorResponse = new Response<Void>()
                .withError(error)
                .withId(id);

        send(errorResponse);
    }

    private void handleMalformedJson(Message message) {

        // error already logged by the Mapper; just construct an error response to send back to the client
        Error error = new Error()
                .withCode(Error.Code.PARSE_ERROR)
                .withMessage("Malformed JSON-RPC message");
        Response<Void> errorResponse = new Response<Void>()
                .withError(error);
        send(errorResponse);
    }

    private void onError(Throwable throwable) {

        if (throwable instanceof LspConnectionClosed) {
            log.warn("Connection closed for controller {}", controllerId);
        } else {
            log.error("Internal error: ", throwable);

            Error error = new Error()
                    .withCode(Error.Code.INTERNAL_ERROR)
                    .withMessage("Internal language server error");

            Response<Void> errorResponse = new Response<Void>()
                    .withError(error)
                    .withId(null);

            send(errorResponse);
        }

        shutdown();
    }

    public void shutdown() {
        // Cancel any threads that are still waiting for file contents, or stuck in computations (probably long-running
        // searches for references). Note that there's no danger of messing up shared state at this point because
        // the entire session is being shut down.
        synchronized (pendingResponses) {
            pendingResponses.values().forEach(task -> task.cancel(true));
        }
        synchronized (requestHandlingExecutor) {
            requestHandlingExecutor.shutdown();
            requestHandlingExecutor.shutdownNow();
        }
        outgoingMessages.onComplete();
        try {
            socket.close();
        } catch (IOException exception) {
            log.error("Error closing connection for LSP controller {}", controllerId);
        }
    }

    private void routeRequest(Message message) {
        Method method = Method.fromString(message.getMethod());
        switch (method) {
            case INITIALIZE:
                handleRequest(messageHandlers::initialize, InitializeParams.class, message);
                break;
            case SHUTDOWN:
                handleRequest(messageHandlers::shutdown, Void.class, message);
                break;
            case EXIT:
                handleRequest(messageHandlers::exit, Void.class, message);
                break;
            case CANCEL_REQUEST:
                handleRequest(messageHandlers::cancelRequest, CancelParams.class, message);
                break;
            case WORKSPACE_SYMBOL:
                handleRequest(messageHandlers::workspaceSymbol, WorkspaceSymbolParams.class, message);
                break;
            case WORKSPACE_XPACKAGES:
                handleRequest(messageHandlers::workspaceXPackages, Void.class, message);
                break;
            case WORKSPACE_XDEPENDENCIES:
                handleRequest(messageHandlers::workspaceXDependencies, Void.class, message);
                break;
            case WORKSPACE_XREFERENCES:
                handleRequest(messageHandlers::workspaceXReferences, WorkspaceReferencesParams.class, message);
                break;
            case WORKSPACE_FILES:
                handleRequest(messageHandlers::workspaceFiles, WorkspaceFilesParams.class, message);
                break;
            case TEXT_DOCUMENT_DID_CLOSE:
                handleRequest(messageHandlers::textDocumentDidClose, DidCloseTextDocumentParams.class, message);
                break;
            case TEXT_DOCUMENT_DID_OPEN:
                handleRequest(messageHandlers::textDocumentDidOpen, DidOpenTextDocumentParams.class, message);
                break;
            case TEXT_DOCUMENT_HOVER:
                handleRequest(messageHandlers::textDocumentHover, TextDocumentPositionParams.class, message);
                break;
            case TEXT_DOCUMENT_REFERENCES:
                handleRequest(messageHandlers::textDocumentReferences, ReferenceParams.class, message);
                break;
            case TEXT_DOCUMENT_DOCUMENT_SYMBOL:
                handleRequest(messageHandlers::textDocumentDocumentSymbol, DocumentSymbolParams.class, message);
                break;
            case TEXT_DOCUMENT_DEFINITION:
                handleRequest(messageHandlers::textDocumentDefinition, TextDocumentPositionParams.class, message);
                break;
            case TEXT_DOCUMENT_XDEFINITION:
                handleRequest(messageHandlers::textDocumentXDefinition, TextDocumentPositionParams.class, message);
                break;
            case TEXT_DOCUMENT_CONTENT:
                handleRequest(messageHandlers::textDocumentContent, TextDocumentContentParams.class, message);
                break;
            case PARTIAL_RESULT: // neither the mock client nor the server use these, so just drop them
                break;
            case UNKNOWN:
            default:
                handleUnknownMethod(message);
                break;
        }
    }

    private <P> void handleRequest(RequestHandler<P> requestHandler, Class<P> paramsClass, Message message) {

        synchronized (requestHandlingExecutor) {
            if (requestHandlingExecutor.isShutdown()) {
                // if we've shut down already, then ignore any further requests that might come in (typically the exit request)
                return;
            }
        }

        requestHandlingExecutor.submit(() -> {
            try {
                Request<P> request = Mapper.convertMessageToRequest(message, paramsClass);
                Map<String, Object> ctx = new ConcurrentHashMap<>();
                Tracing.contextSetTracer(ctx, tracer);
                Span rootSpan = Tracing.startSpanForRequest(ctx, request);
                String lslink = Tracing.lightstepLink(rootSpan);
                if (lslink != null) {
                    if (paramsClass != null && !paramsClass.equals(Void.class)) {
                        log.debug("Request [{}: {}] trace available at {}", request.getMethodAsString(), Mapper.writeValueAsString(request.getParams()), lslink);
                    } else {
                        log.debug("Request [{}] trace available at {}", request.getMethodAsString(), lslink);
                    }
                }
                Tracing.contextSetRootSpan(ctx, rootSpan);
                requestHandler.handle(ctx, request);
                if (rootSpan != null) {
                    rootSpan.finish();
                }
            } catch (OutOfMemoryError e) {
                log.error("FATAL ERROR!!!", e);
                Runtime.getRuntime().exit(1);
            } catch (Throwable e) {  // catch both Exceptions and Errors (Java compiler can throw assertion errors)
                // if the handler throws an exception, we assume it did not send a response, so we send an error response here
                log.error(String.format("Unhandled exception: %s", e.toString()));
                e.printStackTrace();
                this.send(new Response<>().withError(new Error().withCode(Error.Code.INTERNAL_ERROR).withMessage(e.toString())).withId(message.getId()));
            }
        });
    }

    private static Message parseAndLogMessage(String m) {
        Message msg = Mapper.parseMessage(m);
        if (msg != null && msg.getMethod() != null) {
            log.trace("LSP request received: " + m);
        }
        return msg;
    }

    private void handleConnection(Socket socket) {
        if (this.socket != null) {
            throw new RuntimeException("socket already exists");
        }
        this.socket = socket;
        try {
            // need to use an intermediate Subject here to correctly multi-cast to the downstream filters
            Subject<Message> incomingMessages = PublishSubject.create();
            Reader reader = new Reader(socket.getInputStream());
            reader.getInputObservable()
                    .map(logLsp ? Controller::parseAndLogMessage : Mapper::parseMessage)
                    .subscribe(incomingMessages);

            log.trace("Setting up request handlers for LSP controller {}", controllerId);
            incomingMessages
                    .filter(Message::isRequest)
                    .subscribe(this::routeRequest, this::onError);

            incomingResponses = incomingMessages.filter(Message::isResponse);

            // response handlers for individual request ids will be added by the user
            log.trace("Setting up handler for malformed JSON messages for LSP controller {}", controllerId);
            incomingMessages
                    .filter(message -> !message.isRequest() && !message.isResponse())
                    .subscribe(this::handleMalformedJson, this::onError);

            Subject<Object> outgoingSubject = PublishSubject.create();
            outgoingSubject
                    .map(Mapper::writeValueAsString)
                    .subscribe(new Writer(socket.getOutputStream()).getOutputObserver());

            outgoingMessages = outgoingSubject;

            reader.startReading();

        } catch (Exception exception) {
            log.error("Error spawning new LSP controller instance {}", controllerId);
        }
    }

    @Override
    public InputStream readContent(String uri) throws IOException {
        Request<TextDocumentContentParams> request = new Request<TextDocumentContentParams>()
                .withMethod(Method.TEXT_DOCUMENT_CONTENT)
                .withParams(new TextDocumentContentParams()
                        .withTextDocument(new TextDocumentIdentifier()
                                .withUri(uri)))
                .withId(Controller.generateId());

        try {
            Response<TextDocumentItem> response = this.sendBlockingRequest(request, TextDocumentItem.class);

            if (response.getError() != null) {
                throw new LspException(response.getError().toString() + ", Method: " + Method.TEXT_DOCUMENT_CONTENT);
            } else {
                return IOUtils.toInputStream(response.getResult().getText(), StandardCharsets.ISO_8859_1);
            }
        } catch (LspException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String uri) throws IOException {
        Request<WorkspaceFilesParams> request = new Request<WorkspaceFilesParams>()
                .withMethod(Method.WORKSPACE_FILES)
                .withParams(new WorkspaceFilesParams().withBase(uri))
                .withId(Controller.generateId());
        try {
            Response<WorkspaceFilesResult> response = this.sendBlockingRequest(request, WorkspaceFilesResult.class);
            if (response.getError() != null) {
                log.error("Error receiving response for `{}` request {}", Method.WORKSPACE_FILES.toString(), request.getId());
                throw new IOException(new LspException(response.getError().toString()));
            } else {
                return response.getResult();
            }
        } catch (LspException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public <T> T get(String key, Class<T> valtype) throws Exception {

        Request<CacheGetParams> req = new Request<CacheGetParams>()
                .withMethod(Method.CACHE_GET)
                .withParams(CacheGetParams.of(key));

        Response<T> response = this.sendBlockingRequest(req, valtype);
        if (response.getError() != null) {
            throw new LspException(response.getError().toString() + ", Method: " + Method.CACHE_GET);
        }
        return response.getResult();
    }

    @Override
    public <T> void set(String key, T value) {
        Request<CacheSetParams> req = new Request<CacheSetParams>()
                .withMethod(Method.CACHE_SET)
                .withParams(CacheSetParams.of(key, value));
        this.send(req);
    }

    @Override
    public void sendPartialResult(Object requestId, JsonPatch jsonPatch) {

        PartialResultParams params = PartialResultParams.of(requestId, jsonPatch);

        Request<PartialResultParams> partialResultNotification = new Request<PartialResultParams>()
                .withMethod(Method.PARTIAL_RESULT)
                .withParams(params);

        send(partialResultNotification);
    }

    @Override
    protected void finalize() throws Throwable {
        log.trace("Garbage collecting LSP controller {}", controllerId);
        super.finalize();
    }

    @Override
    public void showMessage(MessageType messageType, String message) {
        send(new Request<>().withMethod(Method.WINDOW_SHOW_MESSAGE).withParams(
                ShowMessageParams.of(messageType, message))
        );
    }
}
