package com.sono99.diffparser.impl.generic.model;

import com.github.difflib.unifieddiff.UnifiedDiffFile;
import java.util.List;

/**
 * Represents all changes in a single file within a diff. Contains metadata about the file and its
 * diff chunks.
 *
 * @param repositoryPath the path of the repository
 * @param filePath the path of the file
 * @param isCreatedNewInMergeRequest whether the file is created new in the merge request
 * @param isDeleted whether the file is deleted
 * @param isRenamed whether the file is renamed
 * @param oldFilePath the old file path if renamed
 * @param diffStartLineForFile the starting line number for the diff in the file
 * @param diffChunks the list of diff chunks in this file
 * @param originalDiffFile the original java-diff-utils UnifiedDiffFile object for lazy text
 *     extraction
 */
public record DiffedFile(
    String repositoryPath,
    String filePath,
    boolean isCreatedNewInMergeRequest,
    boolean isDeleted,
    boolean isRenamed,
    String oldFilePath,
    int diffStartLineForFile,
    List<DiffChunk> diffChunks,
    UnifiedDiffFile originalDiffFile) {
  /**
   * Gets all line ranges from all chunks in this file.
   *
   * @return list of all line ranges
   */
  public List<LineRange> getAllLineRanges() {
    return diffChunks.stream()
        .flatMap(
            chunk -> {
              if (chunk.sourceLineRange() != null && chunk.targetLineRange() != null) {
                return List.of(chunk.sourceLineRange(), chunk.targetLineRange()).stream();
              } else if (chunk.sourceLineRange() != null) {
                return List.of(chunk.sourceLineRange()).stream();
              } else if (chunk.targetLineRange() != null) {
                return List.of(chunk.targetLineRange()).stream();
              } else {
                return List.<LineRange>of().stream();
              }
            })
        .toList();
  }

  /**
   * Gets line ranges of a specific change type from all chunks.
   *
   * @param changeType the change type
   * @return list of matching line ranges
   */
  public List<LineRange> getLineRangesByType(LineRange.ChangeType changeType) {
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
    return diffChunks.stream().anyMatch(DiffChunk::hasChanges);
  }

  /**
   * Gets the diff text for this file. This is useful for sending the file's diff context to an LLM.
   *
   * @return the diff text for this file
   */
  public String getDiffTextForFile() {
    return originalDiffFile.getPatch().toString();
  }
}
