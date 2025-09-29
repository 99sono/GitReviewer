# Code Reviewer Project: Brainstorming and Specification History

This document consolidates the brainstorming conversations and specifications for the automated code reviewer application, originally captured in multiple files during holiday development sessions.

## Project Overview

The Code Reviewer project is a Java Spring Boot application that performs LLM-assisted code reviews on GitHub using LangChain4j. The app supports multiple LLM providers, a configurable rule engine, and multiple modes of operation with a primary focus on reviewing modified Java methods only.

## High-Level Goals

1. Integrate with GitHub to fetch PRs or commits, diffs, changed files, and metadata.
2. Use LangChain4j to connect to different LLMs (OpenAI, Azure OpenAI, local models via Ollama, etc.).
3. Provide a modular rule system to determine which files/methods to review, what prompts to send, and how to format results.
4. Offer several operation modes, with the primary mode focusing on modified Java methods in a diff.
5. Centralize system prompts and rules configuration under resources (for easy tuning of guidelines).
6. Expose a REST API and optional CLI to run reviews, plus webhook receiver for GitHub events.
7. Persist review results and audit trail (e.g., to Postgres or file store).
8. Provide an extensible plugin-style design for future languages or rules.

## Architecture Overview

- Spring Boot application (Java 17+).
- Modules:
  - api: Controllers exposed via REST, for manual trigger and GitHub webhook callback.
  - github: Service for GitHub API integration and diff parsing.
  - rules: Rule engine for determining which entities (file, method) should be reviewed, with a rule registry.
  - llm: LangChain4j abstraction for LLM providers and prompt execution.
  - prompts: Resource management layer for system and rule-specific prompts.
  - review: Core orchestrator that executes selected rules, prepares prompt inputs, invokes LLM, and aggregates results.
  - persistence: Stores runs, decisions, prompts used, requests/responses, and final review artifacts.
  - config: Application config and feature flags.

## Key External Dependencies

- Spring Boot (Web, Validation, Configuration, JPA).
- LangChain4j (LLM providers integration).
- Jackson (JSON).
- SLF4J/Logback (logging).
- JGit or use raw GitHub API for diffs (prefer GitHub API).
- Database: Postgres via Spring Data JPA (or pluggable; H2 for dev).
- Optional: Redis cache.

## Domain Model

### ReviewRun
- id
- repoOwner
- repoName
- pullRequestNumber or commitSha
- mode (enum): MODIFIED_JAVA_METHODS, FULL_FILE_REVIEW, MODIFIED_FILES, CUSTOM_RULE_SET
- rulesetId (optional)
- llmProfileId
- status (PENDING, RUNNING, COMPLETED, FAILED)
- createdAt, updatedAt

### FileChange
- path
- changeType (ADDED, MODIFIED, REMOVED, RENAMED)
- language (detected; initially Java only)
- diffHunks: List<DiffHunk>
- fileContentBefore (optional), fileContentAfter

### DiffHunk
- hunkHeader
- addedLines: List<LineChange>
- removedLines: List<LineChange>
- unifiedDiffText (optional)

### LineChange
- lineNumberAfter
- lineNumberBefore
- content
- changeType (+/-)

### CodeEntity
- type (FILE, CLASS, METHOD, BLOCK)
- identifier (e.g., fully qualified class name, method signature)
- text
- location: startLine, endLine

### Rule
- id
- name
- description
- trigger: TriggerSpec
- selector: SelectorSpec
- promptRef: reference to prompt templates
- priority: integer
- action: ActionSpec (what to send, how to ask, expected output schema)

### TriggerSpec
- onFileChangeTypes: [ADDED, MODIFIED, REMOVED, RENAMED]
- onLanguage: JAVA
- onEntityType: FILE | METHOD
- onlyIfModifiedInDiff: boolean (true indicates we only act on entities that intersect with diff)

### SelectorSpec
- filters: e.g., path patterns (glob), class name patterns, method annotations, access modifiers
- maxTokens or size constraints

### ActionSpec
- scope: part of the entity to send (only modified method, entire method body, entire file)
- additionalContextRefs: references to shared context files or repo-level docs
- resultFormat: JSON, Markdown, or structured
- severityMapping: map of categories -> severity levels

### LlmProfile
- id
- provider: OPENAI, AZURE_OPENAI, OLLAMA
- modelName
- temperature, topP, maxTokens
- rateLimit, timeout
- systemPromptRef
- stopSequences, etc.

### PromptBundle
- system: File reference in resources/prompts/system/system_prompt.md
- rulePrompts: Map<String, PromptTemplate> loaded from resources/prompts/rules/*.md
- optional tool instructions / style guides

### ReviewFinding
- ruleId
- entityRef (file/method signature)
- category
- severity
- title
- details
- suggestedFix
- references
- rawModelOutput
- tokenUsage

### ReviewReport
- runId
- summary
- findings: List<ReviewFinding>
- metrics: tokens, cost (if calculable), duration
- postingStatus: postedToGitHub (boolean)

### PostingOptions
- asPRComments: boolean
- asReviewSummary: boolean
- asCheckRun: boolean

## Operation Modes

1) Modified Java Methods Only (primary)
   - For each file changed in the PR:
   - If file is Java and changeType is ADDED or MODIFIED, parse the file AST (after version).
   - Determine which methods intersect with the diff (lines added or removed).
   - For each method intersecting with the diff:
   - Evaluate rules whose TriggerSpec matches on Java, METHOD, and onlyIfModifiedInDiff=true.
   - SelectorSpec can further narrow (annotations, naming, etc.).
   - If matched, prepare ActionSpec payload:
   - Extract the modified method source (and optionally class context).
   - Include minimal surrounding code as context based on token budget.
   - Compose prompt: system prompt + rule prompt + dynamic variables (repo, file path, method signature, diff snippet).
   - Invoke LLM.
   - Parse structured output (prefer JSON) to ReviewFinding(s).

2) Full File Review
   - For any file with changeType ADDED or MODIFIED and language Java:
   - Evaluate rules with TriggerSpec FILE / onLanguage=JAVA.
   - Send entire file + relevant context documents.

3) Modified Files Summary Review
   - Run summarization and risk analysis prompts across all changed Java files to provide high-level comments and potential meta issues.

4) Custom Rule Set
   - Allow specifying a subset of rules to run via API input or config. Rules can run sequentially or in parallel (within rate limits).

## GitHub Integration

- Authentication via GitHub App or PAT.
- Inbound webhook: pull_request or push events.
- Ability to fetch:
  - PR metadata, list of changed files, diffs (patch), file contents before/after.
- Posting results:
  - PR review comments at file/method lines if possible.
  - Overall PR review summary comment.
  - GitHub Checks API for rich annotations (optional).
- Rate limit handling and retries.

## Rule Engine

- Rule Registry: load rules from configuration YAML/JSON in resources/rules/*.yaml.
- Rule Types:
  - MethodModifiedRule: triggers when Java methods intersect with diff lines.
  - FileCreatedUpdatedRule: triggers on new or modified Java files for full-file review.
- Evaluation pipeline:
  - Gather candidate entities (files, methods).
  - For each rule, check TriggerSpec and SelectorSpec.
  - Produce ReviewTasks (each task has ruleId, entityRef, payload).
- Execution:
  - Tasks batched by LlmProfile and provider limits.
  - Concurrency control and rate limit adherence.
  - Collect outputs and map to findings.
- Extensibility:
  - New rules can be added by adding YAML and optional custom selectors via Spring beans.

## Prompt Management

- File-based prompts under src/main/resources/prompts:
  - system/system_prompt.md: global system instructions for the reviewer assistant (tone, structure, non-guessing policy, emphasis on Java best practices).
  - rules/<ruleId>.md: prompt templates for rules; can contain variables like {{filePath}}, {{methodSignature}}, {{diffSnippet}}, {{projectGuidelines}}, {{severityMapping}}.
  - style_guides/java_style.md: shared doc referenced by prompts.
  - compliance/security/*.md: reusable snippets for secure coding checks.
- Prompt loader service to read templates at startup and cache.
- Template engine: simple Mustache/Handlebars or LangChain4j template support.

## LLM Integration (LangChain4j)

- Providers:
  - OpenAI: via OpenAiChatModel.
  - Azure OpenAI: via AzureOpenAiChatModel.
  - Ollama: via OllamaChatModel.
- Abstraction: LlmClient interface with method generate(LLMRequest) -> LLMResponse.
- LLMRequest:
  - systemPrompt
  - userPrompt
  - model parameters (from LlmProfile)
  - optional function-calling schema or JSON mode
- Error handling:
  - Retries on transient errors.
  - Circuit breaker for provider outages.
- Token/accounting:
  - Use model/tokenizers if available to estimate token usage.
  - Budget per task or run.

## Java Parsing and Diff Intersection

- Use JavaParser (com.github.javaparser) to:
  - Build AST of the after-version file.
  - Extract classes and methods with line ranges.
- Diff intersection:
  - For each DiffHunk, list changed line numbers.
  - A method qualifies if its line range intersects with changed lines.
- Snippet extraction:
  - Extract exact method text.
  - Optionally include imports and class signature if within token budget.

## API Design

- POST /api/reviews/trigger
  - Request:
    - repoOwner, repoName
    - pullRequestNumber or commitSha
    - mode
    - rulesetId (optional)
    - llmProfileId (optional)
    - postingOptions
  - Response:
    - runId, status
- GET /api/reviews/{runId}
  - Returns run status and summary/results.
- POST /api/webhooks/github
  - Receives PR events; can auto-trigger based on config.
- GET /api/config/rules
  - Returns loaded rules metadata.
- GET /api/config/llm-profiles
  - Returns configured LLM profiles.

## Configuration

- application.yml
- github:
  - appId, privateKey, webhookSecret, or token
  - baseUrl (for GH Enterprise)
- llm:
  - profiles:
    - defaultOpenAi:
      provider: OPENAI
      model: gpt-4o-mini
      temperature: 0.2
      maxTokens: 2000
      systemPromptRef: system/system_prompt.md
    - localOllama:
      provider: OLLAMA
      model: llama3:instruct
      temperature: 0.1
- review:
  - defaultMode: MODIFIED_JAVA_METHODS
  - postToGitHub: true
  - maxConcurrentTasks: 4
  - tokenBudgetPerRun: 300000

- rules YAML example (resources/rules/method_modified_security.yaml)
  - id: java-method-security
  - name: Security checks for modified methods
  - trigger:
    onFileChangeTypes: [ADDED, MODIFIED]
    onLanguage: JAVA
    onEntityType: METHOD
    onlyIfModifiedInDiff: true
  - selector:
    filters:
      pathPatterns: ["src/main/java/**"]
      methodAnnotations: ["@Transactional", "@RequestMapping"]
  - action:
    scope: METHOD_BODY
    additionalContextRefs: ["prompts/compliance/security/java_injection.md"]
    resultFormat: JSON
    severityMapping:
      injection: HIGH
      authz: HIGH
      validation: MEDIUM
  - promptRef: "prompts/rules/java_method_security.md"
  - priority: 10

## Processing Flow

1) Trigger received
   - Create ReviewRun, set status RUNNING.
   - Resolve LlmProfile and ruleset based on mode and request.
   - Fetch PR details and changed files via GitHub service.

2) Build candidate entities
   - For Modified Java Methods mode:
   - For each changed Java file with type ADDED or MODIFIED:
   - Parse AST, find methods intersecting with diff changes.

3) Rule evaluation
   - For each method, evaluate rules from registry; collect tasks.

4) Prompt preparation
   - Load system prompt, rule prompt; render with variables.
   - Constrain context by token budget; include diff snippet and method code.

5) LLM invocation
   - Send tasks to LLM via LlmClient; handle retries and errors.
   - Parse JSON outputs into ReviewFindings using a schema or JSON schema validation.

6) Aggregation
   - Aggregate findings into ReviewReport, compute metrics.

7) Posting results
   - Depending on PostingOptions, post line comments or summary to GitHub.
   - Store artifacts in persistence.

8) Completion
   - Mark run completed; expose via API.

## Security and Compliance

- Store secrets in environment variables or vault.
- Verify GitHub webhook signatures.
- Rate limit inbound triggers to avoid abuse.
- Audit logs of prompts and responses (redact secrets).
- Optionally mask code content in logs.

## Extensibility

- Add new language support by implementing:
  - LanguageDetector
  - Parser for entities with line ranges
  - Language-specific rules and prompts
- Add new rule types by implementing Trigger and Selector evaluators.
- Add new LLM providers by implementing LlmClient.

## Testing Strategy

- Unit tests for rule matching, diff intersection, prompt rendering.
- Integration tests with mock GitHub API and mock LLM client.
- End-to-end test using a sample PR and local Ollama model.

## Example System Prompt (resources/prompts/system/system_prompt.md)

- "You are a precise and pragmatic senior Java code reviewer. You focus on correctness, security, performance, maintainability, and adherence to Java best practices and project style guidelines. You do not speculate; if context is insufficient, you ask for clarification or mark as 'insufficient context'. Provide actionable, concise findings with severity and suggested fixes."

## Example Rule Prompt (resources/prompts/rules/java_method_security.md)

- "Review the following modified Java method for security issues (injection, auth/authz, input validation, sensitive data handling). Return JSON with fields: issues[], each having category, severity, title, description, code_refs[], suggested_fix."

## Persistence Schema (JPA)

- Tables: review_runs, review_tasks, review_findings, llm_calls, files_cache
- Store:
  - Raw prompts and outputs (optional, size-limited).
  - Token usage per LLM call.
  - Mappings to GitHub comments/checks for traceability.

## CLI (optional)

- java -jar app.jar review --repo owner/name --pr 123 --mode MODIFIED_JAVA_METHODS --ruleset default
- Useful for local testing.

## Deployment

- Containerized with Docker.
- Profiles: dev (H2, mock providers), prod (Postgres, real LLMs).
- Horizontal scaling with work queue if needed (e.g., Spring Cloud + RabbitMQ) for large PRs.

## Non-functional Requirements

- Performance: Handle PRs with up to ~200 changed files; parallelize tasks with limits.
- Reliability: Retries, idempotent posting to GitHub, resumable runs if possible.
- Observability: Metrics on LLM calls, token usage, queue lengths; structured logs; tracing.

---

## Java AST Parser Module

### Core Model Design

The Java AST parser module provides a simplified, LLM-friendly AST representation with the following key classes:

#### AbstractParsingNode (base type for everything)
- id: String (deterministic: hash of relativePath + kind + startLine + endLine + stable identity token)
- kind: NodeKind (FILE, PACKAGE, IMPORT, TYPE, CLASS, INTERFACE, ENUM, METHOD, CONSTRUCTOR, FIELD, PARAM, JAVADOC, ANNOTATION, UNKNOWN)
- fileName: String
- relativePath: String
- startLine: int
- startColumn: int
- endLine: int
- endColumn: int
- contentFromStartToEnd: String (entire file content for FILE nodes; for non-FILE nodes this is the exact text slice of the node range)
- enrichedNodeContent: String (for any node, the meaningful code slice you want to show the LLM; for FILE this can be same as fullContent or empty; for METHOD it's the method body or signature+body depending on scope)
- javadocRef: Optional<String> (id of associated Javadoc node)
- annotationRefs: List<String> (ids of associated Annotation nodes, source order)
- parentId: Optional<String>
- children: List<AbstractParsingNode> (ordered by start position)
- backend: ParserBackend = JAVADOC (always JAVAPARSER)
- backendRef: String (opaque ID to look up original JavaParser Node in a local map)
- attributes: Map<String, String> (signature, modifiers, types, qualifiedName, etc.)

#### Specialized Nodes (extend AbstractParsingNode)
- FileNode (kind=FILE)
- PackageNode (kind=PACKAGE): attributes["name"]
- ImportNode (kind=IMPORT): attributes["fqName"], ["static"] = "true/false", ["onDemand"] = "true/false"
- TypeNode (CLASS/INTERFACE/ENUM): attributes["simpleName"], ["qualifiedName"] (best-effort from package + simple), ["modifiers"]
- MethodNode: attributes["signature"], ["returnType"], ["parametersSignature"], ["modifiers"]
- ConstructorNode: attributes["signature"], ["parametersSignature"], ["modifiers"]
- FieldNode: attributes["type"], ["name"], ["modifiers"]
- JavadocNode: attributes["raw"] (including /** */), ["summary"] (first sentence)
- AnnotationNode: attributes["name"], ["values"] (stable stringified key=value pairs)

### Construction with JavaParser
- Parse source into CompilationUnit cu with positions; apply LexicalPreservingPrinter.setup(cu).
- If parse fails, record an AST-unavailable error for the file and produce a FILE node only (contentFromStartToEnd = full content; no structured children). Rules requiring structure should skip; file-level rules still work. No second parser is used.

---

## Git/GitHub Diff Parser Module

The diff parser provides a clean Java API for parsing unified diffs, mapping them to precise line numbers for both before and after versions of files.

### Core Models

#### WrappedFileDiff
```java
public final class WrappedFileDiff {
    public enum ChangeType { ADDED, MODIFIED, RENAMED, DELETED }
    
    private final String oldPath;
    private final String newPath;
    private final ChangeType changeType;
    private final List<WrappedHunk> hunks;
    private final FileHeader sourceHeader;
    
    // Methods for intersection and line mapping
    public boolean intersectsNewRange(int startLineInclusive, int endLineInclusive) { ... }
    public boolean intersectsOldRange(int startLineInclusive, int endLineInclusive) { ... }
    public Optional<Integer> mapNewToOldLine(int newLine) { ... }
    public Optional<Integer> mapOldToNewLine(int oldLine) { ... }
}
```

#### WrappedHunk
```java
final class WrappedHunk {
    private final Integer originalStartLine; // Nullable for ADDED-only hunks
    private final Integer originalEndLine;   // Nullable for ADDED-only hunks
    private final Integer newStartLine;      // Nullable for DELETED-only hunks
    private final Integer newEndLine;        // Nullable for DELETED-only hunks
    private final Set<Integer> addedLinesInNewFile; // New-file absolute lines with '+'
    private final Set<Integer> removedLinesInOriginalFile; // Old-file absolute lines with '-'
    private final HunkHeader sourceHunk; // Underlying JGit hunk
}
```

### Implementation
Uses JGit for parsing with Maven dependency:
```xml
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.9.0.202403050737-r</version>
</dependency>
```

---

## GitHub Pull Request Connector Module

### Core Read Models
- PullRequestInfo: owner, repo, prNumber, title, author, state, headSha, baseSha, createdAt, updatedAt, isDraft
- ChangedFile: oldPath, newPath, changeType (ADDED|MODIFIED|RENAMED|DELETED), patch (String unified diff for this file; may be null if unavailable), headContent (String file content at head; null if deleted or binary or too large), additions, deletions, changes (ints), blobSha (optional)
- PullRequestSnapshot: pr: PullRequestInfo, files: List<ChangedFile>

### Write Models
- InlineCommentRequest: filePath (new path), bodyMarkdown, line (after side), side (RIGHT), startLine/endLine (optional for multi-line)
- GeneralCommentRequest: bodyMarkdown

### Service Interface
```java
public interface GitHubPullRequestService {
    PullRequestInfo getPullRequestInfo(String oauthToken, String owner, String repository, int pullRequestNumber);
    PullRequestSnapshot getPullRequestSnapshot(String oauthToken, String owner, String repository, int pullRequestNumber, boolean includeHeadContent);
    List<ChangedFile> listChangedFiles(String oauthToken, String owner, String repository, int pullRequestNumber, boolean includeHeadContent);
    String postGeneralComment(String oauthToken, String owner, String repository, int pullRequestNumber, GeneralCommentRequest request);
    String postInlineComment(String oauthToken, String owner, String repository, int pullRequestNumber, InlineCommentRequest request);
}
```

Uses Hub4J GitHub API library:
```xml
<dependency>
    <groupId>org.kohsuke</groupId>
    <artifactId>github-api</artifactId>
    <version>1.318</version>
</dependency>
```

---

## Finding Schema

Two different finding schemas were developed:

### Structured Finding Schema
For deterministic, rule-based findings with IDs, locations, provenance, and severity.

### LLM Finding Schema
For minimal inline comments with deterministic IDs, repository-aware provenance, and optional telemetry.

#### LLMInlineFinding
```java
public final class LLMInlineFinding {
    private final String id; // deterministic id for dedup
    private final String ruleId; // producer rule identifier (e.g., "JAVA_LONG_METHOD")
    private final String filePath; // repository-relative new path
    private final int afterLine; // 1-based line in "after" (head) version
    private final Integer endAfterLine; // nullable; inclusive end for ranges
    private final String markdown; // exact Markdown to post
    private final LLMProvenance provenance; // base/head SHAs, repository and PR identifiers
    private final LLMRequestTelemetry telemetry; // optional; token usage, latency, model
}
```

#### LLMProvenance
```java
public final class LLMProvenance {
    private final String repositoryId; // logical identifier (e.g., "org/repo" for GitHub)
    private final String repositoryUrl; // optional
    private final String pullRequestId; // for GitHub this is the pull request number/id; for GitLab this is the merge request id
    private final String baseSha; // base commit SHA of the review
    private final String headSha; // head commit SHA of the review
    private final String fileSha; // blob SHA of the evaluated head file (optional)
}
```

#### LLMRequestTelemetry
```java
public final class LLMRequestTelemetry {
    private final String model; // e.g., "gpt-4o-mini", "claude-3-5-sonnet"
    private final Integer inputTokens; // total prompt tokens
    private final Integer outputTokens; // total completion tokens
    private final Long latencyMillis; // end-to-end time for the call
}
```

---

## Engine Bootstrap App

The bootstrap application orchestrates the complete pipeline from GitHub PR fetch to finding generation.

### Key Components

#### BootstrapConfig
Immutable configuration for a single bootstrap run with validated parameters for fetching a Pull Request snapshot, building diffs/ASTs, and executing rules in dry-run or posting mode.

#### SnapshotFetcher
Fetches a PullRequestSnapshot via GitHubPullRequestService with retry logic for transient failures.

#### DiffBuilder
Converts PullRequestSnapshot changed files into a deterministic catalog of per-file after-line mappings using the existing JGit-based diff parser.

#### JavaAstProvider
Lazy AST provider backed by JavaParsingService. Loads ASTs for eligible .java files when head content is available and under size cap.

#### ProvenanceFactoryImpl
Builds LLMProvenance objects from snapshot metadata and per-file details, ensuring findings carry repository, Pull Request, and SHA provenance for auditing.

#### RuleRegistryBuilder
Creates the default RuleRegistry for Phase 1, currently including BroadCatchIntroducedRule.

#### RunnerOrchestrator
Orchestrates rule execution by constructing a RuleContext, invoking RuleRunner, and returning RuleRunnerResult.

#### DryRunRenderer
Renders findings in a deterministic, concise format suitable for logs/stdout.

#### BootstrapRunner
Entry point for a single end-to-end execution in dry-run or posting mode.

---

## Rules and Rule Engine

The rule engine follows a pattern where each rule can have its own traversal style. Rules operate on AST nodes and diff information to generate findings.

### Example Rule: AnalyzeModifiedJavaMethodsRule
This rule traverses the Java AST to find method declarations, intersects each with after-side changed lines from the diff, and for modified methods, calls an LLM with a structured prompt and emits a finding if the LLM recommends improvements.

### LLM Integration Components
- LlmClient interface for provider abstraction
- MethodAuditPromptRenderer for creating structured prompts for method analysis
- LlmResponseParser for validating strict JSON responses from LLMs
- MethodDiffIntersection for determining if methods were modified

### LLM Response Protocol
Strict protocol requiring responses to start with exact decision phrases:
- "Yes, please improve the quality of the code."
- "No, there is no need to modify the code; it is already at a good level."

Followed by bullet-list justifications and Markdown-formatted comment body, all wrapped in JSON with fields for decision, justifications, markdown_comment, and rule-specific sections.

---

## Package Naming Convention Update
All packages in this specification have been updated from `com.example` to `com.sono99` as requested.

This comprehensive specification provides the structure and detail necessary to implement the code reviewer application.