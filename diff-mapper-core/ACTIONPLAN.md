# Diff Mapper Core - Action Plan

## Overview
This module will implement the core diff parsing functionality to support code review analysis. The focus is on parsing unified diffs and creating mappings that can be intersected with AST nodes.

## Package Structure
- **Base Package**: `sono99.diffmapper`
- **Main Classes**: `sono99.diffmapper.DiffParser`, `sono99.diffmapper.DiffMapping`
- **Supporting Classes**: `sono99.diffmapper.*` (DiffFile, LineRange, etc.)

## Key Requirements from Specifications

### From delete_me/01.md
- Parse unified patches into DiffMapping objects with after-line sets
- Handle multi-hunk diffs and renames
- Support intersection with Java parsing nodes
- Provide clean API for diff analysis

## Implementation Plan

### Phase 1: Core Data Models
1. Implement `DiffFile` class to represent changes in a single file
2. Implement `DiffMapping` class for individual diff hunks
3. Implement `LineRange` class for modified line ranges
4. Add JSON serialization support using Gson

### Phase 2: Diff Parser Implementation
1. Implement `DiffParser` class with unified diff parsing
2. Handle file headers (+++ and --- lines)
3. Parse hunk headers (@@ -old_start,old_count +new_start,new_count @@)
4. Extract context lines, additions, and deletions
5. Support for file renames and deletions

### Phase 3: Intersection Support
1. Implement `DiffAstIntersectionService`
2. Add methods to intersect diff ranges with AST node ranges
3. Tag nodes as ADDED, MODIFIED, UNCHANGED
4. Support for method-level modification detection

### Phase 4: Advanced Features
1. Handle binary file diffs
2. Support for merge commits and complex diffs
3. Performance optimizations for large diffs
4. Caching of parsed diff results

## Testing Strategy
- Unit tests for diff parsing with various formats
- Integration tests with sample GitHub PR diffs
- Performance tests with large diff files
- Edge case testing (empty diffs, malformed patches)

## Integration Points
- Will be used by `bootstrap-app` for PR analysis
- Provides data for `rule-engine` to determine which rules to apply
- Supports `java-parser` intersection for targeted analysis
