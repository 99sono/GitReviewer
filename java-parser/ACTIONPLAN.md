# Java Parser - Action Plan

## Overview
This module will implement Java source code parsing using JavaParser to create structured AST representations for code analysis.

## Key Requirements from Specifications

### From delete_me/02A.md and 02b.md
- Use JavaParser as primary parsing library
- Create lightweight domain model (JavaParsingNode) with normalized ranges
- Maintain mapping back to original JavaParser nodes
- Support enriched content with Javadoc and annotation references
- Handle fallback parsing for edge cases

## Implementation Plan

### Phase 1: Core Parsing Framework
1. Implement `JavaParsingService` as main parsing service
2. Create `JavaParsingNode` domain model with normalized ranges
3. Add `EnrichedContent` data structure for parsed results
4. Implement basic JavaParser integration

### Phase 2: AST Node Creation
1. Create visitor pattern for traversing CompilationUnit
2. Implement node creation for classes, methods, fields
3. Add support for Javadoc extraction and annotation parsing
4. Build hierarchical node structure with parent/child relationships

### Phase 3: Content Enrichment
1. Implement `JavadocRef` and `AnnotationRef` quick access references
2. Add enriched content generation combining code + Javadoc + annotations
3. Create `QuickLink` system for navigation within codebase
4. Support for different enrichment strategies

### Phase 4: Advanced Features
1. Add fallback parsing for malformed code
2. Implement caching for parsed files
3. Add performance optimizations
4. Support for incremental parsing

### Phase 5: Integration and Testing
1. Integrate with diff intersection service
2. Add comprehensive unit tests with sample Java files
3. Implement error handling and logging
4. Add performance benchmarks

## Data Model Structure

### JavaParsingNode
- filePath: String
- kind: NodeKind (FILE, CLASS, METHOD, FIELD, etc.)
- name: String and qualifiedName
- signature: String (normalized method/field signatures)
- range: startLine, startColumn, endLine, endColumn
- javadoc: Optional<String>
- annotations: List<AnnotationInfo>
- modifiers: Set<Modifier>
- children: List<JavaParsingNode>
- originalId: String (for back-references)

### EnrichedContent
- rootNode: JavaParsingNode
- quickLinks: Map<String, QuickLink>
- metadata: Map<String, Object>

## Parsing Strategy

### Primary Parsing
- Use JavaParser with LexicalPreservingPrinter
- Configure for position tracking and comment preservation
- Handle parse exceptions gracefully
- Support for different Java versions

### Fallback Mechanisms
- Eclipse JDT integration for complex cases
- Heuristic parsing for partial files
- Error-tolerant parsing modes

## Testing Strategy
- Unit tests for parsing various Java constructs
- Integration tests with real Java files
- Performance tests for large codebases
- Edge case testing (malformed code, large files)

## Integration Points
- Consumed by `diff-mapper-core` for AST intersection
- Used by `rule-engine` for code analysis
- Provides data for `finding-schema` objects
