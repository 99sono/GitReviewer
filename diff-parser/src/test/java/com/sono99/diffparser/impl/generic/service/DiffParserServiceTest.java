package com.sono99.diffparser.impl.generic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.LineRange;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.utils.BasicTestHelper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {DiffParserService.class})
class DiffParserServiceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiffParserServiceTest.class);

  @Autowired private DiffParserService diffParserService;

  @Test
  void contextLoads() {
    assertNotNull(diffParserService);
  }

  /**
   * Tests that the DiffParserService can successfully parse a complex Git diff file
   * (`git_diff_01.diff`) and produce a valid `ParsedDiff` object. This test ensures the basic
   * parsing mechanism is functional and that the original diff content is retained.
   */
  @Test
  void shouldParseGitDiff01Successfully() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertNotNull(parsedDiff);
    assertTrue(parsedDiff.isValid());
    assertEquals(diffContent, parsedDiff.getOriginalDiffContent());
  }

  /**
   * Tests the service's ability to correctly identify and represent a deleted file within a Git
   * diff. It asserts that the `DiffedFile` object for a deleted file has the `isDeleted()` flag set
   * to true, `isCreatedNewInMergeRequest()` to false, and that its `oldFilePath()` points to the
   * original path while `filePath()` is `/dev/null`.
   */
  @Test
  void shouldDetectFileDeletion() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");
    String deletedOldFilePathExpected = "java-parser/ACTIONPLAN.md";
    String deletedFileNewFilePathExpected = "/dev/null";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile deletedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.oldFilePath().equals(deletedOldFilePathExpected))
            .findFirst()
            .orElse(null);

    assertNotNull(deletedFile, "Deleted file not found in parsed diff");
    assertTrue(deletedFile.isDeleted(), "File should be marked as deleted");
    assertFalse(deletedFile.isCreatedNewInMergeRequest(), "File should not be marked as new");
    assertFalse(deletedFile.isRenamed(), "File should not be marked as renamed");
    assertEquals(deletedOldFilePathExpected, deletedFile.oldFilePath());
    assertEquals(deletedFileNewFilePathExpected, deletedFile.filePath());
  }

  /**
   * Verifies that the service accurately identifies a newly created file within a Git diff. It
   * checks that the `DiffedFile` object for a new file has `isCreatedNewInMergeRequest()` set to
   * true, `isDeleted()` to false, and that its `oldFilePath()` is `/dev/null` while `filePath()`
   * points to the new file path.
   */
  @Test
  void shouldDetectFileCreation() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");
    String createdFilePath =
        "java-parser/src/main/java/com/sono99/javaparser/api/v1/model/AbstractJavaNodeV1.java";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile createdFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(createdFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(createdFile, "Created file not found in parsed diff");
    assertTrue(createdFile.isCreatedNewInMergeRequest(), "File should be marked as new");
    assertFalse(createdFile.isDeleted(), "File should not be marked as deleted");
    assertFalse(createdFile.isRenamed(), "File should not be marked as renamed");
    assertEquals("/dev/null", createdFile.oldFilePath());
    assertEquals(createdFilePath, createdFile.filePath());
  }

  /**
   * Confirms that the service correctly identifies a modified file within a Git diff. This test
   * checks that a `DiffedFile` object for a modified file has both `isCreatedNewInMergeRequest()`
   * and `isDeleted()` set to false, and that its `oldFilePath()` and `filePath()` are identical.
   */
  @Test
  void shouldDetectFileModification() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");
    String modifiedFilePath = "java-parser/pom.xml";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile modifiedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(modifiedFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(modifiedFile, "Modified file not found in parsed diff");
    assertFalse(modifiedFile.isCreatedNewInMergeRequest(), "File should not be marked as new");
    assertFalse(modifiedFile.isDeleted(), "File should not be marked as deleted");
    assertFalse(modifiedFile.isRenamed(), "File should not be marked as renamed");
    assertEquals(modifiedFilePath, modifiedFile.oldFilePath());
    assertEquals(modifiedFilePath, modifiedFile.filePath());
  }

  /**
   * Ensures that the service correctly identifies file renames within a Git diff. This test
   * verifies that a `DiffedFile` object representing a renamed file has `isRenamed()` set to true,
   * and that both `oldFilePath()` and `filePath()` reflect the correct paths before and after the
   * rename.
   */
  @Test
  void shouldDetectFileRename() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_03_rename.diff");
    String oldFilePath = "old_file.txt";
    String newFilePath = "new_file.txt";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile renamedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(
                file ->
                    file.oldFilePath().equals(oldFilePath) && file.filePath().equals(newFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(renamedFile, "Renamed file not found in parsed diff");
    assertTrue(renamedFile.isRenamed(), "File should be marked as renamed");
    assertFalse(renamedFile.isCreatedNewInMergeRequest(), "File should not be marked as new");
    assertFalse(renamedFile.isDeleted(), "File should not be marked as deleted");
    assertEquals(oldFilePath, renamedFile.oldFilePath());
    assertEquals(newFilePath, renamedFile.filePath());
  }

  /**
   * Verifies that the service correctly handles line ranges for a newly created file. It asserts
   * that for a new file, the `sourceLineRange()` of its diff chunks is null, while the
   * `targetLineRange()` is not null and has a `ChangeType` of `CHANGE`.
   */
  @Test
  void shouldHandleLineRangesForNewFile() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");
    String newFilePath =
        "java-parser/src/main/java/com/sono99/javaparser/api/v1/model/JavadocNodeV1.java";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile newFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(newFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(newFile, "New file not found in parsed diff");
    assertTrue(newFile.isCreatedNewInMergeRequest(), "File should be marked as new");

    // Assuming a new file will have at least one diff chunk
    assertFalse(newFile.diffChunks().isEmpty(), "New file should have diff chunks");
    DiffChunk firstChunk = newFile.diffChunks().get(0);

    assertNull(firstChunk.sourceLineRange(), "Source line range should be null for a new file");

    assertNotNull(
        firstChunk.targetLineRange(), "Target line range should not be null for a new file");

    assertEquals(
        LineRange.ChangeType.CHANGE,
        firstChunk.targetLineRange().changeType(),
        "Change type for new file target range should be CHANGE");
  }

  /**
   * Validates that the service correctly extracts and populates the `DiffChunk` content and line
   * ranges for a modified file. It checks the `startLine()` and `endLine()` for both source and
   * target line ranges within a modified file's diff chunk.
   */
  @Test
  void shouldValidateDiffChunkContentForModifiedFile() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");
    String modifiedFilePath = "java-parser/pom.xml";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    DiffedFile modifiedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(modifiedFilePath))
            .findFirst()
            .orElse(null);

    assertNotNull(modifiedFile, "Modified file not found in parsed diff");
    assertFalse(modifiedFile.diffChunks().isEmpty(), "Modified file should have diff chunks");

    DiffChunk firstChunk = modifiedFile.diffChunks().get(0);

    // Assert sourceLineRange
    assertNotNull(firstChunk.sourceLineRange(), "Source line range should not be null");
    assertEquals(
        0,
        firstChunk.sourceLineRange().startLineIndex(),
        "Source start line should be 0 (0-based)");
    assertEquals(
        7, firstChunk.sourceLineRange().endLineIndex(), "Source end line should be 7 (0-based)");

    // Assert targetLineRange
    assertNotNull(firstChunk.targetLineRange(), "Target line range should not be null");
    assertEquals(
        0,
        firstChunk.targetLineRange().startLineIndex(),
        "Target start line should be 0 (0-based)");
    assertEquals(
        6, firstChunk.targetLineRange().endLineIndex(), "Target end line should be 6 (0-based)");
  }

  /**
   * Verifies that the `ChangeType` is correctly assigned to `LineRange` objects within diff chunks
   * for modified, deleted, and new files. This test ensures the accurate categorization of changes
   * at the line range level.
   */
  @Test
  void shouldValidateLineRangeChangeTypes() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());

    // --- Test for a modified file (java-parser/pom.xml) ---
    String modifiedFilePath = "java-parser/pom.xml";
    DiffedFile modifiedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(modifiedFilePath))
            .findFirst()
            .orElse(null);
    assertNotNull(modifiedFile, "Modified file not found");
    assertFalse(modifiedFile.diffChunks().isEmpty(), "Modified file should have diff chunks");
    DiffChunk modifiedChunk = modifiedFile.diffChunks().get(0);

    assertNotNull(
        modifiedChunk.sourceLineRange(), "Source line range should not be null for modified file");
    assertEquals(
        LineRange.ChangeType.CHANGE,
        modifiedChunk.sourceLineRange().changeType(),
        "Change type for modified source should be CHANGE");
    assertNotNull(
        modifiedChunk.targetLineRange(), "Target line range should not be null for modified file");
    assertEquals(
        LineRange.ChangeType.CHANGE,
        modifiedChunk.targetLineRange().changeType(),
        "Change type for modified target should be CHANGE");

    // --- Test for a deleted file (java-parser/ACTIONPLAN.md) ---

    String deletedFilePathForTesting = "java-parser/ACTIONPLAN.md"; // Corrected path

    DiffedFile deletedFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.oldFilePath().equals(deletedFilePathForTesting))
            .findFirst()
            .orElse(null);

    assertNotNull(deletedFile, "Deleted file not found");

    assertFalse(deletedFile.diffChunks().isEmpty(), "Deleted file should have diff chunks");

    DiffChunk deletedChunk = deletedFile.diffChunks().get(0);

    assertNotNull(
        deletedChunk.sourceLineRange(), "Source line range should not be null for deleted file");

    assertEquals(
        LineRange.ChangeType.CHANGE,
        deletedChunk.sourceLineRange().changeType(),
        "Change type for deleted source should be CHANGE");

    assertNull(deletedChunk.targetLineRange(), "Target line range should be null for deleted file");

    // --- Test for a new file
    // (java-parser/src/main/java/com/sono99/javaparser/api/v1/model/JavadocNodeV1.java) ---

    String newFilePathForTesting =
        "java-parser/src/main/java/com/sono99/javaparser/api/v1/model/JavadocNodeV1.java"; // Corrected path

    DiffedFile newFile =
        parsedDiff.topLevelResult().diffedFiles().stream()
            .filter(file -> file.filePath().equals(newFilePathForTesting))
            .findFirst()
            .orElse(null);
    assertNotNull(newFile, "New file not found");
    assertFalse(newFile.diffChunks().isEmpty(), "New file should have diff chunks");
    DiffChunk newChunk = newFile.diffChunks().get(0);

    assertNull(newChunk.sourceLineRange(), "Source line range should be null for new file");
    assertNotNull(newChunk.targetLineRange(), "Target line range should not be null for new file");
    assertEquals(
        LineRange.ChangeType.CHANGE,
        newChunk.targetLineRange().changeType(),
        "Change type for new target should be Change");
  }

  /**
   * Validates that the totalFilesChanged count is correct. Counts the number of file entries in
   * git_diff_01.diff and asserts that getTotalFilesChanged() returns the expected number.
   */
  @Test
  void shouldValidateTotalFilesChanged() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    // Count the number of 'diff --git' lines
    long expectedFilesChanged =
        diffContent.lines().filter(line -> line.startsWith("diff --git")).count();
    assertEquals(expectedFilesChanged, parsedDiff.topLevelResult().totalFilesChanged());
  }

  /**
   * Validates that the totalAdditions count is correct. Manually counts the number of addition
   * lines in git_diff_01.diff and asserts that getTotalAdditions() returns the expected number.
   */
  @Test
  void shouldValidateTotalAdditions() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    // Count lines starting with '+' but not '+++'
    long expectedAdditions =
        diffContent.lines().filter(line -> line.startsWith("+") && !line.startsWith("+++")).count();
    assertEquals(expectedAdditions, parsedDiff.topLevelResult().totalAdditions());
  }

  /**
   * Validates that the totalDeletions count is correct. Manually counts the number of deletion
   * lines in git_diff_01.diff and asserts that getTotalDeletions() returns the expected number.
   */
  @Test
  void shouldValidateTotalDeletions() throws IOException {
    // Given
    String diffContent = BasicTestHelper.readTestResourceToString("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertTrue(parsedDiff.isValid());
    // Count lines starting with '-' but not '---'
    long expectedDeletions =
        diffContent.lines().filter(line -> line.startsWith("-") && !line.startsWith("---")).count();
    assertEquals(expectedDeletions, parsedDiff.topLevelResult().totalDeletions());
  }

  /**
   * Tests that the service correctly handles an empty diff string. Asserts that the result is
   * marked as invalid and that all statistics are zero.
   */
  @Test
  void shouldHandleEmptyDiffString() {
    // Given
    String emptyDiff = "";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(emptyDiff);

    // Then
    assertFalse(parsedDiff.isValid());
    assertEquals(0, parsedDiff.topLevelResult().totalFilesChanged());
    assertEquals(0, parsedDiff.topLevelResult().totalAdditions());
    assertEquals(0, parsedDiff.topLevelResult().totalDeletions());
  }

  /**
   * Tests that the service correctly handles a malformed diff string. Asserts that the result is
   * invalid and statistics are zero. The service should not throw an unhandled exception.
   */
  @Test
  void shouldHandleMalformedDiffString() {
    // Given
    String malformedDiff = "hello world\nnot a diff";

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(malformedDiff);

    // Then
    assertFalse(parsedDiff.isValid());
    assertEquals(0, parsedDiff.topLevelResult().totalFilesChanged());
    assertEquals(0, parsedDiff.topLevelResult().totalAdditions());
    assertEquals(0, parsedDiff.topLevelResult().totalDeletions());
  }

  /**
   * Tests that the service correctly handles a diff with no changes. Parses a diff file that
   * contains file headers but no actual hunks or changes. Asserts that statistics are zero.
   */
  @Test
  void shouldHandleDiffWithNoChanges() {
    // Given: A diff with file headers but no changes
    String noChangesDiff =
        """
        diff --git a/test.txt b/test.txt
        index 1234567..abcdef0 100644
        --- a/test.txt
        +++ b/test.txt
        """;

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(noChangesDiff);

    // Then
    assertTrue(parsedDiff.isValid()); // Valid diff structure, just no changes
    assertEquals(1, parsedDiff.topLevelResult().totalFilesChanged()); // One file mentioned
    assertEquals(0, parsedDiff.topLevelResult().totalAdditions());
    assertEquals(0, parsedDiff.topLevelResult().totalDeletions());
  }

  /**
   * Tests a single-file, single-hunk diff. Performs all detailed assertions from Phase 3 on this
   * simple case to isolate logic.
   */
  @Test
  void shouldHandleSingleFileSingleHunkDiff() {
    // Given: Minimal diff with one file and one hunk
    String singleFileDiff =
        """
        diff --git a/test.txt b/test.txt
        index 1234567..abcdef0 100644
        --- a/test.txt
        +++ b/test.txt
        @@ -1,2 +1,3 @@
         line1
        -line2
        +line2 modified
        +line3
        """;

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(singleFileDiff);

    // Then
    assertTrue(parsedDiff.isValid());
    assertEquals(1, parsedDiff.topLevelResult().totalFilesChanged());
    assertEquals(2, parsedDiff.topLevelResult().totalAdditions()); // +line2 modified, +line3
    assertEquals(1, parsedDiff.topLevelResult().totalDeletions()); // -line2

    // Validate file details
    assertEquals(1, parsedDiff.topLevelResult().diffedFiles().size());
    DiffedFile file = parsedDiff.topLevelResult().diffedFiles().get(0);
    assertEquals("test.txt", file.filePath());
    assertFalse(file.isDeleted());
    assertFalse(file.isCreatedNewInMergeRequest());
    assertFalse(file.isRenamed());
  }
}
