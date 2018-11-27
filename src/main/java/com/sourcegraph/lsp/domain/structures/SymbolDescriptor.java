package com.sourcegraph.lsp.domain.structures;

import com.sourcegraph.langserver.langservice.compiler.LanguageData;
import com.sourcegraph.utils.LanguageUtils;

import javax.lang.model.element.ElementKind;
import java.util.Objects;

public class SymbolDescriptor implements SourceGenerable {

    public static SymbolDescriptor of(LanguageData ld, PackageIdentifier pkg) {
        return SymbolDescriptor.of(
                ld.getElement().getKind(),
                ld.getElement().getSimpleName().toString(),
                LanguageUtils.getQualifiedName(ld.getElement()),
                LanguageUtils.getTopLevelClass(ld.getElement()).getSimpleName().toString(),
                LanguageUtils.getPackageName(ld.getElement()),
                pkg
        );
    }

    public static SymbolDescriptor of(ElementKind elementKind, String simpleName, String qualifiedName, String outermostContainerName, String packageName, PackageIdentifier pkg) {
        SymbolDescriptor s = new SymbolDescriptor();
        s.setElementKind(elementKind);
        s.setSimpleName(simpleName);
        s.setQualifiedName(qualifiedName);
        s.setOutermostContainerName(outermostContainerName);
        s.setPackageName(packageName);
        s.setPackage(pkg);
        return s;
    }

    public static SymbolDescriptor of(LanguageData.Signature s, PackageIdentifier pkg) {
        return SymbolDescriptor.of(
                s.elementKind,
                s.simpleName,
                s.qualifiedName,
                s.outermostContainerName,
                s.packageName,
                pkg
        );
    }

    private ElementKind elementKind;
    private String simpleName;
    private String qualifiedName;
    private String outermostContainerName; // the name of the outermost class that encloses this symbol
    private String packageName; // the Java package name
    private PackageIdentifier pkg; // descriptor for the build package

    public ElementKind getElementKind() {
        return elementKind;
    }

    public LanguageData.Signature toSignature() {
        return LanguageData.Signature.of(
                elementKind,
                simpleName,
                qualifiedName,
                outermostContainerName,
                packageName
        );
    }

    public void setElementKind(ElementKind elementKind) {
        this.elementKind = elementKind;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getOutermostContainerName() {
        return outermostContainerName;
    }

    public void setOutermostContainerName(String outermostContainerName) {
        this.outermostContainerName = outermostContainerName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public PackageIdentifier getPackage() {
        return pkg;
    }

    public void setPackage(PackageIdentifier pkg) {
        this.pkg = pkg;
    }

    /**
     * queryMatches treats `this` as a symbol query and returns true if and only if `s` matches the query.
     */
    public boolean queryMatches(SymbolDescriptor s) {
        if (this.elementKind != null && !Objects.equals(this.elementKind, s.elementKind)) {
            return false;
        }
        if (this.simpleName != null && !Objects.equals(this.simpleName, s.simpleName)) {
            return false;
        }
        if (this.qualifiedName != null && !Objects.equals(this.qualifiedName, s.qualifiedName)) {
            return false;
        }
        if (this.outermostContainerName != null && !Objects.equals(this.outermostContainerName, s.outermostContainerName)) {
            return false;
        }
        if (this.packageName != null && !Objects.equals(this.packageName, s.packageName)) {
            return false;
        }
        if (this.pkg != null && !this.pkg.queryMatches(s.pkg)) {
            return false;
        }
        return true;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format(
                "%s.of(%s, %s, %s, %s, %s, %s)",
                getClass().getSimpleName(),
                SourceGenerable.generateSource(elementKind),
                SourceGenerable.q(simpleName),
                SourceGenerable.q(qualifiedName),
                SourceGenerable.q(outermostContainerName),
                SourceGenerable.q(packageName),
                SourceGenerable.q(pkg, linePrefix)
        );
    }
}
