package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.List;

/**
 * Represents a type declaration in Java source code, encompassing class, interface, and record
 * declarations. (e.g., "public class Animal { ... }", "public interface MyInterface { ... }",
 * "public record MyRecord(...) { ... }"). Includes support for Javadoc comments and annotations.
 * Maintains a reference to the original JavaParser TypeDeclaration for enhanced information access.
 *
 * @since v1.0
 */
public class TypeDeclarationNode extends AbstractJavaElementNode<TypeDeclaration<?>> {

  /** The class name. */
  private final String name;

  /** Associated Javadoc comment. */
  private final JavadocNode javadoc;

  /**
   * Constructor for TypeDeclarationNode.
   *
   * @param startLine 1-based line number where this class declaration starts
   * @param endLine 1-based line number where this class declaration ends
   * @param name the class name
   * @param javaCodeChunk the class declaration body
   * @param originalNode reference to the original github JavaParser TypeDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param children the child nodes (methods, fields, inner classes)
   */
  public TypeDeclarationNode(
      int startLine,
      int endLine,
      String name,
      String javaCodeChunk,
      TypeDeclaration<?> originalNode,
      JavadocNode javadoc,
      List<AbstractJavaNode<? extends Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.javadoc = javadoc;
  }

  /**
   * Gets the class name.
   *
   * @return the class name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the associated Javadoc.
   *
   * @return Javadoc node, or null if none
   */
  public JavadocNode getJavadoc() {
    return javadoc;
  }

  /**
   * Gets the source code chunk including Javadoc and annotations.
   *
   * @return the enriched class declaration with metadata
   */
  @Override
  public String getJavaChunkWithJavadocAndAnnotations() {
    StringBuilder enriched = new StringBuilder();

    // Add Javadoc if present
    if (javadoc != null) {
      enriched.append(javadoc.getJavaChunk()).append("\n\n");
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
      enriched.append("\n");
    }

    // Add class declaration
    enriched.append(getJavaChunk());

    return enriched.toString();
  }
}
