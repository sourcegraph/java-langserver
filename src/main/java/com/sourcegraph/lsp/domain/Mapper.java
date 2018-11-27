package com.sourcegraph.lsp.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sourcegraph.lsp.domain.structures.MarkedString;
import com.sourcegraph.lsp.jsonrpc.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Mapper {

    private static Logger log = LoggerFactory.getLogger(Mapper.class);

    private static ObjectMapper objectMapper;

    private static TypeFactory typeFactory;

    static {
        try {
            objectMapper = new ObjectMapper();
            // add our custom MarkedString serializer and deserializer (see doc comments for MarkedString)
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(MarkedString.class, new MarkedString.MarkedStringSerializer());
            simpleModule.addDeserializer(MarkedString.class, new MarkedString.MarkedStringDeserializer());
            objectMapper.registerModule(simpleModule);
            typeFactory = objectMapper.getTypeFactory();
        } catch (Throwable e) {
            log.error("Fatal error (try setting JVM option -Djava.ext.dirs=\"\"):", e);
            System.exit(1);
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static TypeFactory getTypeFactory() {
        return typeFactory;
    }

    public static <T> JavaType constructType(Class<T> type) {
        return typeFactory.constructType(type);
    }

    public static <T> JavaType constructArrayType(Class<T> type) {
        return typeFactory.constructArrayType(type);
    }

    public static Message parseMessage(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, Message.class);
        } catch (IOException exception) {
            log.error("Malformed JSON content: {}", exception.getMessage());
            return new Message().withJsonrpc("malformed");
        }
    }

    public static String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <P> Request<P> convertMessageToRequest(Message message, Class<P> paramsClass) {
        try {
            JavaType requestType = typeFactory.constructParametricType(Request.class, paramsClass);
            return objectMapper.convertValue(message, requestType);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static <R> Response<R> convertMessageToResponse(Message message, Class<R> resultClass) {
        JavaType responseType = typeFactory.constructParametricType(Response.class, resultClass);
        return objectMapper.convertValue(message, responseType);
    }
}
