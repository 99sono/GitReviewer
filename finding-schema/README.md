# Finding Schema Module

This module defines the core data models for code review findings, providing structured representations of issues found during code analysis.

## Purpose

The Finding Schema module is responsible for:
- Defining the `Finding` data structure
- Providing location and provenance information
- Supporting severity classification
- Enabling JSON serialization for persistence

## Key Components

### Core Classes
- `Finding` - Main finding data structure
- `FindingLocation` - Location information for findings
- `FindingProvenance` - Source information for findings
- `FindingSeverity` - Severity classification enum
- `FindingIdFactory` - ID generation for findings

### Data Structures
- Structured representation of code issues
- Support for different finding types
- JSON serialization support
- Comprehensive Javadoc documentation

## Dependencies

- Gson - For JSON serialization
- Spring Context - For dependency injection

## Usage

```java
Finding finding = Finding.builder()
    .id("unique-id")
    .message("Code issue description")
    .severity(FindingSeverity.MEDIUM)
    .location(location)
    .provenance(provenance)
    .build();
```

## Finding Types

### Structured Findings
- Traditional rule-based findings
- Static analysis results
- Pattern-based detections

### Severity Levels
- CRITICAL - Security issues, bugs
- HIGH - Major code quality issues
- MEDIUM - Moderate issues
- LOW - Minor style issues
- INFO - Informational findings

## Testing

The module includes comprehensive tests covering:
- Data structure validation
- JSON serialization/deserialization
- ID generation and uniqueness
- Builder pattern functionality
