package com.sourcegraph.langserver.langservice.workspace;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SourceFile implements WorkspaceFile {

    private static final Logger log = LoggerFactory.getLogger(SourceFile.class);

    private String uri;

    private Path path;

    private String binaryName;

    private String packageName;

    private CharSequence sourceCode;

    public SourceFile(String uri, String binaryName, String content) {
        this.uri = uri;
        this.path = Paths.get(uri);
        this.binaryName = binaryName;
        this.sourceCode = new StringBuilder(content);
        calculatePackageName();
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind()) && (baseName.equals(uri))
                || uri.endsWith("/" + baseName);
    }

    @Override
    public NestingKind getNestingKind() {
        // TODO: something better
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        // TODO: something better
        return null;
    }

    @Override
    public URI toUri() {
        return URI.create(uri);
    }

    @Override
    public String getName() {
        return uri;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new CharSequenceInputStream(sourceCode, StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        StringBuilder builder = new StringBuilder();
        sourceCode = builder;
        return new StringBuilderOutputStream(builder);
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new CharSequenceReader(sourceCode);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return sourceCode;
    }

    @Override
    public Writer openWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        sourceCode = stringWriter.getBuffer();
        return stringWriter;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public boolean delete() {
        return false;
    }

    public String getBinaryName() {
        return binaryName;
    }

    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
        calculatePackageName();
    }

    public String getPackageName() {
        return packageName;
    }

    public Path getSourcePath() {
        return path;
    }

    public class StringBuilderOutputStream extends OutputStream {

        private StringBuilder builder;

        public StringBuilderOutputStream(StringBuilder builder) {
            this.builder = builder;
        }

        public StringBuilder getBuilder() {
            return builder;
        }

        public void write(int character) {
            builder.append(character);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourceFile that = (SourceFile) o;

        return uri != null ? uri.equals(that.uri) : that.uri == null;
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }

    public static String pathToBinaryName(String path) {
        return StringUtils.removeEnd(path.replace('/', '.'), ".java");
    }


    private void calculatePackageName() {
        if (binaryName == null) {
            return;
        }
        int pos = binaryName.lastIndexOf('.');
        this.packageName = pos < 0 ? null : binaryName.substring(0, pos);
    }
}
