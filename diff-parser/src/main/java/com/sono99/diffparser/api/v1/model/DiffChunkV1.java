package com.sono99.diffparser.api.v1.model;

import java.util.List;

/**
 * Public API DTO representing an individual diff hunk. Sanitized version of the internal DiffChunk
 * model.
 *
 * @param hunkStartLine the starting line number of the hunk
 * @param hunkEndLine the ending line number of the hunk
 * @param lineRanges the list of line ranges within this hunk
 * @param contextBefore the context lines before the changes
 * @param contextAfter the context lines after the changes
 * @param diffText the diff text for this hunk
 */
public record DiffChunkV1(
    int hunkStartLine,
    int hunkEndLine,
    List<LineRangeV1> lineRanges,
    String contextBefore,
    String contextAfter,
    String diffText) {
  /**
   * Gets all line ranges of a specific change type.
   *
   * @param changeType the change type to filter by
   * @return list of matching line ranges
   */
  public List<LineRangeV1> getLineRangesByType(LineRangeV1.ChangeType changeType) {
    return lineRanges.stream().filter(range -> range.changeType() == changeType).toList();
  }

  /**
   * Checks if this chunk contains any changes (not just context).
   *
   * @return true if has actual changes
   */
  public boolean hasChanges() {
    return lineRanges.stream()
        .anyMatch(range -> range.changeType() != LineRangeV1.ChangeType.EQUAL);
  }
}
