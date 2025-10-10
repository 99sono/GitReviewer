package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.List;

/**
 * Represents a V1 annotation attached to a Java element. Extends AbstractJavaNodeV1 to provide
 * common AST node functionality and ensure proper inheritance hierarchy.
 *
 * @since v1.0
 */
public class AnnotationNodeV1 extends AbstractJavaNodeV1<AnnotationExpr> {

  /** The annotation name (e.g., "@Entity"). */
  private final String name;

  /**
   * Constructor for AnnotationNodeV1.
   *
   * @param startLine 1-based line number where this annotation starts
   * @param endLine 1-based line number where this annotation ends
   * @param name the annotation name
   * @param javaCodeChunk the complete annotation text
   * @param originalNode reference to the original JavaParser AnnotationExpr
   * @param children the child nodes, typically empty for annotations
   */
  public AnnotationNodeV1(
      int startLine,
      int endLine,
      String name,
      String javaCodeChunk,
      AnnotationExpr originalNode,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
  }

  /**
   * Gets the annotation name.
   *
   * @return the annotation name
   */
  public String getName() {
    return name;
  }
}
