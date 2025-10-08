package com.sono99.javaparser.impl.generic.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Immutable data container that caches efficient line-based access to source content. Pure record
 * with pre-processed line mappings for use by SourceContentUtils.
 *
 * <p>Reads from InputStream during creation to build cached data structures. Provides O(1) line
 * lookups and precomputed terminators for fast range extraction.
 *
 * @param lineCache 1-based line number to line content mapping (without terminators)
 * @param terminators precomputed line terminators between lines
 * @param totalLines total number of lines in the source
 * @param originalSource original source content for compatibility with JavaParser
 */
public record SourceContentHelper(
    /** 1-based line number to line content mapping (without terminators). */
    TreeMap<Integer, String> lineCache,
    /** Precomputed line terminators between lines. */
    List<String> terminators,
    /** Total number of lines in the source. */
    int totalLines,
    /** Original source content for compatibility with JavaParser. */
    String originalSource) {

  /**
   * Factory for creating SourceContentHelper instances. Provides a clean API for dependency
   * injection and testable creation logic.
   */
  public static class Factory {

    /**
     * Creates a cached record from an InputStream. Reads the entire stream and builds line caches
     * and terminators.
     *
     * @param inputStream the InputStream containing source content (will be fully read)
     * @return a configured SourceContentHelper instance
     * @throws Exception if stream reading fails
     */
    public SourceContentHelper createFrom(InputStream inputStream) throws Exception {
      // Read all bytes first to get the original source string
      String originalSource = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      // Now build the caches from the string
      TreeMap<Integer, String> lines = new TreeMap<>();
      List<String> terminators = new ArrayList<>();

      // Split lines with -1 to keep empty lines, and build terminator list
      String[] splitLines = originalSource.split("\\R", -1);

      // Process each line and identify terminators
      int position = 0;
      for (int i = 0; i < splitLines.length; i++) {
        String line = splitLines[i];
        lines.put(i + 1, line); // 1-based line numbers

        // Calculate position after this line
        position += line.length();

        // Detect and store terminator if not the last line
        if (i < splitLines.length - 1 && position < originalSource.length()) {
          char ch = originalSource.charAt(position);
          if (ch == '\r') {
            if (position + 1 < originalSource.length()
                && originalSource.charAt(position + 1) == '\n') {
              terminators.add("\r\n");
              position += 2;
            } else {
              terminators.add("\r");
              position += 1;
            }
          } else if (ch == '\n') {
            terminators.add("\n");
            position += 1;
          } else {
            // No terminator found, add empty string
            terminators.add("");
          }
        }
      }

      int totalLines = lines.size();
      return new SourceContentHelper(lines, terminators, totalLines, originalSource);
    }
  }

  /**
   * Gets the content of a specific line by number (1-based). Direct O(1) map lookup with terminator
   * included.
   *
   * @param lineNumber 1-based line number
   * @return the line content without terminator, or null if out of range
   */
  public String getLine(int lineNumber) {
    return lineCache.get(lineNumber);
  }

  /**
   * Gets the total number of lines in the source.
   *
   * @return total line count
   */
  public int getTotalLines() {
    return totalLines;
  }

  /**
   * Gets the original source content.
   *
   * @return the complete source content this helper was created from
   */
  public String getOriginalSource() {
    return originalSource;
  }
}
