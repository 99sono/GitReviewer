package com.sono99.diffparser.impl.generic.model;

import com.github.difflib.unifieddiff.UnifiedDiff;
import java.util.List;

/**
 * Main container for parsed diff results. This is the primary output of the diff parsing service.
 *
 * @param topLevelResult the top-level result containing all diffed files
 * @param originalDiffContent the original diff content as a string
 * @param originalGitHubUnifiedDiff The the original github java-diff-utils {@link
 *     com.github.difflib.unifieddiff.UnifiedDiff} object that we parsed data into.
 * @param isValid whether the diff was parsed successfully
 */
public record ParsedDiff(
    TopLevelDiffResult topLevelResult,
    String originalDiffContent,
    UnifiedDiff originalGitHubUnifiedDiff,
    boolean isValid) {
  /**
   * Gets all diffed files.
   *
   * @return list of diffed files
   */
  public List<DiffedFile> getDiffedFiles() {
    return topLevelResult.diffedFiles();
  }

  /**
   * Gets all line ranges from the diff.
   *
   * @return list of all line ranges
   */
  public List<LineRange> getAllLineRanges() {
    return topLevelResult.getAllLineRanges();
  }

  /**
   * Gets line ranges of a specific change type.
   *
   * @param changeType the change type
   * @return list of matching line ranges
   */
  public List<LineRange> getLineRangesByType(LineRange.ChangeType changeType) {
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

  /**
   * Gets the diff text for the entire merge request. This is useful for sending the complete diff
   * context to an LLM.
   *
   * @return the full diff text
   */
  public String getOriginalDiffContent() {
    return originalDiffContent;
  }
}
