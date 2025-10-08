# impl/generic/service: Canonical Processing Services (Internal)

## Purpose
This sub-package implements the core, version-agnostic processing logic for Java parsing, focusing on InputStream-to-AST transformation and enrichment. It acts as the engine within the blackbox impl/generic, producing canonical models for internal use. No direct exposure to API versions; delegates handle integration. Follows .clinerules: Use protected methods for extensibility, line comments for multi-step flows, unchecked exceptions.

## Key Classes
- `JavaParsingService`: Main service for AST operations (can be @Service in Spring context, but pure here).
  - `EnrichedContent parse(InputStream source, String sourceName)`: Parses to CompilationUnit, builds root JavaParsingNode tree via visitor, computes metadata/quickLinks (TODO: implement with StaticJavaParser and AstToNodeVisitor).
  - `boolean canParse(InputStream source, String sourceName)`: Checks Java signature (e.g., "package ", "import ") or extension; minimal validation.
- Future: Visitors (e.g., AstToNodeVisitor as top-level class) for tree population; enrichers for Javadoc/Annotation extraction.

## Dependencies
- impl/generic/model (canonical DTOs).
- External: com.github.javaparser (StaticJavaParser, VoidVisitorAdapter).
- No API/v1 or Spring deps (pure Java); logging via SLF4J.

## Versioning Notes
- Unversioned Core: Evolves independently (e.g., new visitor overrides for Java 21 features); bridges adapt outputs.
- Blackbox: Returns internal EnrichedContent; performance opts (e.g., caching parses) don't affect API.
- Enforcement: .clinerules requires // Step X line comments, no deep cloning.
- Benefits: Supports Phase 3 (enrichment via dedicated visitors) and scalability (e.g., switch backends without API change).

Services delegate to models for tree queries; used by impl/v1 mappers.
