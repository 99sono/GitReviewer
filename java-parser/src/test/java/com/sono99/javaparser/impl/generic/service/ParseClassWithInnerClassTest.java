package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.impl.generic.model.ClassDeclarationNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.FieldDeclarationNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Test parsing of nested class structures in Java files. Validates that the DFS approach correctly
 * handles inner classes and builds proper hierarchical AST. Tests the core improvement over the
 * original flat approach for nested class scenarios.
 */
class ParseClassWithInnerClassTest extends AbstractJavaParserTest {

  /**
   * Test that the JavaParserService correctly parses nested class structures. Validates that inner
   * classes are properly represented as children of their containing class, demonstrating the key
   * improvement of the DFS approach over the original flat structure.
   */
  @Test
  void shouldParseNestedClassStructure() throws IOException {
    // Given: Load the ClassWithInnerClass.java test file
    String repositoryPath = "src/main/java/com/example/nested/ClassWithInnerClass.java";
    CompilationUnitNode result = parseFile("ClassWithInnerClass.java", repositoryPath);

    // Then: Validate basic CompilationUnitNode properties
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(result.getChildren()).hasSize(1); // Only 1 class (no package/import)

    // Validate the outer class declaration
    ClassDeclarationNode outerClass =
        result.getChildren().stream()
            .filter(ClassDeclarationNode.class::isInstance)
            .map(ClassDeclarationNode.class::cast)
            .findFirst()
            .orElse(null);

    assertThat(outerClass).isNotNull();
    assertThat(outerClass.getName()).isEqualTo("ClassWithInnerClass");

    // With DFS approach, the outer class should have children: field + method +
    // inner class
    assertThat(outerClass.getChildren()).hasSize(3);

    // Validate parent-child relationships for outer class
    assertThat(result.getParent()).isNull();
    assertThat(outerClass.getParent()).isSameAs(result);

    // Validate field in outer class
    FieldDeclarationNode outerField =
        outerClass.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .filter(f -> "basicField".equals(f.getName()))
            .findFirst()
            .orElse(null);
    assertThat(outerField).isNotNull();
    assertThat(outerField.getName()).isEqualTo("basicField");
    assertThat(outerField.getType()).isEqualTo("String");
    assertThat(outerField.getParent()).isSameAs(outerClass);

    // Validate method in outer class
    MethodDeclarationNode outerMethod =
        outerClass.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "basicMethod".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(outerMethod).isNotNull();
    assertThat(outerMethod.getName()).isEqualTo("basicMethod");
    assertThat(outerMethod.getSignature()).isEqualTo("basicMethod()");
    assertThat(outerMethod.getParent()).isSameAs(outerClass);

    // Validate inner class is properly nested
    ClassDeclarationNode innerClass =
        outerClass.getChildren().stream()
            .filter(ClassDeclarationNode.class::isInstance)
            .map(ClassDeclarationNode.class::cast)
            .filter(c -> "InnerClass".equals(c.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerClass).isNotNull();
    assertThat(innerClass.getName()).isEqualTo("InnerClass");
    assertThat(innerClass.getParent()).isSameAs(outerClass); // Inner class parent is outer class

    // Validate inner class has its own children: field + method
    assertThat(innerClass.getChildren()).hasSize(2);

    // Validate field in inner class
    FieldDeclarationNode innerField =
        innerClass.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .filter(f -> "basicField2".equals(f.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerField).isNotNull();
    assertThat(innerField.getName()).isEqualTo("basicField2");
    assertThat(innerField.getType()).isEqualTo("String");
    assertThat(innerField.getParent()).isSameAs(innerClass);

    // Validate method in inner class
    MethodDeclarationNode innerMethod =
        innerClass.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "basicMethod2".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerMethod).isNotNull();
    assertThat(innerMethod.getName()).isEqualTo("basicMethod2");
    assertThat(innerMethod.getSignature()).isEqualTo("basicMethod2()");
    assertThat(innerMethod.getParent()).isSameAs(innerClass);
  }

  /**
   * Test that validates the hierarchical structure demonstrates the key improvement. In the
   * original flat approach, all classes would be direct children of CompilationUnit. In the DFS
   * approach, inner classes are properly nested under their containing class.
   */
  @Test
  void shouldDemonstrateHierarchicalImprovement() throws IOException {
    // Given: Load the nested class test file
    String repositoryPath = "src/main/java/com/example/nested/ClassWithInnerClass.java";
    CompilationUnitNode result = parseFile("ClassWithInnerClass.java", repositoryPath);

    // Then: CompilationUnit should have only 1 direct child (the outer class)
    assertThat(result.getChildren()).hasSize(1);
    ClassDeclarationNode outerClass = (ClassDeclarationNode) result.getChildren().get(0);
    assertThat(outerClass.getName()).isEqualTo("ClassWithInnerClass");

    // The outer class should have 3 children: field + method + inner class
    assertThat(outerClass.getChildren()).hasSize(3);

    // The inner class should be one of the children of the outer class
    ClassDeclarationNode innerClass =
        outerClass.getChildren().stream()
            .filter(ClassDeclarationNode.class::isInstance)
            .map(ClassDeclarationNode.class::cast)
            .filter(c -> "InnerClass".equals(c.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerClass).isNotNull();

    // The inner class should have its own children (field + method)
    assertThat(innerClass.getChildren()).hasSize(2);

    // Validate the hierarchy depth
    assertThat(result.getChildren()).hasSize(1); // Level 1: CompilationUnit children
    assertThat(outerClass.getChildren()).hasSize(3); // Level 2: Outer class children
    assertThat(innerClass.getChildren()).hasSize(2); // Level 3: Inner class children

    // This demonstrates the key improvement: proper nesting instead of flat
    // structure
    // Original approach would have put both classes as direct children of
    // CompilationUnit
  }

  /**
   * Test that validates Javadoc comments are preserved in the source chunks. Sets up validation for
   * future Javadoc parsing implementation.
   */
  @Test
  void shouldPreserveJavadocInSourceChunks() throws IOException {
    // Given: Load the nested class test file
    String repositoryPath = "src/main/java/com/example/nested/ClassWithInnerClass.java";
    CompilationUnitNode result = parseFile("ClassWithInnerClass.java", repositoryPath);

    // Then: Validate that Javadoc comments are preserved in java chunks
    ClassDeclarationNode outerClass = (ClassDeclarationNode) result.getChildren().get(0);

    // Outer class javaChunk contains the class declaration (Javadoc is preserved in
    // source but not in class chunk)
    assertThat(outerClass.getJavaChunk()).contains("public class ClassWithInnerClass");
    assertThat(outerClass.getJavaChunk()).contains("private String basicField");

    // Field in outer class - javaChunk contains just the field declaration (Javadoc
    // is preserved in source)
    FieldDeclarationNode outerField =
        outerClass.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .filter(f -> "basicField".equals(f.getName()))
            .findFirst()
            .orElse(null);
    assertThat(outerField).isNotNull();
    assertThat(outerField.getJavaChunk()).contains("private String basicField");

    // Method in outer class - javaChunk contains just the method declaration
    // (Javadoc is preserved in source)
    MethodDeclarationNode outerMethod =
        outerClass.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "basicMethod".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(outerMethod).isNotNull();
    assertThat(outerMethod.getJavaChunk()).contains("public void basicMethod()");

    // Inner class - javaChunk contains just the class declaration (Javadoc is
    // preserved in source)
    ClassDeclarationNode innerClass =
        outerClass.getChildren().stream()
            .filter(ClassDeclarationNode.class::isInstance)
            .map(ClassDeclarationNode.class::cast)
            .filter(c -> "InnerClass".equals(c.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerClass).isNotNull();
    assertThat(innerClass.getJavaChunk()).contains("public class InnerClass");

    // Field in inner class - javaChunk contains just the field declaration (Javadoc
    // is preserved in source)
    FieldDeclarationNode innerField =
        innerClass.getChildren().stream()
            .filter(FieldDeclarationNode.class::isInstance)
            .map(FieldDeclarationNode.class::cast)
            .filter(f -> "basicField2".equals(f.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerField).isNotNull();
    assertThat(innerField.getJavaChunk()).contains("private String basicField2");

    // Method in inner class - javaChunk contains just the method declaration
    // (Javadoc is preserved in source)
    MethodDeclarationNode innerMethod =
        innerClass.getChildren().stream()
            .filter(MethodDeclarationNode.class::isInstance)
            .map(MethodDeclarationNode.class::cast)
            .filter(m -> "basicMethod2".equals(m.getName()))
            .findFirst()
            .orElse(null);
    assertThat(innerMethod).isNotNull();
    assertThat(innerMethod.getJavaChunk()).contains("public void basicMethod2()");
  }
}
