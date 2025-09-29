## impl/generic/model/model: Minimal Java Abstract Syntax Tree Domain Model

## Purpose
This package defines a minimalistic Java Abstract Syntax Tree (AST) domain model designed specifically for GitReviewer's code review workflows. Unlike full-featured Java parser libraries with extensive language support and complex APIs, this model captures only the essential structure needed for analyzing Java code changes, reviews, and diffs.

**Key Principles:**
- **Built from Full Parsers**: This domain model is not a parser itself - it's populated by using full-featured Java parsers (like JavaParser library) and then simplified to just the elements relevant to code review use cases.
- **Abstract Syntax Tree (AST) Representation**: Models Java source code as a hierarchical tree structure, where each node represents a language construct with its source location and content.
- **Minimal but Sufficient**: Captures the essential structural elements (classes, methods, fields, annotations, etc.) needed for code analysis while avoiding parser library complexity.
- **Code Review Focus**: Optimized for scenarios like diff analysis, code navigation, and reviewing changes in version control.

## Architecture

### Core Hierarchy
- **`AbstractJavaNode`**: Base class for all AST nodes containing line ranges and source code chunks
- **`AbstractJavaElementNode`**: Nodes representing declarations that can benefit from Javadoc and annotations (classes, methods, fields)

### Node Types
**AST Structure Nodes:**
- `CompilationUnitNode`: Root of the AST representing an entire Java file
- `PackageDeclarationNode`: Package statements (can be null for default package)
- `ImportDeclarationNode`: Import statements (including static imports)
- `ClassDeclarationNode`, `FieldDeclarationNode`, `MethodDeclarationNode`: Standard Java declarations

**Metadata Nodes:**
- `JavadocNode`: Javadoc comment blocks (prevented from having children)
- `AnnotationNode`: Individual annotations attached to elements

### Key Design Decisions
- **Specific Classes Instead of Enums**: Each AST construct is a proper Java class, avoiding generic type discrimination
- **Type Safety**: Strong typing prevents incorrect operations on different node types
- **Tree Structure**: Clear parent/child relationships for hierarchical AST traversal
- **Immutability**: Final fields ensure data consistency, with relationship setters for tree building

## Dependencies
- Standard Java: List, Optional, Map
- No external dependencies for model classes
- Clean separation from parsing logic

## Usage in GitReviewer

This AST enables two fundamentally different approaches to LLM-powered code review:

### Coarse-Grained Code Review Rules (High Cost, Low Precision)
Expensive rules that bundle entire pull requests into LLM context:
- Send all Java files + diff summary to LLM for holistic review
- Potentially effective but expensive and lacks precision
- This AST provides no benefit - use when budget isn't a concern

### Fine-Grained Code Review Rules (Low Cost, High Precision)
Efficient, laser-focused rules that parse changed files into AST trees:
- Parse each modified Java file into this hierarchical AST structure
- Rules can target specific elements with surgical precision
- Enables thousands of rule types similar to SonarQube static analysis but with LLM intelligence

**Rule Examples Enabled by this AST:**

**Documentation Rules:**
- Grammar correction in Javadocs: Iterate tree to find all `JavadocNode`s
- Documentation style consistency: Scan method/field annotations with `getJavaChunkWithJavadocAndAnnotations()`

**Code Quality Rules:**
- Method improvement: Identify changed methods via `getMethodDeclarations()`, send to LLM for optimization suggestions
- Refactor fat methods: Find oversized methods and suggest extraction/simplification
- Naming convention checks: Analyze field/method names across the tree

**Context-Aware Rules:**
- Pair method implementations with git diff hunks for precise change analysis
- Cross-reference imports with usage patterns: Use `getImportDeclarations()` for dependency analysis
- Find related methods in the same class hierarchy

**Granularity Control:**
Parent orchestration code decides analysis level:
- **Fine-grained**: Method/field level (e.g., "review this specific method")
- **Coarse-grained**: File level (e.g., "review this entire compilation unit")
- **Cross-cutting**: Package-level analysis connecting multiple files

This AST provides the tools for efficient, fine-grained code review automation - the middle ground between expensive holistic approaches and naive file-level processing.

## Versioning

**Implementation Canonical Model (This Package):**
This package contains the canonical, unversioned internal implementation that represents the latest and greatest AST representation. These are internal models used for Java parsing throughout the system and evolve freely as needed to support new requirements.

**Versioned APIs for External Consumers:**
External modules should never directly depend on these internal models. Instead, they access Java parsing functionality through stable, versioned APIs located at:
```
java-parser/src/main/java/com/sono99/javaparser/api/v1/model
```

Currently only v1 exists, which provides boilerplate DTOs that mirror the canonical models.

**Future Evolution Strategy:**
As the AST evolves to support more features (e.g., additional metadata fields per node, enhanced parsing capabilities), the internal canonical models will change. When breaking changes occur, new versioned APIs (v2, v3, etc.) will be created to expose enhanced functionality while maintaining backwards compatibility for lower-version API consumers.

- Lower-version APIs (v1) remain stable for application compatibility
- Higher-version APIs (v2+) can provide enhanced capabilities as the model grows
- Bridges between canonical and versioned models ensure clean separation

This architecture enables the AST to evolve internally while providing stable, backwards-compatible APIs for external consumers.

## Implementation Details
- Follows .clinerules: Final fields, Javadoc, proper naming conventions
- No external dependencies for model classes
- Internal usage only - external access through versioned APIs

This minimal yet effective design focuses on the essentials needed for effective code review automation.
