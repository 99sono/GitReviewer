# LLM-Assisted Code Reviewer

A Spring Boot application that provides automated code review capabilities for GitHub pull requests using Large Language Models (LLMs) and traditional static analysis.

## 🎯 Project Intention

This project aims to create a **simple-to-use Spring Boot application** that enables developers to run intelligent code reviews on GitHub repositories. By combining LLM-powered analysis with traditional code quality rules, the system provides comprehensive feedback on code changes in pull requests.

## 🚀 Key Features

- **GitHub Integration**: Seamless integration with GitHub PRs and comments
- **LLM-Powered Analysis**: Intelligent code review using advanced language models
- **Traditional Rules**: Static analysis rules for code quality and best practices
- **Modular Architecture**: Clean separation of concerns with dedicated modules
- **REST API**: Simple API for triggering reviews and managing configurations
- **Extensible**: Easy to add new rules and analysis capabilities

## 📁 Project Structure

The project follows a modular Maven structure with dependencies flowing from data models to parsing, analysis, integration, and the main app.

| Module            | Description                              |
|-------------------|------------------------------------------|
| finding-schema    | Core data models for code findings       |
| llm-finding-schema | LLM-specific extensions to schemas         |
| java-parser       | Java AST parsing using JavaParser        |
| diff-mapper-core  | Unified diff parsing and AST intersection|
| github-connector  | GitHub API integration for PRs           |
| rule-engine       | Framework for analysis rules             |
| outputs           | Finding formatting and rendering         |
| bootstrap-app     | Main Spring Boot application             |

Additional folders:
- metadata/              # Development tracking and summaries
- delete_me/             # Original specification documents

## 🛠️ Technology Stack

- **Java 21** - Modern Java runtime
- **Spring Boot 3.x** - Application framework
- **JavaParser** - Java source code parsing
- **LangChain4j** - LLM integration
- **GitHub API** - Repository integration
- **Maven** - Build and dependency management

## 🎨 Code Style & Formatting

This project uses **Google Java Style** conventions for consistent, professional code formatting across the entire codebase.

### Style System Components

- **Checkstyle Plugin**: Enforces coding standards during Maven builds with error-level severity
- **Spotless Plugin**: Automated code formatting using Google Java Format for consistent style
- **VS Code Integration**: Format-on-save configured for Google style conventions

### Key Features

- **Automated Formatting**: `mvn spotless:apply` formats the entire codebase to Google style
- **Build Enforcement**: Checkstyle runs during `mvn compile` and fails on style violations
- **IDE Integration**: VS Code automatically formats code on save using Google conventions
- **Convenient Tooling**: Well-supported Google style works seamlessly across Maven, VS Code, and CI/CD

### Usage Commands

```bash
# Format entire codebase
mvn spotless:apply

# Check for style violations (runs automatically during compile)
mvn checkstyle:check

# Clean build (includes style checking)
mvn clean compile
```

### Benefits

- **Consistency**: Uniform code style across all developers and environments
- **Automation**: No manual formatting decisions - tools handle everything
- **Quality**: Catches common style issues before they reach production
- **Productivity**: Developers focus on logic, not formatting debates

## 🎯 Use Cases

- **Automated PR Reviews**: Automatically review pull requests for code quality
- **LLM-Powered Feedback**: Get intelligent suggestions from language models
- **Custom Rules**: Define organization-specific coding standards
- **CI/CD Integration**: Integrate with existing development workflows
- **Team Collaboration**: Provide consistent review feedback across teams

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd code-reviewer
   ```

2. **Configure GitHub credentials**
   ```yaml
   # application.yml
   github:
     token: your-github-token
     webhook-secret: your-webhook-secret
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Trigger a review**
   ```bash
   curl -X POST http://localhost:8080/api/review/pr \
     -H "Content-Type: application/json" \
     -d '{"owner":"myorg","repo":"myrepo","prNumber":123}'
   ```

## 📚 Documentation

- [API Documentation](./bootstrap-app/README.md) - REST API endpoints
- [Module Guides](./java-parser/README.md) - Individual module documentation
- [Development](./metadata/) - Development tracking and summaries

## 🤝 Contributing

This project is designed to be modular and extensible. Contributions are welcome for:

- New analysis rules
- Additional LLM integrations
- Enhanced Git hosting platform support
- Performance optimizations
- Documentation improvements

## Java formatting

 - [Java formatting and linting](https://code.visualstudio.com/docs/java/java-linting)
 - [eclipse-java-google-style.xml](https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml)

### VS Code Formatter Configuration

**Configuration Background**: The project uses Google Java Format style in the Spotless Maven plugin for automated formatting. While the Eclipse formatter would have been the path of least resistance (requiring only Checkstyle configuration adjustments), we committed to Google Java Format from the start. Since Google doesn't provide an official VS Code plugin, we use a third-party extension that mimics the Google Java Format style.

#### Setup Instructions

1. **Install the Google Java Format Extension**:
   - Install the VS Code extension from Marko Milic: [Java Google Format](https://marketplace.visualstudio.com/items?itemName=mmilic.java-google-format)
   - Note: There is no official Google plugin for VS Code, so we use Marko Milic's extension

2. **Configure VS Code Settings**:
   - Open Command Palette (`Ctrl+Shift+P`)
   - Select "Dev Containers: Settings" and set format to JSON
   - Add the following configuration to your `.vscode/settings.json`:

   ```json
   {
     "editor.formatOnSave": true,
     "[java]": {
       "editor.defaultFormatter": "mmilic.java-google-format"
     }
   }
   ```

3. **Resolve Formatter Conflicts**:
   - The first time you use "Format Document" (`Shift+Alt+F`), VS Code will prompt you to choose between formatters
   - Select the **Google Java Format** option (Marko Milic's extension)
   - This setting is configured globally

#### Alternative Future Option

> **Note**: We may consider switching back to the native Eclipse formatter in the future as it presents fewer configuration obstacles. The current Google formatter setup is chosen for its compatibility with the Spotless plugin used in our Maven build process.

#### Reference

For complete Google Java Style guidelines, see: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built on specifications from the `delete_me/` folder
- Inspired by modern code review automation trends
- Leverages open-source libraries for robust functionality

---

*Initial project skeleton created on 2025-09-10*
