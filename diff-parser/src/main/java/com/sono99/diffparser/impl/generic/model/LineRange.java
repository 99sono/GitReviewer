package com.sono99.diffparser.impl.generic.model;

import com.github.difflib.patch.Chunk;

/**
 * Represents a contiguous range of lines within a diff, typically corresponding to either the
 * source (old) or target (new) side of a change. This record is designed for intersecting with AST
 * nodes for targeted analysis.
 *
 * <p>This {@code LineRange} directly wraps the {@link com.github.difflib.patch.Chunk} concept from
 * the underlying `java-diff-utils` library. A {@code Chunk} represents a block of lines in either
 * the original or revised text. The indices within this {@code LineRange} ({@code startLineIndex}
 * and {@code endLineIndex}) are **0-based**, aligning with array indexing and the internal
 * representation of the `java-diff-utils` library.
 *
 * <p>For example, a diff hunk header like `@@ -42,11 +42,12 @@ public class DiffParserService`
 * indicates:
 *
 * <ul>
 *   <li>**Source (old) side:** Starts at 1-based line 42, spans 11 lines. This corresponds to a
 *       {@code LineRange} with {@code startLineIndex = 41} and {@code endLineIndex = 51} (41 + 11 -
 *       1).
 *   <li>**Target (new) side:** Starts at 1-based line 42, spans 12 lines. This corresponds to a
 *       {@code LineRange} with {@code startLineIndex = 41} and {@code endLineIndex = 52} (41 + 12 -
 *       1).
 * </ul>
 *
 * @param startLineIndex The 0-based starting index of the line range. This corresponds to {@link
 *     Chunk#getPosition()}.
 * @param endLineIndex The 0-based ending index of the line range. This corresponds to {@link
 *     Chunk#last()}.
 * @param originalGitHubChunk The {@link com.github.difflib.patch.Chunk} object from the
 *     `java-diff-utils` library that this {@code LineRange} wraps. It provides access to the actual
 *     lines of code within this chunk.
 * @param changeType The type of change represented by this line range (e.g., INSERT, DELETE,
 *     CHANGE, EQUAL). See {@link ChangeType}.
 */
public record LineRange(
    int startLineIndex,
    int endLineIndex,
    Chunk<String> originalGitHubChunk,
    ChangeType changeType) {

  /**
   * Returns the 1-based starting line number of this range.
   *
   * <p>This is derived from {@code startLineIndex} by adding 1, converting from a 0-based index to
   * a 1-based line number, which is commonly used in diff tools and text editors.
   *
   * @return The 1-based starting line number.
   */
  public int getStartLine() {
    return startLineIndex + 1;
  }

  /**
   * Returns the 1-based ending line number of this range.
   *
   * <p>This is derived from {@code endLineIndex} by adding 1, converting from a 0-based index to a
   * 1-based line number, which is commonly used in diff tools and text editors.
   *
   * @return The 1-based ending line number.
   */
  public int getEndLine() {
    return endLineIndex + 1;
  }

  /** See {@link com.github.difflib.patch.DeltaType}. */
  public enum ChangeType {
    INSERT,
    DELETE,
    CHANGE,
    EQUAL
  }

  /**
   * Checks if this line range intersects with another range.
   *
   * @param other the other line range
   * @return true if they intersect
   */
  public boolean intersects(LineRange other) {
    return this.startLineIndex <= other.endLineIndex && other.startLineIndex <= this.endLineIndex;
  }

  /**
   * Checks if a line number is within this range.
   *
   * @param lineIndexNumber the line number to check
   * @return true if within range
   */
  public boolean containsLineIndex(int lineIndexNumber) {
    return lineIndexNumber >= startLineIndex && lineIndexNumber <= endLineIndex;
  }

  /**
   * Returns the lines of code associated with this chunk as a single string, joined by newlines.
   *
   * @return A string containing all lines of the original GitHub chunk.
   */
  public String getChunkLinesAsString() {
    return String.join("\n", originalGitHubChunk.getLines());
  }
}
