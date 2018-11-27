package com.sourcegraph.langserver.langservice.compiler;

import com.sun.source.tree.*;
import com.sun.source.util.Trees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import java.util.Collection;
import java.util.List;

public class StringFilteringVisitor implements TreeVisitor<Boolean, Void> {

    private static Logger log = LoggerFactory.getLogger(StringFilteringVisitor.class);

    private String queryName;
    private String secondaryQueryName;

    private CompilationUnitTree compilationUnitTree;

    private Trees trees;

    private boolean exactMatch;

    public StringFilteringVisitor(Element queryElement, CompilationUnitTree compilationUnitTree, Trees trees, boolean exactMatch) {
        this.queryName = queryElement.getSimpleName().toString().toLowerCase();
        if (queryElement.getKind() == ElementKind.CONSTRUCTOR) {
            this.secondaryQueryName = queryElement.getEnclosingElement().getSimpleName().toString().toLowerCase();
        }
        this.compilationUnitTree = compilationUnitTree;
        this.trees = trees;
        this.exactMatch = exactMatch;
    }

    public StringFilteringVisitor(String queryName, CompilationUnitTree compilationUnitTree, Trees trees, boolean exactMatch) {
        this.queryName = queryName.toLowerCase();
        this.compilationUnitTree = compilationUnitTree;
        this.trees = trees;
        this.exactMatch = exactMatch;
    }

    @Override
    public Boolean visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        return search(annotatedTypeTree.getAnnotations()) || search(annotatedTypeTree.getUnderlyingType());
    }

    @Override
    public Boolean visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        return search(annotationTree.getAnnotationType()) || search(annotationTree.getArguments());
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        return search(methodInvocationTree.getArguments())
                || search(methodInvocationTree.getMethodSelect())
                || search(methodInvocationTree.getTypeArguments());
    }

    @Override
    public Boolean visitAssert(AssertTree assertTree, Void aVoid) {
        return search(assertTree.getCondition()) || search(assertTree.getDetail());
    }

    @Override
    public Boolean visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        return search(assignmentTree.getVariable()) || search(assignmentTree.getExpression());
    }

    @Override
    public Boolean visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        return search(compoundAssignmentTree.getVariable()) || search(compoundAssignmentTree.getExpression());
    }

    @Override
    public Boolean visitBinary(BinaryTree binaryTree, Void aVoid) {
        return search(binaryTree.getLeftOperand()) || search(binaryTree.getRightOperand());
    }

    @Override
    public Boolean visitBlock(BlockTree blockTree, Void aVoid) {
        return search(blockTree.getStatements());
    }

    @Override
    public Boolean visitBreak(BreakTree breakTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitCase(CaseTree caseTree, Void aVoid) {
        return search(caseTree.getExpression()) || search(caseTree.getStatements());
    }

    @Override
    public Boolean visitCatch(CatchTree catchTree, Void aVoid) {
        return search(catchTree.getParameter()) || search(catchTree.getBlock());
    }

    @Override
    public Boolean visitClass(ClassTree classTree, Void aVoid) {
        return compareNames(classTree.getSimpleName())
                || checkAnnotation(classTree)
                || search(classTree.getTypeParameters())
                || search(classTree.getMembers())
                || search(classTree.getImplementsClause())
                || search(classTree.getExtendsClause());
    }

    @Override
    public Boolean visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        return search(conditionalExpressionTree.getCondition())
                || search(conditionalExpressionTree.getTrueExpression())
                || search(conditionalExpressionTree.getFalseExpression());
    }

    @Override
    public Boolean visitContinue(ContinueTree continueTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        return search(doWhileLoopTree.getCondition()) || search(doWhileLoopTree.getStatement());
    }

    @Override
    public Boolean visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return search(expressionStatementTree.getExpression());
    }

    @Override
    public Boolean visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        return search(enhancedForLoopTree.getVariable())
                || search(enhancedForLoopTree.getExpression())
                || search(enhancedForLoopTree.getStatement());
    }

    @Override
    public Boolean visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        return search(forLoopTree.getInitializer())
                || search(forLoopTree.getCondition())
                || search(forLoopTree.getUpdate())
                || search(forLoopTree.getStatement());
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return compareNames(identifierTree.getName());
    }

    @Override
    public Boolean visitIf(IfTree ifTree, Void aVoid) {
        return search(ifTree.getCondition()) || search(ifTree.getThenStatement()) || search(ifTree.getElseStatement());
    }

    @Override
    public Boolean visitImport(ImportTree importTree, Void aVoid) {
        return search(importTree.getQualifiedIdentifier());
    }

    @Override
    public Boolean visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        return search(arrayAccessTree.getExpression()) || search(arrayAccessTree.getIndex());
    }

    @Override
    public Boolean visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return compareNames(labeledStatementTree.getLabel()) || search(labeledStatementTree.getStatement());
    }

    @Override
    public Boolean visitLiteral(LiteralTree literalTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMethod(MethodTree methodTree, Void aVoid) {
        return compareNames(methodTree.getName())
                || checkAnnotation(methodTree)
                || search(methodTree.getTypeParameters())
                || search(methodTree.getParameters())
                || search(methodTree.getThrows())
                || search(methodTree.getBody())
                || search(methodTree.getReturnType())
                || search(methodTree.getReceiverParameter());
    }

    @Override
    public Boolean visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        return search(newArrayTree.getInitializers())
                || search(newArrayTree.getType())
                || search(newArrayTree.getAnnotations())
                || search(newArrayTree.getDimensions());
    }

    @Override
    public Boolean visitNewClass(NewClassTree newClassTree, Void aVoid) {
        return search(newClassTree.getIdentifier())
                || search(newClassTree.getArguments())
                || search(newClassTree.getTypeArguments())
                || search(newClassTree.getClassBody());
    }

    @Override
    public Boolean visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return search(lambdaExpressionTree.getParameters()) || search(lambdaExpressionTree.getBody());
    }

    @Override
    public Boolean visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return search(parenthesizedTree.getExpression());
    }

    @Override
    public Boolean visitReturn(ReturnTree returnTree, Void aVoid) {
        return search(returnTree.getExpression());
    }

    @Override
    public Boolean visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return compareNames(memberSelectTree.getIdentifier()) || search(memberSelectTree.getExpression());
    }

    @Override
    public Boolean visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        return compareNames(memberReferenceTree.getName()) || search(memberReferenceTree.getQualifierExpression()) || search(memberReferenceTree.getTypeArguments());
    }

    @Override
    public Boolean visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitSwitch(SwitchTree switchTree, Void aVoid) {
        return search(switchTree.getExpression()) || search(switchTree.getCases());
    }

    @Override
    public Boolean visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        return search(synchronizedTree.getExpression()) || search(synchronizedTree.getBlock());
    }

    @Override
    public Boolean visitThrow(ThrowTree throwTree, Void aVoid) {
        return search(throwTree.getExpression());
    }

    @Override
    public Boolean visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        return search(compilationUnitTree.getPackageName())
                || search(compilationUnitTree.getImports())
                || search(compilationUnitTree.getTypeDecls())
                || search(compilationUnitTree.getPackageAnnotations());
    }

    @Override
    public Boolean visitTry(TryTree tryTree, Void aVoid) {
        return search(tryTree.getBlock())
                || search(tryTree.getResources())
                || search(tryTree.getCatches())
                || search(tryTree.getFinallyBlock());
    }

    @Override
    public Boolean visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        return search(parameterizedTypeTree.getType()) || search(parameterizedTypeTree.getTypeArguments());
    }

    @Override
    public Boolean visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return search(unionTypeTree.getTypeAlternatives());
    }

    @Override
    public Boolean visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return search(intersectionTypeTree.getBounds());
    }

    @Override
    public Boolean visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return search(arrayTypeTree.getType());
    }

    @Override
    public Boolean visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        return search(typeCastTree.getType()) || search(typeCastTree.getExpression());
    }

    @Override
    public Boolean visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        // TODO: compare type names
        return false;
    }

    @Override
    public Boolean visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        return compareNames(typeParameterTree.getName())
                || checkAnnotation(typeParameterTree)
                || search(typeParameterTree.getAnnotations())
                || search(typeParameterTree.getBounds());
    }

    @Override
    public Boolean visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        return search(instanceOfTree.getExpression()) || search(instanceOfTree.getType());
    }

    @Override
    public Boolean visitUnary(UnaryTree unaryTree, Void aVoid) {
        return search(unaryTree.getExpression());
    }

    @Override
    public Boolean visitVariable(VariableTree variableTree, Void aVoid) {
        return compareNames(variableTree.getName())
                || checkAnnotation(variableTree)
                || search(variableTree.getType())
                || search(variableTree.getNameExpression())
                || search(variableTree.getInitializer());
    }

    @Override
    public Boolean visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        return search(whileLoopTree.getCondition()) || search(whileLoopTree.getStatement());
    }

    @Override
    public Boolean visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return search(wildcardTree.getBound());
    }

    @Override
    public Boolean visitOther(Tree tree, Void aVoid) {
        return false;
    }

    private Boolean checkAnnotation(Tree tree) {
        Collection<? extends AnnotationTree> annotations;
        if (tree instanceof MethodTree) {
            MethodTree decl = (MethodTree) tree;
            annotations = decl.getModifiers().getAnnotations();
        } else if (tree instanceof ClassTree) {
            ClassTree decl = (ClassTree) tree;
            annotations = decl.getModifiers().getAnnotations();
        } else {
            return false;
        }
        for (AnnotationTree annotation : annotations) {
            if (search(annotation)) {
                return true;
            }
        }
        return false;
    }

    private Element getElement(Tree tree) {
        return trees.getElement(trees.getPath(compilationUnitTree, tree));
    }

    private Boolean compareNames(Name name) {
        if (name == null) {
            return Boolean.FALSE;
        }
        String n = name.toString().toLowerCase();
        return exactMatch
                ? n.equals(queryName) || secondaryQueryName != null && n.equals(secondaryQueryName)
                : n.contains(queryName) || secondaryQueryName != null && n.contains(secondaryQueryName);
    }

    private Boolean search(Tree tree) {
        if (tree != null) {
            return tree.accept(this, null);
        }
        return false;
    }

    private Boolean search(List<? extends Tree> treeList) {
        if (treeList != null) {
            for (Tree tree : treeList) {
                if (tree.accept(this, null)) return true;
            }
        }
        return false;
    }
}
