package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.lsp.domain.structures.Position;
import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class HoverVisitor implements TreeVisitor<LanguageData, Void> {

    private static Logger log = LoggerFactory.getLogger(HoverVisitor.class);

    private Trees trees;

    private CompilationUnitTree compilationUnit;

    private PositionCalculator positionCalculator;

    private SourcePositions sourcePositions;

    private int offset;

    public HoverVisitor(Trees trees, CompilationUnitTree compilationUnit, Position position) {
        this.trees = trees;
        this.compilationUnit = compilationUnit;
        this.sourcePositions = trees.getSourcePositions();
        this.positionCalculator = new PositionCalculator(sourcePositions, compilationUnit);
        this.offset = PositionCalculator.positionToOffset(getSourceFileContent(compilationUnit.getSourceFile()), position);
    }

    @Override
    public LanguageData visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        return firstNonNull(
                search(annotatedTypeTree.getAnnotations()),
                search(annotatedTypeTree.getUnderlyingType())
        );
    }

    @Override
    public LanguageData visitAnnotation(AnnotationTree annotationTree, Void aVoid) {

        // alexsaveliev: end position of Nth annotation argument may return -1
        // because compiler can transform @foo(bar) into @foo(value=bar) shifting offsets
        // bar location, however, points to proper location so we will ignore range check for
        // annotation arguments and rely on argument's children position
        List<? extends Tree> args = annotationTree.getArguments();
        if (args != null) {
            for (Tree tree : args) {
                LanguageData data = tree.accept(this, null);
                if (data != null) {
                    return data;
                }
            }
        }

        return firstNonNull(
                search(annotationTree.getAnnotationType()),
                getHoverData(annotationTree)
        );
    }

    @Override
    public LanguageData visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        return firstNonNull(
                search(methodInvocationTree.getTypeArguments()),
                search(methodInvocationTree.getMethodSelect()),
                search(methodInvocationTree.getArguments()),
                getHoverData(methodInvocationTree)
        );
    }

    @Override
    public LanguageData visitAssert(AssertTree assertTree, Void aVoid) {
        return firstNonNull(
                search(assertTree.getCondition()),
                search(assertTree.getDetail())
        );
    }

    @Override
    public LanguageData visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        return firstNonNull(
                search(assignmentTree.getVariable()),
                search(assignmentTree.getExpression())
        );
    }

    @Override
    public LanguageData visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        return firstNonNull(
                search(compoundAssignmentTree.getVariable()),
                search(compoundAssignmentTree.getExpression())
        );
    }

    @Override
    public LanguageData visitBinary(BinaryTree binaryTree, Void aVoid) {
        return firstNonNull(
                search(binaryTree.getLeftOperand()),
                search(binaryTree.getRightOperand())
        );
    }

    @Override
    public LanguageData visitBlock(BlockTree blockTree, Void aVoid) {
        return search(blockTree.getStatements());
    }

    @Override
    public LanguageData visitBreak(BreakTree breakTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitCase(CaseTree caseTree, Void aVoid) {
        return firstNonNull(
                search(caseTree.getExpression()),
                search(caseTree.getStatements())
        );
    }

    @Override
    public LanguageData visitCatch(CatchTree catchTree, Void aVoid) {
        return firstNonNull(
                search(catchTree.getParameter()),
                search(catchTree.getBlock())
        );
    }

    @Override
    public LanguageData visitClass(ClassTree classTree, Void aVoid) {
        if (inside(positionCalculator.getBoundingBox(classTree))) return getHoverData(classTree);
        return firstNonNull(
                search(classTree.getModifiers()),
                search(classTree.getTypeParameters()),
                search(classTree.getExtendsClause()),
                search(classTree.getImplementsClause()),
                search(classTree.getMembers())
        );
    }

    @Override
    public LanguageData visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        return firstNonNull(
                search(conditionalExpressionTree.getCondition()),
                search(conditionalExpressionTree.getTrueExpression()),
                search(conditionalExpressionTree.getFalseExpression())
        );
    }

    @Override
    public LanguageData visitContinue(ContinueTree continueTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        return firstNonNull(
                search(doWhileLoopTree.getCondition()),
                search(doWhileLoopTree.getStatement())
        );
    }

    @Override
    public LanguageData visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return search(expressionStatementTree.getExpression());
    }

    @Override
    public LanguageData visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        return firstNonNull(
                search(enhancedForLoopTree.getVariable()),
                search(enhancedForLoopTree.getExpression()),
                search(enhancedForLoopTree.getStatement())
        );
    }

    @Override
    public LanguageData visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        return firstNonNull(
                search(forLoopTree.getInitializer()),
                search(forLoopTree.getCondition()),
                search(forLoopTree.getUpdate()),
                search(forLoopTree.getStatement())
        );
    }

    @Override
    public LanguageData visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return getHoverData(identifierTree);
    }

    @Override
    public LanguageData visitIf(IfTree ifTree, Void aVoid) {
        return firstNonNull(
                search(ifTree.getCondition()),
                search(ifTree.getThenStatement()),
                search(ifTree.getElseStatement())
        );
    }

    @Override
    public LanguageData visitImport(ImportTree importTree, Void aVoid) {
        LanguageData ret = firstNonNull(
                search(importTree.getQualifiedIdentifier()),
                getHoverData(importTree));
        if (ret != null) {
            return ret;
        }
        if (importTree.isStatic()) {
            return staticImportData(importTree);
        }
        return null;
    }

    private LanguageData staticImportData(ImportTree importTree) {
        Tree ident = importTree.getQualifiedIdentifier();
        if (!contains(ident, offset)) {
            return null;
        }
        if (!(ident instanceof MemberSelectTree)) {
            return null;
        }
        MemberSelectTree mst = (MemberSelectTree) ident;
        if ("*".equals(mst.getIdentifier().toString())) {
            return null;
        }
        TreePath path = trees.getPath(compilationUnit, mst.getExpression());
        if (path == null) {
            return null;
        }
        Element element = trees.getElement(path);
        if (element == null) {
            return null;
        }
        Collection<? extends Element> elements = element.getEnclosedElements();
        if (elements == null) {
            return null;
        }
        for (Element candidate: elements) {
            if (candidate.getSimpleName().equals(mst.getIdentifier())) {
                LanguageData ret = new LanguageData(compilationUnit.getSourceFile().getName(), candidate, candidate.asType());
                ret.setLocation(positionCalculator.getLocation(ident));
                path = trees.getPath(candidate);
                if (path != null) {
                    ret.setDocComment(trees.getDocComment(path));
                }
                return ret;
            }
        }
        return null;
    }

    @Override
    public LanguageData visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        return firstNonNull(
                search(arrayAccessTree.getExpression()),
                search(arrayAccessTree.getIndex())
        );
    }

    @Override
    public LanguageData visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return search(labeledStatementTree.getStatement());
    }

    @Override
    public LanguageData visitLiteral(LiteralTree literalTree, Void aVoid) {
        return getHoverData(literalTree);
    }

    @Override
    public LanguageData visitMethod(MethodTree methodTree, Void aVoid) {

        String name = methodTree.getName().toString();

        if ("<init>".equals(name)) {
            Element constructorElement = getElement(methodTree);
            if (constructorElement != null) {
                Element classElement = constructorElement.getEnclosingElement();
                if (classElement != null) {
                    String className = classElement.getSimpleName().toString();
                    if (inside(positionCalculator.getBoundingBox(methodTree, className))) {
                        return getHoverData(methodTree);
                    }
                }
            }
        } else if (inside(positionCalculator.getBoundingBox(methodTree))) {
            return getHoverData(methodTree);
        }

        return firstNonNull(
                search(methodTree.getModifiers()),
                search(methodTree.getReturnType()),
                search(methodTree.getParameters()),
                search(methodTree.getThrows()),
                search(methodTree.getBody()),
                search(methodTree.getDefaultValue())
        );
    }

    @Override
    public LanguageData visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return search(modifiersTree.getAnnotations());
    }

    @Override
    public LanguageData visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        return firstNonNull(
                search(newArrayTree.getAnnotations()),
                search(newArrayTree.getType()),
                search(newArrayTree.getDimensions()),
                search(newArrayTree.getInitializers()),
                getHoverData(newArrayTree)
        );
    }

    @Override
    public LanguageData visitNewClass(NewClassTree newClassTree, Void aVoid) {
        if (contains(newClassTree.getIdentifier(), offset)) {
            if (newClassTree.getClassBody() != null) {
                // it's an anonymous class so use the class element (which corresponds to the identifier) rather than
                // the constructor element (which corresponds to this whole tree)
                return getHoverData(newClassTree.getIdentifier());
            } else {
                return getHoverData(newClassTree);
            }
        }
        return firstNonNull(
                search(newClassTree.getTypeArguments()),
                search(newClassTree.getArguments()),
                search(newClassTree.getClassBody())
        );
    }

    @Override
    public LanguageData visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return firstNonNull(
                search(lambdaExpressionTree.getParameters()),
                search(lambdaExpressionTree.getBody())
        );
    }

    @Override
    public LanguageData visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return search(parenthesizedTree.getExpression());
    }

    @Override
    public LanguageData visitReturn(ReturnTree returnTree, Void aVoid) {
        return search(returnTree.getExpression());
    }

    @Override
    public LanguageData visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return firstNonNull(
                search(memberSelectTree.getExpression()),
                getHoverData(memberSelectTree)
        );
    }

    @Override
    public LanguageData visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        return firstNonNull(
                search(memberReferenceTree.getTypeArguments()),
                search(memberReferenceTree.getQualifierExpression()),
                getHoverData(memberReferenceTree)
        );
    }

    @Override
    public LanguageData visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitSwitch(SwitchTree switchTree, Void aVoid) {
        return firstNonNull(
                search(switchTree.getExpression()),
                search(switchTree.getCases())
        );
    }

    @Override
    public LanguageData visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        return firstNonNull(
                search(synchronizedTree.getExpression()),
                search(synchronizedTree.getBlock())
        );
    }

    @Override
    public LanguageData visitThrow(ThrowTree throwTree, Void aVoid) {
        return search(throwTree.getExpression());
    }

    @Override
    public LanguageData visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        return firstNonNull(
                search(compilationUnitTree.getPackageName()),
                search(compilationUnitTree.getPackageAnnotations()),
                search(compilationUnitTree.getImports()),
                search(compilationUnitTree.getTypeDecls())
        );
    }

    @Override
    public LanguageData visitTry(TryTree tryTree, Void aVoid) {
        return firstNonNull(
                search(tryTree.getBlock()),
                search(tryTree.getCatches()),
                search(tryTree.getFinallyBlock()),
                search(tryTree.getResources())
        );
    }

    @Override
    public LanguageData visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        return firstNonNull(
                search(parameterizedTypeTree.getType()),
                search(parameterizedTypeTree.getTypeArguments())
        );
    }

    @Override
    public LanguageData visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return search(unionTypeTree.getTypeAlternatives());
    }

    @Override
    public LanguageData visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return search(intersectionTypeTree.getBounds());
    }

    @Override
    public LanguageData visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return firstNonNull(
                search(arrayTypeTree.getType()),
                getHoverData(arrayTypeTree)
        );
    }

    @Override
    public LanguageData visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        return firstNonNull(
                search(typeCastTree.getType()),
                search(typeCastTree.getExpression())
        );
    }

    @Override
    public LanguageData visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        return getHoverData(primitiveTypeTree);
    }

    @Override
    public LanguageData visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        return firstNonNull(
                search(typeParameterTree.getBounds()),
                search(typeParameterTree.getAnnotations()),
                getHoverData(typeParameterTree)
        );
    }

    @Override
    public LanguageData visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        return firstNonNull(
                search(instanceOfTree.getExpression()),
                search(instanceOfTree.getType())
        );
    }

    @Override
    public LanguageData visitUnary(UnaryTree unaryTree, Void aVoid) {
        return search(unaryTree.getExpression());
    }

    @Override
    public LanguageData visitVariable(VariableTree variableTree, Void aVoid) {
        if (inside(positionCalculator.getBoundingBox(variableTree))) return getHoverData(variableTree);
        return firstNonNull(
                search(variableTree.getModifiers()),
                search(variableTree.getType()),
                search(variableTree.getNameExpression()),
                search(variableTree.getInitializer())
        );
    }

    @Override
    public LanguageData visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        return firstNonNull(
                search(whileLoopTree.getCondition()),
                search(whileLoopTree.getStatement())
        );
    }

    @Override
    public LanguageData visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return search(wildcardTree.getBound());
    }

    @Override
    public LanguageData visitOther(Tree tree, Void aVoid) {
        return getHoverData(tree);
    }

    private LanguageData search(Tree tree) {
        if (tree != null && contains(tree, offset)) {
            return tree.accept(this, null);
        }
        return null;
    }

    private LanguageData search(List<? extends Tree> treeList) {
        if (treeList != null) {
            for (Tree tree : treeList) {
                if (contains(tree, offset)) {
                    return tree.accept(this, null);
                }
            }
        }
        return null;
    }

    private boolean contains(Tree tree, int offset) {
        if (tree == null) return false;
        long startOffset = sourcePositions.getStartPosition(compilationUnit, tree);
        long endOffset = sourcePositions.getEndPosition(compilationUnit, tree);
        return startOffset <= offset && offset < endOffset;
    }

    private boolean inside(Pair<Integer, Integer> range) {
        return range.getLeft() <= offset && offset < range.getRight();
    }

    // gonna be searching the trees a lot, so let's unroll these instead of using varargs

    private LanguageData firstNonNull(LanguageData a, LanguageData b) {
        return a != null
                ? a
                : b;
    }

    private LanguageData firstNonNull(LanguageData a, LanguageData b, LanguageData c) {
        return a != null
                ? a
                : firstNonNull(b, c);
    }

    private LanguageData firstNonNull(LanguageData a, LanguageData b, LanguageData c, LanguageData d) {
        return a != null
                ? a
                : firstNonNull(b, c, d);
    }

    private LanguageData firstNonNull(LanguageData a, LanguageData b, LanguageData c, LanguageData d, LanguageData e) {
        return a != null
                ? a
                : firstNonNull(b, c, d, e);
    }

    private LanguageData firstNonNull(LanguageData a, LanguageData b, LanguageData c, LanguageData d, LanguageData e, LanguageData f) {
        return a != null
                ? a
                : firstNonNull(b, c, d, e, f);
    }

    private CharSequence getSourceFileContent(JavaFileObject sourceFile) {
        try {
            return sourceFile.getCharContent(true);
        } catch (IOException exception) {
            log.error("Couldn't read content for {}", sourceFile.getName());
            throw new RuntimeException(exception);
        }
    }

    private LanguageData getHoverData(Tree tree) {

        if (tree == null) return null;
        if (!contains(tree, offset)) return null;

        JavaSourceRange sourceRange = positionCalculator.getJavaSourceRange(tree);
        TreePath path = trees.getPath(compilationUnit, tree);
        Element element = trees.getElement(path);
        if (element == null || sourceRange.getSize() <= 0) return null;

        LanguageData data = new LanguageData(compilationUnit.getSourceFile().getName(), element, trees.getTypeMirror(path));
        data.setLocation(positionCalculator.getLocation(tree));
        data.setDocComment(trees.getDocComment(path));
        return data;
    }

    private Element getElement(Tree tree) {
        TreePath path = trees.getPath(compilationUnit, tree);
        if (path == null) return null;
        return trees.getElement(path);
    }
}
