package com.sourcegraph.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Utilities for asynchronous code execution
 * Created by alexsaveliev on 21.02.2017.
 */
public class AsyncUtils {

    private static final Logger log = LoggerFactory.getLogger(AsyncUtils.class);

    /**
     * Runs runnable asynchronously, catches and reports all errors
     * @param runnable runnable to run
     * @param executor executor responsible for thread creation and code running
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                // TODO: bubble up this error
                log.error("An error occurred during asynchronous execution", e);
            }
        }, executor);
    }
}
