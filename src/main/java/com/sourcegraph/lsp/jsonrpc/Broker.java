package com.sourcegraph.lsp.jsonrpc;

import com.sourcegraph.utils.AsyncUtils;
import com.sourcegraph.utils.ExecutorUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.stream.Stream;

public class Broker {

    private static Logger log = LoggerFactory.getLogger(Broker.class);

    private ServerSocket serverSocket;

    public Broker() {
        this(0);
    }

    public Broker(int port) {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Listening for TCP connections on port {}", serverSocket.getLocalPort());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public Observable<Socket> getConnections() {

        Subject<Socket> connections = PublishSubject.create();

        AsyncUtils.runAsync(
                () -> Stream.generate(this::accept).sequential().forEachOrdered(connections::onNext),
                ExecutorUtils.getExecutorService()
        );

        return connections;
    }

    private Socket accept() {
        try {
            Socket socket = serverSocket.accept();
            log.trace("Accepted new TCP connection");
            return socket;
        } catch (IOException exception) {
            log.error("Error accepting TCP connection");
            throw new RuntimeException(exception);
        }
    }
}
