package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.List;

/**
 * Represents a method declaration in Java source code (e.g., "public void getName() { ... }").
 * Includes support for Javadoc comments and annotations. Maintains a reference to the original
 * JavaParser MethodDeclaration for enhanced information access.
 *
 * @since v1.0
 */
public class MethodDeclarationNode extends AbstractJavaElementNode<MethodDeclaration> {

  /** The method name. */
  private final String name;

  /** The method signature (without body). */
  private final String signature;

  /** Associated Javadoc comment. */
  private final JavadocNode javadoc;

  /**
   * Constructor for MethodDeclarationNode.
   *
   * @param startLine 1-based line number where this method declaration starts
   * @param endLine 1-based line number where this method declaration ends
   * @param name the method name
   * @param signature the complete method signature
   * @param javaCodeChunk the method implementation
   * @param originalNode reference to the original JavaParser MethodDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param children method body contents
   */
  public MethodDeclarationNode(
      int startLine,
      int endLine,
      String name,
      String signature,
      String javaCodeChunk,
      MethodDeclaration originalNode,
      JavadocNode javadoc,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.signature = signature;
    this.javadoc = javadoc;
  }

  /**
   * Gets the source code chunk including Javadoc and annotations.
   *
   * @return the enriched method declaration with metadata
   */
  @Override
  public String getJavaChunkWithJavadocAndAnnotations() {
    StringBuilder enriched = new StringBuilder();

    // Add Javadoc if present
    if (javadoc != null) {
      enriched.append(javadoc.getJavaChunk()).append("\n");
    }

    // Add annotations if present
    List<AnnotationNode> annotations =
        getChildren().stream()
            .filter(AnnotationNode.class::isInstance)
            .map(AnnotationNode.class::cast)
            .toList();
    if (!annotations.isEmpty()) {
      for (AnnotationNode annotation : annotations) {
        enriched.append(annotation.getJavaChunk()).append("\n");
      }
    }

    // Add method declaration
    enriched.append(getJavaChunk());

    return enriched.toString();
  }

  /**
   * Gets the method name.
   *
   * @return the method name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the method signature (without body).
   *
   * @return the method signature
   */
  public String getSignature() {
    return signature;
  }

  /**
   * Gets the associated Javadoc.
   *
   * @return Javadoc node, or null if none
   */
  public JavadocNode getJavadoc() {
    return javadoc;
  }
}
