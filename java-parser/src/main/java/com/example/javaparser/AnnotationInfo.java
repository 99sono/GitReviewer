package com.example.javaparser;

import java.util.Map;

/**
 * Data structure representing annotation information extracted from Java source code.
 * Contains the annotation name and its parameter values.
 */
public class AnnotationInfo {

    public String name;                    // Annotation name (e.g., "Override", "Deprecated")
    public Map<String, String> values;     // Key-value pairs of annotation parameters

    /**
     * Creates a simple marker annotation with no parameters.
     *
     * @param name The annotation name
     * @return AnnotationInfo for marker annotation
     */
    public static AnnotationInfo marker(String name) {
        // TODO: Implement marker annotation creation
        return null;
    }

    /**
     * Creates a single-value annotation.
     *
     * @param name The annotation name
     * @param value The single parameter value
     * @return AnnotationInfo for single-value annotation
     */
    public static AnnotationInfo singleValue(String name, String value) {
        // TODO: Implement single-value annotation creation
        return null;
    }

    /**
     * Creates a multi-value annotation with named parameters.
     *
     * @param name The annotation name
     * @param values Map of parameter names to values
     * @return AnnotationInfo for multi-value annotation
     */
    public static AnnotationInfo multiValue(String name, Map<String, String> values) {
        // TODO: Implement multi-value annotation creation
        return null;
    }

    // TODO: Add methods for annotation value formatting
    // TODO: Add validation methods for annotation syntax
}
