package com.example.javaparser;

/**
 * Enumeration of supported parser backends for Java AST parsing.
 * Currently supports JavaParser as the primary backend.
 */
public enum ParserBackend {
    JAVAPARSER,     // JavaParser library - primary backend
    JDT             // Eclipse JDT - fallback for complex cases
}
