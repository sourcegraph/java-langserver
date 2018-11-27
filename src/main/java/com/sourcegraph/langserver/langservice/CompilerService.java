package com.sourcegraph.langserver.langservice;

import com.sourcegraph.langserver.langservice.compiler.CompilationResult;
import com.sourcegraph.langserver.langservice.compiler.Javac;
import com.sourcegraph.langserver.langservice.workspace.Workspace;
import com.sourcegraph.langserver.langservice.workspace.WorkspaceManager;
import com.sourcegraph.lsp.Tracing;
import com.sourcegraph.utils.LanguageUtils;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import io.opentracing.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompilerService {

    private static final Logger log = LoggerFactory.getLogger(CompilerService.class);

    private WorkspaceManager workspaceManager;

    private ConcurrentHashMap<String, CompilationResult> parsed;

    private final Map<String, CompilationResult> analyzed;

    private ConcurrentHashMap<String, CompilationResult> declaredTypes;

    public CompilerService(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.parsed = new ConcurrentHashMap<>();
        this.analyzed = new HashMap<>(); // we're locking this one, so just use a regular HashMap
        this.declaredTypes = new ConcurrentHashMap<>();
    }

    public Optional<CompilationResult> parse(String uri) {
        try {
            Workspace workspace = workspaceManager.getWorkspaceContainingUri(uri);
            if (workspace == null) {
                log.error("Workspace not found for {}, returning empty parse result", uri);
                return Optional.empty();
            }
            JavaFileObject source = workspace.getSourceFile(uri);
            return source == null ? Optional.empty() : parse(source, workspace.getCompiler());
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", uri, e);
            return Optional.empty();
        }
    }

    public Optional<CompilationResult> parse(JavaFileObject file, Javac compiler) {
        CompilationResult result = parsed.computeIfAbsent(file.getName(), fileName -> {
            CompilationUnitTree compilationUnitTree = compiler.parseNicely(file);
            return new CompilationResult(compilationUnitTree, compiler.getTrees(), compiler.getTypes(), compiler);
        });
        return Optional.ofNullable(result);
    }

    public Optional<CompilationResult> analyze(String uri, Map<String, Object> ctx) {
        try {
            Workspace workspace = workspaceManager.getWorkspaceContainingUri(uri);
            if (workspace == null) {
                log.error("Workspace not found for {}, returning empty analyze result", uri);
                return Optional.empty();
            }
            // library indexing will be triggered by this::analyze(JavaFileObject file, Workspace workspace, Map<String, Object> ctx)

            JavaFileObject source = workspace.getSourceFile(uri);
            return source == null ? Optional.empty() : analyze(source, workspace, ctx);
        } catch (Exception e) {
            log.warn("Failed to analyze {}: {}", uri, e);
            return Optional.empty();
        }
    }

    public Optional<CompilationResult> analyze(JavaFileObject file, Workspace workspace, Map<String, Object> ctx) {
        // can't use ConcurrentHashMap::computeIfAbsent because we might need to update multiple entries and end up
        // deadlocking ourselves
        synchronized (analyzed) {
            Span analyzeSpan = Tracing.startSpanFromContext(ctx, "analyze");
            analyzeSpan.setTag("filename", file.getName());
            try {
                if (analyzed.containsKey(file.getName())) {
                    return Optional.ofNullable(analyzed.get(file.getName()));
                }
                Javac compiler = workspace.getCompiler();
                parse(file, compiler);
                CompilationResult parseResult = parsed.get(file.getName());
                for (Element element : compiler.analyze(parseResult.getCompilationUnitTree())) {
                    TreePath path = compiler.getTrees().getPath(element);
                    CompilationUnitTree tree = path.getCompilationUnit();
                    CompilationResult analyzeResult = new CompilationResult(tree, compiler.getTrees(), compiler.getTypes(), compiler);
                    parsed.putIfAbsent(tree.getSourceFile().getName(), analyzeResult);
                    analyzed.putIfAbsent(tree.getSourceFile().getName(), analyzeResult);

                    // map declared type names to their trees; this simplifies looking up definitions on the fly
                    ElementKind kind = element.getKind();
                    if (LanguageUtils.isTopLevel(kind)) {
                        String typeName = ((TypeElement) element).getQualifiedName().toString();
                        declaredTypes.putIfAbsent(typeName, analyzeResult);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Tracing.endSpan(analyzeSpan);
            }
            return Optional.ofNullable(analyzed.get(file.getName()))
                    .map(Optional::of)
                    .orElse(Optional.ofNullable(parsed.get(file.getName())));
        }
    }

    public Optional<CompilationResult> getDeclaredType(String typeName) {
        return Optional.ofNullable(declaredTypes.get(typeName));
    }
}
