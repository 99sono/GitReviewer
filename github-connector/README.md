# GitHub Connector Module

This module provides integration with the GitHub API for fetching pull request data and posting code review comments. It serves as the bridge between the code review system and GitHub's platform.

## Purpose

The GitHub Connector module is responsible for:
- Authenticating with GitHub API
- Fetching pull request snapshots and diff data
- Posting inline comments on pull requests
- Handling rate limiting and error scenarios
- Supporting webhook integration for automated reviews

## Key Components

### Core Classes
- `GitHubPullRequestService` - Main service for PR operations
- `GitHubClient` - Wrapper around GitHub API client
- `PullRequestData` - Data structure for PR information
- `CommentPoster` - Service for posting review comments

### Interfaces
- `PullRequestService` - Interface for PR operations
- `CommentService` - Interface for comment operations

## Dependencies

- GitHub API - For GitHub REST API integration
- Gson - For JSON serialization
- Spring Web - For HTTP client functionality
- Spring Context - For dependency injection

## Usage

```java
@Autowired
private GitHubPullRequestService prService;

public void processPullRequest(String owner, String repo, int prNumber) {
    PullRequestData prData = prService.fetchPullRequest(owner, repo, prNumber);
    // Process the PR data
}
```

## Configuration

The module requires GitHub authentication configuration:
- Personal access tokens
- App authentication (for higher rate limits)
- Webhook secrets for security

## Testing

The module includes comprehensive tests covering:
- Mocked GitHub API responses
- Error handling and rate limiting
- Authentication scenarios
- Integration tests with test repositories
