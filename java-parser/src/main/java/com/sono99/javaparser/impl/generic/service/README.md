# impl/generic/service: Core Service Implementations for Internal Generic Logic

## Purpose
This package houses the core service interfaces and their implementations for the `java-parser` module's generic logic. These services encapsulate the fundamental operations of parsing, AST manipulation, and content enrichment, operating on the canonical data models defined in `impl/generic/model`. They are designed to be backend-agnostic and version-independent, forming the blackbox core of the `java-parser` module.

## Key Characteristics
- **Internal Use Only**: These services are exclusively for internal consumption within the `java-parser` module, primarily by the version-specific bridge implementations.
- **Version-Agnostic**: The logic within these services is independent of any specific API version, allowing for internal evolution without impacting external contracts.
- **Core Functionality**: Contains the essential business logic for Java parsing and AST processing.
- **Decoupled**: Does not depend on any API version-specific classes or interfaces, ensuring a clean separation of concerns.

## Usage
Version-specific service implementations (e.g., in `impl/v1/service`) delegate to these generic services to perform the actual parsing and enrichment tasks. This design ensures that the core logic remains centralized and reusable across different API versions.