package com.sourcegraph.lsp.domain.structures;

public class Position {

    public static Position of(int line, int character) {
        return new Position().withLine(line).withCharacter(character);
    }

    private int line;

    private int character;

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public Position withLine(int line) {
        this.line = line;
        return this;
    }

    public Position withCharacter(int character) {
        this.character = character;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (line != position.line) return false;
        return character == position.character;
    }

    @Override
    public int hashCode() {
        int result = line;
        result = 31 * result + character;
        return result;
    }

    public String toString() {
        return String.format("(L%d,C%d)", line, character);
    }
}
