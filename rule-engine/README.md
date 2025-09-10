# Rule Engine Module

This module provides the framework for defining and executing code review rules. It supports both traditional rule-based analysis and LLM-powered intelligent code review.

## Purpose

The Rule Engine module is responsible for:
- Defining interfaces for code review rules
- Executing rules against parsed code and diff data
- Supporting both structured findings and LLM-based analysis
- Managing rule execution workflow and results

## Key Components

### Core Classes
- `Rule` - Interface for code review rules
- `RuleContext` - Context information for rule execution
- `RuleRunner` - Service for executing rules
- `Finding` - Structured representation of code issues

### Interfaces
- `Rule` - Base interface for all rules
- `LLMRule` - Interface for LLM-powered rules
- `RuleExecutor` - Interface for rule execution strategies

## Dependencies

- Gson - For JSON serialization of findings
- Spring Context - For dependency injection
- LangChain4j - For LLM integration

## Usage

```java
@Autowired
private RuleRunner ruleRunner;

public void analyzeCode(JavaParsingFileNode ast, DiffFile diff) {
    List<Finding> findings = ruleRunner.runRules(ast, diff);
    // Process the findings
}
```

## Rule Types

### Traditional Rules
- Static analysis rules (e.g., naming conventions, code style)
- Pattern-based rules using AST traversal
- Configuration-driven rule definitions

### LLM-Powered Rules
- Intelligent code review using language models
- Context-aware analysis of code changes
- Natural language feedback generation

## Testing

The module includes comprehensive tests covering:
- Rule execution with various code patterns
- LLM integration testing with mocked responses
- Performance testing for rule execution
- Integration tests with other modules
