package com.sono99.diffparser.impl.generic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

  void shouldParseGitDiff01Successfully() throws IOException {
    // Given
    String diffContent = readDiffContent("git_diff_01.diff");

    // When
    ParsedDiff parsedDiff = diffParserService.parseUnifiedDiff(diffContent);

    // Then
    assertNotNull(parsedDiff);
    assertTrue(parsedDiff.isValid());
    assertEquals(diffContent, parsedDiff.getOriginalDiffContent());
  }

  @Test
  void shouldDetectFileDeletion() throws IOException {
    // Given
    String diffContent = readDiffContent("git_diff_01.diff");
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
    assertEquals("/dev/null", deletedFile.filePath());
  }

  @Test
  void shouldDetectFileCreation() throws IOException {
    // Given
    String diffContent = readDiffContent("git_diff_01.diff");
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

  @Test
  void shouldDetectFileModification() throws IOException {
    // Given
    String diffContent = readDiffContent("git_diff_01.diff");
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

  @Test
  void shouldDetectFileRename() throws IOException {
    // Given
    String diffContent = readDiffContent("git_diff_03_rename.diff");
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

  private String readDiffContent(String resourceFileName) throws IOException {
    Path resourcePath = Path.of("src/test/resources/diff-examples/" + resourceFileName);
    return Files.readString(resourcePath);
  }
}
