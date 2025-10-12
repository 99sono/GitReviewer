# Java Parser Module

This module provides a minimalist Abstract Syntax Tree (AST) representation for Java source code, built on top of the `com.github.javaparser` library. It is specifically designed to provide a simplified domain model suitable for tasks like LLM-driven code reviews, where a full-fledged, complex AST might be overkill.

## Purpose

The Java Parser module is responsible for:
- Parsing Java source files into a simplified, versioned AST (`v1` currently).
- Providing a lightweight representation of key Java code elements (e.g., compilation units, classes, methods, fields, imports, packages).
- Maintaining a direct reference to the original `com.github.javaparser.ast.Node` within each custom AST node, allowing for access to the full AST details when necessary, without exposing its complexity by default.
- Offering a stable, versioned API to minimize breaking changes for dependent applications.

## Architecture

This module follows the project's modular architecture pattern:
- **API Layer** (`api/v1/`): Public interfaces and versioned models for external consumption
- **Generic Implementation** (`impl/generic/`): Backend-agnostic core logic and canonical data models. Not to be used directly.
- **Version Implementation** (`impl/v1/`): Version-specific adapters that bridge API contracts with generic implementations.

This structure ensures API stability while allowing internal evolution and supports multiple parser backends if needed in the future.

## Key Components

### API (v1)
- `JavaParsingServiceV1` - The public interface for parsing Java source code.
- `CompilationUnitNodeV1` - Represents a parsed Java source file (compilation unit).
- `AbstractJavaNodeV1` - Base class for all nodes in the simplified AST, holding common properties and a reference to the original JavaParser AST node.
- Other `*NodeV1` classes (e.g., `TypeDeclarationNodeV1`, `MethodDeclarationNodeV1`) - Represent specific Java language constructs in a simplified manner.

### Node Model Hierarchy

The simplified AST provides comprehensive coverage of Java language constructs through these node types:
- **CompilationUnitNode** - Root node representing a complete Java file
- **PackageDeclarationNode** - Package declarations
- **ImportDeclarationNode** - Import statements
- **TypeDeclarationNode** - Classes, interfaces, enums, and annotations
- **FieldDeclarationNode** - Class fields and constants
- **MethodDeclarationNode** - Methods and constructors
- **AnnotationNode** - Java annotations
- **JavadocNode** - Documentation comments

Each node includes metadata like source position, modifiers, and maintains a reference to the original JavaParser AST node for detailed analysis when needed.

## Dependencies

- `com.github.javaparser:javaparser-core` - For underlying Java source code parsing and AST generation.

## Usage

### Basic Parsing

```java
// Example usage of the JavaParsingServiceV1
@Autowired
private JavaParsingServiceV1 parsingService;

public void parseJavaFile(String javaSourceCode, String repositoryPath) {
    CompilationUnitNodeV1 compilationUnit = parsingService.parseCompilationUnit(javaSourceCode, repositoryPath);

    // Process the simplified AST
    System.out.println("Parsed file: " + compilationUnit.getRepositoryPath());
    compilationUnit.getClassDeclarations().forEach(classNode -> {
        System.out.println("  Class: " + classNode.getName());
        classNode.getMethodDeclarations().forEach(methodNode -> {
            System.out.println("    Method: " + methodNode.getSignature());
        });
    });

    // Accessing the original JavaParser AST node if more details are needed
    com.github.javaparser.ast.CompilationUnit originalAst = compilationUnit.getOriginalNode();
    // You can now use the full JavaParser API with 'originalAst'
}
```

### Advanced Analysis Example

```java
public void analyzeJavaFile(String javaSourceCode, String filePath) {
    CompilationUnitNodeV1 unit = parsingService.parseCompilationUnit(javaSourceCode, filePath);

    // Analyze class structure and documentation
    unit.getClassDeclarations().forEach(classNode -> {
        System.out.println("Analyzing class: " + classNode.getName());

        // Analyze method signatures and Javadoc
        classNode.getMethodDeclarations().forEach(method -> {
            System.out.println("  Method: " + method.getSignature());

            if (method.getJavadoc() != null) {
                System.out.println("    Documentation: " +
                    method.getJavadoc().javaCodeChunk().replace("\n", " "));
            }
        });

        // Count fields and methods for complexity analysis
        System.out.println("  Fields: " + classNode.getFieldDeclarations().size());
        System.out.println("  Methods: " + classNode.getMethodDeclarations().size());
    });
}
```
