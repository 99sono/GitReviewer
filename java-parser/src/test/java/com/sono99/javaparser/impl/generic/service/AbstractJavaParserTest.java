package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.Node;
import com.sono99.javaparser.impl.generic.model.AbstractJavaNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.ImportDeclarationNode;
import com.sono99.javaparser.impl.generic.model.PackageDeclarationNode;
import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstract base class for JavaParser tests providing common setup and validation methods.
 * Eliminates code duplication across test classes by centralizing shared functionality.
 *
 * @since v1.0
 */
public abstract class AbstractJavaParserTest {

  /**
   * Creates and configures a JavaParserService with SourceContentUtils for testing. Provides
   * consistent setup across all test classes.
   *
   * @return configured JavaParserService ready for use
   */
  protected JavaParserService createParserService() {
    JavaParserService parserService = new JavaParserService();
    parserService.setSourceUtils(new SourceContentUtils());
    return parserService;
  }

  /**
   * Parses a test file and returns the resulting CompilationUnitNode. Handles common file loading
   * and parsing logic used across tests.
   *
   * @param fileName the test file name (e.g., "Animal.java")
   * @param repositoryPath the expected repository path for validation
   * @return parsed CompilationUnitNode
   * @throws IOException if file reading fails
   */
  protected CompilationUnitNode parseFile(String fileName, String repositoryPath)
      throws IOException {
    JavaParserService parserService = createParserService();
    Path file = Path.of("src/test/resources/example_classes/" + fileName);
    String sourceContent = Files.readString(file);
    return parserService.parseCompilationUnit(
        new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);
  }

  /**
   * Validates basic CompilationUnitNode properties that are common across all tests. Ensures
   * consistent validation of core properties without duplication.
   *
   * @param result the parsed CompilationUnitNode to validate
   * @param sourceContent the original source content for comparison
   * @param repositoryPath the expected repository path
   */
  protected void validateBasicProperties(
      CompilationUnitNode result, String sourceContent, String repositoryPath) {
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(result.getStartLine()).isEqualTo(1);
    long lineCount = sourceContent.lines().count();
    assertThat(result.getEndLine()).isEqualTo((int) lineCount);
    assertThat(result.getJavaChunk()).isEqualTo(sourceContent);
  }

  /**
   * Validates that a PackageDeclarationNode has correct properties. Common validation logic used
   * across multiple test classes.
   *
   * @param root the CompilationUnitNode containing the package declaration
   * @param expectedChunk the expected package declaration text
   */
  protected void assertPackageDeclaration(CompilationUnitNode root, String expectedChunk) {
    PackageDeclarationNode packageNode = root.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getJavaChunk()).isEqualTo(expectedChunk);
    assertThat(packageNode.getParent()).isSameAs(root);
  }

  /**
   * Validates that a specific import exists with correct properties. Common validation logic for
   * import declarations across test classes.
   *
   * @param children list of child nodes to search
   * @param expectedImport the expected import name
   * @param expectStatic whether the import should be static
   * @param expectAsterisk whether the import should be an asterisk import
   */
  protected void assertImportExists(
      List<AbstractJavaNode<? extends Node>> children,
      String expectedImport,
      boolean expectStatic,
      boolean expectAsterisk) {
    ImportDeclarationNode importNode =
        children.stream()
            .filter(ImportDeclarationNode.class::isInstance)
            .map(ImportDeclarationNode.class::cast)
            .filter(imp -> expectedImport.equals(imp.getImportName()))
            .findFirst()
            .orElse(null);

    assertThat(importNode).isNotNull();
    assertThat(importNode.isStatic()).isEqualTo(expectStatic);
    assertThat(importNode.isAsterisk()).isEqualTo(expectAsterisk);
    assertThat(importNode.getParent()).isNotNull();
  }

  /**
   * Validates the total number and types of imports. Common validation logic for import statistics
   * across test classes.
   *
   * @param children list of child nodes to analyze
   */
  protected void assertImportStatistics(List<AbstractJavaNode<? extends Node>> children) {
    List<ImportDeclarationNode> imports =
        children.stream()
            .filter(ImportDeclarationNode.class::isInstance)
            .map(ImportDeclarationNode.class::cast)
            .toList();

    assertThat(imports).isNotEmpty();
    assertThat(imports).allMatch(imp -> !imp.isStatic()); // None are static
    assertThat(imports).allMatch(imp -> !imp.isAsterisk()); // None are asterisk imports
  }
}
