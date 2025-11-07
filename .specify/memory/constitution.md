<!--
Sync Impact Report:
- Version change: none -> 1.0.0
- Principles revised based on user feedback and .clinerules.
- Added sections:
  - Core Principles
  - Governance and Detailed Standards
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
-->
# LLM-Assisted Code Reviewer Constitution

## Core Principles

### I. Provider-Agnostic Git Services
The system MUST be designed with abstractions that treat Git service providers (e.g., GitHub, GitLab) as interchangeable plugins. All interactions, such as commenting on reviews or parsing diffs, MUST use a provider-agnostic interface to allow for swappable implementations.

### II. Versioned & Decoupled Modules
Modules MUST follow the established versioned API and implementation structure. This includes separating public contracts (`api/v1`), backend-agnostic core logic (`impl/generic`), and version-specific adapters (`impl/v1`) to ensure stability and clear separation of concerns, as detailed in `.clinerules`.

### III. Focused LLM-Powered Review
The system's core purpose is to provide LLM-assisted code review. It is not a general-purpose static analysis tool. Features should be centered on leveraging language models to provide intelligent feedback on code changes.

### IV. Rigorous Development Quality
Code written for the GitReviewer project itself MUST be of the highest quality to ensure reliable LLM-generated suggestions. All new functionality requires thorough testing, using Mockito for unit tests and Spring Boot for integration tests, to validate its correctness and maintainability.

### V. Modern Java Stack
The project is built on the Spring Boot framework using Java 21. Development MUST adhere to framework best practices, including constructor injection, standard service/repository annotations, and configuration via `application.yml`.

## Governance and Detailed Standards

This document outlines high-level principles. The single source of truth for detailed coding standards, architectural patterns, build procedures, and versioning is the **`.clinerules`** file. All development MUST adhere to the rules specified within it.

**Version**: 1.0.0 | **Ratified**: 2025-10-18 | **Last Amended**: 2025-10-18