package com.sono99.diffparser.impl.generic.service;

import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffReader;
import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.LineRange;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.generic.model.TopLevelDiffResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Core service for parsing unified diffs using the java-diff-utils library. Converts library
 * objects to our simplified domain model while preserving original object references for lazy text
 * extraction.
 *
 * <p>This service implements the blackbox core logic for diff parsing, handling the conversion from
 * UnifiedDiff structures to our ParsedDiff domain model. It maintains references to the original
 * java-diff-utils objects to enable efficient lazy text extraction.
 */
@Service
public class DiffParserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiffParserService.class);

  /**
   * Parses a unified diff string and returns a structured ParsedDiff result.
   *
   * @param diffContent the unified diff content as a string
   * @return ParsedDiff containing the parsed diff information, or invalid result on error
   */
  public ParsedDiff parseUnifiedDiff(String diffContent) {
    try {
      // (a) Parse using java-diff-utils library
      InputStream inputStream =
          new ByteArrayInputStream(diffContent.getBytes(StandardCharsets.UTF_8));
      UnifiedDiff unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(inputStream);

      // (b) Convert to our domain model
      List<DiffedFile> diffedFiles =
          unifiedDiff.getFiles().stream()
              .map(this::convertUnifiedDiffFile)
              .collect(Collectors.toList());

      // (c) Create top-level result with statistics
      String diffHeader = extractDiffHeader(unifiedDiff);
      int totalFilesChanged = diffedFiles.size();
      int totalAdditions = calculateTotalAdditions(diffedFiles);
      int totalDeletions = calculateTotalDeletions(diffedFiles);

      TopLevelDiffResult topLevelResult =
          new TopLevelDiffResult(
              diffedFiles, diffHeader, totalFilesChanged, totalAdditions, totalDeletions);

      return new ParsedDiff(topLevelResult, diffContent, unifiedDiff, true);

    } catch (Exception e) {
      // (e) Handle parsing errors gracefully
      LOGGER.warn("Failed to parse unified diff: {}", e.getMessage(), e);
      return new ParsedDiff(
          new TopLevelDiffResult(List.of(), "", 0, 0, 0), // Empty result
          diffContent,
          null, // No original unified diff file
          false // Mark as invalid
          );
    }
  }

  /**
   * Converts a UnifiedDiffFile to our DiffedFile domain model.
   *
   * @param unifiedDiffFile the library's UnifiedDiffFile object
   * @return our DiffedFile domain model with original object reference
   */
  private DiffedFile convertUnifiedDiffFile(UnifiedDiffFile unifiedDiffFile) {
    // (a) Extract basic file metadata
    String fromFile = unifiedDiffFile.getFromFile();
    String toFile = unifiedDiffFile.getToFile();
    String repositoryPath = determineRepositoryPath(fromFile, toFile);

    // (b) Convert deltas to diff chunks
    List<DiffChunk> diffChunks =
        unifiedDiffFile.getPatch().getDeltas().stream()
            .map(delta -> convertDeltaToDiffChunk(delta))
            .collect(Collectors.toList());

    // (c) Determine file change type
    boolean isDeleted = unifiedDiffFile.getToFile().equals("/dev/null");
    boolean isCreatedNew = unifiedDiffFile.getFromFile().equals("/dev/null");
    boolean isRenamed = !fromFile.equals(toFile) && !isDeleted && !isCreatedNew;

    // (d) Calculate diff start line (first hunk start)
    int diffStartLineForFile = diffChunks.isEmpty() ? 1 : diffChunks.get(0).hunkStartLine();

    return new DiffedFile(
        repositoryPath,
        toFile,
        isCreatedNew,
        isDeleted,
        isRenamed,
        fromFile,
        diffStartLineForFile,
        diffChunks,
        unifiedDiffFile // Preserve original for lazy text extraction
        );
  }

  /**
   * Converts an AbstractDelta to our DiffChunk domain model.
   *
   * @param delta the library's AbstractDelta object
   * @return our DiffChunk domain model with original object reference
   */
  private DiffChunk convertDeltaToDiffChunk(AbstractDelta<String> delta) {
    // (a) Extract position information
    Chunk<String> sourceChunk = delta.getSource();
    Chunk<String> targetChunk = delta.getTarget();

    // (b) Determine change type based on delta type
    LineRange.ChangeType changeType = convertDeltaTypeToChangeType(delta.getType());

    // (c) Create source line range (direct 1:1 mapping)
    LineRange sourceLineRange = null;
    if (sourceChunk.size() > 0) {
      sourceLineRange = new LineRange(sourceChunk.getPosition(), sourceChunk.last(), changeType);
    }

    // (d) Create target line range (direct 1:1 mapping)
    LineRange targetLineRange = null;
    if (targetChunk.size() > 0) {
      LineRange.ChangeType targetChangeType =
          changeType == LineRange.ChangeType.DELETE ? LineRange.ChangeType.INSERT : changeType;
      targetLineRange =
          new LineRange(targetChunk.getPosition(), targetChunk.last(), targetChangeType);
    }

    // (e) Extract context lines (before/after the changes)
    String contextBefore = extractContextLines(sourceChunk.getLines(), true);
    String contextAfter = extractContextLines(targetChunk.getLines(), false);

    return new DiffChunk(
        sourceLineRange,
        targetLineRange,
        contextBefore,
        contextAfter,
        delta // Preserve original for lazy text extraction
        );
  }

  /**
   * Converts java-diff-utils delta type to our ChangeType enum.
   *
   * @param deltaType the library's delta type
   * @return our corresponding ChangeType
   */
  private LineRange.ChangeType convertDeltaTypeToChangeType(
      com.github.difflib.patch.DeltaType deltaType) {
    return switch (deltaType) {
      case INSERT -> LineRange.ChangeType.INSERT;
      case DELETE -> LineRange.ChangeType.DELETE;
      case CHANGE -> LineRange.ChangeType.CHANGE;
      case EQUAL -> LineRange.ChangeType.EQUAL;
    };
  }

  /**
   * Extracts context lines from a list of lines, typically the first few lines before/after
   * changes.
   *
   * @param lines the list of lines from the delta
   * @param isBefore true if extracting context before changes, false for after
   * @return formatted context string
   */
  private String extractContextLines(List<String> lines, boolean isBefore) {
    if (lines.isEmpty()) {
      return "";
    }

    // Take first 3 lines as context
    int contextLines = Math.min(3, lines.size());
    List<String> context = lines.subList(0, contextLines);

    return context.stream()
        .map(line -> isBefore ? line : "+" + line)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Determines the repository path from the file paths in the diff.
   *
   * @param fromFile the original file path
   * @param toFile the new file path
   * @return the repository path to use
   */
  private String determineRepositoryPath(String fromFile, String toFile) {
    // Use the non-null file path as repository path
    if (!fromFile.equals("/dev/null")) {
      return fromFile;
    } else if (!toFile.equals("/dev/null")) {
      return toFile;
    } else {
      return "unknown";
    }
  }

  /**
   * Extracts the diff header from the UnifiedDiff object.
   *
   * @param unifiedDiff the parsed unified diff
   * @return the diff header or empty string if none
   */
  private String extractDiffHeader(UnifiedDiff unifiedDiff) {
    return unifiedDiff.getHeader() != null ? unifiedDiff.getHeader() : "";
  }

  /**
   * Calculates the total number of additions across all files.
   *
   * @param diffedFiles the list of diffed files
   * @return total additions count
   */
  private int calculateTotalAdditions(List<DiffedFile> diffedFiles) {
    return diffedFiles.stream()
        .flatMap(file -> file.getLineRangesByType(LineRange.ChangeType.INSERT).stream())
        .mapToInt(range -> range.endLine() - range.startLine() + 1)
        .sum();
  }

  /**
   * Calculates the total number of deletions across all files.
   *
   * @param diffedFiles the list of diffed files
   * @return total deletions count
   */
  private int calculateTotalDeletions(List<DiffedFile> diffedFiles) {
    return diffedFiles.stream()
        .flatMap(
            file -> {
              List<LineRange> lineRangesOfTypeRemoved =
                  file.getLineRangesByType(LineRange.ChangeType.DELETE);
              return lineRangesOfTypeRemoved.stream();
            })
        .mapToInt(range -> range.endLine() - range.startLine() + 1)
        .sum();
  }
}
