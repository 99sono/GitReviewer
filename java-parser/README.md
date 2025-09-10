# Java Parser Module

This module provides functionality for parsing Java source code using the JavaParser library. It serves as the foundation for analyzing Java code structure, extracting AST information, and supporting enriched content with quick links.

## Purpose

The Java Parser module is responsible for:
- Parsing Java source files into Abstract Syntax Trees (AST)
- Extracting enriched content including Javadoc references and annotation references
- Providing quick links for navigation within the codebase
- Supporting code analysis and validation rules

## Key Components

### Core Classes
- `JavaParsingService` - Main service for parsing Java files
- `EnrichedContent` - Data structure for parsed content with metadata
- `QuickLink` - Represents navigation links within the code
- `JavadocRef` - References to Javadoc documentation
- `AnnotationRef` - References to annotations

### Interfaces
- `JavaParser` - Interface for parsing operations
- `ContentEnricher` - Interface for enriching parsed content

## Dependencies

- JavaParser Core - For AST construction and parsing
- Gson - For JSON serialization
- Spring Context - For dependency injection

## Usage

```java
@Autowired
private JavaParsingService parsingService;

public void parseJavaFile(Path javaFile) {
    EnrichedContent content = parsingService.parse(javaFile);
    // Process the enriched content
}
```

## Testing

The module includes comprehensive unit tests covering:
- Parsing of various Java constructs
- Error handling for malformed files
- Performance benchmarks
- Integration with other modules
