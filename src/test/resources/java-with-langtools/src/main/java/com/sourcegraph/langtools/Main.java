package com.sourcegraph.langtools;

import com.sun.source.tree.*;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;

public class Main implements TreeVisitor<Void, Void>  {

    public Void visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        CompilationUnitTree root = null;
        Trees trees = null;
        Element element = trees.getElement(trees.getPath(root, annotatedTypeTree));
        return null;
    }

    public Void visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        return null;
    }

    public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        return null;
    }

    public Void visitAssert(AssertTree assertTree, Void aVoid) {
        return null;
    }

    public Void visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        return null;
    }

    public Void visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        return null;
    }

    public Void visitBinary(BinaryTree binaryTree, Void aVoid) {
        return null;
    }

    public Void visitBlock(BlockTree blockTree, Void aVoid) {
        return null;
    }

    public Void visitBreak(BreakTree breakTree, Void aVoid) {
        return null;
    }

    public Void visitCase(CaseTree caseTree, Void aVoid) {
        return null;
    }

    public Void visitCatch(CatchTree catchTree, Void aVoid) {
        return null;
    }

    public Void visitClass(ClassTree classTree, Void aVoid) {
        return null;
    }

    public Void visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        return null;
    }

    public Void visitContinue(ContinueTree continueTree, Void aVoid) {
        return null;
    }

    public Void visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        return null;
    }

    public Void visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return null;
    }

    public Void visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return null;
    }

    public Void visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        return null;
    }

    public Void visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        return null;
    }

    public Void visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return null;
    }

    public Void visitIf(IfTree ifTree, Void aVoid) {
        return null;
    }

    public Void visitImport(ImportTree importTree, Void aVoid) {
        return null;
    }

    public Void visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        return null;
    }

    public Void visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return null;
    }

    public Void visitLiteral(LiteralTree literalTree, Void aVoid) {
        return null;
    }

    public Void visitMethod(MethodTree methodTree, Void aVoid) {
        return null;
    }

    public Void visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return null;
    }

    public Void visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        return null;
    }

    public Void visitNewClass(NewClassTree newClassTree, Void aVoid) {
        return null;
    }

    public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return null;
    }

    public Void visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return null;
    }

    public Void visitReturn(ReturnTree returnTree, Void aVoid) {
        return null;
    }

    public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return null;
    }

    public Void visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        return null;
    }

    public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return null;
    }

    public Void visitSwitch(SwitchTree switchTree, Void aVoid) {
        return null;
    }

    public Void visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        return null;
    }

    public Void visitThrow(ThrowTree throwTree, Void aVoid) {
        return null;
    }

    public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        return null;
    }

    public Void visitTry(TryTree tryTree, Void aVoid) {
        return null;
    }

    public Void visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        return null;
    }

    public Void visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return null;
    }

    public Void visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return null;
    }

    public Void visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return null;
    }

    public Void visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        return null;
    }

    public Void visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        return null;
    }

    public Void visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        return null;
    }

    public Void visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        return null;
    }

    public Void visitUnary(UnaryTree unaryTree, Void aVoid) {
        return null;
    }

    public Void visitVariable(VariableTree variableTree, Void aVoid) {
        return null;
    }

    public Void visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        return null;
    }

    public Void visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return null;
    }

    public Void visitOther(Tree tree, Void aVoid) {
        return null;
    }
}
