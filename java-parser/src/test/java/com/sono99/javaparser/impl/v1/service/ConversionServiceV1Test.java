package com.sono99.javaparser.impl.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.api.v1.model.ClassDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
import com.sono99.javaparser.api.v1.model.FieldDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.ImportDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.MethodDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.PackageDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.service.JavaParserService;
import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import com.sono99.javaparser.impl.v1.converter.AnnotationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.ClassDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.CompilationUnitNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.FieldDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.ImportDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.JavadocNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.MethodDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.PackageDeclarationNodeConverterV1;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for ConversionServiceV1. Tests the complete pipeline from generic parsing to v1
 * API conversion. Validates that the conversion service correctly transforms generic AST nodes to
 * versioned API nodes while preserving all essential information.
 *
 * <p>Uses black-box testing approach: parses with generic JavaParserService, then converts to v1
 * API using ConversionServiceV1, validating the end-to-end conversion quality.
 */
@SpringBootTest
@ContextConfiguration(
    classes = {
      JavaParserService.class,
      ConversionServiceV1.class,
      SourceContentUtils.class,
      // All converter classes need to be explicitly listed for Spring to discover them
      AnnotationNodeConverterV1.class,
      ClassDeclarationNodeConverterV1.class,
      CompilationUnitNodeConverterV1.class,
      FieldDeclarationNodeConverterV1.class,
      ImportDeclarationNodeConverterV1.class,
      JavadocNodeConverterV1.class,
      MethodDeclarationNodeConverterV1.class,
      PackageDeclarationNodeConverterV1.class,
    })
class ConversionServiceV1Test {

  @Autowired private JavaParserService genericParser;
  @Autowired private ConversionServiceV1 conversionService;

  /**
   * Test that verifies the complete conversion pipeline works correctly. Parses Animal.java using
   * the generic parser, converts to v1 API, and validates that all structures are properly
   * transformed while preserving essential information like line numbers, content, and hierarchy.
   *
   * <p>Validates comprehensive AST structure conversion including package declarations, imports,
   * class declarations with Javadoc, field declarations, and method declarations.
   */
  @Test
  void shouldConvertAnimalJavaFileSuccessfully() throws IOException {
    // Given: Load the Animal.java test file and set up services
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);

    // When: Parse with generic service and convert to v1
    CompilationUnitNode genericResult =
        genericParser.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    CompilationUnitNodeV1 v1Result =
        conversionService.convert(genericResult, CompilationUnitNodeV1.class);

    // Then: Validate basic conversion success
    assertThat(v1Result).isNotNull();
    assertThat(v1Result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(v1Result.getJavaChunk()).isEqualTo(sourceContent);
    assertThat(v1Result.getStartLine()).isEqualTo(1);
    assertThat(v1Result.getEndLine()).isEqualTo(69);

    // Should have children: 1 package + 6 imports + 1 class
    assertThat(v1Result.getChildren()).hasSize(8);

    // Validate specific package declaration conversion
    PackageDeclarationNodeV1 packageNode = v1Result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getPackageName()).isEqualTo("com.example.animals");
    assertThat(packageNode.getJavaChunk()).isEqualTo("package com.example.animals;");

    // Validate specific imports conversion
    List<ImportDeclarationNodeV1> importNodes = v1Result.getImportDeclarations();
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

    // Validate the class declaration conversion
    ClassDeclarationNodeV1 classNode = v1Result.getClassDeclarations().get(0);
    assertThat(classNode).isNotNull();
    assertThat(classNode.getName()).isEqualTo("Animal");

    // Validate class-level Javadoc conversion
    assertThat(classNode.getJavadoc()).isNotNull();
    assertThat(classNode.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Represents a generic animal entity in the system.")
        .contains("This class is intended as a base type for specific animal implementations.");

    // Validate line range behavior
    assertThat(classNode.getStartLine()).isEqualTo(14); // Starts at @Entity
    assertThat(classNode.getEndLine()).isEqualTo(69); // Ends at closing brace

    // Validate javaChunk contains the complete annotated class declaration
    assertThat(classNode.getJavaChunk())
        .contains("@Entity")
        .contains("@Table(name = \"animals\")")
        .contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)")
        .contains("public class Animal")
        .contains("private long id;")
        .contains("private String animalTypeName;")
        .contains("@AssertTrue")
        .contains("public boolean isAnimalTypeValid()")
        .endsWith("}");

    // Validate that we have the expected field and method children
    assertThat(classNode.getChildren()).isNotEmpty();
    assertThat(classNode.getChildren()).hasSize(6); // 2 fields + 4 methods

    // Validate field conversion
    List<FieldDeclarationNodeV1> fields = classNode.getFieldDeclarations();
    assertThat(fields).hasSize(2);

    // Validate first field (id) conversion
    FieldDeclarationNodeV1 idField =
        fields.stream().filter(f -> "id".equals(f.getName())).findFirst().orElse(null);
    assertThat(idField).isNotNull();
    assertThat(idField.getName()).isEqualTo("id");
    assertThat(idField.getType()).isEqualTo("long");
    assertThat(idField.getJavaChunk()).contains("@Id");
    assertThat(idField.getJavaChunk()).contains("private long id;");

    // Validate field-level Javadoc conversion for id field
    assertThat(idField.getJavadoc()).isNotNull();
    assertThat(idField.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Primary key identifier for the animal.");

    // Validate second field (animalTypeName) conversion
    FieldDeclarationNodeV1 animalTypeField =
        fields.stream().filter(f -> "animalTypeName".equals(f.getName())).findFirst().orElse(null);
    assertThat(animalTypeField).isNotNull();
    assertThat(animalTypeField.getName()).isEqualTo("animalTypeName");
    assertThat(animalTypeField.getType()).isEqualTo("String");
    assertThat(animalTypeField.getJavaChunk()).contains("private String animalTypeName;");

    // Validate method conversion
    List<MethodDeclarationNodeV1> methods = classNode.getMethodDeclarations();
    assertThat(methods).hasSize(4);

    // Validate isAnimalTypeValid method conversion
    MethodDeclarationNodeV1 validationMethod =
        methods.stream()
            .filter(m -> "isAnimalTypeValid".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(validationMethod).isNotNull();
    assertThat(validationMethod.getName()).isEqualTo("isAnimalTypeValid");
    assertThat(validationMethod.getSignature()).isEqualTo("isAnimalTypeValid()");
    assertThat(validationMethod.getJavaChunk()).contains("@AssertTrue");
    assertThat(validationMethod.getJavaChunk()).contains("public boolean isAnimalTypeValid()");

    // Validate method-level Javadoc conversion
    assertThat(validationMethod.getJavadoc()).isNotNull();
    assertThat(validationMethod.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Validates that the animal type name contains no digits.");

    // Validate that all methods in Animal.java have Javadoc (all 4 methods have Javadoc)
    List<MethodDeclarationNodeV1> methodsWithJavadoc =
        methods.stream().filter(m -> m.getJavadoc() != null).toList();
    assertThat(methodsWithJavadoc).hasSize(4);

    // Validate hierarchical relationships are preserved
    assertThat(v1Result.getChildren()).isNotEmpty();
    assertThat(classNode.getChildren()).isNotEmpty();

    // Validate overall import statistics
    assertThat(importNodes).hasSize(6);
  }

  /**
   * Test that validates conversion of a minimal Java file. Ensures the conversion service can
   * handle simple cases before expanding to complex scenarios.
   */
  @Test
  void shouldConvertMinimalJavaFileSuccessfully() {
    // Given: Minimal valid Java source
    String minimalJava =
        """
                package test;
                public class Test {
                }
                """;
    String repositoryPath = "src/test/java/test/Test.java";

    // When: Parse with generic service and convert to v1
    CompilationUnitNode genericResult =
        genericParser.parseCompilationUnit(
            new ByteArrayInputStream(minimalJava.getBytes()), repositoryPath);

    CompilationUnitNodeV1 v1Result =
        conversionService.convert(genericResult, CompilationUnitNodeV1.class);

    // Then: Basic validation
    assertThat(v1Result).isNotNull();
    assertThat(v1Result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(v1Result.getJavaChunk()).isEqualTo(minimalJava);
    assertThat(v1Result.getStartLine()).isEqualTo(1);
    assertThat(v1Result.getEndLine()).isEqualTo(3);

    assertThat(v1Result.getChildren()).hasSize(2); // Has 1 package declaration + 1 class

    PackageDeclarationNodeV1 packageNode = v1Result.getPackageDeclaration();
    assertThat(packageNode).isNotNull();
    assertThat(packageNode.getPackageName()).isEqualTo("test");
    assertThat(packageNode.getJavaChunk()).isEqualTo("package test;");

    ClassDeclarationNodeV1 classNode = v1Result.getClassDeclarations().get(0);
    assertThat(classNode).isNotNull();
    assertThat(classNode.getName()).isEqualTo("Test");
  }

  /**
   * Test that validates Javadoc conversion across all node types. Ensures that Javadoc comments are
   * properly converted from generic nodes to v1 API nodes.
   */
  @Test
  void shouldConvertJavadocCommentsCorrectly() throws IOException {
    // Given: Load the Animal.java test file which has comprehensive Javadoc
    String repositoryPath = "src/main/java/com/example/animals/Animal.java";
    Path animalFile = Path.of("src/test/resources/example_classes/Animal.java");
    String sourceContent = Files.readString(animalFile);

    // When: Parse and convert to v1
    CompilationUnitNode genericResult =
        genericParser.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    CompilationUnitNodeV1 v1Result =
        conversionService.convert(genericResult, CompilationUnitNodeV1.class);

    ClassDeclarationNodeV1 classNode = v1Result.getClassDeclarations().get(0);

    // Then: Validate class-level Javadoc conversion
    assertThat(classNode.getJavadoc()).isNotNull();
    assertThat(classNode.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Represents a generic animal entity in the system.")
        .contains("This class is intended as a base type for specific animal implementations.")
        .contains("*/");

    // Validate field-level Javadoc conversion
    List<FieldDeclarationNodeV1> fields = classNode.getFieldDeclarations();

    // Validate id field Javadoc conversion
    FieldDeclarationNodeV1 idField =
        fields.stream().filter(f -> "id".equals(f.getName())).findFirst().orElse(null);
    assertThat(idField).isNotNull();
    assertThat(idField.getJavadoc()).isNotNull();
    assertThat(idField.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Primary key identifier for the animal.");

    // Validate method-level Javadoc conversion
    List<MethodDeclarationNodeV1> methods = classNode.getMethodDeclarations();

    // Validate isAnimalTypeValid method Javadoc conversion
    MethodDeclarationNodeV1 validationMethod =
        methods.stream()
            .filter(m -> "isAnimalTypeValid".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(validationMethod).isNotNull();
    assertThat(validationMethod.getJavadoc()).isNotNull();
    assertThat(validationMethod.getJavadoc().javaCodeChunk())
        .contains("/**")
        .contains("Validates that the animal type name contains no digits.");
  }

  /**
   * Test that validates the conversion service handles edge cases gracefully. Ensures that the
   * conversion process is robust and doesn't fail on unusual but valid Java structures.
   */
  @Test
  void shouldHandleConversionEdgeCases() throws IOException {
    // Given: Load a more complex test file
    String repositoryPath = "src/main/java/com/example/animals/ClassWithInnerClass.java";
    Path testFile = Path.of("src/test/resources/example_classes/ClassWithInnerClass.java");
    String sourceContent = Files.readString(testFile);

    // When: Parse with generic service and convert to v1
    CompilationUnitNode genericResult =
        genericParser.parseCompilationUnit(
            new ByteArrayInputStream(sourceContent.getBytes()), repositoryPath);

    CompilationUnitNodeV1 v1Result =
        conversionService.convert(genericResult, CompilationUnitNodeV1.class);

    // Then: Verify conversion succeeded even with complex structures
    assertThat(v1Result).isNotNull();
    assertThat(v1Result.getChildren()).isNotEmpty();

    // Should have at least one class
    assertThat(v1Result.getClassDeclarations()).isNotEmpty();

    // Validate that the conversion preserved the structure
    ClassDeclarationNodeV1 mainClass = v1Result.getClassDeclarations().get(0);
    assertThat(mainClass).isNotNull();
    assertThat(mainClass.getChildren()).isNotEmpty(); // Should have inner class or other members
  }

  /**
   * Test that verifies Spring properly injected both services. Ensures that the @Autowired
   * annotations work correctly for both the generic parser and conversion service.
   */
  @Test
  void shouldVerifySpringInjectionWorks() {
    // Verify that Spring properly injected both services
    assertThat(genericParser).isNotNull();
    assertThat(conversionService).isNotNull();
  }
}
