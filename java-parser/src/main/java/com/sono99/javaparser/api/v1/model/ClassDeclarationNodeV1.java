package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.List;

/**
 * Represents a V1 class declaration in Java source code. Extends AbstractJavaNodeV1 to provide
 * common AST node functionality and ensure proper inheritance hierarchy. Includes support for
 * Javadoc comments and annotations.
 *
 * @since v1.0
 */
public class ClassDeclarationNodeV1 extends AbstractJavaNodeV1<TypeDeclaration<?>> {

  /** The class name. */
  private final String name;

  /** Associated Javadoc comment. */
  private final JavadocNodeV1 javadoc;

  /** Associated annotations. */
  private final List<AnnotationNodeV1> annotations;

  /**
   * Constructor for ClassDeclarationNodeV1.
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
  public ClassDeclarationNodeV1(
      int startLine,
      int endLine,
      String name,
      String javaCodeChunk,
      TypeDeclaration<?> originalNode,
      JavadocNodeV1 javadoc,
      List<AnnotationNodeV1> annotations,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.javadoc = javadoc;
    this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
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

  /**
   * Gets all field declarations within this class.
   *
   * @return a list of field declaration nodes
   */
  public List<FieldDeclarationNodeV1> getFieldDeclarations() {
    return getChildren().stream()
        .filter(FieldDeclarationNodeV1.class::isInstance)
        .map(FieldDeclarationNodeV1.class::cast)
        .toList();
  }

  /**
   * Gets all method declarations within this class.
   *
   * @return a list of method declaration nodes
   */
  public List<MethodDeclarationNodeV1> getMethodDeclarations() {
    return getChildren().stream()
        .filter(MethodDeclarationNodeV1.class::isInstance)
        .map(MethodDeclarationNodeV1.class::cast)
        .toList();
  }
}
