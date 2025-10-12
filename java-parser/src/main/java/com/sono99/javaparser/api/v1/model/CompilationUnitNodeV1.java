package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.CompilationUnit;
import java.util.List;

/**
 * Represents a V1 root node for a Java compilation unit (a source file). Extends AbstractJavaNodeV1
 * to provide common AST node functionality and ensure proper inheritance hierarchy.
 *
 * @since v1.0
 */
public class CompilationUnitNodeV1 extends AbstractJavaNodeV1<CompilationUnit> {

  /** Source file path in repository. */
  private final String repositoryPath;

  /**
   * Constructor for CompilationUnitNodeV1.
   *
   * @param startLine 1-based line number where this compilation unit starts
   * @param endLine 1-based line number where this compilation unit ends
   * @param javaCodeChunk the full source code of the Java file
   * @param originalNode reference to the original JavaParser CompilationUnit
   * @param repositoryPath path to the source file in repository
   * @param children the child nodes forming the subtree, typically classes/packages/imports
   */
  public CompilationUnitNodeV1(
      int startLine,
      int endLine,
      String javaCodeChunk,
      CompilationUnit originalNode,
      String repositoryPath,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.repositoryPath = repositoryPath;
  }

  /**
   * Gets the repository path for this compilation unit.
   *
   * @return the source file path in repository
   */
  public String getRepositoryPath() {
    return repositoryPath;
  }

  /**
   * Gets the package declaration (first child if present).
   *
   * @return package declaration, or null for default package
   */
  public PackageDeclarationNodeV1 getPackageDeclaration() {
    return getChildren().stream()
        .filter(PackageDeclarationNodeV1.class::isInstance)
        .map(PackageDeclarationNodeV1.class::cast)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets all import declarations.
   *
   * @return list of import declaration nodes
   */
  public List<ImportDeclarationNodeV1> getImportDeclarations() {
    return getChildren().stream()
        .filter(ImportDeclarationNodeV1.class::isInstance)
        .map(ImportDeclarationNodeV1.class::cast)
        .toList();
  }

  /**
   * Gets all class declarations.
   *
   * @return list of class declaration nodes
   */
  public List<TypeDeclarationNodeV1> getClassDeclarations() {
    return getChildren().stream()
        .filter(TypeDeclarationNodeV1.class::isInstance)
        .map(TypeDeclarationNodeV1.class::cast)
        .toList();
  }
}
