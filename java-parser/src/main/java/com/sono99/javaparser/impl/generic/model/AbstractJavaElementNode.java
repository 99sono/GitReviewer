package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.Node;
import java.util.List;

/**
 * Abstract base class for Java language elements that support Javadoc and annotations.
 *
 * <p>This class is extended by nodes representing declarations like classes, methods, and fields
 * where Javadoc comments and annotations are commonly expected, even if not always present.
 * Examples include public API methods, class declarations, and public constants.
 *
 * <p>Provides abstract method {@link #getJavaChunkWithJavadocAndAnnotations()} for combining the
 * core element source code with any associated documentation and annotations.
 *
 * @param <T> the type of the original JavaParser AST node, extending com.github.javaparser.ast.Node
 * @since v1.0
 */
public abstract class AbstractJavaElementNode<T extends Node> extends AbstractJavaNode<T> {

  /**
   * Constructor for JavaElementNode.
   *
   * @param startLine 1-based line number where this node starts
   * @param endLine 1-based line number where this node ends
   * @param javaCodeChunk the source code chunk for this node
   * @param originalNode reference to the original JavaParser AST node
   * @param children the child nodes forming the subtree, can be empty
   */
  protected AbstractJavaElementNode(
      int startLine,
      int endLine,
      String javaCodeChunk,
      T originalNode,
      List<AbstractJavaNode<? extends Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
  }

  /**
   * Gets the source code chunk including Javadoc and annotations for this element. This includes
   * the node itself plus any preceding metadata.
   *
   * @return the enriched source code chunk
   */
  public abstract String getJavaChunkWithJavadocAndAnnotations();
}
