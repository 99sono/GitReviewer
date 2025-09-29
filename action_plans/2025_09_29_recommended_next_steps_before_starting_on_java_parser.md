# Recommended Next Steps Before Starting on Java Parser

## Project Skeleton Overview
This is a well-organized multi-module Maven project (`code-reviewer` artifact) built with Java 21 and Spring Boot 3.3.0. The root pom.xml defines shared dependencies (e.g., JavaParser 3.25.8 for AST parsing, GitHub API 1.323 for integration, LangChain4j 0.34.0 for LLM support) and configures the compiler, Surefire, and Spring Boot plugins. It lists 8 modules in this order:
- `finding-schema`: Core data models for code findings.
- `llm-finding-schema`: LLM-specific schemas extending the core models.
- `java-parser`: Java AST parsing service using JavaParser (your focus area).
- `diff-mapper-core`: Handles diff parsing and AST intersection for PR changes.
- `github-connector`: Integrates with GitHub API for PR fetching/comments.
- `rule-engine`: Framework for applying static analysis rules.
- `outputs`: Formats and renders review findings.
- `bootstrap-app`: The main Spring Boot application assembling everything.

The root README.md provides a high-level overview, key features (e.g., GitHub PR integration, modular rules), tech stack, quick start (clone, configure yml, mvn spring-boot:run), and use cases. It references module-specific docs and the `delete_me/` folder for original specs.

The java-parser module is foundational for code analysis. Its README.md describes it as the AST extraction layer, with core classes like `JavaParsingService` (main parser), `EnrichedContent` (parsed output with metadata), `JavaParsingNode` (hierarchical AST nodes), and support classes (e.g., `JavadocRef`, `AnnotationRef`, `QuickLink`). It depends on JavaParser, Gson, and Spring. Usage is straightforward via autowiring the service.

The ACTIONPLAN.md outlines a phased implementation:
1. **Core Parsing**: Implement service, node model, and basic JavaParser integration.
2. **AST Node Creation**: Visitor for traversal, support for classes/methods/fields, Javadoc/annotations.
3. **Content Enrichment**: Refs and quick links.
4. **Advanced**: Fallback parsing, caching.
5. **Integration/Testing**: With diff-mapper, unit tests, benchmarks.

Existing Java files (from file list) like `JavaParsingService.java`, `JavaParsingNode.java`, etc., suggest Phase 1 is partially started, but full implementation (e.g., visitor, enrichment) seems pending.

Overall, the skeleton is logical: data models → parsing → diff/mapping → rules → output → app. It's designed for extensibility, with clear separation of concerns.

## CLINE/AI Friendliness Assessment
The project is already quite AI-friendly for CLINE:
- **Pros**:
  - Maven multi-module: Standardized builds (`mvn clean install` works root-wide), easy dependency management, no complex setups.
  - Modular design: I can focus on one module (e.g., java-parser) without affecting others. Testing is isolated.
  - Documentation: READMEs and ACTIONPLANs provide clear goals, reducing ambiguity. Specs in `delete_me/` and `metadata/` track progress.
  - Tech stack: JavaParser integrates well with AI for AST manipulation; Spring Boot for simple running/testing.
  - Git integration: Remote is GitHub, so I can suggest/test PR workflows if needed.
  - No circular deps or monorepo mess; clean paths from CWD.

- **Cons/Areas for Improvement**:
  - Code style enforcement: No visible configs (e.g., .editorconfig, checkstyle.xml, spotbugs.xml). AI-generated code might introduce inconsistencies (e.g., indentation, imports). This could lead to linter errors or manual fixes.
  - Build automation: Root has pluginManagement, but no full-site plugin or formatting (e.g., Maven Spotless) to auto-format on build.
  - Documentation gaps: Module READMEs are high-level; no Swagger/OpenAPI for API docs in bootstrap-app. ACTIONPLANs are good but could link to specs more explicitly.
  - Testing: Mentions unit tests, but no visible integration/CI setup (e.g., .github/workflows/maven.yml for auto-builds on PRs).
  - Metadata: `metadata/` has summaries, but no AI-specific file like `.clinerules` (custom instructions for CLINE, e.g., "Always use Java 21 records where possible").
  - MCP/External Tools: No MCP servers configured yet. For GitHub work, the official GitHub MCP server (from modelcontextprotocol) could provide tools like `fetch_pr` or `create_comment` without hardcoding tokens, making testing safer/more modular.

Score: 8/10. It's solid for implementation but could be 10/10 with style enforcement and MCP for integrations.

## Recommendations and Next Steps
To prepare for java-parser implementation:
1. **Add Missing Metadata/Configs** (low effort, high impact for AI consistency):
   - `.editorconfig`: Enforce tabs/spaces, charset, etc.
   - `checkstyle.xml` (root or module): XML rules for style (e.g., Google Java style).
   - `.github/workflows/maven.yml`: Basic CI for build/test on push/PR.
   - `.clinerules`: Text file with project-specific AI guidelines (e.g., "Prefer immutable data structures in parsers").
   - Update root README with build/test commands and module diagram.

2. **MCP Server Setup** (optional, for GitHub/LLM enhancements):
   - Install the GitHub MCP server (e.g., from github.com/modelcontextprotocol/servers/tree/main/src/github). It exposes tools like `list_repos`, `get_pr_diff`, `post_comment`. This would let me use `use_mcp_tool` for real GitHub interactions without tokens in code. We'd add it to a config file (e.g., cline-mcp.json) and expose in bootstrap-app.
   - If LLM-specific (LangChain4j), an MCP for OpenAI/Anthropic could help with model calls during reviews.

3. **Before Implementation**:
   - Scan for existing issues: Use tools to list code definitions in java-parser or search for TODOs.
   - Plan java-parser: Start with Phase 1 (complete `JavaParsingService.parse()` using JavaParser's `StaticJavaParser`), then visitor for nodes.

*Generated on 2025-09-29 for planning java-parser implementation.*
