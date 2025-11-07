package com.sono99.diffparser.api.v1.model;

import java.util.List;

/**
 * Public API DTO for main container of parsed diff results. Sanitized version of the internal
 * ParsedDiff model.
 *
 * @param topLevelResult the top-level result containing all diffed files
 * @param originalDiffContent the original diff content as a string
 * @param isValid whether the diff was parsed successfully
 */
public record ParsedDiffV1(
    TopLevelDiffResultV1 topLevelResult, String originalDiffContent, boolean isValid) {
  /**
   * Gets all diffed files.
   *
   * @return list of diffed files
   */
  public List<DiffedFileV1> getDiffedFiles() {
    return topLevelResult.diffedFiles();
  }

  /**
   * Gets all line ranges from the diff.
   *
   * @return list of all line ranges
   */
  public List<LineRangeV1> getAllLineRanges() {
    return topLevelResult.getAllLineRanges();
  }

  /**
   * Gets line ranges of a specific change type.
   *
   * @param changeType the change type
   * @return list of matching line ranges
   */
  public List<LineRangeV1> getLineRangesByType(LineRangeV1.ChangeType changeType) {
    return topLevelResult.getLineRangesByType(changeType);
  }

  /**
   * Checks if the diff has any actual changes.
   *
   * @return true if has changes
   */
  public boolean hasChanges() {
    return topLevelResult.hasChanges();
  }
}
