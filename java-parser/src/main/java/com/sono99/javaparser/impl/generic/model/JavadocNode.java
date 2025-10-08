package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.comments.JavadocComment;
import java.util.List;

/**
 * Represents a Javadoc comment block attached to a Java element. Contains the comment text and
 * source location. Maintains a reference to the original JavaParser JavadocComment for enhanced
 * information access.
 *
 * @since v1.0
 */
public class JavadocNode extends AbstractJavaNode<JavadocComment> {

  /**
   * Constructor for JavadocNode. Javadoc nodes cannot have children - they represent metadata only.
   *
   * @param startLine 1-based line number where this Javadoc starts
   * @param endLine 1-based line number where this Javadoc ends
   * @param rawContent the complete Javadoc text with delimiters
   * @param originalNode reference to the original JavaParser JavadocComment
   * @param children should be null or empty, will be ignored for Javadoc nodes
   */
  public JavadocNode(
      int startLine,
      int endLine,
      String rawContent,
      JavadocComment originalNode,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, rawContent, originalNode, List.of()); // Always use empty children
  }
}
