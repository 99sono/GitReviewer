package com.sono99.diffparser.api.v1.service;

import com.sono99.diffparser.api.v1.model.ParsedDiffV1;

/**
 * Public API service interface for parsing unified diffs. Provides a versioned contract for diff
 * parsing operations, returning sanitized DTOs that hide internal implementation details.
 *
 * <p>This interface defines the v1 API for diff parsing, ensuring stable contracts for consumers
 * while allowing internal implementation changes.
 *
 * @since v1.0
 */
public interface DiffParserServiceV1 {

  /**
   * Parses a unified diff string and returns a structured ParsedDiffV1 result.
   *
   * @param diffContent the unified diff content as a string
   * @return ParsedDiffV1 containing the parsed diff information, or invalid result on error
   * @throws RuntimeException if parsing fails due to syntax errors or IO issues
   */
  ParsedDiffV1 parseUnifiedDiff(String diffContent);
}
