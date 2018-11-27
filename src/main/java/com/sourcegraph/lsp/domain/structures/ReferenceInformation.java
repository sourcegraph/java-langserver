package com.sourcegraph.lsp.domain.structures;

/**
 * Created by beyang on 1/29/17.
 */
public class ReferenceInformation implements SourceGenerable {
    private Location reference;
    private SymbolDescriptor symbol;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReferenceInformation that = (ReferenceInformation) o;

        if (reference != null ? !reference.equals(that.reference) : that.reference != null) return false;
        return symbol != null ? symbol.equals(that.symbol) : that.symbol == null;
    }

    @Override
    public int hashCode() {
        int result = reference != null ? reference.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        return result;
    }

    public Location getReference() {
        return reference;
    }

    public void setReference(Location reference) {
        this.reference = reference;
    }

    public SymbolDescriptor getSymbol() {
        return symbol;
    }

    public void setSymbol(SymbolDescriptor symbol) {
        this.symbol = symbol;
    }

    public static ReferenceInformation of(Location reference, SymbolDescriptor symbol) {
        ReferenceInformation referenceInformation = new ReferenceInformation();
        referenceInformation.setReference(reference);
        referenceInformation.setSymbol(symbol);
        return referenceInformation;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s)",
                getClass().getSimpleName(),
                SourceGenerable.q(reference, linePrefix),
                SourceGenerable.q(symbol, linePrefix)
        );
    }
}
