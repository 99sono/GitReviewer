# api/v1/model: Public v1 Model DTOs

## Purpose
This sub-package contains versioned data transfer objects (DTOs) for the v1 API, representing parsed Java AST structures without exposing internal implementation details from impl/generic. These DTOs provide a stable, sanitized view for consumers (e.g., rule-engine analyzing nodes, github-connector storing metadata). They form the output of `JavaParsingServiceV1.parse`, enabling integration while maintaining black-box separation. Part of sono99:java-parser-api-v1 artifact.

## Key Classes (Planned/Placeholder)
- `EnrichedContentV1`: Versioned container for root node, quick links, and metadata (mirrors impl/generic/model/EnrichedContent but omits backend refs).
- `JavaParsingNodeV1`: Hierarchical AST node with positions, name, signature, children (mirrors impl/generic/model/JavaParsingNode; hides originalId/backend for security).
- Other DTOs (e.g., `AnnotationRefV1`, `JavadocRefV1`, `QuickLinkV1`, `ModifierV1`): Mirror internal models with necessary sanitization.
- Currently: `package.info` as placeholder; implement DTOs (as records for immutability) when fixing V1 interface deps.

## Dependencies
- None on impl, generic, or external libs beyond Java stdlib.
- No JavaParser-specific types exposed—use simple types (String, List, Optional) for portability.

## Versioning Notes
- v1: Basic positions, node kinds (NoteTypeV1 enum?), enrichment refs. DTOs are immutable records, matching .clinerules.
- Conversions: Handled in impl/v1 mappers (generic `EnrichedContent` → `EnrichedContentV1`, flattening/hiding internals).
- Enforcement: No impl deps; tests validate round-trip compatibility (v1 DTO → generic → v1 DTO).
- Future v2: Add AI-enriched fields (e.g., semantic scores) without breaking v1.

These DTOs ensure API reliability—internal model changes (e.g., new fields in generic) are adapted in bridges.
