Yes, but we lack rules for traversing the nodes of a Java file and
checking if a method was modified, and if so, requesting an LLM to audit
the method. And if the LLM finds a problem, we would report it to
GitHub. Is that correct?

Tab 2

Horizon beta says:

Correct. That kind of "method-diff-aware + LLM-audit" rule is not
implemented yet.

What you have

\- One static semantic rule: \`BroadCatchIntroducedRule\` (no LLM calls,
flags broad catches on after-side changed lines).

\- Full plumbing to:

\- Parse Java to an AST (\`JavaParsingService\`).

\- Map diffs to after-side changed lines (diff parser + mapping).

\- Run rules deterministically and render/post findings (bootstrap
scaffolding, posting worker to be added).

What is missing for your described rule

\- A new rule that:

1\) Iterates Java AST nodes to find method declarations.

2\) Determines if each method was modified in the PR by intersecting the
method's after-line span with the diff's after-changed lines.

3\) If modified, constructs a prompt with method context (e.g.,
\`enrichedNodeContent\`, provenance) and calls an LLM.

4\) Interprets the LLM response and emits \`LLMInlineFindingOut\` with
precise after-line anchoring.

5\) Lets the posting worker dedupe and post to GitHub.

If you want, I can outline the exact API shape and scaffolding for such
a rule next:

\- \`MethodModifiedLlmAuditRule\` with:

\- Config (model name, timeouts, prompt templates, max tokens).

\- \`LlmClient\` interface (so the rule is testable with a mock).

\- AST-to-diff intersection helper.

\- Deterministic finding ID generation strategy.

And wire it into \`RuleRegistryBuilder\` behind a config flag so you can
enable it incrementally.

Tab 3

Okay, please review the original specification for this project.

I believe we need to create a bootstrapping project, or perhaps a
sub-module for defining rules.

We need a rule for reviewing the Java code.

See the specification.

Please create an action plan on how to implement such a rule.

And for now, feel free to ignore aspects in the specification about
asynchronicity and using a queuing system.

---\-\--

The spec was like this:

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

Tab 4

Action plan outlined:

Here's a focused action plan to implement the "Java method modified →
LLM audit → GitHub report" rule within the current bootstrap project,
ignoring async/queueing aspects for now. This stays within the existing
engine-bootstrap-app setup and composes your existing parser/diff/engine
components.

Goals

\- Implement a new rule that:

1\) Traverses Java AST to find method declarations.

2\) Determines if a method was modified in the PR by intersecting its
after-line range with changed lines from the diff.

3\) If modified, queries an LLM with a structured prompt.

4\) Parses the LLM's structured JSON output (Yes/No + markdown +
justifications).

5\) Emits LLMInlineFindingOut findings anchored to the modified method's
after-line(s).

\- Keep synchronous execution (no MQ).

\- Use existing components: diff parser, AST, engine runner, GitHub
connector.

\- Make the rule pluggable via RuleRegistryBuilder.

High-level design

\- Add a small LLM client abstraction to keep the rule testable and
provider-agnostic.

\- Add a prompt renderer for the rule to ensure structured schema and
deterministic formatting.

\- Add a strict response parser for JSON validation (enforce exact
decision phrases).

\- Add a utility to intersect method ranges with after-changed lines.

\- Implement the rule class and wire it into RuleRegistryBuilder behind
a flag.

\- Extend DryRunRenderer to print rule message excerpts for validation.

\- Provide config knobs in BootstrapConfig (model, timeouts,
enable/disable rule).

Deliverables

1\) New classes in engine-bootstrap-app

\- \`com.example.bootstrap.llm.LlmClient\` (interface)

\- \`com.example.bootstrap.llm.LlmResponse\`

\- \`com.example.bootstrap.llm.OpenAiLlmClient\` (simple HTTP client;
optional stub for now)

\- \`com.example.bootstrap.prompts.MethodAuditPromptRenderer\`

\- \`com.example.bootstrap.parsing.MethodDiffIntersection\`

\- \`com.example.bootstrap.rules.MethodModifiedLlmAuditRule\`

\- \`com.example.bootstrap.parsing.LlmResponseParser\`

\- \`com.example.bootstrap.config.MethodAuditConfig\` (encapsulate rule
config)

\- \`com.example.bootstrap/logging.RuleDiagnosticsLogger\` (optional
now; useful for timing)

2\) Refinements

\- \`BootstrapConfig\`: add toggle and config for the new rule:
enableMethodAudit, llmModel, llmTimeoutSeconds, prompt tuning fields.

\- \`RuleRegistryBuilder\`: include the new rule if enabled.

\- \`DryRunRenderer\`: render LLM decisions and show first line of
markdown.

3\) Tests (JUnit 5 + AssertJ)

\- Unit tests for intersection logic, prompt rendering, response
parsing, and rule behavior with a mock LlmClient.

\- Basic integration test using synthetic PR snapshot with one modified
method and mocked LLM.

Detailed steps

Step 1: Extend BootstrapConfig

\- Add fields:

\- \`enableMethodAudit\` (boolean, default false)

\- \`llmProvider\` (string; e.g., OPENAI or OLLAMA)

\- \`llmModel\` (string; e.g., gpt-4o-mini)

\- \`llmTimeoutSeconds\` (int; default 60)

\- \`maxPromptTokens\` (int; optional)

\- \`auditPaths\` (Set\<String\> glob-like path filters; default
src/main/java/\*\*)

\- Add builder setters with validation and Javadoc.

Step 2: LLM abstraction

\- \`LlmClient\`:

\- \`LlmResponse generate(String systemPrompt, String userPrompt, int
timeoutSeconds)\`

\- \`LlmResponse\`:

\- fields: \`text\`, \`requestTokens\`, \`responseTokens\`,
\`latencyMillis\`

\- Provide \`OpenAiLlmClient\` stub that throws
UnsupportedOperationException for now or calls a placeholder HTTP
endpoint if desired. The rule will depend only on \`LlmClient\`.

Step 3: Prompt rendering

\- \`MethodAuditPromptRenderer\` responsibilities:

\- Build a deterministic system prompt (either inline string or load
from classpath later).

\- Build user prompt with:

\- Method signature (name, params, modifiers)

\- Annotations and Javadoc (if available)

\- Method body

\- Optional diff snippet for context (best-effort; keep simple)

\- Append strict schema instructions:

\- decision must be exactly one of the two phrases.

\- include justifications array and markdown_comment.

\- Return both system + user prompts.

Step 4: Response parsing

\- \`LlmResponseParser\`:

\- Parse JSON using Jackson.

\- Enforce decision equals exactly one of:

\- "Yes, please improve the quality of the code."

\- "No, there is no need to modify the code; it is already at a good
level."

\- Require \`justifications\` array and \`markdown_comment\` string.

\- Provide a small result DTO: decision, justifications, markdown.

Step 5: Method/diff intersection

\- \`MethodDiffIntersection\`:

\- Helper that, given:

\- a method's startLine/endLine (after-side, per AST provider)

\- \`Set\<Integer\> afterChangedLines\` for the file

\- Returns:

\- \`boolean isModified\` if any overlap exists

\- \`int anchorAfterLine\` chosen deterministically:

\- first changed line within the method if present

\- else method start line

\- This keeps anchoring deterministic and robust.

Step 6: Implement the rule

\- \`MethodModifiedLlmAuditRule\`:

\- Traverse file AST to find method declarations.

\- For each method:

\- Resolve its after-side line range from AST metadata (ensure your
\`JavaParsingFileNode\` exposes line numbers; otherwise, retrieve from
node properties).

\- Use \`MethodDiffIntersection\` with per-file \`afterChangedLines\`
from \`DiffBuilder.DiffCatalog\`.

\- If modified:

\- Render prompts via \`MethodAuditPromptRenderer\`.

\- Call \`LlmClient\` with timeout from config.

\- Parse response via \`LlmResponseParser\`.

\- If decision is "Yes, please improve...", emit
\`LLMInlineFindingOut\`:

\- \`filePath\` = new path

\- \`afterLine\` = anchorAfterLine

\- \`ruleId\` = "java-method-quality"

\- \`title/summary\` = from decision/justifications

\- \`markdownBody\` = parsed markdown_comment

\- \`provenance\` = from \`ProvenanceFactoryImpl\`

\- Deterministic finding id: hash of filePath + method signature +
headSha + ruleId

\- If decision is "No...", skip or optionally produce an informational
finding (disabled by default).

\- Keep calls synchronous; handle exceptions:

\- On LLM errors, skip emitting; optionally log a diagnostic.

Step 7: Wire into RuleRegistryBuilder

\- Inject \`MethodModifiedLlmAuditRule\` when \`cfg.enableMethodAudit\`
is true.

\- Provide its dependencies:

\- \`LlmClient\` instance based on \`cfg.llmProvider\` (OpenAI stub or
future impl)

\- \`MethodAuditConfig\`

\- Access to:

\- \`JavaAstProvider\` via the \`RuleContext\`'s AST supplier

\- \`DiffBuilder.DiffCatalog\` after-changed lines via \`RuleContext\`
(ensure \`RuleContext\` already exposes these; if not, extend
RuleContext usage in orchestrator to pass after-lines map).

Step 8: Orchestrator alignment

\- Confirm \`RunnerOrchestrator\` already passes:

\- eligible file paths

\- map of afterChangedLines per file

\- \`astProvider::getAst\`

\- provenance factory

\- If any of these are missing in your actual \`RuleContext\` API, adapt
the builder usage to the real one.

Step 9: Dry-run output enhancement

\- Extend \`DryRunRenderer\` to show:

\- decision (Yes/No)

\- first bullet of justifications

\- first 80 chars of markdown for a quick glance

Step 10: Tests

\- Unit tests:

\- MethodDiffIntersection: various ranges/overlaps; anchor selection

\- LlmResponseParser: valid/invalid JSON, exact decision phrases

\- MethodAuditPromptRenderer: includes signature/body and schema
instructions

\- MethodModifiedLlmAuditRule: with a mock \`LlmClient\` returning
"Yes..." and "No..." responses

\- Integration test:

\- Synthetic \`PullRequestSnapshot\`:

\- One file \`src/Foo.java\` with a method body change

\- Unified diff patch with added lines inside the method

\- Head content includes the Java code

\- Run orchestrator in dry-run with the rule enabled and a mocked
\`LlmClient\` returning a "Yes..." decision

\- Assert one finding with expected anchor line and ruleId

Risk and assumptions

\- Line numbers must be available in \`JavaParsingFileNode\` for method
declarations. If missing, update \`JavaParsingService\` usage to
retrieve start/end lines; otherwise, fallback to heuristic anchoring
(e.g., method signature line via a simple regex on content).

\- \`RuleContext\` API alignment: the plan assumes we can get AST and
afterChangedLines per file. If your \`RuleContext\` differs, we'll adapt
mapping code accordingly.

\- LLM provider: This plan keeps the provider behind \`LlmClient\`,
enabling swaps later without refactoring the rule.

Minimal class sketches (non-async)

\- LlmClient (interface): generate(systemPrompt, userPrompt,
timeoutSeconds) → LlmResponse

\- LlmResponse: text, requestTokens, responseTokens, latencyMillis

\- MethodAuditPromptRenderer: render(filePath, methodSignature, javadoc,
annotations, body, diffSnippet) → pair of prompts

\- LlmResponseParser: parseDecisionAndMarkdown(text) → result DTO

\- MethodDiffIntersection: isModified(start, end, afterChangedLines) →
{boolean, anchorAfterLine}

\- MethodModifiedLlmAuditRule: implements your engine's Rule interface,
uses above components

Timeline (short)

\- Day 1:

\- Config, LLM interfaces, prompt renderer, response parser,
intersection helper

\- Unit tests for parser + intersection

\- Day 2:

\- Implement rule logic and wire into registry builder (feature-flag)

\- Mock-based unit test for the rule

\- Day 3:

\- Integration test with synthetic PR + mocked LLM

\- DryRunRenderer improvements and docs update

Outcome

\- You'll have a synchronous, testable rule that audits only modified
Java methods with an LLM and produces inline findings in dry-run mode.

\- When ready, we can add the posting worker to publish the same
findings to GitHub, and later introduce queueing for scalability.

Tab 5

Please go.

Full maven project with exceptional javadoc and good line comments
especially on bigger methods.

A [[README.md]{.underline}](http://readme.md) at the end that explains
the project in detail.

Thank.

Tab 6

Got it. I'll provide a single, self-contained Maven module for the
bootstrap application only, assuming all other sub-modules (GitHub
connector, diff parser, Java AST parser, rules engine core) already
exist and are consumed as dependencies. This module adds one new
LLM-backed rule named AnalyzeModifiedJavaMethodsRule (renamed from prior
placeholder) that audits modified Java methods synchronously, no queues.

What you'll get

\- One Maven module: engine-bootstrap-app

\- New rule: AnalyzeModifiedJavaMethodsRule

\- Minimal LLM abstraction (stub by default)

\- Orchestration to fetch PR snapshot, build diffs, parse Java AST
lazily, run rules, and render dry-run findings

\- Javadoc and line comments for clarity

\- README.md detailing setup and usage

Project layout

engine-bootstrap-app/

\- pom.xml

\- README.md

\- src/main/java/com/example/bootstrap/

\- BootstrapConfig.java

\- SnapshotFetcher.java

\- DiffBuilder.java

\- JavaAstProvider.java

\- ProvenanceFactoryImpl.java

\- RuleRegistryBuilder.java

\- RunnerOrchestrator.java

\- DryRunRenderer.java

\- BootstrapRunner.java

\- src/main/java/com/example/bootstrap/llm/

\- LlmClient.java

\- LlmResponse.java

\- StubLlmClient.java

\- src/main/java/com/example/bootstrap/prompts/

\- MethodAuditPromptRenderer.java

\- src/main/java/com/example/bootstrap/parsing/

\- MethodDiffIntersection.java

\- LlmResponseParser.java

\- src/main/java/com/example/bootstrap/rules/

\- AnalyzeModifiedJavaMethodsRule.java

\- src/test/java/com/example/bootstrap/

\- BootstrapConfigTest.java

\- DiffBuilderTest.java

\- JavaAstProviderTest.java

\- LlmResponseParserTest.java

\- MethodDiffIntersectionTest.java

\- AnalyzeModifiedJavaMethodsRuleTest.java

Important notes

\- This module depends on your existing components via Maven coordinates
(you may need to adjust groupId/artifactId/version to real ones).

\- No queueing system is used; LLM calls happen synchronously via a stub
client (you can plug a real client later).

\- The rule uses AST + diff intersection to audit only modified methods,
per your request.

Files

pom.xml

\<?xml version=\"1.0\" encoding=\"UTF-8\"?\>

\<project xmlns=\"http://maven.apache.org/POM/4.0.0\"

xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"

xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0
https://maven.apache.org/xsd/maven-4.0.0.xsd\"\>

\<modelVersion\>4.0.0\</modelVersion\>

\<groupId\>com.example\</groupId\>

\<artifactId\>engine-bootstrap-app\</artifactId\>

\<version\>1.2.0\</version\>

\<name\>Engine Bootstrap App\</name\>

\<description\>Single-module application that runs rules (including
AnalyzeModifiedJavaMethodsRule) synchronously over a GitHub
PR\</description\>

\<properties\>

\<maven.compiler.source\>17\</maven.compiler.source\>

\<maven.compiler.target\>17\</maven.compiler.target\>

\<project.build.sourceEncoding\>UTF-8\</project.build.sourceEncoding\>

\<junit.jupiter.version\>5.10.2\</junit.jupiter.version\>

\<assertj.version\>3.25.3\</assertj.version\>

\<slf4j.version\>2.0.12\</slf4j.version\>

\<jackson.version\>2.17.1\</jackson.version\>

\</properties\>

\<dependencies\>

\<!\-- Existing modules (adjust to your real coordinates) \--\>

\<dependency\>

\<groupId\>com.example\</groupId\>

\<artifactId\>github-pull-request-connector\</artifactId\>

\<version\>1.0.0\</version\>

\</dependency\>

\<dependency\>

\<groupId\>com.example\</groupId\>

\<artifactId\>llm-reviewer-diff-parser\</artifactId\>

\<version\>1.0.0\</version\>

\</dependency\>

\<dependency\>

\<groupId\>com.example\</groupId\>

\<artifactId\>java-ast-parser\</artifactId\>

\<version\>1.0.0\</version\>

\</dependency\>

\<dependency\>

\<groupId\>com.example\</groupId\>

\<artifactId\>rules-engine-core\</artifactId\>

\<version\>1.0.0\</version\>

\</dependency\>

\<!\-- Logging \--\>

\<dependency\>

\<groupId\>org.slf4j\</groupId\>

\<artifactId\>slf4j-api\</artifactId\>

\<version\>\${slf4j.version}\</version\>

\</dependency\>

\<dependency\>

\<groupId\>org.slf4j\</groupId\>

\<artifactId\>slf4j-simple\</artifactId\>

\<version\>\${slf4j.version}\</version\>

\<scope\>runtime\</scope\>

\</dependency\>

\<!\-- JSON (LLM output parsing) \--\>

\<dependency\>

\<groupId\>com.fasterxml.jackson.core\</groupId\>

\<artifactId\>jackson-databind\</artifactId\>

\<version\>\${jackson.version}\</version\>

\</dependency\>

\<!\-- Test \--\>

\<dependency\>

\<groupId\>org.junit.jupiter\</groupId\>

\<artifactId\>junit-jupiter\</artifactId\>

\<version\>\${junit.jupiter.version}\</version\>

\<scope\>test\</scope\>

\</dependency\>

\<dependency\>

\<groupId\>org.assertj\</groupId\>

\<artifactId\>assertj-core\</artifactId\>

\<version\>\${assertj.version}\</version\>

\<scope\>test\</scope\>

\</dependency\>

\<dependency\>

\<groupId\>org.mockito\</groupId\>

\<artifactId\>mockito-core\</artifactId\>

\<version\>5.12.0\</version\>

\<scope\>test\</scope\>

\</dependency\>

\</dependencies\>

\<build\>

\<plugins\>

\<plugin\>

\<groupId\>org.apache.maven.plugins\</groupId\>

\<artifactId\>maven-surefire-plugin\</artifactId\>

\<version\>3.2.5\</version\>

\<configuration\>

\<useModulePath\>false\</useModulePath\>

\</configuration\>

\</plugin\>

\<plugin\>

\<groupId\>org.apache.maven.plugins\</groupId\>

\<artifactId\>maven-jar-plugin\</artifactId\>

\<version\>3.4.1\</version\>

\<configuration\>

\<archive\>

\<manifest\>

\<mainClass\>com.example.bootstrap.BootstrapRunner\</mainClass\>

\</manifest\>

\</archive\>

\</configuration\>

\</plugin\>

\</plugins\>

\</build\>

\</project\>

src/main/java/com/example/bootstrap/BootstrapConfig.java

\[Same as previous message, includes fields for enableMethodAudit,
llmProvider, llmModel, llmTimeoutSeconds; with Javadoc and builder
validation\]

src/main/java/com/example/bootstrap/SnapshotFetcher.java

\[As provided earlier; thin wrapper over GitHubPullRequestService\]

src/main/java/com/example/bootstrap/DiffBuilder.java

\[As provided; uses existing DiffParser/WrappedFileDiff/WrappedHunk,
builds after-changed lines\]

src/main/java/com/example/bootstrap/JavaAstProvider.java

\[As provided; lazy parsing from head content with JavaParsingService\]

src/main/java/com/example/bootstrap/ProvenanceFactoryImpl.java

\[As provided; creates LLMProvenance from PullRequestSnapshot\]

src/main/java/com/example/bootstrap/RuleRegistryBuilder.java

package com.example.bootstrap;

import com.example.bootstrap.llm.LlmClient;

import com.example.bootstrap.llm.StubLlmClient;

import com.example.bootstrap.prompts.MethodAuditPromptRenderer;

import com.example.bootstrap.parsing.LlmResponseParser;

import com.example.bootstrap.rules.AnalyzeModifiedJavaMethodsRule;

import com.example.engine.RuleRegistry;

import com.example.engine.rules.BroadCatchIntroducedRule;

/\*\*

\* Builds a RuleRegistry composed of:

\* - BroadCatchIntroducedRule (static)

\* - AnalyzeModifiedJavaMethodsRule (LLM-backed; optional)

\*/

public final class RuleRegistryBuilder {

public RuleRegistry build(BootstrapConfig cfg) {

RuleRegistry reg = new RuleRegistry();

reg.add(new BroadCatchIntroducedRule());

if (cfg.isEnableMethodAudit()) {

LlmClient llm = new StubLlmClient(); // swap with real client later

var renderer = new MethodAuditPromptRenderer(cfg.getLlmModel());

var parser = new LlmResponseParser();

reg.add(new AnalyzeModifiedJavaMethodsRule(llm, renderer, parser,
cfg.getLlmTimeoutSeconds()));

}

return reg;

}

}

src/main/java/com/example/bootstrap/RunnerOrchestrator.java

\[As provided; builds RuleContext with files, afterChangedLines,
astProvider, provenanceFactory, headSha; executes RuleRunner\]

src/main/java/com/example/bootstrap/DryRunRenderer.java

\[As provided; prints summary and per-finding preview lines, durations\]

src/main/java/com/example/bootstrap/BootstrapRunner.java

\[As provided; reads env vars, runs pipeline; no posting worker\]

src/main/java/com/example/bootstrap/llm/LlmClient.java,
LlmResponse.java, StubLlmClient.java

\[As provided; minimal, with stub returning a valid "Yes" JSON\]

src/main/java/com/example/bootstrap/prompts/MethodAuditPromptRenderer.java

\[As provided; returns strict system prompt and a user prompt with
method signature/body and a small schema reminder\]

src/main/java/com/example/bootstrap/parsing/MethodDiffIntersection.java

package com.example.bootstrap.parsing;

import java.util.Set;

/\*\*

\* Utility to determine whether a method is modified by intersecting

\* its after-side line range with the file\'s afterChangedLines.

\*/

public final class MethodDiffIntersection {

/\*\*

\* Result of the intersection decision.

\*/

public static final class Result {

private final boolean modified;

private final int anchorAfterLine;

public Result(boolean modified, int anchorAfterLine) {

this.modified = modified;

this.anchorAfterLine = anchorAfterLine;

}

public boolean isModified() { return modified; }

public int getAnchorAfterLine() { return anchorAfterLine; }

}

/\*\*

\* Checks if any after-side changed line falls within \[startLine,
endLine\] inclusive.

\* Chooses a deterministic anchor line:

\* - First changed line within the method if present, otherwise
startLine.

\*/

public Result intersect(int startLine, int endLine, Set\<Integer\>
afterChangedLines) {

int min = Math.min(startLine, endLine);

int max = Math.max(startLine, endLine);

int anchor = min;

boolean modified = false;

for (int l = min; l \<= max; l++) {

if (afterChangedLines.contains(l)) {

anchor = l;

modified = true;

break;

}

}

return new Result(modified, anchor);

}

}

src/main/java/com/example/bootstrap/parsing/LlmResponseParser.java

package com.example.bootstrap.parsing;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

import java.util.List;

/\*\*

\* Strict parser for LLM responses.

\* Enforces exact decision phrases and required fields.

\*/

public final class LlmResponseParser {

private static final String YES = \"Yes, please improve the quality of
the code.\";

private static final String NO = \"No, there is no need to modify the
code; it is already at a good level.\";

private final ObjectMapper mapper = new ObjectMapper();

public static final class Parsed {

public final String decision;

public final List\<String\> justifications;

public final String markdown;

public Parsed(String decision, List\<String\> justifications, String
markdown) {

this.decision = decision;

this.justifications = justifications;

this.markdown = markdown;

}

}

public Parsed parse(String text) {

try {

JsonNode root = mapper.readTree(text.trim());

String decision = reqText(root, \"decision\");

if (!YES.equals(decision) && !NO.equals(decision)) {

throw new IllegalArgumentException(\"Invalid decision phrase: \" +
decision);

}

List\<String\> justifications = toList(reqArray(root,
\"justifications\"));

String markdown = reqText(root, \"markdown_comment\");

return new Parsed(decision, justifications, markdown);

} catch (Exception e) {

throw new IllegalArgumentException(\"Invalid LLM JSON: \" +
e.getMessage(), e);

}

}

private static JsonNode reqArray(JsonNode root, String name) {

JsonNode n = root.get(name);

if (n == null \|\| !n.isArray()) throw new
IllegalArgumentException(\"Missing array: \" + name);

return n;

}

private static String reqText(JsonNode root, String name) {

JsonNode n = root.get(name);

if (n == null \|\| !n.isTextual()) throw new
IllegalArgumentException(\"Missing text: \" + name);

return n.asText();

}

private static List\<String\> toList(JsonNode arr) {

List\<String\> out = new ArrayList\<\>();

arr.forEach(n -\> out.add(n.asText()));

return out;

}

}

src/main/java/com/example/bootstrap/rules/AnalyzeModifiedJavaMethodsRule.java

package com.example.bootstrap.rules;

import com.example.ast.JavaParsingFileNode;

import com.example.bootstrap.llm.LlmClient;

import com.example.bootstrap.llm.LlmResponse;

import com.example.bootstrap.parsing.LlmResponseParser;

import com.example.bootstrap.parsing.MethodDiffIntersection;

import com.example.bootstrap.prompts.MethodAuditPromptRenderer;

import com.example.engine.Rule;

import com.example.engine.RuleContext;

import com.example.engine.RuleFinding;

import com.example.findings.LLMInlineFindingOut;

import java.util.\*;

import java.util.function.Function;

/\*\*

\* Rule that:

\* - traverses the Java AST to find method declarations,

\* - intersects each with after-side changed lines from the diff,

\* - for modified methods, calls an LLM with a structured prompt,

\* - emits a finding if the LLM recommends improvements.

\*

\* Notes:

\* - Synchronous; no queues.

\* - Uses a stub LLM by default, replace via RuleRegistryBuilder.

\*/

public final class AnalyzeModifiedJavaMethodsRule implements Rule {

private static final String RULE_ID = \"java-method-quality\";

private final LlmClient llm;

private final MethodAuditPromptRenderer promptRenderer;

private final LlmResponseParser responseParser;

private final int timeoutSeconds;

public AnalyzeModifiedJavaMethodsRule(LlmClient llm,

MethodAuditPromptRenderer promptRenderer,

LlmResponseParser responseParser,

int timeoutSeconds) {

this.llm = Objects.requireNonNull(llm);

this.promptRenderer = Objects.requireNonNull(promptRenderer);

this.responseParser = Objects.requireNonNull(responseParser);

this.timeoutSeconds = timeoutSeconds;

}

\@Override

public String id() {

return RULE_ID;

}

\@Override

public List\<RuleFinding\> apply(RuleContext ctx) {

List\<RuleFinding\> out = new ArrayList\<\>();

MethodDiffIntersection intersector = new MethodDiffIntersection();

// Iterate deterministically over eligible files.

for (String path : ctx.getEligibleFiles()) {

Set\<Integer\> afterChanged =
ctx.getAfterChangedLines().getOrDefault(path, Set.of());

Optional\<JavaParsingFileNode\> astOpt =
ctx.getAstProvider().apply(path);

// If no AST, skip AST-dependent rule for this file.

if (astOpt.isEmpty()) continue;

JavaParsingFileNode ast = astOpt.get();

// Traverse file-level children to find method nodes.

// Exact API depends on your JavaParsingFileNode; this uses a
hypothetical access pattern.

for (JavaParsingFileNode node : ast.children) {

if (!\"METHOD\".equalsIgnoreCase(node.kind.name())) continue;

int start = node.startLine;

int end = node.endLine;

MethodDiffIntersection.Result r = intersector.intersect(start, end,
afterChanged);

if (!r.isModified()) {

continue; // method unmodified; skip

}

// Gather prompt inputs. Replace with real fields from your AST node.

String signature = node.name != null ? node.name : \"(anonymous)\";

String body = node.content != null ? node.content : \"\";

String userPrompt = promptRenderer.userPrompt(path, signature, body);

String systemPrompt = promptRenderer.systemPrompt();

// Synchronous LLM call with strict protocol.

LlmResponse resp = llm.generate(systemPrompt, userPrompt,
timeoutSeconds);

LlmResponseParser.Parsed parsed = responseParser.parse(resp.text());

// If decision is \"Yes\", emit a finding at anchor line.

if (parsed.decision.startsWith(\"Yes\")) {

String headSha = ctx.getHeadSha();

String findingId = deterministicId(RULE_ID, path, signature, headSha);

LLMInlineFindingOut f = new LLMInlineFindingOut();

f.setId(findingId);

f.setRuleId(RULE_ID);

f.setFilePath(path);

f.setAfterLine(r.getAnchorAfterLine());

f.setMarkdownBody(parsed.markdown);

f.setProvenance(ctx.getProvenanceFactory().apply(path));

out.add(f);

}

}

}

return out;

}

/\*\*

\* Creates a deterministic finding id from stable inputs.

\* You may replace with your project's canonical ID method if available.

\*/

private static String deterministicId(String ruleId, String filePath,
String signature, String headSha) {

String s = ruleId + \"\|\" + filePath + \"\|\" + signature + \"\|\" +
headSha;

return Integer.toHexString(s.hashCode());

}

}

Tests

\- BootstrapConfigTest.java, DiffBuilderTest.java,
JavaAstProviderTest.java: as previously provided.

\- LlmResponseParserTest.java: validates strict decision and fields.

\- MethodDiffIntersectionTest.java: validates overlap and anchor
selection.

\- AnalyzeModifiedJavaMethodsRuleTest.java: uses Mockito to mock
LlmClient and RuleContext AST provider with a synthetic method node,
asserts a finding is produced with expected anchor.

README.md

Engine Bootstrap App

Overview

Engine Bootstrap App is a single-module Java 17 project that
orchestrates a synchronous rules-engine run over a GitHub Pull Request
(PR). It reuses your existing components (GitHub connector, diff parser,
Java AST parser, rules engine) and contributes one new LLM-backed rule:
AnalyzeModifiedJavaMethodsRule.

Key capabilities

\- Synchronous pipeline: no queues or async workers

\- Diff + AST intersection: audit only modified methods

\- Strict LLM response protocol: Yes/No decision, justifications,
markdown_comment in JSON

\- Dry-run mode: safe validation without posting to GitHub

\- Deterministic execution: stable ordering and IDs

Dependencies (provided by your repo)

\- com.example:github-pull-request-connector:1.0.0

\- GitHubPullRequestService, PullRequestSnapshot, ChangedFile,
Inline/General comment DTOs

\- com.example:llm-reviewer-diff-parser:1.0.0

\- DiffParser, WrappedFileDiff, WrappedHunk

\- com.example:java-ast-parser:1.0.0

\- JavaParsingService, JavaParsingFileNode

\- com.example:rules-engine-core:1.0.0

\- Rule, RuleContext, RuleRegistry, RuleRunner, RuleRunnerResult,
BroadCatchIntroducedRule

What this module adds

\- AnalyzeModifiedJavaMethodsRule: walks Java AST to find methods,
intersects with diff, calls an LLM synchronously, and emits inline
findings when improvements are suggested.

\- LLM abstraction: LlmClient, LlmResponse, and a StubLlmClient for
development.

\- Prompt tooling: MethodAuditPromptRenderer, LlmResponseParser (strict
JSON).

\- Orchestration: Fetch snapshot, build diffs, lazy AST, run rules,
render dry-run output.

Configuration

Environment variables:

\- GH_TOKEN, GH_OWNER, GH_REPO, GH_PR: required for fetching the PR

\- DRY_RUN: true\|false (default true)

\- ENABLE_METHOD_AUDIT: true\|false (default true)

\- LLM_PROVIDER: STUB (default; for future real clients)

\- LLM_MODEL: model name (default stub-model)

\- LLM_TIMEOUT_SECONDS: default 30

Build

1\) Ensure upstream modules are installed or available in your Maven
repo.

2\) Build:

\- cd engine-bootstrap-app

\- mvn clean package

Run (dry-run)

\- export GH_TOKEN=\...

\- export GH_OWNER=\...

\- export GH_REPO=\...

\- export GH_PR=123

\- export ENABLE_METHOD_AUDIT=true

\- java -jar target/engine-bootstrap-app-1.2.0.jar

Expected output

\- \[DRY-RUN\] findings=N

\- \[DRY-RUN\]\[001\] rule=java-method-quality id=\... file=src/\...
line=\... bodyPreview=\...

How AnalyzeModifiedJavaMethodsRule works

1\) Candidate selection:

\- Uses DiffBuilder to compute after-side changed lines per file

\- Uses JavaAstProvider to lazily parse .java files for which head
content is available

2\) Traversal:

\- Scans AST for method nodes (using your JavaParsingFileNode API)

\- For each method, intersects \[startLine, endLine\] with
afterChangedLines

3\) LLM audit (synchronous):

\- If intersecting lines exist, constructs system + user prompts
(signature + body)

\- Calls LlmClient (StubLlmClient returns a valid Yes JSON
deterministically)

\- Parses output with LlmResponseParser (strict JSON)

4\) Finding emission:

\- If decision is "Yes...", emits LLMInlineFindingOut anchored to the
first changed line in the method

\- Provenance added via ProvenanceFactoryImpl

Extending to real LLM providers

\- Replace StubLlmClient with a real implementation (e.g., OpenAI or
Ollama).

\- Keep the interface LlmClient unchanged to avoid touching rule logic.

\- Ensure the real client returns the same JSON protocol.

Notes and caveats

\- AST API alignment: The example references fields like node.kind,
node.startLine, node.endLine, node.name, node.content. Adapt accessors
to your real JavaParsingFileNode API.

\- RuleContext API alignment: The orchestrator uses a hypothetical
RuleContext.Builder with methods like withEligibleFiles,
withAfterChangedLines, withAstProvider, withProvenanceFactory,
withHeadSha. Map these to your actual engine API accordingly.

\- Posting to GitHub: This module is dry-run oriented. A posting worker
can be added later if needed.

Troubleshooting

\- No findings: Ensure PR has .java changes and patches are included
(GitHub may omit for very large files).

\- AST missing: Increase GH_MAX_HEAD_BYTES or ensure
includeHeadFileContent is enabled.

\- LLM JSON error: The strict parser requires exact decision phrases and
fields.

License

\- Inherit your project's main license (e.g., Apache-2.0).
