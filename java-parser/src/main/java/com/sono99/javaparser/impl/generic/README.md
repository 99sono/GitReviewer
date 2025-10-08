
# impl/generic: Canonical Core Logic (Blackbox)

## Purpose
This package holds the backend-agnostic, version-independent core implementation for the java-parser module. It encapsulates all essential logic (parsing, AST building, enrichment) in a blackbox: No awareness of API versions or external contracts. Canonical models and services here evolve independently, optimized for efficiency, and are consumed only by bridge adapters (impl/v1). This follows .clinerules for modularity—internal only, not exposed in public Maven artifacts (uses impl-core.jar).

The generic package contains the canonical logic. Outside of .generic, we find boilerplate to convert from version-specific classes/APIs to generic (or vice-versa). The essential logic is under .generic.

## Key Classes/Packages
- **model/**: Canonical DTOs representing enriched AST (immutable records preferred).
  - `JavaParsingNode`: Hierarchical node with positions, name, signature, children, Javadocs/annotations.
  - `EnrichedContent`: Container for root node, quick links, metadata.
  - Supporting: `AnnotationInfo`, `AnnotationRef`, `JavadocRef`, `QuickLink`, `Modifier`, `NoteType` enum, `ParserBackend` enum.
- **service/**: Core processing.
  - `JavaParsingService`: Handles InputStream parsing to EnrichedContent using JavaParser visitors (e.g., AstToNodeVisitor for tree building).
- Future: Backends sub-package for extensibility (e.g., alternative parsers).

## Dependencies
- External libs: JavaParser (for AST), SLF4J (logging), no Spring here (generic is pure).
- No deps on api/v1 or other modules' impl; only stdlib + libs.
- Models: Use Java 21 records, immutable collections (List.of, etc.) per .clinerules.

## Versioning Notes
- Version-agnostic: Changes here (e.g., new NodeType) don't affect API consumers—bridges adapt.
- Blackbox: Consumers (even impl/v1) interact via simple methods (e.g., parse returns internal EnrichedContent).
- Enforcement: .clinerules/CI validate no deps to api; performance focus (caching, no deep clones).
- Benefits: Scalable—generic can add perf opts (e.g., streaming) without v1 breakage.

For consumption: Bridges map generic outputs (e.g., EnrichedContent → EnrichedContentV1).
