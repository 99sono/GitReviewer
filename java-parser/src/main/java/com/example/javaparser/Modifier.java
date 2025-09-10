package com.example.javaparser;

/**
 * Enumeration of Java modifiers that can be applied to classes, methods, and fields.
 * Represents the standard Java access and non-access modifiers.
 */
public enum Modifier {
    PUBLIC,         // public access modifier
    PRIVATE,        // private access modifier
    PROTECTED,      // protected access modifier
    PACKAGE_PRIVATE, // default/package-private access (no modifier)

    STATIC,         // static modifier
    FINAL,          // final modifier
    ABSTRACT,       // abstract modifier
    SYNCHRONIZED,   // synchronized modifier
    VOLATILE,       // volatile modifier
    TRANSIENT,      // transient modifier
    NATIVE,         // native modifier
    STRICTFP        // strictfp modifier
}
