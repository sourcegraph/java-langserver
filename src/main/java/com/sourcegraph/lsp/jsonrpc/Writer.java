package com.sourcegraph.lsp.jsonrpc;

import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Writer {

    private static Logger log = LoggerFactory.getLogger(Writer.class);

    private BufferedWriter writer;

    public Writer(OutputStream outputStream) throws UnsupportedEncodingException {
        writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    public Observer<String> getOutputObserver() {

        Subject<String> outgoing = PublishSubject.create();

        outgoing.subscribe(this::write, this::error, writer::close);

        return outgoing;
    }

    private void write(String message) throws IOException {
        int len = message.getBytes(StandardCharsets.UTF_8).length;
        writer.write("Content-Length: ");
        writer.write(String.valueOf(len));
        writer.write("\r\n\r\n");
        writer.write(message);
        writer.flush();
    }

    private void error(Throwable throwable) {
        log.error("Error sending TCP message: {}", throwable.getMessage());
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Error closing output stream: {}", e.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        log.trace("Garbage collecting JSON-RPC output stream writer");
        super.finalize();
    }
}
