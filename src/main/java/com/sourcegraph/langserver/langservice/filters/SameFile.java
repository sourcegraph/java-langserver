package com.sourcegraph.langserver.langservice.filters;

import javax.tools.JavaFileObject;
import java.util.function.Predicate;

/**
 * Predicate that matches java file objects with the same file name
 */
class SameFile implements Predicate<JavaFileObject> {

    private String fileName;

    SameFile(String fileName) {
        this.fileName = fileName;
    }
    @Override
    public boolean test(JavaFileObject candidate) {
        return candidate.getName().equals(fileName);
    }
}
