package com.sourcegraph.langserver.langservice.compiler;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An Android .aar file.
 *
 * Created by beyang on 3/23/17.
 */
public class AarFileSource implements JarSource {

    private static final String CLASSES_FILE = "classes.jar";

    private String filename;
    private ZipFile file;
    private List<JarEntry> entries;

    public AarFileSource(String filename) throws IOException {
        this.filename = filename;
        this.file = new ZipFile(filename);
        this.entries = Lists.newArrayList();
            InputStream in = file.getInputStream(new ZipEntry(CLASSES_FILE));
            JarInputStream jarin = new JarInputStream(in);
            JarEntry ent = jarin.getNextJarEntry();
            while (ent != null) {
                entries.add(ent);
                ent = jarin.getNextJarEntry();
            }
    }

    @Override
    public String getFileName() {
        return filename;
    }

    @Override
    public Enumeration<JarEntry> entries() {
        return Collections.enumeration(entries);
    }

    @Override
    public InputStream getInputStream(JarEntry entry) throws IOException {
        InputStream in = file.getInputStream(new ZipEntry(CLASSES_FILE));
        JarInputStream jarin = new JarInputStream(in);
        JarEntry e = jarin.getNextJarEntry();
        while (e != null && !e.getName().equals(entry.getName())) {
            e = jarin.getNextJarEntry();
        }
        if (e == null) {
            return null;
        }
        return jarin;
    }
}
