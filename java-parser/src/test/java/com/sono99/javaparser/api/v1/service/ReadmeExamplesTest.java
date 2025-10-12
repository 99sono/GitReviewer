package com.sono99.javaparser.api.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
import com.sono99.javaparser.api.v1.model.MethodDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.TypeDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.service.JavaParserService;
import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import com.sono99.javaparser.impl.v1.converter.AnnotationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.CompilationUnitNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.FieldDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.ImportDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.JavadocNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.MethodDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.PackageDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.converter.TypeDeclarationNodeConverterV1;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.JavaParsingServiceV1Impl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test demonstrating realistic usage examples for README.md documentation. Tests the scenarios that
 * would be most useful for developers using the Java Parser module, focusing on practical code
 * analysis patterns that showcase the simplified AST capabilities.
 */
@SpringBootTest
@ContextConfiguration(
    classes = {
      JavaParsingServiceV1Impl.class,
      JavaParserService.class,
      ConversionServiceV1.class,
      SourceContentUtils.class,
      AnnotationNodeConverterV1.class,
      TypeDeclarationNodeConverterV1.class,
      CompilationUnitNodeConverterV1.class,
      FieldDeclarationNodeConverterV1.class,
      ImportDeclarationNodeConverterV1.class,
      JavadocNodeConverterV1.class,
      MethodDeclarationNodeConverterV1.class,
      PackageDeclarationNodeConverterV1.class,
    })
class ReadmeExamplesTest {

  @Autowired private JavaParsingServiceV1 javaParsingServiceV1;

  /**
   * Tests basic parsing functionality as shown in README examples. Demonstrates how to parse a Java
   * file and access its basic structure through the simplified AST. This represents the most common
   * use case for LLM-driven code analysis.
   *
   * @throws IOException if the test resource file cannot be read
   */
  @Test
  void shouldParseJavaFileAndAccessBasicStructure() throws IOException {
    // Given: Load the Animal.java test file (contains JPA annotations and validation)
    String repositoryPath = "src/test/resources/example_classes/Animal.java";
    Path animalFile = Path.of(repositoryPath);
    String sourceContent = Files.readString(animalFile);

    // When: Parse the source content using JavaParsingServiceV1 (BASIC EXAMPLE from README)
    CompilationUnitNodeV1 compilationUnit =
        javaParsingServiceV1.parseCompilationUnit(sourceContent, repositoryPath);

    // Then: Validate the parsed result (BASIC EXAMPLE validation)
    assertThat(compilationUnit).isNotNull();
    assertThat(compilationUnit.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(compilationUnit.getJavaChunk()).isEqualTo(sourceContent);

    // Access package declaration
    assertThat(compilationUnit.getPackageDeclaration()).isNotNull();
    assertThat(compilationUnit.getPackageDeclaration().getPackageName())
        .isEqualTo("com.example.animals");

    // Access class declaration
    assertThat(compilationUnit.getClassDeclarations()).hasSize(1);
    TypeDeclarationNodeV1 classNode = compilationUnit.getClassDeclarations().get(0);
    assertThat(classNode.getName()).isEqualTo("Animal");
  }

  /**
   * Tests advanced analysis functionality showing how to extract detailed information from parsed
   * Java code. Demonstrates method signature analysis and Javadoc extraction, which are key
   * capabilities for code review automation.
   *
   * @throws IOException if the test resource file cannot be read
   */
  @Test
  void shouldAnalyzeMethodSignaturesAndDocumentation() throws IOException {
    // Given: Load the Animal.java test file
    String repositoryPath = "src/test/resources/example_classes/Animal.java";
    Path animalFile = Path.of(repositoryPath);
    String sourceContent = Files.readString(animalFile);

    // When: Parse and analyze the compilation unit (ADVANCED EXAMPLE from README)
    CompilationUnitNodeV1 unit =
        javaParsingServiceV1.parseCompilationUnit(sourceContent, repositoryPath);

    // Then: Analyze class structure and methods
    TypeDeclarationNodeV1 classNode = unit.getClassDeclarations().get(0);
    assertThat(classNode.getName()).isEqualTo("Animal");

    // Analyze method signatures and documentation
    assertThat(classNode.getMethodDeclarations()).hasSize(4); // constructor + 3 methods

    // Find specific methods by name pattern
    MethodDeclarationNodeV1 getIdMethod =
        classNode.getMethodDeclarations().stream()
            .filter(method -> method.getName().equals("getId"))
            .findFirst()
            .orElseThrow();

    assertThat(getIdMethod.getSignature()).isEqualTo("getId()");
    assertThat(getIdMethod.getJavadoc()).isNotNull();
    assertThat(getIdMethod.getJavadoc().javaCodeChunk()).contains("Gets the unique identifier");

    // Analyze setter method
    MethodDeclarationNodeV1 setAnimalTypeMethod =
        classNode.getMethodDeclarations().stream()
            .filter(method -> method.getName().equals("setAnimalTypeName"))
            .findFirst()
            .orElseThrow();

    assertThat(setAnimalTypeMethod.getSignature()).isEqualTo("setAnimalTypeName(String)");
    assertThat(setAnimalTypeMethod.getJavadoc()).isNotNull();
    assertThat(setAnimalTypeMethod.getJavadoc().javaCodeChunk()).contains("Sets the type name");
  }

  /**
   * Tests the integration pattern showing how to access detailed AST information when the
   * simplified AST doesn't provide enough detail. Demonstrates the bridge pattern between
   * simplified and full AST analysis.
   *
   * @throws IOException if the test resource file cannot be read
   */
  @Test
  void shouldAccessOriginalAstForDetailedAnalysis() throws IOException {
    // Given: Load the Animal.java test file (contains JPA annotations)
    String repositoryPath = "src/test/resources/example_classes/Animal.java";
    Path animalFile = Path.of(repositoryPath);
    String sourceContent = Files.readString(animalFile);

    // When: Parse the source and access original AST for detailed analysis
    CompilationUnitNodeV1 unit =
        javaParsingServiceV1.parseCompilationUnit(sourceContent, repositoryPath);

    // Then: Use original AST for complex analysis not available in simplified AST
    com.github.javaparser.ast.CompilationUnit originalAst = unit.getOriginalNode();

    // Access full type information, modifiers, and annotations via original AST
    assertThat(originalAst.getTypes()).hasSize(1);
    com.github.javaparser.ast.body.TypeDeclaration<?> originalType = originalAst.getTypes().get(0);

    // The original AST contains detailed information like modifiers and annotations
    // that are not exposed in the simplified AST for performance reasons
    assertThat(originalType.getModifiers()).isNotEmpty();
    assertThat(originalType.getAnnotations()).isNotEmpty();

    // Verify we can access JPA annotations through original AST
    boolean hasEntityAnnotation =
        originalType.getAnnotations().stream()
            .anyMatch(annotation -> annotation.getNameAsString().equals("Entity"));
    assertThat(hasEntityAnnotation).isTrue();
  }

  /**
   * Tests practical code analysis scenarios that would be useful for automated code review.
   * Demonstrates how to extract meaningful information for quality analysis and documentation
   * verification.
   *
   * @throws IOException if the test resource file cannot be read
   */
  @Test
  void shouldExtractCodeQualityMetrics() throws IOException {
    // Given: Load the Animal.java test file
    String repositoryPath = "src/test/resources/example_classes/Animal.java";
    Path animalFile = Path.of(repositoryPath);
    String sourceContent = Files.readString(animalFile);

    // When: Analyze code for quality metrics
    CompilationUnitNodeV1 unit =
        javaParsingServiceV1.parseCompilationUnit(sourceContent, repositoryPath);
    TypeDeclarationNodeV1 classNode = unit.getClassDeclarations().get(0);

    // Then: Extract useful metrics for code review
    // Note: Simplified AST doesn't include modifiers, so we count all methods
    long totalMethodCount = classNode.getMethodDeclarations().size();
    assertThat(totalMethodCount).isEqualTo(4); // constructor + 3 methods

    // Check for Javadoc coverage
    long documentedMethods =
        classNode.getMethodDeclarations().stream()
            .filter(method -> method.getJavadoc() != null)
            .count();
    assertThat(documentedMethods).isEqualTo(4); // All methods including constructor are documented

    // Analyze field declarations
    assertThat(classNode.getFieldDeclarations()).hasSize(2); // id and animalTypeName fields

    // Verify class has proper structure for analysis
    assertThat(classNode.getName()).isEqualTo("Animal");
    assertThat(unit.getPackageDeclaration().getPackageName()).isEqualTo("com.example.animals");
  }
}
