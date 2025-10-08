package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.ImportDeclaration;
import java.util.List;

/**
 * Represents an import declaration in Java source code (e.g., "import
 * jakarta.persistence.Entity;"). Maintains a reference to the original JavaParser ImportDeclaration
 * for enhanced information access.
 *
 * @since v1.0
 */
public class ImportDeclarationNode extends AbstractJavaNode<ImportDeclaration> {

  /** The full import statement (e.g., "jakarta.persistence.Entity"). */
  private final String importName;

  /** Whether this is a static import. */
  private final boolean isStatic;

  /** Whether this is an asterisk/wildcard import. */
  private final boolean isAsterisk;

  /**
   * Constructor for ImportDeclarationNode.
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
  public ImportDeclarationNode(
      int startLine,
      int endLine,
      String importName,
      boolean isStatic,
      boolean isAsterisk,
      String javaCodeChunk,
      ImportDeclaration originalNode,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.importName = importName;
    this.isStatic = isStatic;
    this.isAsterisk = isAsterisk;
  }

  /**
   * Gets the imported name.
   *
   * @return the full import name
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
   * Checks if this is a wildcard/asterisk import.
   *
   * @return true if asterisk import
   */
  public boolean isAsterisk() {
    return isAsterisk;
  }
}
