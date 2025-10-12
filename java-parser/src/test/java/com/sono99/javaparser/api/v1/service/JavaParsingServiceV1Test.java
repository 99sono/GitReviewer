package com.sono99.javaparser.api.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
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
 * Integration tests for {@link JavaParsingServiceV1}. Verifies the correct functionality of the
 * Java parsing API using a Spring Boot test context.
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
class JavaParsingServiceV1Test {

  @Autowired private JavaParsingServiceV1 javaParsingServiceV1;

  /**
   * Tests that the {@link JavaParsingServiceV1#parseCompilationUnit(String, String)} method
   * successfully parses a basic Java interface file and extracts its key components. This test uses
   * {@code BasicInterface.java} as a test resource.
   *
   * @throws IOException if the test resource file cannot be read
   */
  @Test
  void shouldParseBasicInterfaceSuccessfully() throws IOException {
    // Given: Load the BasicInterface.java test file
    String repositoryPath = "src/test/resources/example_classes/BasicInterface.java";
    Path basicInterfaceFile = Path.of("src/test/resources/example_classes/BasicInterface.java");
    String sourceContent = Files.readString(basicInterfaceFile);

    // When: Parse the source content using JavaParsingServiceV1
    CompilationUnitNodeV1 result =
        javaParsingServiceV1.parseCompilationUnit(sourceContent, repositoryPath);

    // Then: Validate the parsed result
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryPath()).isEqualTo(repositoryPath);
    assertThat(result.getJavaChunk()).isEqualTo(sourceContent);
    assertThat(result.getStartLine()).isEqualTo(1);
    assertThat(result.getEndLine()).isEqualTo(15);

    // Should have 1 package declaration + 1 interface declaration
    assertThat(result.getChildren()).hasSize(2);

    assertThat(result.getPackageDeclaration()).isNotNull();
    assertThat(result.getPackageDeclaration().getPackageName()).isEqualTo("com.example.interfaces");

    TypeDeclarationNodeV1 interfaceNode = result.getClassDeclarations().get(0);
    assertThat(interfaceNode).isNotNull();
    assertThat(interfaceNode.getName()).isEqualTo("BasicInterface");
  }
}
