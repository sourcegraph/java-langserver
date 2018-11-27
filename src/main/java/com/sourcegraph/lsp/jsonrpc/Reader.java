package com.sourcegraph.lsp.jsonrpc;

import com.sourcegraph.lsp.exception.LspConnectionClosed;
import com.sourcegraph.utils.AsyncUtils;
import com.sourcegraph.utils.ExecutorUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Reader {

    private static Logger log = LoggerFactory.getLogger(Reader.class);

    private static final int MAX_HEADER_LENGTH = 800; // seems like a reasonably long max header length

    private InputStream inputStream;

    private final Subject<String> incoming;

    public Reader(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
        this.incoming = PublishSubject.create();
    }

    public Observable<String> getInputObservable() {
        return incoming;
    }

    // provide this method so that this object's user can start reading after all subscribers are listening
    public synchronized void startReading() {
        AsyncUtils.runAsync(() -> {
            while (true) {
                synchronized (incoming) {
                    try {
                        String input = read();
                        if (input == null) {
                            log.warn("Terminating input stream");
                            incoming.onError(new LspConnectionClosed());
                            break;
                        } else {
                            incoming.onNext(input);
                        }
                    } catch (Throwable e) {
                        log.error("Terminating input stream on error: {}", e.getMessage());
                        incoming.onError(e);
                        break;
                    }
                }
            }
        }, ExecutorUtils.getExecutorService());
    }

    private synchronized String read() {

        Integer contentLength = null;

        String line;
        while (true) {

            line = readLine();

            if (line == null) {
                return null;
            }
            if (line.isEmpty()) {
                break;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex < 1 || colonIndex == line.length() - 1) {
                log.error("Malformed JSON-RPC header: {}", line);
                return null;
            }

            String headerName = line.substring(0, colonIndex).trim();
            String headerValue = line.substring(colonIndex + 1).trim();

            if (headerName.equals("Content-Length")) {
                contentLength = parseInt(headerValue);
                if (contentLength < 0) {
                    log.error("Invalid JSON-RPC Content-Length: {}", headerValue);
                    return null;
                }
            }
        }

        if (contentLength == null) {
            log.error("JSON-RPC message must contain Content-Length header");
            return null;
        }

        int soFar = 0;
        byte buffer[] = new byte[contentLength];

        do {
            int bytesRead = read(buffer, soFar, contentLength);
            if (bytesRead < 0) {
                return null;
            }
            contentLength -= bytesRead;
            soFar += bytesRead;
        } while (contentLength > 0);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    private synchronized String readLine() {

        byte[] buffer = new byte[80];
        int bytesRead = 0;

        while (bytesRead < MAX_HEADER_LENGTH) {

            if (bytesRead >= buffer.length) {
                byte[] newBuf = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
                buffer = newBuf;
            }

            byte theByte = readByte(inputStream);
            if (theByte < 0) {
                return null;
            }
            buffer[bytesRead++] = theByte;

            if (theByte == '\r') {
                theByte = readByte(inputStream);
                if (theByte < 0) {
                    return null;
                }
                buffer[bytesRead++] = theByte;
                if (theByte == '\n') {
                    return new String(buffer, 0, bytesRead - 2);
                }
            }
        }

        log.error("Unreasonably long JSON-RPC header");
        return null;
    }

    private byte readByte(InputStream inputStream) {
        try {
            int theByte = inputStream.read();
            if (theByte < 0) {
                log.warn("Couldn't read byte; TCP connection appears to be closed");
                close();
            }
            return (byte) theByte;
        } catch (IOException exception) {
            log.error("Error reading byte from TCP connection: {}", exception.getMessage());
            close();
            return -1;
        }
    }

    private synchronized int read(byte buffer[], int offset, int length) {
        try {
            int bytesRead = inputStream.read(buffer, offset, length);
            if (bytesRead < 0) {
                log.warn("Couldn't read block of bytes; TCP connection appears to be closed");
                close();
            }
            return bytesRead;
        } catch (IOException exception) {
            log.error("Error reading chunk from TCP connection: {}", exception.getMessage());
            close();
            return -1;
        }
    }

    private synchronized static int parseInt(String integer) {
        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private synchronized void close() {
        try {
            inputStream.close();
        } catch (IOException exception) {
            log.error("Error closing TCP connection: {}", exception.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        log.trace("Garbage collecting JSON-RPC input stream reader");
        super.finalize();
    }
}
