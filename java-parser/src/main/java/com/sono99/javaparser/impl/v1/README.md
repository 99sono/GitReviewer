
# impl/v1: v1 Bridge Adapters

## Purpose
This package provides version-specific (v1) bridge implementations that adapt the public api/v1 contracts to the canonical impl/generic core. It handles all boilerplate for input/output conversions, ensuring strict separation: API consumers see only stable v1 DTOs, while generic logic remains blackboxed. Key for GitReviewer's reliability—internal changes (e.g., new generic fields) are absorbed here without API breakage. Part of impl-v1.jar (internal artifact only).

In here we should have a basic class that implements the api.v1.service.JavaParsingServiceV1 interface, for example impl/v1/JavaParsingServiceV1Impl, that calls the generic service JavaParsingService to do the work and returns back versioned model classes (e.g., EnrichedContentV1).

## Key Classes
- `JavaParsingServiceV1Impl`: Implements `api.v1.service.JavaParsingServiceV1`.
  - Delegates to `impl.generic.service.JavaParsingService` for core logic (e.g., parse → generic EnrichedContent).
  - Converts: generic models → v1 DTOs (e.g., recursive map JavaParsingNode to JavaParsingNodeV1, sanitizing backend/originalId).
  - Handles: InputStream forwarding, error wrapping, minimal validation.
  - Future: Mappers (manual or MapStruct) for tree conversions (e.g., node tree deep-copy with sanitization).
- Sub-packages: `/service/` for v1-specific services if needed; currently flat for simplicity.

## Dependencies
- api/v1/service and api/v1/model (implements/returns v1 DTOs).
- impl/generic/service and impl/generic/model (delegates to core, converts from canonical).
- Spring: @Service annotation for DI; constructor injection.

## Versioning Notes
- v1-Specific: Tailored to v1 API (e.g., EnrichedContentV1 mirroring v1 features like basic enrichment).
- Boilerplate Focus: ~20-30% code for bidirectional mappers (generic ↔ v1); e.g., traverse children, hide internals.
- Enforcement: .clinerules requires no generic changes to break v1; tests round-trip (v1 input → generic → v1 output).
- Scalability: New v2 adds impl/v2/ without touching v1; deprecation handled via API if needed.
- Benefits: Ensures API stability—e.g., generic perf updates don't expose to consumers.

Bridges like this maintain loose coupling across GitReviewer modules.
