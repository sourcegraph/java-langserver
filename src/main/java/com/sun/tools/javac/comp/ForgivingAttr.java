package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.TypeTag.NONE;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

/**
 * When parse tree is really messed up,
 * issues warnings rather than throwing AssertError
 */
public class ForgivingAttr extends Attr {

    private static final Logger LOG = Logger.getLogger("main");
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ForgivingAttr.class);

    protected ForgivingAttr(Context context) {
        super(context);
    }

    public static ForgivingAttr instance(Context context) {
        ForgivingAttr attr = null;
        try {
            if (context.get(attrKey) != null) {
                System.out.println("CONTEXT ALREADY CONTAINS ATTR, BUT WHATEVER");
            }
            attr = new ForgivingAttr(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attr;
    }

    @Override
    public void validateTypeAnnotations(JCTree tree, boolean sigOnly) {
        tree.accept(new TypeAnnotationsValidator(sigOnly));
    }

    //where
    private final class TypeAnnotationsValidator extends TreeScanner {

        private final boolean sigOnly;
        public TypeAnnotationsValidator(boolean sigOnly) {
            this.sigOnly = sigOnly;
        }

        public void visitAnnotation(JCTree.JCAnnotation tree) {
            chk.validateTypeAnnotation(tree, false);
            super.visitAnnotation(tree);
        }
        public void visitAnnotatedType(JCTree.JCAnnotatedType tree) {
            if (!tree.underlyingType.type.isErroneous()) {
                super.visitAnnotatedType(tree);
            }
        }
        public void visitTypeParameter(JCTree.JCTypeParameter tree) {
            chk.validateTypeAnnotations(tree.annotations, true);
            scan(tree.bounds);
            // Don't call super.
            // This is needed because above we call validateTypeAnnotation with
            // false, which would forbid annotations on type parameters.
            // super.visitTypeParameter(tree);
        }
        public void visitMethodDef(JCTree.JCMethodDecl tree) {
            if (tree.recvparam != null &&
                !tree.recvparam.vartype.type.isErroneous()) {
                checkForDeclarationAnnotations(tree.recvparam.mods.annotations,
                                               tree.recvparam.vartype.type.tsym);
            }
            if (tree.restype != null && tree.restype.type != null) {
                validateAnnotatedType(tree.restype, tree.restype.type);
            }
            if (sigOnly) {
                scan(tree.mods);
                scan(tree.restype);
                scan(tree.typarams);
                scan(tree.recvparam);
                scan(tree.params);
                scan(tree.thrown);
            } else {
                scan(tree.defaultValue);
                scan(tree.body);
            }
        }
        public void visitVarDef(final JCTree.JCVariableDecl tree) {
            if (tree.sym != null && tree.sym.type != null)
                validateAnnotatedType(tree.vartype, tree.sym.type);
            scan(tree.mods);
            scan(tree.vartype);
            if (!sigOnly) {
                scan(tree.init);
            }
        }
        public void visitTypeCast(JCTree.JCTypeCast tree) {
            if (tree.clazz != null && tree.clazz.type != null)
                validateAnnotatedType(tree.clazz, tree.clazz.type);
            super.visitTypeCast(tree);
        }
        public void visitTypeTest(JCTree.JCInstanceOf tree) {
            if (tree.clazz != null && tree.clazz.type != null)
                validateAnnotatedType(tree.clazz, tree.clazz.type);
            super.visitTypeTest(tree);
        }
        public void visitNewClass(JCTree.JCNewClass tree) {
            if (tree.clazz.type != null) {
                if (tree.clazz.hasTag(ANNOTATED_TYPE)) {
                    checkForDeclarationAnnotations(((JCTree.JCAnnotatedType) tree.clazz).annotations,
                            tree.clazz.type.tsym);
                }
                if (tree.def != null) {
                    checkForDeclarationAnnotations(tree.def.mods.annotations, tree.clazz.type.tsym);
                }
                validateAnnotatedType(tree.clazz, tree.clazz.type);
            }
            super.visitNewClass(tree);
        }
        public void visitNewArray(JCTree.JCNewArray tree) {
            if (tree.elemtype != null && tree.elemtype.type != null) {
                if (tree.elemtype.hasTag(ANNOTATED_TYPE)) {
                    checkForDeclarationAnnotations(((JCTree.JCAnnotatedType) tree.elemtype).annotations,
                                                   tree.elemtype.type.tsym);
                }
                validateAnnotatedType(tree.elemtype, tree.elemtype.type);
            }
            super.visitNewArray(tree);
        }
        public void visitClassDef(JCTree.JCClassDecl tree) {
            if (sigOnly) {
                scan(tree.mods);
                scan(tree.typarams);
                scan(tree.extending);
                scan(tree.implementing);
            }
            for (JCTree member : tree.defs) {
                if (member.hasTag(JCTree.Tag.CLASSDEF)) {
                    continue;
                }
                scan(member);
            }
        }
        public void visitBlock(JCTree.JCBlock tree) {
            if (!sigOnly) {
                scan(tree.stats);
            }
        }

        /* I would want to model this after
         * com.sun.tools.javac.comp.Check.Validator.visitSelectInternal(JCFieldAccess)
         * and override visitSelect and visitTypeApply.
         * However, we only set the annotated type in the top-level type
         * of the symbol.
         * Therefore, we need to override each individual location where a type
         * can occur.
         */
        private void validateAnnotatedType(final JCTree errtree, final Type type) {
//             System.out.println("Attr.validateAnnotatedType: " + errtree + " type: " + type);

            if (errtree == null || type == null) {
                logger.warn("Encountered nulls while validating annotated type: {} {}", errtree, type);
                return;
            }

            if (type.isPrimitiveOrVoid()) {
                return;
            }

            JCTree enclTr = errtree;
            Type enclTy = type;

            boolean repeat = true;
            while (repeat) {
                if (enclTr.hasTag(TYPEAPPLY)) {
                    List<Type> tyargs = enclTy.getTypeArguments();
                    List<JCTree.JCExpression> trargs = ((JCTree.JCTypeApply)enclTr).getTypeArguments();
                    if (trargs.length() > 0) {
                        // Nothing to do for diamonds
                        if (tyargs.length() == trargs.length()) {
                            for (int i = 0; i < tyargs.length(); ++i) {
                                validateAnnotatedType(trargs.get(i), tyargs.get(i));
                            }
                        }
                        // If the lengths don't match, it's either a diamond
                        // or some nested type that redundantly provides
                        // type arguments in the tree.
                    }

                    // Look at the clazz part of a generic type
                    enclTr = ((JCTree.JCTypeApply)enclTr).clazz;
                }

                if (enclTr.hasTag(SELECT)) {
                    enclTr = ((JCTree.JCFieldAccess)enclTr).getExpression();
                    if (enclTy != null &&
                        !enclTy.hasTag(NONE)) {
                        enclTy = enclTy.getEnclosingType();
                    }
                } else if (enclTr.hasTag(ANNOTATED_TYPE)) {
                    JCTree.JCAnnotatedType at = (JCTree.JCAnnotatedType) enclTr;
                    if (enclTy == null ||
                        enclTy.hasTag(NONE)) {
                        if (at.getAnnotations().size() == 1) {
                            log.error(at.underlyingType.pos(), "cant.type.annotate.scoping.1", at.getAnnotations().head.attribute);
                        } else {
                            ListBuffer<Attribute.Compound> comps = new ListBuffer<Attribute.Compound>();
                            for (JCTree.JCAnnotation an : at.getAnnotations()) {
                                comps.add(an.attribute);
                            }
                            log.error(at.underlyingType.pos(), "cant.type.annotate.scoping", comps.toList());
                        }
                        repeat = false;
                    }
                    enclTr = at.underlyingType;
                    // enclTy doesn't need to be changed
                } else if (enclTr.hasTag(IDENT)) {
                    repeat = false;
                } else if (enclTr.hasTag(JCTree.Tag.WILDCARD)) {
                    JCTree.JCWildcard wc = (JCTree.JCWildcard) enclTr;
                    if (wc.getKind() == JCTree.Kind.EXTENDS_WILDCARD) {
                        validateAnnotatedType(wc.getBound(), ((Type.WildcardType)enclTy.unannotatedType()).getExtendsBound());
                    } else if (wc.getKind() == JCTree.Kind.SUPER_WILDCARD) {
                        validateAnnotatedType(wc.getBound(), ((Type.WildcardType)enclTy.unannotatedType()).getSuperBound());
                    } else {
                        // Nothing to do for UNBOUND
                    }
                    repeat = false;
                } else if (enclTr.hasTag(TYPEARRAY)) {
                    JCTree.JCArrayTypeTree art = (JCTree.JCArrayTypeTree) enclTr;
                    validateAnnotatedType(art.getType(), ((Type.ArrayType)enclTy.unannotatedType()).getComponentType());
                    repeat = false;
                } else if (enclTr.hasTag(TYPEUNION)) {
                    JCTree.JCTypeUnion ut = (JCTree.JCTypeUnion) enclTr;
                    for (JCTree t : ut.getTypeAlternatives()) {
                        validateAnnotatedType(t, t.type);
                    }
                    repeat = false;
                } else if (enclTr.hasTag(TYPEINTERSECTION)) {
                    JCTree.JCTypeIntersection it = (JCTree.JCTypeIntersection) enclTr;
                    for (JCTree t : it.getBounds()) {
                        validateAnnotatedType(t, t.type);
                    }
                    repeat = false;
                } else if (enclTr.getKind() == JCTree.Kind.PRIMITIVE_TYPE ||
                           enclTr.getKind() == JCTree.Kind.ERRONEOUS) {
                    repeat = false;
                } else {
                    // This only happens when there are parse errors
                    LOG.warning("Unexpected tree: " + enclTr + " with kind: " + enclTr.getKind() +
                                " within: " + errtree + " with kind: " + errtree.getKind());
                    repeat = false;
                }
            }
        }

        private void checkForDeclarationAnnotations(List<? extends JCTree.JCAnnotation> annotations,
                                                    Symbol sym) {
            // Ensure that no declaration annotations are present.
            // Note that a tree type might be an AnnotatedType with
            // empty annotations, if only declaration annotations were given.
            // This method will raise an error for such a type.
            for (JCTree.JCAnnotation ai : annotations) {
                if (!ai.type.isErroneous() &&
                    typeAnnotations.annotationType(ai.attribute, sym) == TypeAnnotations.AnnotationType.DECLARATION) {
                    log.error(ai.pos(), "annotation.type.not.applicable");
                }
            }
        }
    };
}
