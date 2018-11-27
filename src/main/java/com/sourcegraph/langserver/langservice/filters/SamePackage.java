package com.sourcegraph.langserver.langservice.filters;

import com.sourcegraph.langserver.langservice.workspace.SourceFile;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;
import java.util.function.Predicate;

/**
 * Matches java file objects defined in the same package.
 * For candidates, package is object's binary name till last period (if any)
 * For elements, package is name of package-type element's enclosing element
 */
class SamePackage implements Predicate<JavaFileObject> {

    private String packageName;

    SamePackage (Element element) {
        this.packageName = getPackageName(element);
    }
    @Override
    public boolean test(JavaFileObject candidate) {
        return StringUtils.equals(packageName, getPackageName(candidate));
    }

    private String getPackageName(JavaFileObject source) {
        if (!(source instanceof SourceFile)) {
            return null;
        }
        return ((SourceFile) source).getPackageName();
    }

    private String getPackageName(Element element) {
        if (element == null) {
            return null;
        }
        ElementKind kind = element.getKind();
        if (kind == ElementKind.PACKAGE) {
            return element.toString();
        }
        return getPackageName(element.getEnclosingElement());
    }
}
