# api/v1/model: Public v1 API Data Transfer Objects (DTOs)

## Purpose
This package contains the Data Transfer Objects (DTOs) that define the public, stable v1 data contract for the `java-parser` module. These DTOs are used by external consumers to interact with the `java-parser` API, ensuring a consistent and versioned data structure. This includes DTOs representing various Java type declarations such as classes, interfaces, and records. They mirror the internal canonical models but are sanitized to hide implementation details and ensure API stability.

## Key Characteristics
- **Public Contract**: These DTOs are part of the public API and should be treated as immutable once exposed.
- **Versioned**: Specific to v1 of the API. Breaking changes would necessitate a new version (e.g., v2).
- **Sanitized**: Internal implementation details (e.g., backend references) are removed or abstracted to prevent leakage and maintain API stability.
- **Immutability**: Follows the project's `.clinerules` by preferring records or immutable classes for data representation.

## Usage
Consumers of the `java-parser` API will receive and send data using these DTOs. Converters in the `impl/v1/converter` package are responsible for translating between these public DTOs and the internal generic models.