package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.langserver.langservice.compiler.CompilerOption;
import com.sourcegraph.langserver.langservice.compiler.Javac;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.ForgivingAttr;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.parser.FuzzyParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a reference to a Java compiler,
 * and several of its internal data structures,
 * which we need to fiddle with to get incremental compilation
 * and extract the diagnostic information we want.
 */
public class JavacHolder implements Javac {
    private static final Logger log = LoggerFactory.getLogger(JavacHolder.class);
    // javac places all of its internal state into this Context object,
    // which is basically a Map<String, Object>
    public Context context;
    // Error reporting initially goes nowhere
    // When we want to report errors back to VS Code, we'll replace this with something else
    private DiagnosticListener<JavaFileObject> errorsDelegate;
    // javac isn't friendly to swapping out the error-reporting DiagnosticListener,
    // so we install this intermediate DiagnosticListener, which forwards to errorsDelegate
    private final DiagnosticListener<JavaFileObject> errors;

    // Sets command-line options
    private Options options;

    private List<CompilerOption> originalOptions;
    private List<CompilerOption> rawOptions;

    // Pre-register some custom components before javac initializes

    private Log javacLog;
    private ForgivingAttr attr;
    private FuzzyParserFactory parserFactory;

    // Initialize javac

    public JavaCompiler compiler;

    // javac has already been initialized, fetch a few components for easy access

    private final Todo todo;
    private final JavacTrees trees;
    private final Types types;
    private final javax.lang.model.util.Types niceTypes;

    private final ShimFileManager fileManager;

    public JavacHolder(Workspace workspace, List<CompilerOption> compilerOptions) {
        this.context = new Context();
        this.errorsDelegate = diagnostic -> {};
        this.errors = diagnostic -> errorsDelegate.report(diagnostic);
        this.context.put(DiagnosticListener.class, errors);
        this.options = Options.instance(context);
        this.rawOptions = new ArrayList<>(compilerOptions);
        this.originalOptions = compilerOptions;
        rawOptions.add(new CompilerOption("-Xlint:none", ""));
        rawOptions.add(new CompilerOption("-nowarn", ""));
        this.rawOptions.forEach(option -> this.options.put(option.getName(), option.getValue()));
        this.javacLog = Log.instance(context);
        this.fileManager = new ShimFileManager(context, workspace);
        this.context.put(ShimFileManager.class, this.fileManager);
        this.attr = ForgivingAttr.instance(context);
        this.parserFactory = FuzzyParserFactory.instance(context);

        this.compiler = JavaCompiler.instance(context);

        this.compiler.keepComments = true;
        this.todo = Todo.instance(context);
        this.trees = JavacTrees.instance(context);
        this.types = Types.instance(context);
        this.niceTypes = JavacTypes.instance(context);

        MultiTaskListener.instance(context).add(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
                log.trace("started {}", e);

                JCTree.JCCompilationUnit unit = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            }

            @Override
            public void finished(TaskEvent e) {
                log.trace("finished {}", e);

                JCTree.JCCompilationUnit unit = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            }
        });
        this.javacLog.multipleErrors = true;
    }

    public List<CompilerOption> getOriginalOptions() {
        return originalOptions;
    }

    public List<CompilerOption> getOptions() {
        return rawOptions;
    }

    /**
     * Compile the indicated source file, and its dependencies if they have been modified.
     */
    public synchronized JCTree.JCCompilationUnit parse(JavaFileObject source) {

        JCTree.JCCompilationUnit result = compiler.parse(source);

        return result;
    }

    public synchronized CompilationUnitTree parseNicely(JavaFileObject file) {
        return parse(file);
    }

    public Trees getTrees() {
        return trees;
    }

    public javax.lang.model.util.Types getTypes() {
        return niceTypes;
    }

    /**
     * Compile a set of parsed files.
     *
     * If these files reference un-parsed dependencies, those dependencies will also be parsed and compiled.
     */
    public synchronized List<Element> analyze(CompilationUnitTree parsed) {

        List<Element> elements = new ArrayList<>();

        compiler.processAnnotations(compiler.enterTrees(com.sun.tools.javac.util.List.of((JCTree.JCCompilationUnit) parsed)));

        while (!todo.isEmpty()) {
            Env<AttrContext> next = todo.remove();

            try {
                // We don't do the desugar or generate phases, because they remove method bodies and methods
                Env<AttrContext> attributedTree = compiler.attribute(next);

                switch (attributedTree.tree.getTag()) {
                    case CLASSDEF:
                        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) attributedTree.tree;
                        if (classDecl.sym != null) {
                            elements.add(classDecl.sym);
                        }
                        break;
                    case TOPLEVEL:
                        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) attributedTree.tree;
                        if (compilationUnit.packge != null) {
                            elements.add(compilationUnit.packge);
                        }
                }
            } catch (Throwable e) {
                log.error("Error compiling {}", next.toplevel.sourcefile.getName(), e);
                // Keep going
            }
        }
        return elements;
    }
}
