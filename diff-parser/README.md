# Diff Parser Module

This module provides functionality for parsing unified diffs and mapping them to code changes. It serves as the core component for understanding what parts of the code have been modified in a pull request or merge request.

## Purpose

The Diff Parser module is responsible for:
- Parsing unified diff format patches using the java-diff-utils library
- Extracting modified line ranges from diff hunks
- Mapping diff changes to source code locations
- Supporting intersection with AST nodes for targeted analysis
- Providing lazy text extraction for efficient LLM processing

## Library Mapping: java-diff-utils to Domain Model

This module uses the [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) library for parsing unified diffs. Below is the mapping between the library's concepts and our simplified domain model:

### java-diff-utils Library Structure
```
UnifiedDiff (root)
├── UnifiedDiffFile (per file)
│   ├── getFromFile() → original file path
│   ├── getToFile() → new file path
│   ├── getPatch() → Patch object
│   │   └── getDeltas() → List<AbstractDelta<String>>
│   └── AbstractDelta<String> (per hunk)
│       ├── getSource() → Chunk (original lines)
│       ├── getTarget() → Chunk (new lines)
│       ├── getType() → INSERT, DELETE, CHANGE, EQUAL
│       └── getSource().getPosition() → line numbers
└── getFiles() → List<UnifiedDiffFile>
```

### Our Domain Model Mapping
```
ParsedDiff (root)
├── DiffedFile (per file)
│   ├── originalDiffFile → UnifiedDiffFile (for lazy text extraction)
│   ├── filePath, repositoryPath, etc. → metadata
│   └── diffChunks → List<DiffChunk>
└── DiffChunk (per hunk)
    ├── originalDelta → AbstractDelta<String> (for lazy text extraction)
    ├── hunkStartLine, hunkEndLine → position info
    └── lineRanges → List<LineRange> (extracted from delta positions)
```

### Key Design Decisions

**Lazy Text Extraction:**
- `DiffedFile.getDiffTextForFile()` → `originalDiffFile.getPatch().toString()`
- `DiffChunk.getDiffTextForHunk()` → `originalDelta.toString()`
- Original objects are preserved for efficient text access without duplication

**Line Range Extraction:**
- `AbstractDelta.getSource().getPosition()` → `LineRange` with change type
- Context lines (EQUAL) vs actual changes (INSERT/DELETE/CHANGE)
- Position mapping handles additions/deletions correctly

**Error Handling:**
- Malformed diffs return empty `ParsedDiff` with `isValid = false`
- Binary files are detected and marked appropriately
- IO errors are wrapped in `RuntimeException`

## Key Components

### Domain Models (impl/generic/model/)
- `ParsedDiff` - Main container for parsed diff results
- `DiffedFile` - Represents all changes in a single file
- `DiffChunk` - Represents an individual diff hunk
- `LineRange` - Represents a range of modified lines with change type

### Services (impl/generic/service/)
- `DiffParserService` - Core parsing service using java-diff-utils
- Converts library objects to our domain models
- Handles lazy text extraction and error cases

## Dependencies

- **java-diff-utils** (4.16) - For unified diff parsing
- **Gson** - For JSON serialization of diff data
- **Spring Context** - For dependency injection

## Usage

```java
@Autowired
private DiffParserService diffParser;

public void processDiff(String diffContent) {
    ParsedDiff result = diffParser.parseUnifiedDiff(diffContent);

    for (DiffedFile file : result.getDiffedFiles()) {
        String diffText = file.getDiffTextForFile(); // Lazy extraction
        // Process the parsed diff file
    }
}
```

## Testing

The module includes comprehensive unit tests covering:
- Parsing of various diff formats (git_diff_01.diff, git_diff_02.diff)
- Handling of multi-hunk diffs
- Edge cases like file renames, deletions, and binary files
- Lazy text extraction functionality
- Error handling for malformed diffs
