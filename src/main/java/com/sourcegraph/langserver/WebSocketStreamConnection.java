package com.sourcegraph.langserver;

import com.sun.tools.internal.ws.wsdl.document.Output;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * WebSocketStreamConnection presents a stream-based interface for reading/writing on a WebSocket connection.
 * It uses JSON-RPC messages to determine the frame boundaries.
 */
public class WebSocketStreamConnection {
    WebSocket conn;
    InputStream in;
    OutputStream out;
    public WebSocketStreamConnection(WebSocket conn) {
        this.conn = conn;
        this.in = new InputStream(conn);
        this.out = new OutputStream(conn);
    }

    public InputStream input() {
        return in;
    }

    public OutputStream out() {
        return out;
    }

    /**
     * receive receives incoming messages and writes these to the input stream
     */
    public void receive(String message) {
        // NEXT: add input/output stream buffers
    }

    /**
     * writeNext writes the next sequence of bytes that comprises a complete WebSocket message by reading
     * from the output buffer.
     */
    void writeNext(String message) {

    }

    private static class OutputStream extends java.io.OutputStream {
        WebSocket conn;
        OutputStream(WebSocket conn) {
            this.conn = conn;
        }
        @Override
        public void write(int b) throws IOException {
            // TODO
        }
    }

    private static class InputStream extends java.io.InputStream {
        WebSocket conn;
        InputStream(WebSocket conn) {
            this.conn = conn;
        }

        void receive(String message) {

        }

        @Override
        public int read() throws IOException {
            // TODO
            return 0;
        }
    }
}
