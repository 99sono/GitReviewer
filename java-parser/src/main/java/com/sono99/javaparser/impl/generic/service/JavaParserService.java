package com.sono99.javaparser.impl.generic.service;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.sono99.javaparser.impl.generic.model.AbstractJavaNode;
import com.sono99.javaparser.impl.generic.model.AnnotationNode;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.model.FieldDeclarationNode;
import com.sono99.javaparser.impl.generic.model.ImportDeclarationNode;
import com.sono99.javaparser.impl.generic.model.JavadocNode;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.generic.model.PackageDeclarationNode;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import com.sono99.javaparser.impl.generic.utils.SourceContentHelper;
import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Core JavaParser service implementation using Depth-First Search (DFS) approach. Builds AST
 * representations from source code using recursive bottom-up traversal. This approach properly
 * handles nested structures like inner classes and creates hierarchical parent-child relationships.
 */
@Service
public class JavaParserService {

  @Autowired private SourceContentUtils sourceUtils; // No field Javadoc here

  /**
   * Sets the SourceContentUtils for testing purposes. Normally this is injected by Spring, but for
   * unit testing we need manual injection.
   */
  public void setSourceUtils(SourceContentUtils sourceUtils) {
    this.sourceUtils = sourceUtils;
  }

  /**
   * Parses Java source from InputStream and returns the raw GitHub parser CompilationUnit. This is
   * a convenience API for advanced use cases where direct access to the GitHub parser AST is needed
   * for complex git review use cases.
   *
   * <p>Note: For code review purposes, the {@link #parseCompilationUnit(InputStream, String)}
   * method returning {@link CompilationUnitNode} is normally preferred as it provides a much
   * simpler AST structure that fits many use cases.
   *
   * @param source the InputStream containing Java source code
   * @param repositoryPath the repository-relative path for the source (e.g.,
   *     src/main/java/com/example/MyClass.java)
   * @return CompilationUnit the raw GitHub parser AST node for advanced processing
   * @throws RuntimeException if parsing fails due to syntax errors or IO issues
   */
  public CompilationUnit parseGitHubCompilationUnit(InputStream source, String repositoryPath) {
    try {
      // (a) Read full source content first
      String javaCodeChunk = readSourceAsString(source);

      StaticJavaParser.getParserConfiguration()
          .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);

      // (c) Parse source to GitHub parser CompilationUnit and return immediately
      CompilationUnit gitHubParserCompilationUnitNode = StaticJavaParser.parse(javaCodeChunk);

      return gitHubParserCompilationUnitNode;

    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Java source: " + repositoryPath, e);
    }
  }

  /**
   * Parses Java source from String and returns root CompilationUnitNode. Convenience overload for
   * string-based source (internally converts to InputStream).
   *
   * @param source the Java source code as a string
   * @param repositoryPath the repository-relative path for the source
   * @return CompilationUnitNode representing the root of the AST
   * @throws RuntimeException if parsing fails due to syntax errors
   */
  public CompilationUnitNode parseCompilationUnit(String source, String repositoryPath) {
    return parseCompilationUnit(
        new java.io.ByteArrayInputStream(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        repositoryPath);
  }

  /**
   * Parses Java source from InputStream and returns root CompilationUnitNode. Uses DFS approach to
   * build proper hierarchical structure with parent-child relationships.
   *
   * @param source the InputStream containing Java source code
   * @param repositoryPath the repository-relative path for the source (e.g.,
   *     src/main/java/com/example/MyClass.java)
   * @return CompilationUnitNode representing the root of the AST with proper hierarchical children
   *     (fields/methods as children of classes)
   * @throws RuntimeException if parsing fails due to syntax errors or IO issues
   */
  public CompilationUnitNode parseCompilationUnit(InputStream source, String repositoryPath) {
    try {
      // (a) Read full source content first
      String javaCodeChunk = readSourceAsString(source);

      // (b) Create SourceContentHelper for efficient text operations
      SourceContentHelper contentHelper = sourceUtils.createHelper(javaCodeChunk);

      // (c) Configure JavaParser to use JAVA_21 language level
      StaticJavaParser.getParserConfiguration().setLanguageLevel(JAVA_21);

      // (d) Parse source to JavaParser CompilationUnit
      CompilationUnit gitHubParserCompilationUnitNode = StaticJavaParser.parse(javaCodeChunk);

      // (d) Extract basic information for root node
      int startLine = 1;
      int endLine = 1;

      if (gitHubParserCompilationUnitNode.getRange().isPresent()) {
        Range range = gitHubParserCompilationUnitNode.getRange().get();
        endLine = range.end.line;
      }

      // (e) Use DFS to build hierarchical structure for children only
      List<AbstractJavaNode<? extends Node>> children = new ArrayList<>();

      // Process direct children of CompilationUnit
      for (Node childNode : gitHubParserCompilationUnitNode.getChildNodes()) {
        AbstractJavaNode<? extends Node> child = buildNodeHierarchy(childNode, contentHelper);
        if (child != null) {
          children.add(child);
        }
      }

      // (f) Create final root CompilationUnitNode with hierarchical children
      CompilationUnitNode root =
          new CompilationUnitNode(
              startLine,
              endLine,
              javaCodeChunk,
              gitHubParserCompilationUnitNode,
              repositoryPath,
              children);

      // (g) Establish parent relationships
      for (AbstractJavaNode<? extends Node> child : children) {
        child.setParent(root);
      }

      return root;

    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Java source: " + repositoryPath, e);
    }
  }

  /**
   * Core DFS method that recursively builds the node hierarchy. Returns created node if we support
   * the type, null otherwise.
   */
  private AbstractJavaNode<? extends Node> buildNodeHierarchy(
      Node gitHubNode, SourceContentHelper helper) {
    // (a) First, recursively process all children (bottom-up approach)
    List<AbstractJavaNode<? extends Node>> children = new ArrayList<>();

    for (Node childNode : gitHubNode.getChildNodes()) {
      AbstractJavaNode<? extends Node> child = buildNodeHierarchy(childNode, helper);
      if (child != null) {
        children.add(child);
      }
    }

    // (b) Create current node based on type
    AbstractJavaNode<? extends Node> currentNode = createNode(gitHubNode, children, helper);

    // (c) Set parent references for all children
    if (currentNode != null) {
      for (AbstractJavaNode<? extends Node> child : children) {
        child.setParent(currentNode);
      }
    }

    return currentNode;
  }

  /**
   * Creates appropriate model node based on GitHub JavaParser node type. Returns null if node type
   * is not supported.
   */
  private AbstractJavaNode<? extends Node> createNode(
      Node gitHubNode,
      List<AbstractJavaNode<? extends Node>> children,
      SourceContentHelper helper) {

    // Handle PackageDeclaration
    if (gitHubNode instanceof PackageDeclaration pkg) {
      return createPackageDeclarationNode(pkg, helper);
    }

    // Handle ImportDeclaration
    if (gitHubNode instanceof ImportDeclaration imp) {
      return createImportDeclarationNode(imp, helper);
    }

    // Handle AnnotationExpr
    if (gitHubNode instanceof AnnotationExpr annotationExpr) {
      return createAnnotationNode(annotationExpr, helper);
    }

    // Handle TypeDeclaration (classes, interfaces, enums, records)
    if (gitHubNode instanceof TypeDeclaration<?> typeDecl) {
      return createTypeDeclarationNode(typeDecl, children, helper);
    }

    // Handle FieldDeclaration
    if (gitHubNode instanceof FieldDeclaration field) {
      return createFieldDeclarationNode(field, children, helper);
    }

    // Handle MethodDeclaration
    if (gitHubNode instanceof MethodDeclaration method) {
      return createMethodDeclarationNode(method, children, helper);
    }

    // Return null for unsupported node types (statements, expressions, etc.)
    return null;
  }

  /**
   * Creates JavadocNode from TypeDeclaration if Javadoc comment exists. Returns null if no Javadoc
   * comment is present.
   */
  private JavadocNode createJavadocNodeForType(
      TypeDeclaration<?> typeDecl, SourceContentHelper helper) {
    var javadocOpt = typeDecl.getJavadocComment();
    if (javadocOpt.isPresent()) {
      var javadoc = javadocOpt.get();
      var range = javadoc.getRange().orElse(null);
      if (range != null) {
        String javadocContent =
            sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);
        return new JavadocNode(
            range.begin.line, range.end.line, javadocContent, javadoc, List.of());
      }
    }
    return null;
  }

  /**
   * Creates JavadocNode from FieldDeclaration if Javadoc comment exists. Returns null if no Javadoc
   * comment is present.
   */
  private JavadocNode createJavadocNodeForField(
      FieldDeclaration field, SourceContentHelper helper) {
    var javadocOpt = field.getJavadocComment();
    if (javadocOpt.isPresent()) {
      var javadoc = javadocOpt.get();
      var range = javadoc.getRange().orElse(null);
      if (range != null) {
        String javadocContent =
            sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);
        return new JavadocNode(
            range.begin.line, range.end.line, javadocContent, javadoc, List.of());
      }
    }
    return null;
  }

  /**
   * Creates JavadocNode from MethodDeclaration if Javadoc comment exists. Returns null if no
   * Javadoc comment is present.
   */
  private JavadocNode createJavadocNodeForMethod(
      MethodDeclaration method, SourceContentHelper helper) {
    var javadocOpt = method.getJavadocComment();
    if (javadocOpt.isPresent()) {
      var javadoc = javadocOpt.get();
      var range = javadoc.getRange().orElse(null);
      if (range != null) {
        String javadocContent =
            sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);
        return new JavadocNode(
            range.begin.line, range.end.line, javadocContent, javadoc, List.of());
      }
    }
    return null;
  }

  /** Creates PackageDeclarationNode from GitHub PackageDeclaration. */
  private PackageDeclarationNode createPackageDeclarationNode(
      PackageDeclaration pkg, SourceContentHelper helper) {
    Range range = pkg.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String packageChunk = sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    return new PackageDeclarationNode(
        range.begin.line,
        range.end.line,
        pkg.getName().asString(),
        packageChunk,
        pkg,
        List.of() // leaf node
        );
  }

  /** Creates ImportDeclarationNode from GitHub ImportDeclaration. */
  private ImportDeclarationNode createImportDeclarationNode(
      ImportDeclaration imp, SourceContentHelper helper) {
    Range range = imp.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String importChunk = sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    return new ImportDeclarationNode(
        range.begin.line,
        range.end.line,
        imp.getName().asString(),
        imp.isStatic(),
        imp.isAsterisk(),
        importChunk,
        imp,
        List.of() // leaf node
        );
  }

  /** Creates TypeDeclarationNode from GitHub TypeDeclaration. */
  private TypeDeclarationNode createTypeDeclarationNode(
      TypeDeclaration<?> typeDecl,
      List<AbstractJavaNode<? extends Node>> children,
      SourceContentHelper helper) {
    String name = typeDecl.getNameAsString();
    Range range = typeDecl.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String classChunk = sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    // Extract Javadoc if present
    JavadocNode javadoc = createJavadocNodeForType(typeDecl, helper);

    return new TypeDeclarationNode(
        range.begin.line, range.end.line, name, classChunk, typeDecl, javadoc, children);
  }

  /** Creates FieldDeclarationNode from GitHub FieldDeclaration. */
  private FieldDeclarationNode createFieldDeclarationNode(
      FieldDeclaration field,
      List<AbstractJavaNode<? extends Node>> children,
      SourceContentHelper helper) {
    Range range = field.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String fieldChunk = sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    // Extract field information
    String fieldName = field.getVariables().get(0).getNameAsString();
    String fieldType = field.getElementType().asString();

    // Extract Javadoc for field
    JavadocNode javadoc = createJavadocNodeForField(field, helper);

    return new FieldDeclarationNode(
        range.begin.line,
        range.end.line,
        fieldName,
        fieldType,
        fieldChunk,
        field,
        javadoc,
        children // field annotations and other children
        );
  }

  /** Creates MethodDeclarationNode from GitHub MethodDeclaration. */
  private MethodDeclarationNode createMethodDeclarationNode(
      MethodDeclaration method,
      List<AbstractJavaNode<? extends Node>> children,
      SourceContentHelper helper) {
    Range range = method.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String methodChunk = sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    String methodName = method.getNameAsString();
    String signature = method.getSignature().asString();

    // Extract Javadoc for method
    JavadocNode javadoc = createJavadocNodeForMethod(method, helper);

    return new MethodDeclarationNode(
        range.begin.line,
        range.end.line,
        methodName,
        signature,
        methodChunk,
        method,
        javadoc,
        children // method body contents
        );
  }

  /** Creates AnnotationNode from GitHub AnnotationExpr. */
  private AnnotationNode createAnnotationNode(
      AnnotationExpr annotationExpr, SourceContentHelper helper) {
    var range = annotationExpr.getRange().orElse(null);
    if (range == null) {
      return null;
    }

    String annotationText =
        sourceUtils.extractSourceRange(helper, range.begin.line, range.end.line);

    return new AnnotationNode(
        range.begin.line,
        range.end.line,
        annotationExpr.getNameAsString(),
        annotationText,
        annotationExpr,
        List.of() // annotations have no children
        );
  }

  /** Helper method to read entire InputStream as String. */
  private static String readSourceAsString(InputStream source) throws IOException {
    return new String(source.readAllBytes(), StandardCharsets.UTF_8);
  }
}
