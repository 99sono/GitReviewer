package com.sono99.javaparser.api.v1.model;

import com.github.javaparser.ast.ImportDeclaration;
import java.util.List;

/**
 * Represents a V1 import declaration in Java source code. Extends AbstractJavaNodeV1 to provide
 * common AST node functionality and ensure proper inheritance hierarchy.
 *
 * @since v1.0
 */
public class ImportDeclarationNodeV1 extends AbstractJavaNodeV1<ImportDeclaration> {

  /** The full imported name. */
  private final String importName;

  /** True if this is a static import. */
  private final boolean isStatic;

  /** True if this is a wildcard import. */
  private final boolean isAsterisk;

  /**
   * Constructor for ImportDeclarationNodeV1.
   *
   * @param startLine 1-based line number where this import declaration starts
   * @param endLine 1-based line number where this import declaration ends
   * @param importName the full imported name
   * @param isStatic true if this is a static import
   * @param isAsterisk true if this is a wildcard import
   * @param javaCodeChunk the complete declaration text
   * @param originalNode reference to the original JavaParser ImportDeclaration
   * @param children the child nodes, typically empty for imports
   */
  public ImportDeclarationNodeV1(
      int startLine,
      int endLine,
      String importName,
      boolean isStatic,
      boolean isAsterisk,
      String javaCodeChunk,
      ImportDeclaration originalNode,
      List<AbstractJavaNodeV1<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.importName = importName;
    this.isStatic = isStatic;
    this.isAsterisk = isAsterisk;
  }

  /**
   * Gets the full imported name.
   *
   * @return the imported name
   */
  public String getImportName() {
    return importName;
  }

  /**
   * Checks if this is a static import.
   *
   * @return true if static import
   */
  public boolean isStatic() {
    return isStatic;
  }

  /**
   * Checks if this is a wildcard import.
   *
   * @return true if wildcard import
   */
  public boolean isAsterisk() {
    return isAsterisk;
  }
}
