package com.sourcegraph.langserver.langservice.compiler;

import com.sourcegraph.utils.LanguageUtils;
import com.sourcegraph.lsp.domain.structures.Hover;
import com.sourcegraph.lsp.domain.structures.Location;
import com.sourcegraph.lsp.domain.structures.MarkedString;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class LanguageData {

    private String fileName;

    private Element element;

    private TypeMirror typeMirror;

    private String docComment;

    private Location location;

    private Signature signature;

    public LanguageData(String fileName, Element element, TypeMirror typeMirror) {
        this.fileName = fileName;
        this.element = element;
        this.typeMirror = typeMirror;
        if (element == null) {
            this.signature = null;
        } else {
            Element topLevelClass = LanguageUtils.getTopLevelClass(element);
            String outermostContainerName = (topLevelClass != null ? topLevelClass.getSimpleName().toString() : null);
            this.signature = Signature.of(
                    element.getKind(),
                    element.getKind() == ElementKind.CONSTRUCTOR ?
                            element.getEnclosingElement().getSimpleName().toString() :
                            element.getSimpleName().toString(),
                    LanguageUtils.getQualifiedName(element),
                    outermostContainerName,
                    LanguageUtils.getPackageName(element)
            );
        }
    }

    public LanguageData(String fileName, Element element, TypeMirror typeMirror, String qualifiedname) {
        this.fileName = fileName;
        this.element = element;
        this.typeMirror = typeMirror;
        if (element == null) {
            this.signature = null;
        } else {
            Element topLevelClass = LanguageUtils.getTopLevelClass(element);
            String outermostContainerName = (topLevelClass != null ? topLevelClass.getSimpleName().toString() : null);
            this.signature = Signature.of(
                    element.getKind(),
                    element.getKind() == ElementKind.CONSTRUCTOR ?
                            element.getEnclosingElement().getSimpleName().toString() :
                            element.getSimpleName().toString(),
                    qualifiedname,
                    outermostContainerName,
                    LanguageUtils.getPackageName(element)
            );
        }
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Element getElement() {
        return element;
    }

    public TypeMirror getTypeMirror() {
        return typeMirror;
    }

    public String getDocComment() {
        return docComment;
    }

    public void setDocComment(String docComment) {
        this.docComment = docComment;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Signature getSignature() {
        return signature;
    }

    public String getElementSignature() {
        return LanguageUtils.getElementSignature(element);
    }

    public Signature getCrossRepoSignature() {
        Element topLevelClass = LanguageUtils.getTopLevelClass(element);
        String outermostContainerName = (topLevelClass != null ? topLevelClass.getSimpleName().toString() : null);
        return Signature.of(
                element.getKind(),
                element.getKind() == ElementKind.CONSTRUCTOR ?
                        element.getEnclosingElement().getSimpleName().toString() :
                        element.getSimpleName().toString(),
                LanguageUtils.getCrossRepoQualifiedName(element),
                outermostContainerName,
                LanguageUtils.getPackageName(element)
        );
    }

    public List<MarkedString> getData() {

        List<MarkedString> result = new ArrayList<>();

        result.add(new MarkedString().withValue(getElementSignature()).withLanguage(Hover.LANGUAGE_JAVA));
        if (docComment != null) result.add(new MarkedString().withValue(docComment).withLanguage(Hover.LANGUAGE_MARKDOWN));
        return result;
    }

    public static class Signature {
        public ElementKind elementKind;
        public String simpleName;
        public String qualifiedName;
        public String outermostContainerName;
        public String packageName;

        public static Signature of(ElementKind elementKind, String simpleName, String qualifiedName, String outermostContainerName, String packageName) {
            return new Signature(elementKind, simpleName, qualifiedName, outermostContainerName, packageName);
        }

        private Signature(ElementKind elementKind, String simpleName, String qualifiedName, String outermostContainerName, String packageName) {
            this.elementKind = elementKind;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.outermostContainerName = outermostContainerName;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return String.join(
                    ":",
                    elementKind.toString(),
                    qualifiedName
            );
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            Signature signature = (Signature) object;

            if (elementKind != signature.elementKind) return false;
            if (simpleName != null ? !simpleName.equals(signature.simpleName) : signature.simpleName != null)
                return false;
            if (qualifiedName != null ? !qualifiedName.equals(signature.qualifiedName) : signature.qualifiedName != null)
                return false;
            if (packageName != null ? !packageName.equals(signature.packageName) : signature.packageName != null)
                return false;
            return outermostContainerName != null ? outermostContainerName.equals(signature.outermostContainerName) : signature.outermostContainerName == null;
        }

        @Override
        public int hashCode() {
            int result = elementKind != null ? elementKind.hashCode() : 0;
            result = 31 * result + (simpleName != null ? simpleName.hashCode() : 0);
            result = 31 * result + (qualifiedName != null ? qualifiedName.hashCode() : 0);
            result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
            result = 31 * result + (outermostContainerName != null ? outermostContainerName.hashCode() : 0);
            return result;
        }
    }
}
