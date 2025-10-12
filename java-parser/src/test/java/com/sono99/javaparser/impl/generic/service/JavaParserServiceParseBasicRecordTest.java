package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.body.RecordDeclaration;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Test that the JavaParserService correctly parses Java record classes. Ensures that these language
 * constructs are properly represented in the internal AST model.
 */
class JavaParserServiceParseBasicRecordTest extends AbstractJavaParserTest {

  /**
   * Test that a basic Java record class is correctly parsed. Verifies that the record is identified
   * as a TypeDeclarationNode and its original node is a RecordDeclaration.
   */
  @Test
  void shouldParseBasicRecordClass() throws IOException {
    // Given: Load the BasicRecord.java test file
    String repositoryPath = "src/main/java/com/example/records/BasicRecord.java";
    CompilationUnitNode result = parseFile("BasicRecord.java", repositoryPath);

    // Then: Validate basic CompilationUnitNode properties
    assertThat(result).isNotNull();
    // Filter for TypeDeclarationNode instances, as package declarations are not considered direct
    assertThat(result.getChildren().stream().filter(TypeDeclarationNode.class::isInstance).toList())
        .hasSize(1); // Expecting one top-level declaration

    // Validate the record declaration
    TypeDeclarationNode recordNode =
        result.getChildren().stream()
            .filter(TypeDeclarationNode.class::isInstance)
            .map(TypeDeclarationNode.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(recordNode).isNotNull();
    assertThat(recordNode.getName()).isEqualTo("BasicRecord");
    assertThat(recordNode.getParent()).isSameAs(result);

    // Verify that the original node is indeed a RecordDeclaration
    assertThat(recordNode.getOriginalNode()).isInstanceOf(RecordDeclaration.class);
    RecordDeclaration originalRecord = (RecordDeclaration) recordNode.getOriginalNode();
    assertThat(originalRecord.getNameAsString()).isEqualTo("BasicRecord");

    // Check for components of the record (e.g., compact constructor, methods)
    // The record has a compact constructor and one explicit method 'getInfo()'
    assertThat(recordNode.getChildren()).hasSize(1); // Only getInfo() method is explicitly declared

    MethodDeclarationNode getInfoMethod =
        recordNode.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "getInfo".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(getInfoMethod).isNotNull();
    assertThat(getInfoMethod.getSignature()).isEqualTo("getInfo()");
    assertThat(getInfoMethod.getParent()).isSameAs(recordNode);
  }
}
