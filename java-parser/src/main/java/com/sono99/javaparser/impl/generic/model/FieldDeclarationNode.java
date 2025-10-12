package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import java.util.List;

/**
 * Represents a field declaration in Java source code (e.g., "private String name;"). Includes
 * support for Javadoc comments and annotations. Maintains a reference to the original JavaParser
 * FieldDeclaration for enhanced information access.
 *
 * @since v1.0
 */
public class FieldDeclarationNode extends AbstractJavaElementNode<FieldDeclaration> {

  /** The field name. */
  private final String name;

  /** The field type. */
  private final String type;

  /** Associated Javadoc comment. */
  private final JavadocNode javadoc;

  /**
   * Constructor for FieldDeclarationNode.
   *
   * @param startLine 1-based line number where this field declaration starts
   * @param endLine 1-based line number where this field declaration ends
   * @param name the field name
   * @param type the field type
   * @param javaCodeChunk the field declaration text
   * @param originalNode reference to the original JavaParser FieldDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param children empty for fields
   */
  public FieldDeclarationNode(
      int startLine,
      int endLine,
      String name,
      String type,
      String javaCodeChunk,
      FieldDeclaration originalNode,
      JavadocNode javadoc,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.type = type;
    this.javadoc = javadoc;
  }

  /**
   * Gets the source code chunk including Javadoc and annotations.
   *
   * @return the enriched field declaration with metadata
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

    // Add field declaration
    enriched.append(getJavaChunk());

    return enriched.toString();
  }

  /**
   * Gets the field name.
   *
   * @return the field name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the field type.
   *
   * @return the field type
   */
  public String getType() {
    return type;
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
