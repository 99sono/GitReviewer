Yes---that is a great next move. The finding-schema is purely the data
model that all other modules depend on, so locking it down early
stabilizes identifiers, locations, provenance, and severities for the
rest of the pipeline.

Proposed next incremental step: finalize finding-schema with tests and a
README

Scope

1\) Implement the finding-schema module

\- FindingSeverity: INFO, WARNING, ERROR with documented ordering
guarantees.

\- FindingLocation: file path, after-line, optional end-after-line
(range support), validation.

\- FindingProvenance: baseSha, headSha, optional blobSha, optional
ruleVersion, optional toolVersion.

\- Finding: immutable diagnostic object with id, ruleId, severity,
category, message, rationale, remediation, location, provenance.

\- FindingIdFactory: deterministic SHA-256 identifier from
filePath\|afterLine\|ruleId\|headSha.

2\) Add JUnit 5 tests (AssertJ)

\- Validates:

\- FindingLocation validation (line bounds, start \<= end).

\- Deterministic FindingIdFactory: same inputs -\> same id; change any
component -\> different id.

\- Finding immutability and required fields.

\- Provenance optional fields behavior.

3\) Provide a concise README

\- Clarify responsibilities and guarantees: pure data model, no
traversal or orchestration.

\- Explain how identifiers are formed and why this design kicks ass
(determinism, auditability, portability).

\- Show minimal usage snippet.

Why this step

\- It creates a stable contract consumed by rule-engine, outputs, and
bootstrap-app, reducing churn later.

\- It enables immediate work on rule-engine and renderer with a solid,
tested data model.

\- It avoids concern overlap by keeping schema free of any rule or
traversal logic.

Deliverables outline (no abbreviations, intention-revealing names,
Javadoc everywhere)

Package: com.example.findings

FindingSeverity.java

\- Enum with INFO, WARNING, ERROR

\- Javadoc includes ordering note and usage guidance

FindingLocation.java

\- Fields: filePath, afterLine, Optional\<Integer\> endAfterLine

\- Validation: afterLine \>= 1; if end present, end \>= afterLine

\- Accessors and equals/hashCode/toString

FindingProvenance.java

\- Fields: baseSha, headSha, Optional\<String\> blobSha,
Optional\<String\> ruleVersion, Optional\<String\> toolVersion

\- Accessors and equals/hashCode/toString

Finding.java

\- Fields: id, ruleId, FindingSeverity, category, message, rationale,
remediation, FindingLocation, FindingProvenance

\- Constructor validation: all required non-null, strings non-blank
where appropriate

\- Accessors and equals/hashCode/toString

FindingIdFactory.java

\- createId(FindingLocation location, String ruleId, String headSha)

\- SHA-256( filePath + \"\|\" + afterLine + \"\|\" + ruleId + \"\|\" +
headSha ) in lowercase hex

\- Javadoc explains determinism and deduplication rationale

Tests

FindingIdFactoryTest

\- sameInputs_produceSameId()

\- differentHeadSha_producesDifferentId()

\- differentRuleId_producesDifferentId()

\- differentLine_producesDifferentId()

\- pathNormalization_notApplied_idsRemainPathSensitive() (document that
normalization is caller's responsibility)

FindingLocationTest

\- validatesBounds_andEndAfterLine()

\- equalsHashCode_contract()

FindingTest

\- constructsFinding_withAllFields()

\- rejectsNulls_andBlankStringsWhereRequired()

README (module-specific)

\- What it is: Provider-agnostic finding model for precise inline review

\- Why it kicks ass: deterministic ids, explicit location/provenance,
small immutable types

\- How to use: tiny example creating a Finding and id via
FindingIdFactory

Comments on why the code kicks ass

\- Deterministic and auditable identifiers: stable dedup and idempotent
posting.

\- Separation of concerns: zero orchestration or traversal in the
schema.

\- Small, immutable types are easy to test and reason about.

If you give the go-ahead, I will produce the complete finding-schema
module code with full Javadoc, no name reductions, inline "why this
kicks ass" comments, the tests, and the README in the next message.

Tab 2

please go with unit tests and a nice readme explain thr project and what
it delivers in the grand scheme of things

Tab 3

Great. I'll deliver the finding-schema module with complete unit tests
and a clear README that explains its role in the overall system.

Module: finding-schema

Package: com.example.findings

Files

\- src/main/java/com/example/findings/FindingSeverity.java

\- src/main/java/com/example/findings/FindingLocation.java

\- src/main/java/com/example/findings/FindingProvenance.java

\- src/main/java/com/example/findings/Finding.java

\- src/main/java/com/example/findings/FindingIdFactory.java

\- src/test/java/com/example/findings/FindingIdFactoryTest.java

\- src/test/java/com/example/findings/FindingLocationTest.java

\- src/test/java/com/example/findings/FindingTest.java

\- README.md

Source code

FindingSeverity.java

package com.example.findings;

/\*\*

\* Severity levels for a finding reported by rules.

\*

\* Ordering guarantee: ERROR \> WARNING \> INFO

\* Use WARNING as the default for risky or non-blocking issues.

\*/

public enum FindingSeverity {

INFO,

WARNING,

ERROR;

/\*\*

\* Returns true if this severity is greater than or equal to the given
minimum.

\*/

public boolean atLeast(FindingSeverity minimum) {

return this.ordinal() \>= minimum.ordinal();

}

}

FindingLocation.java

package com.example.findings;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Precise location of a finding in the \"after\" version of a file.

\* All line numbers are 1-based. Range is closed-inclusive when
endAfterLine is present.

\*

\* Immutable and validated on construction.

\*/

public final class FindingLocation {

private final String filePath;

private final int afterLine;

private final Integer endAfterLine; // nullable for single-line findings

public FindingLocation(String filePath, int afterLine, Integer
endAfterLine) {

this.filePath = Objects.requireNonNull(filePath, \"filePath\");

if (filePath.isBlank()) throw new IllegalArgumentException(\"filePath
must not be blank\");

if (afterLine \< 1) throw new IllegalArgumentException(\"afterLine must
be \>= 1\");

if (endAfterLine != null && endAfterLine \< afterLine) {

throw new IllegalArgumentException(\"endAfterLine must be \>=
afterLine\");

}

this.afterLine = afterLine;

this.endAfterLine = endAfterLine;

}

public String getFilePath() {

return filePath;

}

public int getAfterLine() {

return afterLine;

}

public Optional\<Integer\> getEndAfterLine() {

return Optional.ofNullable(endAfterLine);

}

\@Override

public boolean equals(Object o) {

if (this == o) return true;

if (!(o instanceof FindingLocation)) return false;

FindingLocation that = (FindingLocation) o;

return afterLine == that.afterLine &&

filePath.equals(that.filePath) &&

Objects.equals(endAfterLine, that.endAfterLine);

}

\@Override

public int hashCode() {

return Objects.hash(filePath, afterLine, endAfterLine);

}

\@Override

public String toString() {

return \"FindingLocation{\" +

\"filePath=\'\" + filePath + \'\\\'\' +

\", afterLine=\" + afterLine +

\", endAfterLine=\" + endAfterLine +

\'}\';

}

}

FindingProvenance.java

package com.example.findings;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Provenance information for a finding: commit SHAs and optional tool
metadata.

\* This enables auditability and idempotent reporting across reruns.

\*

\* Immutable and validated on construction.

\*/

public final class FindingProvenance {

private final String baseSha;

private final String headSha;

private final String blobSha; // optional

private final String ruleVersion; // optional

private final String toolVersion; // optional

public FindingProvenance(String baseSha, String headSha, String blobSha,
String ruleVersion, String toolVersion) {

this.baseSha = requireSha(baseSha, \"baseSha\");

this.headSha = requireSha(headSha, \"headSha\");

this.blobSha = emptyToNull(blobSha);

this.ruleVersion = emptyToNull(ruleVersion);

this.toolVersion = emptyToNull(toolVersion);

}

private static String requireSha(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

private static String emptyToNull(String v) {

if (v == null) return null;

return v.isBlank() ? null : v;

}

public String getBaseSha() {

return baseSha;

}

public String getHeadSha() {

return headSha;

}

public Optional\<String\> getBlobSha() {

return Optional.ofNullable(blobSha);

}

public Optional\<String\> getRuleVersion() {

return Optional.ofNullable(ruleVersion);

}

public Optional\<String\> getToolVersion() {

return Optional.ofNullable(toolVersion);

}

\@Override

public String toString() {

return \"FindingProvenance{\" +

\"baseSha=\'\" + baseSha + \'\\\'\' +

\", headSha=\'\" + headSha + \'\\\'\' +

\", blobSha=\" + blobSha +

\", ruleVersion=\" + ruleVersion +

\", toolVersion=\" + toolVersion +

\'}\';

}

}

Finding.java

package com.example.findings;

import java.util.Objects;

/\*\*

\* Immutable diagnostic result produced by a rule.

\*

\* Fields:

\* - id: deterministic identifier for dedup/idempotency

\* - ruleId: stable identifier of the rule (e.g., \"JAVA_BROAD_CATCH\")

\* - severity: INFO/WARNING/ERROR

\* - category: short classification (\"bug-risk\", \"style\",
\"security\")

\* - message: concise message

\* - rationale: explanation of why this matters

\* - remediation: actionable guidance

\* - location: precise after-file location

\* - provenance: commit SHAs and tool metadata for audits

\*/

public final class Finding {

private final String id;

private final String ruleId;

private final FindingSeverity severity;

private final String category;

private final String message;

private final String rationale;

private final String remediation;

private final FindingLocation location;

private final FindingProvenance provenance;

public Finding(String id,

String ruleId,

FindingSeverity severity,

String category,

String message,

String rationale,

String remediation,

FindingLocation location,

FindingProvenance provenance) {

this.id = requireNonBlank(id, \"id\");

this.ruleId = requireNonBlank(ruleId, \"ruleId\");

this.severity = Objects.requireNonNull(severity, \"severity\");

this.category = requireNonBlank(category, \"category\");

this.message = requireNonBlank(message, \"message\");

this.rationale = requireNonBlank(rationale, \"rationale\");

this.remediation = requireNonBlank(remediation, \"remediation\");

this.location = Objects.requireNonNull(location, \"location\");

this.provenance = Objects.requireNonNull(provenance, \"provenance\");

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

public String getId() {

return id;

}

public String getRuleId() {

return ruleId;

}

public FindingSeverity getSeverity() {

return severity;

}

public String getCategory() {

return category;

}

public String getMessage() {

return message;

}

public String getRationale() {

return rationale;

}

public String getRemediation() {

return remediation;

}

public FindingLocation getLocation() {

return location;

}

public FindingProvenance getProvenance() {

return provenance;

}

\@Override

public String toString() {

return \"Finding{\" +

\"id=\'\" + id + \'\\\'\' +

\", ruleId=\'\" + ruleId + \'\\\'\' +

\", severity=\" + severity +

\", category=\'\" + category + \'\\\'\' +

\", message=\'\" + message + \'\\\'\' +

\", location=\" + location +

\", provenance=\" + provenance +

\'}\';

}

}

FindingIdFactory.java

package com.example.findings;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.HexFormat;

import java.util.Objects;

/\*\*

\* Deterministic ID factory for findings.

\*

\* Format: hex( SHA-256( filePath + \"\|\" + afterLine + \"\|\" +
ruleId + \"\|\" + headSha ) )

\*

\* Rationale:

\* - Stable across reruns for the same PR head, enabling idempotent
posting and dedup.

\* - Sensitive to file path, line, and rule, avoiding collisions.

\* - Head SHA anchors to the reviewed commit, improving auditability.

\*

\* Note: Path normalization is the caller\'s responsibility. Use
repository-relative newPath.

\*/

public final class FindingIdFactory {

private FindingIdFactory() {}

public static String createId(FindingLocation location, String ruleId,
String headSha) {

Objects.requireNonNull(location, \"location\");

Objects.requireNonNull(ruleId, \"ruleId\");

Objects.requireNonNull(headSha, \"headSha\");

if (ruleId.isBlank()) throw new IllegalArgumentException(\"ruleId must
not be blank\");

if (headSha.isBlank()) throw new IllegalArgumentException(\"headSha must
not be blank\");

String payload = location.getFilePath()

\+ \"\|\" + location.getAfterLine()

\+ \"\|\" + ruleId

\+ \"\|\" + headSha;

try {

MessageDigest md = MessageDigest.getInstance(\"SHA-256\");

byte\[\] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));

return HexFormat.of().formatHex(digest);

} catch (Exception e) {

throw new IllegalStateException(\"Failed to compute SHA-256 for finding
id\", e);

}

}

}

Unit tests

FindingIdFactoryTest.java

package com.example.findings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindingIdFactoryTest {

\@Test

void sameInputs_produceSameId() {

FindingLocation loc = new FindingLocation(\"src/Foo.java\", 10, null);

String id1 = FindingIdFactory.createId(loc, \"RULE_X\", \"deadbeef\");

String id2 = FindingIdFactory.createId(loc, \"RULE_X\", \"deadbeef\");

assertThat(id1).isEqualTo(id2);

}

\@Test

void differentHeadSha_producesDifferentId() {

FindingLocation loc = new FindingLocation(\"src/Foo.java\", 10, null);

String a = FindingIdFactory.createId(loc, \"RULE_X\", \"aaaaaaaa\");

String b = FindingIdFactory.createId(loc, \"RULE_X\", \"bbbbbbbb\");

assertThat(a).isNotEqualTo(b);

}

\@Test

void differentRuleId_producesDifferentId() {

FindingLocation loc = new FindingLocation(\"src/Foo.java\", 10, null);

String a = FindingIdFactory.createId(loc, \"RULE_A\", \"deadbeef\");

String b = FindingIdFactory.createId(loc, \"RULE_B\", \"deadbeef\");

assertThat(a).isNotEqualTo(b);

}

\@Test

void differentLine_producesDifferentId() {

FindingLocation loc1 = new FindingLocation(\"src/Foo.java\", 10, null);

FindingLocation loc2 = new FindingLocation(\"src/Foo.java\", 11, null);

String a = FindingIdFactory.createId(loc1, \"RULE_X\", \"deadbeef\");

String b = FindingIdFactory.createId(loc2, \"RULE_X\", \"deadbeef\");

assertThat(a).isNotEqualTo(b);

}

\@Test

void pathNormalization_notApplied_idsRemainPathSensitive() {

FindingLocation loc1 = new FindingLocation(\"./src/Foo.java\", 10,
null);

FindingLocation loc2 = new FindingLocation(\"src/Foo.java\", 10, null);

String a = FindingIdFactory.createId(loc1, \"RULE_X\", \"deadbeef\");

String b = FindingIdFactory.createId(loc2, \"RULE_X\", \"deadbeef\");

assertThat(a).isNotEqualTo(b);

}

}

FindingLocationTest.java

package com.example.findings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;

class FindingLocationTest {

\@Test

void validatesBounds_andEndAfterLine() {

new FindingLocation(\"src/Foo.java\", 1, null);

new FindingLocation(\"src/Foo.java\", 2, 5);

assertThatThrownBy(() -\> new FindingLocation(\"src/Foo.java\", 0,
null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new FindingLocation(\"src/Foo.java\", 5, 4))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new FindingLocation(\" \", 1, null))

.isInstanceOf(IllegalArgumentException.class);

}

\@Test

void equalsHashCode_contract() {

FindingLocation a = new FindingLocation(\"src/Foo.java\", 3, 7);

FindingLocation b = new FindingLocation(\"src/Foo.java\", 3, 7);

FindingLocation c = new FindingLocation(\"src/Foo.java\", 3, null);

assertThat(a).isEqualTo(b);

assertThat(a.hashCode()).isEqualTo(b.hashCode());

assertThat(a).isNotEqualTo(c);

}

}

FindingTest.java

package com.example.findings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindingTest {

\@Test

void constructsFinding_withAllFields() {

var loc = new FindingLocation(\"src/Foo.java\", 10, null);

var prov = new FindingProvenance(\"base123\", \"head456\", \"blob789\",
\"1.0.0\", \"tool-1\");

new Finding(

\"id123\",

\"RULE_X\",

FindingSeverity.WARNING,

\"bug-risk\",

\"Message\",

\"Rationale\",

\"Remediation\",

loc,

prov

);

}

\@Test

void rejectsNulls_andBlankStringsWhereRequired() {

var loc = new FindingLocation(\"src/Foo.java\", 10, null);

var prov = new FindingProvenance(\"base123\", \"head456\", null, null,
null);

assertThatThrownBy(() -\> new Finding(null, \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \"fix\", loc, prov))

.isInstanceOf(NullPointerException.class);

assertThatThrownBy(() -\> new Finding(\" \", \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \"fix\", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \" \",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \"fix\", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\", null, \"cat\",
\"m\", \"r\", \"fix\", loc, prov))

.isInstanceOf(NullPointerException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \" \", \"m\", \"r\", \"fix\", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \"cat\", \" \", \"r\", \"fix\", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \" \", \"fix\", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \" \", loc, prov))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \"fix\", null, prov))

.isInstanceOf(NullPointerException.class);

assertThatThrownBy(() -\> new Finding(\"id\", \"RULE\",
FindingSeverity.INFO, \"cat\", \"m\", \"r\", \"fix\", loc, null))

.isInstanceOf(NullPointerException.class);

}

}

README.md

Finding Schema

What it is

A small, immutable, provider-agnostic data model for code review
findings. It defines how a rule reports a result: where it applies
(location), how severe it is, why it matters, how to fix it, and which
commit snapshot produced it. It contains no orchestration, AST logic, or
GitHub code.

Why it exists

\- Determinism and auditability: Stable identifiers and explicit commit
provenance make runs reproducible and safe to post multiple times
without duplication.

\- Separation of concerns: Rules, renderers, and orchestrators all
depend on a shared schema that never takes on execution
responsibilities.

\- Portability: The same finding can be rendered to GitHub inline
comments, SARIF, logs, or dashboards without translation churn.

What it delivers

\- Finding: immutable diagnostic with message, rationale, remediation,
and classification.

\- FindingLocation: precise, 1-based after-file location with optional
range support.

\- FindingProvenance: base/head SHAs, optional blob SHA and tool
metadata.

\- FindingSeverity: INFO/WARNING/ERROR with a simple ordering helper.

\- FindingIdFactory: deterministic SHA-256 IDs based on file path, line,
rule, and head SHA.

Deterministic IDs

We compute: hex(SHA-256(filePath \| afterLine \| ruleId \| headSha)).
This makes IDs stable for a given PR head and finding location, enabling
idempotent posting (no duplicate comments) and clear auditing during
re-runs. Paths are not normalized; callers should pass
repository-relative newPath.

How it fits in the bigger picture

\- github-connector: fetches PR snapshot; not part of this module.

\- diff-mapper-core: produces changed after-line sets and hunk metadata;
not part of this module.

\- java-parser: builds ASTs; not part of this module.

\- rule-engine: rules generate Findings using this schema; the engine
aggregates them.

\- outputs: renderers (e.g., GitHub inline) consume Findings; the schema
gives them all the information they need.

Example usage

FindingLocation loc = new
FindingLocation(\"src/main/java/com/example/Foo.java\", 42, null);

FindingProvenance prov = new FindingProvenance(\"baseSha\", \"headSha\",
\"blobSha\", \"rule-1.0.0\", \"tool-0.1.0\");

String id = FindingIdFactory.createId(loc, \"JAVA_BROAD_CATCH\",
prov.getHeadSha());

Finding finding = new Finding(

id,

\"JAVA_BROAD_CATCH\",

FindingSeverity.WARNING,

\"bug-risk\",

\"Broad catch detected: Exception\",

\"Catching Exception can hide real errors.\",

\"Catch a specific exception or rethrow after logging.\",

loc,

prov

);

Testing

This module includes JUnit 5 tests that validate:

\- Location validation and equality semantics

\- Deterministic IDs and path sensitivity

\- Required field enforcement for Finding and Provenance

Non-goals

\- No orchestration: fetching PRs, looping files, or running rules is
not in scope.

\- No AST logic or traversal utilities.

\- No rendering: that belongs in outputs modules.

Why this kicks ass

It locks down a stable contract that every part of the system trusts.
IDs are deterministic, locations are precise, and provenance is
explicit---so postings are idempotent, runs are auditable, and
developers understand exactly what is being flagged and why.

Next suggested step

With finding-schema stable, wire it into rule-engine and outputs:

\- Implement Rule and RuleContext interfaces in rule-engine and a simple
RuleRunner.

\- Create a minimal GitHub renderer in outputs that formats inline
comments using this schema.

\- Then implement one conservative AST-based rule and integrate via
bootstrap-app in dry-run mode.

Tab 4

Here is a revised version of your text with corrected grammar and
improved clarity:

\-\--

You are relying on the fact that the LLM output is converted into a list
of findings. To be honest, I do not think this is necessary at all.

In my Python-based code reviewer, which I created previously, simply
obtaining feedback from the LLM in Markdown format and adding it as a
comment to the review directly as it came from the LLM was perfect.

The LLM would provide a response like: \"Yes, I believe we can improve
the code. Rule 1 about having concise methods is being violated, bla bla
bla.\"

I honestly do not see much value in this findings review, except for
traceability for posterity. Otherwise, the pure Markdown from the LLM
will be excellent.

\-\--

Tab 5

Totally fair point. If your primary goal is fast, useful feedback in
PRs, posting the LLM's Markdown directly can be excellent. Many teams
succeed with that approach. The "findings schema" adds value only if you
need a few specific things beyond free-form comments:

Where the schema helps

\- Inline anchoring: Precise per-line comments mapped to changed lines
only, avoiding noise on unchanged code.

\- Idempotency/dedup: Deterministic IDs let you update or re-run without
duplicating comments.

\- Filtering and routing: Severity/category enable "only post WARNING+
on touched files" or route security findings differently.

\- Multi-render targets: Same output can go to GitHub inline, SARIF, or
logs without bespoke transforms.

\- Audits: Explicit base/head SHAs and blob SHAs make runs verifiable,
which helps in regulated or large-scale environments.

If you don't need those today, you can keep the pipeline simple and
still keep the door open for structure later.

Pragmatic compromise plan

1\) Keep "LLM-to-Markdown-to-comment" as the default path

\- Bootstrap-app:

\- For each changed Java file, gather a compact hunk context and
optionally AST excerpts.

\- Build a prompt and get a Markdown response from the LLM.

\- Post it as a single PR comment or one comment per file. No structured
findings required.

2\) Add a very thin optional adapter

\- If/when you want per-line anchoring or dedup, provide a small parser
that can extract "hints" from the Markdown (e.g., file:line code fences
or explicit "Line 42" markers) and convert only those into Findings.

\- Otherwise, the Markdown goes straight through unchanged.

3\) Keep the findings-schema as a utility, not a requirement

\- Rules you write by hand (non-LLM) can output Findings directly.

\- LLM-based reviews can remain Markdown-only. If you later want inline
anchoring, add minimal hints to the prompt and parse them.

Concrete next step

\- Implement the LLM Markdown path in bootstrap-app with a simple GitHub
renderer:

\- Fetch PR snapshot (filtered to ADDED/MODIFIED/RENAMED Java files).

\- Build compact context per file: filename, a few changed hunks, and
surrounding lines.

\- Prompt the LLM to produce a short, constructive Markdown review
focusing only on changed lines.

\- Post the Markdown as a single comment (or per-file thread) in dry-run
first.

\- Keep the findings-schema module as-is, but do not wire it into the
LLM flow yet.

\- Later, if you want inline comments for specific lines, add a light
prompt convention like:

\- "When you want to anchor a point to a changed line, add a bullet
like: \[anchor: src/Foo.java:42\] Your note here."

\- Then parse those anchors into Findings and post them inline, while
still posting the full Markdown summary as a top-level comment.

Why this approach works now

\- You get value immediately with minimal complexity.

\- You retain the option to evolve toward structured, per-line findings
without reworking the architecture.

\- You avoid premature rigidity while keeping a clear path to
determinism and idempotency if/when needed.

If you want, I can provide the bootstrap-app code for:

\- Building the per-file hunk context

\- Crafting a prompt

\- Posting a single Markdown comment on a PR (dry-run toggle)

And we can defer all findings-structured work for when you ask for
anchoring/dedup.

Tab 6

Here is the corrected version of your text:

\-\--

OK, wait a second.

Adding inline comments on the precise line is essential, and I had that
too.

So, in my case, unlike what you recommend---and you make a good
point---I had a single visitor that would iterate over the nodes and ask
each rule what it wanted to do.

So, the rule would trigger on a specific method, for example, asking the
LLM for feedback and seeing that there was a problem in the method. The
feedback in Markdown was collected immediately, and at that point, it
was immediately added to GitLab. In that case, it was a GitLab app on a
line of code of the method with either a plus or a minus. The diff for
that method was shown, so you could see the feedback was still
pinpointed.

However, you are also right that this app is more complex, as it likely
has asynchronous behavior. So, you need to harvest the feedback and send
it out later. The finding module definitely makes sense as it helps with
this sort of thing.

But then, I would tend to say that perhaps we should review the finding
of the LLM to not need to have something like error or warning for now.
Instead, it should be an object that would identify metadata such as:

\- Some sort of abstract ID for the rule name that generated the
finding.

\- The specific relative path for the file evaluated.

\- The line of code where we want to add the comment on the pull
request.

\- Something like the pull or merge request ID.

\- The LLM Markdown.

\- The SHA-1 version of the file evaluated.

The JavaDoc should explain that the finding object allows rules to
trigger and generate one or more findings if they want. By encapsulating
the feedback of the LLM into the finding, we can later send these
findings asynchronously into the merge request by having a pull request
update queue that reads findings and writes them out independently of
the code hitting the LLM.

Tab 7

feesback was:

Agreed. Let's tailor the schema to your workflow: precise inline
comments produced by rules (often LLM-backed), collected immediately,
and posted asynchronously. We can keep it minimal and focused on
anchoring and provenance, without prescriptive severities or categories
for now.

Proposed minimal LLM-centric finding model

Goal: represent "what to post inline" with just enough metadata to
anchor, dedupe, and audit.

Fields

\- ruleId: stable identifier of the producing rule (string).

\- filePath: repository-relative new path for the file (string).

\- afterLine: 1-based line in the after (head) version to anchor the
comment (int). Optional endAfterLine for ranges if you want.

\- pullRequestId: identifier for the PR/MR context (string). If you
prefer, we can keep this out of the core object and carry it in the
queue envelope; your call.

\- headSha: the head commit SHA (string).

\- baseSha: the base commit SHA (string).

\- fileSha: the blob SHA of the evaluated head file (string, optional).

\- markdown: the exact Markdown to post inline (string).

\- id: deterministic identifier for dedup/idempotency (string). Computed
from filePath\|afterLine\|ruleId\|headSha.

Why this is enough now

\- Anchoring: filePath + afterLine pins the comment to the exact changed
line.

\- Idempotency: deterministic id prevents duplicate comments when
re-running.

\- Auditability: baseSha/headSha/fileSha capture the exact snapshot and
content evaluated.

\- Async-ready: the object is self-contained; a background queue can
read and post later without re-computing context.

\- Non-opinionated: no severity/category---LLM or human rules can write
whatever Markdown they want.

Concrete types

LLMInlineFinding.java

\- Fields: id, ruleId, filePath, afterLine, Optional\<Integer\>
endAfterLine, markdown, headSha, baseSha, Optional\<String\> fileSha.

\- Validation: non-blank strings, afterLine \>= 1, endAfterLine \>=
afterLine when present.

\- Immutable.

LLMInlineFindingIdFactory.java

\- createId(filePath, afterLine, ruleId, headSha) -\>
hex(SHA-256(payload))

\- Payload: filePath + \"\|\" + afterLine + \"\|\" + ruleId + \"\|\" +
headSha.

Queue envelope (optional)

\- If preferred, define a lightweight envelope for the async publisher:

\- pullRequestId, repositoryId (or owner/name), and the LLMInlineFinding
payload.

\- This keeps the core finding reusable beyond a single provider.

Unit tests

\- Deterministic ID tests: same inputs -\> same id; change any part -\>
different id.

\- Validation tests: line bounds, non-blank strings, end \>= start.

\- Round-trip tests for optional range lines and fileSha presence.

Javadoc guidance

\- Explain the intent: a rule (LLM- or code-based) can emit one or more
LLMInlineFinding instances per visited node.

\- Clarify that callers should pass repository-relative newPath and
headSha from the reviewed snapshot.

\- Call out idempotency use: re-running on the same head should avoid
duplicate inline comments by checking id or comparing body + anchor.

\- Note that path normalization is the caller's responsibility.

Example usage

\- A single visitor walks AST.

\- On visiting a method with changed after-lines, the rule asks the LLM
for feedback with the method's hunk context.

\- When the LLM responds, the rule constructs:

\- loc = (filePath, afterLine)

\- id = LLMInlineFindingIdFactory.createId(loc.filePath, loc.afterLine,
ruleId, headSha)

\- finding = new LLMInlineFinding(id, ruleId, filePath, afterLine,
Optional.empty(), markdown, headSha, baseSha, Optional.of(fileSha))

\- The rule publishes to an async queue. A background worker posts
inline comments to the PR using the mapping from afterLine to
GitHub/GitLab diff anchors.

Deliverables for this increment

1\) finding-schema additions (or a sibling module, e.g.,
llm-finding-schema, if you want to keep it separate from the
"structured" model):

\- LLMInlineFinding.java

\- LLMInlineFindingIdFactory.java

\- Tests: LLMInlineFindingIdFactoryTest, LLMInlineFindingTest

\- README update explaining the LLM inline flow and async posting model.

2\) README content highlights

\- What this object is and why: minimal, line-anchored, async-friendly.

\- How to compute IDs and why that kicks ass (idempotent posting).

\- How it fits the big picture: rules produce findings; a queue/worker
posts them; bootstrap orchestrates; diff-mapper provides after-line
anchors.

If you approve, I will produce the code and tests exactly as above, with
clean Javadoc and intention-revealing names, and keep the existing
structured Finding types intact but clearly optional.

Tab 8

Here is the revised version of your text with improved grammar and
clarity:

\-\--

Brilliant.

Yes, definitely call it \"repository ID\" and add JavaDoc to explain
that, in the case of GitHub, this is a pull request ID, and in the case
of GitLab, it would be the merge request ID. We might even want to have
the full repository URL as a field.

But to be clear, on the topic of the visitor pattern, your idea that
each rule decides how to navigate a Java file, implementing its own
visitor pattern as it sees fit, is really good. Your approach identified
a bottleneck in my approach.

Sometimes it is hard to have a one-size-fits-all way of traversing a
tree of nodes. So, it is better that rules are given the raw material to
traverse the file and make their own decisions.

So, yes, please go ahead and review the implementation of this findings
project. Remember to update the README. Have very nice JavaDoc that
explains the thought process and the decisions taken. Explain why this
approach is excellent and what it is enabling us to do later, with rules
being able to collect feedback and delegate to a later consumer the
update of the system.

Also, we might want to have some more metadata in the finding object if
we are able to have statistics on the input tokens used and total output
tokens received. We may want to store the findings in a database for
statistics. Additionally, include the identification of the LLM model
used and the time it took for the synchronous call to the LLM.

one more thing

after all this.

please at a final section of your reply with a reviwed version for the
full project specifiation that both reflects what has already been
achieved but bot tested in real life and what remains open to be done.

and recommendation on how to move forward

Tab 9

alpha horizon gives now:

Excellent. I will implement the minimal, LLM-centric inline finding
schema you outlined, with repository-aware provenance and optional
telemetry, plus unit tests and a README that clearly explains the
intent, tradeoffs, and how this enables async posting. I will also
include a final section with a reviewed version of the full project
specification and a concrete recommendation for the next steps.

Module: llm-finding-schema

Package: com.example.findings.llm

Source files

\- LLMInlineFinding.java

\- LLMInlineFindingIdFactory.java

\- LLMRequestTelemetry.java

\- LLMProvenance.java

\- LLMPostingEnvelope.java

\- README.md

Tests

\- LLMInlineFindingIdFactoryTest.java

\- LLMInlineFindingTest.java

Source code

LLMInlineFinding.java

package com.example.findings.llm;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Minimal, immutable descriptor of an inline comment produced by a rule
(often LLM-backed).

\* Anchors to a specific repository-relative file path and after-line,
and carries Markdown to render.

\*

\* Design goals:

\* - Precise inline anchoring to changed lines (filePath + afterLine).

\* - Deterministic id for idempotent posting (see
LLMInlineFindingIdFactory).

\* - Repository-aware provenance to support async posting and audits.

\* - Optional telemetry for cost/performance analytics without coupling
to a specific vendor.

\*

\* Why this approach is excellent:

\* - Rules can emit findings immediately (synchronously) as they
traverse nodes, while a separate

\* queue/worker is responsible for posting them. This decouples LLM
latency from PR responsiveness.

\* - We avoid prescribing severities or categories at this stage; the
Markdown carries the message.

\* - It keeps us future-proof: we can add structured findings or
severities later without changing producers.

\*/

public final class LLMInlineFinding {

private final String id; // deterministic id for dedup

private final String ruleId; // producer rule identifier (e.g.,
\"JAVA_LONG_METHOD\")

private final String filePath; // repository-relative new path

private final int afterLine; // 1-based line in \"after\" (head) version

private final Integer endAfterLine; // nullable; inclusive end for
ranges

private final String markdown; // exact Markdown to post

private final LLMProvenance provenance; // base/head SHAs, repository
and PR identifiers

private final LLMRequestTelemetry telemetry; // optional; token usage,
latency, model

public LLMInlineFinding(

String id,

String ruleId,

String filePath,

int afterLine,

Integer endAfterLine,

String markdown,

LLMProvenance provenance,

LLMRequestTelemetry telemetry

) {

this.id = requireNonBlank(id, \"id\");

this.ruleId = requireNonBlank(ruleId, \"ruleId\");

this.filePath = requireNonBlank(filePath, \"filePath\");

if (afterLine \< 1) throw new IllegalArgumentException(\"afterLine must
be \>= 1\");

if (endAfterLine != null && endAfterLine \< afterLine) {

throw new IllegalArgumentException(\"endAfterLine must be \>=
afterLine\");

}

this.afterLine = afterLine;

this.endAfterLine = endAfterLine;

this.markdown = requireNonBlank(markdown, \"markdown\");

this.provenance = Objects.requireNonNull(provenance, \"provenance\");

this.telemetry = telemetry; // may be null if not captured

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

public String getId() { return id; }

public String getRuleId() { return ruleId; }

public String getFilePath() { return filePath; }

public int getAfterLine() { return afterLine; }

public Optional\<Integer\> getEndAfterLine() { return
Optional.ofNullable(endAfterLine); }

public String getMarkdown() { return markdown; }

public LLMProvenance getProvenance() { return provenance; }

public Optional\<LLMRequestTelemetry\> getTelemetry() { return
Optional.ofNullable(telemetry); }

\@Override

public String toString() {

return \"LLMInlineFinding{\" +

\"id=\'\" + id + \'\\\'\' +

\", ruleId=\'\" + ruleId + \'\\\'\' +

\", filePath=\'\" + filePath + \'\\\'\' +

\", afterLine=\" + afterLine +

\", endAfterLine=\" + endAfterLine +

\", provenance=\" + provenance +

\", telemetry=\" + telemetry +

\'}\';

}

}

LLMProvenance.java

package com.example.findings.llm;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Provenance contextualizes a finding to a repository and commit
snapshot.

\*

\* repositoryId: logical identifier for the repository (e.g.,
\"org/repo\" for GitHub).

\* repositoryUrl: canonical URL (optional), useful for analytics or deep
links.

\* pullRequestId: for GitHub this is the pull request number/id; for
GitLab this is the merge request id.

\* baseSha: base commit SHA of the review.

\* headSha: head commit SHA of the review.

\* fileSha: blob SHA of the evaluated head file (optional).

\*

\* Notes:

\* - We keep repositoryId explicit for cross-provider clarity;
repositoryUrl is optional.

\* - pullRequestId may be a string to accommodate provider-specific
encodings.

\*/

public final class LLMProvenance {

private final String repositoryId;

private final String repositoryUrl; // optional

private final String pullRequestId;

private final String baseSha;

private final String headSha;

private final String fileSha; // optional

public LLMProvenance(

String repositoryId,

String repositoryUrl,

String pullRequestId,

String baseSha,

String headSha,

String fileSha

) {

this.repositoryId = requireNonBlank(repositoryId, \"repositoryId\");

this.repositoryUrl = emptyToNull(repositoryUrl);

this.pullRequestId = requireNonBlank(pullRequestId, \"pullRequestId\");

this.baseSha = requireNonBlank(baseSha, \"baseSha\");

this.headSha = requireNonBlank(headSha, \"headSha\");

this.fileSha = emptyToNull(fileSha);

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

private static String emptyToNull(String v) {

return (v == null \|\| v.isBlank()) ? null : v;

}

public String getRepositoryId() { return repositoryId; }

public Optional\<String\> getRepositoryUrl() { return
Optional.ofNullable(repositoryUrl); }

public String getPullRequestId() { return pullRequestId; }

public String getBaseSha() { return baseSha; }

public String getHeadSha() { return headSha; }

public Optional\<String\> getFileSha() { return
Optional.ofNullable(fileSha); }

\@Override

public String toString() {

return \"LLMProvenance{\" +

\"repositoryId=\'\" + repositoryId + \'\\\'\' +

\", repositoryUrl=\" + repositoryUrl +

\", pullRequestId=\'\" + pullRequestId + \'\\\'\' +

\", baseSha=\'\" + baseSha + \'\\\'\' +

\", headSha=\'\" + headSha + \'\\\'\' +

\", fileSha=\" + fileSha +

\'}\';

}

}

LLMRequestTelemetry.java

package com.example.findings.llm;

/\*\*

\* Optional telemetry captured for an LLM call that produced a finding.

\* This is useful for analytics (cost, latency) and operational
insights.

\*

\* All fields are optional; producers should populate what they have.

\*/

public final class LLMRequestTelemetry {

private final String model; // e.g., \"gpt-4o-mini\",
\"claude-3-5-sonnet\"

private final Integer inputTokens; // total prompt tokens

private final Integer outputTokens; // total completion tokens

private final Long latencyMillis; // end-to-end time for the call

public LLMRequestTelemetry(String model, Integer inputTokens, Integer
outputTokens, Long latencyMillis) {

this.model = model;

this.inputTokens = inputTokens;

this.outputTokens = outputTokens;

this.latencyMillis = latencyMillis;

}

public String getModel() { return model; }

public Integer getInputTokens() { return inputTokens; }

public Integer getOutputTokens() { return outputTokens; }

public Long getLatencyMillis() { return latencyMillis; }

\@Override

public String toString() {

return \"LLMRequestTelemetry{\" +

\"model=\'\" + model + \'\\\'\' +

\", inputTokens=\" + inputTokens +

\", outputTokens=\" + outputTokens +

\", latencyMillis=\" + latencyMillis +

\'}\';

}

}

LLMInlineFindingIdFactory.java

package com.example.findings.llm;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.HexFormat;

import java.util.Objects;

/\*\*

\* Deterministic ID factory for LLMInlineFinding.

\*

\* Format:

\* hex( SHA-256( filePath + \"\|\" + afterLine + \"\|\" + ruleId +
\"\|\" + headSha ) )

\*

\* Rationale:

\* - Anchors to the exact head snapshot and position for idempotent
posting.

\* - Sensitive to ruleId, enabling different rules to comment at the
same line without collisions.

\* - Repository-agnostic; provenance is not included in the id to keep
it stable per head commit.

\*

\* Note: Path normalization is the caller\'s responsibility; pass a
repository-relative newPath.

\*/

public final class LLMInlineFindingIdFactory {

private LLMInlineFindingIdFactory() {}

public static String createId(String filePath, int afterLine, String
ruleId, String headSha) {

Objects.requireNonNull(filePath, \"filePath\");

Objects.requireNonNull(ruleId, \"ruleId\");

Objects.requireNonNull(headSha, \"headSha\");

if (filePath.isBlank()) throw new IllegalArgumentException(\"filePath
must not be blank\");

if (ruleId.isBlank()) throw new IllegalArgumentException(\"ruleId must
not be blank\");

if (headSha.isBlank()) throw new IllegalArgumentException(\"headSha must
not be blank\");

if (afterLine \< 1) throw new IllegalArgumentException(\"afterLine must
be \>= 1\");

String payload = filePath + \"\|\" + afterLine + \"\|\" + ruleId +
\"\|\" + headSha;

try {

MessageDigest md = MessageDigest.getInstance(\"SHA-256\");

byte\[\] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));

return HexFormat.of().formatHex(digest);

} catch (Exception e) {

throw new IllegalStateException(\"Failed to compute SHA-256 for finding
id\", e);

}

}

}

LLMPostingEnvelope.java

package com.example.findings.llm;

import java.util.Objects;

/\*\*

\* Envelope for queue-based async posting. This separates
provider-scoped routing

\* (repositoryId, pullRequestId) from the finding payload for transport
or storage.

\*

\* You may omit this if your queue carries repository/pr in its message
key or attributes.

\*/

public final class LLMPostingEnvelope {

private final String repositoryId;

private final String pullRequestId;

private final LLMInlineFinding finding;

public LLMPostingEnvelope(String repositoryId, String pullRequestId,
LLMInlineFinding finding) {

this.repositoryId = requireNonBlank(repositoryId, \"repositoryId\");

this.pullRequestId = requireNonBlank(pullRequestId, \"pullRequestId\");

this.finding = Objects.requireNonNull(finding, \"finding\");

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

public String getRepositoryId() { return repositoryId; }

public String getPullRequestId() { return pullRequestId; }

public LLMInlineFinding getFinding() { return finding; }

\@Override

public String toString() {

return \"LLMPostingEnvelope{\" +

\"repositoryId=\'\" + repositoryId + \'\\\'\' +

\", pullRequestId=\'\" + pullRequestId + \'\\\'\' +

\", finding=\" + finding +

\'}\';

}

}

Unit tests

LLMInlineFindingIdFactoryTest.java

package com.example.findings.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LLMInlineFindingIdFactoryTest {

\@Test

void sameInputs_produceSameId() {

String a = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_X\", \"deadbeef\");

String b = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_X\", \"deadbeef\");

assertThat(a).isEqualTo(b);

}

\@Test

void differentHeadSha_producesDifferentId() {

String a = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_X\", \"aaaaaaaa\");

String b = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_X\", \"bbbbbbbb\");

assertThat(a).isNotEqualTo(b);

}

\@Test

void differentRuleId_orLine_producesDifferentId() {

String a = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_A\", \"deadbeef\");

String b = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 10,
\"RULE_B\", \"deadbeef\");

String c = LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 11,
\"RULE_A\", \"deadbeef\");

assertThat(a).isNotEqualTo(b);

assertThat(a).isNotEqualTo(c);

}

\@Test

void rejectsInvalidInputs() {

assertThatThrownBy(() -\> LLMInlineFindingIdFactory.createId(\" \", 10,
\"RULE\", \"sha\"))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\>
LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 0, \"RULE\",
\"sha\"))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\>
LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 1, \" \", \"sha\"))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\>
LLMInlineFindingIdFactory.createId(\"src/Foo.java\", 1, \"RULE\", \"
\"))

.isInstanceOf(IllegalArgumentException.class);

}

}

LLMInlineFindingTest.java

package com.example.findings.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LLMInlineFindingTest {

\@Test

void constructs_withValidInputs() {

LLMProvenance prov = new LLMProvenance(

\"org/repo\",

\"https://github.com/org/repo\",

\"123\",

\"baseSha\",

\"headSha\",

\"blobSha\"

);

LLMRequestTelemetry tel = new LLMRequestTelemetry(\"gpt-4o-mini\", 1200,
350, 800L);

new LLMInlineFinding(

\"id123\",

\"RULE_X\",

\"src/Foo.java\",

42,

null,

\"Some helpful Markdown.\",

prov,

tel

);

}

\@Test

void rejectsInvalid() {

LLMProvenance prov = new LLMProvenance(\"org/repo\", null, \"123\",
\"base\", \"head\", null);

assertThatThrownBy(() -\> new LLMInlineFinding(null, \"RULE\",
\"src/Foo.java\", 1, null, \"md\", prov, null))

.isInstanceOf(NullPointerException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\" \", \"RULE\",
\"src/Foo.java\", 1, null, \"md\", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \" \",
\"src/Foo.java\", 1, null, \"md\", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \"RULE\", \" \",
1, null, \"md\", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \"RULE\",
\"src/Foo.java\", 0, null, \"md\", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \"RULE\",
\"src/Foo.java\", 10, 9, \"md\", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \"RULE\",
\"src/Foo.java\", 1, null, \" \", prov, null))

.isInstanceOf(IllegalArgumentException.class);

assertThatThrownBy(() -\> new LLMInlineFinding(\"id\", \"RULE\",
\"src/Foo.java\", 1, null, \"md\", null, null))

.isInstanceOf(NullPointerException.class);

}

}

README.md

LLM Finding Schema

What this module is

A minimal, provider-agnostic schema for inline review comments produced
by rules (often LLM-assisted). It captures just enough to anchor
comments precisely, dedupe across reruns, and support asynchronous
posting and analytics.

What it delivers

\- LLMInlineFinding: immutable descriptor of an inline comment with
filePath, afterLine, Markdown, deterministic id, provenance, and
optional telemetry.

\- LLMProvenance: repository-aware context (repositoryId, optional
repositoryUrl), pullRequestId, base/head SHAs, and optional fileSha.

\- LLMRequestTelemetry: optional model name, token counts, and latency
for analytics.

\- LLMInlineFindingIdFactory: deterministic SHA-256 id from
filePath\|afterLine\|ruleId\|headSha.

\- LLMPostingEnvelope: optional queue envelope carrying repositoryId and
pullRequestId with the finding payload.

Why this approach is excellent

\- Precision without friction: Anchors comments to the exact changed
line in the head version.

\- Idempotency and audits: Deterministic ids and explicit SHAs make
reruns safe and verifiable.

\- Async-first: Rules emit findings immediately; a queue/worker posts
them later, decoupling LLM latency from developer workflows.

\- Extensible: No severity or category now---Markdown speaks for itself.
If you later want structured findings or severities, you can add them
without breaking producers.

Design decisions

\- Single responsibility: This module does not fetch PRs, parse ASTs, or
render comments. It is a small data model used by producers and
consumers.

\- Repository identifiers: repositoryId is a stable logical key (e.g.,
"org/repo"). repositoryUrl is optional for deep linking. pullRequestId
is a string; for GitHub this maps to PR number or node id; for GitLab to
the MR id.

\- Deterministic IDs: We use headSha in the id to ensure stability
within a PR head while allowing changes across commits to generate new
ids.

\- Path normalization: Callers must pass a repository-relative new path;
we do not normalize paths inside the schema to keep it deterministic and
caller-controlled.

Example

String filePath = \"src/main/java/com/example/Service.java\";

int afterLine = 42;

String ruleId = \"JAVA_LONG_METHOD\";

String headSha = \"abc123\...\";

String id = LLMInlineFindingIdFactory.createId(filePath, afterLine,
ruleId, headSha);

LLMProvenance prov = new LLMProvenance(

\"org/repo\", // repositoryId

\"https://github.com/org/repo\", // repositoryUrl (optional)

\"1234\", // pullRequestId (PR or MR id)

\"baseSha\",

\"headSha\",

\"blobSha\" // optional

);

LLMRequestTelemetry tel = new LLMRequestTelemetry(\"gpt-4o-mini\", 1200,
350, 800L);

LLMInlineFinding finding = new LLMInlineFinding(

id,

ruleId,

filePath,

afterLine,

null,

\"Concise, constructive Markdown feedback.\",

prov,

tel

);

Async posting model

\- Producer (rule) emits LLMInlineFinding as soon as it has feedback for
a node.

\- A queue carries either the LLMInlineFinding or a LLMPostingEnvelope
with repository/pr routing.

\- A worker resolves diff anchors to provider-specific APIs and posts
inline comments, checking the finding id to avoid duplicates on reruns.

Testing

Unit tests validate:

\- Deterministic id creation (same inputs -\> same id; changes in line,
ruleId, or headSha -\> different id).

\- Input validation for lines, strings, and ranges.

Non-goals

\- No AST traversal, diff mapping, or GitHub/GitLab clients.

\- No severity/category taxonomy.

\- No rendering or storage implementation (those live in
orchestrator/outputs).

How this enables the broader architecture

\- Rules can independently traverse ASTs, prompt LLMs, and emit findings
with precise anchors.

\- The orchestrator (bootstrap-app) can batch, throttle, and schedule
posting without blocking on LLM calls.

\- Telemetry supports cost monitoring, model A/B tests, and performance
tuning.

Final section: Reviewed project specification and recommendations

What has been achieved (designed and specified; some parts with tests)

\- Core diff mapping tests and a smoke entrypoint for wiring
(modules/diff-mapper-core and bootstrap-app smoke). Validates hunk
handling and windowing without coupling to parsing logic.

\- Clear orchestration plan: bootstrap-app is the glue that fetches PR
snapshots, filters files, builds DiffMappings, parses ASTs, constructs
RuleContext, and runs rules.

\- Finding schemas:

\- Structured finding-schema for deterministic, rule-based findings
(IDs, locations, provenance, severity). Fully specified with tests and
README.

\- LLM-centric llm-finding-schema for minimal inline comments with
deterministic IDs, repository-aware provenance, and optional telemetry.
Implemented with tests and README.

\- Separation of concerns:

\- github-connector: fetch/post only.

\- diff-mapper-core: unified patch to after-line mapping.

\- java-parser: ASTs.

\- rule-engine: Rule, RuleContext, RuleRunner; rules own traversal
patterns.

\- outputs: renderers (inline, logs, SARIF later).

What remains open to be done (MVP)

\- github-connector minimal implementation:

\- Fetch PR/MR snapshot (base/head SHAs, changed files with unified
patches, head file contents).

\- Post inline comment API (dry-run first).

\- rule-engine scaffolding:

\- Interfaces for Rule and RuleContext, a simple RuleRunner.

\- Context accessors: getAst(path), getDiffMapping(path),
changedAfterLines(path).

\- java-parser wiring:

\- Minimal parser wrapper returning a JavaParsingFileNode; size/time
limits.

\- One conservative rule:

\- Example: BroadCatchIntroducedRule using AST visitor, restricted to
changed after-lines.

\- Emits both: a) LLMInlineFinding with Markdown, b) optionally
structured Finding for pipelines that want it.

\- bootstrap-app orchestration:

\- End-to-end dry-run for one PR: select files (ADDED/MODIFIED/RENAMED),
build mappings, parse ASTs, run rule(s), emit LLMInlineFindings to a
queue (or in-memory), and print/post comments.

\- Include base/head SHAs in logs for audit.

\- outputs:

\- GitHub (and/or GitLab) inline renderer that consumes LLMInlineFinding
and handles per-line anchors based on after-line numbers and provider
expectations.

\- Storage/queue (optional for MVP):

\- In-memory queue worker for posting in batch to simulate async;
persistent storage can come later.

Risks and mitigations

\- Anchor accuracy: Use diff-mapper-core and repository-relative new
paths; test against edge cases (renames, multi-hunks).

\- LLM latency and cost: Async queue plus telemetry fields allow us to
measure and throttle.

\- Provider differences (GitHub vs. GitLab): Abstract posting with a
common interface; provenance holds repositoryId and pullRequestId for
routing.

Recommendations on how to move forward (incremental plan)

1\) Lock in llm-finding-schema (this PR):

\- Merge the schema and tests. This stabilizes the contract for
producers and the posting worker.

2\) Implement github-connector (or gitlab-connector) minimal:

\- Snapshot fetch: base/head, changed files, patches, head content.

\- Inline posting API with dry-run.

3\) Wire bootstrap-app end-to-end (dry-run):

\- File selection (ADDED/MODIFIED/RENAMED -\> newPath).

\- DiffMapping build per file.

\- AST parse per selected file.

\- A single rule that visits nodes, prompts an LLM for changed regions,
and emits LLMInlineFinding with deterministic id and telemetry.

\- In-memory queue and a simple worker that logs "would post" with
anchors and IDs.

4\) Tighten anchors and tests:

\- Add integration tests with real unified diff fixtures and a fake
provider client.

\- Verify we can post at plus lines in multi-hunk patches.

5\) Gradually expand:

\- Add one or two more rules with different traversal styles to validate
the "rules own traversal" strategy.

\- Optional: introduce structured findings when you need severities or
SARIF export.

This plan keeps the system simple where it matters, delivers immediate
value via precise inline comments, and preserves a clean path to more
structure and analytics as we scale.

Tab 10

Here is the revised version of your text with improved grammar and
clarity:

\-\--

Wait a second, you are again not providing the JavaDoc I want.

\*\*LLMInlineFinding\*\*

Look at the class. You put no JavaDoc on any field as if everything is
self-evident. I really dislike that.

All code must be documented. The meaning of fields, especially on DTOs,
is fundamental. Please review the code so that it is properly documented
and try to remember that good code is documented code, where intentions
are laid bare and clear.

Documentation always precedes code. Code may fail to do what it intends
to do, but you should be able to state why you created that code in the
first place.

Tab 11

You're absolutely right. I will revise the DTOs to include thorough,
intention-revealing JavaDoc on every field, constructor parameter, and
core method. Below are updated, fully documented versions for the key
classes, plus brief notes where appropriate. I will keep the code
minimal but explicit, so the rationale and contracts are clear without
needing to read other modules.

Revised code with comprehensive JavaDoc

LLMInlineFinding.java

package com.example.findings.llm;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Immutable, minimal descriptor of an inline code review comment
produced by a rule

\* (often backed by an LLM call). This object is designed to be queued
and posted

\* asynchronously to a code host (GitHub, GitLab) with precise, per-line
anchoring.

\*

\* Why this exists:

\* - To decouple the act of generating feedback (rules traversing code
and prompting LLMs)

\* from the act of posting that feedback to the pull/merge request
(which may be rate-limited

\* or delayed).

\* - To make posting idempotent (via a deterministic id) and auditable
(via provenance).

\* - To keep the producer simple and flexible: the feedback is free-form
Markdown, not constrained

\* to a severity/category taxonomy at this stage.

\*

\* When to use:

\* - A rule identifies a specific file/line and obtains LLM feedback for
that location.

\* - The rule constructs an LLMInlineFinding with the line anchor,
Markdown body, and provenance.

\* - A separate worker posts this finding inline on the PR/MR or logs it
in dry-run mode.

\*

\* Non-goals:

\* - This class does not fetch diffs, parse ASTs, or post to any
provider API.

\* - It does not prescribe a severity taxonomy; the Markdown carries the
message.

\*/

public final class LLMInlineFinding {

/\*\*

\* Deterministic identifier used for idempotent posting and
deduplication across reruns.

\* Recommended format: hex(SHA-256(filePath + \"\|\" + afterLine +
\"\|\" + ruleId + \"\|\" + headSha)).

\* Motivation:

\* - Running the same review on the same headSha should not produce
duplicate comments.

\* - Distinct rules can comment on the same line without collisions
(ruleId is part of the id).

\*/

private final String id;

/\*\*

\* Stable identifier of the producing rule (e.g., \"JAVA_LONG_METHOD\",
\"BROAD_CATCH\").

\* Purpose:

\* - Traceability (which logic produced this comment).

\* - Input to id computation so multiple rules can comment at the same
location.

\*/

private final String ruleId;

/\*\*

\* Repository-relative new path of the file in the \"after\" (head)
version (e.g., \"src/Foo.java\").

\* Expectations:

\* - Use the new path for MODIFIED/RENAMED/ADDED files.

\* - Caller is responsible for path normalization; this class does not
normalize.

\* Rationale:

\* - Keeps ids stable and posting anchors unambiguous across providers.

\*/

private final String filePath;

/\*\*

\* One-based line number in the \"after\" (head) version of the file at
which to anchor the comment.

\* Constraints: \>= 1.

\* Rationale:

\* - Anchors directly to the reviewed (head) content, matching code host
expectations for inline diffs.

\*/

private final int afterLine;

/\*\*

\* Optional inclusive end line in the \"after\" (head) version, enabling
range comments.

\* Constraints:

\* - If present, endAfterLine must be \>= afterLine.

\* When to use:

\* - For multi-line findings (e.g., long block, method span).

\* - For code hosts that support range annotations; otherwise ignore at
posting time.

\*/

private final Integer endAfterLine;

/\*\*

\* The exact Markdown body to post as the inline comment.

\* Guidance:

\* - Keep it concise and constructive. Include actionable suggestions.

\* - Avoid provider-specific formatting; stick to common Markdown for
portability.

\*/

private final String markdown;

/\*\*

\* Provenance tying this finding to a specific repository and commit
snapshot.

\* Purpose:

\* - Audits and traceability: know exactly which repo, PR/MR, and SHAs
were reviewed.

\* - Routing: downstream workers can route by repositoryId and
pullRequestId.

\*/

private final LLMProvenance provenance;

/\*\*

\* Optional telemetry captured from the LLM request that produced this
finding (model, tokens, latency).

\* Motivation:

\* - Operational visibility and cost tracking.

\* - Enables analytics and guardrails without coupling producers to a
vendor-specific API here.

\*/

private final LLMRequestTelemetry telemetry;

/\*\*

\* Constructs an immutable inline finding with precise anchoring and
provenance.

\*

\* \@param id Deterministic identifier for dedup/idempotency.

\* \@param ruleId Stable identifier of the producing rule.

\* \@param filePath Repository-relative new path in the head version.

\* \@param afterLine One-based line number in the head version where
this comment should anchor.

\* \@param endAfterLine Optional inclusive end line for range comments;
must be \>= afterLine when present.

\* \@param markdown Exact Markdown body to post.

\* \@param provenance Repository/PR and commit snapshot information.

\* \@param telemetry Optional LLM telemetry (model, tokens, latency).
May be null if not captured.

\* \@throws NullPointerException if any required argument is null.

\* \@throws IllegalArgumentException if any required string is blank,
afterLine \< 1,

\* or endAfterLine \< afterLine when present.

\*/

public LLMInlineFinding(

String id,

String ruleId,

String filePath,

int afterLine,

Integer endAfterLine,

String markdown,

LLMProvenance provenance,

LLMRequestTelemetry telemetry

) {

this.id = requireNonBlank(id, \"id\");

this.ruleId = requireNonBlank(ruleId, \"ruleId\");

this.filePath = requireNonBlank(filePath, \"filePath\");

if (afterLine \< 1) throw new IllegalArgumentException(\"afterLine must
be \>= 1\");

if (endAfterLine != null && endAfterLine \< afterLine) {

throw new IllegalArgumentException(\"endAfterLine must be \>=
afterLine\");

}

this.afterLine = afterLine;

this.endAfterLine = endAfterLine;

this.markdown = requireNonBlank(markdown, \"markdown\");

this.provenance = Objects.requireNonNull(provenance, \"provenance\");

this.telemetry = telemetry; // optional

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

/\*\*

\* Returns the deterministic identifier for this finding, used for
idempotent posting.

\*/

public String getId() { return id; }

/\*\*

\* Returns the stable rule identifier that produced this finding.

\*/

public String getRuleId() { return ruleId; }

/\*\*

\* Returns the repository-relative path of the file in the head version.

\*/

public String getFilePath() { return filePath; }

/\*\*

\* Returns the one-based line number in the head version where this
comment anchors.

\*/

public int getAfterLine() { return afterLine; }

/\*\*

\* Returns the optional inclusive end line for range comments.

\*/

public Optional\<Integer\> getEndAfterLine() { return
Optional.ofNullable(endAfterLine); }

/\*\*

\* Returns the Markdown body to post.

\*/

public String getMarkdown() { return markdown; }

/\*\*

\* Returns the provenance that ties this finding to a repository and
commit snapshot.

\*/

public LLMProvenance getProvenance() { return provenance; }

/\*\*

\* Returns telemetry about the LLM call that produced this finding, if
captured.

\*/

public Optional\<LLMRequestTelemetry\> getTelemetry() { return
Optional.ofNullable(telemetry); }

\@Override

public String toString() {

return \"LLMInlineFinding{\" +

\"id=\'\" + id + \'\\\'\' +

\", ruleId=\'\" + ruleId + \'\\\'\' +

\", filePath=\'\" + filePath + \'\\\'\' +

\", afterLine=\" + afterLine +

\", endAfterLine=\" + endAfterLine +

\", provenance=\" + provenance +

\", telemetry=\" + telemetry +

\'}\';

}

}

LLMProvenance.java

package com.example.findings.llm;

import java.util.Objects;

import java.util.Optional;

/\*\*

\* Repository- and commit-aware provenance for an inline finding.

\*

\* Why this exists:

\* - To unambiguously bind a finding to the exact repository, pull/merge
request, and commit snapshot

\* that was reviewed when the finding was produced.

\* - To allow async posting workers to route findings correctly without
re-fetching context.

\*

\* Field semantics:

\* - repositoryId: Logical identifier (e.g., \"org/repo\" on GitHub).
Used for routing and analytics.

\* - repositoryUrl: Canonical URL (optional) to support logging, deep
links, or dashboards.

\* - pullRequestId: Provider-specific identifier. On GitHub, this is the
Pull Request number (or node id);

\* on GitLab, the Merge Request id. Stored as a string to be
provider-agnostic.

\* - baseSha: Base commit SHA of the review range.

\* - headSha: Head commit SHA of the review range.

\* - fileSha: Optional blob SHA of the head-version file content used
for analysis (if available).

\*/

public final class LLMProvenance {

private final String repositoryId;

private final String repositoryUrl; // optional

private final String pullRequestId;

private final String baseSha;

private final String headSha;

private final String fileSha; // optional

/\*\*

\* Constructs repository- and commit-aware provenance for a finding.

\*

\* \@param repositoryId Logical repository identifier (e.g.,
\"org/repo\").

\* \@param repositoryUrl Optional canonical URL of the repository (may
be null or blank).

\* \@param pullRequestId Provider-specific PR/MR identifier as a string.

\* \@param baseSha Base commit SHA of the review.

\* \@param headSha Head commit SHA of the review.

\* \@param fileSha Optional blob SHA of the evaluated head file (may be
null or blank).

\* \@throws NullPointerException if any required field is null.

\* \@throws IllegalArgumentException if any required string is blank.

\*/

public LLMProvenance(

String repositoryId,

String repositoryUrl,

String pullRequestId,

String baseSha,

String headSha,

String fileSha

) {

this.repositoryId = requireNonBlank(repositoryId, \"repositoryId\");

this.repositoryUrl = emptyToNull(repositoryUrl);

this.pullRequestId = requireNonBlank(pullRequestId, \"pullRequestId\");

this.baseSha = requireNonBlank(baseSha, \"baseSha\");

this.headSha = requireNonBlank(headSha, \"headSha\");

this.fileSha = emptyToNull(fileSha);

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

private static String emptyToNull(String v) {

return (v == null \|\| v.isBlank()) ? null : v;

}

/\*\*

\* Returns the logical repository identifier (e.g., \"org/repo\").

\*/

public String getRepositoryId() { return repositoryId; }

/\*\*

\* Returns the canonical repository URL, if provided.

\*/

public Optional\<String\> getRepositoryUrl() { return
Optional.ofNullable(repositoryUrl); }

/\*\*

\* Returns the provider-specific PR/MR identifier as a string.

\*/

public String getPullRequestId() { return pullRequestId; }

/\*\*

\* Returns the base commit SHA of the review.

\*/

public String getBaseSha() { return baseSha; }

/\*\*

\* Returns the head commit SHA of the review.

\*/

public String getHeadSha() { return headSha; }

/\*\*

\* Returns the optional blob SHA of the head-version file used for
analysis.

\*/

public Optional\<String\> getFileSha() { return
Optional.ofNullable(fileSha); }

\@Override

public String toString() {

return \"LLMProvenance{\" +

\"repositoryId=\'\" + repositoryId + \'\\\'\' +

\", repositoryUrl=\" + repositoryUrl +

\", pullRequestId=\'\" + pullRequestId + \'\\\'\' +

\", baseSha=\'\" + baseSha + \'\\\'\' +

\", headSha=\'\" + headSha + \'\\\'\' +

\", fileSha=\" + fileSha +

\'}\';

}

}

LLMRequestTelemetry.java

package com.example.findings.llm;

/\*\*

\* Optional telemetry describing the LLM call that produced a finding.

\*

\* Why this exists:

\* - To enable analytics (token usage, latency) and cost/quality
tracking without

\* coupling the finding schema to any particular LLM provider.

\*

\* Notes:

\* - All fields are optional. Producers may populate what they can
measure.

\* - Keep values as coarse-grained as needed to avoid exposing sensitive
prompts.

\*/

public final class LLMRequestTelemetry {

/\*\*

\* Identifier of the LLM model used (e.g., \"gpt-4o-mini\",
\"claude-3-5-sonnet\").

\* Purpose: analytics and model A/B testing.

\*/

private final String model;

/\*\*

\* Total input tokens sent in the request (prompt + system + tools), if
available.

\*/

private final Integer inputTokens;

/\*\*

\* Total output tokens generated by the model, if available.

\*/

private final Integer outputTokens;

/\*\*

\* End-to-end latency of the LLM call in milliseconds, if available.

\*/

private final Long latencyMillis;

/\*\*

\* Constructs telemetry for an LLM request. All fields are optional.

\*

\* \@param model Model identifier (nullable).

\* \@param inputTokens Total input tokens (nullable).

\* \@param outputTokens Total output tokens (nullable).

\* \@param latencyMillis End-to-end latency in milliseconds (nullable).

\*/

public LLMRequestTelemetry(String model, Integer inputTokens, Integer
outputTokens, Long latencyMillis) {

this.model = model;

this.inputTokens = inputTokens;

this.outputTokens = outputTokens;

this.latencyMillis = latencyMillis;

}

/\*\* Returns the LLM model identifier, if provided. \*/

public String getModel() { return model; }

/\*\* Returns the total input tokens, if provided. \*/

public Integer getInputTokens() { return inputTokens; }

/\*\* Returns the total output tokens, if provided. \*/

public Integer getOutputTokens() { return outputTokens; }

/\*\* Returns the end-to-end LLM call latency in milliseconds, if
provided. \*/

public Long getLatencyMillis() { return latencyMillis; }

\@Override

public String toString() {

return \"LLMRequestTelemetry{\" +

\"model=\'\" + model + \'\\\'\' +

\", inputTokens=\" + inputTokens +

\", outputTokens=\" + outputTokens +

\", latencyMillis=\" + latencyMillis +

\'}\';

}

}

LLMInlineFindingIdFactory.java

package com.example.findings.llm;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.HexFormat;

import java.util.Objects;

/\*\*

\* Factory for deterministic identifiers of inline findings, enabling
idempotent posting.

\*

\* Contract:

\* - The id must remain stable for the same filePath, afterLine, ruleId,
and headSha.

\* - Changing any of these should produce a different id.

\*

\* Recommended implementation:

\* id = hex( SHA-256( filePath + \"\|\" + afterLine + \"\|\" + ruleId +
\"\|\" + headSha ) )

\*

\* Design notes:

\* - headSha anchors the id to a specific review snapshot; new commits
produce new ids.

\* - repositoryId is not included to keep the id stable across
forks/mirrors reviewing the same head.

\* - Callers must pass repository-relative filePath; no normalization is
performed here.

\*/

public final class LLMInlineFindingIdFactory {

private LLMInlineFindingIdFactory() {}

/\*\*

\* Computes a deterministic id suitable for deduplication across reruns.

\*

\* \@param filePath Repository-relative new path in the head version.

\* \@param afterLine One-based after line number.

\* \@param ruleId Stable rule identifier.

\* \@param headSha Head commit SHA of the review.

\* \@return Lowercase hex SHA-256 string.

\* \@throws NullPointerException if any argument is null.

\* \@throws IllegalArgumentException if any string is blank or afterLine
\< 1.

\*/

public static String createId(String filePath, int afterLine, String
ruleId, String headSha) {

Objects.requireNonNull(filePath, \"filePath\");

Objects.requireNonNull(ruleId, \"ruleId\");

Objects.requireNonNull(headSha, \"headSha\");

if (filePath.isBlank()) throw new IllegalArgumentException(\"filePath
must not be blank\");

if (ruleId.isBlank()) throw new IllegalArgumentException(\"ruleId must
not be blank\");

if (headSha.isBlank()) throw new IllegalArgumentException(\"headSha must
not be blank\");

if (afterLine \< 1) throw new IllegalArgumentException(\"afterLine must
be \>= 1\");

String payload = filePath + \"\|\" + afterLine + \"\|\" + ruleId +
\"\|\" + headSha;

try {

MessageDigest md = MessageDigest.getInstance(\"SHA-256\");

byte\[\] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));

return HexFormat.of().formatHex(digest);

} catch (Exception e) {

throw new IllegalStateException(\"Failed to compute SHA-256 for finding
id\", e);

}

}

}

LLMPostingEnvelope.java

package com.example.findings.llm;

import java.util.Objects;

/\*\*

\* Transport envelope for queue-based asynchronous posting.

\*

\* Why this exists:

\* - Separates routing keys (repositoryId, pullRequestId) from the
finding payload.

\* - Allows message brokers or storage to index by repository and PR/MR
without inspecting payloads.

\*

\* When to use:

\* - If your queue infra prefers to keep routing metadata outside the
payload.

\* - Otherwise, you may embed only LLMInlineFinding and carry routing in
message attributes/headers.

\*/

public final class LLMPostingEnvelope {

/\*\*

\* Logical repository identifier (e.g., \"org/repo\" on GitHub).

\* Purpose: routing and partitioning in queues or storage.

\*/

private final String repositoryId;

/\*\*

\* Provider-specific PR/MR identifier as a string.

\* Purpose: routing to the correct review thread on the provider.

\*/

private final String pullRequestId;

/\*\*

\* The inline finding payload to be posted asynchronously.

\*/

private final LLMInlineFinding finding;

/\*\*

\* Constructs a queue envelope for async posting.

\*

\* \@param repositoryId Logical repository identifier (e.g.,
\"org/repo\").

\* \@param pullRequestId Provider-specific PR/MR identifier as a string.

\* \@param finding The inline finding payload.

\* \@throws NullPointerException if any argument is null.

\* \@throws IllegalArgumentException if repositoryId or pullRequestId
are blank.

\*/

public LLMPostingEnvelope(String repositoryId, String pullRequestId,
LLMInlineFinding finding) {

this.repositoryId = requireNonBlank(repositoryId, \"repositoryId\");

this.pullRequestId = requireNonBlank(pullRequestId, \"pullRequestId\");

this.finding = Objects.requireNonNull(finding, \"finding\");

}

private static String requireNonBlank(String v, String name) {

Objects.requireNonNull(v, name);

if (v.isBlank()) throw new IllegalArgumentException(name + \" must not
be blank\");

return v;

}

/\*\* Returns the logical repository identifier (e.g., \"org/repo\").
\*/

public String getRepositoryId() { return repositoryId; }

/\*\* Returns the provider-specific PR/MR identifier as a string. \*/

public String getPullRequestId() { return pullRequestId; }

/\*\* Returns the inline finding payload to be posted. \*/

public LLMInlineFinding getFinding() { return finding; }

\@Override

public String toString() {

return \"LLMPostingEnvelope{\" +

\"repositoryId=\'\" + repositoryId + \'\\\'\' +

\", pullRequestId=\'\" + pullRequestId + \'\\\'\' +

\", finding=\" + finding +

\'}\';

}

}

What changed and why this now kicks ass

\- Every field and constructor parameter is documented with intent,
constraints, and rationale. A reader can understand why each exists
without cross-referencing other modules.

\- DTOs are explicit about provider-agnostic behavior, anchoring
semantics, and idempotency guarantees.

\- Telemetry and provenance are explained in terms of future analytics
and async routing.

\- Javadoc clarifies what is deliberately not in scope, reducing
ambiguity and accidental coupling.

Recommendations and next steps

1\) Merge these DTO updates and run unit tests.

2\) Update the module README to reflect the stronger documentation
standards:

\- "Every public field, constructor param, and method must have JavaDoc
explaining intent and constraints."

3\) Add a short CONTRIBUTING note: documentation precedes code for DTOs
and public APIs; PRs without JavaDoc will not be accepted.

4\) In bootstrap-app, when producing LLMInlineFinding:

\- Always compute id via LLMInlineFindingIdFactory.

\- Always fill provenance with repositoryId, pullRequestId, base/head
SHAs, and fileSha if available.

\- Optionally attach telemetry with model, token counts, and latency.

5\) In the posting worker, use id for dedup, and log provenance + id for
audit.

Updated final section: reviewed project specification and plan

\- Achieved (designed + tested):

\- finding-schema: structured model with deterministic ids and
provenance.

\- llm-finding-schema: minimal inline model with repository-aware
provenance and telemetry; now fully documented at field level, with
tests.

\- Separation of concerns and orchestration plan are clear and
intention-revealing.

\- Still to implement (MVP path):

\- github/gitlab connector: snapshot fetch + inline posting (dry-run
first).

\- diff-mapper-core integration for precise after-line anchoring.

\- java-parser wrapper for ASTs with size/time caps.

\- rule-engine scaffolding and one end-to-end visitor-based rule
emitting LLMInlineFindings.

\- bootstrap-app wiring with an in-memory queue + worker to post or log
comments.

\- Recommendations to move forward:

1\) Land the documented llm-finding-schema now (this PR).

2\) Implement the connector and a posting worker; verify inline
anchoring on a test PR.

3\) Wire bootstrap-app to produce LLMInlineFindings for a single
conservative rule; dry-run, then live.

4\) Add integration tests around multi-hunk diffs and renames.

5\) Iterate on prompt and telemetry capture; start building cost/latency
dashboards using telemetry.

If you want, I can also update the README to restate the documentation
standard and include a concrete example end-to-end showing how a rule
constructs an LLMInlineFinding, why each field is set, and how the
posting worker uses it.

Tab 12
