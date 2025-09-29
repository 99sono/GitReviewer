package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.Node;
import java.util.List;

/**
 * Abstract base class for all nodes in the Java parsing tree. Provides common properties like line
 * ranges and basic tree traversal. Maintains a reference to the original JavaParser AST node for
 * enhanced information access.
 *
 * @param <T> the type of the original JavaParser AST node, extending com.github.javaparser.ast.Node
 * @since v1.0
 */
public abstract class AbstractJavaNode<T extends Node> {

  /** 1-based line number where this node starts. */
  protected final int startLine;

  /** 1-based line number where this node ends (inclusive). */
  protected final int endLine;

  /** The source code chunk for this node. */
  protected final String javaCodeChunk;

  /** Reference to the original JavaParser AST node for enhanced information access. */
  protected final T originalNode;

  /** Parent node in the AST hierarchy - mutable for tree construction, null for root nodes. */
  protected AbstractJavaNode<? extends Node> parent;

  /** Child nodes forming the subtree, immutable. */
  protected final List<AbstractJavaNode<? extends Node>> children;

  /**
   * Constructor for AbstractNode.
   *
   * @param startLine 1-based line number where this node starts
   * @param endLine 1-based line number where this node ends
   * @param javaCodeChunk the source code chunk for this node
   * @param originalNode reference to the original JavaParser AST node
   * @param children the child nodes forming the subtree, can be empty
   */
  protected AbstractJavaNode(
      int startLine,
      int endLine,
      String javaCodeChunk,
      T originalNode,
      List<AbstractJavaNode<? extends Node>> children) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.javaCodeChunk = javaCodeChunk;
    this.originalNode = originalNode;
    this.parent = null; // Will be set later via setParent()
    this.children = children != null ? List.copyOf(children) : List.of();
  }

  /**
   * Sets the parent node reference. Protected to ensure only package-internal tree construction
   * code can modify parent relationships after node creation. While classes strive for
   * immutability, parent references must be mutable because AST trees are built bottom-up - the
   * root node can only be created when all children exist, then parent links are established as a
   * final step.
   *
   * @param parent the parent node in the AST hierarchy
   */
  public void setParent(AbstractJavaNode<? extends Node> parent) {
    this.parent = parent;
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
   * Gets the parent node in the AST hierarchy.
   *
   * @return parent node, or null for root nodes
   */
  public AbstractJavaNode getParent() {
    return parent;
  }

  /**
   * Gets the child nodes forming the subtree.
   *
   * @return unmodifiable list of child nodes, may be empty
   */
  public List<AbstractJavaNode<? extends Node>> getChildren() {
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
