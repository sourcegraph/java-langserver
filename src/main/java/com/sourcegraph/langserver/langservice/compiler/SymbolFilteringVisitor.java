package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.lsp.domain.structures.SymbolDescriptor;
import com.sun.source.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import java.util.List;

public class SymbolFilteringVisitor implements TreeVisitor<Boolean, Void> {

    private static Logger log = LoggerFactory.getLogger(SymbolFilteringVisitor.class);

    private SymbolDescriptor querySymbol;

    private boolean exactMatch;

    public SymbolFilteringVisitor(SymbolDescriptor querySymbol, boolean exactMatch) {
        this.querySymbol = querySymbol;
        this.exactMatch = exactMatch;
    }

    @Override
    public Boolean visitAnnotatedType(AnnotatedTypeTree annotatedTypeTree, Void aVoid) {
        return search(annotatedTypeTree.getUnderlyingType());
    }

    @Override
    public Boolean visitAnnotation(AnnotationTree annotationTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitAssert(AssertTree assertTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitBinary(BinaryTree binaryTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitBlock(BlockTree blockTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitBreak(BreakTree breakTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitCase(CaseTree caseTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitCatch(CatchTree catchTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitClass(ClassTree classTree, Void aVoid) {
        ElementKind queryKind = querySymbol.getElementKind();
        if (queryKind == null
                || queryKind == ElementKind.CLASS
                || queryKind == ElementKind.INTERFACE
                || queryKind == ElementKind.ENUM
                || queryKind == ElementKind.ANNOTATION_TYPE
                || queryKind == ElementKind.CONSTRUCTOR) {
            return compareNames(classTree.getSimpleName()) || search(classTree.getMembers());
        } else {
            return search(classTree.getMembers());
        }
    }

    @Override
    public Boolean visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitContinue(ContinueTree continueTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitErroneous(ErroneousTree erroneousTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitExpressionStatement(ExpressionStatementTree expressionStatementTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitForLoop(ForLoopTree forLoopTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitIf(IfTree ifTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitImport(ImportTree importTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitArrayAccess(ArrayAccessTree arrayAccessTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitLabeledStatement(LabeledStatementTree labeledStatementTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitLiteral(LiteralTree literalTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMethod(MethodTree methodTree, Void aVoid) {
        ElementKind queryKind = querySymbol.getElementKind();
        if (queryKind == null || queryKind == ElementKind.METHOD || queryKind == ElementKind.CONSTRUCTOR) {
            return compareNames(methodTree.getName());
        } else {
            return false;
        }
    }

    @Override
    public Boolean visitModifiers(ModifiersTree modifiersTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitNewArray(NewArrayTree newArrayTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitNewClass(NewClassTree newClassTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitParenthesized(ParenthesizedTree parenthesizedTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitReturn(ReturnTree returnTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMemberSelect(MemberSelectTree memberSelectTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMemberReference(MemberReferenceTree memberReferenceTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitEmptyStatement(EmptyStatementTree emptyStatementTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitSwitch(SwitchTree switchTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitSynchronized(SynchronizedTree synchronizedTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitThrow(ThrowTree throwTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void aVoid) {
        String thisPackageName = compilationUnitTree.getPackageName() == null
                ? ""
                : compilationUnitTree.getPackageName().toString();
        String queryPackageName = querySymbol.getPackageName();
        if (queryPackageName == null || thisPackageName.equals(queryPackageName)) {
            return search(compilationUnitTree.getTypeDecls());
        } else {
            return false;
        }
    }

    @Override
    public Boolean visitTry(TryTree tryTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitUnionType(UnionTypeTree unionTypeTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitIntersectionType(IntersectionTypeTree intersectionTypeTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitArrayType(ArrayTypeTree arrayTypeTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitTypeCast(TypeCastTree typeCastTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitTypeParameter(TypeParameterTree typeParameterTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitUnary(UnaryTree unaryTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitVariable(VariableTree variableTree, Void aVoid) {
        ElementKind queryKind = querySymbol.getElementKind();
        if (queryKind == null || queryKind == ElementKind.FIELD || queryKind == ElementKind.ENUM_CONSTANT) {
            return compareNames(variableTree.getName());
        } else {
            return false;
        }
    }

    @Override
    public Boolean visitWhileLoop(WhileLoopTree whileLoopTree, Void aVoid) {
        return null;
    }

    @Override
    public Boolean visitWildcard(WildcardTree wildcardTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitOther(Tree tree, Void aVoid) {
        return false;
    }

    private Boolean compareNames(Name name) {
        return exactMatch
                ? name.toString().equals(querySymbol.getSimpleName())
                : name.toString().toLowerCase().contains(querySymbol.getSimpleName().toLowerCase());
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
