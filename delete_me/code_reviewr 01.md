Below is a comprehensive specification for a Java Spring Boot
application that performs LLM-assisted code reviews on GitHub, using
LangChain4j (LangChain for Java). The app supports multiple LLM
providers, a configurable rule engine, and multiple modes of operation
with a primary focus on reviewing modified Java methods only.

High-level goals

1\. Integrate with GitHub to fetch PRs or commits, diffs, changed files,
and metadata.

2\. Use LangChain4j to connect to different LLMs (OpenAI, Azure OpenAI,
local models via Ollama, etc.).

3\. Provide a modular rule system to determine which files/methods to
review, what prompts to send, and how to format results.

4\. Offer several operation modes, with the primary mode focusing on
modified Java methods in a diff.

5\. Centralize system prompts and rules configuration under resources
(for easy tuning of guidelines).

6\. Expose a REST API and optional CLI to run reviews, plus webhook
receiver for GitHub events.

7\. Persist review results and audit trail (e.g., to Postgres or file
store).

8\. Provide an extensible plugin-style design for future languages or
rules.

Architecture overview

\- Spring Boot application (Java 17+).

\- Modules:

\- api: Controllers exposed via REST, for manual trigger and GitHub
webhook callback.

\- github: Service for GitHub API integration and diff parsing.

\- rules: Rule engine for determining which entities (file, method)
should be reviewed, with a rule registry.

\- llm: LangChain4j abstraction for LLM providers and prompt execution.

\- prompts: Resource management layer for system and rule-specific
prompts.

\- review: Core orchestrator that executes selected rules, prepares
prompt inputs, invokes LLM, and aggregates results.

\- persistence: Stores runs, decisions, prompts used,
requests/responses, and final review artifacts.

\- config: Application config and feature flags.

Key external dependencies

\- Spring Boot (Web, Validation, Configuration, JPA).

\- LangChain4j (LLM providers integration).

\- Jackson (JSON).

\- SLF4J/Logback (logging).

\- JGit or use raw GitHub API for diffs (prefer GitHub API).

\- Database: Postgres via Spring Data JPA (or pluggable; H2 for dev).

\- Optional: Redis cache.

Domain model

\- ReviewRun

\- id

\- repoOwner

\- repoName

\- pullRequestNumber or commitSha

\- mode (enum): MODIFIED_JAVA_METHODS, FULL_FILE_REVIEW, MODIFIED_FILES,
CUSTOM_RULE_SET

\- rulesetId (optional)

\- llmProfileId

\- status (PENDING, RUNNING, COMPLETED, FAILED)

\- createdAt, updatedAt

\- FileChange

\- path

\- changeType (ADDED, MODIFIED, REMOVED, RENAMED)

\- language (detected; initially Java only)

\- diffHunks: List\<DiffHunk\>

\- fileContentBefore (optional), fileContentAfter

\- DiffHunk

\- hunkHeader

\- addedLines: List\<LineChange\>

\- removedLines: List\<LineChange\>

\- unifiedDiffText (optional)

\- LineChange

\- lineNumberAfter

\- lineNumberBefore

\- content

\- changeType (+/−)

\- CodeEntity

\- type (FILE, CLASS, METHOD, BLOCK)

\- identifier (e.g., fully qualified class name, method signature)

\- text

\- location: startLine, endLine

\- Rule

\- id

\- name

\- description

\- trigger: TriggerSpec

\- selector: SelectorSpec

\- promptRef: reference to prompt templates

\- priority: integer

\- action: ActionSpec (what to send, how to ask, expected output schema)

\- TriggerSpec

\- onFileChangeTypes: \[ADDED, MODIFIED, REMOVED, RENAMED\]

\- onLanguage: JAVA

\- onEntityType: FILE \| METHOD

\- onlyIfModifiedInDiff: boolean (true indicates we only act on entities
that intersect with diff)

\- SelectorSpec

\- filters: e.g., path patterns (glob), class name patterns, method
annotations, access modifiers

\- maxTokens or size constraints

\- ActionSpec

\- scope: part of the entity to send (only modified method, entire
method body, entire file)

\- additionalContextRefs: references to shared context files or
repo-level docs

\- resultFormat: JSON, Markdown, or structured

\- severityMapping: map of categories -\> severity levels

\- LlmProfile

\- id

\- provider: OPENAI, AZURE_OPENAI, OLLAMA

\- modelName

\- temperature, topP, maxTokens

\- rateLimit, timeout

\- systemPromptRef

\- stopSequences, etc.

\- PromptBundle

\- system: File reference in resources/prompts/system/system_prompt.md

\- rulePrompts: Map\<String, PromptTemplate\> loaded from
resources/prompts/rules/\*.md

\- optional tool instructions / style guides

\- ReviewFinding

\- ruleId

\- entityRef (file/method signature)

\- category

\- severity

\- title

\- details

\- suggestedFix

\- references

\- rawModelOutput

\- tokenUsage

\- ReviewReport

\- runId

\- summary

\- findings: List\<ReviewFinding\>

\- metrics: tokens, cost (if calculable), duration

\- postingStatus: postedToGitHub (boolean)

\- PostingOptions

\- asPRComments: boolean

\- asReviewSummary: boolean

\- asCheckRun: boolean

Operation modes

1\) Modified Java Methods Only (primary)

\- For each file changed in the PR:

\- If file is Java and changeType is ADDED or MODIFIED, parse the file
AST (after version).

\- Determine which methods intersect with the diff (lines added or
removed).

\- For each method intersecting with the diff:

\- Evaluate rules whose TriggerSpec matches on Java, METHOD, and
onlyIfModifiedInDiff=true.

\- SelectorSpec can further narrow (annotations, naming, etc.).

\- If matched, prepare ActionSpec payload:

\- Extract the modified method source (and optionally class context).

\- Include minimal surrounding code as context based on token budget.

\- Compose prompt: system prompt + rule prompt + dynamic variables
(repo, file path, method signature, diff snippet).

\- Invoke LLM.

\- Parse structured output (prefer JSON) to ReviewFinding(s).

2\) Full File Review

\- For any file with changeType ADDED or MODIFIED and language Java:

\- Evaluate rules with TriggerSpec FILE / onLanguage=JAVA.

\- Send entire file + relevant context documents.

3\) Modified Files Summary Review

\- Run summarization and risk analysis prompts across all changed Java
files to provide high-level comments and potential meta issues.

4\) Custom Rule Set

\- Allow specifying a subset of rules to run via API input or config.
Rules can run sequentially or in parallel (within rate limits).

GitHub integration

\- Authentication via GitHub App or PAT.

\- Inbound webhook: pull_request or push events.

\- Ability to fetch:

\- PR metadata, list of changed files, diffs (patch), file contents
before/after.

\- Posting results:

\- PR review comments at file/method lines if possible.

\- Overall PR review summary comment.

\- GitHub Checks API for rich annotations (optional).

\- Rate limit handling and retries.

Rule engine

\- Rule Registry: load rules from configuration YAML/JSON in
resources/rules/\*.yaml.

\- Rule Types:

\- MethodModifiedRule: triggers when Java methods intersect with diff
lines.

\- FileCreatedUpdatedRule: triggers on new or modified Java files for
full-file review.

\- Evaluation pipeline:

\- Gather candidate entities (files, methods).

\- For each rule, check TriggerSpec and SelectorSpec.

\- Produce ReviewTasks (each task has ruleId, entityRef, payload).

\- Execution:

\- Tasks batched by LlmProfile and provider limits.

\- Concurrency control and rate limit adherence.

\- Collect outputs and map to findings.

\- Extensibility:

\- New rules can be added by adding YAML and optional custom selectors
via Spring beans.

Prompt management

\- File-based prompts under src/main/resources/prompts:

\- system/system_prompt.md: global system instructions for the reviewer
assistant (tone, structure, non-guessing policy, emphasis on Java best
practices).

\- rules/\<ruleId\>.md: prompt templates for rules; can contain
variables like {{filePath}}, {{methodSignature}}, {{diffSnippet}},
{{projectGuidelines}}, {{severityMapping}}.

\- style_guides/java_style.md: shared doc referenced by prompts.

\- compliance/security/\*.md: reusable snippets for secure coding
checks.

\- Prompt loader service to read templates at startup and cache.

\- Template engine: simple Mustache/Handlebars or LangChain4j template
support.

LLM integration (LangChain4j)

\- Providers:

\- OpenAI: via OpenAiChatModel.

\- Azure OpenAI: via AzureOpenAiChatModel.

\- Ollama: via OllamaChatModel.

\- Abstraction: LlmClient interface with method generate(LLMRequest) -\>
LLMResponse.

\- LLMRequest:

\- systemPrompt

\- userPrompt

\- model parameters (from LlmProfile)

\- optional function-calling schema or JSON mode

\- Error handling:

\- Retries on transient errors.

\- Circuit breaker for provider outages.

\- Token/accounting:

\- Use model/tokenizers if available to estimate token usage.

\- Budget per task or run.

Java parsing and diff intersection

\- Use JavaParser (com.github.javaparser) to:

\- Build AST of the after-version file.

\- Extract classes and methods with line ranges.

\- Diff intersection:

\- For each DiffHunk, list changed line numbers.

\- A method qualifies if its line range intersects with changed lines.

\- Snippet extraction:

\- Extract exact method text.

\- Optionally include imports and class signature if within token
budget.

API design

\- POST /api/reviews/trigger

\- Request:

\- repoOwner, repoName

\- pullRequestNumber or commitSha

\- mode

\- rulesetId (optional)

\- llmProfileId (optional)

\- postingOptions

\- Response:

\- runId, status

\- GET /api/reviews/{runId}

\- Returns run status and summary/results.

\- POST /api/webhooks/github

\- Receives PR events; can auto-trigger based on config.

\- GET /api/config/rules

\- Returns loaded rules metadata.

\- GET /api/config/llm-profiles

\- Returns configured LLM profiles.

Configuration

\- application.yml

\- github:

\- appId, privateKey, webhookSecret, or token

\- baseUrl (for GH Enterprise)

\- llm:

\- profiles:

\- defaultOpenAi:

provider: OPENAI

model: gpt-4o-mini

temperature: 0.2

maxTokens: 2000

systemPromptRef: system/system_prompt.md

\- localOllama:

provider: OLLAMA

model: llama3:instruct

temperature: 0.1

\- review:

\- defaultMode: MODIFIED_JAVA_METHODS

\- postToGitHub: true

\- maxConcurrentTasks: 4

\- tokenBudgetPerRun: 300000

\- rules YAML example (resources/rules/method_modified_security.yaml)

\- id: java-method-security

\- name: Security checks for modified methods

\- trigger:

onFileChangeTypes: \[ADDED, MODIFIED\]

onLanguage: JAVA

onEntityType: METHOD

onlyIfModifiedInDiff: true

\- selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

methodAnnotations: \[\"@Transactional\", \"@RequestMapping\"\]

\- action:

scope: METHOD_BODY

additionalContextRefs:
\[\"prompts/compliance/security/java_injection.md\"\]

resultFormat: JSON

severityMapping:

injection: HIGH

authz: HIGH

validation: MEDIUM

\- promptRef: \"prompts/rules/java_method_security.md\"

\- priority: 10

Processing flow

1\) Trigger received

\- Create ReviewRun, set status RUNNING.

\- Resolve LlmProfile and ruleset based on mode and request.

\- Fetch PR details and changed files via GitHub service.

2\) Build candidate entities

\- For Modified Java Methods mode:

\- For each changed Java file with type ADDED or MODIFIED:

\- Parse AST, find methods intersecting with diff changes.

3\) Rule evaluation

\- For each method, evaluate rules from registry; collect tasks.

4\) Prompt preparation

\- Load system prompt, rule prompt; render with variables.

\- Constrain context by token budget; include diff snippet and method
code.

5\) LLM invocation

\- Send tasks to LLM via LlmClient; handle retries and errors.

\- Parse JSON outputs into ReviewFindings using a schema or JSON schema
validation.

6\) Aggregation

\- Aggregate findings into ReviewReport, compute metrics.

7\) Posting results

\- Depending on PostingOptions, post line comments or summary to GitHub.

\- Store artifacts in persistence.

8\) Completion

\- Mark run completed; expose via API.

Security and compliance

\- Store secrets in environment variables or vault.

\- Verify GitHub webhook signatures.

\- Rate limit inbound triggers to avoid abuse.

\- Audit logs of prompts and responses (redact secrets).

\- Optionally mask code content in logs.

Extensibility

\- Add new language support by implementing:

\- LanguageDetector

\- Parser for entities with line ranges

\- Language-specific rules and prompts

\- Add new rule types by implementing Trigger and Selector evaluators.

\- Add new LLM providers by implementing LlmClient.

Testing strategy

\- Unit tests for rule matching, diff intersection, prompt rendering.

\- Integration tests with mock GitHub API and mock LLM client.

\- End-to-end test using a sample PR and local Ollama model.

Example system prompt (resources/prompts/system/system_prompt.md)

\- "You are a precise and pragmatic senior Java code reviewer. You focus
on correctness, security, performance, maintainability, and adherence to
Java best practices and project style guidelines. You do not speculate;
if context is insufficient, you ask for clarification or mark as
'insufficient context'. Provide actionable, concise findings with
severity and suggested fixes."

Example rule prompt (resources/prompts/rules/java_method_security.md)

\- "Review the following modified Java method for security issues
(injection, auth/authz, input validation, sensitive data handling).
Return JSON with fields: issues\[\], each having category, severity,
title, description, code_refs\[\], suggested_fix."

Persistence schema (JPA)

\- Tables: review_runs, review_tasks, review_findings, llm_calls,
files_cache

\- Store:

\- Raw prompts and outputs (optional, size-limited).

\- Token usage per LLM call.

\- Mappings to GitHub comments/checks for traceability.

CLI (optional)

\- java -jar app.jar review \--repo owner/name \--pr 123 \--mode
MODIFIED_JAVA_METHODS \--ruleset default

\- Useful for local testing.

Deployment

\- Containerized with Docker.

\- Profiles: dev (H2, mock providers), prod (Postgres, real LLMs).

\- Horizontal scaling with work queue if needed (e.g., Spring Cloud +
RabbitMQ) for large PRs.

Non-functional requirements

\- Performance: Handle PRs with up to \~200 changed files; parallelize
tasks with limits.

\- Reliability: Retries, idempotent posting to GitHub, resumable runs if
possible.

\- Observability: Metrics on LLM calls, token usage, queue lengths;
structured logs; tracing.

This specification provides the structure and detail necessary to
implement the app. If you want, I can generate a starter Spring Boot
project skeleton (packages, classes, and example configuration files)
based on this spec.

Tab 2

i asked to improve the spec

Tab 3

Great request. Below is a refined and explicit process flow that
incorporates your desired behavior, aligning it with the previously
proposed architecture and rule engine model.

End-to-end process flow

1\) User selects a Pull Request to review

\- Input: repoOwner, repoName, pullRequestNumber, optional mode and
ruleset.

\- The application initializes a ReviewRun with status RUNNING and
records configuration (LLM profile, ruleset, mode).

2\) Initialize external connections

\- GitHub: Create authenticated client (GitHub App or PAT).

\- LLM: Resolve LLM profile and instantiate the appropriate
LangChain4j-backed LlmClient (OpenAI/Azure/Ollama).

\- Prompt loader: Load system prompt and active rule-specific prompt
templates from resources.

3\) Fetch and pre-filter PR files

\- Retrieve the list of changed files with their change types and
unified diffs (patches).

\- Non-Java files are ignored immediately.

\- Map change types to an enum: FileChangeType = ADDED, MODIFIED,
REMOVED, RENAMED.

\- For processing purposes:

\- Consider only ADDED and MODIFIED Java files.

\- Ignore REMOVED files entirely in this release.

\- Optionally treat RENAMED + MODIFIED as MODIFIED if diffs exist.

4\) Parse diffs into structured hunks

\- For each considered Java file:

\- Parse the GitHub patch into DiffHunk objects.

\- Each hunk contains addedLines and removedLines with line numbers and
content.

\- Compute the set of modified line numbers on the after-version of the
file (lines with + in the diff).

\- Optionally keep a mapping of before/after line numbers if needed for
precise intersection.

5\) Parse Java files into an AST with a hierarchical node structure

\- Use JavaParser to parse the after-version file content into a
CompilationUnit (AST).

\- Build or reuse a normalized tree abstraction:

\- FileNode (represents the entire file)

\- PackageDeclarationNode

\- ImportNodes (list)

\- ClassNodes (and/or Interface/Enum)

\- AnnotationsNode (on the class)

\- JavadocNode (for the class)

\- FieldNodes

\- AnnotationsNode (on fields)

\- JavadocNode (for fields)

\- MethodNodes

\- AnnotationsNode (on methods)

\- JavadocNode (for methods)

\- ParametersNode

\- Each Parameter with annotations

\- BodyNode (block/statements nested)

\- Each node must carry:

\- NodeType enum (FILE, PACKAGE, IMPORT, CLASS, FIELD, METHOD,
PARAMETER, JAVADOC, ANNOTATION, BLOCK, STATEMENT, etc.)

\- Source range: startLine, endLine.

\- Source text extraction function.

\- Parent/children references.

6\) Determine modified entities via diff intersection

\- Compute for each AST node whether it intersects with the changed
lines:

\- Intersects if any line in the node's startLine..endLine appears in
the modified line set.

\- Node-level modification flags:

\- NodeModificationStatus enum: UNCHANGED, MODIFIED, NEW

\- NEW for files with ADDED or nodes present only in after-version (may
infer via AST ranges + diff).

\- MODIFIED if the node intersects with changed lines.

\- This intersection is used to determine whether rules with
onlyIfModifiedInDiff=true should consider the node.

7\) Visitor pattern over the AST

\- Implement an AST visitor that traverses the FileNode tree
depth-first.

\- Context carried by visitor:

\- File path, FileChangeType, language=JAVA

\- Diff context: modified lines, diff hunks

\- LLM/profile info (for token budgeting)

\- At each node visit:

\- Construct a NodeContext object:

\- filePath, fileChangeType

\- nodeType, nodeRange, nodeText supplier

\- parent signatures (e.g., class name, method signature)

\- nodeModificationStatus

\- diff snippets relevant to the node (optional)

\- Dispatch node to the Rule Engine for rule consideration.

8\) Rule engine: interest-based triggering

\- Active rules are loaded from configuration and registered with a
RuleRegistry.

\- Each Rule defines:

\- TriggerSpec: onLanguage=JAVA, onEntityTypes it cares about (e.g.,
METHOD, JAVADOC, CLASS, FILE), file change types, onlyIfModifiedInDiff,
etc.

\- SelectorSpec: further filters (path patterns, annotations present,
naming patterns).

\- ActionSpec: scope and payload composition (what text to send:
javadoc+annotations+method body; how to format; result expectations).

\- Rule interest check:

\- For the current NodeContext, evaluate TriggerSpec quickly:

\- Match language=JAVA

\- Match fileChangeType ∈ {ADDED, MODIFIED} (for this version)

\- Match entity type (e.g., if rule cares about JAVADOC nodes only)

\- If onlyIfModifiedInDiff=true, ensure nodeModificationStatus ∈ {NEW,
MODIFIED}

\- If TriggerSpec passes, evaluate SelectorSpec filters (e.g., method
annotations, path globs).

\- If both pass, the rule is "interested" and should produce a
ReviewTask.

9\) Example: Javadoc typo rule

\- Rule: triggers on JAVADOC nodes for methods and classes; only when
modified or new.

\- When the visitor reaches a Method JavadocNode:

\- If the method's Javadoc intersects with diff or the method is
new/modified, the rule triggers.

\- ActionSpec for this rule:

\- scope: javadoc + method signature + annotations + method body
(optionally truncated) as context.

\- promptRef: prompts/rules/javadoc_typo_review.md

\- resultFormat: JSON with fields like: typos\[\], suggested_text,
severity=LOW/MEDIUM, rationale.

10\) Example: Modified Java method review rule

\- Rule: triggers on METHOD nodes; only when intersecting with diff
lines.

\- ActionSpec might specify:

\- Send the method body, method signature, annotations, and the diff
snippet relevant to the method.

\- Include nearby imports or class fields if needed for context and
within token budget.

\- Use a specific rule prompt: prompts/rules/java_method_security.md.

11\) Task assembly and batching

\- For every interested rule-node match, create a ReviewTask:

\- task fields: runId, ruleId, filePath, nodeType, entity signature
(e.g., fully qualified class + method signature), payload (rendered
prompt inputs), token budget estimate.

\- Apply concurrency and batching constraints by LLM provider/profile.

\- Deduplicate tasks if rules opt-in to do so across overlapping nodes
(optional).

12\) Prompt construction

\- Compose final prompt with:

\- System prompt (from resources/prompts/system/system_prompt.md).

\- Rule-specific prompt template:

\- Variables: filePath, nodeType, methodSignature, javadocText,
annotations, methodBody, diffSnippet, project style guide refs, severity
mapping.

\- Enforce token budget:

\- Trim large bodies, include minimal necessary context.

\- Use LangChain4j client to send the composed prompt to the chosen
model.

13\) LLM invocation and output parsing

\- Execute LLM calls with retries and timeouts.

\- Expect JSON output for structured findings; validate schema.

\- Record raw outputs, token usage, latency in persistence.

14\) Findings aggregation

\- Convert each LLM output into ReviewFinding objects:

\- ruleId, entityRef, category, severity, title, description, code_refs,
suggested_fix, rawModelOutput, tokenUsage.

\- Assemble all findings into a ReviewReport for the run.

\- Optionally consolidate duplicate or overlapping findings.

15\) Posting results to GitHub

\- Depending on PostingOptions:

\- Post line comments on specific files and lines (if mapping from
entity ranges to lines is available).

\- Post a PR summary comment with grouped findings by
file/rule/severity.

\- Optionally publish a GitHub Check Run with annotations.

16\) Persistence and audit

\- Store ReviewRun, ReviewTasks, LLM requests/responses (size-limited),
and ReviewFindings.

\- Maintain traceability from each finding to:

\- PR, file path, node type, entity signature, ruleId.

\- Log errors with correlation IDs; mask secrets.

17\) Completion

\- Update run status to COMPLETED or FAILED.

\- Expose status and results via GET /api/reviews/{runId}.

Key implementation details supporting this flow

\- Node model and visitor

\- Create a language-agnostic Node interface and JavaNode implementation
backed by JavaParser.

\- Enable extracting precise source ranges and text.

\- Visitor receives both AST node and diff context to compute
nodeModificationStatus quickly.

\- Diff intersection

\- Maintain a set of added/modified lines for the after-version.

\- Intersection check is a simple range overlap with node start/end.

\- Rule API

\- Rule interface methods:

\- boolean isInterested(NodeContext ctx) for fast filter.

\- ReviewTask buildTask(NodeContext ctx) to assemble payload based on
ActionSpec.

\- Config-driven rules

\- YAML-based rules define TriggerSpec/SelectorSpec/ActionSpec.

\- For Javadoc-specific rules, TriggerSpec.onEntityType = JAVADOC and
onlyIfModifiedInDiff = true.

\- Prompts

\- Prompts are stored in resources/prompts/system and
resources/prompts/rules.

\- Templates support variables for assembling precise, minimal context
per rule.

\- Modes

\- Primary mode still focuses on modified methods, but the visitor/rule
approach also supports:

\- Full-file rules (Trigger on FILE nodes).

\- Javadoc-only rules (Trigger on JAVADOC nodes).

\- Class-level rules (Trigger on CLASS nodes).

Behavioral examples

\- A large 1k-line Java file with a small method changed:

\- Only the method whose lines intersect with the diff triggers
method-level rules.

\- A Javadoc rule triggers if the method's Javadoc changed or the method
is new/modified.

\- A file-level rule would not trigger unless configured to run for any
modified file.

\- A new Java file:

\- Nodes may be marked NEW, enabling rules that review entire files or
all new methods.

This flow ensures the application:

\- Starts with a user-selected PR.

\- Connects to GitHub and LLM providers up front.

\- Ignores non-Java and deleted files.

\- Parses diffs and ASTs, uses a visitor pattern, and triggers rules
selectively based on node type and modification status.

\- Sends precisely the right slices of context to the LLM per rule.

\- Produces structured findings and posts them back to GitHub.

Tab 4

continhe improving spec

Tab 5

Below is the updated and augmented specification, incorporating the
recommended GitHub integration approach, a clearer end-to-end process
flow, and concrete examples of system and rule prompts for two
Java-focused rules: single-method quality review and Javadoc audit.

High-level goals

1\) Review GitHub pull requests using LLMs via LangChain4j, focusing
initially on Java code.

2\) Provide a configurable rule engine that triggers on AST nodes (file,
class, method, javadoc) based on diff intersections and selectors.

3\) Support multiple LLM providers (OpenAI, Azure OpenAI, Ollama) via
pluggable profiles.

4\) Use a visitor pattern over parsed Java AST to route nodes to
interested rules.

5\) Centralize prompts under resources and encourage structured JSON
outputs for findings.

6\) Integrate with GitHub via hub4j/github-api, with fallback to direct
REST using Spring WebClient for endpoints not covered (e.g., Checks
API).

Architecture overview

\- Spring Boot (Java 17+).

\- Modules:

\- api: REST controllers for triggers and webhooks.

\- github: GitHub integration service with hub4j and optional WebClient
fallback.

\- rules: Rule registry, trigger/selector/action evaluation, rule
configuration loader.

\- llm: LangChain4j-based LLM clients and abstraction.

\- prompts: Loader and renderer for system and rule prompts
(file-based).

\- parser: Java AST parsing and node model (JavaParser), diff
intersection.

\- review: Orchestrator to execute runs, assemble tasks, invoke LLM,
aggregate findings, and post results.

\- persistence: JPA entities for runs, tasks, findings, and LLM calls.

\- config: Profiles, feature flags, and provider settings.

Key external dependencies

\- Spring Boot: web, validation, configuration, data JPA.

\- LangChain4j: LLM provider integrations.

\- hub4j/github-api: primary GitHub REST client.

\- Spring WebClient: fallback for endpoints like Checks API or gaps in
hub4j.

\- JavaParser: Java AST parsing.

\- Jackson: JSON serialization/deserialization.

\- Database: Postgres (prod), H2 (dev).

\- SLF4J/Logback: logging.

GitHub integration

\- Primary: hub4j/github-api (formerly kohsuke/github-api).

\- Get PR info, list changed files with patches, comment on PRs.

\- Fallback: Spring WebClient for endpoints not supported or requiring
newer features.

\- Checks API: create/update check runs with annotations.

\- Authentication:

\- GitHub App (preferred): appId, privateKey (PEM), installationId.
Generate JWT and exchange for installation token.

\- PAT: simple token for development or personal repos.

\- Abstraction:

\- interface GitHubService {

PullRequestInfo getPullRequest(String owner, String repo, int prNumber);

List\<ChangedFile\> listChangedFiles(String owner, String repo, int
prNumber);

FileContent getFileContentAtRef(String owner, String repo, String path,
String ref);

void postComment(String owner, String repo, int prNumber, String body);

void postReviewCommentsBatch(List\<InlineComment\> comments);

void createOrUpdateCheckRun(CheckRunRequest request);

}

\- Hub4jGitHubService implements GitHubService.

\- WebClientGitHubService implements GitHubService (partial or for
gaps).

\- Webhook security: verify X-Hub-Signature-256.

Operation modes

\- MODIFIED_JAVA_METHODS (primary).

\- FULL_FILE_REVIEW (for new/updated files).

\- MODIFIED_FILES_SUMMARY (summary/risk analysis).

\- CUSTOM_RULE_SET (explicit rule selection).

End-to-end process flow

1\) User selects a Pull Request

\- Input: repoOwner, repoName, pullRequestNumber, mode (default
MODIFIED_JAVA_METHODS), rulesetId (optional), llmProfileId (optional).

\- Create ReviewRun with status RUNNING.

2\) Initialize external connections

\- GitHubService: authenticate (GitHub App or PAT).

\- LlmClient: resolve LlmProfile and create LangChain4j-backed client
(OpenAI/Azure/Ollama).

\- Prompt loader: load system prompt and rule templates from resources.

3\) Fetch and pre-filter PR files

\- Retrieve changed files with change types and unified diffs.

\- Ignore non-Java files immediately.

\- Map change types to FileChangeType enum: ADDED, MODIFIED, REMOVED,
RENAMED.

\- Process only ADDED and MODIFIED Java files (ignore REMOVED).

\- Treat RENAMED+MODIFIED as MODIFIED if applicable.

4\) Parse diffs into DiffHunks

\- Build DiffHunk objects from patches for each Java file.

\- Compute modified line numbers for the after-version (+ lines), and
maintain before/after mapping if needed.

5\) Parse Java files to AST

\- Use JavaParser to get a CompilationUnit.

\- Normalize into node hierarchy:

\- FileNode

\- PackageDeclarationNode

\- ImportNodes

\- Class/Interface/Enum Nodes

\- AnnotationsNode

\- JavadocNode

\- FieldNodes (with annotations, javadocs)

\- MethodNodes

\- AnnotationsNode

\- JavadocNode

\- ParametersNode (with annotations)

\- BodyNode (blocks/statements)

\- Each node contains:

\- NodeType enum (FILE, PACKAGE, IMPORT, CLASS, FIELD, METHOD,
PARAMETER, JAVADOC, ANNOTATION, BLOCK, STATEMENT, etc.).

\- Source range: startLine, endLine.

\- Source text supplier.

\- Parent/children references.

6\) Diff intersection and node modification status

\- For each node, mark NodeModificationStatus:

\- NEW (file ADDED or node newly created).

\- MODIFIED (node range intersects with modified lines).

\- UNCHANGED otherwise.

\- This supports rules with onlyIfModifiedInDiff=true.

7\) Visitor pattern

\- Traverse the AST depth-first with a visitor carrying context:

\- filePath, language=JAVA, fileChangeType, diff hunks, modified lines
set, class and method signatures.

\- For each node visited, create NodeContext and dispatch to Rule
Engine.

8\) Rule engine: interest-based triggering

\- RuleRegistry loads active rules from resources/rules/\*.yaml and any
custom beans.

\- Rule has:

\- TriggerSpec: onLanguage=JAVA, onEntityTypes (e.g., METHOD, JAVADOC),
onFileChangeTypes, onlyIfModifiedInDiff.

\- SelectorSpec: path patterns, annotations, modifiers, naming
conventions.

\- ActionSpec: what text to include (e.g., javadoc + annotations +
method body), promptRef, result format.

\- isInterested(NodeContext) checks TriggerSpec and SelectorSpec
quickly.

\- If interested, build ReviewTask with payload.

9\) Task batching and execution

\- Group tasks by LLM profile/provider.

\- Apply concurrency limits and rate limiting.

\- Execute tasks via LlmClient, with retries and timeouts.

10\) Prompt composition

\- Compose:

\- System prompt (global reviewer persona and policies).

\- Rule-specific user prompt (task instructions and required output
format).

\- Inject variables: file path, entity signature, javadoc text,
annotations, method body, diff snippet.

\- Enforce token budget by trimming content and including essential
context only.

11\) LLM invocation and output parsing

\- Use LangChain4j to call the configured model.

\- Expect JSON outputs; validate and convert to ReviewFinding objects.

\- Record token usage and raw outputs.

12\) Aggregation and posting

\- Aggregate findings into ReviewReport.

\- Post inline comments, a summary comment, or a Check Run with
annotations.

\- Persist run, tasks, findings, prompts, and responses.

13\) Completion

\- Mark run COMPLETED or FAILED.

\- Expose results via GET /api/reviews/{runId}.

Rule types and examples

A\) Java Single-Method Quality Review Rule

\- Goal: Review individual modified methods for clarity, correctness
hints, maintainability, basic performance, and simple conventions.

\- TriggerSpec:

\- onLanguage: JAVA

\- onEntityTypes: METHOD

\- onFileChangeTypes: \[ADDED, MODIFIED\]

\- onlyIfModifiedInDiff: true

\- SelectorSpec:

\- pathPatterns: \[\"src/main/java/\*\*\"\]

\- optional annotations or naming filters.

\- ActionSpec:

\- scope: Include method signature, annotations, javadoc, and method
body. Include minimal class context if needed and within budget. Include
diff snippet for the method.

\- promptRef: prompts/rules/java_method_quality.md

\- resultFormat: JSON

B\) Java Javadoc Audit Rule

\- Goal: Audit Javadoc for typos, grammar, clarity, param/return
consistency. Low severity by default.

\- TriggerSpec:

\- onLanguage: JAVA

\- onEntityTypes: JAVADOC

\- onFileChangeTypes: \[ADDED, MODIFIED\]

\- onlyIfModifiedInDiff: true

\- SelectorSpec:

\- pathPatterns: \[\"src/main/java/\*\*\"\]

\- optionally restrict to method or class Javadocs.

\- ActionSpec:

\- scope: Include the Javadoc text, the associated element signature
(method/class), any relevant annotations/params for consistency checks,
and optionally the method body for context.

\- promptRef: prompts/rules/javadoc_audit.md

\- resultFormat: JSON

Example prompts layout

\- resources/prompts/system/system_prompt.md

\- resources/prompts/rules/java_method_quality.md

\- resources/prompts/rules/javadoc_audit.md

\- resources/prompts/style_guides/java_style.md (optional shared
reference)

Example system prompt (system_prompt.md)

You are a highly competent, pragmatic senior Java engineer performing
focused code reviews. Your priorities are:

1\) Correctness and clarity of logic in the reviewed scope.

2\) Maintainability and readability (naming, decomposition, comments).

3\) Safe and idiomatic Java practices.

4\) Lightweight performance considerations when relevant.

Guidelines:

\- Review only the provided scope. Do not speculate about code outside
the provided context.

\- Prefer small, actionable suggestions with clear rationale and
examples.

\- If context is insufficient, state "insufficient context."

\- Favor simplicity. Avoid overengineering.

\- Do not request null checks for public method parameters unless the
method's contract or annotations require them.

\- Methods should remain reasonably small; prefer extraction of helper
methods if they grow too large or do multiple things.

\- Follow Java conventions and any provided style guides.

Output:

\- Return JSON according to the requested schema in the user prompt.
Keep your responses concise and specific.

Example user prompt for Java Single-Method Quality Review
(java_method_quality.md)

Task: Review a single Java method that was changed in this pull request.

Context:

\- File: {{filePath}}

\- Class: {{className}}

\- Method: {{methodSignature}}

\- Annotations: {{methodAnnotations}}

\- Javadoc (if any):

{{methodJavadoc}}

Diff snippet (for this method):

{{diffSnippet}}

Method implementation:

\`\`\`java

{{methodBody}}

\`\`\`

Rules to apply:

1\) Encourage small, focused methods; suggest extractions if the method
does multiple things or is overly long.

2\) Do not require null checks for input parameters unless annotations
or contract demand them.

3\) Prefer clear names, simple control flow, and early returns when
helpful.

4\) Suggest tests if critical logic is present or edge cases are
apparent.

5\) Point out obvious performance pitfalls only if evident and
low-effort to fix.

Output JSON schema:

{

\"summary\": \"short overview\",

\"issues\": \[

{

\"category\":
\"maintainability\|readability\|correctness\|performance\|testing\",

\"severity\": \"LOW\|MEDIUM\|HIGH\",

\"title\": \"short title\",

\"description\": \"what and why\",

\"code_refs\": \[{\"line\": number, \"hint\": \"optional\"}\],

\"suggested_fix\": \"concrete suggestion or snippet\"

}

\]

}

If no issues: return {\"summary\": \"OK\", \"issues\": \[\]}.

Example user prompt for Javadoc Audit (javadoc_audit.md)

Task: Audit the following Javadoc for typos, grammar, clarity, and
consistency with parameters and return type.

Context:

\- File: {{filePath}}

\- Element: {{elementType}} {{elementSignature}} // e.g., method or
class

\- Parameters (with types and names): {{parameters}}

\- Return type: {{returnType}}

Javadoc:

\"\"\"

{{javadocText}}

\"\"\"

Rules to apply:

1\) Correct typos and grammatical errors; propose improved wording while
preserving intent.

2\) Ensure \@param and \@return tags match actual parameters and return
type.

3\) Suggest concise and active-voice phrasing.

Output JSON schema:

{

\"summary\": \"short overview\",

\"typos_corrections\": \[

{\"original\": \"string\", \"suggestion\": \"string\", \"reason\":
\"grammar/spelling/clarity\"}

\],

\"tag_issues\": \[

{\"type\": \"PARAM_MISMATCH\|RETURN_MISMATCH\|MISSING_TAG\|EXTRA_TAG\",
\"detail\": \"string\"}

\],

\"suggested_javadoc\": \"fully revised Javadoc text\"

}

If no issues: return {\"summary\": \"OK\", \"typos_corrections\": \[\],
\"tag_issues\": \[\], \"suggested_javadoc\": \"{{javadocText}}\"}.

Domain model (selected)

\- ReviewRun, FileChange, DiffHunk, LineChange, CodeEntity, ReviewTask,
ReviewFinding, ReviewReport (as previously defined).

\- Node model carries NodeType, source range, text supplier,
parent/children.

\- NodeModificationStatus: NEW, MODIFIED, UNCHANGED.

\- FileChangeType: ADDED, MODIFIED, REMOVED, RENAMED.

API design

\- POST /api/reviews/trigger

\- body: { repoOwner, repoName, pullRequestNumber, mode?, rulesetId?,
llmProfileId?, postingOptions? }

\- returns: runId, status

\- GET /api/reviews/{runId}

\- returns run status, report

\- POST /api/webhooks/github

\- handles PR events (opened, synchronize), verifies signature

\- GET /api/config/rules, GET /api/config/llm-profiles

Configuration (application.yml)

\- github:

\- appId, privateKey, installationId, or token

\- baseUrl (for GH Enterprise)

\- webhookSecret

\- llm:

\- profiles:

\- openai-default:

provider: OPENAI

model: gpt-4o-mini

temperature: 0.2

maxTokens: 2000

systemPromptRef: prompts/system/system_prompt.md

\- ollama-local:

provider: OLLAMA

model: llama3:instruct

temperature: 0.1

\- review:

\- defaultMode: MODIFIED_JAVA_METHODS

\- postToGitHub: true

\- maxConcurrentTasks: 4

\- tokenBudgetPerRun: 300000

Rules configuration examples (YAML)

resources/rules/java_method_quality.yaml

id: java-method-quality

name: Quality review for modified Java methods

trigger:

onLanguage: JAVA

onEntityTypes: \[METHOD\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: METHOD_SIGNATURE_ANNOTATIONS_JAVADOC_AND_BODY

promptRef: \"prompts/rules/java_method_quality.md\"

resultFormat: JSON

priority: 10

resources/rules/javadoc_audit.yaml

id: javadoc-audit

name: Javadoc typo and grammar audit

trigger:

onLanguage: JAVA

onEntityTypes: \[JAVADOC\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: JAVADOC_WITH_SIGNATURE_AND_PARAMS

promptRef: \"prompts/rules/javadoc_audit.md\"

resultFormat: JSON

priority: 5

LLM integration (LangChain4j)

\- LlmClient abstraction

\- generate(LLMRequest) -\> LLMResponse

\- Providers:

\- OpenAI: OpenAiChatModel

\- Azure OpenAI: AzureOpenAiChatModel

\- Ollama: OllamaChatModel

\- JSON outputs recommended; enforce JSON mode or schema if provider
supports tool/function calling.

\- Retries, timeouts, and token usage accounting.

Java parsing and diff intersection

\- JavaParser to build AST.

\- Compute modified lines from DiffHunks.

\- Node is MODIFIED if node range intersects modified lines; NEW if file
is ADDED or node appears after addition.

\- Extract precise method body, annotations, and Javadoc for payloads.

Persistence schema

\- review_runs, review_tasks, review_findings, llm_calls, files_cache,
prompts_cache (optional).

\- Store raw prompts and outputs with truncation/size limits.

Posting results to GitHub

\- Inline comments where line mapping is available.

\- Summary PR comment aggregating findings.

\- Optional Check Runs with annotations via WebClient to Checks API.

Security and compliance

\- Secrets via env or vault.

\- Webhook signature verification.

\- Rate limiting and retry backoff.

\- Redact sensitive content in logs.

Testing

\- Unit tests for rule matches, diff intersection, prompt rendering.

\- Integration tests with mock GitHub and mock LLM.

\- E2E run with a sample PR and local Ollama.

Non-functional requirements

\- Handle large PRs efficiently (parallel tasks with limits).

\- Observability: metrics for task counts, LLM latency, token usage;
structured logs; tracing.

This updated spec now explicitly recommends hub4j/github-api with
WebClient fallback, provides concrete, ready-to-use system and rule
prompts, and details how the method-quality and Javadoc-audit rules
operate within the visitor-based, diff-aware pipeline.

Tab 6

asked grok4 to review the spec with review comments

Below is a comprehensive set of feedbacks, phrased as targeted prompts
you can send directly to an LLM (e.g., Grok or another model) to further
refine and improve the specification. These build on my previous audit
and feedbacks, incorporating the new considerations: implementing a
message queue for scaling parallelization (to handle LLM delays,
encapsulate requests, and enable state resumption for consumers), and
structuring prompts for easy parsing (with specific starting phrases,
justifications, and Markdown-formatted outputs for GitHub comments).

I\'ve organized them into themed prompts for clarity, making them
self-contained and actionable. Each prompt references the existing spec
(you can paste the full spec into the LLM query) and focuses on
increasing the chances of first-try implementation success by adding
details like edge cases, code snippets, rationales, and integrations.
This should help flesh out scalability, reliability, and prompt
engineering aspects.

\### 1. \*\*Prompt for Integrating Message Queue for Parallelization and
Scalability\*\*

\"Review this specification for a LLM-based GitHub code reviewer app,
focusing on the task batching, execution, and LLM invocation sections.
To scale parallelization effectively, incorporate a message queue system
(e.g., using Spring Boot with RabbitMQ or Kafka) where each LLM request
is encapsulated as a message containing the full state context (e.g.,
current file path, diff hunk, node details, rule ID, prompt payload).
Consumers (e.g., worker threads or microservices) should resume from
this state, make synchronous LLM calls, parse the response, and then
update GitHub with review comments or check runs. Add details on queue
configuration (e.g., durable queues, dead-letter handling for failures),
concurrency (e.g., multiple consumers per queue), and integration with
persistence (e.g., store task IDs in DB for resumption). Include error
handling like retries with exponential backoff and partial run recovery.
Provide updated sections for the architecture overview, end-to-end flow
(steps 9-12), and configuration YAML with queue settings. Suggest Spring
Boot dependencies (e.g., spring-boot-starter-amqp) and pseudo-code for
enqueuing/dequeuing tasks.\"

\### 2. \*\*Prompt for Enhancing Prompt Structure and Parsing\*\*

\"Audit the prompt composition and LLM output parsing sections in this
spec, along with the example prompts (system_prompt.md,
java_method_quality.md, javadoc_audit.md). Update them to ensure outputs
are easy to parse and suitable for GitHub: Instruct the LLM to always
start responses with either \'No, there is no need to modify the code;
it is already at a good level.\' (if no issues) or \'Yes, please improve
the quality of the code.\' (if issues found), followed by justifications
for any suggestions. Require all outputs to use Markdown formatting
(e.g., \*\*bold\*\* for titles, code blocks for snippets, bullet lists
for issues) so they can be directly posted as formatted GitHub comments.
Revise the JSON schemas in rule prompts to include these elements (e.g.,
add a \'decision\' field for the starting phrase, \'justification\'
array, and \'markdown_comment\' field). Add parsing logic in the app
(e.g., using regex or Jackson to extract decision and Markdown). Include
new guidelines in the system prompt to enforce this structure and
prevent hallucinations. Provide fully updated example prompt files with
these changes, and suggest how to handle non-compliant outputs (e.g.,
retry with clarification).\"

\### 3. \*\*Prompt for Combining Queue with LLM and GitHub Updates\*\*

\"Enhance the review orchestration and posting sections of this GitHub
code reviewer spec to integrate message queues with LLM calls and GitHub
updates. For each ReviewTask, enqueue a message with serialized context
(e.g., JSON payload including file, diff hunk, AST node, prompt).
Consumers dequeue, invoke the LLM synchronously, parse the structured
output (starting with \'Yes/No\' decision, justifications, and
Markdown), then post to GitHub (e.g., inline comment tied to diff hunk
lines). Add flow for resuming interrupted tasks from queue or DB state.
Include edge cases like queue overflow (e.g., rate-limit enqueuing), LLM
timeouts (requeue with priority), and GitHub failures (e.g., store
pending comments in DB for retry). Update the persistence schema to
track queue message IDs and task states (e.g., ENQUEUED, PROCESSING,
COMPLETED). Provide pseudo-code for a QueueService class (e.g., using
Spring\'s RabbitTemplate) and revised end-to-end flow steps 9-12.\"

\### 4. \*\*Prompt for Overall Resilience and Error Handling with New
Features\*\*

\"Refine the entire specification for this LLM-powered code reviewer,
incorporating message queues for parallel LLM requests and structured
prompts with \'Yes/No\' decisions, justifications, and Markdown outputs.
Add resilience features: For queues, handle consumer failures by
requeuing with context restoration; for prompts, add retries if output
doesn\'t match the required format (e.g., parse for starting phrase).
Ensure GitHub posts use Markdown directly from LLM for well-formatted
comments. Address potential issues like high-latency LLMs delaying runs
(e.g., timeout queues after 5min), or parsing failures (e.g., fallback
to raw text posting with warning). Update non-functional requirements
for scalability (e.g., horizontal scaling of consumers) and testing
(e.g., mock queues with Testcontainers). Provide a revised architecture
diagram in text form (e.g., using ASCII art) showing queue integration,
and add config options like queue names and consumer counts in
application.yml.\"

\### 5. \*\*Prompt for Rule-Specific Updates and Examples\*\*

\"Update the rule types, examples, and YAML configs in this spec to
align with new prompt structuring. For rules like Java Single-Method
Quality Review and Javadoc Audit, revise the user prompts to mandate
starting with \'No, there is no need\...\' or \'Yes, please
improve\...\', followed by detailed justifications and
Markdown-formatted suggestions (e.g., use \`\`\`java for code snippets).
Modify the output JSON schemas to include \'decision_phrase\',
\'justifications\' (array of strings), and \'formatted_comment\'
(Markdown string). Add a new example rule for full-file summary that
uses the queue system for batched processing. Ensure selectors and
actions reference diff hunks in payloads for context. Provide updated
YAML files and prompt MD files, plus app-side parsing code snippets
(e.g., in Java using JsonNode).\"

\### 6. \*\*Prompt for Testing and Validation with New Elements\*\*

\"Expand the testing section of this code reviewer specification to
cover the new message queue integration and structured prompts. Include
unit tests for enqueuing/dequeuing (e.g., with embedded RabbitMQ),
integration tests for LLM parsing (e.g., mock responses starting with
\'Yes/No\' and Markdown, verify JSON extraction), and E2E tests
simulating a PR with delayed LLM calls (e.g., consumer resumes and posts
formatted comment). Add scenarios for edge cases like invalid prompt
outputs (e.g., missing decision---trigger retry) or queue failures
(e.g., dead-letter handling). Recommend tools like JUnit, Mockito for
mocks, and Spring Boot Test for queues. Provide sample test code
snippets and acceptance criteria, such as \'System posts Markdown
comment to GitHub after queue processing, with justification if \'Yes\'
decision\'.\"

\### 7. \*\*Holistic Refinement Prompt\*\*

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption,
followed by sync LLM calls and GitHub updates. Restructure all prompts
to start with \'No, there is no need to modify the code; it is already
at a good level.\' or \'Yes, please improve the quality of the code.\',
include justifications, and output in Markdown for direct posting.
Resolve any gaps from previous audits (e.g., diff edge cases, AST parse
errors). Add rationales for choices (e.g., RabbitMQ over Kafka for
simplicity), code examples (e.g., \@RabbitListener for consumers), and
updated modules (e.g., add \'queue\' module). Output the entire revised
spec with these improvements for implementation readiness.\"

These prompts are designed to be iterative---start with one or two, then
feed the outputs back for further refinement. They emphasize practical
details (e.g., dependencies, pseudo-code) to boost first-try success,
while ensuring the spec remains focused and modular. If you need me to
expand on any or simulate LLM responses, let me know!

Tab 7

i feed this feed back

1\. \*\*Prompt for Integrating Message Queue for Parallelization and
Scalability\*\*

\"Review this specification for a LLM-based GitHub code reviewer app,
focusing on the task batching, execution, and LLM invocation sections.
To scale parallelization effectively, incorporate a message queue system
(e.g., using Spring Boot with RabbitMQ or Kafka) where each LLM request
is encapsulated as a message containing the full state context (e.g.,
current file path, diff hunk, node details, rule ID, prompt payload).
Consumers (e.g., worker threads or microservices) should resume from
this state, make synchronous LLM calls, parse the response, and then
update GitHub with review comments or check runs. Add details on queue
configuration (e.g., durable queues, dead-letter handling for failures),
concurrency (e.g., multiple consumers per queue), and integration with
persistence (e.g., store task IDs in DB for resumption). Include error
handling like retries with exponential backoff and partial run recovery.
Provide updated sections for the architecture overview, end-to-end flow
(steps 9-12), and configuration YAML with queue settings. Suggest Spring
Boot dependencies (e.g., spring-boot-starter-amqp) and pseudo-code for
enqueuing/dequeuing tasks.\"

Tab 8

Below is the updated specification content that integrates a message
queue for parallelization and scalability. It augments the architecture
overview, end-to-end flow (notably steps 9--12), configuration YAML,
dependencies, and includes pseudo-code for enqueue/dequeue processing
with retries, DLQ, and persistence-based resumption.

Architecture overview (updated)

\- Spring Boot (Java 17+).

\- Modules:

\- api: REST controllers for triggers and webhooks.

\- github: GitHub integration via hub4j/github-api (primary) and Spring
WebClient (fallback for Checks API).

\- rules: Rule registry, trigger/selector/action evaluation, rule
configuration loader.

\- llm: LangChain4j-based LLM abstraction and providers.

\- prompts: Loader/renderer for system/rule prompts from resources.

\- parser: JavaParser-based AST parsing, diff intersection.

\- review: Orchestrator that builds ReviewRun, creates ReviewTasks, and
enqueues them.

\- mq: Message queue publisher/consumer components for distributing LLM
tasks (RabbitMQ or Kafka).

\- persistence: JPA entities for runs, tasks, findings, and LLM calls.

\- config: Profiles, feature flags, provider settings.

Message queue design

\- Purpose: Decouple task creation from LLM execution to scale
horizontally and enable resiliency.

\- Message model: Each message represents a single ReviewTask with all
state needed to execute the LLM call and post results. Include:

\- runId, taskId

\- repoOwner, repoName, prNumber

\- filePath, fileChangeType

\- nodeType, nodeSignature (e.g., fully qualified method signature),
node ranges

\- nodeModificationStatus

\- ruleId

\- promptPayload: rendered system prompt ref, rule prompt ref, variables
(methodBody, javadocText, annotations, diffSnippet), and serialized
final user prompt text

\- llmProfileId

\- retryCount, createdAt

\- Topology:

\- One queue per task class or a single unified queue with consumers
scaling horizontally. Example default: queue name "code-review.tasks".

\- Dead-letter queue (DLQ) "code-review.tasks.dlq" for messages that
fail after max retries or due to unrecoverable exceptions.

\- Consumers:

\- Spring Boot worker(s) that:

\- Fetch message, load or reconstruct minimal context.

\- Make synchronous LLM call.

\- Parse and persist results.

\- Post to GitHub.

\- Ack on success; Nack/requeue or route to DLQ on failure.

\- Concurrency:

\- Configure multiple consumers per queue; scale worker replicas
horizontally (Kubernetes).

\- Limit in-flight tasks per worker to control rate and cost.

\- Persistence coupling:

\- Store ReviewTask rows (status: PENDING -\> ENQUEUED -\> RUNNING -\>
COMPLETED/FAILED).

\- Message contains taskId; worker updates the same DB row to RUNNING
then COMPLETED/FAILED.

\- Idempotency: If a task reappears (redelivery), check DB status and
skip if already COMPLETED.

End-to-end process flow (updated steps 9--12)

Up to step 8 remains unchanged (AST visitor and rule interest checks).
The following updates steps 9--12.

9\) Task assembly and enqueue

\- For each interested rule-node match, create a ReviewTask in DB:

\- Fields: runId, ruleId, filePath, nodeType, entity signature,
llmProfileId, status=PENDING, attemptCount=0, payloadHash (for
idempotency), createdAt.

\- Render prompt payload:

\- System prompt ref + rule prompt template + variables (method
signature, method body, javadoc, annotations, diff snippet, file/class
context).

\- Serialize a compact "execution envelope" (JSON) with all data needed
by a stateless consumer.

\- Set task status=ENQUEUED, then publish message to queue
"code-review.tasks":

\- Message body: execution envelope with taskId.

\- Headers: runId, ruleId, priority (optional), attemptCount.

10\) Consumer execution (LLM invocation via workers)

\- Worker receives message (taskId).

\- Load ReviewTask from DB; if status is COMPLETED or CANCELED, ack and
stop (idempotency).

\- Update status=RUNNING; increment attemptCount.

\- Resolve LLM profile and provider; instantiate LangChain4j model
client.

\- Compose final prompts from envelope (system + user). Ensure token
limits.

\- Call LLM synchronously (with timeout).

\- Parse JSON response. Validate schema and map to ReviewFinding(s).

\- Persist LLM call metrics and findings. Aggregate partial report state
for the run.

11\) Post-processing and GitHub updates

\- Based on PostingOptions:

\- Create PR inline comments or review summary comment via
GitHubService.

\- Optionally create/update a Check Run with annotations (via WebClient
fallback).

\- On success: mark task COMPLETED, record token usage, duration.

\- Acknowledge message.

12\) Error handling, retries, and DLQ

\- If a transient error occurs (network, 5xx from LLM/GitHub):

\- Nack/requeue with exponential backoff (e.g., via delayed exchange, or
scheduled re-enqueue).

\- Update attemptCount and lastError.

\- If attemptCount exceeds maxRetries, route to DLQ and set
status=FAILED.

\- If a non-retryable error (e.g., prompt schema invalid) occurs:

\- Mark FAILED and send to DLQ immediately with reason.

\- Partial run recovery:

\- ReviewRun can complete even with some FAILED tasks; report includes
failed task summary.

\- An API endpoint can requeue failed tasks by taskId/runId.

Queue configuration and properties

\- RabbitMQ (recommended starter):

\- Durable queue: code-review.tasks, DLQ: code-review.tasks.dlq

\- Dead-letter exchange configured; messages that exceed x-death or are
rejected with requeue=false go to DLQ.

\- Optional delayed exchange or message TTL + DLX for exponential
backoff.

\- Kafka (alternative):

\- Topic: code-review.tasks, partitions for parallelism.

\- DLQ topic: code-review.tasks.dlq.

\- Use consumer groups for worker scaling.

\- Implement retry topics pattern (e.g., code-review.tasks.retry.1m,
.5m, .30m) for backoff.

Spring Boot dependencies

\- RabbitMQ:

\- org.springframework.boot:spring-boot-starter-amqp

\- Kafka (alternative):

\- org.springframework.kafka:spring-kafka

\- Core:

\- spring-boot-starter-web

\- spring-boot-starter-validation

\- spring-boot-starter-data-jpa

\- com.github.javaparser:javaparser-core

\- org.kohsuke:github-api (hub4j/github-api)

\- langchain4j:langchain4j-core (+ provider-specific modules)

\- com.fasterxml.jackson.core:jackson-databind

\- Database driver (Postgres), H2 for dev

Configuration YAML (augmented)

application.yml

github:

appId: \${GITHUB_APP_ID:}

privateKey: \${GITHUB_APP_PRIVATE_KEY:}

installationId: \${GITHUB_APP_INSTALLATION_ID:}

token: \${GITHUB_TOKEN:} \# optional PAT

baseUrl: \${GITHUB_BASE_URL:https://api.github.com}

webhookSecret: \${GITHUB_WEBHOOK_SECRET:}

llm:

profiles:

openai-default:

provider: OPENAI

model: gpt-4o-mini

temperature: 0.2

maxTokens: 2000

systemPromptRef: prompts/system/system_prompt.md

ollama-local:

provider: OLLAMA

model: llama3:instruct

temperature: 0.1

review:

defaultMode: MODIFIED_JAVA_METHODS

postToGitHub: true

maxConcurrentTasks: 4 \# local batching before enqueue

tokenBudgetPerRun: 300000

mq:

type: rabbitmq \# or kafka

rabbitmq:

host: \${RABBITMQ_HOST:localhost}

port: \${RABBITMQ_PORT:5672}

username: \${RABBITMQ_USER:guest}

password: \${RABBITMQ_PASS:guest}

queues:

tasks: code-review.tasks

dlq: code-review.tasks.dlq

concurrency:

consumersPerQueue: 5 \# number of concurrent consumers per worker
instance

prefetch: 5 \# QoS prefetch

retry:

enabled: true

maxAttempts: 5

initialBackoffMs: 2000

multiplier: 2.0

maxBackoffMs: 60000

dlq:

enabled: true

kafka:

bootstrapServers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

topics:

tasks: code-review.tasks

dlq: code-review.tasks.dlq

consumer:

groupId: code-review-workers

concurrency: 5

maxPollRecords: 5

retry:

pattern: topic-based \# or headers-based backoff

attempts: 5

backoffMs: 2000

Pseudo-code for enqueueing tasks (producer)

class TaskPublisher {

\@Autowired RabbitTemplate rabbitTemplate;

\@Value(\"\${mq.rabbitmq.queues.tasks}\") String tasksQueue;

public void publishTask(ReviewTask task, TaskEnvelope envelope) {

// Persist task: status=ENQUEUED

taskRepository.save(task.withStatus(ENQUEUED));

MessageProperties props = new MessageProperties();

props.setContentType(\"application/json\");

props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

props.setHeader(\"runId\", task.getRunId());

props.setHeader(\"ruleId\", task.getRuleId());

props.setHeader(\"attempt\", task.getAttemptCount());

byte\[\] body = objectMapper.writeValueAsBytes(envelope);

Message message = new Message(body, props);

rabbitTemplate.send(\"\", tasksQueue, message); // default exchange

}

}

TaskEnvelope (serialized)

{

\"runId\": \"\...\",

\"taskId\": \"\...\",

\"repoOwner\": \"\...\",

\"repoName\": \"\...\",

\"prNumber\": 123,

\"filePath\": \"src/main/java/\.../Foo.java\",

\"fileChangeType\": \"MODIFIED\",

\"nodeType\": \"METHOD\",

\"nodeSignature\": \"com.example.Foo#bar(String,int)\",

\"nodeRange\": {\"start\": 120, \"end\": 180},

\"nodeModificationStatus\": \"MODIFIED\",

\"ruleId\": \"java-method-quality\",

\"llmProfileId\": \"openai-default\",

\"promptPayload\": {

\"systemPromptRef\": \"prompts/system/system_prompt.md\",

\"rulePromptRef\": \"prompts/rules/java_method_quality.md\",

\"variables\": {

\"className\": \"Foo\",

\"methodSignature\": \"bar(String,int)\",

\"methodAnnotations\": \[\"@Transactional\"\],

\"methodJavadoc\": \"/\*\* \... \*/\",

\"diffSnippet\": \"@@ -120,10 +120,12 @@ \...\",

\"methodBody\": \"public int bar(\...) { \... }\"

},

\"finalUserPrompt\": \"Task: Review a single Java method \...\",

\"tokenBudget\": 2000

},

\"retryCount\": 0,

\"createdAt\": \"\...\"

}

Pseudo-code for consuming tasks (worker)

\@RabbitListener(queues = \"\${mq.rabbitmq.queues.tasks}\",

concurrency = \"\${mq.rabbitmq.concurrency.consumersPerQueue}\")

public void onMessage(Message msg, Channel channel) {

long deliveryTag = msg.getMessageProperties().getDeliveryTag();

try {

TaskEnvelope env = objectMapper.readValue(msg.getBody(),
TaskEnvelope.class);

ReviewTask task = taskRepository.findById(env.getTaskId())

.orElseThrow(() -\> new IllegalStateException(\"Task not found\"));

if (task.getStatus() == COMPLETED \|\| task.getStatus() == CANCELED) {

channel.basicAck(deliveryTag, false);

return;

}

taskRepository.markRunning(task.getId());

LlmClient client = llmClientFactory.fromProfile(env.getLlmProfileId());

String systemPrompt =
promptLoader.loadSystem(env.getPromptPayload().getSystemPromptRef());

String userPrompt = env.getPromptPayload().getFinalUserPrompt();

LLMResponse resp = client.generate(LLMRequest.of(systemPrompt,
userPrompt));

ReviewFindings findings = findingsParser.parse(resp.getText());

persistenceService.persistFindings(env.getRunId(), env.getTaskId(),
findings, resp.getUsage());

githubPostingService.postResults(env, findings);

taskRepository.markCompleted(task.getId(), resp.getUsage(),
Instant.now());

channel.basicAck(deliveryTag, false);

} catch (TransientException te) {

// Requeue with backoff or route to retry mechanism

handleRetry(msg, channel, deliveryTag, te);

} catch (NonRetryableException nre) {

routeToDlq(msg, channel, deliveryTag, nre);

} catch (Exception e) {

handleRetry(msg, channel, deliveryTag, e); // default: treat as
transient up to max attempts

}

}

Retry/backoff strategy

\- RabbitMQ options:

\- Use a delayed exchange plugin or TTL+DLX pattern.

\- On retry:

\- Increment attemptCount in DB.

\- Republish message to a delayed queue with x-delay computed as
min(maxBackoff, initialBackoff \* multiplier\^(attempt-1)).

\- Nack original message without requeue (to avoid immediate
redelivery).

\- Kafka options:

\- Use retry topics (code-review.tasks.retry.2s / 10s / 60s) with
increasing delays.

\- After final retry, publish to DLQ topic and mark task FAILED.

DLQ handling and observability

\- A separate consumer or admin UI can inspect DLQ messages.

\- Provide an admin API: POST /api/reviews/tasks/{taskId}/requeue to
move a FAILED task back to the main queue after fixing root causes.

\- Metrics:

\- tasks.enqueued, tasks.completed, tasks.failed

\- queue.depth, consumer.lag (Kafka)

\- llm.latency, llm.tokens, llm.cost (if estimable)

\- retries.count, dlq.count

Partial run recovery

\- ReviewRun status can be COMPLETED_WITH_ERRORS if some tasks failed.

\- A follow-up trigger can re-enqueue only FAILED tasks for a given run.

\- Idempotent posting to GitHub (e.g., detect duplicates, update
existing check runs).

Security notes

\- Validate and sanitize all fields in TaskEnvelope.

\- Apply rate limiting at the worker for GitHub and LLM calls.

\- Store minimal necessary code snippets in messages; avoid full file
content unless needed.

This update adds a robust queue-backed execution layer that enables
horizontal scaling, resiliency via retries/DLQ, and clear separation of
orchestration from execution while preserving the previously defined
rule engine, AST visitor, and prompt composition model.

Tab 9

\## 2. \*\*Prompt for Enhancing Prompt Structure and Parsing\*\*

\"Audit the prompt composition and LLM output parsing sections in this
spec, along with the example prompts (system_prompt.md,
java_method_quality.md, javadoc_audit.md). Update them to ensure outputs
are easy to parse and suitable for GitHub: Instruct the LLM to always
start responses with either \'No, there is no need to modify the code;
it is already at a good level.\' (if no issues) or \'Yes, please improve
the quality of the code.\' (if issues found), followed by justifications
for any suggestions. Require all outputs to use Markdown formatting
(e.g., \*\*bold\*\* for titles, code blocks for snippets, bullet lists
for issues) so they can be directly posted as formatted GitHub comments.
Revise the JSON schemas in rule prompts to include these elements (e.g.,
add a \'decision\' field for the starting phrase, \'justification\'
array, and \'markdown_comment\' field). Add parsing logic in the app
(e.g., using regex or Jackson to extract decision and Markdown). Include
new guidelines in the system prompt to enforce this structure and
prevent hallucinations. Provide fully updated example prompt files with
these changes, and suggest how to handle non-compliant outputs (e.g.,
retry with clarification).\"

further feedback:

Below is the updated specification content that integrates the requested
enhancements to prompt structure, composition, output parsing, and
example prompts. This audit focuses on making LLM outputs more parseable
(via enforced starting phrases and JSON fields), GitHub-friendly
(Markdown formatting), and reliable (guidelines against hallucinations,
handling non-compliance). I\'ve revised the relevant sections, updated
the example prompt files (system_prompt.md, java_method_quality.md,
javadoc_audit.md), modified JSON schemas in rule prompts, added app-side
parsing logic with examples, and included retry mechanisms for
non-compliant outputs. These changes align with the overall spec while
preserving existing elements like rule engine and task execution.

Prompt composition (updated section)

\- Compose prompts as before: system prompt (global) + rule-specific
user prompt (task + context + variables).

\- New requirements:

\- All LLM responses must start with one of two exact phrases: \"No,
there is no need to modify the code; it is already at a good level.\"
(no issues) or \"Yes, please improve the quality of the code.\" (issues
found).

\- Follow the phrase immediately with justifications (bullet points
explaining why, with rationale and examples).

\- Format the entire output in Markdown for direct GitHub posting (e.g.,
\*\*bold\*\* for emphasis, - bullets for lists, \`\`\`java for code
snippets).

\- Enforce this via updated system prompt guidelines and rule prompt
instructions.

\- Outputs are JSON-wrapped for parsing, with new fields: \"decision\"
(the starting phrase), \"justifications\" (array of strings),
\"markdown_comment\" (full Markdown-formatted response body), plus
existing schema elements.

\- Token budget: Trim content to fit; if exceeded, prioritize method
body/diff and warn in prompt.

\- Variables injected: As before (e.g., {{filePath}}, {{methodBody}}),
plus any needed for new structure.

LLM invocation and output parsing (updated section)

\- Use LangChain4j to call the model, preferring providers with JSON
mode or tool calling (e.g., OpenAI) for schema enforcement.

\- Expect JSON outputs; parse with Jackson (ObjectMapper) to
ReviewFinding objects.

\- New parsing logic:

\- Validate \"decision\" field matches one of the two phrases exactly.

\- Extract \"justifications\" for auditing/persistence.

\- Use \"markdown_comment\" directly for GitHub posting (e.g., as PR
comment body).

\- If output is non-compliant (e.g., wrong phrase, malformed JSON, no
Markdown), handle via retry: Re-invoke LLM with a clarification prompt
appended (e.g., \"Your previous response did not follow the required
format. Please adhere strictly to the decision phrase, justifications,
and Markdown structure. Rephrase your output accordingly.\").

\- Retries: Max 2 attempts; on failure, log raw output, mark finding as
\"PARSE_ERROR\", and post a fallback summary (e.g., \"Automated review
failed due to output formatting issues.\").

\- Record raw response for debugging.

\- Pseudo-code for parsing (in LlmResponseHandler or similar):

\`\`\`java

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

public ReviewFindings parse(String llmText) {

ObjectMapper mapper = new ObjectMapper();

try {

JsonNode root = mapper.readTree(llmText);

String decision = root.path(\"decision\").asText();

if (!decision.equals(\"No, there is no need to modify the code; it is
already at a good level.\") &&

!decision.equals(\"Yes, please improve the quality of the code.\")) {

throw new InvalidFormatException(\"Invalid decision phrase\");

}

List\<String\> justifications =
mapper.convertValue(root.path(\"justifications\"), new
TypeReference\<List\<String\>\>(){});

String markdownComment = root.path(\"markdown_comment\").asText();

// Extract other schema fields (e.g., issues array)

List\<Issue\> issues = mapper.convertValue(root.path(\"issues\"), new
TypeReference\<List\<Issue\>\>(){});

// Regex fallback if JSON fails: e.g., Pattern.compile(\"\^((No\|Yes),
.+?\\\\.)\"); but prefer Jackson

return new ReviewFindings(decision, justifications, markdownComment,
issues);

} catch (Exception e) {

// Trigger retry or fallback

log.error(\"Parsing failed: {}\", e);

return ReviewFindings.error(\"PARSE_ERROR\", llmText);

}

}

\`\`\`

\- For non-JSON-supporting providers (e.g., Ollama), use regex to
extract sections post-call, then convert to JSON internally.

System prompt (updated: system_prompt.md)

You are a highly competent, pragmatic senior Java engineer performing
focused code reviews. Your priorities are:

1\) Correctness and clarity of logic in the reviewed scope.

2\) Maintainability and readability (naming, decomposition, comments).

3\) Safe and idiomatic Java practices.

4\) Lightweight performance considerations when relevant.

Guidelines:

\- Review only the provided scope. Do not speculate about code outside
the provided context.

\- Prefer small, actionable suggestions with clear rationale and
examples.

\- If context is insufficient, state "insufficient context" in
justifications.

\- Favor simplicity. Avoid overengineering.

\- Do not request null checks for public method parameters unless the
method's contract or annotations require them.

\- Methods should remain reasonably small; prefer extraction of helper
methods if they grow too large or do multiple things.

\- Follow Java conventions and any provided style guides.

\- Strictly enforce output structure: Start with EXACTLY one of: \"No,
there is no need to modify the code; it is already at a good level.\" or
\"Yes, please improve the quality of the code.\" Follow immediately with
justifications as bullet points. Use Markdown throughout (e.g.,
\*\*bold\*\*, - lists, \`\`\`java code blocks).

\- Prevent hallucinations: Base all suggestions on provided code only;
if no issues, use the \"No\" phrase and explain why it\'s good.

\- Output as JSON matching the schema in the user prompt. Keep responses
concise and specific.

Example user prompt for Java Single-Method Quality Review (updated:
java_method_quality.md)

Task: Review a single Java method that was changed in this pull request.

Context:

\- File: {{filePath}}

\- Class: {{className}}

\- Method: {{methodSignature}}

\- Annotations: {{methodAnnotations}}

\- Javadoc (if any):

{{methodJavadoc}}

Diff snippet (for this method):

{{diffSnippet}}

Method implementation:

\`\`\`java

{{methodBody}}

\`\`\`

Rules to apply:

1\) Encourage small, focused methods; suggest extractions if the method
does multiple things or is overly long.

2\) Do not require null checks for input parameters unless annotations
or contract demand them.

3\) Prefer clear names, simple control flow, and early returns when
helpful.

4\) Suggest tests if critical logic is present or edge cases are
apparent.

5\) Point out obvious performance pitfalls only if evident and
low-effort to fix.

Output structure:

\- Start your response text with EXACTLY: \"No, there is no need to
modify the code; it is already at a good level.\" (if no issues) or
\"Yes, please improve the quality of the code.\" (if issues).

\- Follow immediately with justifications: Bullet points explaining why
(for \"Yes\", detail issues and rationales; for \"No\", explain
strengths).

\- Use Markdown formatting throughout for readability (e.g., \*\*Issue
Title\*\*, - Bullet points, \`\`\`java for suggested fixes).

\- Wrap the entire output in JSON.

Output JSON schema:

{

\"decision\": \"exact starting phrase (No\... or Yes\...)\",

\"justifications\": \[\"array of justification strings\"\],

\"markdown_comment\": \"full Markdown-formatted response text (including
decision and justifications)\",

\"summary\": \"short overview\",

\"issues\": \[

{

\"category\":
\"maintainability\|readability\|correctness\|performance\|testing\",

\"severity\": \"LOW\|MEDIUM\|HIGH\",

\"title\": \"short title\",

\"description\": \"what and why\",

\"code_refs\": \[{\"line\": number, \"hint\": \"optional\"}\],

\"suggested_fix\": \"concrete suggestion or snippet\"

}

\]

}

If no issues: {\"decision\": \"No\...\", \"justifications\":
\[\"explanations of why good\"\], \"markdown_comment\": \"Markdown
text\", \"summary\": \"OK\", \"issues\": \[\]}.

Example user prompt for Javadoc Audit (updated: javadoc_audit.md)

Task: Audit the following Javadoc for typos, grammar, clarity, and
consistency with parameters and return type.

Context:

\- File: {{filePath}}

\- Element: {{elementType}} {{elementSignature}} // e.g., method or
class

\- Parameters (with types and names): {{parameters}}

\- Return type: {{returnType}}

Javadoc:

\"\"\"

{{javadocText}}

\"\"\"

Rules to apply:

1\) Correct typos and grammatical errors; propose improved wording while
preserving intent.

2\) Ensure \@param and \@return tags match actual parameters and return
type.

3\) Suggest concise and active-voice phrasing.

Output structure:

\- Start your response text with EXACTLY: \"No, there is no need to
modify the code; it is already at a good level.\" (if no issues) or
\"Yes, please improve the quality of the code.\" (if issues).

\- Follow immediately with justifications: Bullet points explaining why
(for \"Yes\", detail issues and rationales; for \"No\", explain
strengths).

\- Use Markdown formatting throughout for readability (e.g., \*\*Issue
Title\*\*, - Bullet points, \`\`\`java for suggested fixes).

\- Wrap the entire output in JSON.

Output JSON schema:

{

\"decision\": \"exact starting phrase (No\... or Yes\...)\",

\"justifications\": \[\"array of justification strings\"\],

\"markdown_comment\": \"full Markdown-formatted response text (including
decision and justifications)\",

\"summary\": \"short overview\",

\"typos_corrections\": \[

{\"original\": \"string\", \"suggestion\": \"string\", \"reason\":
\"grammar/spelling/clarity\"}

\],

\"tag_issues\": \[

{\"type\": \"PARAM_MISMATCH\|RETURN_MISMATCH\|MISSING_TAG\|EXTRA_TAG\",
\"detail\": \"string\"}

\],

\"suggested_javadoc\": \"fully revised Javadoc text\"

}

If no issues: {\"decision\": \"No\...\", \"justifications\":
\[\"explanations of why good\"\], \"markdown_comment\": \"Markdown
text\", \"summary\": \"OK\", \"typos_corrections\": \[\],
\"tag_issues\": \[\], \"suggested_javadoc\": \"{{javadocText}}\"}.

Handling non-compliant outputs

\- In the consumer/worker (e.g., after LLM call in queue processing): If
parsing throws InvalidFormatException, retry the LLM invocation with an
appended clarification user prompt: \"Your response did not match the
required structure. Please output JSON with the exact \'decision\'
phrase, justifications array, and Markdown-formatted
\'markdown_comment\'. Do not add extra text. Here is the original task:
\[original user prompt\]\".

\- Max retries: 2; on final failure, persist raw output as a finding
with status \"FORMAT_ERROR\" and post a generic GitHub comment: \"Review
encountered formatting issues; manual review recommended.\"

\- Log non-compliance rates for monitoring prompt effectiveness.

This update ensures outputs are structured for easy parsing and direct
use in GitHub, reduces hallucinations via strict guidelines, and adds
robustness with retries. It integrates seamlessly with the queue system
(e.g., retries can requeue the task if needed).

\### Feedback on the Output for Point 2 (Enhancing Prompt Structure and
Parsing)

Overall, this output is a high-quality refinement that directly
addresses the prompt\'s requests. It thoughtfully updates the spec
sections (e.g., prompt composition, parsing) and provides fully revised
example files with the new structure (decision phrases, justifications,
Markdown). The addition of JSON fields like \"decision\",
\"justifications\", and \"markdown_comment\" makes parsing
straightforward, while the pseudo-code and retry logic add practical
implementation details. This enhances reliability and GitHub
integration, increasing first-try success by making outputs more
predictable and post-ready. The changes align well with the original
spec\'s focus on Java rules and efficiency.

\*\*Strengths\*\*:

\- \*\*Structure and Parseability\*\*: The enforced phrases and new JSON
fields are integrated cleanly into schemas, with clear instructions to
prevent deviations. System prompt updates (e.g., anti-hallucination
guidelines) are proactive and comprehensive.

\- \*\*GitHub Suitability\*\*: Requiring Markdown throughout (with
examples like \*\*bold\*\*, code blocks) ensures outputs can be posted
verbatim, improving usability.

\- \*\*Parsing and Error Handling\*\*: Jackson-based pseudo-code is
Spring Boot-appropriate and robust; regex fallback is a smart addition
for edge cases. Retry mechanism with clarification prompts is
well-detailed, tying into queue resilience.

\- \*\*Completeness\*\*: Updated all specified files (system and rule
prompts), added handling for non-compliance, and kept token awareness.

\- \*\*Alignment\*\*: Doesn\'t disrupt prior elements (e.g., variables,
rules) and enhances them (e.g., schemas now include new fields without
bloat).

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Verbosity in Prompts\*\*: The updated user prompts repeat
\"Output structure\" instructions---consider factoring some into the
system prompt to reduce token usage, especially for long contexts.

\- \*\*Schema Flexibility\*\*: JSON schemas are revised well, but lack
optional fields for edge cases (e.g., \"insufficient_context\" flag in
justifications). Add validation for Markdown escaping (e.g., ensure no
invalid chars for GitHub).

\- \*\*Provider Compatibility\*\*: Mentions preferring JSON-mode
providers, but for Ollama, regex parsing could fail on complex
outputs---suggest a dedicated fallback parser class with more examples.

\- \*\*Testing Gaps\*\*: No updates to the testing section (e.g., add
tests for phrase extraction or retry flows). Recommend scenarios like
\"mock LLM response with wrong decision---verify retry and fallback
comment.\"

\- \*\*Edge Cases\*\*: Doesn\'t cover if LLM ignores Markdown (e.g.,
plain text output)---add a post-parse sanitizer to force Markdown if
needed. Also, for \"No\" cases, justifications could be optional but are
required here; clarify if empty array is allowed.

\- \*\*Minor Inconsistencies\*\*: The spec assumes JSON wrapping, but
some providers might not support it natively---expand on using
LangChain4j\'s structured output features. Pseudo-code uses
\`InvalidFormatException\` but doesn\'t define it (add a note to create
a custom exception).

\- \*\*Readability\*\*: Outputs are detailed, but could include a sample
LLM response JSON for illustration (e.g., under schemas).

This output is about 85-90% ready---strong on core enhancements, but
polishing testing and edges would make it even better for
implementation. If you\'d like refinements or the next prompt\'s output,
I\'m ready!

Tab 10

Excellent improvement point. Below is an updated spec section
integrating enhanced prompt structure and parsing, fully aligned with
the prior design (queue-based execution, rule engine, AST visitor), and
including concrete prompt files, JSON schema changes, parsing logic,
non-compliance handling, and testing guidance.

Updated prompt composition and output structure

\- Composition remains: system prompt + rule-specific user prompt +
context variables.

\- New global requirements:

\- All LLM responses must begin with one of the exact phrases:

\- No, there is no need to modify the code; it is already at a good
level.

\- Yes, please improve the quality of the code.

\- Immediately follow with justifications in bullet list form.

\- Entire response must be Markdown-formatted, suitable for GitHub
comments:

\- Use bold for issue titles.

\- Use - bullet lists.

\- Use fenced code blocks (\`\`\`java) for code snippets.

\- Responses must be wrapped in JSON with fields:

\- decision: one of the exact phrases above.

\- justifications: array of strings (bulleted rationale).

\- markdown_comment: full Markdown body (including decision and
justifications), ready to post to GitHub.

\- summary: short overview.

\- issues: array of structured issues (for method quality rule).

\- For Javadoc audit: typos_corrections, tag_issues, suggested_javadoc.

\- Anti-hallucination and compliance guidelines included in the system
prompt.

\- Token budget: prioritize method body/diff snippet, then annotations,
then javadoc; truncate class context if needed.

Updated LLM invocation and parsing

\- Prefer providers/models that support structured output or JSON mode
(OpenAI/Azure OpenAI).

\- Parsing:

\- Validate decision equals exactly one of the two required phrases.

\- Collect justifications array.

\- Use markdown_comment directly for GitHub posting.

\- Map issues array (or javadoc fields) into ReviewFinding(s).

\- Non-compliance handling:

\- If JSON parsing fails or decision phrase is invalid:

\- Retry up to 2 times with a clarification prompt appended:

\- "Your previous response did not follow the required format. Please
output valid JSON with fields decision, justifications,
markdown_comment, and the schema below. Start with the exact decision
phrase. Do not add extra text outside the JSON."

\- If still invalid, persist a FORMAT_ERROR finding with raw output, and
post a fallback GitHub comment indicating formatting failure.

\- Provider compatibility:

\- For providers without robust JSON support (e.g., some Ollama models),
attempt JSON parse first, fallback to regex section extraction, then
reconstruct JSON.

Updated example prompt files

resources/prompts/system/system_prompt.md

You are a highly competent, pragmatic senior Java engineer performing
focused code reviews. Priorities:

1\) Correctness and clarity in the reviewed scope only.

2\) Maintainability and readability.

3\) Safe and idiomatic Java practices.

4\) Lightweight performance considerations when relevant.

Behavior and formatting:

\- Review only the provided scope; do not speculate about unseen code.

\- Provide small, actionable suggestions with clear rationale and
examples.

\- If context is insufficient, say "insufficient context" in
justifications.

\- Do not require null checks for public method parameters unless
annotations/contract require them.

\- Methods should remain reasonably small; suggest extractions if doing
multiple things or too long.

\- Follow Java conventions and any provided style guides.

\- Strict output structure:

\- Start with EXACTLY one of:

\- "No, there is no need to modify the code; it is already at a good
level."

\- "Yes, please improve the quality of the code."

\- Immediately follow with justifications as bullet points.

\- Use Markdown formatting for the entire response, suitable for GitHub
(bold text, bullet lists, fenced code blocks).

\- Wrap the entire response in a single JSON object according to the
user prompt schema.

\- Prevent hallucinations:

\- Base all suggestions on provided code/diff only.

\- If no issues, use the "No" phrase and briefly explain why it is good.

\- Be concise. Do not add text outside the JSON object.

resources/prompts/rules/java_method_quality.md

Task: Review a single Java method changed in this PR.

Context:

\- File: {{filePath}}

\- Class: {{className}}

\- Method: {{methodSignature}}

\- Annotations: {{methodAnnotations}}

\- Javadoc (if any):

{{methodJavadoc}}

Diff snippet (for this method):

{{diffSnippet}}

Method implementation:

\`\`\`java

{{methodBody}}

\`\`\`

Rules to apply:

1\) Encourage small, focused methods; suggest extractions if the method
does multiple things or is overly long.

2\) Do not require null checks for input parameters unless annotations
or contract demand them.

3\) Prefer clear names, simple control flow, early returns when helpful.

4\) Suggest tests if critical logic or edge cases are apparent.

5\) Flag obvious low-effort performance pitfalls if evident.

Output format requirements:

\- Start with EXACT decision phrase ("No..." or "Yes..."), then bullet
justifications.

\- Entire body must be valid Markdown and wrapped in JSON below.

\- Do not include extra text outside the JSON.

Output JSON schema:

{

\"decision\": \"No, there is no need to modify the code; it is already
at a good level. \| Yes, please improve the quality of the code.\",

\"justifications\": \[\"bullet point 1\", \"bullet point 2\"\],

\"markdown_comment\": \"Full Markdown response ready for GitHub comment,
including decision and bullet justifications, bold issue titles, and
fenced code blocks for snippets.\",

\"summary\": \"Short overview\",

\"issues\": \[

{

\"category\":
\"maintainability\|readability\|correctness\|performance\|testing\",

\"severity\": \"LOW\|MEDIUM\|HIGH\",

\"title\": \"short title\",

\"description\": \"what and why\",

\"code_refs\": \[{\"line\": 123, \"hint\": \"optional\"}\],

\"suggested_fix\": \"concrete suggestion or Java snippet\"

}

\]

}

If no issues: set decision to "No...", provide brief justifications
praising strengths, summary "OK", and issues as \[\].

resources/prompts/rules/javadoc_audit.md

Task: Audit the following Javadoc for typos, grammar, clarity, and
consistency with parameters and return type.

Context:

\- File: {{filePath}}

\- Element: {{elementType}} {{elementSignature}}

\- Parameters: {{parameters}}

\- Return type: {{returnType}}

Javadoc:

\"\"\"

{{javadocText}}

\"\"\"

Rules to apply:

1\) Correct typos and grammar; preserve original intent.

2\) Ensure \@param and \@return tags match actual parameters and return
type.

3\) Prefer concise, active-voice phrasing.

Output format requirements:

\- Start with EXACT decision phrase ("No..." or "Yes..."), then bullet
justifications.

\- Entire body must be valid Markdown and wrapped in JSON below.

\- Do not include extra text outside the JSON.

Output JSON schema:

{

\"decision\": \"No, there is no need to modify the code; it is already
at a good level. \| Yes, please improve the quality of the code.\",

\"justifications\": \[\"bullet point 1\", \"bullet point 2\"\],

\"markdown_comment\": \"Full Markdown response ready for GitHub comment,
including decision and bullet justifications.\",

\"summary\": \"Short overview\",

\"typos_corrections\": \[

{\"original\": \"strng\", \"suggestion\": \"string\", \"reason\":
\"spelling\"}

\],

\"tag_issues\": \[

{\"type\": \"PARAM_MISMATCH\|RETURN_MISMATCH\|MISSING_TAG\|EXTRA_TAG\",
\"detail\": \"string\"}

\],

\"suggested_javadoc\": \"/\*\* Revised Javadoc text here\... \*/\"

}

If no issues: set decision to "No...", provide brief justifications of
strengths, summary "OK", typos_corrections \[\], tag_issues \[\], and
suggested_javadoc equal to input text.

App-side parsing logic and enforcement

\- Parsing validator:

\- Ensure response is a single JSON object (no leading/trailing text).

\- Validate decision equals one of the two phrases.

\- Validate justifications is a non-null array (allow empty for "No" but
prefer at least one positive justification).

\- Validate markdown_comment is non-empty Markdown string.

\- Use Jackson for primary parsing; add a Regex fallback extractor if
JSON parsing fails (then wrap extracted components into a compliant
object).

\- Post-processing:

\- Use markdown_comment directly for GitHub comment body.

\- Also map "issues" or "javadoc" fields into structured ReviewFinding
records for reporting.

Java pseudo-code for parsing and retry

public ParsedResponse parseOrRetry(String llmText, Supplier\<String\>
clarificationPromptSupplier) {

try {

return parseStrictJson(llmText);

} catch (InvalidFormatException e) {

// First retry

String retryPrompt = clarificationPromptSupplier.get();

String retryResponse = llmClient.generate(LLMRequest.of(systemPrompt,
retryPrompt)).getText();

try {

return parseStrictJson(retryResponse);

} catch (Exception e2) {

// Second retry with stronger instruction

String retryPrompt2 = retryPrompt + \"\\nImportant: Output only valid
JSON; do not include Markdown outside the JSON string fields.\";

String retryResponse2 = llmClient.generate(LLMRequest.of(systemPrompt,
retryPrompt2)).getText();

return parseStrictJsonOrFail(retryResponse2);

}

}

}

private ParsedResponse parseStrictJson(String text) {

JsonNode root = mapper.readTree(text.trim());

String decision = requiredText(root, \"decision\");

if (!Objects.equals(decision, NO_PHRASE) && !Objects.equals(decision,
YES_PHRASE)) {

throw new InvalidFormatException(\"Invalid decision phrase\");

}

String md = requiredText(root, \"markdown_comment\");

ArrayNode just = requiredArray(root, \"justifications\");

// parse issues/typos fields as needed

return toParsedResponse(root);

}

Handling non-compliant outputs

\- Retry strategy:

\- 2 retries per task with a clarification prompt that restates the
exact decision phrase requirement and JSON fields.

\- If still failing, mark task FORMAT_ERROR, persist raw output, and
post a minimal GitHub comment:

\- "Automated review formatting failed. Here is the raw response (may be
unformatted): ..."

\- Sanitization:

\- If markdown_comment is empty but issues exist, generate a minimal
Markdown summary from structured fields to ensure a valid GitHub
comment.

Testing additions

\- Unit tests:

\- Valid JSON with "No" decision and empty issues → parsed correctly.

\- Valid JSON with "Yes" decision and populated issues → parsed
correctly.

\- Invalid decision phrase → triggers retry then failure.

\- Non-JSON plaintext response → retry and fallback parsing path.

\- Integration tests:

\- Mock LLM returns malformed JSON first, then valid on retry → ensure
success and single GitHub post.

\- Ensure markdown_comment posts render correctly on GitHub (basic smoke
via Markdown snapshots).

\- Property-based tests (optional):

\- Randomly drop required fields to ensure parser errors and retry path
are robust.

Notes on token efficiency

\- Keep detailed structural enforcement primarily in the system prompt
to reduce repetition.

\- In rule prompts, keep the schema concise and refer to the system's
"strict output structure" to save tokens.

\- For large methods, truncate body with a clear NOTE in the user prompt
that context was truncated, prioritizing changed sections and diff
snippet.

This enhancement makes outputs deterministic, parseable, and
GitHub-ready, while providing robust fallback and retry behavior for
non-compliant generations.

Tab 11

grok point of situation on spec:

\### Feedback on the Output for Point 2 (Enhancing Prompt Structure and
Parsing)

This output is another strong iteration, building effectively on the
previous spec while addressing the prompt\'s goals. It refines the
composition, invocation/parsing sections, and example files with the
required Yes/No phrases, justifications, Markdown, and JSON wrappers.
The anti-hallucination guidelines, token efficiency notes, and retry
logic are well-integrated, making outputs more reliable and
GitHub-ready. Pseudo-code for parsing/retry is detailed and practical
(e.g., using Jackson with fallbacks), and the addition of testing
additions ties in nicely. Overall, it enhances parseability without
overcomplicating, boosting first-try success by emphasizing strict
enforcement and fallbacks.

\*\*Strengths\*\*:

\- \*\*Enforced Structure\*\*: The exact phrases, bullet justifications,
and Markdown are mandated clearly in system/rule prompts, with JSON
schemas updated consistently (e.g., \"decision\" as a string match,
\"markdown_comment\" for posting). This reduces variability.

\- \*\*Parsing and Resilience\*\*: Strict validation in pseudo-code
(e.g., phrase checks, required fields) is robust; retry with
clarification is detailed, including a second escalation. Fallback for
non-JSON providers (regex) is thoughtful.

\- \*\*Example Files\*\*: Fully updated MD files are complete and
consistent, with schema examples including \"No\" case handling.
Instructions like \"Do not include extra text outside the JSON\" prevent
common LLM issues.

\- \*\*Testing and Efficiency\*\*: New testing additions
(unit/integration for parsing/retries) are specific and actionable.
Token notes (e.g., truncate priorities) show awareness of LLM limits.

\- \*\*Alignment\*\*: Seamlessly fits prior spec (e.g., queue mentions
in handling, rule variables unchanged).

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Schema Details\*\*: The \"decision\" field in schemas uses \"\|\"
for options, which might confuse parsers---suggest quoting as a literal
string example. Justifications require bullets but are arrays of
strings; clarify if the array should include Markdown (e.g., \"-
point\") or plain text (with Markdown built in post-parse).

\- \*\*Retry Depth\*\*: Retries are good, but no tie-in to queue (e.g.,
requeue on final failure instead of just marking error). Clarify if
retries count toward task attemptCount in DB.

\- \*\*Markdown Validation\*\*: Assumes LLM produces valid Markdown, but
no app-side sanitizer (e.g., escape invalid chars for GitHub). Add a
post-parse step to ensure renderability.

\- \*\*Provider-Specifics\*\*: Mentions preferring JSON-mode providers,
but lacks config (e.g., in llm.profiles, enable json_mode=true for
OpenAI). For Ollama, regex fallback could use examples (e.g., Pattern
for phrase extraction).

\- \*\*Edge Cases\*\*: Doesn\'t cover if justifications are empty for
\"Yes\" (require at least one?); or multi-issue Markdown structure
(e.g., suggest subheadings). No sample full JSON response for
illustration.

\- \*\*Testing Gaps\*\*: Tests are added but basic---expand to cover
fallback GitHub posts on failure, or property tests for schema
variations.

\- \*\*Minor Inconsistencies\*\*: System prompt says \"Wrap the entire
response in a single JSON object\" but rule prompts say \"wrapped in
JSON below\"---ensure uniformity. Pseudo-code uses \"Supplier\<String\>
clarificationPromptSupplier\" but doesn\'t show how it\'s supplied
(e.g., from envelope).

\- \*\*Readability/Conciseness\*\*: Lengthy, with some repetition (e.g.,
phrase instructions in every file)---consider a shared
\"output_instructions.md\" reference.

This output is \~85% polished; it\'s functional but could benefit from
tighter integration with queues/DB and more examples for clarity.

\### Relevance of Remaining Prompts (3-7) Based on Current Spec

All of prompts 3-7 are still highly relevant, as the current spec has
incorporated prompt structuring but hasn\'t fully merged it with queues,
resilience, rules YAML, testing, or a holistic view. The spec is
building incrementally, so these would weave in the new features (e.g.,
Yes/No + Markdown) while addressing gaps like queue-LLM integration and
overall robustness. Here\'s a quick evaluation of each, with why it\'s
relevant and minor tweaks if needed (to reference the latest output):

\- \*\*Prompt 3 (Combining Queue with LLM and GitHub Updates)\*\*: Still
fully relevant. The current spec mentions queues briefly in handling but
lacks detailed integration (e.g., how structured outputs are
parsed/posted in consumers). This would add enqueue payload details for
prompts and edge cases like timeouts, directly building on the new
parsing logic.

\- \*\*Prompt 4 (Overall Resilience and Error Handling)\*\*: Very
relevant. Current output has prompt retries but not queue-level
resilience (e.g., requeuing on parse failure) or timeouts. It could
enhance non-functionals like scalability, which aren\'t updated yet.
Tweak: Add \"Incorporate the latest parsing pseudo-code and retry
clarification prompts.\"

\- \*\*Prompt 5 (Rule-Specific Updates and Examples)\*\*: Relevant, but
partially addressed (prompt MD files are updated, but not YAML configs
or a new rule example). This would align YAML (e.g.,
action.resultFormat=JSON_WITH_DECISION) and add a full-file summary
rule, plus parsing snippets---still needed for completeness.

\- \*\*Prompt 6 (Testing and Validation)\*\*: Highly relevant. Current
spec adds some testing for prompts but not queue integration or full E2E
with new structures (e.g., mock Yes/No responses in queues). This
expands coverage without overlap.

\- \*\*Prompt 7 (Holistic Refinement)\*\*: Most relevant now, as the
spec is maturing. It would consolidate everything (queues + structured
prompts) into a unified doc, resolving gaps like diff/AST edges. Use
this last to finalize.

\### Would I Iterate More on This Specification?

Yes, absolutely---I recommend iterating further to create a cohesive,
end-to-end spec. The current version is strong on prompts but
fragmented; continuing with 3-7 will integrate queues, resilience, and
testing fully, reducing implementation risks. Start with 3 or 4 for
queue merging, then 5-6 for details, and end with 7 for polish. This
modular approach has worked well so far.

\### Comprehensive Feedback Prompts to Send to the LLM for Further
Improvement

Here are refined prompts (slightly tweaked for the latest spec, e.g.,
referencing new pseudo-code). Paste the current spec output into the LLM
query.

1\. \*\*Refined Prompt 3\*\*:

\"Enhance the review orchestration, posting, and queue sections of this
GitHub code reviewer spec to integrate message queues with the new
structured LLM prompts and outputs. For each ReviewTask, enqueue a
message with serialized context (e.g., JSON payload including file, diff
hunk, AST node, rendered prompt with Yes/No instructions). Consumers
dequeue, invoke the LLM synchronously, parse the structured JSON
(validating \'decision\' phrase, justifications, and markdown_comment),
then post the markdown_comment to GitHub (e.g., inline comment tied to
diff hunk lines). Add flow for resuming interrupted tasks from queue or
DB state. Include edge cases like queue overflow (rate-limit enqueuing),
LLM timeouts (requeue with priority), and GitHub failures (store pending
comments in DB for retry). Update the persistence schema to track queue
message IDs and task states (ENQUEUED, PROCESSING, COMPLETED). Provide
pseudo-code for a QueueService class (using Spring\'s RabbitTemplate)
and revised end-to-end flow steps 9-12, aligning with the latest parsing
logic.\"

2\. \*\*Refined Prompt 4\*\*:

\"Refine the entire specification for this LLM-powered code reviewer,
incorporating message queues for parallel LLM requests and the new
structured prompts with \'Yes/No\' decisions, justifications, and
Markdown outputs. Add resilience features: For queues, handle consumer
failures by requeuing with context restoration; for prompts, extend the
existing retry logic if output doesn\'t match the required format (e.g.,
invalid decision phrase). Ensure GitHub posts use markdown_comment
directly for well-formatted comments. Address issues like high-latency
LLMs (timeout queues after 5min), or parsing failures (fallback to raw
text posting with warning, building on the pseudo-code). Update
non-functional requirements for scalability (horizontal scaling of
consumers) and testing (mock queues with Testcontainers). Provide a
revised architecture diagram in text form (ASCII art) showing queue
integration, and add config options like queue names and consumer counts
in application.yml.\"

3\. \*\*Refined Prompt 5\*\*:

\"Update the rule types, examples, and YAML configs in this spec to
align with the new prompt structuring. For rules like Java Single-Method
Quality Review and Javadoc Audit, revise the user prompts in MD files to
mandate starting with \'No, there is no need\...\' or \'Yes, please
improve\...\', followed by detailed justifications and
Markdown-formatted suggestions (e.g., use \`\`\`java for code snippets).
Modify the output JSON schemas to include \'decision\' (exact phrase),
\'justifications\' (array of strings), and \'markdown_comment\'
(Markdown string). Add a new example rule for full-file summary that
uses the queue system for batched processing. Ensure selectors and
actions reference diff hunks in payloads for context. Provide updated
YAML files (e.g., java_method_quality.yaml with
resultFormat=JSON_WITH_DECISION) and prompt MD files, plus app-side
parsing code snippets (e.g., in Java using JsonNode, extending the
existing parseStrictJson).\"

4\. \*\*Refined Prompt 6\*\*:

\"Expand the testing section of this code reviewer specification to
cover the new message queue integration and structured prompts. Include
unit tests for enqueuing/dequeuing (e.g., with embedded RabbitMQ),
integration tests for LLM parsing (e.g., mock responses with \'Yes/No\'
decisions and Markdown, verify JSON extraction using the parseStrictJson
code), and E2E tests simulating a PR with delayed LLM calls (e.g.,
consumer resumes and posts markdown_comment). Add scenarios for edge
cases like invalid prompt outputs (e.g., missing decision---trigger
retry) or queue failures (e.g., dead-letter handling). Recommend tools
like JUnit, Mockito for mocks, and Spring Boot Test for queues. Provide
sample test code snippets and acceptance criteria, such as \'System
posts Markdown comment to GitHub after queue processing, with
justification if \'Yes\' decision\'.\"

5\. \*\*Refined Prompt 7\*\*:

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption,
followed by sync LLM calls and GitHub updates. Restructure all prompts
to start with \'No, there is no need to modify the code; it is already
at a good level.\' or \'Yes, please improve the quality of the code.\',
include justifications, and output in Markdown for direct posting,
building on the updated MD files and parsing pseudo-code. Resolve any
gaps from previous audits (e.g., diff edge cases, AST parse errors). Add
rationales for choices (e.g., RabbitMQ over Kafka for simplicity), code
examples (e.g., \@RabbitListener for consumers), and updated modules
(e.g., add \'queue\' module). Output the entire revised spec with these
improvements for implementation readiness.\"

Tab 12

Enhance the review orchestration, posting, and queue sections of this
GitHub code reviewer spec to integrate message queues with the new
structured LLM prompts and outputs. For each ReviewTask, enqueue a
message with serialized context (e.g., JSON payload including file, diff
hunk, AST node, rendered prompt with Yes/No instructions). Consumers
dequeue, invoke the LLM synchronously, parse the structured JSON
(validating \'decision\' phrase, justifications, and markdown_comment),
then post the markdown_comment to GitHub (e.g., inline comment tied to
diff hunk lines). Add flow for resuming interrupted tasks from queue or
DB state. Include edge cases like queue overflow (rate-limit enqueuing),
LLM timeouts (requeue with priority), and GitHub failures (store pending
comments in DB for retry). Update the persistence schema to track queue
message IDs and task states (ENQUEUED, PROCESSING, COMPLETED). Provide
pseudo-code for a QueueService class (using Spring\'s RabbitTemplate)
and revised end-to-end flow steps 9-12, aligning with the latest parsing
logic

Tab 13

the horizon alpha model said this:

Below is the enhanced spec content focusing on review orchestration,
posting, and queue integration, aligned with the structured LLM
prompts/outputs and robust parsing. It adds message payload design,
consumer behavior, resume logic, edge cases, persistence updates, and
concrete pseudo-code for a QueueService using Spring's RabbitTemplate.
It also revises end-to-end steps 9--12 to reflect this flow.

Message model and enqueueing

\- Each ReviewTask is serialized into a stateless JSON payload
sufficient for an independent worker to:

\- Reconstruct context (repo, PR, file, diff, AST node metadata).

\- Invoke the LLM with the correct system/user prompts (Yes/No decision
requirement).

\- Parse structured JSON output (decision, justifications,
markdown_comment).

\- Post an inline or summary GitHub comment.

\- TaskEnvelope fields (JSON):

\- ids: runId, taskId, queueMessageId (set by broker on publish if
available), createdAt, attemptCount

\- repo: owner, name, prNumber, prHeadSha (optional)

\- file: path, changeType (ADDED\|MODIFIED), language=\"JAVA\"

\- diff: hunkHeader(s), addedLines, removedLines, line mapping;
targetLine for inline comment if known

\- astNode: nodeType, nodeSignature, startLine, endLine,
modificationStatus

\- rule: ruleId, actionScope

\- llm: profileId, tokenBudget

\- prompts:

\- systemPromptRef

\- rulePromptRef

\- renderedUserPrompt (already rendered with Yes/No instruction and
schema)

\- postingOptions: inlineAllowed (boolean), summaryAllowed (boolean),
checksApiAllowed (boolean)

\- retry: priority (NORMAL\|HIGH), nextBackoffMs

\- Size control: prefer keeping code slices minimal (method body,
javadoc, diff) over entire file to limit message size.

Persistence schema updates

\- review_tasks

\- id (UUID)

\- run_id

\- rule_id

\- file_path

\- node_type

\- node_signature

\- status: PENDING \| ENQUEUED \| PROCESSING \| COMPLETED \| FAILED \|
CANCELED \| FORMAT_ERROR

\- attempt_count (int)

\- last_error (text)

\- queue_message_id (string, nullable)

\- priority: NORMAL \| HIGH

\- posting_status: NONE \| PENDING \| POSTED \| FAILED

\- inline_comment_ref (string, nullable) // GitHub comment id or
check-run annotation id

\- payload_hash (string) // for idempotency

\- created_at, updated_at

\- pending_comments

\- id

\- task_id

\- repo_owner, repo_name, pr_number

\- file_path, line_number, side (LEFT/RIGHT), body_markdown

\- status: PENDING \| POSTED \| FAILED

\- last_error, created_at, updated_at

\- llm_calls

\- id, task_id, provider, model, request_chars/tokens,
response_chars/tokens, latency_ms, raw_response (truncated), created_at

Queue integration and orchestration flow

\- Producer side:

\- ReviewOrchestrator discovers entities via AST+diff and produces
ReviewTasks with status=PENDING.

\- For each ReviewTask, prepare TaskEnvelope (render prompts, include
diff mapping and node metadata).

\- Enqueue via QueueService.publish(envelope), update task
status=ENQUEUED, store queue_message_id if available.

\- Consumer side:

\- Workers listen to the tasks queue. On message:

\- Load ReviewTask by taskId.

\- If status COMPLETED/CANCELED, ack and drop (idempotency).

\- Mark status=PROCESSING; increment attempt_count.

\- Invoke LLM synchronously with system + rendered user prompt.

\- Parse JSON; validate decision phrase and presence of
markdown_comment.

\- Create pending comment record(s) and try to post to GitHub. If inline
mapping failed, fall back to file-level or PR summary.

\- On success: mark posting_status=POSTED and task COMPLETED.

\- On transient failure: requeue with backoff and possibly set
priority=HIGH.

\- On persistent format parsing failure: mark FORMAT_ERROR and persist
raw output; enqueue a minimal fallback comment or mark for human review.

Updated end-to-end flow steps 9--12

9\) Task assembly, rendering, and enqueue

\- For each interested rule-node:

\- Create ReviewTask in DB, status=PENDING.

\- Render prompts:

\- Load system prompt (includes Yes/No start, Markdown-only, JSON
schema, anti-hallucination).

\- Render rule prompt with variables (filePath, methodSignature,
javadocText, diffSnippet, methodBody).

\- Compose final user prompt (includes schema with decision,
justifications, markdown_comment).

\- Build TaskEnvelope with all needed context and renderedUserPrompt.

\- QueueService.publish(envelope):

\- Publish to RabbitMQ durable queue with PERSISTENT delivery.

\- If queue reports capacity/overflow (e.g., resource-limited), respect
backpressure:

\- Apply rate-limit: schedule retry publish with exponential backoff.

\- Optionally degrade by batching fewer tasks per second.

\- Persist queue_message_id (if available) and set ReviewTask:
status=ENQUEUED.

10\) Dequeue and LLM invocation

\- Consumer receives message and attempts to acquire task lock:

\- If task not found or already COMPLETED/CANCELED, ack and return.

\- Update status=PROCESSING, attempt_count++.

\- LLM call:

\- Resolve LLM profile; construct request with systemPrompt +
renderedUserPrompt.

\- Set client-side timeout (e.g., 30--60s).

\- If timeout:

\- Nack and requeue with priority=HIGH and increased backoff.

\- Update last_error and attempt_count in DB.

\- If success, proceed to parse.

11\) Parsing, decision validation, and posting

\- Parse JSON:

\- Validate decision equals exactly one of:

\- "No, there is no need to modify the code; it is already at a good
level."

\- "Yes, please improve the quality of the code."

\- Extract justifications array and markdown_comment.

\- Map issues/javadoc fields to ReviewFinding entities and persist.

\- Posting:

\- Construct inline comment:

\- Use envelope file path and diff context to compute the appropriate PR
line (right/after side). If a precise target line is not resolvable,
post a file-level comment referencing the method signature and line
range.

\- Call GitHubService.postInlineComment or postGeneralComment; if Checks
API is enabled, optionally create a Check Run with annotations.

\- If GitHub returns 4xx/5xx:

\- Store comment in pending_comments with status=PENDING and schedule
retry.

\- For rate limiting (403 with rate-limit headers), backoff according to
headers, requeue a "post" task or rely on scheduled retry.

\- On success:

\- Update pending_comments to POSTED, set task posting_status=POSTED.

\- Set task status=COMPLETED; ack message.

12\) Error handling, retries, and resumption

\- Transient LLM errors (timeouts, 5xx):

\- Requeue message with exponential backoff (use delayed exchange or
TTL+DLX).

\- priority=HIGH for subsequent attempts.

\- Parsing failures (malformed JSON, wrong decision phrase):

\- Attempt up to 2 retries with clarification prompts (append to
renderedUserPrompt).

\- If still invalid: mark task FORMAT_ERROR; persist raw output; create
a fallback pending comment stating formatting failure.

\- GitHub failures:

\- Store pending comment with status=PENDING and plan retry via
scheduled job or separate queue.

\- Resume interrupted tasks:

\- If worker crashes mid-task:

\- Message is redelivered; the consumer checks DB status.

\- If status=PROCESSING but no recent heartbeat and message redelivery
count \> 0, allow processing (idempotent) and proceed; posting methods
should attempt to deduplicate (e.g., search for existing identical
comment and skip).

\- Admin API can set task status from FAILED to PENDING and requeue by
taskId.

\- Queue overflow:

\- QueueService.applyBackpressure(): if broker reports resource
shortage, delay publishing and throttle per-config maxEnqueueRate.

\- Orchestrator can batch and schedule enqueue waves.

Queue and posting edge cases

\- Inline mapping unavailable:

\- Post a file-level comment with markdown_comment and include a note
referencing nodeSignature and line range.

\- Very large markdown_comment:

\- Truncate politely with a "...truncated due to size limits" note and
attach a link to full report if stored elsewhere.

\- Duplicate comments:

\- GitHubService should attempt idempotency: before posting, search
recent comments on the PR for the same taskId signature; if found and
identical, skip.

Queue configuration (RabbitMQ)

\- Durable queues and exchanges:

\- Queue: code-review.tasks (durable)

\- DLQ: code-review.tasks.dlq

\- Optional delayed exchange or TTL+DLX for exponential backoff.

\- Consumer concurrency:

\- Configurable per instance; prefetch to control in-flight tasks.

\- Retry policy:

\- maxAttempts configurable (e.g., 5), with backoff initialBackoffMs,
multiplier, maxBackoffMs.

\- Priority:

\- Use message priority if enabled (e.g., 0--9). Timeouts re-enqueued
with higher priority.

Configuration YAML (augmented)

mq:

type: rabbitmq

rabbitmq:

host: \${RABBITMQ_HOST:localhost}

port: \${RABBITMQ_PORT:5672}

username: \${RABBITMQ_USER:guest}

password: \${RABBITMQ_PASS:guest}

queues:

tasks: code-review.tasks

dlq: code-review.tasks.dlq

concurrency:

consumersPerQueue: 5

prefetch: 5

retry:

enabled: true

maxAttempts: 5

initialBackoffMs: 2000

multiplier: 2.0

maxBackoffMs: 60000

priority:

enabled: true

maxPriority: 9

backpressure:

maxEnqueueRatePerSec: 50

overflowPolicy: THROTTLE \# or REJECT

QueueService pseudo-code (RabbitTemplate)

class QueueService {

\@Autowired private RabbitTemplate rabbitTemplate;

\@Autowired private TaskRepository taskRepo;

\@Value(\"\${mq.rabbitmq.queues.tasks}\") private String tasksQueue;

\@Value(\"\${mq.backpressure.maxEnqueueRatePerSec:0}\") private int
maxRate;

private final RateLimiter limiter = (maxRate \> 0) ?
RateLimiter.create(maxRate) : null;

public void publish(TaskEnvelope env) {

if (limiter != null) limiter.acquire(); // throttle if enabled

ReviewTask task = taskRepo.findById(env.getTaskId())

.orElseThrow(() -\> new IllegalStateException(\"Task not found\"));

MessageProperties props = new MessageProperties();

props.setContentType(\"application/json\");

props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

props.setPriority(priorityToInt(env.getRetry().getPriority())); // if
enabled

props.setHeader(\"runId\", env.getRunId());

props.setHeader(\"ruleId\", env.getRule().getRuleId());

props.setHeader(\"attempt\", task.getAttemptCount());

byte\[\] body = objectMapper.writeValueAsBytes(env);

Message message = new Message(body, props);

rabbitTemplate.send(\"\", tasksQueue, message);

// Optionally read message id via post-processor or correlation (depends
on broker/plugins)

task.setStatus(Status.ENQUEUED);

task.setQueueMessageId(UUID.randomUUID().toString()); // placeholder if
broker id not available

taskRepo.save(task);

}

public void requeueWithBackoff(TaskEnvelope env, int attempt, long
initialBackoffMs, double multiplier, long maxBackoffMs, boolean
highPriority) {

long delay = Math.min(maxBackoffMs, (long)(initialBackoffMs \*
Math.pow(multiplier, Math.max(0, attempt - 1))));

env.getRetry().setNextBackoffMs(delay);

if (highPriority) env.getRetry().setPriority(Priority.HIGH);

// Use delayed exchange or publish to a delay queue with TTL

// Implementation depends on RabbitMQ setup; conceptually:

rabbitTemplate.convertAndSend(\"delayed-exchange\",
\"code-review.tasks\", env, m -\> {

m.getMessageProperties().setDelay((int) delay);

return m;

});

}

private int priorityToInt(Priority p) { return p == Priority.HIGH ? 8 :
4; }

}

Consumer pseudo-code (with parsing and posting)

\@RabbitListener(queues = \"\${mq.rabbitmq.queues.tasks}\",

concurrency = \"\${mq.rabbitmq.concurrency.consumersPerQueue}\")

public void onMessage(Message msg, Channel channel) {

long tag = msg.getMessageProperties().getDeliveryTag();

TaskEnvelope env = null;

try {

env = objectMapper.readValue(msg.getBody(), TaskEnvelope.class);

ReviewTask task = taskRepo.findById(env.getTaskId()).orElseThrow();

if (task.getStatus().isTerminal()) {

channel.basicAck(tag, false);

return;

}

taskRepo.markProcessing(task.getId());

LlmClient client = llmFactory.fromProfile(env.getLlm().getProfileId());

String systemPrompt =
promptLoader.loadSystem(env.getPrompts().getSystemPromptRef());

String userPrompt = env.getPrompts().getRenderedUserPrompt();

LLMResponse resp = client.generate(LLMRequest.of(systemPrompt,
userPrompt).timeoutSeconds(60));

llmCallsRepo.save(from(resp, task.getId()));

ParsedResponse parsed = parser.parse(resp.getText()); // validates
decision + markdown_comment

// Build posting target (inline if possible)

Optional\<InlineTarget\> target = diffLocator.locate(env.getFile(),
env.getDiff(), env.getAstNode());

String body = parsed.getMarkdownComment();

if (target.isPresent() && env.getPostingOptions().isInlineAllowed()) {

String commentId = gitHubService.postInlineComment(env.getRepo(),
env.getFile().getPath(), env.getRepo().getPrNumber(), target.get(),
body);

taskRepo.markPosted(task.getId(), commentId);

} else if (env.getPostingOptions().isSummaryAllowed()) {

String commentId = gitHubService.postGeneralComment(env.getRepo(),
env.getRepo().getPrNumber(), body);

taskRepo.markPosted(task.getId(), commentId);

} else {

// Store for later posting

pendingCommentsRepo.save(PendingComment.from(env, body));

taskRepo.markCompleted(task.getId());

}

findingsRepo.saveAll(mapFindings(parsed, env, task.getId()));

channel.basicAck(tag, false);

} catch (LlmTimeoutException te) {

// Requeue with higher priority/backoff

queueService.requeueWithBackoff(env, incrementAttempt(task),
cfg.initialBackoffMs, cfg.multiplier, cfg.maxBackoffMs, true);

taskRepo.updateLastError(task.getId(), \"LLM timeout\");

channel.basicAck(tag, false); // ack original; re-enqueued separately
via delayed exchange

} catch (ParseException pe) {

if (task.getAttemptCount() \< cfg.maxParseRetries) {

// Append clarification and re-enqueue

TaskEnvelope clarified = appendClarification(env, pe.getMessage());

queueService.requeueWithBackoff(clarified, incrementAttempt(task),
cfg.initialBackoffMs, cfg.multiplier, cfg.maxBackoffMs, false);

channel.basicAck(tag, false);

} else {

taskRepo.markFormatError(task.getId(), pe.getMessage());

// Optional: store a fallback pending comment

pendingCommentsRepo.save(PendingComment.from(env, \"Automated review
formatting failed. Raw output stored for manual inspection.\"));

channel.basicAck(tag, false);

}

} catch (GitHubRateLimitException rl) {

// Persist pending comment for retry, schedule later according to rate
limit reset

pendingCommentsRepo.save(PendingComment.from(env,
lastBodyOrPlaceholder(env)));

scheduleCommentRetry(rl.getResetEpochSeconds());

taskRepo.updateLastError(task.getId(), \"GitHub rate limit\");

channel.basicAck(tag, false);

} catch (TransientException tx) {

queueService.requeueWithBackoff(env, incrementAttempt(task),
cfg.initialBackoffMs, cfg.multiplier, cfg.maxBackoffMs, false);

taskRepo.updateLastError(task.getId(), \"Transient: \" +
tx.getMessage());

channel.basicAck(tag, false);

} catch (Exception ex) {

taskRepo.markFailed(task.getId(), ex.getMessage());

channel.basicAck(tag, false);

}

}

Resumption strategy

\- Message redelivery:

\- If broker redelivers, worker checks ReviewTask.status:

\- If PROCESSING with stale updated_at (e.g., \> N minutes) or
attempt_count increased, proceed; operations are idempotent.

\- DB-based resume:

\- Admin API endpoints:

\- POST /api/reviews/tasks/{taskId}/requeue → sets status=PENDING and
republish envelope.

\- POST /api/reviews/{runId}/requeue-failed → requeue all
FAILED/FORMAT_ERROR tasks.

\- Pending comments retry:

\- Scheduled job periodically attempts to post pending_comments.

\- On success, mark POSTED and update ReviewTask posting_status if
needed.

GitHub posting specifics

\- Inline comments:

\- Use GitHub PR review comment API with file path and position on the
"right" (after) side mapped from diff.

\- If patch positions are ambiguous, fallback to file-level comment with
explicit lines (e.g., "lines 120--180").

\- Deduplication:

\- Optionally include taskId in a hidden HTML comment in
markdown_comment to avoid duplicates on re-post.

Edge cases and safeguards

\- Queue overflow/backpressure:

\- Rate-limit publish operations based on configured
maxEnqueueRatePerSec.

\- If broker rejects publish (channel closed or resource shortage),
apply exponential backoff and retry publish; if persistent failure,
leave task in PENDING for scheduled retry.

\- LLM timeouts:

\- Re-enqueue with HIGH priority and longer backoff; log and increment
attempt_count.

\- Provider/model JSON drift:

\- Parser supports strict JSON first; if it fails and provider is known
to drift, retry with clarification emphasizing exact JSON-only output
and decision phrase.

\- Large outputs:

\- Truncate markdown_comment to a safe length (e.g., 50k chars), persist
full text in DB if needed, note truncation in the posted comment.

Alignment with structured prompts

\- System and rule prompts already enforce:

\- Yes/No decision phrase.

\- Justifications as bullets.

\- markdown_comment for GitHub-ready Markdown.

\- JSON wrapper for parseability.

\- Parser validates decision phrase and required fields; on
non-compliance, retries with clarification.

With these updates, the orchestration cleanly marries the queue-driven
execution with the new structured prompt/response contract, provides
resumability through DB state and idempotent posting, and handles
critical edge cases (overflow, timeouts, GitHub errors) with backoff and
persistence-backed retries.

Tab 14

grok4 feedback:

\### Feedback on the Outcome for Refined Prompt 3 (Combining Queue with
LLM and GitHub Updates)

This outcome is a robust enhancement, effectively merging the queue
system with structured prompts, parsing, and posting. It adds practical
details like TaskEnvelope fields (including diff for inline comments),
persistence updates (e.g., pending_comments table), and edge case
handling (e.g., timeouts, overflows), while revising flow steps 9-12 for
clarity. Pseudo-code for QueueService and consumer is comprehensive,
Spring-aligned (e.g., RateLimiter for backpressure, \@RabbitListener),
and integrates the Yes/No/Markdown parsing seamlessly. This makes the
spec more resilient and implementation-friendly, with good focus on
idempotency and resumption.

\*\*Strengths\*\*:

\- \*\*Integration Depth\*\*: Excellently ties queues to prompts (e.g.,
renderedUserPrompt in envelope, validation in consumer parse). Posting
logic handles inline/fallback well, using diff hunk for line mapping.

\- \*\*Resilience Features\*\*: Strong on edges like timeouts (requeue
with HIGH priority), overflows (throttling via RateLimiter), GitHub
failures (pending_comments for retries), and resumption (DB checks +
admin API).

\- \*\*Persistence Updates\*\*: New schema fields (e.g.,
queue_message_id, posting_status) enable tracking; pending_comments
table is a smart addition for async retries.

\- \*\*Pseudo-Code Quality\*\*: Detailed and actionable (e.g.,
requeueWithBackoff, onMessage with exception branches); handles
clarification retries for parsing failures.

\- \*\*Alignment and Completeness\*\*: Revises steps 9-12 logically;
config YAML additions (e.g., backpressure) are practical. Keeps
token/size awareness.

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Envelope Size/Rendering\*\*: Rendering userPrompt at enqueue is
good, but if variables change (unlikely), it\'s static---clarify no
dynamic reloads. Large envelopes (e.g., big diff hunks) need compression
notes (e.g., gzip body).

\- \*\*Inline Mapping Details\*\*: Mentions diffLocator.locate but no
pseudo-code or how it computes positions (e.g., from hunk headers + node
ranges). Edge: if diff is renamed, how to map?

\- \*\*Retry Granularity\*\*: Parse retries are in-consumer (up to 2),
but queue requeues on transients---specify if parse failures count
toward maxAttempts to avoid loops.

\- \*\*Testing/Observability Gaps\*\*: No updates to testing (e.g., mock
requeues for timeouts); add metrics for backpressure events or
pending_comments depth.

\- \*\*Edge Cases Expansion\*\*: Covers main ones, but misses e.g.,
broker downtime (auto-reconnect via Spring AMQP) or very high-priority
floods overwhelming queues.

\- \*\*Minor Inconsistencies\*\*: Consumer acks on requeue (basicAck
after re-enqueue)---but for redelivery, it should nack if not
processing. YAML has overflowPolicy=THROTTLE but pseudo-code uses
RateLimiter---align terms.

\- \*\*Readability\*\*: Dense pseudo-code; add comments for key lines.
No ASCII diagram, though not requested.

This outcome is \~90% solid---highly actionable, with minor tweaks for
polish.

\### Relevance of Remaining Prompts (4-7) Based on Current Spec

All remaining prompts (4-7) are still relevant, as the spec now has
queue-prompt integration but lacks full resilience (e.g., consumer
scaling), rule YAML updates, comprehensive testing, and a holistic
consolidation. These would complete the picture without redundancy.
Here\'s why each fits, with minor refinements to reference the latest
(e.g., TaskEnvelope, pending_comments).

\- \*\*Prompt 4 (Overall Resilience and Error Handling)\*\*: Highly
relevant. Current spec has good edges but not e.g., queue timeouts or
horizontal scaling details. It can build on new pseudo-code for broader
non-functionals.

\- \*\*Prompt 5 (Rule-Specific Updates and Examples)\*\*: Relevant.
Prompts are structured, but YAML (e.g., resultFormat) isn\'t updated,
and no new rule example. This adds that layer.

\- \*\*Prompt 6 (Testing and Validation)\*\*: Very relevant. Spec
mentions testing minimally; this expands to cover queues + new
parsing/posting.

\- \*\*Prompt 7 (Holistic Refinement)\*\*: Most relevant now---tie
everything together, resolve lingering gaps (e.g., AST errors from
earlier audits).

\### Would I Iterate More on This Specification?

Yes, definitely iterate more. The spec is evolving well (now with strong
queue-LLM-posting flow), but it\'s still sectional---prompts 4-7 will
unify resilience, rules, testing, and the full doc, making it truly
ready for coding. Proceed sequentially: 4 for resilience, 5 for rules, 6
for testing, then 7 for final polish.

\### Comprehensive Feedback Prompts to Send to the LLM for Further
Improvement

Refined prompts below, updated for the latest outcome (e.g., referencing
TaskEnvelope, pending_comments, diffLocator). Paste the current outcome
into the LLM query.

1\. \*\*Refined Prompt 4\*\*:

\"Refine the entire specification for this LLM-powered code reviewer,
incorporating message queues for parallel LLM requests and the new
structured prompts with \'Yes/No\' decisions, justifications, and
Markdown outputs. Add resilience features: For queues, handle consumer
failures by requeuing with context restoration (building on
requeueWithBackoff); for prompts, extend the existing retry logic if
output doesn\'t match the required format (e.g., invalid decision phrase
in parse). Ensure GitHub posts use markdown_comment directly for
well-formatted comments, with fallbacks via pending_comments. Address
issues like high-latency LLMs (timeout queues after 5min, aligning with
LLM call timeout), or parsing failures (fallback to raw text posting
with warning). Update non-functional requirements for scalability
(horizontal scaling of consumers) and by testing (mock queues with
Testcontainers). Provide a revised architecture diagram in text form
(ASCII art) showing queue integration, and add config options like queue
names and consumer counts in application.yml, extending the mq YAML.\"

2\. \*\*Refined Prompt 5\*\*:

\"Update the rule types, examples, and YAML configs in this spec to
align with the new prompt structuring. For rules like Java Single-Method
Quality Review and Javadoc Audit, revise the user prompts in MD files to
mandate starting with \'No, there is no need\...\' or \'Yes, please
improve\...\', followed by detailed justifications and
Markdown-formatted suggestions (e.g., use \`\`\`java for code snippets).
Modify the output JSON schemas to include \'decision\' (exact phrase),
\'justifications\' (array of strings), and \'markdown_comment\'
(Markdown string). Add a new example rule for full-file summary that
uses the queue system for batched processing. Ensure selectors and
actions reference diff hunks in payloads for context, integrating with
TaskEnvelope. Provide updated YAML files (e.g., java_method_quality.yaml
with resultFormat=JSON_WITH_DECISION) and prompt MD files, plus app-side
parsing code snippets (e.g., in Java using JsonNode, extending the
parseStrictJson from earlier).\"

3\. \*\*Refined Prompt 6\*\*:

\"Expand the testing section of this code reviewer specification to
cover the new message queue integration and structured prompts. Include
unit tests for enqueuing/dequeuing (e.g., with embedded RabbitMQ,
testing publish and requeueWithBackoff), integration tests for LLM
parsing (e.g., mock responses with \'Yes/No\' decisions and Markdown,
verify JSON extraction using the consumer parse logic), and E2E tests
simulating a PR with delayed LLM calls (e.g., consumer resumes via
redelivery, posts markdown_comment, handles timeouts). Add scenarios for
edge cases like invalid prompt outputs (e.g., missing decision---trigger
retry), queue failures (e.g., dead-letter handling), or GitHub rate
limits (retry via pending_comments). Recommend tools like JUnit, Mockito
for mocks, and Spring Boot Test for queues. Provide sample test code
snippets and acceptance criteria, such as \'System posts Markdown
comment to GitHub after queue processing, with justification if \'Yes\'
decision\'.\"

4\. \*\*Refined Prompt 7\*\*:

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption
as in TaskEnvelope, followed by sync LLM calls and GitHub updates via
pending_comments. Restructure all prompts to start with \'No, there is
no need to modify the code; it is already at a good level.\' or \'Yes,
please improve the quality of the code.\', include justifications, and
output in Markdown for direct posting, building on the updated MD files
and parsing pseudo-code. Resolve any gaps from previous audits (e.g.,
diff edge cases like renames in hunk mapping, AST parse errors). Add
rationales for choices (e.g., RabbitMQ over Kafka for simplicity), code
examples (e.g., \@RabbitListener for consumers with onMessage), and
updated modules (e.g., add \'queue\' module). Output the entire revised
spec with these improvements for implementation readiness.\"

Tab 15

alpha horizon gave this spec

Here is the refined, end-to-end specification for the LLM-powered GitHub
code reviewer, integrating message queues for parallelization and the
structured prompt/output protocol with Yes/No decisions, justifications,
and Markdown outputs. It adds resilience for queue consumers and prompt
parsing, robust posting to GitHub, handling of high-latency LLMs, and
comprehensive non-functional and testing updates. It also includes an
ASCII architecture diagram and expanded configuration.

1\) Purpose and scope

\- Automatically review GitHub PRs using LLMs, focusing initially on
Java (methods and Javadoc).

\- Use AST + diff intersection to trigger rule-based reviews on modified
entities.

\- Parallelize LLM requests with a message queue and resilient worker
consumers.

\- Enforce structured LLM outputs: decision phrase, justifications,
Markdown body, and JSON wrapper.

\- Post results to GitHub as inline or summary comments; persist
findings and raw outputs.

2\) High-level architecture

\- Spring Boot (Java 17+).

\- Modules:

\- api: REST endpoints for triggers and webhooks.

\- github: hub4j/github-api primary client; Spring WebClient fallback
(Checks API, advanced).

\- parser: JavaParser-based AST parsing and diff intersection.

\- rules: Rule configuration/registry, triggers/selectors/actions.

\- prompts: System and rule prompt rendering with context variables.

\- llm: LangChain4j client abstraction and profiles.

\- review: Orchestrator building ReviewRuns and ReviewTasks.

\- mq: Queue publisher/consumer, backoff and resumption.

\- posting: GitHub posting and pending comment manager.

\- persistence: JPA entities for runs, tasks, findings, LLM calls,
pending comments.

\- config: Externalized settings for GitHub, LLM, MQ, and review
behavior.

3\) ASCII architecture diagram

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| Client UI \| \| GitHub Webhooks \|

\| (Trigger via API) \| \| (PR opened/Sync) \|

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\--+

\| \|

v v

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| API Controller \|\<\-\-\-\-\-\-\-\-\--\>\| GitHubService \|

\| (trigger/webhooks) \| \| (hub4j + WebClient) \|

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\|

v

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+ AST/Diff
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| ReviewOrchestrator\|\-\-\-\-\-\-\-\-\-\-\-\-\-\--\>\| Parser
(JavaParser) \|

\| (build run/tasks) \| \| + Diff Intersection \|

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\|

\| tasks (rendered prompts + context)

v

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+ publish
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| QueueService \|\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--\>\| MQ Broker
\|

\| (Rabbit/Kafka) \|\<\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--\| (tasks +
DLQ) \|

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+ backpressure
+\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\--+

\^ \|

\| requeueWithBackoff \| deliver

\| v

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| PendingCommentJob \| \| Worker Consumers \|

\| (retry GitHub post)\| \| (LLM call + parse + post) \|

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\^ \|

\| DB \| GitHub comments

\| v

+\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| Persistence \|\<\-\-\-\-\-\-\-\-\-\-\-\-\-\--\>\| GitHubService \|

\| (runs/tasks/finds) \| \| (inline/summary/Checks) \|

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

4\) Review flow overview

\- User selects PR to review.

\- App connects to GitHub and LLM provider.

\- Retrieves changed files; ignores non-Java and deleted files.

\- Parses diffs and Java files; builds AST node tree.

\- Intersects diff lines with node ranges; marks node status
(NEW/MODIFIED/UNCHANGED).

\- Visitor traverses nodes; rules signal interest by type and
modification status.

\- For each interested node/rule, create ReviewTask with rendered prompt
payload and enqueue.

5\) Prompt protocol and enforcement

\- System prompt:

\- Requires starting with exact decision phrase:

\- "No, there is no need to modify the code; it is already at a good
level."

\- "Yes, please improve the quality of the code."

\- Immediately follow with bullet list justifications.

\- Entire body must be Markdown-formatted and wrapped in JSON according
to rule schema.

\- Anti-hallucination: only comment on provided scope; if insufficient
context, state it in justifications.

\- Rule prompts:

\- Java method quality and Javadoc audit prompts include schema with
fields:

\- decision (exact phrase), justifications\[\], markdown_comment,
summary.

\- For method quality: issues\[\] with category, severity, title,
description, code_refs, suggested_fix.

\- For Javadoc audit: typos_corrections\[\], tag_issues\[\],
suggested_javadoc.

\- Parsing:

\- Validate JSON structure, exact decision phrase, and presence of
markdown_comment; if invalid, retry with clarification up to 2 times.

6\) Message queue integration and resilience

Message model (TaskEnvelope)

\- ids: runId, taskId, createdAt, attemptCount, optional queueMessageId.

\- repo: owner, name, prNumber, headSha (optional).

\- file: path, changeType, language.

\- diff: hunks, added/removed lines, mapping, best target line for
inline posting.

\- astNode: nodeType, nodeSignature, startLine, endLine,
modificationStatus.

\- rule: ruleId, actionScope.

\- llm: profileId, tokenBudget.

\- prompts: systemPromptRef, rulePromptRef, renderedUserPrompt.

\- postingOptions: inlineAllowed, summaryAllowed, checksApiAllowed.

\- retry: priority (NORMAL/HIGH), nextBackoffMs.

\- Size-conscious payloads with necessary code slices only.

Producer orchestration

\- ReviewOrchestrator:

\- Builds ReviewRun, discovers interested nodes, creates ReviewTask
records (PENDING).

\- Renders prompts; serializes TaskEnvelope.

\- QueueService.publish(envelope):

\- Throttles publishes if backpressure configured.

\- Sets task status=ENQUEUED, stores queueMessageId if available.

Consumer execution

\- Worker:

\- Dequeues envelope; loads ReviewTask. If task already
COMPLETED/CANCELED, ack and return.

\- Marks task PROCESSING; increments attemptCount.

\- Invokes LLM synchronously with system + renderedUserPrompt.

\- If timeout (call timeout aligned with LLM profile), requeue with HIGH
priority and backoff.

\- Parses response JSON; validates decision and markdown_comment.

\- Persists findings and LLM call metrics.

\- Posts markdown_comment to GitHub (inline or summary). On failure,
stores pending comment for retry.

\- On success: mark task COMPLETED and comment POSTED; ack message.

Consumer failures and resumption

\- If consumer crashes mid-task:

\- Broker redelivers; new consumer checks DB state:

\- If status=PROCESSING but stale updated_at or no posted comment,
proceed (idempotent operations).

\- Posting uses deduplication to avoid duplicate comments.

\- Requeue with context:

\- requeueWithBackoff(envelope, attempt) publishes the same envelope
with delay and priority, preserving context for stateless resumption.

Queue edge cases

\- Overflow/backpressure: QueueService rate-limits enqueuing; if broker
rejects, backoff and retry. Leave tasks in PENDING or ENQUEUED for later
attempts.

\- Dead-lettering: Messages exceeding retry thresholds or non-retryable
errors are routed to DLQ; admin tools can inspect and requeue after
fixing causes.

7\) Posting to GitHub and fallback

\- Inline posting:

\- Use diff mapping to compute right-side PR line for inline comments.

\- If mapping ambiguous or missing, fallback to file-level or PR summary
comment including nodeSignature and lines.

\- Use markdown_comment directly as the comment body.

\- Pending comments:

\- If GitHub posting fails (rate limits, transient 5xx), persist
pending_comments with body_markdown and schedule retries via background
job.

\- Checks API (optional):

\- Create/update check runs with annotations; fallback to comments if
Checks API fails.

8\) Error handling and retries

\- LLM timeouts:

\- LLM call timeout set per profile; if exceeds, treat as transient. For
very high latency, cap at 5 minutes end-to-end and requeue with HIGH
priority and increased backoff.

\- Parsing failures:

\- Retry up to 2 times with clarification emphasizing exact JSON,
decision phrase, and Markdown. If still invalid:

\- Mark FORMAT_ERROR; store raw output; optionally post minimal fallback
comment indicating formatting issue and include a truncated raw snippet.

\- GitHub failures:

\- Rate limit: respect reset headers, reschedule pending comment
posting.

\- Transient 5xx: requeue posting attempts via pending_comments job.

\- Non-retryable errors:

\- Schema/prompt mismatch: mark FAILED and send to DLQ.

\- Partial run recovery:

\- Run can complete with COMPLETED_WITH_ERRORS if some tasks failed;
provide report and requeue APIs.

9\) Updated persistence schema

review_runs

\- id, repo_owner, repo_name, pr_number, status
(RUNNING\|COMPLETED\|FAILED\|COMPLETED_WITH_ERRORS), created_at,
updated_at

review_tasks

\- id, run_id, rule_id, file_path, node_type, node_signature

\- status:
PENDING\|ENQUEUED\|PROCESSING\|COMPLETED\|FAILED\|FORMAT_ERROR\|CANCELED

\- posting_status: NONE\|PENDING\|POSTED\|FAILED

\- attempt_count, last_error

\- queue_message_id (nullable), priority

\- payload_hash (idempotency)

\- created_at, updated_at

review_findings

\- id, task_id, rule_id, entity_signature, file_path, line_start,
line_end

\- decision, markdown_comment, summary

\- issues_json or javadoc_json (stored structured)

\- severity_aggregate (optional)

\- created_at

pending_comments

\- id, task_id, repo_owner, repo_name, pr_number, file_path,
line_number, side, body_markdown

\- status: PENDING\|POSTED\|FAILED

\- last_error, created_at, updated_at

llm_calls

\- id, task_id, provider, model, request_chars/tokens,
response_chars/tokens, latency_ms, raw_response (truncated), created_at

10\) Revised end-to-end steps 9--12

9\) Task assembly and enqueue

\- Build ReviewTask (PENDING).

\- Render prompts with Yes/No schema and JSON wrapper.

\- Serialize TaskEnvelope including prompts and diff/node context.

\- QueueService.publish(envelope) with persistent delivery; set
task=ENQUEUED.

10\) Dequeue and LLM invocation

\- Worker acquires message; marks task=PROCESSING, attempt++.

\- LLM call with system + renderedUserPrompt; timeout per profile.

\- On timeout or 5xx: requeueWithBackoff(envelope, attempt,
priority=HIGH).

11\) Parse and post

\- Parse JSON, validate decision phrase and markdown_comment.

\- Persist findings and tokens/latency.

\- Post markdown_comment inline (preferred) or file-level/summary; on
failure, create pending_comments record.

12\) Completion or retry

\- On success: mark task COMPLETED, posting POSTED; ack.

\- On parse failure: retry up to 2 with clarification; otherwise
FORMAT_ERROR and fallback posting or pending comment.

\- On GitHub failure: store pending comment; schedule posting retries;
task may be COMPLETED with posting_status=PENDING or FAILED depending on
policy.

11\) Pseudo-code highlights

QueueService (RabbitTemplate)

class QueueService {

void publish(TaskEnvelope env) {

throttleIfConfigured();

Message m = buildPersistentMessage(env);

rabbitTemplate.send(\"\", cfg.tasksQueue, m);

taskRepo.updateStatusAndMsgId(env.getTaskId(), ENQUEUED,
extractMsgIdOrGenerated(m));

}

void requeueWithBackoff(TaskEnvelope env, int attempt, boolean
highPriority) {

long delay = computeDelay(attempt);

rabbitTemplate.convertAndSend(cfg.delayedExchange, cfg.routingKey, env,
msg -\> {

msg.getMessageProperties().setDelay((int) delay);

if (cfg.priorityEnabled)
msg.getMessageProperties().setPriority(highPriority ? 8 : 4);

return msg;

});

}

}

Worker consumer

\@RabbitListener(queues = \"\${mq.rabbitmq.queues.tasks}\",

concurrency = \"\${mq.rabbitmq.concurrency.consumersPerQueue}\")

public void onMessage(Message msg) {

TaskEnvelope env = deserialize(msg);

ReviewTask task = taskRepo.find(env.getTaskId());

if (task.isTerminal()) return ack();

taskRepo.markProcessing(task.getId());

try {

LLMResponse resp = llmClient.generate(systemPrompt(env),
userPrompt(env), timeout(cfg));

Parsed parsed = parser.parse(resp.getText()); // validates decision +
markdown_comment

findingsRepo.saveAll(map(parsed, env, task));

String commentId = postingService.post(env,
parsed.getMarkdownComment());

taskRepo.markCompleted(task.getId(), commentId);

ack();

} catch (LlmTimeoutException te) {

queueService.requeueWithBackoff(env, task.incrementAttempt(), true);

taskRepo.updateLastError(task.getId(), \"LLM timeout\");

ack();

} catch (ParseException pe) {

if (task.getAttemptCount() \< cfg.maxParseRetries) {

queueService.requeueWithBackoff(appendClarification(env, pe),
task.incrementAttempt(), false);

ack();

} else {

taskRepo.markFormatError(task.getId(), pe.getMessage());

postingService.postFallbackOrPending(env, \"Automated review formatting
failed. Raw output stored.\");

ack();

}

} catch (GitHubException ge) {

pendingCommentsRepo.save(PendingComment.from(env,
lastBodyOrPlaceholder(env)));

taskRepo.updatePostingStatus(task.getId(), PENDING);

ack();

} catch (Exception ex) {

queueService.requeueWithBackoff(env, task.incrementAttempt(), false);

taskRepo.updateLastError(task.getId(), ex.getMessage());

ack();

}

}

PostingService behavior

\- Try inline comment; if mapping unavailable, fallback to file-level or
summary.

\- Deduplicate by embedding hidden marker \<!\-- taskId:\... \--\> in
markdown_comment.

\- If posting fails, create pending_comments and let PendingCommentJob
retry.

12\) Configuration (application.yml)

github:

appId: \${GITHUB_APP_ID:}

privateKey: \${GITHUB_APP_PRIVATE_KEY:}

installationId: \${GITHUB_APP_INSTALLATION_ID:}

token: \${GITHUB_TOKEN:}

baseUrl: \${GITHUB_BASE_URL:https://api.github.com}

webhookSecret: \${GITHUB_WEBHOOK_SECRET:}

llm:

profiles:

openai-default:

provider: OPENAI

model: gpt-4o-mini

temperature: 0.2

maxTokens: 2000

timeoutSeconds: 60

systemPromptRef: prompts/system/system_prompt.md

ollama-local:

provider: OLLAMA

model: llama3:instruct

temperature: 0.1

timeoutSeconds: 60

review:

defaultMode: MODIFIED_JAVA_METHODS

postToGitHub: true

tokenBudgetPerRun: 300000

maxParseRetries: 2

maxEndToEndTaskTimeoutSeconds: 300

mq:

type: rabbitmq

rabbitmq:

host: \${RABBITMQ_HOST:localhost}

port: \${RABBITMQ_PORT:5672}

username: \${RABBITMQ_USER:guest}

password: \${RABBITMQ_PASS:guest}

queues:

tasks: code-review.tasks

dlq: code-review.tasks.dlq

delayedExchange: code-review.delayed

routingKey: code-review.tasks

concurrency:

consumersPerQueue: 5

prefetch: 5

retry:

enabled: true

maxAttempts: 5

initialBackoffMs: 2000

multiplier: 2.0

maxBackoffMs: 60000

priority:

enabled: true

maxPriority: 9

backpressure:

maxEnqueueRatePerSec: 50

overflowPolicy: THROTTLE

posting:

checksApiEnabled: false

maxCommentLength: 50000

dedupeWindowMinutes: 30

13\) Resilience summary

\- Queue:

\- Durable queues, DLQ, delayed backoff, message priority.

\- Requeue with full context preserved in envelope.

\- Idempotency by task status and deduped GitHub comments.

\- Prompts/Parsing:

\- Strict structure enforced; retry with clarification if invalid (up to
2).

\- On persistent failure, fallback posting with warning and raw snippet.

\- LLM latency:

\- Per-call timeout; overall task cap at 5 minutes; requeue with HIGH
priority if exceeded.

\- Posting:

\- Use markdown_comment directly; persist pending comments on failure
and retry via job.

14\) Non-functional requirements

\- Scalability:

\- Horizontal scaling by increasing worker instances and queue
partitions (Kafka) or consumer concurrency (RabbitMQ).

\- Rate-limited publishing and provider quotas respected.

\- Reliability:

\- Durable queues, at-least-once processing with idempotency, DLQ
inspection tooling.

\- Observability:

\- Metrics: tasks.enqueued/completed/failed, retries, dlq.count, queue
depth, consumer lag.

\- LLM: latency, tokens, cost estimates.

\- Posting: comment.post.success/fail, pending queue size.

\- Structured logs with taskId/runId correlation.

\- Security:

\- Webhook signature verification.

\- Secrets via env/vault; redact in logs.

\- Minimize code content in messages.

15\) Testing strategy

\- Unit:

\- Diff-to-line mapping, visitor triggers, prompt rendering variable
injection.

\- Parser: valid Yes/No decisions, schema validation, retry-on-parse
failure.

\- Integration:

\- Mock LLM and GitHub with Testcontainers for RabbitMQ/Kafka.

\- Simulate consumer crash and redelivery resumption.

\- Simulate GitHub rate limit and pending comment retries.

\- E2E:

\- Full PR run using local Ollama or mocked LLM; assert posted comments
contain decision phrase and Markdown.

16\) Rule examples

Java method quality rule

\- Trigger: on METHOD, ADDED/MODIFIED, onlyIfModifiedInDiff=true.

\- Action: send method signature/body/javadoc/diff; expect structured
JSON with decision, justifications, markdown_comment, issues\[\].

\- Post markdown_comment inline on method line.

Javadoc audit rule

\- Trigger: on JAVADOC, ADDED/MODIFIED, onlyIfModifiedInDiff=true.

\- Action: send Javadoc text + signature + params/return; expect
structured JSON with decision, justifications, markdown_comment,
typos/tag_issues/suggested_javadoc.

\- Post markdown_comment inline or file-level as appropriate.

This refined specification integrates queue-driven parallelization with
strict, GitHub-ready prompt/output structure, robust resumption and
retry strategies, and practical operational guidance to achieve
reliability and scale.

Tab 16

feedback grok4

\### Feedback on the Outcome for Refined Prompt 4 (Overall Resilience
and Error Handling)

This outcome is an excellent holistic refinement, pulling together the
full spec into a cohesive, end-to-end document with integrated queues,
structured prompts, resilience features (e.g., timeouts, requeues,
pending_comments), and updates like the ASCII diagram, config
expansions, and testing strategy. It resolves many prior gaps (e.g.,
high-latency handling with 5-min caps, parse retries), while maintaining
modularity and practicality. The pseudo-code is refined and consistent,
and non-functionals (scalability, observability) are well-covered. This
version feels much closer to implementation-ready, with clear flow and
rationales implied in structure.

\*\*Strengths\*\*:

\- \*\*Cohesiveness\*\*: Provides a complete spec from purpose to
testing, with the ASCII diagram adding visual clarity. Integrates
prompts/queues/posting seamlessly (e.g., markdown_comment direct
posting, decision validation in parse).

\- \*\*Resilience Depth\*\*: Comprehensive handling of failures (e.g.,
consumer crashes via redelivery + idempotency, LLM timeouts with
priority escalation, parse retries with clarification). Pending_comments
and DLQ are robust for retries/DLQ.

\- \*\*Non-Functionals\*\*: Strong additions like metrics (e.g., queue
depth), security (redaction), and scalability (horizontal workers).
Config YAML is detailed and env-friendly.

\- \*\*Flow and Schema\*\*: Revised steps 9-12 are precise; persistence
schema supports resumption well (e.g., posting_status).

\- \*\*Alignment\*\*: Builds on prior outputs (e.g., TaskEnvelope from
previous, Yes/No enforcement); includes rule examples for context.

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Diagram Clarity\*\*: ASCII art is good but simplistic---could
label flows more (e.g., \"requeue\" arrows). No mention of tools like
PlantUML for better visuals.

\- \*\*Latency/Timeout Details\*\*: 5-min cap is added, but no
enforcement mechanism (e.g., overall task watchdog timer in consumer).
High-latency requeues could loop if LLM is always slow---add max
attempts per task.

\- \*\*Parsing/Retry Integration\*\*: Retries for parse are mentioned,
but pseudo-code in consumer shows requeue on ParseException without the
2-retry limit detail---align with earlier pseudo-code.

\- \*\*Edge Cases\*\*: Covers main ones, but misses e.g., queue broker
outage (use Spring AMQP auto-reconnect) or very large PRs exhausting
tokenBudgetPerRun (add run-level throttling).

\- \*\*Testing Depth\*\*: Strategy is outlined but high-level---no
sample code snippets or tools like Testcontainers for RabbitMQ mocks, as
requested in some prior prompts.

\- \*\*Minor Inconsistencies\*\*: Spec says \"overall task cap at 5
minutes\" but config has maxEndToEndTaskTimeoutSeconds=300 (5
min)---consistent, but clarify if this kills the consumer thread. Rule
examples reference schemas but don\'t show updated YAML.

\- \*\*Readability/Completeness\*\*: Lengthy but structured; could add
section numbers for rules YAML/prompts. No explicit rationales (e.g.,
why RabbitMQ: \"simpler for task queues vs Kafka\'s streaming\").

This outcome is \~95% polished---very strong, with only minor
operational tweaks needed for production.

\### Relevance of Remaining Prompts (5-7) Based on Current Spec

The remaining prompts (5-7) are still relevant but less critical now, as
the spec is nearly complete (e.g., prompts are structured, queues
integrated, resilience added). However, they address lingering specifics
like YAML updates, detailed testing code, and final gap resolution
(e.g., diff edges). Use them to fine-tune rather than overhaul.

\- \*\*Prompt 5 (Rule-Specific Updates and Examples)\*\*: Relevant. The
spec has rule examples but not updated YAML (e.g., resultFormat) or a
new full-file rule. This would add that, plus parsing snippets---useful
for completeness.

\- \*\*Prompt 6 (Testing and Validation)\*\*: Highly relevant. Testing
is summarized but lacks snippets, tools (e.g., Testcontainers), and
scenarios (e.g., retry on invalid outputs)---this expands it fully.

\- \*\*Prompt 7 (Holistic Refinement)\*\*: Still relevant as a capstone
to resolve any remaining audits (e.g., AST parse errors, rationales like
RabbitMQ vs Kafka) and output the absolute final spec.

\### Would I Iterate More on This Specification?

Yes, but minimally--- the spec is now comprehensive and ready for
prototyping. Iterate with 5 and 6 for details on rules/testing, then 7
to polish and consolidate. This would seal it without over-engineering.

\### Comprehensive Feedback Prompts to Send to the LLM for Further
Improvement

Refined prompts below, tailored to the latest outcome (e.g., referencing
ASCII diagram, pending_comments, 5-min cap). Paste the current outcome
into the LLM query.

1\. \*\*Refined Prompt 5\*\*:

\"Update the rule types, examples, and YAML configs in this spec to
align with the new prompt structuring. For rules like Java Single-Method
Quality Review and Javadoc Audit, revise the user prompts in MD files to
mandate starting with \'No, there is no need\...\' or \'Yes, please
improve\...\', followed by detailed justifications and
Markdown-formatted suggestions (e.g., use \`\`\`java for code snippets).
Modify the output JSON schemas to include \'decision\' (exact phrase),
\'justifications\' (array of strings), and \'markdown_comment\'
(Markdown string). Add a new example rule for full-file summary that
uses the queue system for batched processing. Ensure selectors and
actions reference diff hunks in payloads for context, integrating with
TaskEnvelope. Provide updated YAML files (e.g., java_method_quality.yaml
with resultFormat=JSON_WITH_DECISION) and prompt MD files, plus app-side
parsing code snippets (e.g., in Java using JsonNode, extending the
existing parse logic in consumer pseudo-code).\"

2\. \*\*Refined Prompt 6\*\*:

\"Expand the testing section of this code reviewer specification to
cover the new message queue integration and structured prompts. Include
unit tests for enqueuing/dequeuing (e.g., with embedded RabbitMQ,
testing publish and requeueWithBackoff), integration tests for LLM
parsing (e.g., mock responses with \'Yes/No\' decisions and Markdown,
verify JSON extraction and validation in consumer), and E2E tests
simulating a PR with delayed LLM calls (e.g., consumer resumes via
redelivery, handles 5-min timeouts, posts markdown_comment, retries
pending_comments). Add scenarios for edge cases like invalid prompt
outputs (e.g., missing decision---trigger retry with clarification),
queue failures (e.g., dead-letter handling), or GitHub rate limits
(retry via PendingCommentJob). Recommend tools like JUnit, Mockito for
mocks, and Spring Boot Test/Testcontainers for queues. Provide sample
test code snippets and acceptance criteria, such as \'System posts
Markdown comment to GitHub after queue processing, with justification if
\'Yes\' decision\'.\"

3\. \*\*Refined Prompt 7\*\*:

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption
as in TaskEnvelope, followed by sync LLM calls and GitHub updates via
pending_comments. Restructure all prompts to start with \'No, there is
no need to modify the code; it is already at a good level.\' or \'Yes,
please improve the quality of the code.\', include justifications, and
output in Markdown for direct posting, building on the updated schemas
and parsing. Resolve any gaps from previous audits (e.g., diff edge
cases like renames in hunk mapping for inline posts, AST parse errors
with fallback skipping). Add rationales for choices (e.g., RabbitMQ over
Kafka for simplicity in task queues), code examples (e.g.,
\@RabbitListener for consumers with onMessage pseudo-code), and updated
modules (e.g., add \'posting\' module details). Improve the ASCII
diagram with more flow labels if needed. Output the entire revised spec
with these improvements for implementation readiness.\"

Tab 17

improved spec by alpha horizon

Great suggestion. Below is a concise, end-to-end refinement focusing on
rules, YAML configs, prompt files, TaskEnvelope context alignment, and
parsing---now fully consistent with the structured prompt contract
(decision, justifications, markdown_comment) and queue-based execution.

Updated rule types and examples

1\) Java Single-Method Quality Review

\- Purpose: Focused review on a modified method (clarity,
maintainability, correctness hints, low-effort performance, and tests).

\- Trigger: onLanguage=JAVA, onEntityTypes=METHOD,
onlyIfModifiedInDiff=true, fileChangeTypes=\[ADDED, MODIFIED\].

\- Context payload: method signature, annotations, Javadoc, method body,
diff snippet/hunks mapped to after-side lines, and minimal class
context.

\- Output: JSON with decision, justifications, markdown_comment, and
issues\[\].

2\) Java Javadoc Audit

\- Purpose: Audit and fix typos/grammar; check \@param/@return
consistency; propose a revised Javadoc.

\- Trigger: onLanguage=JAVA, onEntityTypes=JAVADOC,
onlyIfModifiedInDiff=true, fileChangeTypes=\[ADDED, MODIFIED\].

\- Context payload: Javadoc text, element signature, params, return
type, and diff snippet for the Javadoc section.

\- Output: JSON with decision, justifications, markdown_comment,
typos_corrections\[\], tag_issues\[\], suggested_javadoc.

3\) Full-File Summary (new example rule)

\- Purpose: Provide a concise, GitHub-ready summary for the entire
modified file (e.g., risks, hotspots, noteworthy changes), batched by
queue like all other tasks.

\- Trigger: onLanguage=JAVA, onEntityTypes=FILE,
onlyIfModifiedInDiff=true, fileChangeTypes=\[ADDED, MODIFIED\].

\- Context payload: file path, list of diff hunks, top-level
declarations, and short excerpts of modified methods/classes
(size-capped).

\- Output: JSON with decision, justifications, markdown_comment, and
optional sections like changed_entities\[\].

Updated prompt MD files

resources/prompts/system/system_prompt.md

You are a pragmatic senior Java engineer performing focused code
reviews.

Priorities:

1\) Correctness and clarity in the reviewed scope only.

2\) Maintainability and readability.

3\) Idiomatic Java and safe practices.

4\) Lightweight performance when relevant.

Strict output structure:

\- Start with EXACTLY one of:

\- \"No, there is no need to modify the code; it is already at a good
level.\"

\- \"Yes, please improve the quality of the code.\"

\- Immediately follow with justifications as bullet points.

\- Use Markdown for the entire body: bold for titles, - bullets, and
fenced code blocks \`\`\`java for code.

\- Wrap everything in a single JSON object per the user prompt schema.

\- If context is insufficient, explicitly say "insufficient context" in
justifications.

\- Do not add any text outside the JSON. Do not hallucinate beyond
provided code/diff.

\- Do not require null checks unless the contract/annotations demand
them.

\- Prefer small, focused methods; suggest extractions if too long or
doing multiple things.

resources/prompts/rules/java_method_quality.md

Task: Review a modified Java method changed in this PR and provide
actionable feedback.

Context:

\- File: {{filePath}}

\- Class: {{className}}

\- Method: {{methodSignature}}

\- Annotations: {{methodAnnotations}}

\- Javadoc (if any):

{{methodJavadoc}}

Diff snippet (for this method):

{{diffSnippet}}

Method implementation:

\`\`\`java

{{methodBody}}

\`\`\`

Rules to apply briefly:

1\) Keep methods small and focused; consider extractions if doing
multiple things.

2\) No null-check asks unless annotations/contract require it.

3\) Prefer clarity: naming, simple control flow, early returns when
useful.

4\) Suggest tests for critical paths or edge cases.

5\) Only flag low-effort performance issues if obvious.

Output format requirements:

\- Start with EXACT decision phrase ("No..." or "Yes..."), then bullet
justifications.

\- Entire body must be valid Markdown and wrapped in JSON below.

\- Use \`\`\`java for code suggestions; bold titles; bullet lists for
issues.

\- No text outside the JSON object.

Output JSON schema:

{

\"decision\": \"No, there is no need to modify the code; it is already
at a good level. \| Yes, please improve the quality of the code.\",

\"justifications\": \[\"bullet point 1\", \"bullet point 2\"\],

\"markdown_comment\": \"Full Markdown response ready for GitHub comment,
including the exact decision phrase, justifications, and any code
blocks.\",

\"summary\": \"Short overview\",

\"issues\": \[

{

\"category\":
\"maintainability\|readability\|correctness\|performance\|testing\",

\"severity\": \"LOW\|MEDIUM\|HIGH\",

\"title\": \"short title\",

\"description\": \"what and why\",

\"code_refs\": \[{\"line\": 123, \"hint\": \"optional\"}\],

\"suggested_fix\": \"concrete suggestion or \`\`\`java snippet\`\`\`\"

}

\]

}

resources/prompts/rules/javadoc_audit.md

Task: Audit this Javadoc for typos/grammar/clarity and \@param/@return
consistency. Provide a revised Javadoc if needed.

Context:

\- File: {{filePath}}

\- Element: {{elementType}} {{elementSignature}}

\- Parameters: {{parameters}}

\- Return type: {{returnType}}

Javadoc:

\"\"\"

{{javadocText}}

\"\"\"

Output format requirements:

\- Start with EXACT decision phrase ("No..." or "Yes..."), then bullet
justifications.

\- Entire body must be valid Markdown and wrapped in JSON below.

\- No text outside the JSON object.

Output JSON schema:

{

\"decision\": \"No, there is no need to modify the code; it is already
at a good level. \| Yes, please improve the quality of the code.\",

\"justifications\": \[\"bullet point 1\", \"bullet point 2\"\],

\"markdown_comment\": \"Full Markdown response ready for GitHub
comment.\",

\"summary\": \"Short overview\",

\"typos_corrections\": \[

{\"original\": \"strng\", \"suggestion\": \"string\", \"reason\":
\"spelling\|grammar\|clarity\"}

\],

\"tag_issues\": \[

{\"type\": \"PARAM_MISMATCH\|RETURN_MISMATCH\|MISSING_TAG\|EXTRA_TAG\",
\"detail\": \"string\"}

\],

\"suggested_javadoc\": \"/\*\* Revised Javadoc here\... \*/\"

}

resources/prompts/rules/full_file_summary.md (new)

Task: Provide a concise summary of the key changes and risks in this
modified Java file. Focus on modified areas.

Context:

\- File: {{filePath}}

\- Modified hunks (unified diff headers):

{{hunkHeaders}}

\- High-level declarations changed:

{{changedTopLevelEntities}}

\- Excerpts from notable modified methods/classes (truncated for
brevity):

\`\`\`java

{{notableExcerpts}}

\`\`\`

Output format requirements:

\- Start with EXACT decision phrase ("No..." or "Yes...") depending on
whether improvements are warranted.

\- Provide bullet justifications summarizing risk, testing needs, or
design concerns.

\- Use Markdown for the full comment. Wrap in JSON.

Output JSON schema:

{

\"decision\": \"No, there is no need to modify the code; it is already
at a good level. \| Yes, please improve the quality of the code.\",

\"justifications\": \[\"bullet point 1\", \"bullet point 2\"\],

\"markdown_comment\": \"Full Markdown response suitable as a PR summary
comment.\",

\"summary\": \"Short overview\",

\"changed_entities\": \[

{\"type\": \"CLASS\|METHOD\|FIELD\", \"name\":
\"QualifiedNameOrSignature\", \"note\": \"optional\"}

\]

}

Updated YAML rule configs

resources/rules/java_method_quality.yaml

id: java-method-quality

name: Quality review for modified Java methods

trigger:

onLanguage: JAVA

onEntityTypes: \[METHOD\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: METHOD_SIGNATURE_ANNOTATIONS_JAVADOC_AND_BODY

promptRef: \"prompts/rules/java_method_quality.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true \# adds diff hunks/snippet to payload
(TaskEnvelope.diff)

includeAstMetadata: true

priority: 10

resources/rules/javadoc_audit.yaml

id: javadoc-audit

name: Javadoc typo and grammar audit

trigger:

onLanguage: JAVA

onEntityTypes: \[JAVADOC\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: JAVADOC_WITH_SIGNATURE_AND_PARAMS

promptRef: \"prompts/rules/javadoc_audit.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true

includeAstMetadata: true

priority: 5

resources/rules/full_file_summary.yaml (new)

id: full-file-summary

name: Full-file summary for modified Java files

trigger:

onLanguage: JAVA

onEntityTypes: \[FILE\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: FILE_WITH_DIFF_HUNKS_AND_OVERVIEW

promptRef: \"prompts/rules/full_file_summary.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true

includeAstMetadata: false

priority: 3

Selectors and actions alignment with TaskEnvelope

\- includeDiffHunks=true ensures the orchestrator adds:

\- diff.hunkHeaders, modified line ranges, and line mapping data to
TaskEnvelope.diff.

\- A formatted {{diffSnippet}} or {{hunkHeaders}} variable in the
rendered prompt.

\- includeAstMetadata=true ensures:

\- astNode metadata (nodeType, nodeSignature, startLine, endLine,
modificationStatus) is serialized into TaskEnvelope.astNode and
available to the consumer for inline comment mapping and deduplication.

Queue TaskEnvelope fields (relevant excerpt)

{

\"runId\": \"\...\",

\"taskId\": \"\...\",

\"repo\": {\"owner\": \"\...\", \"name\": \"\...\", \"prNumber\": 123},

\"file\": {\"path\": \"src/\.../Foo.java\", \"changeType\":
\"MODIFIED\", \"language\": \"JAVA\"},

\"diff\": {

\"hunks\": \[\"@@ -120,10 +120,12 @@\"\],

\"addedLines\": \[130,131,132\],

\"removedLines\": \[121,122\],

\"lineMapping\": \[{\"before\": 125, \"after\": 133}\],

\"preferredAfterLine\": 133

},

\"astNode\": {\"nodeType\": \"METHOD\", \"nodeSignature\":
\"com.x.Foo#bar()\", \"startLine\": 120, \"endLine\": 180,
\"modificationStatus\": \"MODIFIED\"},

\"rule\": {\"ruleId\": \"java-method-quality\", \"actionScope\":
\"METHOD_SIGNATURE_ANNOTATIONS_JAVADOC_AND_BODY\"},

\"llm\": {\"profileId\": \"openai-default\", \"tokenBudget\": 2000},

\"prompts\": {

\"systemPromptRef\": \"prompts/system/system_prompt.md\",

\"rulePromptRef\": \"prompts/rules/java_method_quality.md\",

\"renderedUserPrompt\": \"Task: \... Output JSON schema: {\...}\"

},

\"postingOptions\": {\"inlineAllowed\": true, \"summaryAllowed\": true,
\"checksApiAllowed\": false},

\"retry\": {\"priority\": \"NORMAL\", \"nextBackoffMs\": 0}

}

App-side parsing code snippet (Java)

Decision phrases

private static final String YES = \"Yes, please improve the quality of
the code.\";

private static final String NO = \"No, there is no need to modify the
code; it is already at a good level.\";

Parsing

public ParsedReview parseResponse(String text) {

try {

JsonNode root = mapper.readTree(text.trim());

String decision = requireText(root, \"decision\");

if (!YES.equals(decision) && !NO.equals(decision)) {

throw new ParseException(\"Invalid decision phrase: \" + decision);

}

ArrayNode just = requireArray(root, \"justifications\");

String markdown = requireText(root, \"markdown_comment\");

// Optional rule-specific parsing

List\<Issue\> issues = root.has(\"issues\")

? mapper.convertValue(root.get(\"issues\"), new
TypeReference\<List\<Issue\>\>() {})

: List.of();

return new ParsedReview(decision, toList(just), markdown, issues, root);

} catch (Exception e) {

throw new ParseException(\"Unable to parse structured JSON\", e);

}

}

Helper methods

private String requireText(JsonNode root, String field) {

JsonNode n = root.get(field);

if (n == null \|\| n.asText().isBlank()) throw new
ParseException(\"Missing/blank field: \" + field);

return n.asText();

}

private ArrayNode requireArray(JsonNode root, String field) {

JsonNode n = root.get(field);

if (n == null \|\| !n.isArray()) throw new
ParseException(\"Missing/invalid array: \" + field);

return (ArrayNode) n;

}

private List\<String\> toList(ArrayNode arr) {

List\<String\> out = new ArrayList\<\>();

arr.forEach(x -\> out.add(x.asText()));

return out;

}

Consumer integration (excerpt)

ParsedReview parsed = parser.parseResponse(resp.getText());

String body = parsed.getMarkdown(); // post markdown_comment directly

InlineTarget target = diffLocator.locate(env.getFile(), env.getDiff(),
env.getAstNode()).orElse(null);

if (target != null && env.getPostingOptions().isInlineAllowed()) {

githubService.postInlineComment(repo, prNumber, env.getFile().getPath(),
target, body);

} else {

githubService.postGeneralComment(repo, prNumber, body);

}

If parsing fails (invalid decision or schema):

\- Retry with clarification up to maxParseRetries.

\- On final failure, persist FORMAT_ERROR and either:

\- Post a fallback minimal comment: "Automated review formatting failed;
raw content (truncated) below." plus a quoted raw snippet, or

\- Store in pending_comments for later/manual handling.

Config updates (YAML highlights)

\- Add resultFormat=JSON_WITH_DECISION to rules.

\- Ensure mq.rabbitmq.\* properties already present; rules rely on
orchestrator to respect includeDiffHunks/includeAstMetadata when
building TaskEnvelope.

\- Ensure llm.profiles.\* include timeoutSeconds aligning with queue
requeue thresholds.

Testing notes

\- Add unit tests verifying:

\- Rule YAMLs load and map to includeDiffHunks/includeAstMetadata.

\- Prompt rendering includes decision instruction and JSON schema.

\- Parser accepts only exact decision phrases and extracts
markdown_comment.

\- Integration with Testcontainers:

\- RabbitMQ container for producer/consumer flow.

\- Mock LLM returning valid/invalid JSON to exercise retry paths.

\- Mock GitHub posting errors to validate pending_comments pipeline.

This update aligns all rules, prompts, and YAML configs with the
structured output contract, ensures TaskEnvelope includes diff hunk
context for inline posting, adds the new full-file summary rule, and
provides parsing code integrated with the consumer flow.

Tab 18

feedback from grok4

\### Feedback on the Outcome for Refined Prompt 5 (Rule-Specific Updates
and Examples)

This outcome is a focused and effective update, successfully aligning
the rules, prompts, and YAML with the structured output (Yes/No,
justifications, markdown_comment). It introduces the new full-file
summary rule logically, enhances YAML with flags like includeDiffHunks
for TaskEnvelope integration, and provides practical parsing snippets
using JsonNode. The MD files are revised consistently with
anti-hallucination and Markdown mandates, and the consumer excerpt shows
good posting flow. This strengthens the spec\'s modularity and ensures
diff context supports inline comments, improving overall coherence.

\*\*Strengths\*\*:

\- \*\*Alignment and Completeness\*\*: Rules are updated with the
JSON_WITH_DECISION format; new rule fits queue batching well.
includeDiffHunks/includeAstMetadata ties directly to envelope for
context-aware payloads.

\- \*\*Prompt Refinements\*\*: MD files are detailed yet concise, with
schema examples and \"No text outside JSON\" to prevent issues.
Variables like {{hunkHeaders}} for new rule are thoughtful.

\- \*\*Parsing Code\*\*: JsonNode-based snippet is
Spring/Jackson-friendly, with validation for exact phrases and helpers
like requireText---integrates smoothly with consumer pseudo-code.

\- \*\*TaskEnvelope Excerpt\*\*: Useful illustration of how diff/AST
data flows to queues/posting.

\- \*\*Testing/Config Notes\*\*: Brief but relevant additions (e.g.,
unit tests for rendering, YAML loading).

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Schema Notation\*\*: Uses \"\|\" in \"decision\" schema, which
isn\'t valid JSON---clarify as example strings. Justifications array
expects plain \"bullet point 1\" but prompts say \"bullet
points\"---specify if array items include \"- \" or if Markdown is
assembled post-parse.

\- \*\*New Rule Details\*\*: Full-file summary is good, but lacks
selector specifics (e.g., restrict to certain paths) or how to truncate
{{notableExcerpts}} for token limits.

\- \*\*Sample Outputs\*\*: No example full JSON response (e.g., with
decision and markdown_comment populated)---would help illustrate.

\- \*\*Edge Cases\*\*: Doesn\'t address if justifications are empty
(allow for \"No\" but require one positive?); or how parsing handles
escaped Markdown in array.

\- \*\*Minor Inconsistencies\*\*: YAML priority values differ
(10,5,3)---explain ordering. Testing notes mention Testcontainers but no
snippets.

\- \*\*Readability\*\*: Dense YAML/MD; could bold key changes.

This outcome is \~90% strong---adds key details without bloat, but
samples and clarifications would perfect it.

\### Relevance of Remaining Prompts (6-7) Based on Current Spec

Both remaining prompts (6-7) are still relevant to finalize the spec.
With rules now aligned, 6 expands testing to cover these updates (e.g.,
YAML loading, new rule parsing), and 7 consolidates everything,
resolving any leftover gaps like AST errors.

\- \*\*Prompt 6 (Testing and Validation)\*\*: Highly relevant. Spec has
testing notes but needs detailed snippets, tools, and scenarios (e.g.,
mock invalid JSON for retry, full-file rule E2E).

\- \*\*Prompt 7 (Holistic Refinement)\*\*: Essential as the
closer---unify the spec, add rationales (e.g., RabbitMQ choice), and
ensure all audits (e.g., diff renames) are addressed.

\### Would I Iterate More on This Specification?

Yes, iterate with the last two prompts to wrap up. The spec is robust,
but 6 adds concrete testing for confidence, and 7 ensures a polished,
single-document final version ready for development.

\### Comprehensive Feedback Prompts to Send to the LLM for Further
Improvement

Refined prompts below, updated for the latest outcome (e.g., referencing
full_file_summary.yaml, JsonNode parsing). Paste the current outcome
into the LLM query.

1\. \*\*Refined Prompt 6\*\*:

\"Expand the testing section of this code reviewer specification to
cover the new message queue integration and structured prompts. Include
unit tests for enqueuing/dequeuing (e.g., with embedded RabbitMQ,
testing publish and requeueWithBackoff), integration tests for LLM
parsing (e.g., mock responses with \'Yes/No\' decisions and Markdown,
verify JSON extraction using the JsonNode-based parseResponse code,
handling invalid phrases), and E2E tests simulating a PR with delayed
LLM calls (e.g., consumer resumes via redelivery, handles 5-min
timeouts, posts markdown_comment, retries pending_comments). Add
scenarios for edge cases like invalid prompt outputs (e.g., missing
decision---trigger retry with clarification), queue failures (e.g.,
dead-letter handling), or GitHub rate limits (retry via
PendingCommentJob). Test new rules like full-file summary (e.g., verify
hunkHeaders in rendered prompt). Recommend tools like JUnit, Mockito for
mocks, and Spring Boot Test/Testcontainers for queues. Provide sample
test code snippets and acceptance criteria, such as \'System posts
Markdown comment to GitHub after queue processing, with justification if
\'Yes\' decision\'.\"

2\. \*\*Refined Prompt 7\*\*:

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption
as in TaskEnvelope, followed by sync LLM calls and GitHub updates via
pending_comments. Restructure all prompts to start with \'No, there is
no need to modify the code; it is already at a good level.\' or \'Yes,
please improve the quality of the code.\', include justifications, and
output in Markdown for direct posting, building on the updated MD files
(e.g., full_file_summary.md) and JsonNode parsing. Resolve any gaps from
previous audits (e.g., diff edge cases like renames in hunk mapping for
inline posts, AST parse errors with fallback skipping). Add rationales
for choices (e.g., RabbitMQ over Kafka for simplicity in task queues),
code examples (e.g., \@RabbitListener for consumers with onMessage
pseudo-code), and updated modules (e.g., add \'rules\' module details).
Improve the ASCII diagram with more flow labels if needed. Output the
entire revised spec with these improvements for implementation
readiness.\"

Tab 19

output of alpha horizon

Below is the expanded testing section, tailored to the queue-backed
orchestration and the structured prompt/output contract. It includes
unit, integration, and end-to-end coverage, edge cases, recommended
tools, sample code snippets, and clear acceptance criteria. It also
explicitly tests the new full-file summary rule and diff hunk
propagation.

Testing strategy overview

\- Goals:

\- Validate reliable enqueue/dequeue with backoff and resumption.

\- Enforce structured prompt outputs (decision, justifications,
markdown_comment) and robust parsing.

\- Ensure GitHub posting uses markdown_comment directly, with
pending_comments fallback.

\- Verify resilience for timeouts, DLQ handling, and rate limits.

\- Confirm rules correctly render prompts with diff hunks and AST
metadata.

Tooling

\- Unit tests: JUnit 5, AssertJ, Mockito.

\- Spring Boot integration tests: spring-boot-starter-test, Spring AMQP
Test (for Rabbit), Spring Kafka Test (if using Kafka).

\- Testcontainers:

\- RabbitMQ or Kafka containers for real broker semantics.

\- Postgres container for persistence.

\- GitHub mocking: WireMock or Mockito-based GitHubService stub.

\- LLM mocking: Mockito or WireMock to simulate LangChain4j provider
responses.

Test coverage matrix

1\) Unit tests

A\) QueueService publishing and backoff

\- Purpose: Ensure tasks are published with persistent delivery; rate
limiting and backoff work; requeueWithBackoff sets delay and priority
correctly.

\- Scenarios:

\- publish: builds persistent message with headers (runId, ruleId,
attempt) and stores status=ENQUEUED.

\- backpressure throttle: when limiter is configured, enforce max rate.

\- requeueWithBackoff: computes delay by attempt and sets message
priority (HIGH on timeouts).

\- Snippet:

\@Test

void publish_setsPersistentDeliveryAndEnqueuedStatus() {

TaskEnvelope env = Fixtures.envelope();

when(taskRepo.findById(env.getTaskId())).thenReturn(Optional.of(Fixtures.taskPending()));

queueService.publish(env);

verify(rabbitTemplate).send(eq(\"\"), eq(\"code-review.tasks\"),
any(Message.class));

verify(taskRepo).updateStatusAndMsgId(eq(env.getTaskId()),
eq(Status.ENQUEUED), anyString());

}

\@Test

void requeueWithBackoff_setsDelayAndPriority() {

TaskEnvelope env = Fixtures.envelope();

queueService.requeueWithBackoff(env, 3, true);

verify(rabbitTemplate).convertAndSend(eq(\"code-review.delayed\"),
eq(\"code-review.tasks\"), eq(env), any());

}

B\) Parser (JsonNode-based parseResponse)

\- Purpose: Enforce exact decision phrase, presence of justifications
and markdown_comment, map issues/javadoc fields.

\- Scenarios:

\- Valid "Yes" decision with Markdown and issues array.

\- Valid "No" decision with empty issues.

\- Invalid decision phrase → ParseException.

\- Missing markdown_comment → ParseException.

\- Snippet:

\@Test

void parse_yesDecision_extractsMarkdownAndIssues() {

String json = \"\"\"

{\"decision\":\"Yes, please improve the quality of the code.\",

\"justifications\":\[\"Refactor long method.\"\],

\"markdown_comment\":\"Yes, please improve\...\\\\n-
Refactor\...\\\\n\`\`\`java\\\\n\...\\\\n\`\`\`\",

\"issues\":\[{\"category\":\"maintainability\",\"severity\":\"MEDIUM\",\"title\":\"Long
method\",\"description\":\"\...\"}\]}

\"\"\";

ParsedReview pr = parser.parseResponse(json);

assertThat(pr.getDecision()).isEqualTo(Decision.YES);

assertThat(pr.getMarkdown()).contains(\"\`\`\`java\");

assertThat(pr.getIssues()).hasSize(1);

}

\@Test

void parse_invalidDecision_throws() {

String json = \"\"\"

{\"decision\":\"Maybe?\",

\"justifications\":\[\"\...\"\],

\"markdown_comment\":\"\...\"}

\"\"\";

assertThatThrownBy(() -\> parser.parseResponse(json))

.isInstanceOf(ParseException.class);

}

C\) Prompt rendering (Rule templates)

\- Purpose: Ensure rendered prompts include decision instruction and
JSON schema; include diff hunks if includeDiffHunks=true.

\- Scenarios:

\- java_method_quality: rendered prompt contains "Start with EXACT
decision phrase" and Output JSON schema with
decision/justifications/markdown_comment.

\- full_file_summary: includes hunkHeaders variable in output.

\- Snippet:

\@Test

void render_methodQuality_includesDecisionInstructionAndSchema() {

String rendered =
promptRenderer.render(\"prompts/rules/java_method_quality.md\", vars);

assertThat(rendered).contains(\"Start with EXACT decision phrase\");

assertThat(rendered).contains(\"\\\"decision\\\"\");

assertThat(rendered).contains(\"\\\"markdown_comment\\\"\");

}

\@Test

void render_fullFileSummary_includesHunkHeaders() {

Map\<String,Object\> vars = Map.of(\"filePath\", \"src/Main.java\",
\"hunkHeaders\", \"@@ -10,0 +10,5 @@\");

String rendered =
promptRenderer.render(\"prompts/rules/full_file_summary.md\", vars);

assertThat(rendered).contains(\"@@ -10,0 +10,5 @@\");

}

D\) DiffLocator mapping

\- Purpose: Map TaskEnvelope.diff to inline comment line positions.

\- Scenarios:

\- Mapping found → returns InlineTarget.

\- Mapping ambiguous → empty Optional triggers fallback.

\- Snippet:

\@Test

void locate_returnsInlineTarget_whenLinePresent() {

InlineTarget t = diffLocator.locate(fileMeta, diffMeta,
astMeta).orElseThrow();

assertThat(t.getPosition()).isGreaterThan(0);

}

2\) Integration tests

A\) Enqueue/Dequeue with Testcontainers RabbitMQ

\- Purpose: Validate publish → consume → LLM mock → parse → GitHub post
path.

\- Setup:

\- Testcontainers: RabbitMQ + Postgres.

\- Mock LLM returns compliant JSON with "Yes" and markdown_comment.

\- Mock GitHubService to capture posted comments.

\- Flow:

\- Trigger orchestrator to create ReviewTask and enqueue TaskEnvelope.

\- Consumer processes message, calls mocked LLM, parses, posts
markdown_comment inline, updates DB.

\- Assertions:

\- ReviewTask status=COMPLETED; posting_status=POSTED.

\- GitHubService.postInlineComment called with markdown body containing
decision phrase and bullets.

B\) Retry on parsing failure

\- LLM first returns invalid JSON (missing decision) → consumer retries
with clarification → second response valid.

\- Assertions:

\- Attempt count increments, final status=COMPLETED.

\- Parser logs include first failure, success on second.

C\) LLM timeout and requeue with priority

\- Mock LLM client to throw LlmTimeoutException.

\- Assertions:

\- QueueService.requeueWithBackoff invoked with high priority.

\- ReviewTask attempt_count incremented, status remains
ENQUEUED/PROCESSING appropriately.

\- Message eventually consumed after delay and succeeds with a later
valid response.

D\) GitHub rate limit and pending_comments retry

\- Mock GitHubService to throw rate limit with reset time.

\- Assertions:

\- pending_comments row created with status=PENDING.

\- PendingCommentJob retries after reset; POSTED status set on success.

E\) DLQ path

\- Force repeated parse failures beyond maxParseRetries.

\- Assertions:

\- ReviewTask status=FORMAT_ERROR; raw_response stored in llm_calls.

\- No inline comment posted; optional fallback comment or pending
comment created.

F\) Full-file summary rule integration

\- Trigger full-file-summary rule on a file with multiple hunks.

\- Assertions:

\- Rendered prompt contains hunkHeaders.

\- LLM JSON parsed; summary markdown_comment posted at PR level (general
comment).

3\) End-to-end (E2E) tests

A\) PR with modified Java method and delayed LLM

\- Setup:

\- End-to-end pipeline using Testcontainers (RabbitMQ, Postgres).

\- Mock LLM introduces artificial delay near timeout; first call times
out, requeued, second call returns valid JSON.

\- Steps:

\- Trigger review, enqueue tasks.

\- Verify consumer redelivery and resumption.

\- Acceptance:

\- System posts Markdown comment to GitHub after queue processing, with
justification if "Yes" decision.

\- ReviewTask ends COMPLETED; llm_calls includes latency; findings
stored.

B\) 5-minute hard cap and redelivery

\- Simulate model stall beyond 5 minutes.

\- Assertions:

\- Consumer requeues with HIGH priority; task not stuck in PROCESSING.

\- Eventually either succeeds or transitions to FAILED/FORMAT_ERROR per
policy.

C\) Pending comments end-to-end

\- Force transient GitHub 5xx for a period, then allow success.

\- Assertions:

\- PendingCommentJob picks up PENDING, retries, and posts;
posting_status transitions to POSTED.

Edge case scenarios

\- Invalid prompt outputs:

\- Missing decision: triggers clarification retry; final fail →
FORMAT_ERROR and fallback posting.

\- Wrong decision phrase: ParseException triggers retry path.

\- Queue failures:

\- Broker unavailable at publish: QueueService applies backoff; tasks
remain PENDING then republish succeeds.

\- Message routed to DLQ: DLQ consumer test verifies visibility and
requeue via admin endpoint.

\- Mapping failures:

\- No inline target: posting falls back to file-level or general PR
comment; test verifies fallback behavior.

\- Oversized markdown_comment:

\- PostingService truncates and notes truncation; full text stored in
DB. Test ensures truncation marker is present.

Sample acceptance criteria

\- Unit: Parser accepts only exact "Yes, please improve the quality of
the code." or "No, there is no need to modify the code; it is already at
a good level." and extracts markdown_comment; invalid phrases cause
ParseException.

\- Integration: After enqueue, a worker processes the message, posts a
Markdown comment containing the decision and bullet justifications, and
marks task COMPLETED with POSTED status.

\- E2E: With an LLM delay causing timeout, the consumer requeues the
task with HIGH priority and eventually posts markdown_comment. Pending
GitHub posts are retried until POSTED or marked FAILED after configured
attempts.

\- Full-file summary: For a file with multiple hunks, rendered prompt
includes hunkHeaders; LLM response is parsed and posted as a PR summary.

Additional implementation notes for tests

\- Use \@RabbitAvailable or Testcontainers RabbitMQ for realistic AMQP
behavior.

\- Use Awaitility to wait for asynchronous state transitions in
integration/E2E tests.

\- Introduce deterministic fixtures for TaskEnvelope with minimal but
realistic diff and AST metadata.

\- Snapshot tests for Markdown output rendering can catch regressions in
formatting.

\- Seed rules from classpath YAML and assert loading of
resultFormat=JSON_WITH_DECISION and includeDiffHunks/includeAstMetadata
flags.

By covering publishing/backoff, structured parsing, timeout/resume, DLQ,
pending posting retries, and rule-specific prompt rendering (including
new full-file summary), this test plan ensures the system's
queue-driven, structured-output pipeline is reliable, scalable, and
GitHub-ready.

Tab 20

\### Feedback on the Outcome for Refined Prompt 6 (Testing and
Validation)

This outcome is a thorough and well-organized expansion of the testing
section, providing a clear strategy with coverage matrix, tooling,
scenarios, snippets, and acceptance criteria. It effectively
incorporates the queue integration (e.g., enqueue/dequeue, backoff),
structured prompts (e.g., decision validation, parse failures), and new
elements like the full-file summary rule. The use of Testcontainers for
realistic brokers/DB, Awaitility for async waits, and specific
assertions (e.g., status transitions, Markdown posting) makes it highly
practical for Spring Boot development. This adds the needed rigor to
ensure the system\'s reliability without being overly verbose.

\*\*Strengths\*\*:

\- \*\*Comprehensive Coverage\*\*: Matrix breaks down
unit/integration/E2E logically, addressing key features (e.g.,
timeouts/requeues, parsing retries, pending_comments). Edge cases like
DLQ, invalid outputs, and mapping failures are explicitly tested.

\- \*\*Tooling and Snippets\*\*: Recommends appropriate tools (JUnit,
Mockito, Testcontainers) and provides concise, executable snippets
(e.g., \@Test for publish, parse_yesDecision). Fixtures usage is smart
for reproducibility.

\- \*\*Rule-Specific Tests\*\*: Good inclusion of full-file summary
(e.g., hunkHeaders in rendered prompt) and general rule rendering/YAML
loading.

\- \*\*Acceptance Criteria\*\*: Clear and measurable (e.g., \"posts a
Markdown comment containing the decision and bullet justifications\"),
tying back to business value.

\- \*\*Notes\*\*: Helpful tips like snapshot tests for Markdown and
deterministic fixtures enhance implementability.

\*\*Areas for Improvement/Gaps/Inconsistencies\*\*:

\- \*\*Snippet Completeness\*\*: Snippets are good but lack imports
(e.g., \@Test needs import org.junit.jupiter.api.Test) or full class
context---add for copy-paste usability. No snippet for E2E (e.g.,
\@SpringBootTest with Testcontainers setup).

\- \*\*Coverage Balance\*\*: Heavy on queue/parsing, but less on GitHub
mocks (e.g., WireMock stubs for rate limits)---expand with a snippet for
that.

\- \*\*Performance/Metrics\*\*: No tests for non-functionals like queue
depth metrics or scalability (e.g., load test with multiple
consumers)---suggest adding under \"Load/Stress Tests\".

\- \*\*Edge Cases Depth\*\*: Covers main ones, but misses e.g., broker
outage during redelivery (simulate with Testcontainers stop/start) or
oversized envelopes causing deserialization errors.

\- \*\*Minor Inconsistencies\*\*: Assumes cfg from prior spec (e.g.,
maxParseRetries), but no tie-in to config YAML tests. Full-file summary
test mentions \"posted as a PR summary\"---clarify if that\'s general
comment.

\- \*\*Readability\*\*: Matrix is structured, but could use tables for
scenarios/assertions if in MD format.

This outcome is \~95% excellent---makes the spec testable and robust,
with minor additions for completeness.

\### Relevance of Remaining Prompt (7) Based on Current Spec

The last prompt (7) is still relevant and ideal as the finale. The spec
has all pieces (queues, prompts, rules, testing), but 7 will unify them
into one polished document, add explicit rationales (e.g., RabbitMQ
choice), resolve any unresolved audits (e.g., AST errors, diff renames),
and ensure everything cross-references (e.g., testing to YAML flags).

\### Would I Iterate More on This Specification?

Yes, one final iteration with prompt 7 to consolidate and refine the
entire spec. It\'s already strong and implementation-ready, but this
will make it a single, cohesive artifact without loose ends.

\### Comprehensive Feedback Prompts to Send to the LLM for Further
Improvement

Here\'s the refined prompt 7, updated for the latest outcomes (e.g.,
referencing testing snippets, full_file_summary.md, JsonNode parsing).
Paste all recent outputs into the LLM query for context.

\*\*Refined Prompt 7\*\*:

\"Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption
as in TaskEnvelope, followed by sync LLM calls and GitHub updates via
pending_comments. Restructure all prompts to start with \'No, there is
no need to modify the code; it is already at a good level.\' or \'Yes,
please improve the quality of the code.\', include justifications, and
output in Markdown for direct posting, building on the updated MD files
(e.g., full_file_summary.md) and JsonNode parsing with parseResponse.
Resolve any gaps from previous audits (e.g., diff edge cases like
renames in hunk mapping for inline posts, AST parse errors with fallback
skipping). Add rationales for choices (e.g., RabbitMQ over Kafka for
simplicity in task queues vs streaming), code examples (e.g.,
\@RabbitListener for consumers with onMessage pseudo-code, JUnit
snippets from testing matrix), and updated modules (e.g., add \'rules\'
module details). Improve the ASCII diagram with more flow labels if
needed. Incorporate the expanded testing strategy with its matrix,
snippets, and criteria. Output the entire revised spec with these
improvements for implementation readiness.\"

Tab 21

asked grok to specify next step for the specification

Take this full specification for a Spring Boot-based GitHub code
reviewer using LLMs and enhance it comprehensively. Integrate a message
queue (e.g., RabbitMQ) for scalable parallelization of LLM requests,
where messages encapsulate state (file, diff hunk, etc.) for resumption
as in TaskEnvelope, followed by sync LLM calls and GitHub updates via
pending_comments. Restructure all prompts to start with \'No, there is
no need to modify the code; it is already at a good level.\' or \'Yes,
please improve the quality of the code.\', include justifications, and
output in Markdown for direct posting, building on the updated MD files
(e.g., full_file_summary.md) and JsonNode parsing with parseResponse.
Resolve any gaps from previous audits (e.g., diff edge cases like
renames in hunk mapping for inline posts, AST parse errors with fallback
skipping). Add rationales for choices (e.g., RabbitMQ over Kafka for
simplicity in task queues vs streaming), code examples (e.g.,
\@RabbitListener for consumers with onMessage pseudo-code, JUnit
snippets from testing matrix), and updated modules (e.g., add \'rules\'
module details). Improve the ASCII diagram with more flow labels if
needed. Incorporate the expanded testing strategy with its matrix,
snippets, and criteria. Output the entire revised spec with these
improvements for implementation readiness.

After outputting the full revised specification, provide the following
additional sections:

1\. \*\*List of All Files Needed for the Project\*\*: Create a
comprehensive list of all Java source files, configuration files,
resource files (e.g., YAML, MD prompts), and any other necessary files
(e.g., build scripts like pom.xml or build.gradle if applicable). For
each file, describe its main responsibilities in 2-4 bullet points, and
include a pseudo-idea of what APIs or methods it would offer (e.g.,
public interfaces, key classes, or endpoints). Organize the list by
module or package (e.g., com.example.reviewer.api,
com.example.reviewer.mq) for clarity. Focus on core implementation
files, avoiding test files unless critical.

2\. \*\*Action Plan for Implementation\*\*: Outline a multi-stage action
plan with 8-12 progressive stages to implement the project. Start with
independent components that don\'t require external integrations (e.g.,
Java parsing using visitor pattern, diff file parsing and intersection).
Progressively build up to rules engine, LLM invocation (using mocks
initially), queue system, persistence, and finally GitHub integration.
Each stage should include:

\- A brief description of what to implement.

\- Key files/classes involved.

\- Dependencies on previous stages.

\- Testing approach (e.g., unit tests first, then integration).

\- Milestones for verification (e.g., \"Run a local parse on a sample
Java file and verify AST nodes\").

Emphasize starting with standalone, testable code (e.g., parsing and
rules without GitHub or real LLMs), using mocks/stubs for externals
until later stages. Ensure the plan is realistic, with stages that can
be implemented incrementally for quick wins and easy debugging.

Tab 22

overall specification for the project worked with several llms.

Below is the complete, implementation-ready specification for a Spring
Boot-based GitHub code reviewer using LLMs. It integrates a message
queue (RabbitMQ) for scalable parallelization, enforces a structured
prompt-output protocol with Yes/No decisions, justifications, and
Markdown, includes robust parsing and posting flows, and provides
detailed testing and operational guidance. It resolves prior gaps (diff
edge cases, AST fallback, resumption), includes rationale for
architectural choices, and adds code and config snippets.

I. Purpose and scope

\- Automatically review GitHub Pull Requests (PRs) using LLMs with a
focus on Java (method-level reviews, Javadoc audits, and full-file
summaries).

\- Use diff + AST intersection to select relevant nodes and apply rules.

\- Scale parallel LLM requests via RabbitMQ; messages encapsulate full
state (TaskEnvelope) to enable stateless workers and resumption.

\- Enforce structured LLM outputs:

\- Start with EXACT decision phrase: either "No, there is no need to
modify the code; it is already at a good level." or "Yes, please improve
the quality of the code."

\- Provide bullet-list justifications and a Markdown-formatted body
suitable for GitHub comments.

\- Wrap in JSON for reliable parsing; fields include decision,
justifications, markdown_comment, and rule-specific sections.

\- Post to GitHub inline (preferred) or as summary comments; persist
pending comments for retry.

II\. Architecture overview and rationale

\- Spring Boot (Java 17+).

\- Modules:

\- api: Webhooks and manual triggers.

\- github: GitHub App integration; posting services.

\- parser: Diff parsing, JavaParser AST, intersection, mapping to PR
lines.

\- rules: Registry, YAML definitions, selectors, actions, and prompt
rendering.

\- prompts: Prompt templates and renderer.

\- llm: Client abstraction (LangChain4j or custom) with profiles and
timeouts.

\- mq: Queue publisher/consumer components, backoff, resumption.

\- review: Orchestrator building runs and tasks.

\- posting: Pending comment management and retry jobs.

\- persistence: Repositories and entities for runs, tasks, findings,
pending comments, llm calls.

\- config: Externalized configuration and feature flags.

Rationale for RabbitMQ vs Kafka

\- RabbitMQ is optimized for task queues and work distribution with
features like per-message priority, delayed delivery (via plugin or
TTL+DLX), and simple consumer concurrency. It fits at-least-once
processing and stateless workers well.

\- Kafka excels at high-throughput streaming and event sourcing but adds
complexity for delayed retries, per-message priority, and small-task
semantics. For per-task LLM reviews with controlled concurrency and
backoff needs, RabbitMQ is simpler and sufficient. Kafka can be added
later if streaming analytics or replay at scale become requirements.

III\. ASCII architecture diagram

+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| API Controller \|\<\-\-\-\-\-\--\>\| GitHubService \|

\| (webhooks/triggers) \| \| (hub4j + WebClient) \|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\--+

\| \|

v \|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+ fetch PR info\|post
comments/checks

\| ReviewOrchestrator \|\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| (build run & tasks) \| v

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| TaskEnvelope (rendered prompts, diff, AST) \|

v \|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| QueueService \|\-\-\-\-\-\-\-\--\>\| RabbitMQ Broker \|

\| (publish, backoff) \|\<\-\-\-\-\-\-\-\--\| (durable, DLQ, delay) \|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+ backoff
+\-\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\--+

\^ \|

\| requeueWithBackoff \| deliver / redeliver

\| v

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| PendingCommentJob \|\-\-\-\-\-\-\-\--\>\| Worker Consumers \|

\| (retry GitHub posts) \| post \| (@RabbitListener) \|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+\<\-\-\-\-\-\-\-\--+
(LLM call + parse + post\|

\^ DB + persist) \|

\| ack\|

+\-\-\-\-\-\-\-\-\-\--+\-\-\-\-\-\-\-\-\-\-\-\--+
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

\| Persistence \|\<\-\-\-\-\-\-\--\>\| LLM Providers \|

\| (runs/tasks/findings/ \| metrics \| (LangChain4j/OpenAI) \|

\| pending_comments/llm) \|
+\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\--+

IV\. End-to-end flow

1\) Trigger and prep

\- Receive webhook or manual trigger for a PR.

\- Fetch changed files/patches; ignore deletions and non-Java for
current rules.

2\) Parse diffs and AST

\- Parse unified diffs; detect renames/moves (from GitHub API) and
maintain file path mapping for inline posts.

\- Parse Java files with JavaParser; handle parse errors:

\- If AST parse fails for a file, skip AST-dependent rules for that
file; still allow file-level rules (e.g., full-file summary based on
diff only).

\- Log and include a note in run report.

3\) Rule selection and context assembly

\- Intersect diff hunks with AST node ranges; mark nodes as
ADDED/MODIFIED/UNCHANGED in modified regions.

\- For interested node/rule pairs, collect variables (method signature,
annotations, Javadoc, diffSnippet/hunkHeaders, method body, etc.).

4\) Prompt rendering

\- Load system prompt and rule prompt; render with variables.

\- Enforce structured output instructions and JSON schema in prompts.

5\) Task creation and enqueue (Step 9)

\- Create ReviewTask rows (PENDING), fill payload_hash for idempotency.

\- Build TaskEnvelope with full context, including:

\- repo/pr info, file metadata, diff hunks, AST node metadata.

\- prompts with systemPromptRef, rulePromptRef, renderedUserPrompt.

\- posting options (inlineAllowed, summaryAllowed).

\- retry priority and attempt count.

\- QueueService.publish(envelope) → durable message; mark task ENQUEUED.

6\) Consumer execution (Step 10)

\- Worker dequeues message, loads ReviewTask.

\- If task terminal (COMPLETED/CANCELED), ack and drop (idempotency).

\- Mark PROCESSING; increment attempt_count.

\- Call LLM synchronously with system + renderedUserPrompt; timeout via
profile (e.g., 60s).

\- On timeout:

\- Requeue with HIGH priority and exponential backoff.

\- Update last_error and attempts; ack original after re-enqueue via
delayed exchange.

7\) Parse and post (Step 11)

\- Parse JSON with strict validator:

\- decision must equal one of the two exact phrases.

\- justifications array present.

\- markdown_comment non-empty string.

\- Persist findings and LLM call metrics.

\- Post markdown_comment:

\- Prefer inline: map after-side line using diff locator that accounts
for renames (map to new file path).

\- If inline mapping unavailable or ambiguous, fallback to file-level
comment or PR summary.

\- On GitHub POST failures:

\- Store pending_comments with body_markdown and related metadata.

\- Posting job retries respecting rate-limit reset headers.

8\) Completion or retry (Step 12)

\- Success: mark task COMPLETED and posting_status POSTED; ack.

\- Parse failure: retry with clarification prompt up to maxParseRetries;
on final failure mark FORMAT_ERROR and optionally post fallback minimal
comment or store pending.

\- Transient errors: requeue with backoff.

\- Non-retryable errors: mark FAILED and route message to DLQ.

V. Prompt protocol and templates

Global rules

\- Start response with EXACTLY one of the two phrases.

\- Provide bullet justifications; use Markdown for the entire comment
body.

\- Wrap in a single JSON object with fields: decision, justifications,
markdown_comment, and rule-specific fields.

\- No text outside the JSON object.

\- If insufficient context, state so in justifications.

\- Avoid hallucinations; only reference provided code/diff.

Prompt files

\- System prompt: prompts/system/system_prompt.md

\- Java method quality: prompts/rules/java_method_quality.md

\- Javadoc audit: prompts/rules/javadoc_audit.md

\- Full-file summary: prompts/rules/full_file_summary.md

Each rule prompt includes the JSON schema with required fields decision,
justifications, markdown_comment, and rule-specific sections (issues\[\]
or typos_corrections/tag_issues/suggested_javadoc/changed_entities).

VI\. Message queue design

RabbitMQ setup

\- Durable queue: code-review.tasks

\- DLQ: code-review.tasks.dlq

\- Delayed retries: delayed exchange plugin or TTL+DLX with per-attempt
backoff.

\- Prefetch: tune to control in-flight LLM calls per worker.

\- Priority: enable per-message priority to expedite timeouts and
critical requeues.

TaskEnvelope fields (summary)

\- ids: runId, taskId, createdAt, attemptCount,
queueMessageId(optional), priority

\- repo: owner, name, prNumber, headSha(optional)

\- file: path, newPath(if renamed), changeType, language

\- diff: hunks, added/removed lines, lineMapping, preferredAfterLine

\- astNode: nodeType, nodeSignature, startLine, endLine,
modificationStatus

\- rule: ruleId, actionScope

\- llm: profileId, tokenBudget

\- prompts: systemPromptRef, rulePromptRef, renderedUserPrompt

\- postingOptions: inlineAllowed, summaryAllowed, checksApiAllowed

\- retry: priority, nextBackoffMs

VII\. Persistence schema (key tables)

review_runs

\- id, repo_owner, repo_name, pr_number, status
(RUNNING\|COMPLETED\|FAILED\|COMPLETED_WITH_ERRORS), created_at,
updated_at

review_tasks

\- id, run_id, rule_id, file_path, new_file_path(nullable), node_type,
node_signature

\- status:
PENDING\|ENQUEUED\|PROCESSING\|COMPLETED\|FAILED\|FORMAT_ERROR\|CANCELED

\- posting_status: NONE\|PENDING\|POSTED\|FAILED

\- attempt_count, last_error

\- queue_message_id, priority

\- payload_hash, created_at, updated_at

review_findings

\- id, task_id, file_path, decision, markdown_comment, summary

\- issues_json or javadoc_json or changed_entities_json

\- line_start, line_end (optional), severity_aggregate(optional)

\- created_at

pending_comments

\- id, task_id, repo_owner, repo_name, pr_number

\- file_path, line_number, side, body_markdown

\- status: PENDING\|POSTED\|FAILED, last_error, created_at, updated_at

llm_calls

\- id, task_id, provider, model, req_tokens, resp_tokens, latency_ms,
raw_response(truncated), created_at

VIII\. GitHub posting

\- Inline: map to right-side position using diff and handle renames (map
file_path→new_file_path).

\- Summary fallback: post PR-level comment if inline mapping is
ambiguous.

\- Check Runs: optional; if failures, fallback to comments.

\- Deduplication: include hidden marker \<!\-- taskId:\... \--\> inside
markdown_comment to prevent duplicate posts.

IX\. Error handling and resilience

\- Consumer failures:

\- If worker dies mid-task, broker redelivers; worker checks DB:

\- If COMPLETED/CANCELED, ack-drop.

\- If PROCESSING but stale, proceed; posting is idempotent with dedupe
marker.

\- Backoff and retries:

\- LLM timeouts: requeue with HIGH priority and exponential backoff.

\- Parsing failures: retry with clarification up to maxParseRetries.

\- GitHub rate-limits: store pending, retry after reset header
(PendingCommentJob).

\- AST parse errors:

\- Skip AST-dependent rules; proceed with file-level rules; log.

X. Configuration (application.yml excerpts)

github:

appId: \${GITHUB_APP_ID:}

privateKey: \${GITHUB_APP_PRIVATE_KEY:}

installationId: \${GITHUB_APP_INSTALLATION_ID:}

token: \${GITHUB_TOKEN:}

baseUrl: \${GITHUB_BASE_URL:https://api.github.com}

webhookSecret: \${GITHUB_WEBHOOK_SECRET:}

llm:

profiles:

openai-default:

provider: OPENAI

model: gpt-4o-mini

temperature: 0.2

maxTokens: 2000

timeoutSeconds: 60

systemPromptRef: prompts/system/system_prompt.md

ollama-local:

provider: OLLAMA

model: llama3:instruct

temperature: 0.1

timeoutSeconds: 60

review:

postToGitHub: true

tokenBudgetPerRun: 300000

maxParseRetries: 2

maxEndToEndTaskTimeoutSeconds: 300

mq:

type: rabbitmq

rabbitmq:

host: \${RABBITMQ_HOST:localhost}

port: \${RABBITMQ_PORT:5672}

username: \${RABBITMQ_USER:guest}

password: \${RABBITMQ_PASS:guest}

queues:

tasks: code-review.tasks

dlq: code-review.tasks.dlq

delayedExchange: code-review.delayed

routingKey: code-review.tasks

concurrency:

consumersPerQueue: 5

prefetch: 5

retry:

enabled: true

maxAttempts: 5

initialBackoffMs: 2000

multiplier: 2.0

maxBackoffMs: 60000

priority:

enabled: true

maxPriority: 9

backpressure:

maxEnqueueRatePerSec: 50

overflowPolicy: THROTTLE

posting:

checksApiEnabled: false

maxCommentLength: 50000

dedupeWindowMinutes: 30

XI\. Rule definitions (YAML)

resources/rules/java_method_quality.yaml

id: java-method-quality

name: Quality review for modified Java methods

trigger:

onLanguage: JAVA

onEntityTypes: \[METHOD\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: METHOD_SIGNATURE_ANNOTATIONS_JAVADOC_AND_BODY

promptRef: \"prompts/rules/java_method_quality.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true

includeAstMetadata: true

priority: 10

resources/rules/javadoc_audit.yaml

id: javadoc-audit

name: Javadoc typo and grammar audit

trigger:

onLanguage: JAVA

onEntityTypes: \[JAVADOC\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: JAVADOC_WITH_SIGNATURE_AND_PARAMS

promptRef: \"prompts/rules/javadoc_audit.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true

includeAstMetadata: true

priority: 5

resources/rules/full_file_summary.yaml

id: full-file-summary

name: Full-file summary for modified Java files

trigger:

onLanguage: JAVA

onEntityTypes: \[FILE\]

onFileChangeTypes: \[ADDED, MODIFIED\]

onlyIfModifiedInDiff: true

selector:

filters:

pathPatterns: \[\"src/main/java/\*\*\"\]

action:

scope: FILE_WITH_DIFF_HUNKS_AND_OVERVIEW

promptRef: \"prompts/rules/full_file_summary.md\"

resultFormat: JSON_WITH_DECISION

includeDiffHunks: true

includeAstMetadata: false

priority: 3

XII\. Consumer and parsing code excerpts

Rabbit listener pseudo-code

\@RabbitListener(queues = \"\${mq.rabbitmq.queues.tasks}\",

concurrency = \"\${mq.rabbitmq.concurrency.consumersPerQueue}\")

public void onMessage(Message msg) {

TaskEnvelope env = mapper.readValue(msg.getBody(), TaskEnvelope.class);

ReviewTask task = taskRepo.findById(env.getTaskId()).orElse(null);

if (task == null \|\| task.getStatus().isTerminal()) return ack();

taskRepo.markProcessing(task.getId());

try {

LLMResponse resp = llmClient.generate(systemPrompt(env),
userPrompt(env), timeout(cfg));

llmCallsRepo.save(metricsFrom(resp, task.getId()));

ParsedReview parsed = parser.parseResponse(resp.getText()); // validates
decision + markdown_comment

findingsRepo.saveAll(mapFindings(parsed, env, task));

Optional\<InlineTarget\> inline = diffLocator.locate(env.getFile(),
env.getDiff(), env.getAstNode(), env.getFile().getNewPath());

String body = addDedupeMarker(parsed.getMarkdown(), task.getId());

if (inline.isPresent() && env.getPostingOptions().isInlineAllowed()) {

String commentId = githubService.postInlineComment(env.getRepo(),
env.getRepo().getPrNumber(), inline.get(), body);

taskRepo.markCompleted(task.getId(), commentId);

} else {

String commentId = githubService.postGeneralComment(env.getRepo(),
env.getRepo().getPrNumber(), body);

taskRepo.markCompleted(task.getId(), commentId);

}

ack();

} catch (LlmTimeoutException te) {

queueService.requeueWithBackoff(env, task.incrementAttempt(), true);

taskRepo.updateLastError(task.getId(), \"LLM timeout\");

ack();

} catch (ParseException pe) {

if (task.getAttemptCount() \< cfg.maxParseRetries) {

queueService.requeueWithBackoff(appendClarification(env, pe),
task.incrementAttempt(), false);

} else {

taskRepo.markFormatError(task.getId(), pe.getMessage());

postingService.postFallbackOrPending(env, \"Automated review formatting
failed; raw output stored.\");

}

ack();

} catch (GitHubRateLimitException rl) {

pendingRepo.save(PendingComment.from(env, lastBodyOrPlaceholder(env)));

taskRepo.updatePostingStatus(task.getId(), PostingStatus.PENDING);

ack();

} catch (TransientException tx) {

queueService.requeueWithBackoff(env, task.incrementAttempt(), false);

taskRepo.updateLastError(task.getId(), tx.getMessage());

ack();

} catch (Exception ex) {

taskRepo.markFailed(task.getId(), ex.getMessage());

ack();

}

}

JsonNode-based parseResponse

private static final String YES = \"Yes, please improve the quality of
the code.\";

private static final String NO = \"No, there is no need to modify the
code; it is already at a good level.\";

public ParsedReview parseResponse(String text) {

JsonNode root;

try { root = mapper.readTree(text.trim()); }

catch (Exception e) { throw new ParseException(\"Invalid JSON\", e); }

String decision = requireText(root, \"decision\");

if (!YES.equals(decision) && !NO.equals(decision)) {

throw new ParseException(\"Invalid decision phrase: \" + decision);

}

List\<String\> justifications = toList(requireArray(root,
\"justifications\"));

String markdown = requireText(root, \"markdown_comment\");

List\<Issue\> issues = root.has(\"issues\")

? mapper.convertValue(root.get(\"issues\"), new
TypeReference\<List\<Issue\>\>() {})

: List.of();

return new ParsedReview(decision, justifications, markdown, issues,
root);

}

XIII\. Expanded testing strategy

Tools

\- JUnit 5, AssertJ, Mockito, Spring Boot Test

\- Spring AMQP Test and Testcontainers (RabbitMQ, Postgres)

\- WireMock for GitHub or Mockito stubs

\- Awaitility for async assertions

Coverage highlights

\- Unit: QueueService.publish and requeueWithBackoff; parser decisions;
prompt renderer includes schema and hunkHeaders.

\- Integration: Publish→consume→mock LLM→parse→post; retries on parse
failure; timeouts requeued with priority; pending_comments retry flow;
DLQ path.

\- E2E: Simulate PR, delayed LLM, redeliveries, 5-minute cap; final
comment includes decision phrase and Markdown; full-file summary rule
includes hunkHeaders.

Example acceptance criteria

\- "System posts Markdown comment to GitHub after queue processing, with
justification when decision is 'Yes'."

\- "Invalid decision phrase triggers retry with clarification; succeeds
within 2 attempts or marks FORMAT_ERROR."

\- "On GitHub rate-limit, pending comment is stored and later posted by
PendingCommentJob."

XIV\. Non-functional requirements

\- Scalability: Horizontal scale by adding consumer instances; tune
prefetch and concurrency; throttle publishing.

\- Reliability: Durable queues, DLQ, at-least-once with idempotent
posting.

\- Observability: Metrics for queue depth, retries, DLQ count, LLM
latency/tokens, posting success/fail; structured logs with runId/taskId.

\- Security: Webhook signature verification; secrets via env/vault;
redact sensitive content; minimal code in messages.

XV\. Example rationale for diff edge cases

\- Renames/moves: Extract new file path from GitHub diff metadata; store
in TaskEnvelope.file.newPath; the diffLocator uses newPath for inline
mapping.

\- Partial hunks and context lines: Mapping considers hunk header
offsets and context; if uncertainty remains, fallback to file-level
comment to avoid misposting.

------------------------------------------------------------------------------------------------------------------------

Additional Sections

1\) List of All Files Needed for the Project

Organized by package/module. Responsibilities and key APIs are
indicative.

Root

\- pom.xml or build.gradle

\- Manage dependencies: Spring Boot starters (web, validation, data-jpa,
amqp), hub4j, JavaParser, Jackson, LangChain4j, Testcontainers.

com.example.reviewer.api

\- WebhookController.java

\- POST /webhook/github to receive PR events; validate signature;
trigger orchestration.

\- Dependencies: GitHubService, ReviewOrchestrator.

\- ReviewRunController.java

\- POST /api/reviews/{owner}/{repo}/pulls/{pr}/run to start review
manually.

\- POST /api/reviews/tasks/{taskId}/requeue to requeue failed tasks.

com.example.reviewer.github

\- GitHubService.java

\- Methods: getPullRequestDetails(), listChangedFiles(),
postInlineComment(), postGeneralComment(), createOrUpdateCheckRun(),
getRateLimitReset().

\- Wrap hub4j and WebClient for Checks API.

\- GitHubClientFactory.java

\- Build authenticated clients for installations or PAT.

com.example.reviewer.parser

\- DiffParser.java

\- Parse unified diff, extract hunks, detect renames, build line
mappings.

\- JavaAstParser.java

\- Parse Java sources using JavaParser; expose node ranges; handle parse
errors gracefully.

\- DiffAstIntersectionService.java

\- Intersect AST nodes with diff; label nodes ADDED/MODIFIED; produce
candidates for rules.

\- DiffLocator.java

\- Map envelope.diff to PR inline positions; consider renames via
newPath; return Optional\<InlineTarget\>.

com.example.reviewer.rules

\- RuleDefinition.java

\- POJO representing rule config (trigger, selector, action, priority).

\- RuleRegistry.java

\- Load YAML rule files; provide lookup and filtering by context.

\- RuleSelectorEngine.java

\- Given AST/diff, select applicable nodes per rule.

\- RuleActionExecutor.java

\- Assemble variables for prompt rendering per rule's scope (method,
javadoc, file summary).

\- RuleConfigLoader.java

\- Load YAML from resources/rules/\*.yaml; validate fields
(resultFormat, includeDiffHunks, includeAstMetadata).

com.example.reviewer.prompts

\- PromptRenderer.java

\- Render MD templates with variables; inject system prompt and rule
prompt; ensure schema presence.

\- PromptLoader.java

\- Load MD files from classpath; cache.

com.example.reviewer.llm

\- LlmClient.java (interface)

\- generate(systemPrompt, userPrompt, timeoutSeconds): LLMResponse

\- LlmClientFactory.java

\- Create client based on profileId; supports OpenAI, Ollama, etc.

\- LlmProfiles.java

\- Bind llm.profiles.\* from YAML to POJOs.

\- LLMResponse.java

\- text(), usage/tokens, latency.

com.example.reviewer.mq

\- QueueService.java

\- publish(TaskEnvelope), requeueWithBackoff(TaskEnvelope, attempt,
highPriority)

\- Rate limiting and persistent message properties.

\- TaskEnvelope.java

\- Serializable DTO containing runId, taskId, repo/file/diff/ast,
prompts, retry, posting options.

\- WorkerConsumer.java

\- \@RabbitListener on tasks queue; onMessage() orchestrates LLM call,
parse, post, persist, and ack; handles retries and DLQ.

\- MqConfig.java

\- Declare queues, exchanges, bindings, and RabbitTemplate settings
(prefetch, priorities).

com.example.reviewer.review

\- ReviewOrchestrator.java

\- Orchestrate runs: parse diffs, ASTs, select rules/nodes, render
prompts, create tasks, enqueue envelopes.

\- ReviewRunService.java

\- Manage run lifecycle; aggregate results and finalize status.

\- ClarificationPromptService.java

\- Build clarification prompts for parse retries.

com.example.reviewer.posting

\- PostingService.java

\- Post inline or general comments using GitHubService; dedupe via
hidden marker; truncate long comments safely.

\- PendingCommentJob.java

\- Scheduled job to retry pending comments; respects rate limits.

\- PendingCommentService.java

\- CRUD for pending_comments and retry orchestration.

com.example.reviewer.persistence

\- Entities: ReviewRun, ReviewTask, ReviewFinding, PendingComment,
LlmCall

\- Repositories: ReviewRunRepository, ReviewTaskRepository,
ReviewFindingRepository, PendingCommentRepository, LlmCallRepository

\- PersistenceConfig.java (JPA config, auditing timestamps)

com.example.reviewer.parsing (response parsing)

\- ResponseParser.java

\- parseResponse(String) → ParsedReview; validates decision and
markdown_comment.

\- ParsedReview.java

\- decision, justifications, markdown, issues/javadoc/changed_entities,
raw JsonNode.

com.example.reviewer.config

\- AppProperties.java

\- Bind application.yml sections (github, llm, mq, posting, review).

\- SecurityConfig.java (optional for webhook verification)

resources

\- application.yml

\- prompts/system/system_prompt.md

\- prompts/rules/java_method_quality.md

\- prompts/rules/javadoc_audit.md

\- prompts/rules/full_file_summary.md

\- rules/java_method_quality.yaml

\- rules/javadoc_audit.yaml

\- rules/full_file_summary.yaml

2\) Action Plan for Implementation

Stage 1: Core parsing foundations

\- Implement DiffParser and JavaAstParser with error handling.

\- Implement DiffAstIntersectionService.

\- Milestone: Parse a sample repo; print AST nodes intersecting modified
lines.

\- Tests: Unit tests for diff parsing and AST parsing; sample file
assertions.

Stage 2: Rule definitions and selection

\- Implement RuleDefinition, RuleConfigLoader, RuleRegistry,
RuleSelectorEngine.

\- Load YAML rules; select nodes per rule triggers/selectors.

\- Milestone: For a sample diff+AST, list which rules fire on which
nodes.

\- Tests: Unit tests validating selectors and YAML loading.

Stage 3: Prompt rendering

\- Implement PromptLoader and PromptRenderer; render MD prompts with
variables; ensure decision/schema present.

\- Milestone: Render java_method_quality prompt with a sample method and
diff snippet.

\- Tests: Unit tests for template variable injection; ensure schema
fields exist.

Stage 4: Response parser

\- Implement ResponseParser with strict JsonNode-based validation.

\- Milestone: Parse valid/invalid sample JSON; enforce decision phrase.

\- Tests: Unit tests for yes/no cases, missing fields, invalid
decisions.

Stage 5: Persistence layer

\- Create JPA entities and repositories for runs, tasks, findings,
pending comments, LLM calls.

\- Milestone: Persist run/tasks; query and update task status
transitions.

\- Tests: Repository tests with H2.

Stage 6: Review orchestration

\- Implement ReviewOrchestrator: build run, parse, select rules, render
prompts, create ReviewTask rows.

\- Milestone: Orchestrate tasks without queue yet; print envelopes to
logs.

\- Tests: Unit tests using mocks for parsers and rules.

Stage 7: MQ integration (producer)

\- Implement QueueService with RabbitTemplate; MqConfig
(queues/exchanges).

\- Publish TaskEnvelope messages; apply rate limiting.

\- Milestone: Enqueue messages to RabbitMQ (Testcontainers) and verify
queue depth.

\- Tests: Integration tests for publish and requeueWithBackoff.

Stage 8: MQ consumer and LLM mock

\- Implement WorkerConsumer with \@RabbitListener; integrate
ResponseParser and PostingService (mock GitHub).

\- Use LlmClient mock initially; handle retries and parsing errors.

\- Milestone: Consume message, parse, and simulate post via mock.

\- Tests: Integration tests for end-to-end message flow.

Stage 9: LLM provider integration

\- Implement LlmClientFactory and OpenAI/Ollama clients; profile config
and timeout handling.

\- Milestone: Real LLM call returning valid JSON for a sample prompt (or
continue with mocks).

\- Tests: Integration tests with mocked provider or sanitized e2e if
credentials available.

Stage 10: GitHub integration

\- Implement GitHubService posting (inline and general); handle rate
limit and errors.

\- Implement PendingCommentJob for retries.

\- Milestone: Post to a test PR (or mock) and verify comment content.

\- Tests: Integration with WireMock or sandbox repo; verify
pending_comments path.

Stage 11: Full-file summary rule and edge cases

\- Implement full_file_summary rule prompt, orchestrator action scope,
and parsing.

\- Handle rename mapping in DiffLocator; AST parse fallback paths.

\- Milestone: For a file rename with hunks, produce PR-level summary
comment referencing hunkHeaders.

\- Tests: Unit/integration tests for rename mapping and rule posting.

Stage 12: Observability and hardening

\- Add metrics and structured logging; DLQ management tools or
endpoints.

\- Security hardening (webhook sig verification, secret management).

\- Milestone: Dashboards/logs show task lifecycle; DLQ observable; admin
requeue endpoint works.

\- Tests: Non-functional checks; load/concurrency smoke tests.

Each stage is independently testable. Early stages use mocks/stubs for
externals (LLM/GitHub). Progressively integrate RabbitMQ and GitHub once
core parsing and rules are solid, enabling incremental validation and
faster iteration.
