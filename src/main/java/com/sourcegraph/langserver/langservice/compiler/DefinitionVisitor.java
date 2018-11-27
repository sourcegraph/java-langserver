package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.lsp.domain.structures.Location;
import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

public class DefinitionVisitor implements TreeVisitor<LanguageData, Void> {

    private static Logger log = LoggerFactory.getLogger(DefinitionVisitor.class);

    private Trees trees;

    private Types types;

    private CompilationUnitTree compilationUnit;

    private PositionCalculator positionCalculator;

    private SourcePositions sourcePositions;

    private Element referenceElement;

    private TypeMirror referenceType;

    public DefinitionVisitor(Trees trees, Types types, CompilationUnitTree compilationUnit, Element referenceElement, TypeMirror referenceType) {
        this.trees = trees;
        this.types = types;
        this.compilationUnit = compilationUnit;
        this.sourcePositions = trees.getSourcePositions();
        this.positionCalculator = new PositionCalculator(sourcePositions, compilationUnit);
        this.referenceElement = referenceElement;
        this.referenceType = referenceType;
    }

    @Override
    public LanguageData visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        LanguageData data = search(annotatedTypeTree.getAnnotations(), null);
        if (data != null) return data;
        return search(annotatedTypeTree.getUnderlyingType(), null);
    }

    @Override
    public LanguageData visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        LanguageData data = search(annotationTree.getAnnotationType(), null);
        if (data != null) return data;
        return search(annotationTree.getArguments(), null);
    }

    @Override
    public LanguageData visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        LanguageData data = search(methodInvocationTree.getTypeArguments(), null);
        if (data != null) return data;
        data = search(methodInvocationTree.getMethodSelect(), null);
        if (data != null) return data;
        return search(methodInvocationTree.getArguments(), null);
    }

    @Override
    public LanguageData visitAssert(AssertTree assertTree, Void aVoid) {
        LanguageData data = search(assertTree.getCondition(), null);
        if (data != null) return data;
        return search(assertTree.getDetail(), null);
    }

    @Override
    public LanguageData visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        LanguageData data = search(assignmentTree.getVariable(), null);
        if (data != null) return data;
        return search(assignmentTree.getExpression(), null);
    }

    @Override
    public LanguageData visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        LanguageData data = search(compoundAssignmentTree.getVariable(), null);
        if (data != null) return data;
        return search(compoundAssignmentTree.getExpression(), null);
    }

    @Override
    public LanguageData visitBinary(BinaryTree binaryTree, Void aVoid) {
        LanguageData data = search(binaryTree.getLeftOperand(), null);
        if (data != null) return data;
        return search(binaryTree.getRightOperand(), null);
    }

    @Override
    public LanguageData visitBlock(BlockTree blockTree, Void aVoid) {
        return search(blockTree.getStatements(), aVoid);
    }

    @Override
    public LanguageData visitBreak(BreakTree breakTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitCase(CaseTree caseTree, Void aVoid) {
        LanguageData data = search(caseTree.getExpression(), null);
        if (data != null) return data;
        return search(caseTree.getStatements(), null);
    }

    @Override
    public LanguageData visitCatch(CatchTree catchTree, Void aVoid) {
        LanguageData data = search(catchTree.getParameter(), null);
        if (data != null) return data;
        return search(catchTree.getBlock(), null);
    }

    @Override
    public LanguageData visitClass(ClassTree classTree, Void aVoid) {
        LanguageData data = checkDefinition(classTree);
        if (data != null) return data;
        data = search(classTree.getTypeParameters(), null);
        if (data != null) return data;
        return search(classTree.getMembers(), null);
    }

    @Override
    public LanguageData visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        LanguageData data = search(conditionalExpressionTree.getCondition(), null);
        if (data != null) return data;
        data = search(conditionalExpressionTree.getTrueExpression(), null);
        if (data != null) return data;
        return search(conditionalExpressionTree.getFalseExpression(), null);
    }

    @Override
    public LanguageData visitContinue(ContinueTree continueTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        LanguageData data = search(doWhileLoopTree.getCondition(), null);
        if (data != null) return data;
        return search(doWhileLoopTree.getStatement(), null);
    }

    @Override
    public LanguageData visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return search(expressionStatementTree.getExpression(), null);
    }

    @Override
    public LanguageData visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        LanguageData data = search(enhancedForLoopTree.getExpression(), null);
        if (data != null) return data;
        data = search(enhancedForLoopTree.getVariable(), null);
        if (data != null) return data;
        return search(enhancedForLoopTree.getStatement(), null);
    }

    @Override
    public LanguageData visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        LanguageData data = search(forLoopTree.getInitializer(), null);
        if (data != null) return data;
        data = search(forLoopTree.getCondition(), null);
        if (data != null) return data;
        data = search(forLoopTree.getUpdate(), null);
        if (data != null) return data;
        return search(forLoopTree.getStatement(), null);
    }

    @Override
    public LanguageData visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitIf(IfTree ifTree, Void aVoid) {
        LanguageData data = search(ifTree.getCondition(), null);
        if (data != null) return data;
        data = search(ifTree.getThenStatement(), null);
        if (data != null) return data;
        return search(ifTree.getElseStatement(), null);
    }

    @Override
    public LanguageData visitImport(ImportTree importTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        LanguageData data = search(arrayAccessTree.getExpression(), null);
        if (data != null) return data;
        return search(arrayAccessTree.getIndex(), null);
    }

    @Override
    public LanguageData visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return search(labeledStatementTree.getStatement(), null);
    }

    @Override
    public LanguageData visitLiteral(LiteralTree literalTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitMethod(MethodTree methodTree, Void aVoid) {
        LanguageData data = checkDefinition(methodTree);
        if (data != null) return data;
        data = search(methodTree.getTypeParameters(), null);
        if (data != null) return data;
        data = search(methodTree.getParameters(), null);
        if (data != null) return data;
        return search(methodTree.getBody(), null);
    }

    @Override
    public LanguageData visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        LanguageData data = search(newArrayTree.getAnnotations(), null);
        if (data != null) return data;
        data = search(newArrayTree.getInitializers(), null);
        if (data != null) return data;
        return search(newArrayTree.getType(), null);
    }

    @Override
    public LanguageData visitNewClass(NewClassTree newClassTree, Void aVoid) {
        LanguageData data = search(newClassTree.getClassBody(), null);
        if (data != null) return data;
        data = search(newClassTree.getArguments(), null);
        if (data != null) return data;
        return search(newClassTree.getTypeArguments(), null);
    }

    @Override
    public LanguageData visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        LanguageData data = search(lambdaExpressionTree.getParameters(), null);
        if (data != null) return data;
        return search(lambdaExpressionTree.getBody(), null);
    }

    @Override
    public LanguageData visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return search(parenthesizedTree.getExpression(), null);
    }

    @Override
    public LanguageData visitReturn(ReturnTree returnTree, Void aVoid) {
        return search(returnTree.getExpression(), null);
    }

    @Override
    public LanguageData visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return search(memberSelectTree.getExpression(), null);
    }

    @Override
    public LanguageData visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        LanguageData data = search(memberReferenceTree.getTypeArguments(), null);
        if (data != null) return data;
        return search(memberReferenceTree.getQualifierExpression(), null);
    }

    @Override
    public LanguageData visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitSwitch(SwitchTree switchTree, Void aVoid) {
        LanguageData data = search(switchTree.getExpression(), null);
        if (data != null) return data;
        return search(switchTree.getCases(), aVoid);
    }

    @Override
    public LanguageData visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        LanguageData data = search(synchronizedTree.getExpression(), null);
        if (data != null) return data;
        return search(synchronizedTree.getBlock(), null);
    }

    @Override
    public LanguageData visitThrow(ThrowTree throwTree, Void aVoid) {
        return search(throwTree.getExpression(), null);
    }

    @Override
    public LanguageData visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        LanguageData data = search(compilationUnitTree.getPackageAnnotations(), null);
        if (data != null) return data;
        return search(compilationUnitTree.getTypeDecls(), null);
    }

    @Override
    public LanguageData visitTry(TryTree tryTree, Void aVoid) {
        LanguageData data = search(tryTree.getBlock(), null);
        if (data != null) return data;
        data = search(tryTree.getCatches(), null);
        if (data != null) return data;
        data = search(tryTree.getFinallyBlock(), null);
        if (data != null) return data;
        return search(tryTree.getResources(), null);
    }

    @Override
    public LanguageData visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        // TODO: figure out what we were trying to do here ... seems like we gave up part-way through
//        TreePath path = trees.getPath(compilationUnit, parameterizedTypeTree);
//        TypeMirror typeMirror = trees.getTypeMirror(path);
//        if (typeMirror.getKind() == TypeKind.DECLARED) {
//            TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
//        }

        LanguageData data = search(parameterizedTypeTree.getType(), null);
        if (data != null) return data;
        return search(parameterizedTypeTree.getTypeArguments(), null);
        // TODO: see if we can fetch the generic of which this is an instance
    }

    @Override
    public LanguageData visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return search(unionTypeTree.getTypeAlternatives(), null);
    }

    @Override
    public LanguageData visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return search(intersectionTypeTree.getBounds(), null);
    }

    @Override
    public LanguageData visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return search(arrayTypeTree.getType(), null);
    }

    @Override
    public LanguageData visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        LanguageData data = search(typeCastTree.getType(), null);
        if (data != null) return data;
        return search(typeCastTree.getExpression(), null);
    }

    @Override
    public LanguageData visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        return null;
    }

    @Override
    public LanguageData visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        LanguageData data = checkDefinition(typeParameterTree);
        if (data != null) return data;
        data = search(typeParameterTree.getBounds(), null);
        if (data != null) return data;
        return search(typeParameterTree.getAnnotations(), null);
    }

    @Override
    public LanguageData visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        LanguageData data = search(instanceOfTree.getExpression(), null);
        if (data != null) return data;
        return search(instanceOfTree.getType(), null);
    }

    @Override
    public LanguageData visitUnary(UnaryTree unaryTree, Void aVoid) {
        return search(unaryTree.getExpression(), null);
    }

    @Override
    public LanguageData visitVariable(VariableTree variableTree, Void aVoid) {
        LanguageData data = checkDefinition(variableTree);
        if (data != null) return data;
        data = search(variableTree.getInitializer(), null);
        if (data != null) return data;
        data = search(variableTree.getType(), null);
        if (data != null) return data;
        return search(variableTree.getNameExpression(), null);
    }

    @Override
    public LanguageData visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        LanguageData data = search(whileLoopTree.getCondition(), null);
        if (data != null) return data;
        return search(whileLoopTree.getStatement(), null);
    }

    @Override
    public LanguageData visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return search(wildcardTree.getBound(), null);
    }

    @Override
    public LanguageData visitOther(Tree tree, Void aVoid) {
        return null;
    }

    private LanguageData search(Tree tree, Void aVoid) {
        if (tree != null) {
            return tree.accept(this, null);
        }
        return null;
    }

    private LanguageData search(List<? extends Tree> treeList, Void aVoid) {
        if (treeList != null) {
            for (Tree tree : treeList) {
                LanguageData result = tree.accept(this, null);
                if (result != null) return result;
            }
        }
        return null;
    }

    private LanguageData getHoverData(Tree tree) {
        if (tree == null) return null;
        JavaSourceRange sourceRange = positionCalculator.getJavaSourceRange(tree);
        TreePath path = trees.getPath(compilationUnit, tree);
        Element element = trees.getElement(path);
        if (element == null || sourceRange.getSize() <= 0) return null;
        LanguageData data = new LanguageData(compilationUnit.getSourceFile().getName(), element, trees.getTypeMirror(path));
        Location location = element.getKind() == ElementKind.CONSTRUCTOR && tree instanceof MethodTree
                ? positionCalculator.getLocation((MethodTree) tree, element.getEnclosingElement().getSimpleName().toString())
                : positionCalculator.getLocation(tree);
        data.setLocation(location);
        data.setDocComment(trees.getDocComment(path));
        return data;
    }

    private LanguageData checkDefinition(Tree candidate) {
        return isCorrectDefinition(candidate) ? getHoverData(candidate) : null;
    }

    private boolean isCorrectDefinition(Tree candidateTree) {

        TreePath candidatePath = trees.getPath(compilationUnit, candidateTree);
        Element candidateElement = trees.getElement(candidatePath);

        if (candidateElement == null || candidatePath == null) return false;

        // Quick filters for simple name and kind
        if (!candidateElement.getSimpleName().toString().equals(referenceElement.getSimpleName().toString())) return false;
        if (!candidateElement.getKind().equals(referenceElement.getKind())) return false;

        // QualifiedNameable elements are things like classes, so we can just compare their qualified names since
        // they're unique.
        if (referenceElement instanceof QualifiedNameable) {
            return ((QualifiedNameable) referenceElement).getQualifiedName().toString().equals(((TypeElement) candidateElement).getQualifiedName().toString());
        }

        // Methods can be generic and overloaded, and reconstructing the generic signature from the call site is a bit
        // tedious, but fortunately the string representations correspond to the generic signature
        if (referenceElement instanceof ExecutableElement) {
            return referenceElement.toString().equals(candidateElement.toString());
        }

        // No more shortcuts to take, so pull out the TypeMirrors and compare them
        String fullRefName = LanguageUtils.getQualifiedName(referenceElement);
        String fullDefName = LanguageUtils.getQualifiedName(candidateElement);
        if (!fullRefName.equals(fullDefName)) return false;

        TypeMirror candidateType = trees.getTypeMirror(candidatePath);
        return referenceType != null
                && candidateType != null
                && referenceType.toString().equals(candidateType.toString());
    }
}
