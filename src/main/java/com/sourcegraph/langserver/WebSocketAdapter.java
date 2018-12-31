package com.sourcegraph.langserver;

import com.sourcegraph.langserver.langservice.LanguageService2;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketAdapter extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAdapter.class);

    private ConcurrentHashMap<WebSocket, LanguageService2> languageServers;

    public WebSocketAdapter(int port) {
        super(new InetSocketAddress(port));
        this.languageServers = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // NEXT
        System.out.println("# HERE");
        LanguageService2 ls = new LanguageService2();
        languageServers.put(conn, ls);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // TODO
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        LanguageService2 ls = languageServers.get(conn);
        if (ls == null) {
            // TODO: log error
            return;
        }
    }

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
