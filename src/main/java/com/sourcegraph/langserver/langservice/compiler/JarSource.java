package com.sourcegraph.langserver.langservice.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A wrapper around any resource that provides a source of jar entries.
 *
 * Created by beyang on 3/23/17.
 */
public interface JarSource {
    static JarSource fromFileOrNull(String filename) {
        try {
            return JarSource.fromFile(filename);
        } catch (IOException e) {
            return null;
        }
    }

    static JarSource fromFile(String filename) throws IOException {
        Path filepath = Paths.get(filename);
        if (Files.notExists(filepath)) {
            return null;
        }

        if (filename.endsWith(".jar")) {
            JarFile jarFile = new JarFile(filepath.toFile());
            return new JarFileSource(jarFile);
        } else if (filename.endsWith(".aar")) {
            return new AarFileSource(filename);
        }
        return null;
    }

    String getFileName();
    Enumeration<JarEntry> entries();
    InputStream getInputStream(JarEntry entry) throws IOException;
}
