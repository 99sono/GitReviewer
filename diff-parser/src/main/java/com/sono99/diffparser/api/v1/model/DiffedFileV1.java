package com.sono99.diffparser.api.v1.model;

import java.util.List;

/**
 * Public API DTO representing all changes in a single file within a diff. Sanitized version of the
 * internal DiffedFile model.
 *
 * @param repositoryPath the path of the repository
 * @param filePath the path of the file
 * @param isCreatedNewInMergeRequest whether the file is created new in the merge request
 * @param isDeleted whether the file is deleted
 * @param isRenamed whether the file is renamed
 * @param oldFilePath the old file path if renamed
 * @param diffStartLineForFile the starting line number for the diff in the file
 * @param diffChunks the list of diff chunks in this file
 * @param diffText the diff text for this file
 */
public record DiffedFileV1(
    String repositoryPath,
    String filePath,
    boolean isCreatedNewInMergeRequest,
    boolean isDeleted,
    boolean isRenamed,
    String oldFilePath,
    int diffStartLineForFile,
    List<DiffChunkV1> diffChunks,
    String diffText) {
  /**
   * Gets all line ranges from all chunks in this file.
   *
   * @return list of all line ranges
   */
  public List<LineRangeV1> getAllLineRanges() {
    return diffChunks.stream().flatMap(chunk -> chunk.lineRanges().stream()).toList();
  }

  /**
   * Gets line ranges of a specific change type from all chunks.
   *
   * @param changeType the change type
   * @return list of matching line ranges
   */
  public List<LineRangeV1> getLineRangesByType(LineRangeV1.ChangeType changeType) {
    return diffChunks.stream()
        .flatMap(chunk -> chunk.getLineRangesByType(changeType).stream())
        .toList();
  }

  /**
   * Checks if this file has any actual changes (not just context).
   *
   * @return true if has changes
   */
  public boolean hasChanges() {
    return diffChunks.stream().anyMatch(DiffChunkV1::hasChanges);
  }
}
