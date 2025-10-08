package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.List;

/**
 * Represents a class declaration in Java source code (e.g., "public class Animal { ... }").
 * Includes support for Javadoc comments and annotations. Maintains a reference to the original
 * JavaParser TypeDeclaration for enhanced information access.
 *
 * @since v1.0
 */
public class ClassDeclarationNode extends AbstractJavaElementNode<TypeDeclaration<?>> {

  /** The class name. */
  private final String name;

  /** Associated Javadoc comment. */
  private final JavadocNode javadoc;

  /**
   * Associated annotations. FIXME: POTENTIALLY UNNECESSARY - JavaParser's getRange() for class
   * declarations already includes annotations in the javaCodeChunk. Consider eliminating this field
   * if structured annotation parsing proves unnecessary for LLM code review use cases. Observed
   * with Animal.java: getRange() correctly captures @Entity, @Table, etc.
   */
  private final List<AnnotationNode> annotations;

  /**
   * Constructor for ClassDeclarationNode.
   *
   * @param startLine 1-based line number where this class declaration starts
   * @param endLine 1-based line number where this class declaration ends
   * @param name the class name
   * @param javaCodeChunk the class declaration body
   * @param originalNode reference to the original JavaParser TypeDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param annotations the associated annotations, can be null
   * @param children the child nodes (methods, fields, inner classes)
   */
  public ClassDeclarationNode(
      int startLine,
      int endLine,
      String name,
      String javaCodeChunk,
      TypeDeclaration originalNode,
      JavadocNode javadoc,
      List<AnnotationNode> annotations,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.javadoc = javadoc;
    this.annotations = annotations != null ? annotations : List.of();
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
   * Gets the associated annotations.
   *
   * @return list of annotation nodes, may be empty
   */
  public List<AnnotationNode> getAnnotations() {
    return annotations;
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
