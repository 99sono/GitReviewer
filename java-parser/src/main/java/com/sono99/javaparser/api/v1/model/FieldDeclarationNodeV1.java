package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import java.util.List;

/**
 * Represents a V1 field declaration in Java source code. Extends AbstractJavaNodeV1 to provide
 * common AST node functionality and ensure proper inheritance hierarchy. Includes support for
 * Javadoc comments and annotations.
 *
 * @since v1.0
 */
public class FieldDeclarationNodeV1 extends AbstractJavaNodeV1<FieldDeclaration> {

  /** The field name. */
  private final String name;

  /** The field type. */
  private final String type;

  /** Associated Javadoc comment. */
  private final JavadocNodeV1 javadoc;

  /** Associated annotations. */
  private final List<AnnotationNodeV1> annotations;

  /**
   * Constructor for FieldDeclarationNodeV1.
   *
   * @param startLine 1-based line number where this field declaration starts
   * @param endLine 1-based line number where this field declaration ends
   * @param name the field name
   * @param type the field type
   * @param javaCodeChunk the field declaration text
   * @param originalNode reference to the original JavaParser FieldDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param annotations the associated annotations, can be null
   * @param children empty for fields
   */
  public FieldDeclarationNodeV1(
      int startLine,
      int endLine,
      String name,
      String type,
      String javaCodeChunk,
      FieldDeclaration originalNode,
      JavadocNodeV1 javadoc,
      List<AnnotationNodeV1> annotations,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.type = type;
    this.javadoc = javadoc;
    this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
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
  public JavadocNodeV1 getJavadoc() {
    return javadoc;
  }

  /**
   * Gets the associated annotations.
   *
   * @return list of annotation nodes, may be empty
   */
  public List<AnnotationNodeV1> getAnnotations() {
    return annotations;
  }
}
