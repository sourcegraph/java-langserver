package com.sourcegraph.langserver.langservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcegraph.lsp.Cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory cache for testing.
 */
public class MockCache implements Cache {

    private Map<String, String> data = new ConcurrentHashMap<>();

    private static ObjectMapper json = new ObjectMapper();

    @Override
    public <T> T get(String key, Class<T> valtype) throws Exception {
        String valueString = data.get(key);
        if (valueString == null) {
            return null;
        }
        return json.readValue(valueString, valtype);
    }

    @Override
    public <T> void set(String key, T value) {
        try {
            String valueString = json.writeValueAsString(value);
            data.put(key, valueString);
        } catch (JsonProcessingException e) {
            System.out.println("Couldn't serialize a value for mock caching.");
        }
    }

    public Map<String, String> getData() {
        return data;
    }
}
