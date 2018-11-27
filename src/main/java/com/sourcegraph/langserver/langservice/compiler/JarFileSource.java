package com.sourcegraph.langserver.langservice.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A JarSource backed by a regular .jar file.
 *
 * Created by beyang on 3/23/17.
 */
public class JarFileSource implements JarSource {

    private JarFile jarFile;

    public JarFileSource(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public String getFileName() {
        return jarFile.getName();
    }

    @Override
    public Enumeration<JarEntry> entries() {
        return jarFile.entries();
    }

    @Override
    public InputStream getInputStream(JarEntry entry) throws IOException {
        return jarFile.getInputStream(entry);
    }
}
