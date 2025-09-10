package com.example.javaparser;

/**
 * Enumeration of all supported syntactic node kinds in the Java AST.
 * These represent the different types of Java language constructs that
 * can be parsed and analyzed.
 */
public enum NodeKind {
    FILE,           // Entire source file
    PACKAGE,        // Package declaration
    IMPORT,         // Import statement
    CLASS,          // Class declaration
    INTERFACE,      // Interface declaration
    ENUM,           // Enum declaration
    METHOD,         // Method declaration
    CONSTRUCTOR,    // Constructor declaration
    FIELD,          // Field/variable declaration
    PARAMETER,      // Method/constructor parameter
    JAVADOC,        // Javadoc comment
    ANNOTATION,     // Annotation usage
    UNKNOWN         // Fallback for unsupported constructs
}
