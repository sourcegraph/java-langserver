package com.sourcegraph.lsp.domain.structures;

public class SymbolLocationInformation implements SourceGenerable {

    public static SymbolLocationInformation of(Location location, SymbolDescriptor symbol) {
        return new SymbolLocationInformation().withLocation(location).withSymbol(symbol);
    }

    private Location location;
    private SymbolDescriptor symbol;

    public Location getLocation() {
        return location;
    }

    public SymbolDescriptor getSymbol() {
        return symbol;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setSymbol(SymbolDescriptor symbol) {
        this.symbol = symbol;
    }

    public SymbolLocationInformation withLocation(Location location) {
        this.location = location;
        return this;
    }

    public SymbolLocationInformation withSymbol(SymbolDescriptor symbol) {
        this.symbol = symbol;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SymbolLocationInformation that = (SymbolLocationInformation) o;

        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        return symbol != null ? symbol.equals(that.symbol) : that.symbol == null;
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        return result;
    }

    @Override
    public String generateSource(String linePrefix) {
        String linePrefix2 = linePrefix + "  ";
        return String.format("%s.of(\n%s%s,\n%s%s\n%s)", getClass().getSimpleName(), linePrefix2, location == null ? "null" : location.generateSource(linePrefix2), linePrefix2, symbol.generateSource(linePrefix2), linePrefix);
    }

}
