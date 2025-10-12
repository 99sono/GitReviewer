# api/v1/service: Public v1 API Service Interfaces

## Purpose
This package defines the public service interfaces for the `java-parser` module's v1 API. These interfaces establish the contract for how external consumers interact with the parsing functionality, ensuring a stable and versioned API. Implementations of these interfaces are found in the `impl/v1/service` package, which adapt calls to the internal generic logic.

## Key Characteristics
- **Public Contract**: These interfaces are part of the public API and define the operations available to consumers.
- **Versioned**: Specific to v1 of the API. Breaking changes to these interfaces would necessitate a new API version (e.g., v2).
- **Decoupling**: Promotes loose coupling between the `java-parser` module and its consumers, as consumers depend only on these interfaces, not on concrete implementations.
- **No Implementation Details**: These interfaces should only define method signatures and return types, without exposing any internal implementation specifics.

## Usage
External modules and applications will depend on these interfaces to utilize the `java-parser`'s functionality. The actual logic is handled by implementations that bridge to the `impl/generic` core.