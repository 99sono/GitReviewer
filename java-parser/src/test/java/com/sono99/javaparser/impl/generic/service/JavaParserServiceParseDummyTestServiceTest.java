package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.impl.generic.model.AnnotationNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test parsing of Java files into CompilationUnitNode ASTs with granular validation of package and
 * import children. Validates specific content and structure for DummyTestService.java to ensure
 * generalization of parsing logic. Tests a simpler service class with basic annotations and imports
 * to verify no hard-coded assumptions exist.
 */
class JavaParserServiceParseDummyTestServiceTest extends AbstractJavaParserTest {

  /**
   * Test that the JavaParserService correctly parses the DummyTestService Java source file and
   * creates a valid CompilationUnitNode with proper line ranges and source content. Ensures the
   * parsing logic works for different class structures beyond the complex Animal.java example,
   * confirming no hard-coded assumptions exist.
   */
  @Test
  void shouldParseDummyTestServiceJavaFile() throws IOException {
    // Given: Load the DummyTestService.java test file
    String repositoryPath = "src/main/java/com/example/service/DummyTestService.java";
    CompilationUnitNode result = parseFile("DummyTestService.java", repositoryPath);
    String sourceContent =
        Files.readString(Path.of("src/test/resources/example_classes/DummyTestService.java"));

    // Then: Validate basic CompilationUnitNode properties using common method
    validateBasicProperties(result, sourceContent, repositoryPath);

    // Should have children: 1 package + 2 imports + 1 class
    assertThat(result.getChildren()).hasSize(4);

    // Validate specific package declaration using common method
    assertPackageDeclaration(result, "package com.example.service;");

    // Validate specific imports - different from Animal.java to ensure
    // generalization
    assertImportExists(result.getChildren(), "java.util.List", false, false);
    assertImportExists(
        result.getChildren(), "org.springframework.stereotype.Service", false, false);

    // Validate class declaration is now included
    assertThat(result.getChildren()).hasSize(4); // 1 package + 2 imports + 1 class

    // Validate the class declaration
    TypeDeclarationNode classNode =
        result.getChildren().stream()
            .filter(TypeDeclarationNode.class::isInstance)
            .map(TypeDeclarationNode.class::cast)
            .findFirst()
            .orElse(null);

    assertThat(classNode).isNotNull();
    assertThat(classNode.getName()).isEqualTo("DummyTestService");

    // Validate line range behavior: getRange() includes class annotations
    // This is CORRECT JavaParser behavior - annotations are part of class
    // declaration
    assertThat(classNode.getStartLine()).isEqualTo(11); // Starts at @Service (correct)
    assertThat(classNode.getEndLine()).isEqualTo(32); // Ends at closing brace

    // Validate javaChunk contains the complete annotated class declaration
    assertThat(classNode.getJavaChunk())
        .contains("@Service") // Class annotation
        .contains("public class DummyTestService") // Class declaration
        .contains("public int processItems(List<String> items)") // Method declaration
        .contains("public boolean validateConfig()") // Another method
        .endsWith("}"); // Class closing brace

    // Validate class-level annotations
    List<AnnotationNode> classAnnotations =
        classNode.getChildren().stream()
            .filter(AnnotationNode.class::isInstance)
            .map(AnnotationNode.class::cast)
            .toList();
    assertThat(classAnnotations).hasSize(1);
    assertThat(
            classAnnotations.stream()
                .anyMatch(
                    ann ->
                        ann.getName().equals("Service") && ann.getJavaChunk().equals("@Service")))
        .isTrue();

    // Current implementation: annotations in javaChunk but not parsed to
    // AnnotationNode yet (Step A)

    // With DFS approach, methods and annotations are correctly children of the class
    assertThat(classNode.getChildren()).isNotEmpty(); // Now has method and annotation children
    assertThat(classNode.getChildren())
        .hasSize(3); // 1 class annotation + 2 methods in DummyTestService

    // Validate that we have the expected method children
    long methodCount =
        classNode.getChildren().stream()
            .filter(
                child ->
                    child instanceof com.sono99.javaparser.impl.generic.model.MethodDeclarationNode)
            .count();

    assertThat(methodCount).isEqualTo(2); // processItems and validateConfig methods

    // Validate parent relationships
    assertThat(result.getParent()).isNull();
    assertThat(classNode.getParent()).isSameAs(result);

    // Validate overall import statistics for this simpler class
    assertImportStatistics(result.getChildren());
  }

  /**
   * Test that the JavaParserService correctly handles another minimal but valid Java source code
   * example. Ensures the parser can handle different package structures and import combinations to
   * verify generalization of parsing logic.
   */
  @Test
  void shouldParseMinimalJavaFileWithDifferentPackage() {
    // Given: Minimal valid Java source with different package
    String minimalJava =
        """
                package com.example.service;
                import java.util.List;
                public class SimpleService {
                }
                """;
    String repositoryPath = "src/test/java/com/example/service/SimpleService.java";

    // When: Parse the minimal source using common method
    CompilationUnitNode result =
        createParserService()
            .parseCompilationUnit(new ByteArrayInputStream(minimalJava.getBytes()), repositoryPath);

    // Then: Basic validation using common method
    validateBasicProperties(result, minimalJava, repositoryPath);
    assertThat(result.getChildren()).hasSize(3); // Has 1 package + 1 import + 1 class
    assertThat(result.getParent()).isNull();
  }
}
