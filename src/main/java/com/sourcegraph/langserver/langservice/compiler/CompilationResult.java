package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.lsp.domain.structures.Position;
import com.sourcegraph.lsp.domain.structures.SymbolDescriptor;
import com.sourcegraph.lsp.domain.structures.SymbolInformation;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;

public class CompilationResult {

    private static Logger log = LoggerFactory.getLogger(CompilationResult.class);

    private CompilationUnitTree compilationUnitTree;

    private Trees trees;

    private Types types;

    private Javac compiler;

    public CompilationResult(CompilationUnitTree compilationUnitTree, Trees trees, Types types, Javac compiler) {
        this.compilationUnitTree = compilationUnitTree;
        this.trees = trees;
        this.types = types;
        this.compiler = compiler;
    }

    public CompilationUnitTree getCompilationUnitTree() {
        return compilationUnitTree;
    }

    public Trees getTrees() {
        return trees;
    }

    public Javac getCompiler() {
        return compiler;
    }

    public Optional<LanguageData> findHover(Position position) {
        LanguageData hover = compilationUnitTree.accept(new HoverVisitor(trees, compilationUnitTree, position), null);
        return Optional.ofNullable(hover);
    }

    // search for a definition corresponding to a top-level type in this compilation unit
    public Optional<LanguageData> findDefinition(Element referenceElement, TypeMirror referenceType) {
        return findDefinition(compilationUnitTree, referenceElement, referenceType);
    }

    public Optional<LanguageData> findDefinition(Element definitionContainer, Element referenceElement, TypeMirror referenceType) {

        Tree defTree = trees.getTree(definitionContainer);
        if (defTree == null) {
            return Optional.empty();
        }
        return findDefinition(defTree, referenceElement, referenceType);
    }

    private Optional<LanguageData> findDefinition(Tree defTree, Element referenceElement, TypeMirror referenceType) {
        LanguageData data = compilationUnitTree.accept(new DefinitionVisitor(trees,
                        types,
                        compilationUnitTree,
                        referenceElement,
                        referenceType),
                null);
        if (data == null &&
                referenceElement.getKind() == ElementKind.CONSTRUCTOR &&
                referenceElement instanceof ExecutableElement) {
            // synthetic constructor?
            ExecutableElement executable = (ExecutableElement) referenceElement;
            if (executable.getParameters().isEmpty()) {
                data = compilationUnitTree.accept(new DefinitionVisitor(trees,
                                types,
                                compilationUnitTree,
                                referenceElement.getEnclosingElement(),
                                referenceElement.asType()),
                        null);
            }
        }
        return Optional.ofNullable(data);
    }

    // search for a definition inside an element contained within this compilation unit
    public List<LanguageData> findReferences(LanguageData.Signature signature) {
        ReferenceSignatureScanner scanner = new ReferenceSignatureScanner(trees, compilationUnitTree, signature);
        compilationUnitTree.accept(scanner, null);
        return scanner.getReferences();
    }

    public List<LanguageData> findReferences(LanguageData query) {
        ReferenceScanner scanner = new ReferenceScanner(
                trees,
                types,
                compilationUnitTree,
                query.getElement(),
                query.getTypeMirror()
        );
        compilationUnitTree.accept(scanner, null);
        return scanner.getReferences();
    }

    public boolean containsExactSymbol(Element queryElement) {
        StringFilteringVisitor visitor = new StringFilteringVisitor(queryElement, compilationUnitTree, trees, true);
        return compilationUnitTree.accept(visitor, null);
    }

    public boolean containsExactSymbol(String queryName) {
        StringFilteringVisitor visitor = new StringFilteringVisitor(queryName, compilationUnitTree, trees, true);
        return compilationUnitTree.accept(visitor, null);
    }

    public boolean containsExactSymbol(SymbolDescriptor querySymbol) {
        SymbolFilteringVisitor visitor = new SymbolFilteringVisitor(querySymbol, true);
        return compilationUnitTree.accept(visitor, null);
    }

    public boolean containsPartialSymbol(SymbolDescriptor querySymbol) {
        SymbolFilteringVisitor visitor = new SymbolFilteringVisitor(querySymbol, false);
        return compilationUnitTree.accept(visitor, null);
    }

    public boolean containsPartialSymbol(String queryName) {
        StringFilteringVisitor visitor = new StringFilteringVisitor(queryName, compilationUnitTree, trees, false);
        return compilationUnitTree.accept(visitor, null);
    }

    public List<SymbolInformation> findSymbols(String queryName) {
        SymbolVisitor scanner = new SymbolVisitor(
                trees,
                compilationUnitTree,
                queryName
        );
        compilationUnitTree.accept(scanner, null);
        return scanner.getSymbols();
    }

    public List<SymbolInformation> findSymbols(SymbolDescriptor queryDescriptor) {
        SymbolVisitor scanner = new SymbolVisitor(
                trees,
                compilationUnitTree,
                queryDescriptor
        );
        compilationUnitTree.accept(scanner, null);
        return scanner.getSymbols();
    }
}
