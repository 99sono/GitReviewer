package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Test that the JavaParserService correctly parses Java record classes and interfaces. Ensures that
 * these language constructs are properly represented in the internal AST model.
 */
class ParseInterfaceTest extends AbstractJavaParserTest {

  /**
   * Test that a basic Java interface is correctly parsed. Verifies that the interface is identified
   * as a TypeDeclarationNode and its original node is a ClassOrInterfaceDeclaration representing an
   * interface.
   */
  @Test
  void shouldParseBasicInterface() throws IOException {
    // Given: Load the BasicInterface.java test file
    String repositoryPath = "src/main/java/com/example/interfaces/BasicInterface.java";
    CompilationUnitNode result = parseFile("BasicInterface.java", repositoryPath);

    // Then: Validate basic CompilationUnitNode properties
    assertThat(result).isNotNull();
    // Filter for TypeDeclarationNode instances, as package declarations are not considered
    assertThat(result.getChildren().stream().filter(TypeDeclarationNode.class::isInstance).toList())
        .hasSize(1); // Expecting one top-level declaration

    // Validate the interface declaration
    TypeDeclarationNode interfaceNode =
        result.getChildren().stream()
            .filter(TypeDeclarationNode.class::isInstance)
            .map(TypeDeclarationNode.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(interfaceNode).isNotNull();
    assertThat(interfaceNode.getName()).isEqualTo("BasicInterface");
    assertThat(interfaceNode.getParent()).isSameAs(result);

    // Verify that the original node is indeed a ClassOrInterfaceDeclaration and is an interface
    assertThat(interfaceNode.getOriginalNode()).isInstanceOf(ClassOrInterfaceDeclaration.class);
    ClassOrInterfaceDeclaration originalInterface =
        (ClassOrInterfaceDeclaration) interfaceNode.getOriginalNode();
    assertThat(originalInterface.isInterface()).isTrue();
    assertThat(originalInterface.getNameAsString()).isEqualTo("BasicInterface");

    // Check for components of the interface (e.g., methods and field)
    assertThat(interfaceNode.getChildren()).hasSize(3); // DEFAULT_MESSAGE field, getMessage()
    // method, and printMessage() default
    // method

    MethodDeclarationNode getMessageMethod =
        interfaceNode.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "getMessage".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(getMessageMethod).isNotNull();
    assertThat(getMessageMethod.getSignature()).isEqualTo("getMessage()");
    assertThat(getMessageMethod.getParent()).isSameAs(interfaceNode);

    MethodDeclarationNode printMessageMethod =
        interfaceNode.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "printMessage".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(printMessageMethod).isNotNull();
    assertThat(printMessageMethod.getSignature()).isEqualTo("printMessage()");
    assertThat(printMessageMethod.getParent()).isSameAs(interfaceNode);
  }
}
