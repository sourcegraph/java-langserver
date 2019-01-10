package com.sourcegraph.langserver;

import com.sourcegraph.langserver.langservice.JavacLanguageServer;
import com.sourcegraph.lsp.jsonrpc.Reader;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A WebSocket server that implements a language server over WebSocket.
 */
public class LSPWebSocketServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(LSPWebSocketServer.class);

    private ConcurrentHashMap<WebSocket, Stream> connections;
    private Function<Void, LanguageServer> languageServerProvider;

    public LSPWebSocketServer(int port, Function<Void, LanguageServer> languageServerProvider) {
        super(new InetSocketAddress(port));
        this.connections = new ConcurrentHashMap<>();
        this.languageServerProvider = languageServerProvider;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Stream wsConn = new Stream(conn);
        connections.put(conn, wsConn);
        LanguageServer ls = languageServerProvider.apply(null);
//        // TODO(beyang): use other constructor to add tracing and validation
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(ls, wsConn.in(), wsConn.out());
        if (ls instanceof LanguageClientAware) {
            ((LanguageClientAware)ls).connect(launcher.getRemoteProxy());
        }
        launcher.startListening();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        try {
            connections.get(conn).receive(msg);
        } catch (NullPointerException e) {
            throw new RuntimeException("Connection not found", e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("WebSocket error: {}", ex);
    }

    @Override
    public void onStart() {
        log.info("Listening for websocket connections");
    }

    /**
     * Stream wraps a WebSocket instance (a WebSocket connection) in an InputStream and OutputStream
     * interface. It assumes JSON-RPC messages (and uses these to determine the WebSocket frame
     * boundaries when translating from a stream of bytes.
     */
    private static class Stream {

        WebSocket conn;

        /**
         * Output pipe
         */
        PipedOutputStream outPipeOut;
        PipedInputStream outPipeIn;

        /**
         * Input pipe
         */
        PipedOutputStream inPipeOut;
        PipedInputStream inPipeIn;


        public Stream(WebSocket conn) {
            this.conn = conn;

            try {
                outPipeIn = new PipedInputStream(100000);
                outPipeOut = new PipedOutputStream(outPipeIn);
                Reader reader = new Reader(outPipeIn);
                reader.getInputObservable().subscribe(conn::send);
                reader.startReading();

                inPipeIn = new PipedInputStream(100000);
                inPipeOut = new PipedOutputStream(inPipeIn);
            } catch (IOException e) {
                throw new RuntimeException(e); // TODO(beyang)
            }

        }

        public InputStream in() {
            return new WrappedInputStream(inPipeIn);
        }

        public OutputStream out() {
            return outPipeOut;
        }

        public static class WrappedInputStream extends InputStream {
            InputStream inner;
            public WrappedInputStream(InputStream inner) {
                this.inner = inner;
            }

            @Override
            public int read() throws IOException {
                return this.inner.read();
            }
        }

        /**
         * receive receives incoming messages and writes these to the input stream
         */
        public void receive(String message) {
            try {
                byte[] messageBytes = message.getBytes();
                inPipeOut.write(String.format("Content-Length: %d\r\n\r\n", messageBytes.length).getBytes());
                inPipeOut.write(messageBytes);
            } catch (Exception e) {
                throw new RuntimeException(e); // TODO(beyang): replace with error
            }
        }
    }
}
