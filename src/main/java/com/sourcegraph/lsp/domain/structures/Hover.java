package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Hover implements SourceGenerable {

    /**
     * code's language name
     */
    public static final String LANGUAGE_JAVA = "java";

    public static final String LANGUAGE_MARKDOWN = "markdown";

    public static Hover of(MarkedString... contents) {
        return new Hover().withContents(Lists.newArrayList(contents));
    }

    private List<MarkedString> contents = new ArrayList<>();

    private Range range = null;

    public List<MarkedString> getContents() {
        return contents;
    }

    public void setContents(List<MarkedString> contents) {
        this.contents = contents;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Hover withRange(Range range) {
        this.range = range;
        return this;
    }

    public Hover withContents(List<MarkedString> contents) {
        this.contents = contents;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hover hover = (Hover) o;

        if (contents != null ? !contents.equals(hover.contents) : hover.contents != null) return false;
        return range != null ? range.equals(hover.range) : hover.range == null;
    }

    @Override
    public int hashCode() {
        int result = contents != null ? contents.hashCode() : 0;
        result = 31 * result + (range != null ? range.hashCode() : 0);
        return result;
    }

    @Override
    public String generateSource(String linePrefix) {
        String linePrefix2 = linePrefix + "  ";
        String children = String.join(",\n" + linePrefix2, this.contents.stream().map(c -> c.generateSource(linePrefix2)).collect(Collectors.toList()));
        return String.format("%s.of(\n%s%s\n%s)", this.getClass().getSimpleName(), linePrefix2, children, linePrefix);
    }
}
