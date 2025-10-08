package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.CompilationUnit;
import java.util.List;

/**
 * Root node representing an entire Java compilation unit (source file). Contains package
 * declarations, imports, and top-level type declarations. Maintains a reference to the original
 * JavaParser CompilationUnit for enhanced information access.
 *
 * @since v1.0
 */
public class CompilationUnitNode extends AbstractJavaNode<CompilationUnit> {

  /** Source file path in repository. */
  private final String repositoryPath;

  /**
   * Constructor for CompilationUnitNode.
   *
   * @param startLine 1-based line number where this compilation unit starts
   * @param endLine 1-based line number where this compilation unit ends
   * @param javaCodeChunk the full source code of the Java file
   * @param originalNode reference to the original JavaParser CompilationUnit
   * @param repositoryPath path to the source file in repository
   * @param children the child nodes forming the subtree, typically classes/packages/imports
   */
  public CompilationUnitNode(
      int startLine,
      int endLine,
      String javaCodeChunk,
      CompilationUnit originalNode,
      String repositoryPath,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
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
  public PackageDeclarationNode getPackageDeclaration() {
    return getChildren().stream()
        .filter(PackageDeclarationNode.class::isInstance)
        .map(PackageDeclarationNode.class::cast)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets all import declarations.
   *
   * @return list of import declaration nodes
   */
  public List<ImportDeclarationNode> getImportDeclarations() {
    return getChildren().stream()
        .filter(ImportDeclarationNode.class::isInstance)
        .map(ImportDeclarationNode.class::cast)
        .toList();
  }

  /**
   * Gets all class declarations.
   *
   * @return list of class declaration nodes
   */
  public List<ClassDeclarationNode> getClassDeclarations() {
    return getChildren().stream()
        .filter(ClassDeclarationNode.class::isInstance)
        .map(ClassDeclarationNode.class::cast)
        .toList();
  }

  /**
   * Gets all field declarations in all classes.
   *
   * @return list of field declaration nodes
   */
  public List<FieldDeclarationNode> getFieldDeclarations() {
    return getChildren().stream()
        .filter(ClassDeclarationNode.class::isInstance)
        .map(ClassDeclarationNode.class::cast)
        .flatMap(cls -> cls.getChildren().stream())
        .filter(FieldDeclarationNode.class::isInstance)
        .map(FieldDeclarationNode.class::cast)
        .toList();
  }

  /**
   * Gets all method declarations in all classes.
   *
   * @return list of method declaration nodes
   */
  public List<MethodDeclarationNode> getMethodDeclarations() {
    return getChildren().stream()
        .filter(ClassDeclarationNode.class::isInstance)
        .map(ClassDeclarationNode.class::cast)
        .flatMap(cls -> cls.getChildren().stream())
        .filter(MethodDeclarationNode.class::isInstance)
        .map(MethodDeclarationNode.class::cast)
        .toList();
  }
}
