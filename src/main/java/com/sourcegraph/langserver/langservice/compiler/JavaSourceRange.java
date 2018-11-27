package com.sourcegraph.langserver.langservice.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSourceRange {

    private static Logger log = LoggerFactory.getLogger(JavaSourceRange.class);

    private String fileName;

    private int startOffset;

    private int endOffset;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public int getSize() {
        return endOffset - startOffset;
    }

    public JavaSourceRange withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public JavaSourceRange withStartOffset(int startOffset) {
        this.startOffset = startOffset;
        return this;
    }

    public JavaSourceRange withEndOffset(int endOffset) {
        this.endOffset = endOffset;
        return this;
    }

    public boolean contains(JavaSourceRange that) {
        return this.fileName.equals(that.fileName)
                && this.startOffset <= that.startOffset
                && this.endOffset >= that.endOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaSourceRange that = (JavaSourceRange) o;

        if (startOffset != that.startOffset) return false;
        if (endOffset != that.endOffset) return false;
        return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + startOffset;
        result = 31 * result + endOffset;
        return result;
    }

    public String toString() {
        return String.format("%s(%d,%d)", fileName, startOffset, endOffset);
    }
}
