# impl/v1/converter: Converters for v1 API and Generic Models

## Purpose
This package is responsible for handling the bidirectional conversion between the public v1 API Data Transfer Objects (DTOs) (defined in `api/v1/model`) and the internal canonical data models (defined in `impl/generic/model`). These converters act as a crucial bridge, ensuring that the internal implementation can evolve independently while maintaining a stable external API contract. This package absorbs the boilerplate required for data translation, as outlined in the project's `.clinerules`.

## Key Characteristics
- **Bridging Layer**: Facilitates communication between the version-specific API layer and the version-agnostic generic core.
- **Bidirectional Conversion**: Provides logic to convert from generic models to v1 DTOs and vice-versa.
- **Boilerplate Absorption**: Contains the necessary mapping logic, which can be manual or utilize mapping frameworks (e.g., MapStruct) if introduced.
- **Sanitization**: Ensures that when converting from generic models to v1 DTOs, any internal-only fields or sensitive information is removed or transformed appropriately for public consumption.

## Usage
Service implementations in `impl/v1/service` utilize these converters to translate incoming API requests into generic models for processing by `impl/generic` services, and to transform the results back into v1 API DTOs before returning them to the consumer.
