package com.sono99.diffparser.impl.generic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    assertEquals(deletedFileNewFilePathExpected, deletedFile.filePath());

    List<DiffChunk> diffChunks = deletedFile.diffChunks();
    assertEquals(1, diffChunks.size());
    DiffChunk diffChunkIdx0 = diffChunks.get(0);
    assertNotNull(diffChunkIdx0.originalDelta());
    assertNotNull(diffChunkIdx0.sourceLineRange());
    assertNull(diffChunkIdx0.targetLineRange());
  }

  private String readDiffContent(String resourceFileName) throws IOException {
    Path resourcePath = Path.of("src/test/resources/diff-examples/" + resourceFileName);
    return Files.readString(resourcePath);
  }
}
