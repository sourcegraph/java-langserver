package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.lsp.domain.structures.Location;
import com.sourcegraph.utils.LanguageUtils;
import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import java.util.ArrayList;
import java.util.List;

public class ReferenceSignatureScanner extends TreeScanner<Void, Void> {

    private Trees trees;

    private CompilationUnitTree compilationUnit;

    private PositionCalculator positionCalculator;

    private LanguageData.Signature querySignature;

    private List<LanguageData> references;

    public ReferenceSignatureScanner(Trees trees, CompilationUnitTree compilationUnit, LanguageData.Signature querySignature) {
        this.trees = trees;
        this.compilationUnit = compilationUnit;
        SourcePositions sourcePositions = trees.getSourcePositions();
        this.positionCalculator = new PositionCalculator(sourcePositions, compilationUnit);
        this.querySignature = querySignature;
        this.references = new ArrayList<>();
    }

    public List<LanguageData> getReferences() {
        return references;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        super.visitCompilationUnit(compilationUnitTree, aVoid);
        return null;
    }

    @Override
    public Void visitImport(ImportTree importTree, Void aVoid) {
        super.visitImport(importTree, aVoid);
        return null;
    }

    @Override
    public Void visitClass(ClassTree classTree, Void aVoid) {
        if (compareNames(classTree.getSimpleName())) addReferenceMaybe(classTree);
        super.visitClass(classTree, aVoid);
        return null;
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Void aVoid) {
        Name methodName = methodTree.getName();
        if (compareNames(methodName) || "<init>".equals(methodName.toString())) addReferenceMaybe(methodTree);
        super.visitMethod(methodTree, aVoid);
        return null;
    }

    @Override
    public Void visitVariable(VariableTree variableTree, Void aVoid) {
        if (compareNames(variableTree.getName())) addReferenceMaybe(variableTree);
        super.visitVariable(variableTree, aVoid);
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        super.visitEmptyStatement(emptyStatementTree, aVoid);
        return null;
    }

    @Override
    public Void visitBlock(BlockTree blockTree, Void aVoid) {
        super.visitBlock(blockTree, aVoid);
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        super.visitDoWhileLoop(doWhileLoopTree, aVoid);
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        super.visitWhileLoop(whileLoopTree, aVoid);
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        super.visitForLoop(forLoopTree, aVoid);
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        super.visitEnhancedForLoop(enhancedForLoopTree, aVoid);
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        if (compareNames(labeledStatementTree.getLabel())) addReferenceMaybe(labeledStatementTree);
        super.visitLabeledStatement(labeledStatementTree, aVoid);
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree switchTree, Void aVoid) {
        super.visitSwitch(switchTree, aVoid);
        return null;
    }

    @Override
    public Void visitCase(CaseTree caseTree, Void aVoid) {
        super.visitCase(caseTree, aVoid);
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        super.visitSynchronized(synchronizedTree, aVoid);
        return null;
    }

    @Override
    public Void visitTry(TryTree tryTree, Void aVoid) {
        super.visitTry(tryTree, aVoid);
        return null;
    }

    @Override
    public Void visitCatch(CatchTree catchTree, Void aVoid) {
        super.visitCatch(catchTree, aVoid);
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        super.visitConditionalExpression(conditionalExpressionTree, aVoid);
        return null;
    }

    @Override
    public Void visitIf(IfTree ifTree, Void aVoid) {
        super.visitIf(ifTree, aVoid);
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        super.visitExpressionStatement(expressionStatementTree, aVoid);
        return null;
    }

    @Override
    public Void visitBreak(BreakTree breakTree, Void aVoid) {
        super.visitBreak(breakTree, aVoid);
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree continueTree, Void aVoid) {
        super.visitContinue(continueTree, aVoid);
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree returnTree, Void aVoid) {
        super.visitReturn(returnTree, aVoid);
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree throwTree, Void aVoid) {
        super.visitThrow(throwTree, aVoid);
        return null;
    }

    @Override
    public Void visitAssert(AssertTree assertTree, Void aVoid) {
        super.visitAssert(assertTree, aVoid);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        super.visitMethodInvocation(methodInvocationTree, aVoid);
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree newClassTree, Void aVoid) {
        if (querySignature.simpleName.equals(getClassIdentifier(newClassTree.getIdentifier().toString()))) {
            addReferenceMaybe(newClassTree);
        }
        super.visitNewClass(newClassTree, aVoid);
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        super.visitNewArray(newArrayTree, aVoid);
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        super.visitLambdaExpression(lambdaExpressionTree, aVoid);
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        super.visitParenthesized(parenthesizedTree, aVoid);
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        super.visitAssignment(assignmentTree, aVoid);
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        super.visitCompoundAssignment(compoundAssignmentTree, aVoid);
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree unaryTree, Void aVoid) {
        super.visitUnary(unaryTree, aVoid);
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree binaryTree, Void aVoid) {
        super.visitBinary(binaryTree, aVoid);
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        super.visitTypeCast(typeCastTree, aVoid);
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        super.visitInstanceOf(instanceOfTree, aVoid);
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        super.visitArrayAccess(arrayAccessTree, aVoid);
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        if (compareNames(memberSelectTree.getIdentifier())) addReferenceMaybe(memberSelectTree);
        super.visitMemberSelect(memberSelectTree, aVoid);
        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        if (compareNames(memberReferenceTree.getName())) addReferenceMaybe(memberReferenceTree);
        super.visitMemberReference(memberReferenceTree, aVoid);
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        if (compareNames(identifierTree.getName())) addReferenceMaybe(identifierTree);
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree literalTree, Void aVoid) {
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        super.visitPrimitiveType(primitiveTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        super.visitArrayType(arrayTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        super.visitParameterizedType(parameterizedTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        super.visitUnionType(unionTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        super.visitIntersectionType(intersectionTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        if (compareNames(typeParameterTree.getName())) addReferenceMaybe(typeParameterTree);
        super.visitTypeParameter(typeParameterTree, aVoid);
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        super.visitWildcard(wildcardTree, aVoid);
        return null;
    }

    @Override
    public Void visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        super.visitModifiers(modifiersTree, aVoid);
        return null;
    }

    @Override
    public Void visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        super.visitAnnotation(annotationTree, aVoid);
        return null;
    }

    @Override
    public Void visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        super.visitAnnotatedType(annotatedTypeTree, aVoid);
        return null;
    }

    @Override
    public Void visitOther(Tree tree, Void aVoid) {
        super.visitOther(tree, aVoid);
        return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        super.visitErroneous(erroneousTree, aVoid);
        return null;
    }

    private void addReferenceMaybe(Tree candidate) {

        LanguageData annotationData = checkAnnotations(candidate);
        if (annotationData != null) {
            references.add(annotationData);
            return;
        }

        TreePath candidatePath = trees.getPath(compilationUnit, candidate);
        if (candidatePath == null) {
            return;
        }

        Element candidateElement = trees.getElement(candidatePath);
        if (candidateElement == null) {
            return;
        }

        if (querySignature.elementKind != null && candidateElement.getKind() != querySignature.elementKind) {
            return;
        }

        String candidatePackageName = LanguageUtils.getPackageName(candidateElement);
        if (querySignature.packageName != null && !querySignature.packageName.equals(candidatePackageName)) {
            return;
        }

        String candidateQualifiedName = LanguageUtils.getQualifiedName(candidateElement);
        if (querySignature.qualifiedName != null && !candidateQualifiedName.equals(querySignature.qualifiedName)) {
            return;
        }

        LanguageData data = new LanguageData(
                compilationUnit.getSourceFile().getName(),
                candidateElement,
                trees.getTypeMirror(candidatePath),
                candidateQualifiedName
        );

        Element element = getElement(candidate);
        Location location = element.getKind() == ElementKind.CONSTRUCTOR && candidate instanceof MethodTree
                ? positionCalculator.getLocation((MethodTree) candidate, element.getEnclosingElement().getSimpleName().toString())
                : positionCalculator.getLocation(candidate);

        data.setLocation(location);
        data.setDocComment(trees.getDocComment(candidatePath));

        references.add(data);
    }

    private LanguageData checkAnnotations(Tree candidateTree) {

        if (querySignature.elementKind != ElementKind.ANNOTATION_TYPE) return null;

        Element candidateElement = getElement(candidateTree);
        if (candidateElement == null) return null;

        List<? extends AnnotationMirror> annotationMirrors = candidateElement.getAnnotationMirrors();
        if (annotationMirrors == null) return null;

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (annotationElement == null) continue;
            if (annotationElement.toString().equals(querySignature.qualifiedName)) {
                Tree annotationTree = trees.getTree(candidateElement, annotationMirror);
                if (annotationTree == null) continue;

                LanguageData data = new LanguageData(
                        compilationUnit.getSourceFile().getName(),
                        getElement(annotationTree),
                        annotationMirror.getAnnotationType(),
                        annotationElement.toString()
                );
                data.setLocation(positionCalculator.getLocation(annotationTree));
                return data;
            }
        }
        return null;
    }

    private boolean compareNames(Name name) {
        String n = name.toString();
        return querySignature.simpleName.equals(n) || "this".equals(n);
    }

    private Element getElement(Tree tree) {
        return trees.getElement(trees.getPath(compilationUnit, tree));
    }

    /**
     * @param nameExpr class name expression
     * @return class name stripped from type parameters if any
     */
    private static String getClassIdentifier(String nameExpr) {
        int pos = nameExpr.indexOf('<');
        if (pos < 0) {
            return nameExpr;
        }
        return nameExpr.substring(0, pos).trim();
    }


}
