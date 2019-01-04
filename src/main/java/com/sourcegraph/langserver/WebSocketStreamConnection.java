package com.sourcegraph.langserver;

import com.sourcegraph.lsp.domain.Mapper;
import com.sourcegraph.lsp.jsonrpc.Reader;
import com.sun.tools.internal.ws.wsdl.document.Output;
import org.java_websocket.WebSocket;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * WebSocketStreamConnection presents a stream-based interface for reading/writing on a WebSocket connection.
 * It uses JSON-RPC messages to determine the frame boundaries.
 */
public class WebSocketStreamConnection {

    private static ExecutorService executorService = new ThreadPoolExecutor(
            2,
            5,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

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


    public WebSocketStreamConnection(WebSocket conn) {
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
        return inPipeIn;
    }

    public OutputStream out() {
        return outPipeOut;
    }

    // NEXT: hook up receive to WebSocketServer; then plug into lsp4j Launcher

    /**
     * receive receives incoming messages and writes these to the input stream
     */
    public void receive(String message) {
        try {
            inPipeOut.write(message.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO(beyang): replace with error
        }
    }
}
