# impl/generic/model: Canonical Data Models for Internal Generic Implementation

## Purpose
This package contains the canonical, backend-agnostic data models (DTOs/records) used internally by the `java-parser` module's generic implementation. These models represent the core data structures, such as the Abstract Syntax Tree (AST) nodes and enriched content, including various Java type declarations like classes, interfaces, and records, in a version-independent manner. They are optimized for internal processing and are not exposed directly to external API consumers.

## Key Characteristics
- **Internal Use Only**: These models are strictly for internal use within the `impl/generic` package and its bridge implementations.
- **Version-Agnostic**: Designed to evolve independently of API versions. Changes here do not directly break external API contracts.
- **Rich Representation**: May contain detailed information relevant for internal processing, including references to backend objects or internal IDs.
- **Immutability**: Adheres to the project's `.clinerules` by preferring records or immutable classes for data representation.

## Usage
These models are used by the generic services to perform parsing, AST building, and enrichment. Bridge implementations (e.g., in `impl/v1/converter`) are responsible for converting between these canonical models and the public API DTOs.