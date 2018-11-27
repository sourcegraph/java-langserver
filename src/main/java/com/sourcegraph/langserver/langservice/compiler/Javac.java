package com.sourcegraph.langserver.langservice.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.util.List;

public interface Javac {

    List<CompilerOption> getOptions();


    /**
     * Compile the indicated source file, and its dependencies if they have been modified.
     */
    JCTree.JCCompilationUnit parse(JavaFileObject source);

    CompilationUnitTree parseNicely(JavaFileObject file);

    Trees getTrees();

    javax.lang.model.util.Types getTypes();

    /**
     * Compile a set of parsed files.
     * <p>
     * If these files reference un-parsed dependencies, those dependencies will also be parsed and compiled.
     */
    List<Element> analyze(CompilationUnitTree parsed);
}