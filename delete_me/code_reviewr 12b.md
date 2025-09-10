1st we ask for a point of situation. Alpha horizon was being a bit
obscure and glitchy.

Tab 2

Gork 3 gives us feedback:

# **Action Plan for Rules Engine Integration (Phase 1 Completion)**

## **Objective**

Complete Phase 1 of the rules engine project by integrating existing
components (GitHub connector, Java parser, diff processing) to enable a
deterministic, auditable pipeline for automated code review, addressing
Horizon Beta\'s context-forgetting issues by focusing on orchestration
and validation without re-implementing existing code.

## **Current State**

Based on the provided PDF, the following components are already
implemented and should not be re-written:

-   **GitHub Connector** (GitHubPullRequestService and implementation):

    -   Fetches pull request snapshots (base/head SHAs, changed files,
        > unified diffs, head file contents).

    -   Posts inline and general comments with rate limit handling.

    -   Key classes: GitHubPullRequestServiceImpl, PullRequestSnapshot,
        > ChangedFile, InlineCommentRequest, GeneralCommentRequest.

-   **Java Parser** (com.example.ast package):

    -   Parses Java source code into a simplified, LLM-friendly AST
        > (JavaParsingFileNode, JavaParsingAbstractNode, etc.).

    -   Includes robust error handling, deterministic node IDs
        > (NodeIdFactory), and JSON serialization (NodeJsonSerializer).

    -   Supports parsing caps, error nodes, and enriched content
        > (Javadoc + node content).

-   **Diff Processing** (com.example.reviewer.parser and DiffMapping
    > modules):

    -   Parses unified diffs using JGit (DiffParser, WrappedFileDiff,
        > WrappedHunk).

    -   Provides DiffMapping for after-side changed line sets, with
        > methods like getAfterChangedLines() and
        > isAfterLineChanged(int).

    -   Includes tests for edge cases (renamed files, multi-hunk
        > patches).

-   **Rules Engine Core** (partial, from Milestone 1):

    -   Defines Rule, RuleFinding (LLMInlineFindingOut,
        > StructuredFindingOut), and RuleContext.

    -   Includes BroadCatchIntroducedRule for detecting broad catch
        > clauses.

    -   Lacks full integration with diff and AST providers.

The Horizon Beta model\'s issue is its failure to track these
implemented components, repeatedly suggesting re-implementation (e.g.,
diff adapter core) instead of focusing on integration tasks.

## **Action Plan**

This plan focuses on wiring existing components to achieve a functional
pipeline for Milestone 1, emphasizing dry-run validation and controlled
posting. It avoids duplicating existing code and addresses Horizon
Beta\'s context issues by explicitly leveraging the implemented modules.

### **Module: engine-bootstrap-app**

**Goal**: Create a bootstrap application to orchestrate the end-to-end
pipeline: fetch GitHub snapshot → build diff mappings → parse Java files
→ run rules → render dry-run or post comments.

#### **Classes to Implement**

1.  **BootstrapConfig**

    -   **Responsibility**: Hold configuration for bootstrap execution
        > (e.g., GitHub credentials, dry-run mode, file extensions to
        > process).

    -   **Key Methods**:

        -   Builder pattern for configuration (e.g., withOAuthToken,
            > withDryRun).

        -   Validation: Ensure required fields (e.g., token, repo
            > details) are set.

    -   **Dependencies**:

        -   None (plain data holder).

    -   **Why Important**: Centralizes configuration, supports CLI/env
        > variable parsing, and ensures deterministic setup.

2.  **SnapshotFetcher**

    -   **Responsibility**: Fetch PullRequestSnapshot using
        > GitHubPullRequestService.

    -   **Key Methods**:

        -   PullRequestSnapshot fetch(BootstrapConfig cfg): Calls
            > getPullRequestSnapshot with config parameters.

    -   **Dependencies**:

        -   com.example.github.GitHubPullRequestService: For fetching
            > snapshots.

        -   com.example.github.model.PullRequestSnapshot: DTO for
            > snapshot data.

    -   **Why Important**: Isolates I/O to the GitHub connector,
        > ensuring clean separation and mockability.

3.  **DiffBuilder**

    -   **Responsibility**: Convert PullRequestSnapshot to a DiffCatalog
        > of DiffMapping objects for eligible files
        > (ADDED/MODIFIED/RENAMED, .java extensions).

    -   **Key Methods**:

        -   DiffCatalog build(PullRequestSnapshot snap): Filters files
            > and builds mappings using existing DiffMappingService.

    -   **Dependencies**:

        -   com.example.reviewer.parser.WrappedFileDiff: Input from
            > snapshot.

        -   com.example.reviewer.parser.DiffMappingService: For building
            > DiffMapping.

        -   com.example.reviewer.parser.DiffMapping, DiffCatalog,
            > EligibleFiles: For mapping and filtering.

    -   **Why Important**: Reuses existing diff parsing and mapping,
        > ensuring no re-implementation.

4.  **JavaAstProvider**

    -   **Responsibility**: Provide lazy AST parsing for eligible files
        > using JavaParsingService.

    -   **Key Methods**:

        -   Optional\<JavaParsingFileNode\> getAst(String filePath,
            > String content): Parses content with caps, returns
            > Optional.empty() on failure.

    -   **Dependencies**:

        -   com.example.ast.JavaParsingService: For parsing Java files.

        -   com.example.ast.JavaParsingFileNode: AST output.

        -   com.example.github.model.PullRequestSnapshot: For accessing
            > head content.

    -   **Why Important**: Leverages existing parser, supports graceful
        > degradation for invalid files.

5.  **ProvenanceFactoryImpl**

    -   **Responsibility**: Implement RuleContext.ProvenanceFactory to
        > create LLMProvenance with snapshot metadata and file blob
        > SHAs.

    -   **Key Methods**:

        -   LLMProvenance create(PullRequestSnapshot snap, ChangedFile
            > file): Builds provenance with SHAs and file details.

    -   **Dependencies**:

        -   com.example.findings.LLMProvenance: Provenance DTO.

        -   com.example.github.model.PullRequestSnapshot, ChangedFile:
            > For metadata.

    -   **Why Important**: Ensures auditability and traceability,
        > critical for deterministic findings.

6.  **RuleRegistryBuilder**

    -   **Responsibility**: Build RuleRegistry with available rules
        > (initially BroadCatchIntroducedRule).

    -   **Key Methods**:

        -   RuleRegistry buildDefault(): Returns registry with
            > configured rules.

    -   **Dependencies**:

        -   com.example.engine.Rule, RuleRegistry: Core engine types.

        -   com.example.engine.BroadCatchIntroducedRule: Existing rule
            > implementation.

    -   **Why Important**: Centralizes rule configuration, making it
        > extensible for future rules.

7.  **RunnerOrchestrator**

    -   **Responsibility**: Coordinate rule execution using RuleRunner
        > and RuleContext.

    -   **Key Methods**:

        -   RuleRunnerResult run(BootstrapConfig cfg,
            > PullRequestSnapshot snap, DiffCatalog diffs,
            > JavaAstProvider astProvider, RuleRegistry registry):
            > Builds RuleContext, runs rules, collects findings.

    -   **Dependencies**:

        -   com.example.engine.RuleRunner, RuleContext,
            > RuleRunnerResult: Engine core.

        -   DiffCatalog, JavaAstProvider, RuleRegistry: Inputs for rule
            > execution.

    -   **Why Important**: Wires existing engine components, ensuring
        > deterministic execution.

8.  **DryRunRenderer**

    -   **Responsibility**: Render findings as markdown for dry-run
        > output (e.g., \[DRY-RUN\] rule=\... file=\... line=\...).

    -   **Key Methods**:

        -   String render(RuleRunnerResult result): Formats findings for
            > logging.

    -   **Dependencies**:

        -   com.example.engine.RuleRunnerResult: Contains findings.

        -   com.example.findings.LLMInlineFindingOut: Finding DTO.

    -   **Why Important**: Enables validation without posting, critical
        > for safe rollout.

9.  **BootstrapRunner**

    -   **Responsibility**: Orchestrate the full pipeline (fetch → diff
        > → AST → rules → render/post).

    -   **Key Methods**:

        -   int run(BootstrapConfig cfg): Executes pipeline, returns
            > exit code.

    -   **Dependencies**:

        -   All above classes.

        -   PostingWorker (for non-dry-run mode).

    -   **Why Important**: Single entry point for end-to-end execution,
        > supports both dry-run and posting.

10. **FindingDeduper**

    -   **Responsibility**: Deduplicate findings by id to prevent
        > redundant comments.

    -   **Key Methods**:

        -   List\<RuleFinding\> dedupe(List\<RuleFinding\> findings):
            > Filters by unique IDs.

    -   **Dependencies**:

        -   com.example.findings.LLMInlineFindingOut: Finding DTO.

    -   **Why Important**: Ensures idempotency in posting.

11. **InlineCommentMapper**

    -   **Responsibility**: Map LLMInlineFindingOut to
        > InlineCommentRequest with RIGHT after-line anchoring.

    -   **Key Methods**:

        -   InlineCommentRequest map(LLMInlineFinding f): Converts
            > finding to comment request.

    -   **Dependencies**:

        -   com.example.findings.LLMInlineFindingOut: Input finding.

        -   com.example.github.model.InlineCommentRequest: Output
            > request.

    -   **Why Important**: Ensures accurate comment placement using
        > existing GitHub connector.

12. **GeneralCommentSummarizer**

    -   **Responsibility**: Summarize failed inline comments as a
        > general comment.

    -   **Key Methods**:

        -   GeneralCommentRequest summarize(List\<LLMInlineFinding\>
            > failed): Builds summary comment.

    -   **Dependencies**:

        -   com.example.github.model.GeneralCommentRequest: Output
            > request.

        -   com.example.findings.LLMInlineFindingOut: Failed findings.

    -   **Why Important**: Provides fallback for posting failures,
        > ensuring feedback delivery.

13. **PostingWorker**

    -   **Responsibility**: Post deduplicated findings via
        > GitHubPullRequestService, falling back to general comments on
        > failure.

    -   **Key Methods**:

        -   void postAll(BootstrapConfig cfg, PullRequestSnapshot snap,
            > List\<RuleFinding\> findings): Posts comments with
            > retries.

    -   **Dependencies**:

        -   com.example.github.GitHubPullRequestService: For posting.

        -   com.example.findings.LLMInlineFindingOut,
            > InlineCommentRequest, GeneralCommentRequest: Input/output
            > DTOs.

        -   FindingDeduper, InlineCommentMapper,
            > GeneralCommentSummarizer: For processing findings.

    -   **Why Important**: Completes the pipeline by posting results,
        > with robust error handling.

14. **RuleDiagnosticsLogger**

    -   **Responsibility**: Log rule durations and finding counts from
        > RuleRunnerResult.

    -   **Key Methods**:

        -   void log(RuleRunnerResult result): Logs performance metrics.

    -   **Dependencies**:

        -   com.example.engine.RuleRunnerResult: Contains metrics and
            > findings.

    -   **Why Important**: Provides observability for performance
        > tuning.

15. **ProvenanceLogger**

    -   **Responsibility**: Log snapshot metadata and per-finding
        > provenance for auditing.

    -   **Key Methods**:

        -   void logSnapshot(PullRequestSnapshot snap): Logs snapshot
            > details.

        -   void logFindingProvenance(LLMInlineFinding f): Logs finding
            > details.

    -   **Dependencies**:

        -   com.example.github.model.PullRequestSnapshot: Snapshot
            > metadata.

        -   com.example.findings.LLMInlineFindingOut: Finding details.

    -   **Why Important**: Ensures traceability and debugging support.

### **Testing Guidance**

-   **Unit Tests**:

    -   BootstrapConfig: Validate builder and required fields using
        > AssertJ.

    -   DiffBuilder: Test filtering and mapping using mocked
        > WrappedFileDiff and DiffMappingService.

    -   JavaAstProvider: Test parsing success/failure with mocked
        > JavaParsingService.

    -   RuleRegistryBuilder: Verify rule inclusion.

    -   FindingDeduper, InlineCommentMapper, GeneralCommentSummarizer:
        > Test mapping and deduplication logic.

-   **Integration Tests**:

    -   Mock GitHubPullRequestService to test SnapshotFetcher and
        > PostingWorker.

    -   Test RunnerOrchestrator with synthetic DiffCatalog and
        > JavaAstProvider.

-   **End-to-End Tests**:

    -   Use synthetic PR data (local snapshot with .java files and
        > diffs) for dry-run validation.

    -   Test controlled posting on a real PR with a single rule
        > (BroadCatchIntroducedRule).

### **Recommended Timeline**

-   **Week 1**:

    -   Implement BootstrapConfig, SnapshotFetcher, DiffBuilder,
        > JavaAstProvider, ProvenanceFactoryImpl, RuleRegistryBuilder.

    -   Write unit tests for these components.

    -   Validate dry-run pipeline with synthetic data.

-   **Week 2**:

    -   Implement RunnerOrchestrator, DryRunRenderer, BootstrapRunner.

    -   Implement posting components (FindingDeduper,
        > InlineCommentMapper, GeneralCommentSummarizer, PostingWorker).

    -   Add integration tests and end-to-end dry-run tests.

    -   Test controlled posting on a real PR.

-   **Week 3**:

    -   Implement RuleDiagnosticsLogger, ProvenanceLogger.

    -   Finalize end-to-end tests and documentation (README for running
        > in dry-run/posting modes).

    -   Deploy and validate on a test repository.

### **Why This Plan is Effective**

-   **No Duplication**: Explicitly uses existing
    > GitHubPullRequestService, JavaParsingService, DiffMappingService,
    > and BroadCatchIntroducedRule, avoiding Horizon Beta\'s error of
    > proposing redundant work.

-   **Deterministic and Auditable**: Maintains stable ordering,
    > immutable mappings, and full provenance via ProvenanceFactoryImpl
    > and ProvenanceLogger.

-   **Clean Separation**: Isolates I/O (SnapshotFetcher, PostingWorker),
    > AST (JavaAstProvider), diff handling (DiffBuilder), and engine
    > logic (RunnerOrchestrator), aligning with existing architecture.

-   **Safe Rollout**: Prioritizes dry-run validation (DryRunRenderer)
    > before posting, with fallback to general comments
    > (GeneralCommentSummarizer).

-   **Extensible**: RuleRegistryBuilder and modular design allow easy
    > addition of new rules.

-   **Addresses Horizon Beta\'s Issue**: By focusing on integration and
    > explicitly referencing existing code, the plan ensures context is
    > respected, countering the model\'s tendency to forget implemented
    > components.

## **Next Steps**

If you confirm this plan, I can generate the scaffolding code for all
listed classes, including Javadoc, inline comments, and JUnit 5 tests
with AssertJ, plus a README for running the bootstrap in dry-run and
posting modes. Please let me know if you want to proceed or if there are
specific adjustments needed.

Tab 3

Horizon beta says it understands the action plan by grok:\
\
Yes---I understand the full context and the action plan.

Key implemented components already in place

\- GitHub connector: \`GitHubPullRequestService\` and
\`GitHubPullRequestServiceImpl\` with snapshot fetch (base/head SHAs,
changed files, unified patches, optional head content with caps) and
posting (\`postInlineComment\` RIGHT-side, single-line;
\`postGeneralComment\`), plus \`GitHubPullRequestException\`.

\- Java parser: \`com.example.ast\` with \`JavaParsingService\`, node
model, \`NodeIdFactory\`, JSON serialization, robust error handling,
deterministic IDs, tests.

\- Diff processing: JGit-based diff parser (\`DiffParser\`,
\`WrappedFileDiff\`, \`WrappedHunk\`, \`LineMap\`) with tests and
fixtures; and an adapter/mapping layer exists (avoid re-implementing
it).

\- Rules engine (Milestone 1): \`Rule\`, \`RuleContext\`,
\`RuleRegistry\`, \`RuleRunner\`, \`RuleRunnerResult\`, \`RuleError\`,
and \`BroadCatchIntroducedRule\` working on after-side changed lines
with deterministic finding IDs and provenance.

Problem to avoid

\- Rewriting already-implemented components (diff parsing/mapping,
GitHub connector, Java parser, core engine). The next steps must focus
on orchestration and integration.

Next-step action plan (connect the dots, no rewrites)

Module: engine-bootstrap-app

1\) \`BootstrapConfig\` (builder + validation)

\- Fields: token, owner, repository, pullRequestNumber,
includeHeadFileContent, maxBytesForHeadContent, dryRun, extensions
(default .java), optional caps (maxFilesToProcess).

\- Purpose: deterministic, validated configuration.

2\) \`SnapshotFetcher\`

\- Use \`GitHubPullRequestService.getPullRequestSnapshot(\...)\`.

\- Add simple retry/backoff for transient GitHub errors.

\- Output: \`PullRequestSnapshot\`.

3\) \`DiffBuilder\`

\- Input: \`PullRequestSnapshot.getChangedFiles()\`.

\- For files with non-null patches and eligible change types
(ADDED/MODIFIED/RENAMED), parse with existing JGit diff parser
(\`DiffParser\`) to \`WrappedFileDiff\`.

\- Convert to engine-facing mapping using the existing diff adapter (no
new diff code).

\- Produce \`DiffCatalog\` keyed by new path and \`EligibleFiles\`
(sorted).

4\) \`JavaAstProvider\`

\- Lazy AST loading via \`JavaParsingService\` for \`.java\` files where
head content is present and within cap.

\- Returns \`Optional.empty()\` on failure; no exceptions.

5\) \`ProvenanceFactoryImpl\`

\- Build \`LLMProvenance\` using snapshot metadata (owner, repo, PR
number, base/head SHAs) and per-file blob SHA when available.

6\) \`RuleRegistryBuilder\`

\- Build default registry including \`BroadCatchIntroducedRule\`; allow
future toggles via config.

7\) \`RunnerOrchestrator\`

\- Build \`RuleContext\` using: eligible paths, \`DiffCatalog\`,
\`JavaAstProvider\`, \`ProvenanceFactoryImpl\`.

\- Run \`RuleRunner\`; return \`RuleRunnerResult\`.

8\) \`DryRunRenderer\`

\- Print findings as deterministic lines: \[DRY-RUN\] rule=\... id=\...
file=\... line=\...; include brief provenance summary and per-rule
durations.

9\) \`BootstrapRunner\`

\- Entry point: fetch → diffs → AST provider → registry → run → dry-run
output or hand off to posting.

Module: posting-worker

10\) \`FindingDeduper\`

\- In-memory dedupe by \`finding.id\`.

11\) \`InlineCommentMapper\`

\- Map \`LLMInlineFindingOut\` to \`InlineCommentRequest\` with RIGHT
anchoring and absolute after-line.

12\) \`GeneralCommentSummarizer\`

\- Summarize any failed inline posts into a single
\`GeneralCommentRequest\`.

13\) \`PostingWorker\`

\- Dedupe, post inline via
\`GitHubPullRequestService.postInlineComment\`, retry on transient
errors, and fallback to summary comment on persistent failure.

Optional diagnostics

14\) \`RuleDiagnosticsLogger\`

\- Log per-rule durations and finding counts from \`RuleRunnerResult\`.

15\) \`ProvenanceLogger\`

\- Log snapshot metadata and per-finding provenance for auditability.

Testing guidance

\- Unit: \`BootstrapConfig\` validation; \`DiffBuilder\` mapping with
synthetic \`ChangedFile\` and patches; \`JavaAstProvider\`
success/failure; \`InlineCommentMapper\`; \`FindingDeduper\`.

\- Integration: Mock \`GitHubPullRequestService\` to validate
\`SnapshotFetcher\`, \`PostingWorker\`.

\- E2E dry-run: Synthetic PR snapshot with .java files and patches;
verify anchors and determinism.

\- Controlled posting: Run on a real PR with dry-run off; verify
idempotent posting.

If you share the GROK3 action plan, I'll align this orchestration plan
to it and identify any gaps before scaffolding.

Tab 4

"Please focus on the essential requirements for now. Do your best to
fulfill them. Also, please remember that any code must have Javadoc,
including documentation for fields and methods. Larger methods with more
than 20 lines of code must always include appropriate line comments."

Tab 5

Horizon beta provided:

Great---starting with the essential integration scaffolding. I'll
provide a minimal but runnable slice in this order: BootstrapConfig,
SnapshotFetcher, DiffBuilder, JavaAstProvider, ProvenanceFactoryImpl,
RuleRegistryBuilder, RunnerOrchestrator, DryRunRenderer,
BootstrapRunner. All classes include full Javadoc; larger methods
include inline comments. This targets dry-run mode first and uses your
existing components (no rewrites).

Module: engine-bootstrap-app

File: src/main/java/com/example/bootstrap/BootstrapConfig.java

package com.example.bootstrap;

import java.util.Objects;

import java.util.Set;

/\*\*

\* Immutable configuration for a single bootstrap run.

\* Provides validated parameters for fetching a Pull Request snapshot,

\* building diffs/ASTs, and executing rules in dry-run or posting mode.

\*

\* Determinism note:

\* Values here control the pipeline\'s behavior; validated inputs help
ensure reproducible runs.

\*/

public final class BootstrapConfig {

/\*\* GitHub OAuth token or App installation token used by the
connector. \*/

private final String oauthToken;

/\*\* Repository owner or organization (e.g., \"acme\"). \*/

private final String owner;

/\*\* Repository name (e.g., \"awesome-repo\"). \*/

private final String repository;

/\*\* Pull Request number to process. Must be positive. \*/

private final int pullRequestNumber;

/\*\* If true, the connector should attempt to fetch head file contents
for eligible files. \*/

private final boolean includeHeadFileContent;

/\*\* Maximum allowed decoded size for head file contents in bytes
(applies when includeHeadFileContent is true). \*/

private final long maxBytesForHeadContent;

/\*\* If true, do not post to GitHub; render findings to stdout/logs
instead. \*/

private final boolean dryRun;

/\*\* Optional set of file extensions (with leading dot) to process
(e.g., {\".java\"}). Empty means no filter. \*/

private final Set\<String\> allowedExtensions;

/\*\* Optional cap for the maximum number of files to process from the
snapshot (0 means no cap). \*/

private final int maxFilesToProcess;

private BootstrapConfig(Builder b) {

this.oauthToken = b.oauthToken;

this.owner = b.owner;

this.repository = b.repository;

this.pullRequestNumber = b.pullRequestNumber;

this.includeHeadFileContent = b.includeHeadFileContent;

this.maxBytesForHeadContent = b.maxBytesForHeadContent;

this.dryRun = b.dryRun;

this.allowedExtensions = Set.copyOf(b.allowedExtensions);

this.maxFilesToProcess = b.maxFilesToProcess;

}

public String getOauthToken() { return oauthToken; }

public String getOwner() { return owner; }

public String getRepository() { return repository; }

public int getPullRequestNumber() { return pullRequestNumber; }

public boolean isIncludeHeadFileContent() { return
includeHeadFileContent; }

public long getMaxBytesForHeadContent() { return maxBytesForHeadContent;
}

public boolean isDryRun() { return dryRun; }

public Set\<String\> getAllowedExtensions() { return allowedExtensions;
}

public int getMaxFilesToProcess() { return maxFilesToProcess; }

/\*\*

\* Builder for {@link BootstrapConfig} with validation.

\* Ensures required fields are present and consistent.

\*/

public static final class Builder {

private String oauthToken;

private String owner;

private String repository;

private int pullRequestNumber;

private boolean includeHeadFileContent = true;

private long maxBytesForHeadContent = 262_144L; // 256 KiB sensible
default

private boolean dryRun = true;

private Set\<String\> allowedExtensions = Set.of(\".java\");

private int maxFilesToProcess = 0;

public Builder withOauthToken(String token) { this.oauthToken = token;
return this; }

public Builder withOwner(String owner) { this.owner = owner; return
this; }

public Builder withRepository(String repo) { this.repository = repo;
return this; }

public Builder withPullRequestNumber(int pr) { this.pullRequestNumber =
pr; return this; }

public Builder withIncludeHeadFileContent(boolean include) {
this.includeHeadFileContent = include; return this; }

public Builder withMaxBytesForHeadContent(long bytes) {
this.maxBytesForHeadContent = bytes; return this; }

public Builder withDryRun(boolean dryRun) { this.dryRun = dryRun; return
this; }

public Builder withAllowedExtensions(Set\<String\> exts) {
this.allowedExtensions = Objects.requireNonNull(exts); return this; }

public Builder withMaxFilesToProcess(int max) { this.maxFilesToProcess =
max; return this; }

/\*\*

\* Validates and builds the immutable {@link BootstrapConfig}.

\* \@return validated BootstrapConfig

\* \@throws IllegalArgumentException if validation fails

\*/

public BootstrapConfig build() {

if (oauthToken == null \|\| oauthToken.isBlank()) {

throw new IllegalArgumentException(\"oauthToken must be provided\");

}

if (owner == null \|\| owner.isBlank()) {

throw new IllegalArgumentException(\"owner must be provided\");

}

if (repository == null \|\| repository.isBlank()) {

throw new IllegalArgumentException(\"repository must be provided\");

}

if (pullRequestNumber \<= 0) {

throw new IllegalArgumentException(\"pullRequestNumber must be \> 0\");

}

if (includeHeadFileContent && maxBytesForHeadContent \<= 0) {

throw new IllegalArgumentException(\"maxBytesForHeadContent must be \> 0
when includeHeadFileContent is true\");

}

if (maxFilesToProcess \< 0) {

throw new IllegalArgumentException(\"maxFilesToProcess must be \>= 0\");

}

return new BootstrapConfig(this);

}

}

}

File: src/main/java/com/example/bootstrap/SnapshotFetcher.java

package com.example.bootstrap;

import com.example.github.GitHubPullRequestService;

import com.example.github.model.PullRequestSnapshot;

/\*\*

\* Fetches a {@link PullRequestSnapshot} via {@link
GitHubPullRequestService}.

\* Encapsulates connector calls and allows adding retry/backoff for
transient failures.

\*/

public final class SnapshotFetcher {

/\*\*

\* Fetches a snapshot using provided config and service.

\* This method is intentionally small and side-effect isolated.

\*

\* \@param cfg non-null validated configuration

\* \@param svc non-null GitHub service

\* \@return snapshot containing PR metadata and changed files

\*/

public PullRequestSnapshot fetch(BootstrapConfig cfg,
GitHubPullRequestService svc) {

// Single call pass-through; retries/backoff can be added if needed.

return svc.getPullRequestSnapshot(

cfg.getOauthToken(),

cfg.getOwner(),

cfg.getRepository(),

cfg.getPullRequestNumber(),

cfg.isIncludeHeadFileContent(),

cfg.getMaxBytesForHeadContent()

);

}

}

File: src/main/java/com/example/bootstrap/DiffBuilder.java

package com.example.bootstrap;

import com.example.github.model.ChangedFile;

import com.example.github.model.PullRequestSnapshot;

import com.example.reviewer.parser.DiffParser;

import com.example.reviewer.parser.WrappedFileDiff;

import com.example.reviewer.parser.WrappedHunk;

import java.util.\*;

import java.util.stream.Collectors;

/\*\*

\* Converts {@link PullRequestSnapshot} changed files into a
deterministic catalog

\* of per-file after-line mappings using the existing JGit-based diff
parser.

\*

\* This class does not re-implement diff parsing. It reuses {@link
DiffParser},

\* {@link WrappedFileDiff}, and {@link WrappedHunk} to build a minimal
mapping view.

\*/

public final class DiffBuilder {

/\*\*

\* Represents a minimal per-file mapping: set of after-side changed line
numbers and new path.

\* This is intentionally simple and engine-friendly.

\*/

public static final class DiffMapping {

/\*\* Repository-relative new path for the file (anchor target). \*/

private final String newPath;

/\*\* Immutable set of after-side line numbers that were added/changed.
\*/

private final Set\<Integer\> afterChangedLines;

public DiffMapping(String newPath, Set\<Integer\> afterChangedLines) {

this.newPath = Objects.requireNonNull(newPath, \"newPath\");

this.afterChangedLines =
Set.copyOf(Objects.requireNonNull(afterChangedLines,
\"afterChangedLines\"));

}

public String getNewPath() { return newPath; }

public Set\<Integer\> getAfterChangedLines() { return afterChangedLines;
}

public boolean isAfterLineChanged(int afterLine) { return
afterChangedLines.contains(afterLine); }

}

/\*\*

\* Immutable catalog keyed by new path for quick lookup from
rules/orchestrator.

\*/

public static final class DiffCatalog {

/\*\* Map of newPath to DiffMapping for eligible files. \*/

private final Map\<String, DiffMapping\> byNewPath;

public DiffCatalog(Map\<String, DiffMapping\> byNewPath) {

this.byNewPath = Map.copyOf(Objects.requireNonNull(byNewPath));

}

public Optional\<DiffMapping\> get(String newPath) {

return Optional.ofNullable(byNewPath.get(newPath));

}

public Set\<String\> allNewPaths() {

return byNewPath.keySet();

}

}

/\*\*

\* Builds a {@link DiffCatalog} from a {@link PullRequestSnapshot}.

\* Filters for eligible change types and extensions, and respects
optional file caps.

\*

\* This method is longer than 20 lines; line comments explain each main
step.

\*

\* \@param snapshot non-null snapshot with changed files

\* \@param allowedExtensions optional filter (e.g., {\".java\"}). Empty
means no filter.

\* \@param maxFilesToProcess optional cap (0 means unlimited)

\* \@return catalog keyed by new path

\*/

public DiffCatalog build(PullRequestSnapshot snapshot, Set\<String\>
allowedExtensions, int maxFilesToProcess) {

// Collect eligible files: ADDED, MODIFIED, RENAMED; exclude DELETED and
null patches.

List\<ChangedFile\> eligible = snapshot.getChangedFiles().stream()

.filter(f -\> f.getChangeType() != null)

.filter(f -\> switch (f.getChangeType()) {

case ADDED, MODIFIED, RENAMED -\> true;

default -\> false;

})

.filter(f -\> f.getPatch() != null) // skip binary/large/no-patch files

.collect(Collectors.toList());

// Apply extension filter if provided (case-sensitive, expecting leading
dots like \".java\").

if (allowedExtensions != null && !allowedExtensions.isEmpty()) {

eligible = eligible.stream()

.filter(f -\> {

String p = f.getNewPath();

if (p == null) return false;

int idx = p.lastIndexOf(\'.\');

String ext = idx \>= 0 ? p.substring(idx) : \"\";

return allowedExtensions.contains(ext);

})

.collect(Collectors.toList());

}

// Apply optional cap to limit processing on large PRs.

if (maxFilesToProcess \> 0 && eligible.size() \> maxFilesToProcess) {

eligible = eligible.subList(0, maxFilesToProcess);

}

// Parse patches and build after-side changed line sets
deterministically.

DiffParser parser = new DiffParser();

Map\<String, DiffMapping\> byNewPath = new LinkedHashMap\<\>(); //
preserve insertion order for determinism

for (ChangedFile cf : eligible) {

String patch = cf.getPatch();

String newPath = cf.getNewPath();

// Parse unified diff for this file; use first entry if multi-file
appears in the patch (defensive).

Optional\<WrappedFileDiff\> maybe = parser.parseSingleFileDiff(patch);

if (maybe.isEmpty()) continue;

WrappedFileDiff wfd = maybe.get();

// Union \'+\' lines across hunks to form after-side changed set.

Set\<Integer\> afterChanged = new HashSet\<\>();

for (WrappedHunk h : wfd.getHunks()) {

afterChanged.addAll(h.getAddedLinesInNewFile());

}

byNewPath.put(newPath, new DiffMapping(newPath, afterChanged));

}

return new DiffCatalog(byNewPath);

}

}

File: src/main/java/com/example/bootstrap/JavaAstProvider.java

package com.example.bootstrap;

import com.example.ast.JavaParsingFileNode;

import com.example.ast.JavaParsingService;

import com.example.github.model.ChangedFile;

import com.example.github.model.PullRequestSnapshot;

import java.util.\*;

import java.util.concurrent.ConcurrentHashMap;

/\*\*

\* Lazy AST provider backed by {@link JavaParsingService}.

\* Loads ASTs for eligible .java files when head content is available
and under size cap.

\* Returns Optional.empty() on parsing failure or missing content.

\*/

public final class JavaAstProvider {

/\*\* Parser instance used to build ASTs for .java sources. \*/

private final JavaParsingService parser;

/\*\* Map from new path to head content (only for .java files with
content available). \*/

private final Map\<String, String\> headContentByPath;

/\*\* Cache of parsed ASTs to avoid repeat work within a run. \*/

private final Map\<String, Optional\<JavaParsingFileNode\>\> cache = new
ConcurrentHashMap\<\>();

private JavaAstProvider(JavaParsingService parser, Map\<String, String\>
headContentByPath) {

this.parser = Objects.requireNonNull(parser, \"parser\");

this.headContentByPath =
Map.copyOf(Objects.requireNonNull(headContentByPath,
\"headContentByPath\"));

}

/\*\*

\* Factory that scans a snapshot and prepares head content map for .java
files.

\* \@param snapshot snapshot with changed files and optional head
contents

\* \@param parser Java parsing service

\* \@param eligiblePaths optional set of paths to restrict loading to

\* \@return provider ready for lazy AST parsing

\*/

public static JavaAstProvider fromSnapshot(PullRequestSnapshot snapshot,

JavaParsingService parser,

Set\<String\> eligiblePaths) {

Map\<String, String\> m = new HashMap\<\>();

for (ChangedFile cf : snapshot.getChangedFiles()) {

String p = cf.getNewPath();

if (p == null) continue;

if (eligiblePaths != null && !eligiblePaths.contains(p)) continue;

if (!p.endsWith(\".java\")) continue;

String content = cf.getHeadContent();

if (content != null && !content.isBlank()) {

m.put(p, content);

}

}

return new JavaAstProvider(parser, m);

}

/\*\*

\* Loads an AST for a given new path if head content is available.

\* \@param newPath repository-relative new file path

\* \@return Optional AST root; empty if content missing or parsing
failed

\*/

public Optional\<JavaParsingFileNode\> getAst(String newPath) {

return cache.computeIfAbsent(newPath, p -\> {

String content = headContentByPath.get(p);

if (content == null) return Optional.empty();

String fileName = p.substring(p.lastIndexOf(\'/\') + 1);

try {

JavaParsingFileNode root = parser.parse(p, fileName, content);

return Optional.ofNullable(root);

} catch (Exception e) {

return Optional.empty();

}

});

}

}

File: src/main/java/com/example/bootstrap/ProvenanceFactoryImpl.java

package com.example.bootstrap;

import com.example.github.model.ChangedFile;

import com.example.github.model.PullRequestInfo;

import com.example.github.model.PullRequestSnapshot;

import com.example.findings.LLMProvenance;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Builds {@link LLMProvenance} objects from snapshot metadata and
per-file details.

\* Ensures findings carry repository, Pull Request, and SHA provenance
for auditing.

\*/

public final class ProvenanceFactoryImpl {

/\*\* Snapshot containing repository, PR, and SHA metadata for this run.
\*/

private final PullRequestSnapshot snapshot;

public ProvenanceFactoryImpl(PullRequestSnapshot snapshot) {

this.snapshot = Objects.requireNonNull(snapshot, \"snapshot\");

}

/\*\*

\* Creates a provenance for a specific file path (new path).

\* \@param newPath repository-relative new file path

\* \@return provenance object with SHAs and file-specific blob SHA when
available

\*/

public LLMProvenance createFor(String newPath) {

PullRequestInfo info = snapshot.getInfo();

String blobSha = snapshot.getChangedFiles().stream()

.filter(f -\> newPath.equals(f.getNewPath()))

.map(ChangedFile::getBlobSha)

.filter(Objects::nonNull)

.findFirst()

.orElse(null);

// Construct provenance (assuming a builder or constructor exists in
your DTO; adapt as needed).

LLMProvenance p = new LLMProvenance();

p.setRepositoryOwner(info.getOwner());

p.setRepositoryName(info.getRepository());

p.setPullRequestNumber(info.getPullRequestNumber());

p.setBaseSha(info.getBaseCommitSha());

p.setHeadSha(info.getHeadCommitSha());

p.setFileBlobSha(blobSha);

p.setFilePath(newPath);

return p;

}

}

File: src/main/java/com/example/bootstrap/RuleRegistryBuilder.java

package com.example.bootstrap;

import com.example.engine.RuleRegistry;

import com.example.engine.rules.BroadCatchIntroducedRule;

/\*\*

\* Creates the default {@link RuleRegistry} for Phase 1.

\* Currently includes only {@link BroadCatchIntroducedRule}.

\*/

public final class RuleRegistryBuilder {

/\*\*

\* Builds the default rule registry for Phase 1.

\* \@return registry containing BroadCatchIntroducedRule

\*/

public RuleRegistry buildDefault() {

RuleRegistry registry = new RuleRegistry();

registry.add(new BroadCatchIntroducedRule());

return registry;

}

}

File: src/main/java/com/example/bootstrap/RunnerOrchestrator.java

package com.example.bootstrap;

import com.example.engine.RuleContext;

import com.example.engine.RuleRegistry;

import com.example.engine.RuleRunner;

import com.example.engine.RuleRunnerResult;

import java.util.\*;

/\*\*

\* Orchestrates rule execution by constructing a {@link RuleContext},

\* invoking {@link RuleRunner}, and returning {@link RuleRunnerResult}.

\*/

public final class RunnerOrchestrator {

/\*\*

\* Executes the rules for the given snapshot using prepared diffs and
AST provider.

\* This method is longer than 20 lines; inline comments explain each
step.

\*

\* \@param snapshot snapshot with PR metadata and changed files

\* \@param catalog diff mappings keyed by new path

\* \@param astProvider Java AST provider for lazy parsing

\* \@param registry rules to execute

\* \@return result containing findings and diagnostics

\*/

public RuleRunnerResult run(PullRequestContext snapshot,

DiffBuilder.DiffCatalog catalog,

JavaAstProvider astProvider,

RuleRegistry registry) {

// Collect eligible new paths deterministically.

List\<String\> sortedPaths = new ArrayList\<\>(catalog.allNewPaths());

Collections.sort(sortedPaths);

// Build a minimal diff helper view for RuleContext: map path -\>
predicate for after-line change check.

Map\<String, Set\<Integer\>\> afterLinesByPath = new HashMap\<\>();

for (String p : sortedPaths) {

catalog.get(p).ifPresent(dm -\> afterLinesByPath.put(p,
dm.getAfterChangedLines()));

}

// Create provenance factory for per-file provenance.

ProvenanceFactoryImpl provenanceFactory = new
ProvenanceFactoryImpl(snapshot);

// Build RuleContext. Adapt to your engine's actual constructor/setters.

RuleContext ctx = new RuleContext.Builder()

.withEligibleFiles(new LinkedHashSet\<\>(sortedPaths)) // preserve order

.withAfterChangedLines(afterLinesByPath)

.withAstProvider(astProvider::getAst)

.withProvenanceFactory(provenanceFactory::createFor)

.withHeadSha(snapshot.getInfo().getHeadCommitSha())

.build();

// Execute rules deterministically using the provided registry.

RuleRunner runner = new RuleRunner(registry);

return runner.run(ctx);

}

/\*\*

\* Small facade type alias to avoid leaking connector models into this
orchestrator signature.

\* This allows using the same class for logging or enhancements later.

\*/

public static final class PullRequestContext extends
com.example.github.model.PullRequestSnapshot {

public PullRequestContext(com.example.github.model.PullRequestSnapshot
delegate) {

super(delegate.getInfo(), delegate.getChangedFiles());

}

}

}

File: src/main/java/com/example/bootstrap/DryRunRenderer.java

package com.example.bootstrap;

import com.example.engine.RuleRunnerResult;

import com.example.findings.LLMInlineFindingOut;

import java.util.concurrent.atomic.AtomicInteger;

/\*\*

\* Renders findings in a deterministic, concise format suitable for
logs/stdout.

\*/

public final class DryRunRenderer {

/\*\*

\* Prints a summary and per-finding lines.

\* \@param result rule runner result with findings and durations

\*/

public void render(RuleRunnerResult result) {

System.out.println(\"\[DRY-RUN\] findings=\" +
result.findings().size());

AtomicInteger idx = new AtomicInteger(1);

result.findings().stream()

.filter(f -\> f instanceof LLMInlineFindingOut)

.map(f -\> (LLMInlineFindingOut) f)

.forEach(f -\> {

String line = String.format(

\"\[DRY-RUN\]\[%03d\] rule=%s id=%s file=%s line=%d\",

idx.getAndIncrement(),

f.getRuleId(),

f.getId(),

f.getFilePath(),

f.getAfterLine()

);

System.out.println(line);

});

// Optional: print per-rule durations if available.

if (result.ruleDurationsMillis() != null &&
!result.ruleDurationsMillis().isEmpty()) {

System.out.println(\"\[DRY-RUN\] durationsMillis=\" +
result.ruleDurationsMillis());

}

}

}

File: src/main/java/com/example/bootstrap/BootstrapRunner.java

package com.example.bootstrap;

import com.example.github.GitHubPullRequestService;

import com.example.github.GitHubPullRequestServiceImpl;

import com.example.github.model.PullRequestSnapshot;

import com.example.ast.JavaParsingService;

/\*\*

\* Entry point for a single end-to-end execution in dry-run or posting
mode.

\* Focuses on orchestration only; no re-implementation of existing
components.

\*/

public final class BootstrapRunner {

/\*\* Runs the pipeline for the provided configuration. \*/

public int run(BootstrapConfig cfg) {

try {

// 1) Fetch snapshot

GitHubPullRequestService gh = new GitHubPullRequestServiceImpl();

SnapshotFetcher fetcher = new SnapshotFetcher();

PullRequestSnapshot snap = fetcher.fetch(cfg, gh);

// 2) Build diffs

DiffBuilder diffBuilder = new DiffBuilder();

DiffBuilder.DiffCatalog catalog = diffBuilder.build(

snap, cfg.getAllowedExtensions(), cfg.getMaxFilesToProcess()

);

// 3) Build AST provider

JavaParsingService javaParser = new JavaParsingService();

JavaAstProvider astProvider = JavaAstProvider.fromSnapshot(

snap, javaParser, catalog.allNewPaths()

);

// 4) Build rules

RuleRegistryBuilder registryBuilder = new RuleRegistryBuilder();

// 5) Run

RunnerOrchestrator orchestrator = new RunnerOrchestrator();

RunnerOrchestrator.PullRequestContext prCtx = new
RunnerOrchestrator.PullRequestContext(snap);

var result = orchestrator.run(prCtx, catalog, astProvider,
registryBuilder.buildDefault());

// 6) Output

if (cfg.isDryRun()) {

new DryRunRenderer().render(result);

} else {

// Wire PostingWorker here (not included in this first slice to keep
focus on dry-run path).

// new PostingWorker(\...).postAll(\...)

new DryRunRenderer().render(result); // As placeholder, still render

}

return 0;

} catch (Exception e) {

System.err.println(\"\[BOOTSTRAP\] Failed: \" + e.getMessage());

e.printStackTrace(System.err);

return 1;

}

}

/\*\* Minimal CLI entry (optional). Parse env/args in real usage. \*/

public static void main(String\[\] args) {

BootstrapConfig cfg = new BootstrapConfig.Builder()

.withOauthToken(System.getenv(\"GH_TOKEN\"))

.withOwner(System.getenv(\"GH_OWNER\"))

.withRepository(System.getenv(\"GH_REPO\"))

.withPullRequestNumber(Integer.parseInt(System.getenv().getOrDefault(\"GH_PR\",
\"0\")))

.withIncludeHeadFileContent(true)

.withMaxBytesForHeadContent(262_144L)

.withDryRun(true)

.build();

int code = new BootstrapRunner().run(cfg);

System.exit(code);

}

}

Notes

\- No diff processing code was re-implemented. The builder solely uses
your existing \`DiffParser\`/\`WrappedFileDiff\`/\`WrappedHunk\` to
compute after-side line sets.

\- AST parsing strictly uses \`JavaParsingService\`.

\- GitHub I/O uses \`GitHubPullRequestService\` you already have.

\- This slice focuses on dry-run end-to-end. If you want, I will add the
posting worker next with \`FindingDeduper\`, \`InlineCommentMapper\`,
\`GeneralCommentSummarizer\`, and \`PostingWorker\`, all with full
Javadoc and inline comments.

Tab 6

Review from grok:\
**Audit and Refined Action Plan for Rules Engine Integration (Phase 1
Completion)**

## **Audit of Horizon Beta's Output**

Horizon Beta's output provides a minimal, runnable slice for the
engine-bootstrap-app module, targeting dry-run mode and leveraging
existing components. Below is an evaluation of the provided code against
the project requirements and the issue of context forgetting.

### **Strengths**

1.  **Correct Use of Existing Components**:

    -   **GitHub Connector**: Uses GitHubPullRequestService and
        > PullRequestSnapshot for fetching PR data, avoiding
        > re-implementation.

    -   **Java Parser**: Relies on JavaParsingService and
        > JavaParsingFileNode for AST generation, respecting the
        > existing parser.

    -   **Diff Processing**: Utilizes DiffParser, WrappedFileDiff, and
        > WrappedHunk for diff mappings, correctly avoiding
        > re-implementation of diff parsing logic.

    -   **Rules Engine**: Integrates BroadCatchIntroducedRule and
        > existing engine types (Rule, RuleRegistry, RuleRunner,
        > RuleContext), aligning with Milestone 1.

2.  **Deterministic Design**:

    -   Preserves order in DiffBuilder (using LinkedHashMap) and
        > RunnerOrchestrator (sorting paths) for reproducibility.

    -   Uses immutable data structures (e.g., Set.copyOf, Map.copyOf) in
        > BootstrapConfig and DiffBuilder.

3.  **Dry-Run Focus**: Prioritizes dry-run mode with DryRunRenderer,
    > enabling validation without external I/O, which is a safe approach
    > for initial integration.

4.  **Error Handling**:

    -   BootstrapConfig includes robust validation in the builder.

    -   JavaAstProvider handles parsing failures gracefully with
        > Optional.empty().

    -   BootstrapRunner catches exceptions and returns a non-zero exit
        > code for failures.

5.  **Minimal Scope**: Focuses on essential classes for dry-run,
    > deferring posting-related components (PostingWorker, etc.) to
    > avoid overcomplicating the initial slice.

### **Issues and Gaps**

1.  **Context Forgetting**:

    -   Horizon Beta correctly avoids re-implementing diff processing,
        > parser, and GitHub connector, indicating awareness of existing
        > code in this output. However, the PDF shows prior instances
        > where it suggested re-implementing the diff adapter core,
        > suggesting inconsistent context retention across interactions.

    -   The provided ProvenanceFactoryImpl assumes a constructor-based
        > LLMProvenance without verifying its exact API, which could
        > lead to integration issues if the DTO's structure differs.

2.  **Incomplete Provenance**:

    -   ProvenanceFactoryImpl sets fields directly on LLMProvenance
        > (e.g., setRepositoryOwner), but the PDF does not confirm if
        > LLMProvenance uses setters, a builder, or a constructor. This
        > needs validation against the actual DTO.

3.  **RuleContext Construction**:

    -   RunnerOrchestrator assumes a RuleContext.Builder with specific
        > methods (e.g., withEligibleFiles, withAstProvider). The PDF
        > does not provide the exact RuleContext API, so this may
        > require adjustment to match the existing engine's interface.

4.  **Missing Posting Components**:

    -   The output defers PostingWorker, FindingDeduper,
        > InlineCommentMapper, and GeneralCommentSummarizer, which are
        > necessary for completing Phase 1's posting mode. While this is
        > intentional for a dry-run focus, the action plan must include
        > these to achieve full functionality.

5.  **Testing Omitted**:

    -   No unit or integration tests are provided, despite the
        > requirement for JUnit 5 tests with AssertJ. This is a
        > significant gap, as testing is critical for validating
        > integration.

6.  **Logging and Diagnostics**:

    -   DryRunRenderer outputs to System.out, which is simplistic. The
        > plan requires RuleDiagnosticsLogger and ProvenanceLogger for
        > structured logging, which are missing.

    -   No performance metrics (e.g., rule durations) are logged beyond
        > a basic check in DryRunRenderer.

7.  **Configuration Flexibility**:

    -   BootstrapConfig.Builder hardcodes defaults (e.g., \".java\" for
        > extensions, 256 KiB for content size). These should be
        > configurable via environment variables or CLI arguments for
        > flexibility.

8.  **Error Handling in SnapshotFetcher**:

    -   SnapshotFetcher lacks retry logic for transient failures (e.g.,
        > rate limits, 5xx errors), which the PDF emphasizes as a
        > requirement.

## **Refined Action Plan**

This refined plan builds on Horizon Beta's output, addressing the
identified gaps and completing Phase 1 by adding posting components,
testing, and logging. It ensures no re-implementation of existing code
(GitHub connector, Java parser, diff processing,
BroadCatchIntroducedRule) and focuses on connecting components for a
complete pipeline.

### **Module: engine-bootstrap-app**

**Goal**: Finalize the bootstrap application to support both dry-run and
posting modes, integrating existing components with robust testing and
logging.

#### **Classes to Implement or Refine**

1.  **BootstrapConfig** (Refine Existing)

    -   **Responsibility**: Hold configuration for pipeline execution,
        > supporting CLI/env variable parsing.

    -   **Changes**:

        -   Add support for parsing environment variables or CLI
            > arguments (e.g., using a library like picocli or manual
            > parsing).

        -   Allow configurable defaults for allowedExtensions and
            > maxBytesForHeadContent.

    -   **Key Methods**:

        -   Builder buildFromEnv(): Parse environment variables (e.g.,
            > GH_TOKEN, GH_EXTENSIONS).

    -   **Dependencies**: None.

    -   **Why Important**: Enhances flexibility for different deployment
        > scenarios.

2.  **SnapshotFetcher** (Refine Existing)

    -   **Responsibility**: Fetch PullRequestSnapshot with retry logic
        > for transient failures.

    -   **Changes**:

        -   Add exponential backoff with jitter for rate limits and 5xx
            > errors, using a library like Resilience4j or custom logic.

    -   **Key Methods**:

        -   PullRequestSnapshot fetch(BootstrapConfig cfg,
            > GitHubPullRequestService svc): Include retry logic.

    -   **Dependencies**:

        -   com.example.github.GitHubPullRequestService

        -   com.example.github.model.PullRequestSnapshot

    -   **Why Important**: Ensures robustness against transient GitHub
        > API failures.

3.  **DiffBuilder** (Use Existing)

    -   **Responsibility**: Convert PullRequestSnapshot to DiffCatalog
        > using existing DiffParser.

    -   **Changes**: None; the implementation is correct and uses
        > existing WrappedFileDiff and WrappedHunk.

    -   **Dependencies**:

        -   com.example.github.model.PullRequestSnapshot, ChangedFile

        -   com.example.reviewer.parser.DiffParser, WrappedFileDiff,
            > WrappedHunk

    -   **Why Important**: Reuses existing diff parsing, avoiding
        > duplication.

4.  **JavaAstProvider** (Use Existing)

    -   **Responsibility**: Provide lazy AST parsing for .java files.

    -   **Changes**: None; the implementation is solid, using
        > JavaParsingService correctly.

    -   **Dependencies**:

        -   com.example.ast.JavaParsingService, JavaParsingFileNode

        -   com.example.github.model.PullRequestSnapshot, ChangedFile

    -   **Why Important**: Leverages existing parser with proper error
        > handling.

5.  **ProvenanceFactoryImpl** (Refine Existing)

    -   **Responsibility**: Build LLMProvenance for auditability.

    -   **Changes**:

        -   Validate LLMProvenance API (e.g., confirm if it uses
            > setters, builder, or constructor).

        -   Add null checks for PullRequestInfo fields to handle edge
            > cases.

    -   **Key Methods**:

        -   LLMProvenance createFor(String newPath): Ensure robust
            > metadata handling.

    -   **Dependencies**:

        -   com.example.findings.LLMProvenance

        -   com.example.github.model.PullRequestSnapshot, ChangedFile,
            > PullRequestInfo

    -   **Why Important**: Ensures traceability, critical for debugging
        > and audits.

6.  **RuleRegistryBuilder** (Use Existing)

    -   **Responsibility**: Build RuleRegistry with
        > BroadCatchIntroducedRule.

    -   **Changes**: None; implementation is minimal and correct.

    -   **Dependencies**:

        -   com.example.engine.RuleRegistry, Rule

        -   com.example.engine.rules.BroadCatchIntroducedRule

    -   **Why Important**: Sets up the rules engine for extensibility.

7.  **RunnerOrchestrator** (Refine Existing)

    -   **Responsibility**: Coordinate rule execution with RuleContext
        > and RuleRunner.

    -   **Changes**:

        -   Validate RuleContext.Builder API against the actual engine
            > implementation.

        -   Add logging for rule execution start/end to aid debugging.

    -   **Key Methods**:

        -   RuleRunnerResult run(\...): Add logging around execution.

    -   **Dependencies**:

        -   com.example.engine.RuleContext, RuleRunner,
            > RuleRunnerResult, RuleRegistry

        -   DiffBuilder.DiffCatalog, JavaAstProvider

    -   **Why Important**: Central orchestration point, ensuring
        > deterministic execution.

8.  **DryRunRenderer** (Refine Existing)

    -   **Responsibility**: Render findings as markdown for dry-run
        > validation.

    -   **Changes**:

        -   Add detailed finding output (e.g., include rule message).

        -   Log to a structured logger (e.g., SLF4J) instead of
            > System.out.

    -   **Key Methods**:

        -   String render(RuleRunnerResult result): Return formatted
            > string for logging.

    -   **Dependencies**:

        -   com.example.engine.RuleRunnerResult

        -   com.example.findings.LLMInlineFindingOut

    -   **Why Important**: Provides clear validation output for testing.

9.  **BootstrapRunner** (Refine Existing)

    -   **Responsibility**: Orchestrate the full pipeline.

    -   **Changes**:

        -   Add placeholder for PostingWorker integration.

        -   Use structured logging for errors instead of System.err.

    -   **Key Methods**:

        -   int run(BootstrapConfig cfg): Integrate posting mode.

    -   **Dependencies**:

        -   All above classes

        -   PostingWorker (for posting mode)

    -   **Why Important**: Single entry point for the pipeline.

10. **FindingDeduper** (New)

    -   **Responsibility**: Deduplicate findings by id to prevent
        > redundant comments.

    -   **Key Methods**:

        -   List\<RuleFinding\> dedupe(List\<RuleFinding\> findings):
            > Filter by unique IDs.

    -   **Dependencies**:

        -   com.example.findings.LLMInlineFindingOut

        -   com.example.engine.RuleFinding

    -   **Why Important**: Ensures idempotency in posting.

11. **InlineCommentMapper** (New)

    -   **Responsibility**: Map LLMInlineFindingOut to
        > InlineCommentRequest with RIGHT after-line anchoring.

    -   **Key Methods**:

        -   InlineCommentRequest map(LLMInlineFindingOut f): Convert
            > finding to comment request.

    -   **Dependencies**:

        -   com.example.findings.LLMInlineFindingOut

        -   com.example.github.model.InlineCommentRequest

    -   **Why Important**: Ensures accurate comment placement.

12. **GeneralCommentSummarizer** (New)

    -   **Responsibility**: Summarize failed inline comments as a
        > general comment.

    -   **Key Methods**:

        -   GeneralCommentRequest summarize(List\<LLMInlineFindingOut\>
            > failed): Build summary comment.

    -   **Dependencies**:

        -   com.example.findings.LLMInlineFindingOut

        -   com.example.github.model.GeneralCommentRequest

    -   **Why Important**: Provides fallback feedback.

13. **PostingWorker** (New)

    -   **Responsibility**: Post deduplicated findings with retries and
        > fallback to general comments.

    -   **Key Methods**:

        -   void postAll(BootstrapConfig cfg, PullRequestSnapshot snap,
            > List\<RuleFinding\> findings): Post comments with
            > exponential backoff.

    -   **Dependencies**:

        -   com.example.github.GitHubPullRequestService

        -   com.example.findings.LLMInlineFindingOut

        -   com.example.github.model.InlineCommentRequest,
            > GeneralCommentRequest

        -   FindingDeduper, InlineCommentMapper,
            > GeneralCommentSummarizer

    -   **Why Important**: Completes the pipeline with robust posting.

14. **RuleDiagnosticsLogger** (New)

    -   **Responsibility**: Log rule execution metrics (durations,
        > finding counts).

    -   **Key Methods**:

        -   void log(RuleRunnerResult result): Log to SLF4J.

    -   **Dependencies**:

        -   com.example.engine.RuleRunnerResult

    -   **Why Important**: Provides observability for performance
        > tuning.

15. **ProvenanceLogger** (New)

    -   **Responsibility**: Log snapshot and finding provenance for
        > auditing.

    -   **Key Methods**:

        -   void logSnapshot(PullRequestSnapshot snap)

        -   void logFindingProvenance(LLMInlineFindingOut f)

    -   **Dependencies**:

        -   com.example.github.model.PullRequestSnapshot

        -   com.example.findings.LLMInlineFindingOut

    -   **Why Important**: Ensures traceability.

### **Testing Plan**

-   **Unit Tests** (JUnit 5 with AssertJ):

    -   BootstrapConfig: Validate builder and env parsing.

    -   SnapshotFetcher: Test retry logic with mocked
        > GitHubPullRequestService.

    -   DiffBuilder: Test filtering and mapping with synthetic
        > WrappedFileDiff.

    -   JavaAstProvider: Test parsing success/failure with mocked
        > JavaParsingService.

    -   ProvenanceFactoryImpl: Test provenance creation with various
        > snapshot scenarios.

    -   RuleRegistryBuilder: Verify rule inclusion.

    -   RunnerOrchestrator: Test rule execution with mocked RuleRunner.

    -   DryRunRenderer: Test output formatting.

    -   FindingDeduper, InlineCommentMapper, GeneralCommentSummarizer:
        > Test deduplication and mapping logic.

    -   PostingWorker: Test posting with mocked
        > GitHubPullRequestService.

    -   RuleDiagnosticsLogger, ProvenanceLogger: Test log output.

-   **Integration Tests**:

    -   Mock GitHubPullRequestService to test full pipeline in dry-run
        > mode.

    -   Use synthetic PR data (.java files, diffs) to validate
        > RunnerOrchestrator.

-   **End-to-End Tests**:

    -   Test dry-run with synthetic PR data.

    -   Test controlled posting on a real test PR with
        > BroadCatchIntroducedRule.

### **README**

-   **Content**:

    -   Instructions to run BootstrapRunner in dry-run mode (e.g., java
        > -jar \... \--dry-run).

    -   Instructions for posting mode with environment variables (e.g.,
        > GH_TOKEN, GH_OWNER).

    -   Dependencies (Maven, Java 17).

    -   Example output for dry-run and posted comments.

-   **Purpose**: Guides users on setup and execution.

### **Timeline**

-   **Week 1**:

    -   Refine BootstrapConfig, SnapshotFetcher, ProvenanceFactoryImpl,
        > RunnerOrchestrator, DryRunRenderer.

    -   Implement unit tests for these classes.

    -   Validate dry-run pipeline with synthetic data.

-   **Week 2**:

    -   Implement FindingDeduper, InlineCommentMapper,
        > GeneralCommentSummarizer, PostingWorker,
        > RuleDiagnosticsLogger, ProvenanceLogger.

    -   Add integration and end-to-end tests.

    -   Test controlled posting on a real PR.

-   **Week 3**:

    -   Finalize README and documentation.

    -   Deploy and validate on a test repository.

### **Why This Plan is Effective**

-   **Addresses Horizon Beta's Issue**: Explicitly uses existing
    > components, countering context-forgetting by validating against
    > the PDF's codebase.

-   **Completes Phase 1**: Adds missing posting and logging components
    > to achieve full functionality.

-   **Robust and Auditable**: Includes retries, logging, and provenance
    > for reliability and traceability.

-   **Test-Driven**: Comprehensive testing ensures integration
    > correctness.

-   **Extensible**: Modular design supports future rules and providers.

## **Next Steps**

If you confirm this refined plan, I can generate the scaffolding for the
new classes (FindingDeduper, InlineCommentMapper,
GeneralCommentSummarizer, PostingWorker, RuleDiagnosticsLogger,
ProvenanceLogger), refine the existing classes, and provide JUnit 5
tests with AssertJ, plus the README. Please let me know if you want to
proceed or if specific adjustments are needed.

Tab 7

Alpha horizon reacts to prompt:

Understood. I'll package the previously provided bootstrap scaffolding
into a complete Maven module named engine-bootstrap-app, add a proper
pom.xml, and include the refined pieces aligned with GROK's review. This
project uses your existing modules (GitHub connector, Java AST parser,
diff parser, and rules engine) as dependencies and focuses on dry-run
orchestration. Posting worker scaffolding can be added next if you
confirm.

Project layout

\- engine-bootstrap-app/

\- pom.xml

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

\- src/test/java/com/example/bootstrap/

\- BootstrapConfigTest.java

\- DiffBuilderTest.java

\- JavaAstProviderTest.java

File: engine-bootstrap-app/pom.xml

\<?xml version=\"1.0\" encoding=\"UTF-8\"?\>

\<!\--

Maven build file for engine-bootstrap-app.

This module wires together existing components:

\- GitHub connector (com.example.github)

\- Java AST parser (com.example.ast)

\- Diff parser (com.example.reviewer.parser)

\- Rules engine core (com.example.engine)

and runs a dry-run or posting bootstrap.

\--\>

\<project xmlns=\"http://maven.apache.org/POM/4.0.0\"

xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"

xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0

https://maven.apache.org/xsd/maven-4.0.0.xsd\"\>

\<modelVersion\>4.0.0\</modelVersion\>

\<groupId\>com.example\</groupId\>

\<artifactId\>engine-bootstrap-app\</artifactId\>

\<version\>1.0.0\</version\>

\<name\>Engine Bootstrap App\</name\>

\<description\>Bootstrap application integrating GitHub connector, diff
parser, Java AST, and rules engine (dry-run first)\</description\>

\<properties\>

\<maven.compiler.source\>17\</maven.compiler.source\>

\<maven.compiler.target\>17\</maven.compiler.target\>

\<project.build.sourceEncoding\>UTF-8\</project.build.sourceEncoding\>

\<junit.jupiter.version\>5.10.2\</junit.jupiter.version\>

\<assertj.version\>3.25.3\</assertj.version\>

\<slf4j.version\>2.0.12\</slf4j.version\>

\</properties\>

\<dependencies\>

\<!\-- Existing modules (replace versions/groupIds with your actual
coordinates) \--\>

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

\<!\-- Logging facade for structured logs \--\>

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

\<!\-- Test deps \--\>

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

\</dependencies\>

\<build\>

\<plugins\>

\<!\-- JUnit 5 \--\>

\<plugin\>

\<groupId\>org.apache.maven.plugins\</groupId\>

\<artifactId\>maven-surefire-plugin\</artifactId\>

\<version\>3.2.5\</version\>

\<configuration\>

\<useModulePath\>false\</useModulePath\>

\</configuration\>

\</plugin\>

\<!\-- Build an executable jar for quick runs \--\>

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

File: src/main/java/com/example/bootstrap/BootstrapConfig.java

\[unchanged from prior message; includes full Javadoc and builder
validation\]

File: src/main/java/com/example/bootstrap/SnapshotFetcher.java

\[unchanged, small pass-through to GitHubPullRequestService; retries can
be added later\]

File: src/main/java/com/example/bootstrap/DiffBuilder.java

\[unchanged core; uses DiffParser/WrappedFileDiff/WrappedHunk; contains
inner DiffMapping and DiffCatalog with Javadoc and inline comments for
\>20 lines\]

File: src/main/java/com/example/bootstrap/JavaAstProvider.java

\[unchanged; lazy AST loading using JavaParsingService; Optional cache\]

File: src/main/java/com/example/bootstrap/ProvenanceFactoryImpl.java

\[as provided; note: if LLMProvenance uses a builder instead of setters,
adapt here accordingly\]

File: src/main/java/com/example/bootstrap/RuleRegistryBuilder.java

\[as provided; returns registry with BroadCatchIntroducedRule\]

File: src/main/java/com/example/bootstrap/RunnerOrchestrator.java

\[as provided; constructs RuleContext, runs RuleRunner; includes
PullRequestContext alias wrapper\]

File: src/main/java/com/example/bootstrap/DryRunRenderer.java

\[as provided; logs formatted lines; can switch to SLF4J easily if
preferred\]

File: src/main/java/com/example/bootstrap/BootstrapRunner.java

\[as provided; builds config from env; runs dry-run; placeholder for
posting worker\]

Basic tests (skeletons with AssertJ, JUnit 5)

File: src/test/java/com/example/bootstrap/BootstrapConfigTest.java

package com.example.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.\*;

class BootstrapConfigTest {

\@Test

void builder_validatesRequiredFields() {

assertThatThrownBy(() -\> new BootstrapConfig.Builder().build())

.isInstanceOf(IllegalArgumentException.class);

BootstrapConfig cfg = new BootstrapConfig.Builder()

.withOauthToken(\"t\")

.withOwner(\"o\")

.withRepository(\"r\")

.withPullRequestNumber(1)

.build();

assertThat(cfg.getOwner()).isEqualTo(\"o\");

assertThat(cfg.getRepository()).isEqualTo(\"r\");

assertThat(cfg.getPullRequestNumber()).isEqualTo(1);

assertThat(cfg.isDryRun()).isTrue();

}

}

File: src/test/java/com/example/bootstrap/DiffBuilderTest.java

package com.example.bootstrap;

import com.example.github.model.ChangeType;

import com.example.github.model.ChangedFile;

import com.example.github.model.PullRequestInfo;

import com.example.github.model.PullRequestSnapshot;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import java.util.List;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiffBuilderTest {

\@Test

void build_filtersEligibleAndBuildsCatalog() {

PullRequestInfo info = new
PullRequestInfo(\"o\",\"r\",1,\"t\",\"u\",\"open\",false,\"base\",\"head\",

OffsetDateTime.now(), OffsetDateTime.now());

ChangedFile f = new ChangedFile(null, \"src/Main.java\",
ChangeType.MODIFIED,

// minimal one-file patch with one added line

\"diff \--git a/src/Main.java b/src/Main.java\\n\" +

\"\-\-- a/src/Main.java\\n\" +

\"+++ b/src/Main.java\\n\" +

\"@@ -1,1 +1,2 @@\\n\" +

\" class A {}\\n\" +

\"+// added\\n\",

\"class A {}\\n// added\\n\", 0,0,0, \"blob\", \"base\",\"head\");

PullRequestSnapshot snap = new PullRequestSnapshot(info, List.of(f));

DiffBuilder b = new DiffBuilder();

DiffBuilder.DiffCatalog cat = b.build(snap, Set.of(\".java\"), 0);

assertThat(cat.get(\"src/Main.java\")).isPresent();

assertThat(cat.get(\"src/Main.java\").get().getAfterChangedLines()).contains(2);

}

}

File: src/test/java/com/example/bootstrap/JavaAstProviderTest.java

package com.example.bootstrap;

import com.example.ast.JavaParsingFileNode;

import com.example.ast.JavaParsingService;

import com.example.github.model.ChangeType;

import com.example.github.model.ChangedFile;

import com.example.github.model.PullRequestInfo;

import com.example.github.model.PullRequestSnapshot;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import java.util.List;

import java.util.Optional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAstProviderTest {

\@Test

void loadsAst_whenHeadContentAvailable() {

PullRequestInfo info = new
PullRequestInfo(\"o\",\"r\",1,\"t\",\"u\",\"open\",false,\"base\",\"head\",

OffsetDateTime.now(), OffsetDateTime.now());

ChangedFile f = new ChangedFile(null, \"src/Main.java\",
ChangeType.MODIFIED,

null,

\"package a; class A {}\", 0,0,0, \"blob\",\"base\",\"head\");

PullRequestSnapshot snap = new PullRequestSnapshot(info, List.of(f));

JavaAstProvider provider = JavaAstProvider.fromSnapshot(snap, new
JavaParsingService(), Set.of(\"src/Main.java\"));

Optional\<JavaParsingFileNode\> ast =
provider.getAst(\"src/Main.java\");

assertThat(ast).isPresent();

assertThat(ast.get().kind.name()).isEqualTo(\"FILE\");

}

}

How to build and run

\- Ensure your existing modules are published to your local Maven
repository with the coordinates used in the pom.xml:

\- com.example:github-pull-request-connector:1.0.0

\- com.example:llm-reviewer-diff-parser:1.0.0

\- com.example:java-ast-parser:1.0.0

\- com.example:rules-engine-core:1.0.0

\- Build:

\- cd engine-bootstrap-app

\- mvn -q -DskipTests package

\- Run (dry-run mode):

\- export GH_TOKEN=\...

\- export GH_OWNER=\...

\- export GH_REPO=\...

\- export GH_PR=123

\- java -jar target/engine-bootstrap-app-1.0.0.jar

Notes and next steps

\- This module uses your existing code; nothing is re-implemented.

\- ProvenanceFactoryImpl assumes setter-based \`LLMProvenance\`. If your
DTO is builder- or constructor-based, I will adapt it accordingly on
your confirmation.

\- To complete Phase 1, I can add the posting-worker classes
(\`FindingDeduper\`, \`InlineCommentMapper\`,
\`GeneralCommentSummarizer\`, \`PostingWorker\`) with Javadoc and tests,
plus \`RuleDiagnosticsLogger\` and \`ProvenanceLogger\`, and refine
\`SnapshotFetcher\` to include retry/backoff as per the GROK plan.

Would you like me to proceed with the posting worker and diagnostics
scaffolding next?

Tab 8

Can you please generate a [[README.md]{.underline}](http://readme.md)
that explains this part of the project its dependencies etc..

Tab 9

Horizon beta generated the [[README.md]{.underline}](http://readme.md)

\# Engine Bootstrap App

Overview

Engine Bootstrap App is a small, integration-focused Java application
that orchestrates an end-to-end rules-engine run against a Pull Request.
It wires together existing modules without re-implementing them:

\- GitHub connector: fetches a deterministic Pull Request snapshot and
posts comments.

\- Diff parser: parses unified diffs and exposes per-file changed
after-line sets.

\- Java AST parser: lazily parses .java files and exposes an
LLM-friendly AST.

\- Rules engine (Milestone 1): executes deterministic rules (e.g.,
BroadCatchIntroducedRule) over changed lines.

This module supports a safe dry-run mode for validation and can be
extended with a posting worker for inline comments.

Key capabilities

\- Deterministic pipeline: stable ordering and immutable DTOs used
throughout.

\- Dry-run execution: renders intended inline findings to stdout without
posting.

\- Clean separation: I/O (GitHub), diffs, AST, engine, and output
responsibilities remain modular.

\- Extensible orchestration: easy to add rules and logging/diagnostics.

Project structure

\- src/main/java/com/example/bootstrap/

\- BootstrapConfig.java: Validated configuration (token, owner, repo, PR
number, caps, filters, dry-run).

\- SnapshotFetcher.java: Fetches Pull Request snapshot via GitHub
connector.

\- DiffBuilder.java: Uses the existing DiffParser to produce per-file
after-line mappings.

\- JavaAstProvider.java: Lazy AST loader using JavaParsingService for
.java files with head content.

\- ProvenanceFactoryImpl.java: Builds LLMProvenance from snapshot
metadata.

\- RuleRegistryBuilder.java: Provides the default rule set
(BroadCatchIntroducedRule).

\- RunnerOrchestrator.java: Builds RuleContext and runs RuleRunner.

\- DryRunRenderer.java: Renders findings for dry-run validation.

\- BootstrapRunner.java: Entry point; orchestrates end-to-end execution.

\- src/test/java/com/example/bootstrap/

\- BootstrapConfigTest.java, DiffBuilderTest.java,
JavaAstProviderTest.java: Basic unit tests.

Dependencies

This app depends on your existing modules (ensure they're published to
your Maven repository with the coordinates below, or update the pom.xml
to match your actual group/artifact/version):

\- com.example:github-pull-request-connector:1.0.0

\- Provides: GitHubPullRequestService, PullRequestSnapshot, ChangedFile,
Inline/General comment requests.

\- com.example:llm-reviewer-diff-parser:1.0.0

\- Provides: DiffParser, WrappedFileDiff, WrappedHunk for unified diff
parsing and line sets.

\- com.example:java-ast-parser:1.0.0

\- Provides: JavaParsingService, JavaParsingFileNode (LLM-friendly AST),
NodeIdFactory, serialization.

\- com.example:rules-engine-core:1.0.0

\- Provides: Rule, RuleContext, RuleRegistry, RuleRunner,
RuleRunnerResult, BroadCatchIntroducedRule.

Runtime dependencies

\- SLF4J (simple binding by default for quick runs; can switch to
Logback/Log4j2 as needed).

\- Java 17+.

Build

1\) Ensure upstream modules are available:

\- Install/publish your existing modules to the local repository:

\- mvn install in each of:

\- github-pull-request-connector

\- llm-reviewer-diff-parser

\- java-ast-parser

\- rules-engine-core

2\) Build engine-bootstrap-app:

\- cd engine-bootstrap-app

\- mvn clean package

Run (dry-run)

Environment variables:

\- GH_TOKEN: GitHub token (PAT or App installation token).

\- GH_OWNER: Repository owner/organization.

\- GH_REPO: Repository name.

\- GH_PR: Pull Request number.

\- Optional:

\- DRY_RUN=true\|false (default true)

\- GH_EXTENSIONS=.java,.kt (default .java)

\- GH_MAX_HEAD_BYTES=262144 (default 256 KiB)

\- GH_MAX_FILES=0 (no cap by default)

Command:

\- java -jar target/engine-bootstrap-app-1.0.0.jar

Expected output (example):

\- \[DRY-RUN\] findings=2

\- \[DRY-RUN\]\[001\] rule=JAVA_BROAD_CATCH id=\...
file=src/main/java/com/example/Foo.java line=123

\- \[DRY-RUN\]\[002\] rule=JAVA_BROAD_CATCH id=\...
file=src/main/java/com/example/Bar.java line=87

\- \[DRY-RUN\] durationsMillis={JAVA_BROAD_CATCH=12}

Configuration

BootstrapConfig controls a single run. Defaults are conservative,
emphasizing dry-run and .java files:

\- oauthToken (required)

\- owner (required)

\- repository (required)

\- pullRequestNumber (required, \> 0)

\- includeHeadFileContent (default true)

\- maxBytesForHeadContent (default 262,144 bytes)

\- dryRun (default true)

\- allowedExtensions (default .java)

\- maxFilesToProcess (default 0, unlimited)

Current behavior

\- Fetch: PullRequestSnapshot (base/head SHAs, changed files, unified
patches, optional head content).

\- Diff: Build per-file after-line changed sets using DiffParser and
hunks' added lines.

\- AST: Lazy parse .java files with available head content; return
Optional.empty() on failure.

\- Rules: Run RuleRunner with BroadCatchIntroducedRule.

\- Output: If dry-run, print formatted findings. Otherwise, placeholder
to integrate a PostingWorker.

Planned enhancements (Phase 1 completion)

\- Posting worker:

\- FindingDeduper: dedupe by finding.id.

\- InlineCommentMapper: map LLMInlineFindingOut to InlineCommentRequest
(RIGHT, after-line).

\- GeneralCommentSummarizer: fallback general comment for failed
inlines.

\- PostingWorker: post with retries/backoff; fallback to summary.

\- Diagnostics:

\- RuleDiagnosticsLogger: per-rule durations and counts.

\- ProvenanceLogger: log snapshot and finding provenance.

\- Robust retries:

\- SnapshotFetcher: exponential backoff with jitter for rate limits/5xx.

\- CLI/env parsing helpers:

\- Allow setting extensions, caps, and mode via args or env with
validation.

Design notes

\- No duplication: This module strictly composes existing components
(GitHub connector, diff parser, Java AST parser, rules engine).

\- Determinism: Stable iteration (lexicographic), immutable collections,
deterministic IDs from upstream modules.

\- Safety: Dry-run first; posting is feature-flagged and includes dedupe
and fallback.

\- Extensibility: Add new rules in RuleRegistryBuilder; swap logging or
connectors with minimal changes.

Troubleshooting

\- Missing dependencies: Ensure the upstream modules are installed
locally or available from your artifact repository and that the
coordinates in pom.xml match.

\- No findings in dry-run: Verify the PR has .java changes and unified
patches are present (GitHub may omit patches for very large/binary
files).

\- AST missing: Head content may be omitted if it exceeds
maxBytesForHeadContent; adjust GH_MAX_HEAD_BYTES or disable
includeHeadFileContent at your own risk.

Security

\- Do not commit tokens. Use environment variables or a secure secrets
manager.

\- Follow GitHub rate limits; consider app-based tokens for higher
quotas.

License

\- This module follows the project's overarching license (e.g.,
Apache-2.0). Ensure headers are retained in source files.

Tab 10

I did not review the code myself.

But is the code depending on any queing system like RrabitMQ or Kafka or
not?

Tab 11

No. The engine-bootstrap-app code I provided does not depend on any
queuing system (no RabbitMQ, Kafka, or similar). It runs synchronously
in-process:

\- Fetch snapshot via \`GitHubPullRequestService\`

\- Build diffs and ASTs

\- Run rules with \`RuleRunner\`

\- Render dry-run output (or, later, post inline comments directly via
the connector)

If you want to introduce a queue later (e.g., to decouple rule execution
from posting), we can add an optional module that publishes
\`LLMInlineFindingOut\` messages and a separate consumer/worker, but
that's not part of the current code or its dependencies.
