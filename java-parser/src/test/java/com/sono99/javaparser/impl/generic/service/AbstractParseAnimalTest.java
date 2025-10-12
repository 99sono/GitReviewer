package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import com.sono99.javaparser.impl.generic.model.AnnotationNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.FieldDeclarationNode;
import com.sono99.javaparser.impl.generic.model.ImportDeclarationNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.generic.model.PackageDeclarationNode;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class containing core test logic for JavaParserService testing. Provides common
 * test methods that can be used by both unit tests (manual instantiation) and integration tests
 * (Spring context). Eliminates code duplication while supporting both fast unit tests and
 * comprehensive integration tests.
 *
 * <p>Subclasses must implement {@link #createParserService()} to provide the appropriate service
 * instance for their testing strategy.
 */
abstract class AbstractParseAnimalTest {

  /**
   * Creates and configures a JavaParserService instance for testing. Unit tests should return
   * manually instantiated service, while integration tests should return Spring-injected service.
   *
   * @return configured JavaParserService ready for testing
   */
  protected abstract JavaParserService createParserService();

  /**
   * Test that the JavaParserService correctly parses a complex Java source file and creates a valid
   * CompilationUnitNode with proper line ranges and source content. Ensures the parser foundation
   * is solid before adding child node population.
   *
   * <p>Validates comprehensive AST structure including package declarations, imports, class
   * declarations with annotations, field declarations with Javadoc, and method declarations. Tests
   * the hierarchical parent-child relationships created by the DFS parsing approach.
   */
  @Test
  void shouldParseAnimalJavaFile() throws IOException {
    // Given: Load the Animal.java test file and create parser service
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);
    JavaParserService parserService = createParserService();

    // When: Parse the source using the configured service
    CompilationUnitNode result =
        parserService.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    // Then: Validate basic CompilationUnitNode properties
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(result.getJavaChunk()).isEqualTo(sourceContent);
    assertThat(result.getStartLine()).isEqualTo(1);
    assertThat(result.getEndLine()).isEqualTo(69);

    // Should have children: 1 package + 6 imports + 1 class
    assertThat(result.getChildren()).hasSize(8);

    // Validate specific package declaration
    PackageDeclarationNode packageNode = result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getPackageName()).isEqualTo("com.example.animals");
    assertThat(packageNode.getJavaChunk()).isEqualTo("package com.example.animals;");

    // Validate specific imports
    List<ImportDeclarationNode> importNodes = result.getImportDeclarations();
    assertThat(importNodes).hasSize(6);

    // Check for specific imports
    assertThat(
            importNodes.stream()
                .anyMatch(imp -> imp.getImportName().equals("jakarta.persistence.Entity")))
        .isTrue();
    assertThat(
            importNodes.stream()
                .anyMatch(
                    imp -> imp.getImportName().equals("jakarta.validation.constraints.AssertTrue")))
        .isTrue();

    // Validate the class declaration
    TypeDeclarationNode classNode =
        result.getChildren().stream()
            .filter(TypeDeclarationNode.class::isInstance)
            .map(TypeDeclarationNode.class::cast)
            .findFirst()
            .orElse(null);

    assertThat(classNode).isNotNull();
    assertThat(classNode.getName()).isEqualTo("Animal");

    // Validate class-level Javadoc extraction
    assertThat(classNode.getJavadoc()).isNotNull();
    assertThat(classNode.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Represents a generic animal entity in the system.")
        .contains("This class is intended as a base type for specific animal implementations.");

    // Validate line range behavior: getRange() includes class annotations
    // This is CORRECT JavaParser behavior - annotations are part of class declaration
    assertThat(classNode.getStartLine()).isEqualTo(14); // Starts at @Entity (correct)
    assertThat(classNode.getEndLine()).isEqualTo(69); // Ends at closing brace

    // Validate javaChunk contains the complete annotated class declaration
    assertThat(classNode.getJavaChunk())
        .contains("@Entity") // Class annotation
        .contains("@Table(name = \"animals\")") // Table annotation
        .contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)") // Inheritance
        // annotation
        .contains("public class Animal") // Class declaration
        .contains("private long id;") // Field
        .contains("private String animalTypeName;") // Another field
        .contains("@AssertTrue") // Method annotation
        .contains("public boolean isAnimalTypeValid()") // Method declaration
        .endsWith("}"); // Class closing brace

    // Validate class-level annotations
    List<AnnotationNode> classAnnotations =
        classNode.getChildren().stream()
            .filter(AnnotationNode.class::isInstance)
            .map(AnnotationNode.class::cast)
            .toList();
    assertThat(classAnnotations).hasSize(3);
    assertThat(
            classAnnotations.stream()
                .anyMatch(
                    ann -> ann.getName().equals("Entity") && ann.getJavaChunk().equals("@Entity")))
        .isTrue();
    assertThat(
            classAnnotations.stream()
                .anyMatch(
                    ann -> {
                      if (ann.getName().equals("Table")) {
                        System.out.println("DEBUG: @Table javaCodeChunk = " + ann.getJavaChunk());
                      }
                      String expectedAnnoationCode = "@Table(name = \"animals\")";
                      return ann.getName().equals("Table")
                          && ann.getJavaChunk().equals(expectedAnnoationCode);
                    }))
        .isTrue();
    assertThat(
            classAnnotations.stream()
                .anyMatch(
                    ann ->
                        ann.getName().equals("Inheritance")
                            && ann.getJavaChunk()
                                .equals("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)")))
        .isTrue();

    // Current implementation: annotations in javaChunk but not parsed to AnnotationNode yet

    // With DFS approach, fields, methods, and annotations are correctly children of the class
    assertThat(classNode.getChildren())
        .isNotEmpty(); // Now has field, method, and annotation children
    assertThat(classNode.getChildren()).hasSize(9); // 3 class annotations + 2 fields + 4 methods

    // Validate that we have the expected field and method children
    long fieldCount =
        classNode.getChildren().stream()
            .filter(child -> child instanceof FieldDeclarationNode)
            .count();
    long methodCount =
        classNode.getChildren().stream()
            .filter(child -> child instanceof MethodDeclarationNode)
            .count();

    assertThat(fieldCount).isEqualTo(2); // id and animalTypeName fields
    assertThat(methodCount).isEqualTo(4); // 4 methods in Animal class

    // Validate field names and content
    List<FieldDeclarationNode> fields =
        classNode.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .toList();

    assertThat(fields).hasSize(2);

    // Validate first field (id) - includes @Id annotation
    FieldDeclarationNode idField =
        fields.stream().filter(f -> "id".equals(f.getName())).findFirst().orElse(null);
    assertThat(idField).isNotNull();
    assertThat(idField.getName()).isEqualTo("id");
    assertThat(idField.getType()).isEqualTo("long");
    assertThat(idField.getJavaChunk()).contains("@Id");
    assertThat(idField.getJavaChunk()).contains("private long id;");

    // Validate field-level annotations for idField
    List<AnnotationNode> fieldAnnotations =
        idField.getChildren().stream()
            .filter(AnnotationNode.class::isInstance)
            .map(AnnotationNode.class::cast)
            .toList();
    assertThat(fieldAnnotations).hasSize(1); // Field should have @Id annotation as child
    assertThat(
            fieldAnnotations.stream()
                .anyMatch(ann -> ann.getName().equals("Id") && ann.getJavaChunk().contains("@Id")))
        .isTrue();

    // Validate field-level Javadoc extraction for id field
    assertThat(idField.getJavadoc()).isNotNull();
    assertThat(idField.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Primary key identifier for the animal.");

    // Validate second field (animalTypeName) - no annotations but has indentation
    FieldDeclarationNode animalTypeField =
        fields.stream().filter(f -> "animalTypeName".equals(f.getName())).findFirst().orElse(null);
    assertThat(animalTypeField).isNotNull();
    assertThat(animalTypeField.getName()).isEqualTo("animalTypeName");
    assertThat(animalTypeField.getType()).isEqualTo("String");
    assertThat(animalTypeField.getJavaChunk()).contains("private String animalTypeName;");

    // Validate field-level Javadoc extraction for animalTypeName field
    assertThat(animalTypeField.getJavadoc()).isNotNull();
    assertThat(animalTypeField.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Describes the type of animal, e.g., \"Cat\", \"Shark\", etc.");

    // Validate method signatures and content
    List<MethodDeclarationNode> methods =
        classNode.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .toList();

    assertThat(methods).hasSize(4);

    // Validate isAnimalTypeValid method
    MethodDeclarationNode validationMethod =
        methods.stream()
            .filter(m -> "isAnimalTypeValid".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(validationMethod).isNotNull();
    assertThat(validationMethod.getName()).isEqualTo("isAnimalTypeValid");
    assertThat(validationMethod.getSignature()).isEqualTo("isAnimalTypeValid()");
    assertThat(validationMethod.getJavaChunk()).contains("@AssertTrue");
    assertThat(validationMethod.getJavaChunk()).contains("public boolean isAnimalTypeValid()");

    // Validate method-level annotations for validationMethod
    List<AnnotationNode> methodAnnotations =
        validationMethod.getChildren().stream()
            .filter(AnnotationNode.class::isInstance)
            .map(AnnotationNode.class::cast)
            .toList();
    assertThat(methodAnnotations).hasSize(1);
    assertThat(
            methodAnnotations.stream()
                .anyMatch(
                    ann ->
                        ann.getName().equals("AssertTrue")
                            && ann.getJavaChunk().contains("@AssertTrue")))
        .isTrue();

    // Validate method-level Javadoc extraction for isAnimalTypeValid method
    assertThat(validationMethod.getJavadoc()).isNotNull();
    assertThat(validationMethod.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Validates that the animal type name contains no digits.");

    // Validate parent relationships
    assertThat(result.getParent()).isNull();
    assertThat(classNode.getParent()).isSameAs(result);

    // Validate overall import statistics (assuming 6 imports)
    assertThat(importNodes).hasSize(6);
  }

  /**
   * Test that the new node reference functionality works correctly. Verifies that we can access the
   * original JavaParser AST nodes for enhanced information access.
   *
   * <p>Tests the bridge between our custom AST model and the original JavaParser AST nodes,
   * ensuring that enhanced information from the GitHub parser is accessible when needed.
   */
  @Test
  void shouldProvideAccessToOriginalJavaParserNodes() throws IOException {
    // Given: Load the Animal.java test file and create parser service
    JavaParserService parserService = createParserService();
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";

    // When: Parse the source using our service
    CompilationUnitNode result =
        parserService.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    // Then: Verify we can access the original JavaParser CompilationUnit
    assertThat(result.getOriginalNode()).isNotNull();
    assertThat(result.getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.CompilationUnit.class);

    // Verify package declaration node has access to original PackageDeclaration
    PackageDeclarationNode packageNode = result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getOriginalNode()).isNotNull();
    assertThat(packageNode.getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.PackageDeclaration.class);

    // Verify import declaration nodes have access to original ImportDeclaration
    List<ImportDeclarationNode> importNodes = result.getImportDeclarations();
    assertThat(importNodes).isNotEmpty();
    for (ImportDeclarationNode importNode : importNodes) {
      assertThat(importNode.getOriginalNode()).isNotNull();
      assertThat(importNode.getOriginalNode())
          .isInstanceOf(com.github.javaparser.ast.ImportDeclaration.class);
    }

    // Verify class declaration node has access to original TypeDeclaration
    TypeDeclarationNode classNode = result.getTypeDeclarations().get(0);
    assertThat(classNode).isNotNull();
    assertThat(classNode.getOriginalNode()).isNotNull();
    assertThat(classNode.getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.body.TypeDeclaration.class);

    // Verify that we can extract additional information from the original nodes
    // For example, get the class modifiers from the original TypeDeclaration
    com.github.javaparser.ast.body.TypeDeclaration<?> originalTypeDecl =
        (com.github.javaparser.ast.body.TypeDeclaration<?>) classNode.getOriginalNode();
    assertThat(originalTypeDecl.getModifiers()).isNotEmpty();

    // Verify that the original CompilationUnit contains the expected types
    com.github.javaparser.ast.CompilationUnit originalCu =
        (com.github.javaparser.ast.CompilationUnit) result.getOriginalNode();
    assertThat(originalCu.getTypes()).hasSize(1); // One class in Animal.java
    assertThat(originalCu.getTypes().get(0).getNameAsString()).isEqualTo("Animal");
  }

  /**
   * Test that the JavaParserService correctly handles minimal but valid Java source code. Ensures
   * the parser can handle edge cases with small compilation units before expanding to complex
   * scenarios.
   *
   * <p>Validates basic parsing functionality with the simplest possible valid Java source, ensuring
   * the parser doesn't have assumptions about complexity requirements.
   */
  @Test
  void shouldParseMinimalJavaFile() {
    // Given: Minimal valid Java source
    String minimalJava =
        """
                package test;
                public class Test {
                }
                """;
    String repositoryPath = "src/test/java/test/Test.java";
    JavaParserService parserService = createParserService();

    // When: Parse the minimal source using the configured service
    CompilationUnitNode result =
        parserService.parseCompilationUnit(
            new ByteArrayInputStream(minimalJava.getBytes()), repositoryPath);

    // Then: Basic validation
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(result.getJavaChunk()).isEqualTo(minimalJava);
    assertThat(result.getStartLine()).isEqualTo(1);
    assertThat(result.getEndLine()).isEqualTo(3);

    assertThat(result.getChildren()).hasSize(2); // Has 1 package declaration + 1 class
    assertThat(result.getParent()).isNull();

    PackageDeclarationNode packageNode = result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getPackageName()).isEqualTo("test");
    assertThat(packageNode.getJavaChunk()).isEqualTo("package test;");

    TypeDeclarationNode classNode = result.getTypeDeclarations().get(0);
    assertThat(classNode).isNotNull();
    assertThat(classNode.getName()).isEqualTo("Test");
  }

  /**
   * Test comprehensive Javadoc extraction for classes, fields, and methods. Validates that Javadoc
   * comments are properly extracted and associated with their respective elements.
   *
   * <p>Ensures that Javadoc comments are correctly positioned relative to their associated elements
   * and that the content is properly extracted for LLM processing and documentation generation
   * purposes.
   */
  @Test
  void shouldExtractJavadocCommentsCorrectly() throws IOException {
    // Given: Load the Animal.java test file which has comprehensive Javadoc
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);
    JavaParserService parserService = createParserService();

    // When: Parse and extract the class node
    CompilationUnitNode result =
        parserService.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);
    TypeDeclarationNode classNode = result.getTypeDeclarations().get(0);

    // Then: Validate class-level Javadoc extraction
    assertThat(classNode.getJavadoc()).isNotNull();
    assertThat(classNode.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Represents a generic animal entity in the system.")
        .contains("This class is intended as a base type for specific animal implementations.")
        .contains("*/");

    // Validate class-level Javadoc positioning
    assertThat(classNode.getJavadoc().getStartLine()).isEqualTo(10); // Line before @Entity
    assertThat(classNode.getJavadoc().getEndLine()).isEqualTo(13); // End of Javadoc

    // Validate field-level Javadoc extraction
    List<FieldDeclarationNode> fields =
        classNode.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .toList();

    // Validate id field Javadoc
    FieldDeclarationNode idField =
        fields.stream().filter(f -> "id".equals(f.getName())).findFirst().orElse(null);
    assertThat(idField).isNotNull();
    assertThat(idField.getJavadoc()).isNotNull();
    assertThat(idField.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Primary key identifier for the animal.")
        .contains("*/");

    // Validate animalTypeName field Javadoc
    FieldDeclarationNode animalTypeField =
        fields.stream().filter(f -> "animalTypeName".equals(f.getName())).findFirst().orElse(null);
    assertThat(animalTypeField).isNotNull();
    assertThat(animalTypeField.getJavadoc()).isNotNull();
    assertThat(animalTypeField.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Describes the type of animal, e.g., \"Cat\", \"Shark\", etc.")
        .contains("*/");

    // Validate method-level Javadoc extraction
    List<MethodDeclarationNode> methods =
        classNode.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .toList();

    // Validate isAnimalTypeValid method Javadoc
    MethodDeclarationNode validationMethod =
        methods.stream()
            .filter(m -> "isAnimalTypeValid".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(validationMethod).isNotNull();
    assertThat(validationMethod.getJavadoc()).isNotNull();
    assertThat(validationMethod.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Validates that the animal type name contains no digits.")
        .contains("@return true if the name has no numbers")
        .contains("*/");

    // Validate that all methods in Animal.java have Javadoc (all 4 methods have Javadoc)
    List<MethodDeclarationNode> methodsWithJavadoc =
        methods.stream().filter(m -> m.getJavadoc() != null).toList();
    assertThat(methodsWithJavadoc).hasSize(4); // All methods in Animal.java have Javadoc

    // Validate Javadoc positioning relative to elements
    assertThat(classNode.getJavadoc().getEndLine() + 1).isEqualTo(classNode.getStartLine());
    assertThat(idField.getJavadoc().getEndLine() + 1).isEqualTo(idField.getStartLine());
    assertThat(validationMethod.getJavadoc().getEndLine() + 1)
        .isEqualTo(validationMethod.getStartLine());

    // Validate that Javadoc nodes have access to original JavaParser nodes
    assertThat(classNode.getJavadoc().getOriginalNode()).isNotNull();
    assertThat(classNode.getJavadoc().getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.comments.JavadocComment.class);

    assertThat(idField.getJavadoc().getOriginalNode()).isNotNull();
    assertThat(idField.getJavadoc().getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.comments.JavadocComment.class);

    assertThat(validationMethod.getJavadoc().getOriginalNode()).isNotNull();
    assertThat(validationMethod.getJavadoc().getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.comments.JavadocComment.class);
  }

  /**
   * Test that the new parseGitHubCompilationUnit API works correctly. Validates that the method
   * returns a non-null CompilationUnit result. Ensures the convenience API for advanced AST
   * processing is functional.
   *
   * <p>This test validates the bridge API that provides direct access to the GitHub parser AST for
   * advanced use cases requiring the full power of the JavaParser library.
   */
  @Test
  void shouldParseGitHubCompilationUnitSuccessfully() throws IOException {
    // Given: Load the Animal.java test file and create parser service
    JavaParserService parserService = createParserService();
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";

    // When: Parse the source using the new GitHub parser API
    CompilationUnit result =
        parserService.parseGitHubCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    // Then: Verify the result is not null (trust GitHub parser correctness)
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(CompilationUnit.class);

    // Verify that we can access basic information from the raw CompilationUnit
    assertThat(result.getTypes()).isNotEmpty(); // Should have at least one type (Animal class)
    assertThat(result.getTypes().get(0).getNameAsString()).isEqualTo("Animal");
  }

  // === Helper Methods ===

  /**
   * Helper method to parse a test file from resources.
   *
   * @param fileName name of the test file in resources
   * @param repositoryPath repository path for the file
   * @return parsed CompilationUnitNode
   * @throws IOException if file cannot be read
   */
  protected CompilationUnitNode parseFile(String fileName, String repositoryPath)
      throws IOException {
    Path filePath = Path.of("src/test/resources/example_classes/" + fileName);
    String sourceContent = Files.readString(filePath);
    JavaParserService parserService = createParserService();
    return parserService.parseCompilationUnit(
        new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);
  }

  /**
   * Validates basic properties of a CompilationUnitNode.
   *
   * @param result the parsed CompilationUnitNode
   * @param expectedSource the expected source content
   * @param expectedRepositoryPath the expected repository path
   */
  protected void validateBasicProperties(
      CompilationUnitNode result, String expectedSource, String expectedRepositoryPath) {
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(expectedRepositoryPath);
    assertThat(result.getJavaChunk()).isEqualTo(expectedSource);
    assertThat(result.getStartLine()).isEqualTo(1);
  }

  /**
   * Asserts that a package declaration exists with the expected content.
   *
   * @param result the CompilationUnitNode to check
   * @param expectedPackageDeclaration the expected package declaration string
   */
  protected void assertPackageDeclaration(
      CompilationUnitNode result, String expectedPackageDeclaration) {
    PackageDeclarationNode packageNode = result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getJavaChunk()).isEqualTo(expectedPackageDeclaration);
  }

  /**
   * Asserts that an import exists in the children list.
   *
   * @param children the list of child nodes
   * @param importName the name of the import to find
   * @param isStatic whether the import should be static
   * @param isAsterisk whether the import should be an asterisk import
   */
  protected void assertImportExists(
      List<com.sono99.javaparser.impl.generic.model.AbstractJavaNode<?>> children,
      String importName,
      boolean isStatic,
      boolean isAsterisk) {
    assertThat(
            children.stream()
                .anyMatch(
                    child -> {
                      if (child instanceof ImportDeclarationNode importNode) {
                        return importNode.getImportName().equals(importName)
                            && importNode.isStatic() == isStatic
                            && importNode.isAsterisk() == isAsterisk;
                      }
                      return false;
                    }))
        .isTrue();
  }

  /**
   * Validates overall import statistics.
   *
   * @param children the list of child nodes to analyze
   */
  protected void assertImportStatistics(
      List<com.sono99.javaparser.impl.generic.model.AbstractJavaNode<?>> children) {
    long importCount =
        children.stream().filter(child -> child instanceof ImportDeclarationNode).count();
    assertThat(importCount).isEqualTo(6); // Expected number of imports in Animal.java
  }
}
