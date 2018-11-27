package com.sourcegraph.langserver.langservice.filters;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utilities to create filters used to restrict number of files to scan while looking for references
 */
public class ReferenceFilterUtils {

    /**
     * Visibility scope
     */
    private enum Scope {
        GLOBAL,
        PACKAGE,
        FILE
    }

    /**
     * @param element element references to we are searching
     * @param fileName file where given element is defined
     * @return predicate to filter java file objects when searching for element's references
     */
    public static Predicate<JavaFileObject> getFilter(Element element, String fileName) {
        Scope scope = referencesScope(element);
        switch (scope) {
            case FILE:
                return new SameFile(fileName);
            case PACKAGE:
                return new SamePackage(element);
            default:
                return __ -> true;
        }
    }

    /**
     * @param element element references for we are searching
     * @return element's visibility scope
     */
    private static Scope referencesScope(Element element) {
        if (element == null) {
            return Scope.GLOBAL;
        }
        ElementKind kind = element.getKind();
        if (kind.isInterface() ||
                kind.isClass() ||
                kind.isField() ||
                kind == ElementKind.METHOD ||
                kind == ElementKind.CONSTRUCTOR) {
            // protected element may be visible globally if enclosing element has global visibility scope
            Scope modifiers = referencesScope(element.getModifiers());
            Scope parent = referencesScope(element.getEnclosingElement());
            return modifiers.ordinal() > parent.ordinal() ? modifiers : parent;
        }
        if (kind == ElementKind.PACKAGE) {
            return Scope.GLOBAL;
        } else if (kind == ElementKind.LOCAL_VARIABLE ||
                kind == ElementKind.EXCEPTION_PARAMETER ||
                kind == ElementKind.TYPE_PARAMETER ||
                kind == ElementKind.PARAMETER) {
            return Scope.FILE;
        } else return referencesScope(element.getEnclosingElement());
    }

    /**
     * @param modifiers element's modifiers
     * @return element's visibility scope based solely on its modifiers
     */
    private static Scope referencesScope(Set<Modifier> modifiers) {
        if (modifiers == null) {
            return Scope.PACKAGE;
        }
        if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED)) {
            return Scope.GLOBAL;
        }
        if (modifiers.contains(Modifier.PRIVATE)) {
            return Scope.FILE;
        }
        return Scope.PACKAGE;
    }

}
