package com.sono99.diffparser.api.v1.model;

import java.util.List;

/**
 * Public API DTO for top-level result containing all parsed diff files. Sanitized version of the
 * internal TopLevelDiffResult model.
 *
 * @param diffedFiles the list of diffed files
 * @param diffHeader the header of the diff
 * @param totalFilesChanged the total number of files changed
 * @param totalAdditions the total number of additions
 * @param totalDeletions the total number of deletions
 */
public record TopLevelDiffResultV1(
    List<DiffedFileV1> diffedFiles,
    String diffHeader,
    int totalFilesChanged,
    int totalAdditions,
    int totalDeletions) {
  /**
   * Gets all line ranges from all files.
   *
   * @return list of all line ranges
   */
  public List<LineRangeV1> getAllLineRanges() {
    return diffedFiles.stream().flatMap(file -> file.getAllLineRanges().stream()).toList();
  }

  /**
   * Gets line ranges of a specific change type from all files.
   *
   * @param changeType the change type
   * @return list of matching line ranges
   */
  public List<LineRangeV1> getLineRangesByType(LineRangeV1.ChangeType changeType) {
    return diffedFiles.stream()
        .flatMap(file -> file.getLineRangesByType(changeType).stream())
        .toList();
  }

  /**
   * Checks if there are any actual changes in the diff.
   *
   * @return true if has changes
   */
  public boolean hasChanges() {
    return diffedFiles.stream().anyMatch(DiffedFileV1::hasChanges);
  }
}
