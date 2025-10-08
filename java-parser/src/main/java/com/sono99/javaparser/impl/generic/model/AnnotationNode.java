package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.List;

/**
 * Represents an annotation attached to a Java element. Contains the annotation metadata and source
 * location. Maintains a reference to the original JavaParser AnnotationExpr for enhanced
 * information access.
 *
 * @since v1.0
 */
public class AnnotationNode extends AbstractJavaNode<AnnotationExpr> {

  /** The annotation name (e.g., "@Entity"). */
  private final String name;

  /**
   * Constructor for AnnotationNode.
   *
   * @param startLine 1-based line number where this annotation starts
   * @param endLine 1-based line number where this annotation ends
   * @param name the annotation name
   * @param fullText the complete annotation text
   * @param originalNode reference to the original JavaParser AnnotationExpr
   * @param children the child nodes, typically empty for annotations
   */
  public AnnotationNode(
      int startLine,
      int endLine,
      String name,
      String fullText,
      AnnotationExpr originalNode,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, fullText, originalNode, children);
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
