package com.sourcegraph.langserver.langservice.workspace;

import com.sourcegraph.langserver.langservice.compiler.JarSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;

/**
 * Intended for storing classes that have been extracted from dependency jars.
 */
public class JarEntryFile implements WorkspaceFile {

    private static final Logger log = LoggerFactory.getLogger(JarEntryFile.class);

    private String name;

    private Path path;

    private String binaryName;

    private JarSource sourceFile;

    private JarEntry jarEntry;

    public JarEntryFile(String name, JarSource sourceFile, JarEntry jarEntry) {
        this.name = name;
        this.path = Paths.get(name);
        this.binaryName = computeBinaryName(name);
        this.sourceFile = sourceFile;
        this.jarEntry = jarEntry;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind()) && (baseName.equals(name))
                || name.endsWith("/" + baseName);
    }

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return null;
    }

    @Override
    public URI toUri() {
        return URI.create(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return sourceFile.getInputStream(jarEntry);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(openInputStream(), StandardCharsets.ISO_8859_1);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new IllegalStateException();
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

    public Path getSourcePath() {
        return path;
    }

    public String getJarName() {
        return sourceFile.getFileName();
    }

    private static String computeBinaryName(String name) {
        return name.replace('/', '.').replaceAll("\\.class\\Z", "");
    }
}
