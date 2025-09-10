# Bootstrap App Module

This is the main Spring Boot application that orchestrates the entire code review workflow, integrating all modules to provide a complete automated code review system.

## Purpose

The Bootstrap App module is responsible for:
- Orchestrating the end-to-end code review process
- Managing application configuration and lifecycle
- Providing REST APIs for external integration
- Coordinating between all internal modules
- Handling error scenarios and logging

## Key Components

### Core Classes
- `CodeReviewApplication` - Main Spring Boot application class
- `CodeReviewController` - REST API endpoints
- `CodeReviewService` - Main orchestration service
- `Configuration` - Application configuration classes

### Services
- `PullRequestProcessor` - Processes GitHub PR events
- `CodeAnalysisOrchestrator` - Coordinates analysis workflow
- `FindingDispatcher` - Routes findings to appropriate outputs

## Dependencies

- All internal modules (java-parser, diff-mapper-core, etc.)
- Spring Boot Web Starter - For REST APIs
- Spring Boot Actuator - For monitoring and health checks
- Spring Context - For dependency injection

## Usage

### Running the Application
```bash
mvn spring-boot:run
```

### REST API Endpoints
```java
POST /api/review/pr
// Trigger PR review

GET /api/health
// Health check endpoint
```

## Configuration

The application supports configuration through:
- `application.yml` for basic settings
- Environment variables for secrets
- Spring profiles for different environments

## Workflow

1. **PR Event Reception**: Receives webhook from GitHub
2. **Data Fetching**: Retrieves PR diff and file contents
3. **Analysis**: Parses code, maps diffs, runs rules
4. **Finding Generation**: Collects and processes findings
5. **Output**: Formats and posts findings as comments

## Testing

The module includes comprehensive tests covering:
- End-to-end workflow testing
- Integration tests with mocked services
- REST API testing
- Configuration testing
- Error scenario handling
