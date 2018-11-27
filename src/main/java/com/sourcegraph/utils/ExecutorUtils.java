package com.sourcegraph.utils;

import java.util.concurrent.*;

/**
 * Utilities for executors, thread pools etc
 * Created by alexsaveliev on 23.02.2017.
 */
public class ExecutorUtils {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * The executor for fetching artifacts from remote repositories. Used to parallelize multiple levels of the artifact
     * fetching process.
     */
    private static ExecutorService artifactsFetcherExecutorService = new ThreadPoolExecutor(
            1000,
            1000,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    /**
     * Slightly less aggressive executor for fetching file contents from the LSP proxy
     */
    private static ExecutorService fileFetcherExecutorService = new ThreadPoolExecutor(
            200,
            200,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    /**
     * This executor should be used for direct fetching (i.e., manually fetching one artifact at a time) -- we want
     * to avoid spawning too many simultaneous connections, otherwise we might get rate-limited by Maven Central.
     * Maven itself defaults to downloading up to 5 artifacts concurrently.
     */
    private static ExecutorService reasonableFetcherExecutorService = new ThreadPoolExecutor(
            8,
            8,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static ExecutorService getArtifactsFetcherExecutorService() {
        return artifactsFetcherExecutorService;
    }

    public static ExecutorService getFileFetcherExecutorService() {
        return fileFetcherExecutorService;
    }

    public static ExecutorService getReasonableFetcherExecutorService() {
        return reasonableFetcherExecutorService;
    }
}
