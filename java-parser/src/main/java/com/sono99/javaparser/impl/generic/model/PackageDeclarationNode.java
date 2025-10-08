package com.sono99.javaparser.impl.generic.model;

import com.github.javaparser.ast.PackageDeclaration;
import java.util.List;

/**
 * Represents a package declaration in Java source code (e.g., "package com.example.animals;").
 * Maintains a reference to the original JavaParser PackageDeclaration for enhanced information
 * access.
 *
 * @since v1.0
 */
public class PackageDeclarationNode extends AbstractJavaNode<PackageDeclaration> {

  /** The full package name (e.g., "com.example.animals"). */
  private final String packageName;

  /**
   * Constructor for PackageDeclarationNode.
   *
   * @param startLine 1-based line number where this package declaration starts
   * @param endLine 1-based line number where this package declaration ends
   * @param packageName the full package name
   * @param javaCodeChunk the complete declaration text
   * @param originalNode reference to the original JavaParser PackageDeclaration
   * @param children the child nodes, typically empty for package declarations
   */
  public PackageDeclarationNode(
      int startLine,
      int endLine,
      String packageName,
      String javaCodeChunk,
      PackageDeclaration originalNode,
      List<AbstractJavaNode<? extends com.github.javaparser.ast.Node>> children) {
    super(startLine, endLine, javaCodeChunk, originalNode, children);
    this.packageName = packageName;
  }

  /**
   * Gets the package name.
   *
   * @return the full package name
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Gets the package value (same as package name for backwards compatibility).
   *
   * @return the package name
   */
  public String getValue() {
    return packageName;
  }
}
