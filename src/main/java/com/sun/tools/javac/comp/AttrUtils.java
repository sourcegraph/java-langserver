package com.sun.tools.javac.comp;

public class AttrUtils {
    public static boolean isStatic(AttrContext context) {
        return context.staticLevel > 0;
    }
}
