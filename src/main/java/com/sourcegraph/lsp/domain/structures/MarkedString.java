package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class MarkedString implements SourceGenerable {

    public static MarkedString of (String language, String value) {
        return new MarkedString().withLanguage(language).withValue(value);
    }

    private String language;

    private String value;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public MarkedString withLanguage(String language) {
        this.language = language;
        return this;
    }

    public MarkedString withValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarkedString that = (MarkedString) o;

        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = language != null ? language.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s)", this.getClass().getSimpleName(),
                SourceGenerable.q(language),
                SourceGenerable.q(value));
    }

    /**
     * Add a custom serializer and deserializer for marked strings -- this is because the markdown-formatted JavaDocs
     * should be serialized as plain strings, rather than as objects with `language` and `value` fields, in order to
     * be properly rendered in the VS Code tooltips. (The actual Java type signatures still need to serialized as
     * language+value objects though).
     */
    public static class MarkedStringSerializer extends StdSerializer<MarkedString> {

        public MarkedStringSerializer() {
            this(MarkedString.class);
        }

        MarkedStringSerializer(Class<MarkedString> t) {
            super(t);
        }

        @Override
        public void serialize(MarkedString markedString, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (markedString.getLanguage().equals("markdown")) {
                jsonGenerator.writeString(markedString.getValue());
            } else {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("language", markedString.getLanguage());
                jsonGenerator.writeStringField("value", markedString.getValue());
                jsonGenerator.writeEndObject();
            }
        }
    }

    public static class MarkedStringDeserializer extends StdDeserializer<MarkedString> {

        public MarkedStringDeserializer() {
            this(MarkedString.class);
        }

        MarkedStringDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public MarkedString deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            if (node instanceof TextNode) {
                return MarkedString.of("markdown", node.asText());
            } else {
                return MarkedString.of(node.get("language").asText(), node.get("value").asText());
            }
        }
    }
}
