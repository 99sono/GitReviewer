package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.comments.JavadocComment;
import java.util.List;

/**
 * Represents a V1 Javadoc comment block. This is an immutable record.
 *
 * @param startLine 1-based line number where this Javadoc starts
 * @param endLine 1-based line number where this Javadoc ends
 * @param javaCodeChunk the complete Javadoc text with delimiters
 * @param originalNode reference to the original JavaParser JavadocComment
 * @param children should be empty, will be ignored for Javadoc nodes
 * @since v1.0
 */
public record JavadocNodeV1(
    int startLine,
    int endLine,
    String javaCodeChunk,
    JavadocComment originalNode,
    List<AbstractJavaNodeV1<?>> children) {

  /**
   * Canonical constructor for JavadocNodeV1. Ensures that the children list is always empty, as
   * Javadoc nodes do not have children in this model.
   */
  public JavadocNodeV1 {
    children = List.of(); // Javadoc nodes have no children
  }
}
