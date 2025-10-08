# api/v1/service: Public v1 Service Interfaces

## Purpose
This sub-package contains the stable, versioned interfaces for core services in the v1 API, defining contracts for parsing Java source into enriched ASTs. It enables external modules (e.g., github-connector fetching PR files, rule-engine analyzing nodes) to interact with java-parser without internal knowledge. Interfaces are simple, extensible, and return versioned DTOs from api/v1/model. Part of sono99:java-parser-api-v1 artifact; no impl exposure.

## Key Classes
- `JavaParsingServiceV1`: Primary interface for AST parsing.
  - `EnrichedContentV1 parse(InputStream source, String sourceName)`: Parses Java source to versioned enriched AST content.
  - `boolean canParse(InputStream source, String sourceName)`: Validates if content is parsable as Java (e.g., header/extension check).

## Dependencies
- api/v1/model (DTOs); stdlib for InputStream.
- No external libs or impl/generic deps.

## Versioning Notes
- v1: Streaming InputStream for large files, basic validation, enriched outputs.
- Extensibility: Spring @Service impls in bridges; constructor injection.
- Enforcement: .clinerules ensures @param/@return Javadocs; no unchecked deps.
- Future v2: Add async or advanced features.

Consumers declare dependency on the interface for loose coupling.
