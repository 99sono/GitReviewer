package com.sono99.javaparser.impl.generic.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

/**
 * Utility service for source code content manipulation operations. Provides text processing
 * utilities specifically for Java source code parsing. Handles line-based content extraction,
 * validation, and manipulation.
 */
@Service
public class SourceContentUtils {

  private final SourceContentHelper.Factory sourceContentFactory =
      new SourceContentHelper.Factory();

  /**
   * Factory method to create an efficient SourceContentHelper from an InputStream. This is the
   * primary API for creating cached line operations.
   *
   * @param inputStream the InputStream containing source content (will be fully read)
   * @return a SourceContentHelper instance for efficient text operations
   */
  public SourceContentHelper createHelper(InputStream inputStream) {
    try {
      return sourceContentFactory.createFrom(inputStream);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create SourceContentHelper from InputStream", e);
    }
  }

  /**
   * Convenience factory method to create SourceContentHelper from a String. Internally converts to
   * InputStream for processing.
   *
   * @param source the complete source code content
   * @return a SourceContentHelper instance for efficient text operations
   */
  public SourceContentHelper createHelper(String source) {
    return createHelper(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Extracts source content between specified line numbers (inclusive). Extremely fast
   * O(range_size) performance using pre-cached line mappings.
   *
   * @param helper the cached source helper containing line data
   * @param startLine 1-based start line number
   * @param endLine 1-based end line number (inclusive)
   * @return the extracted content between the specified lines, preserving original line endings
   * @throws IllegalArgumentException if parameters are invalid
   */
  public String extractSourceRange(SourceContentHelper helper, int startLine, int endLine) {
    if (helper == null) {
      throw new IllegalArgumentException("SourceContentHelper cannot be null");
    }
    if (startLine < 1) {
      throw new IllegalArgumentException("Start line must be >= 1, got: " + startLine);
    }
    if (endLine < startLine) {
      throw new IllegalArgumentException(
          "End line (" + endLine + ") must be >= start line (" + startLine + ")");
    }

    // For backward compatibility, return empty string for out-of-bounds start lines
    if (startLine > helper.getTotalLines()) {
      return "";
    }

    // Adjust endLine if it exceeds total lines
    if (endLine > helper.getTotalLines()) {
      endLine = helper.getTotalLines();
    }

    // Build result by concatenating cached lines with precomputed terminators
    StringBuilder result = new StringBuilder();
    for (int line = startLine; line <= endLine; line++) {
      result.append(helper.getLine(line));
      // Add terminator if not the last line in the range and terminator exists
      if (line < endLine && line - 1 < helper.terminators().size()) {
        result.append(helper.terminators().get(line - 1)); // 0-based index for terminators
      }
    }

    return result.toString();
  }

  /**
   * Gets the content of a specific line by number (1-based). Direct O(1) lookup from cached line
   * data.
   *
   * @param helper the cached source helper containing line data
   * @param lineNumber 1-based line number
   * @return the line content, or null if out of range
   */
  public String getLine(SourceContentHelper helper, int lineNumber) {
    if (helper == null) {
      return null;
    }
    return helper.getLine(lineNumber);
  }

  /**
   * Gets the total number of lines in the source. Direct O(1) lookup from cached data.
   *
   * @param helper the cached source helper containing line data
   * @return total line count
   */
  public int getTotalLines(SourceContentHelper helper) {
    return helper != null ? helper.getTotalLines() : 0;
  }

  /**
   * Gets the original source content. Direct access from cached data.
   *
   * @param helper the cached source helper containing line data
   * @return the original source content
   */
  public String getOriginalSource(SourceContentHelper helper) {
    return helper != null ? helper.getOriginalSource() : null;
  }

  // Additional utility methods can be added here:
  // - Content normalization
  // - Whitespace handling
  // - Syntax validation helpers
  // - Range intersection calculations
}
