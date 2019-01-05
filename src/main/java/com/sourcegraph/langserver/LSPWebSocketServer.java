package com.sourcegraph.langserver;

import com.sourcegraph.langserver.langservice.JavacLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class LSPWebSocketServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(LSPWebSocketServer.class);

    private ConcurrentHashMap<WebSocket, WebSocketStreamConnection> connections;

    public LSPWebSocketServer(int port) {
        super(new InetSocketAddress(port));
//        this.languageServers = new ConcurrentHashMap<>();
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        WebSocketStreamConnection wsConn = new WebSocketStreamConnection(conn);
        connections.put(conn, wsConn);
        JavacLanguageServer ls = new JavacLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(ls, wsConn.in(), wsConn.out());// TODO(beyang): use other constructor to add tracing and validation
        ls.connect(launcher.getRemoteProxy());
        launcher.startListening();

        // NEXT: why doesn't hover work?
    }

//    @Override
//    public void onOpen(WebSocket conn, ClientHandshake handshake) {
//        LSPConnection lspConn = new LSPConnection(conn);
//        LanguageService2 ls = new LanguageService2(lspConn);
//        languageServers.put(conn, ls);
//    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // TODO
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        try {
            connections.get(conn).receive(msg);
        } catch (NullPointerException e) {
            throw new RuntimeException("Connection not found", e);
        }
    }

//    @Override
//    public void onMessage(WebSocket conn, String msg) {
//        LanguageService2 ls = languageServers.get(conn);
//        if (ls == null) {
//            log.error("Did not find LanguageService for connection, dropping message {}", msg);
//            return;
//        }
//        Message message = Mapper.parseMessage(msg);
//        ls.dispatch(message);
//    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error starting websocket server", ex);
        // TODO
    }

    @Override
    public void onStart() {
        log.info("Listening for websocket connections");
    }
}
