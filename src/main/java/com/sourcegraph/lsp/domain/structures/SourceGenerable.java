package com.sourcegraph.lsp.domain.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface SourceGenerable {
    /**
     * generateSource returns a string of Java code that would produce an instance of this class that is equivalent to
     * this instance.
     * @param linePrefix is the pretty-print indentation prefix to use in the returned string. The first line should NOT
     *                   apply the prefix (as it is the caller that supplies the prefix for the first line, but
     *                   subsequent lines should include the prefix (along with any additional indentation necessary for
     *                   formatting).
     */
    String generateSource(String linePrefix);

    static String q(String s) {
        return s == null ? "null" : String.format("\"%s\"", s
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\f", "\\f")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
        );
    }

    static String q(SourceGenerable o, String linePrefix) {
        return o == null ? "null" : o.generateSource(linePrefix + "  ");
    }

    static String q(List<? extends SourceGenerable> l, String linePrefix, String classConstructorName) {
        if (l == null) {
            return "null";
        }
        String linePrefix2 = linePrefix + "  ";
        String children = String.join(",\n" + linePrefix2, l.stream().map(c -> c.generateSource(linePrefix2)).collect(Collectors.toList()));
        return String.format("%s(\n%s%s\n%s)", classConstructorName, linePrefix2, children, linePrefix);
    }

    static String q(List<? extends SourceGenerable> l, String linePrefix) {
        return SourceGenerable.q(l, linePrefix, "Lists.newArrayList");
    }

    static String q(Map<String, String> m, String linePrefix) {
        if (m == null) {
            return "null";
        }
        if (m.size() == 0) {
            return "ImmutableMap.of()";
        }
        String linePrefix2 = linePrefix + "  ";
        ArrayList<String> kv = new ArrayList<>();
        for (Map.Entry<String, String> e : m.entrySet()) {
            kv.add(e.getKey());
            kv.add(e.getValue());
        }
        String children = String.join(",\n" + linePrefix2, kv.stream().map(c ->SourceGenerable.q(c)).collect(Collectors.toList()));
        return String.format("ImmutableMap.of(\n%s%s\n%s)", linePrefix2, children, linePrefix);
    }

    static String generateSource(Enum enumInstance) {
        if (enumInstance == null) return "null";
        String className;
        if (enumInstance instanceof PackageIdentifier.Type) {
            className = "PackageIdentifier.Type";
        } else {
            className = enumInstance.getClass().getSimpleName();
        }
        return String.format("%s.%s", className, enumInstance.toString());
    }
}
