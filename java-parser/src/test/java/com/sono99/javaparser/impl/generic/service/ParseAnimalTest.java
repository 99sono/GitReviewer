package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import com.sono99.javaparser.impl.generic.model.ClassDeclarationNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.FieldDeclarationNode;
import com.sono99.javaparser.impl.generic.model.ImportDeclarationNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.generic.model.PackageDeclarationNode;
import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test parsing of Java files into CompilationUnitNode ASTs with granular validation of package and
 * import children. Validates specific content and structure rather than just counts.
 */
class ParseAnimalTest extends AbstractJavaParserTest {

  /**
   * Test that the JavaParserService2 correctly parses a complex Java source file and creates a
   * valid CompilationUnitNode with proper line ranges and source content. Ensures the parser
   * foundation is solid before adding child node population.
   */
  @Test
  void shouldParseAnimalJavaFile() throws IOException {
    // Given: Load the Animal.java test file
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    CompilationUnitNode result = parseFile("Animal.java", repositoryPath);
    String sourceContent =
        Files.readString(Path.of("src/test/resources/example_classes/Animal.java"));

    // Then: Validate basic CompilationUnitNode properties using common method
    validateBasicProperties(result, sourceContent, repositoryPath);

    // Should have children: 1 package + 6 imports + 1 class
    assertThat(result.getChildren()).hasSize(8);

    // Validate specific package declaration using common method
    assertPackageDeclaration(result, "package com.example.animals;");

    // Validate specific imports using common method
    assertImportExists(result.getChildren(), "jakarta.persistence.Entity", false, false);
    assertImportExists(
        result.getChildren(), "jakarta.validation.constraints.AssertTrue", false, false);

    // Validate class declaration is now included
    assertThat(result.getChildren()).hasSize(8); // 1 package + 6 imports + 1 class

    // Validate the class declaration
    ClassDeclarationNode classNode =
        result.getChildren().stream()
            .filter(ClassDeclarationNode.class::isInstance)
            .map(ClassDeclarationNode.class::cast)
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
    // This is CORRECT JavaParser behavior - annotations are part of class
    // declaration
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

    // Current implementation: annotations in javaChunk but not parsed to
    // AnnotationNode yet (Step A)
    assertThat(classNode.getAnnotations()).isEmpty(); // Step A - no structured annotations yet

    // With DFS approach, fields and methods are correctly children of the class
    assertThat(classNode.getChildren()).isNotEmpty(); // Now has field and method children
    assertThat(classNode.getChildren()).hasSize(6); // 2 fields + 4 methods

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

    // Validate method-level Javadoc extraction for isAnimalTypeValid method
    assertThat(validationMethod.getJavadoc()).isNotNull();
    assertThat(validationMethod.getJavadoc().getJavaChunk())
        .contains("/**")
        .contains("Validates that the animal type name contains no digits.");

    // Validate parent relationships
    assertThat(result.getParent()).isNull();
    assertThat(classNode.getParent()).isSameAs(result);

    // Document expected behavior: JavaParser getRange() includes annotations and
    // javadoc
    // This provides complete context for LLM processing while keeping Step A
    // implementation simple

    // Validate overall import statistics
    assertImportStatistics(result.getChildren());
  }

  /**
   * Test that the new node reference functionality works correctly. Verifies that we can access the
   * original JavaParser AST nodes for enhanced information access.
   */
  @Test
  void shouldProvideAccessToOriginalJavaParserNodes() throws IOException {
    // Given: Load the Animal.java test file and create parser service
    JavaParserService parserService = new JavaParserService();
    SourceContentUtils utils = new SourceContentUtils();
    parserService.setSourceUtils(utils);
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
    ClassDeclarationNode classNode = result.getClassDeclarations().get(0);
    assertThat(classNode).isNotNull();
    assertThat(classNode.getOriginalNode()).isNotNull();
    assertThat(classNode.getOriginalNode())
        .isInstanceOf(com.github.javaparser.ast.body.TypeDeclaration.class);

    // Verify that we can extract additional information from the original nodes
    // For example, get the class modifiers from the original TypeDeclaration
    com.github.javaparser.ast.body.TypeDeclaration<?> originalTypeDecl =
        classNode.getOriginalNode();
    assertThat(originalTypeDecl.getModifiers()).isNotEmpty();

    // Verify that the original CompilationUnit contains the expected types
    com.github.javaparser.ast.CompilationUnit originalCu = result.getOriginalNode();
    assertThat(originalCu.getTypes()).hasSize(1); // One class in Animal.java
    assertThat(originalCu.getTypes().get(0).getNameAsString()).isEqualTo("Animal");
  }

  /**
   * Test that the JavaParserService correctly handles minimal but valid Java source code. Ensures
   * the parser can handle edge cases with small compilation units before expanding to complex
   * scenarios.
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

    // When: Parse the minimal source using common method
    CompilationUnitNode result =
        createParserService()
            .parseCompilationUnit(new ByteArrayInputStream(minimalJava.getBytes()), repositoryPath);

    // Then: Basic validation using common method
    validateBasicProperties(result, minimalJava, repositoryPath);
    assertThat(result.getChildren()).hasSize(2); // Has 1 package declaration + 1 class
    assertThat(result.getParent()).isNull();
  }

  /**
   * Test comprehensive Javadoc extraction for classes, fields, and methods. Validates that Javadoc
   * comments are properly extracted and associated with their respective elements.
   */
  @Test
  void shouldExtractJavadocCommentsCorrectly() throws IOException {
    // Given: Load the Animal.java test file which has comprehensive Javadoc
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    CompilationUnitNode result = parseFile("Animal.java", repositoryPath);

    // When: Extract the class node
    ClassDeclarationNode classNode = result.getClassDeclarations().get(0);

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

    // Validate that all methods in Animal.java have Javadoc (all 4 methods have
    // Javadoc)
    List<MethodDeclarationNode> methodsWithJavadoc =
        methods.stream().filter(m -> m.getJavadoc() != null).toList();
    assertThat(methodsWithJavadoc).hasSize(4); // All methods in Animal.java have Javadoc

    // Validate Javadoc positioning relative to elements
    assertThat(classNode.getJavadoc().getEndLine() + 1)
        .isEqualTo(classNode.getStartLine()); // Javadoc ends
    // right
    // before class
    // starts
    assertThat(idField.getJavadoc().getEndLine() + 1)
        .isEqualTo(idField.getStartLine()); // Javadoc ends
    // right
    // before field
    // starts
    assertThat(validationMethod.getJavadoc().getEndLine() + 1)
        .isEqualTo(validationMethod.getStartLine()); // Javadoc
    // ends
    // right
    // before
    // method
    // starts

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
   */
  @Test
  void shouldParseGitHubCompilationUnitSuccessfully() throws IOException {
    // Given: Load the Animal.java test file and create parser service
    JavaParserService parserService = new JavaParserService();
    SourceContentUtils utils = new SourceContentUtils();
    parserService.setSourceUtils(utils);
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
}
