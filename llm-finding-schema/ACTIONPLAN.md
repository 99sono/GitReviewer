# LLM Finding Schema - Action Plan

## Overview
This module will implement data models for LLM-generated findings with comprehensive provenance tracking and telemetry.

## Package Structure
- **Base Package**: `sono99.llmfindingschema`
- **Main Classes**: `sono99.llmfindingschema.LLMInlineFinding`, `sono99.llmfindingschema.LLMProvenance`
- **Supporting Classes**: `sono99.llmfindingschema.*` (LLMRequestTelemetry, LLMPostingEnvelope, etc.)

## Key Requirements from Specifications

### From delete_me/12A.md/12b.md
- Implement LLMInlineFinding, LLMProvenance, LLMRequestTelemetry, LLMInlineFindingIdFactory, LLMPostingEnvelope
- Support JSON serialization for persistence and transmission
- Comprehensive Javadoc and unit tests

## Implementation Plan

### Phase 1: Core LLM Data Models
1. Implement `LLMProvenance` class for tracking LLM interactions
2. Create `LLMRequestTelemetry` class for performance metrics
3. Add `LLMInlineFindingIdFactory` for deterministic ID generation
4. Implement `LLMPostingEnvelope` for packaging findings

### Phase 2: Main LLM Finding Class
1. Implement `LLMInlineFinding` class with builder pattern
2. Add confidence scoring and metadata
3. Implement JSON serialization support
4. Create validation methods

### Phase 3: Integration Features
1. Add LangChain4j integration points
2. Implement telemetry collection
3. Create finding deduplication support
4. Add performance monitoring hooks

### Phase 4: Testing and Validation
1. Write comprehensive unit tests
2. Add JSON serialization tests
3. Implement telemetry validation
4. Create integration test scenarios

## Data Model Structure

### LLMInlineFinding
- id: String (unique identifier)
- message: String (LLM-generated feedback)
- confidence: double (confidence score 0.0-1.0)
- provenance: LLMProvenance
- telemetry: LLMRequestTelemetry
- metadata: Map<String, Object>

### LLMProvenance
- modelName: String
- modelVersion: String
- promptTemplate: String
- timestamp: Instant
- sessionId: String

### LLMRequestTelemetry
- requestTimeMs: long
- responseTimeMs: long
- promptTokens: int
- completionTokens: int
- totalTokens: int
- costEstimate: double

### LLMPostingEnvelope
- findings: List<LLMInlineFinding>
- batchId: String
- postingTimestamp: Instant
- targetRepository: String

## LLM Integration Features

### Provenance Tracking
- Complete audit trail of LLM interactions
- Model versioning and prompt tracking
- Session management for consistency
- Timestamp tracking for temporal analysis

### Telemetry Collection
- Performance metrics collection
- Token usage monitoring
- Cost estimation and tracking
- Error rate monitoring

### Quality Assurance
- Confidence scoring for findings
- Source attribution for transparency
- Metadata for debugging and improvement

## Testing Strategy
- Unit tests for all data models
- JSON round-trip testing
- Telemetry accuracy validation
- Integration tests with LangChain4j

## Integration Points
- Used by `rule-engine` for LLM rule outputs
- Consumed by `outputs` for GitHub posting
- Monitored by `bootstrap-app` for performance
- Persisted for analysis and improvement
