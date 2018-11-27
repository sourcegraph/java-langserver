package com.sourcegraph.lsp.domain.structures;


public class SymbolInformation implements SourceGenerable {

    public static SymbolInformation of(String name, SymbolKind kind, String containerName, Location location) {
        return new SymbolInformation().withName(name).withKind(kind).withContainerName(containerName).withLocation(location);
    }

    private String name;

    private SymbolKind kind;

    private Location location;

    private String containerName = null;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SymbolKind getKind() {
        return this.kind;
    }

    public void setKind(SymbolKind kind) {
        this.kind = kind;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public SymbolInformation withName(String name) {
        this.name = name;
        return this;
    }

    public SymbolInformation withKind(SymbolKind kind) {
        this.kind = kind;
        return this;
    }

    public SymbolInformation withLocation(Location location) {
        this.location = location;
        return this;
    }

    public SymbolInformation withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SymbolInformation that = (SymbolInformation) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (kind != that.kind) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        return containerName != null ? containerName.equals(that.containerName) : that.containerName == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (containerName != null ? containerName.hashCode() : 0);
        return result;
    }

    @Override
    public String generateSource(String linePrefix) {
        String linePrefix2 = linePrefix + "  ";
        return String.format("%s.of(\"%s\", %s, \"%s\", %s)", getClass().getSimpleName(),
                name, SourceGenerable.generateSource(getKind()), containerName, location.generateSource(linePrefix2));
    }
}
