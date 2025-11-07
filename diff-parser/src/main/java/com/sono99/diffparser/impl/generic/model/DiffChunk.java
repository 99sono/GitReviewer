package com.sono99.diffparser.impl.generic.model;

import com.github.difflib.patch.AbstractDelta;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an individual diff hunk containing source and target line ranges. A hunk is a section
 * of the diff that shows context lines and changes.
 *
 * @param sourceLineRange the line range in the source file (nullable for file creation)
 * @param targetLineRange the line range in the target file (nullable for file deletion)
 * @param originalDelta the original java-diff-utils AbstractDelta object for lazy text extraction
 */
public record DiffChunk(
    LineRange sourceLineRange, LineRange targetLineRange, AbstractDelta<String> originalDelta) {

  /**
   * Gets all line ranges of a specific change type by combining source and target ranges.
   *
   * @param changeType the change type to filter by
   * @return list of matching line ranges
   */
  public List<LineRange> getLineRangesByType(LineRange.ChangeType changeType) {
    List<LineRange> result = new ArrayList<>();
    if (sourceLineRange != null && sourceLineRange.changeType() == changeType) {
      result.add(sourceLineRange);
    }
    if (targetLineRange != null && targetLineRange.changeType() == changeType) {
      result.add(targetLineRange);
    }
    return result;
  }

  /**
   * Checks if this chunk contains any changes (not just context).
   *
   * @return true if has actual changes
   */
  public boolean hasChanges() {
    return List.of(sourceLineRange, targetLineRange).stream()
        .filter(range -> range != null)
        .anyMatch(range -> range.changeType() != LineRange.ChangeType.EQUAL);
  }

  /**
   * Gets the diff text for this hunk. This is useful for sending the hunk's diff context to an LLM.
   *
   * @return the diff text for this hunk
   */
  public String getDiffTextForHunk() {
    return originalDelta.toString();
  }

  /**
   * Gets the source hunk start line number.
   *
   * @return the start line number of the source hunk, or null if no source range
   */
  public Integer getSourceHunkStartLine() {
    return sourceLineRange != null ? sourceLineRange.startLineIndex() : null;
  }

  /**
   * Gets the source hunk end line number.
   *
   * @return the end line number of the source hunk, or null if no source range
   */
  public Integer getSourceHunkEndLine() {
    return sourceLineRange != null ? sourceLineRange.endLineIndex() : null;
  }

  /**
   * Gets the target hunk start line number.
   *
   * @return the start line number of the target hunk, or null if no target range
   */
  public Integer getTargetHunkStartLineIndex() {
    return targetLineRange != null ? targetLineRange.startLineIndex() : null;
  }

  /**
   * Gets the target hunk end line number.
   *
   * @return the end line number of the target hunk, or null if no target range
   */
  public Integer getTargetHunkEndLineIndex() {
    return targetLineRange != null ? targetLineRange.endLineIndex() : null;
  }
}
