package com.sourcegraph.lsp.domain.structures;

public class Location implements SourceGenerable {

    public static Location of(String uri, int startLine, int startCol, int endLine, int endCol) {
        return new Location().withUri(uri).withRange(new Range().withStart(new Position().withLine(startLine).withCharacter(startCol)).withEnd(new Position().withLine(endLine).withCharacter(endCol)));
    }

    private String uri;

    private Range range;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Location withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Location withRange(Range range) {
        this.range = range;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (uri != null ? !uri.equals(location.uri) : location.uri != null) return false;
        return range != null ? range.equals(location.range) : location.range == null;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (range != null ? range.hashCode() : 0);
        return result;
    }

    public String toString() {
        return uri + "[" + range.toString() + "]";
    }

    @Override
    public String generateSource(String prefix) {
        return String.format("Location.of(\"%s\", %d, %d, %d, %d)", uri, range.getStart().getLine(), range.getStart().getCharacter(), range.getEnd().getLine(), range.getEnd().getCharacter());
    }
}
