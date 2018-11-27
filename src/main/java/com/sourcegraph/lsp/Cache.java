package com.sourcegraph.lsp;

/**
 * Cache is a simple cache interface that reflects the interface of the LSP cache methods ("xcache/set" and
 * "xcache/get").
 */
public interface Cache {

    /**
     * Provide a cache tag/prefix here so that we can easily version the cache entries.
     */
    String CACHE_TAG = "javaconfig-1.0.0::";

    /**
     * get returns the value for the given cache key if it exists. Returns null if no value exists for the key.
     * Throws an exception if there is an error accessing the cache.
     */
    <T> T get(String key, Class<T> valtype) throws Exception;

    /**
     * set sets the value for the given cache key. This is best effort and offers no guarantee as to whether the value
     * is actually contained in the cache on return.
     */
    <T> void set(String key, T value);

}
