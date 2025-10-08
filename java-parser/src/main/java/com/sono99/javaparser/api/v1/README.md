# api/v1: Public v1 API Package

## Purpose
This package defines the public, stable v1 contract for the java-parser module, enabling external consumers (e.g., github-connector, rule-engine) to parse Java source into enriched AST representations without depending on internal implementations. It follows GitReviewer's versioning principles: Stable API for integration, with no exposure of impl details. Consumers depend on the sono99:java-parser-api-v1 Maven artifact.

## Key Classes
- `service/JavaParsingServiceV1`: Core interface for parsing operations.
  - `EnrichedContentV1 parse(InputStream source, String sourceName)`: Parses Java source to versioned enriched AST (to be implemented; currently placeholder).
  - `boolean canParse(InputStream source, String sourceName)`: Validates if content is parsable as Java.
- Future: `model/` for versioned DTOs (e.g., `EnrichedContentV1`, `JavaParsingNodeV1`)—mirrors internal structure but sanitizes (e.g., hides backend refs).

## Dependencies
- None on impl or generic; only Java stdlib and project deps.
- No internal JavaParser refs exposed.

## Versioning Notes
- v1: Basic parsing with positions, nodes, metadata. Breaking changes require v2 (additive only here).
- Bridge: Consumers never see impl/generic; conversions handled in impl/v1.
- Enforcement: .clinerules ensures no impl deps; Maven separates api-v1.jar.

For extensions, implement JavaParsingServiceV1 in your bridge (e.g., impl/v1).
