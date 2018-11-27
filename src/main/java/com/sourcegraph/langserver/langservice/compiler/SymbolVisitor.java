package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.lsp.domain.structures.Location;
import com.sourcegraph.lsp.domain.structures.SymbolDescriptor;
import com.sourcegraph.lsp.domain.structures.SymbolInformation;
import com.sourcegraph.utils.LanguageUtils;
import com.sun.source.tree.*;
import com.sun.source.util.Trees;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SymbolVisitor implements TreeVisitor<Void, Void> {

    private static Logger log = LoggerFactory.getLogger(SymbolVisitor.class);

    private String packageName;

    private PositionCalculator positionCalculator;

    private String queryName;

    private SymbolDescriptor querySymbol;

    private List<SymbolInformation> symbols;

    private Stack<String> nameStack;

    public SymbolVisitor(Trees trees, CompilationUnitTree compilationUnit, String queryName) {
        this(trees, compilationUnit);
        this.queryName = queryName.toLowerCase();
        this.querySymbol = null;
    }

    public SymbolVisitor(Trees trees, CompilationUnitTree compilationUnit, SymbolDescriptor querySymbol) {
        this(trees, compilationUnit);
        this.queryName = querySymbol.getSimpleName().toLowerCase();
        this.querySymbol = querySymbol;
    }

    private SymbolVisitor(Trees trees, CompilationUnitTree compilationUnit) {
        this.packageName = (compilationUnit.getPackageName() != null ? compilationUnit.getPackageName().toString() : null);
        this.positionCalculator = new PositionCalculator(trees.getSourcePositions(), compilationUnit);
        this.symbols = new ArrayList<>();
        this.nameStack = new Stack<>();
    }

    public List<SymbolInformation> getSymbols() {
        return symbols;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        if (!matchPackage()) {
            return null;
        }
        if (compilationUnitTree.getPackageName() != null) {
            nameStack.push(compilationUnitTree.getPackageName().toString());
        }
        List<? extends Tree> types = compilationUnitTree.getTypeDecls();
        if (types != null) {
            for (Tree type : types) {
                type.accept(this, null);
            }
        }
        if (compilationUnitTree.getPackageName() != null) {
            nameStack.pop();
        }
        return null;
    }

    @Override
    public Void visitImport(ImportTree importTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitClass(ClassTree classTree, Void aVoid) {
        nameStack.push(classTree.getSimpleName().toString());
        if (matchQuery(LanguageUtils::isTopLevel)) {
            ElementKind queryKind = (querySymbol == null ? null : querySymbol.getElementKind());
            addSymbol(classTree, queryKind == null ? ElementKind.CLASS : queryKind);
        }
        List<? extends Tree> members = classTree.getMembers();
        if (members != null) {
            for (Tree member : members) {
                member.accept(this, null);
            }
        }
        nameStack.pop();
        return null;
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Void aVoid) {
        nameStack.push(getMethodString(methodTree, false, false));
        String name = methodTree.getName().toString();
        boolean ctor = "<init>".equals(name);
        if (ctor && nameStack.size() > 1) {
            // matching against class name instead
            name = nameStack.elementAt(nameStack.size() - 2);
        }
        if (matchQuery(this::isMethod, name)) {
            ElementKind queryKind = (querySymbol == null ? null : querySymbol.getElementKind());
            addSymbol(methodTree,
                    queryKind == null ? ElementKind.METHOD : queryKind,
                    getMethodString(methodTree, true, true), ctor);
        }
        nameStack.pop();
        return null;
    }

    @Override
    public Void visitVariable(VariableTree variableTree, Void aVoid) {
        nameStack.push(variableTree.getName().toString());
        if (matchQuery(this::isField)) {
            ElementKind queryKind = (querySymbol == null ? null : querySymbol.getElementKind());
            addSymbol(variableTree, queryKind == null ? ElementKind.FIELD : queryKind);
        }
        nameStack.pop();
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitBlock(BlockTree blockTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree switchTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitCase(CaseTree caseTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitTry(TryTree tryTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitCatch(CatchTree catchTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitIf(IfTree ifTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitBreak(BreakTree breakTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree continueTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree returnTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree throwTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitAssert(AssertTree assertTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree newClassTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree unaryTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree binaryTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree literalTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitOther(Tree tree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return null;
    }

    private void addSymbol(Tree tree, ElementKind kind) {
        addSymbol(tree, kind, null, false);
    }

    /**
     * @param tree   current AST tree
     * @param kind   current element's kind
     * @param name   symbol name, if empty then will be taken from the current stack
     * @param isCtor if current tree is a constructor, in this case special symbol name generation being used
     */
    private void addSymbol(Tree tree, ElementKind kind, String name, boolean isCtor) {
        if (name == null) {
            name = nameStack.peek();
        }
        Location location;
        if (isCtor) {
            String className = nameStack.size() > 1 ? nameStack.elementAt(nameStack.size() - 2) : "<init>";
            location = positionCalculator.getLocation((MethodTree) tree, className);
        } else {
            location = positionCalculator.getLocation(tree);
        }
        symbols.add(
                SymbolInformation.of(
                        name,
                        LanguageUtils.toSymbolKind(kind),
                        nameStack.size() > 1 ? nameStack.elementAt(nameStack.size() - 2) : "",
                        location
                )
        );
    }

    private boolean matchName(String name) {
        return name.toLowerCase().contains(queryName);
    }

    private boolean matchPackage() {
        return querySymbol == null ||
                querySymbol.getPackageName() == null ||
                packageName == null ||
                packageName.equals(querySymbol.getPackageName());
    }

    private boolean matchQuery(Predicate<ElementKind> kindPredicate) {
        return matchQuery(kindPredicate, null);
    }

    private boolean matchQuery(Predicate<ElementKind> kindPredicate, String simpleName) {

        if (simpleName == null) {
            simpleName = nameStack.peek();
        }

        if (querySymbol == null) {
            return matchName(simpleName);
        }

        if (querySymbol.getElementKind() != null && !kindPredicate.test(querySymbol.getElementKind())) {
            return false;
        }

        if (!querySymbol.getSimpleName().equals(simpleName)) {
            return false;
        }

        String qualifiedQueryName = querySymbol.getQualifiedName();
        if (StringUtils.isEmpty(qualifiedQueryName)) {
            return true;
        }

        String qualifiedNodeName = String.join(".", nameStack);
        return qualifiedNodeName.equals(qualifiedQueryName);
    }

    private boolean isMethod(ElementKind kind) {
        return kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR;
    }

    private boolean isField(ElementKind kind) {
        return kind == ElementKind.FIELD || kind == ElementKind.ENUM_CONSTANT;
    }

    /**
     * @param methodTree method AST tree
     * @param pretty     if we should produce pretty representation (spaces, parameter names)
     * @param onStack    true if method string representation is already on the top of a stack (used when constructing ctor names)
     * @return string representation of a given method tree
     */
    private String getMethodString(MethodTree methodTree, boolean pretty, boolean onStack) {
        StringBuilder ret = new StringBuilder();
        String typeParams = listOfTreesToString(methodTree.getTypeParameters(), pretty);
        if (!typeParams.isEmpty()) {
            ret.append('<').append(typeParams).append('>');
        }
        String name = methodTree.getName().toString();
        if ("<init>".equals(name)) {
            int offset = onStack ? 1 : 0;
            name = nameStack.size() > offset ? nameStack.elementAt(nameStack.size() - offset - 1) : "<init>";
        }
        ret.append(name);
        List<? extends Tree> params;
        if (!pretty) {
            params = methodTree
                    .getParameters()
                    .stream()
                    .map(VariableTree::getType)
                    .collect(Collectors.toList());
        } else {
            params = methodTree
                    .getParameters();
        }
        ret.append('(').append(listOfTreesToString(params, pretty)).append(')');
        String methodString = ret.toString();
        if (!pretty) {
            // KLUDGE -- turning a type into a string sometimes puts spaces after commas, sometimes not -- this seems
            // to depend mostly on whether we're serializing Trees or TypeMirrors. Just normalize it here so that it'll
            // match what xDefinition returns via LanguageUtils::getCrossRepoMethodName
            methodString = methodString.replace(", ", ",");
        }
        return methodString;
    }

    /**
     * @param list   list of AST trees
     * @param pretty if we should return pretty representation (with spaces)
     * @return string representation of a given list of AST trees
     */
    private static String listOfTreesToString(List<? extends Tree> list, boolean pretty) {
        StringBuilder ret = new StringBuilder();
        for (Tree tree : list) {
            if (ret.length() > 0) {
                ret.append(',');
                if (pretty) {
                    ret.append(' ');
                }
            }
            ret.append(tree.toString());
        }
        return ret.toString();
    }
}
