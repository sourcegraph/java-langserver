package com.sourcegraph.langserver.langservice.compiler;

public class CompilerOption {

    private String name;

    private String value;

    @SuppressWarnings("unused")
    private CompilerOption() {}

    public CompilerOption(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
