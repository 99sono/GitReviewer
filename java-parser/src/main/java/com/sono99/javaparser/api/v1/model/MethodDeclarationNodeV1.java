package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.List;

/**
 * Represents a V1 method declaration in Java source code. Extends AbstractJavaNodeV1 to provide
 * common AST node functionality and ensure proper inheritance hierarchy. Includes support for
 * Javadoc comments and annotations.
 *
 * @since v1.0
 */
public class MethodDeclarationNodeV1 extends AbstractJavaNodeV1<MethodDeclaration> {

  /** The method name. */
  private final String name;

  /** The complete method signature. */
  private final String signature;

  /** Associated Javadoc comment. */
  private final JavadocNodeV1 javadoc;

  /** Associated annotations. */
  private final List<AnnotationNodeV1> annotations;

  /**
   * Constructor for MethodDeclarationNodeV1.
   *
   * @param startLine 1-based line number where this method declaration starts
   * @param endLine 1-based line number where this method declaration ends
   * @param name the method name
   * @param signature the complete method signature
   * @param javaCodeChunk the method implementation
   * @param originalNode reference to the original JavaParser MethodDeclaration
   * @param javadoc the associated Javadoc comment, can be null
   * @param annotations the associated annotations, can be null
   * @param children method body contents
   */
  public MethodDeclarationNodeV1(
      int startLine,
      int endLine,
      String name,
      String signature,
      String javaCodeChunk,
      MethodDeclaration originalNode,
      JavadocNodeV1 javadoc,
      List<AnnotationNodeV1> annotations,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.name = name;
    this.signature = signature;
    this.javadoc = javadoc;
    this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
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
   * Gets the complete method signature.
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
