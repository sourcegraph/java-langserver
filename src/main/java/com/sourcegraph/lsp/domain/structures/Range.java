package com.sourcegraph.lsp.domain.structures;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Range {

    private Position start;

    private Position end;

    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Position getEnd() {
        return end;
    }

    public void setEnd(Position end) {
        this.end = end;
    }

    public Range withStart(Position start) {
        this.start = start;
        return this;
    }

    public Range withEnd(Position end) {
        this.end = end;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (start != null ? !start.equals(range.start) : range.start != null) return false;
        return end != null ? end.equals(range.end) : range.end == null;
    }

    @Override
    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }

    public boolean contains(Position position) {

        if (start.getLine() > position.getLine()) return false;
        if (end.getLine() < position.getLine()) return false;

        if (start.getLine() == position.getLine() && start.getCharacter() > position.getCharacter()) return false;
        if (end.getLine() == position.getLine() && end.getCharacter() < position.getCharacter()) return false;

        return true;
    }

    public String toString() {
        return String.format("%s-%s", start.toString(), end.toString());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return start.getLine() == end.getLine() && start.getCharacter() >= end.getCharacter();
    }
}
