# Diff Examples Documentation

This directory contains sample Git diff files used for testing and demonstrating the diff parser functionality. Each diff file represents different scenarios and types of changes that the parser should handle.

## Available Diff Examples

### git_diff_01.diff
**Purpose:** Full branch comparison  
**Generation Method:** Compare a feature branch against the main branch
**Command Used:**
```bash
git diff main...HEAD > feature_branch.patch
```

**Description:**  
This diff represents the complete changes between the main branch and the current HEAD (feature branch). This example helps test how the parser handles:
- Multiple file changes
- Additions and deletions across various files
- Branch-level diff analysis

### git_diff_02.diff
**Purpose:** Specific file modifications with targeted changes  
**Target File:** `DiffParserService.java`  
**Generation Method:** Compare a single file with minimal, controlled changes

**Changes Made:**

1. **Line Comment Addition**
   - Added a new line comment at line 45
   - Comment added: `// added a dummy line at line number 45 of this file.`
   - Purpose: Test single-line addition detection

2. **Code Modification**
   - Modified an existing code comment
   - **Before:** `// (b) Convert to our domain model`
   - **After:** `// (b) Convert to our domain model (dummy modification line 50)`
   - Purpose: Test line modification detection

**Command Used:**
```bash
git diff diff-parser/src/main/java/com/sono99/diffparser/impl/generic/service/DiffParserService.java
```

## Usage in Testing

These diff examples are used in the diff parser test suite to verify:
- Accurate parsing of different diff formats
- Correct identification of added, deleted, and modified lines
- Proper handling of both file-level and branch-level comparisons
- Robust parsing of various diff scenarios

## Test File Format

The diff files follow the standard unified diff format, which includes:
- Header lines with file information
- Hunk headers with line number ranges
- Context lines and change indicators (+, -, and space)
- Patch metadata for context
