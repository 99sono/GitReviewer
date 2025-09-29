# impl/generic/utils: Text Processing Utilities

## Purpose
This package provides reusable utility services for text processing operations, particularly focused on Java source code manipulation. These utilities keep core parsing logic clean by extracting common text operations into proper Spring-managed services.

## Key Services

### SourceContentUtils
Core utility service for line-based source code operations:

- **Content Extraction**: `extractSourceRange()` - Extract precise text content between line ranges
- **Line Operations**: `getLine()`, `countLines()` - Individual line access and counting
- **Validation**: `validateLineRange()` - Parameter validation for safe operations
- **Normalization**: Handles various line terminator conventions (LF, CRLF, CR)

## Design Principles

### Spring Integration
- All utilities are `@Service` annotated for proper dependency injection
- Fully testable with mocking capabilities
- Spring context management for lifecycle and configuration

### Separation of Concerns
- Text manipulation separated from parsing logic
- Single responsibility per utility method
- Validation and processing clearly separated

### Performance Considerations
- Minimal memory allocation in hot paths
- No unnecessary object creation
- Efficient string operations

## Usage in Parsing

```java
@Service
public class JavaParserService {

    @Autowired
    private SourceContentUtils sourceUtils;

    protected PackageDeclarationNode createPackageDeclarationNode(CompilationUnit cu, String fullSource) {
        // Clean extraction without cluttering parsing logic
        String packageChunk = sourceUtils.extractSourceRange(fullSource, range.begin.line, range.end.line);
        // ... use extracted content
    }
}
```

## Future Extensions

This package can be extended with:
- Content formatting and indentation handling
- Syntax-aware text manipulation
- Performance-optimized caching layers
- Validation utilities for AST consistency

## Dependencies
- Standard Java libraries only
- No external dependencies for maximum reusability
