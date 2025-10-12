package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.List;

/**
 * Represents a V1 type declaration in Java source code, encompassing class, interface, and record
 * declarations. Extends AbstractJavaNodeV1 to provide common AST node functionality and ensure
 * proper inheritance hierarchy. Includes support for Javadoc comments and annotations.
 *
 * @since v1.0
 */
public class TypeDeclarationNodeV1 extends AbstractJavaNodeV1<TypeDeclaration<?>> {

  /** The class name. */
  private final String name;

  /** Associated Javadoc comment. */
  private final JavadocNodeV1 javadoc;

  /**
   * Constructor for TypeDeclarationNodeV1.
   *
   * @param startLine 1-based line number where this class declaration starts
   * @param endLine 1-based line number where this class declaration ends
   * @param name the class name
   * @param javaCodeChunk the class declaration body
   * @param originalNode reference to the original JavaParser TypeDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param children the child nodes (methods, fields, inner classes)
   */
  public TypeDeclarationNodeV1(
      int startLine,
      int endLine,
      String name,
      String javaCodeChunk,
      TypeDeclaration<?> originalNode,
      JavadocNodeV1 javadoc,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
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
  public JavadocNodeV1 getJavadoc() {
    return javadoc;
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
