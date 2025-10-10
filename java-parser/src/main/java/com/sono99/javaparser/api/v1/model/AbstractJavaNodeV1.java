package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.Node;
import java.util.List;

/**
 * Abstract base class for all nodes in the V1 Java parsing tree. Provides common properties like
 * line ranges and basic tree traversal. Maintains a reference to the original JavaParser AST node
 * for enhanced information access.
 *
 * @param <T> the type of the original JavaParser AST node, extending com.github.javaparser.ast.Node
 * @since v1.0
 */
public abstract class AbstractJavaNodeV1<T extends Node> {

  /** 1-based line number where this node starts. */
  protected final int startLine;

  /** 1-based line number where this node ends (inclusive). */
  protected final int endLine;

  /** The source code chunk for this node. */
  protected final String javaCodeChunk;

  /** Reference to the original JavaParser AST node for enhanced information access. */
  protected final T originalNode;

  /** Child nodes forming the subtree, immutable. */
  protected final List<AbstractJavaNodeV1<? extends Node>> children;

  /**
   * Constructor for AbstractNodeV1.
   *
   * @param startLine 1-based line number where this node starts
   * @param endLine 1-based line number where this node ends
   * @param javaCodeChunk the source code chunk for this node
   * @param originalNode reference to the original JavaParser AST node
   * @param children the child nodes forming the subtree, can be empty
   */
  protected AbstractJavaNodeV1(
      int startLine,
      int endLine,
      String javaCodeChunk,
      T originalNode,
      List<AbstractJavaNodeV1<? extends Node>> children) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.javaCodeChunk = javaCodeChunk;
    this.originalNode = originalNode;
    this.children = children != null ? List.copyOf(children) : List.of();
  }

  /**
   * Gets the source code chunk represented by this node.
   *
   * @return the source code from startLine to endLine
   */
  public String getJavaChunk() {
    return javaCodeChunk;
  }

  /**
   * Gets the 1-based line number where this node starts.
   *
   * @return start line number
   */
  public int getStartLine() {
    return startLine;
  }

  /**
   * Gets the 1-based line number where this node ends (inclusive).
   *
   * @return end line number
   */
  public int getEndLine() {
    return endLine;
  }

  /**
   * Gets the child nodes forming the subtree.
   *
   * @return unmodifiable list of child nodes, may be empty
   */
  public List<AbstractJavaNodeV1<? extends Node>> getChildren() {
    return children;
  }

  /**
   * Gets the reference to the original JavaParser AST node. This allows access to additional AST
   * information when needed.
   *
   * @return the original JavaParser AST node
   */
  public T getOriginalNode() {
    return originalNode;
  }
}
