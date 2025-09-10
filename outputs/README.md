# Outputs Module

This module handles the formatting and rendering of code review findings for various output destinations including GitHub comments, console output, and file reports.

## Purpose

The Outputs module is responsible for:
- Formatting findings into human-readable comments
- Rendering LLM findings as inline GitHub comments
- Supporting dry-run mode for testing
- Handling deduplication of findings

## Key Components

### Core Classes
- `GitHubInlineRenderer` - Renders findings as GitHub comments
- `FindingFormatter` - Formats findings for different outputs
- `CommentRenderer` - Handles comment formatting and posting
- `DeduplicationService` - Prevents duplicate findings

### Interfaces
- `OutputRenderer` - Interface for different output formats
- `FindingRenderer` - Interface for finding formatting

## Dependencies

- Gson - For JSON serialization
- Spring Context - For dependency injection
- Spring Web - For HTTP operations

## Usage

```java
@Autowired
private GitHubInlineRenderer renderer;

public void renderFindings(List<Finding> findings) {
    List<Comment> comments = renderer.render(findings);
    // Post comments to GitHub
}
```

## Output Formats

### GitHub Comments
- Inline comments on pull requests
- Threaded discussions for complex findings
- Support for code suggestions

### Console Output
- Formatted text output for local development
- Color-coded severity levels
- Structured JSON output option

### File Reports
- HTML reports for detailed analysis
- JSON exports for integration
- CSV exports for data analysis

## Testing

The module includes comprehensive tests covering:
- Comment formatting with various finding types
- Deduplication logic testing
- Integration with GitHub API mocking
- Performance testing for large finding sets
