package com.sono99.diffparser.impl.generic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.LineRange;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.utils.BasicTestHelper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {DiffParserService.class})
class ParseDiffWithLineAdditionAndModificationTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ParseDiffWithLineAdditionAndModificationTest.class);

  @Autowired private DiffParserService diffParserService;

  @Test
  void contextLoads() {
    assertNotNull(diffParserService);
  }

  /**
   * Tests that the DiffParserService can successfully parse `git_diff_02.diff` and produce a valid
   * `ParsedDiff` object. This ensures the basic parsing mechanism is functional and that the
   * original diff content is retained.
   */
  @Test
  void shouldParseGitDiff02Successfully() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_02.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertNotNull(parsedDiff);
    assertTrue(parsedDiff.isValid());
    assertEquals(diffContent, parsedDiff.getOriginalDiffContent());
  }

  /**
   * Tests that the DiffParserService correctly identifies `DiffParserService.java` within the
   * parsed `git_diff_02.diff`.
   */
  @Test
  void shouldIdentifyDiffParserServiceFile() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_02.diff");
    String targetFilePath =
        "diff-parser/src/main/java/com/sono99/diffparser/impl/generic/service/DiffParserService.java";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertNotNull(parsedDiff);
    assertTrue(parsedDiff.isValid());
    DiffedFile diffedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(targetFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(diffedFile, "DiffParserService.java should be found in the parsed diff");
    assertEquals(targetFilePath, diffedFile.filePath());
  }

  /**
   * This test validates the indexing behavior of `LineRange.startLine()` and `LineRange.endLine()`
   * and the content of the `originalGithubChunk` for the `DiffParserService.java` file in
   * `git_diff_02.diff`. It asserts that the line ranges and content accurately reflect the changes
   * observed in the debugger and the diff hunk header `@@ -42,11 +42,12 @@`.
   */
  @Test
  void shouldValidateLineRangesForSpecificChanges() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_02.diff");
    String targetFilePath =
        "diff-parser/src/main/java/com/sono99/diffparser/impl/generic/service/DiffParserService.java";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile diffedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(targetFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(diffedFile, "DiffParserService.java should be found");
    assertFalse(diffedFile.diffChunks().isEmpty(), "DiffedFile should have chunks");

    DiffChunk diffChunk = diffedFile.diffChunks().get(0); // Assuming one chunk for simplicity

    // --- Validate LineRange startLine and endLine (0-based indices) ---
    // Hunk header: @@ -42,11 +42,12 @@
    // Source: starts at 1-based line 42, spans 11 lines (including line 42) meaning lines (42-52)
    // -> 0-based indices 41-51
    // Target: starts at 1-based line 42, spans 12 lines (including line 42) meaning lines (42-53)
    // -> 0-based indices 41-52

    assertNotNull(diffChunk.sourceLineRange(), "Source line range should not be null");
    assertEquals(
        41, diffChunk.sourceLineRange().startLineIndex(), "Source start line should be 41");
    assertEquals(
        42, diffChunk.sourceLineRange().getStartLine(), "Source 1-based start line should be 42");
    assertEquals(
        52, diffChunk.sourceLineRange().getEndLine(), "Source 1-based end line should be 52");

    assertNotNull(diffChunk.targetLineRange(), "Target line range should not be null");
    assertEquals(
        41, diffChunk.targetLineRange().startLineIndex(), "Target start line should be 41");
    assertEquals(52, diffChunk.targetLineRange().endLineIndex(), "Target end line should be 52");
    assertEquals(
        42, diffChunk.targetLineRange().getStartLine(), "Target 1-based start line should be 42");
    assertEquals(
        53, diffChunk.targetLineRange().getEndLine(), "Target 1-based end line should be 53");

    assertEquals(
        LineRange.ChangeType.CHANGE,
        diffChunk.sourceLineRange().changeType(),
        "Source LineRange ChangeType should be CHANGE");
    assertEquals(
        LineRange.ChangeType.CHANGE,
        diffChunk.targetLineRange().changeType(),
        "Target LineRange ChangeType should be CHANGE");
    // --- Validate Content of originalGitHubChunk ---
    // Source chunk content validation
    List<String> sourceLines = diffChunk.sourceLineRange().originalGitHubChunk().getLines();
    assertFalse(sourceLines.isEmpty(), "Source lines should not be empty");
    assertTrue(
        sourceLines.get(0).contains("public ParsedDiff parseUnifiedDiff(String diffContent) {"),
        "Source first line content mismatch");
    assertTrue(
        sourceLines.get(10).contains(".map(this::convertUnifiedDiffFile)"),
        "Source last line content mismatch");
    assertTrue(
        sourceLines.get(7).contains("// (b) Convert to our domain model"),
        "Source modified line content mismatch");

    // Target chunk content validation
    List<String> targetLines = diffChunk.targetLineRange().originalGitHubChunk().getLines();
    assertFalse(targetLines.isEmpty(), "Target lines should not be empty");

    assertTrue(
        targetLines.get(0).contains("public ParsedDiff parseUnifiedDiff(String diffContent) {"),
        "Target first line content mismatch");

    assertTrue(
        targetLines.get(2).contains("      // (a) Parse using java-diff-utils library"),
        "Target added line content mismatch");

    assertTrue(
        targetLines.get(3).contains("// added a dummy line at line number 45 of this file."),
        "Target added line content mismatch");

    assertTrue(
        targetLines
            .get(8)
            .contains("      // (b) Convert to our domain model (dummy modification line 50)"),
        "Target modified line content mismatch");
    assertTrue(
        targetLines.get(11).contains(".map(this::convertUnifiedDiffFile)"),
        "Target last line content mismatch");
  }

  /**
   * Tests that the service correctly detects the specific line addition in `git_diff_02.diff`. It
   * validates that the chunk contains the added line and that the target line range accounts for
   * it.
   */
  @Test
  void shouldDetectLineAdditionAtSpecificLine() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_02.diff");
    String targetFilePath =
        "diff-parser/src/main/java/com/sono99/diffparser/impl/generic/service/DiffParserService.java";
    String addedLineContent =
        "      // added a dummy line at line number 45 of this file."; // Note: leading spaces

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile diffedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(targetFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(diffedFile, "DiffParserService.java should be found");
    assertFalse(diffedFile.diffChunks().isEmpty(), "DiffedFile should have chunks");

    DiffChunk diffChunk = diffedFile.diffChunks().get(0); // Assuming one chunk for simplicity

    // Validate target line range accounts for the added line
    assertNotNull(diffChunk.targetLineRange(), "Target line range should not be null");
    assertEquals(
        41, diffChunk.targetLineRange().startLineIndex(), "Target start line should be 41");
    assertEquals(52, diffChunk.targetLineRange().endLineIndex(), "Target end line should be 52");

    // Validate the added line content is present in the target chunk
    List<String> targetLines = diffChunk.targetLineRange().originalGitHubChunk().getLines();
    assertTrue(
        targetLines.contains(addedLineContent), "Target chunk should contain the added line");
    assertEquals(
        addedLineContent, targetLines.get(3), "Added line content mismatch at expected index");
  }

  /**
   * Tests that the service correctly detects the specific line modification in `git_diff_02.diff`.
   * It validates that the chunk contains the modified line and that the target line range accounts
   * for it.
   */
  @Test
  void shouldDetectLineModificationAtSpecificLine() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_02.diff");
    String targetFilePath =
        "diff-parser/src/main/java/com/sono99/diffparser/impl/generic/service/DiffParserService.java";
    String modifiedLineContent =
        "      // (b) Convert to our domain model (dummy modification line 50)";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile diffedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(targetFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(diffedFile, "DiffParserService.java should be found");
    assertFalse(diffedFile.diffChunks().isEmpty(), "DiffedFile should have chunks");

    DiffChunk diffChunk = diffedFile.diffChunks().get(0); // Assuming one chunk for simplicity

    // Validate target line range accounts for the modified line
    assertNotNull(diffChunk.targetLineRange(), "Target line range should not be null");
    assertEquals(
        41, diffChunk.targetLineRange().startLineIndex(), "Target start line should be 41");
    assertEquals(52, diffChunk.targetLineRange().endLineIndex(), "Target end line should be 52");

    // Validate the modified line content is present in the target chunk
    List<String> targetLines = diffChunk.targetLineRange().originalGitHubChunk().getLines();
    LOGGER.info("--- Debugging shouldDetectLineModificationAtSpecificLine ---");
    LOGGER.info("Target Lines (size {}): {}", targetLines.size(), targetLines);
    LOGGER.info("Expected Modified Line: {}", modifiedLineContent);
    LOGGER.info("----------------------------------------------------------");

    assertTrue(
        targetLines.contains(modifiedLineContent), "Target chunk should contain the modified line");
    assertEquals(
        modifiedLineContent,
        targetLines.get(8),
        "Modified line content mismatch at expected index");
  }
}
