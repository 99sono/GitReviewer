# Diff Mapper Core Module

This module provides functionality for parsing unified diffs and mapping them to code changes. It serves as the core component for understanding what parts of the code have been modified in a pull request.

## Purpose

The Diff Mapper Core module is responsible for:
- Parsing unified diff format patches
- Extracting modified line ranges from diff hunks
- Mapping diff changes to source code locations
- Supporting intersection with AST nodes for targeted analysis

## Key Components

### Core Classes
- `DiffParser` - Parses unified diff format into structured objects
- `DiffMapping` - Represents a single diff hunk with line mappings
- `DiffFile` - Represents all changes in a single file
- `LineRange` - Represents a range of modified lines

### Interfaces
- `DiffMapper` - Interface for diff parsing operations
- `HunkProcessor` - Interface for processing individual diff hunks

## Dependencies

- Gson - For JSON serialization of diff data
- Spring Context - For dependency injection

## Usage

```java
@Autowired
private DiffParser diffParser;

public void processDiff(String diffContent) {
    List<DiffFile> diffFiles = diffParser.parse(diffContent);
    // Process the parsed diff files
}
```

## Testing

The module includes comprehensive unit tests covering:
- Parsing of various diff formats
- Handling of multi-hunk diffs
- Edge cases like file renames and deletions
- Integration with other modules
