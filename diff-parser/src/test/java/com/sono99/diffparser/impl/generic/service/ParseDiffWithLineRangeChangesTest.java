package com.sono99.diffparser.impl.generic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.utils.BasicTestHelper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {DiffParserService.class})
class ParseDiffWithLineRangeChangesTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ParseDiffWithLineRangeChangesTest.class);

  @Autowired private DiffParserService diffParserService;

  @Test
  void shouldReturnCorrectChunkLinesAsStringForSourceAndTarget() throws IOException {
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

    DiffChunk diffChunk = diffedFile.diffChunks().get(0);

    // Validate source chunk lines
    assertNotNull(diffChunk.sourceLineRange(), "Source line range should not be null");
    String expectedSourceLines =
        BasicTestHelper.readTestResourceToString("git_diff_02_expected_old_chunk.txt");
    assertEquals(expectedSourceLines, diffChunk.sourceLineRange().getChunkLinesAsString());

    // Validate target chunk lines
    assertNotNull(diffChunk.targetLineRange(), "Target line range should not be null");
    String expectedTargetLines =
        BasicTestHelper.readTestResourceToString("git_diff_02_expected_new_chunk.txt");
    assertEquals(expectedTargetLines, diffChunk.targetLineRange().getChunkLinesAsString());

    // Validate changed line numbers for source chunk
    assertEquals(1, diffChunk.sourceLineRange().getChangedLineNumbers().size());
    assertTrue(diffChunk.sourceLineRange().getChangedLineNumbers().contains(49));

    // Validate changed line numbers for target chunk
    assertEquals(2, diffChunk.targetLineRange().getChangedLineNumbers().size());
    assertTrue(diffChunk.targetLineRange().getChangedLineNumbers().contains(45));
    assertTrue(diffChunk.targetLineRange().getChangedLineNumbers().contains(50));

    // Validate changed line number to line content map for target chunk (first validation)
    Map<Integer, String> targetChangedLinesMap =
        diffChunk.targetLineRange().getChangedLineNumberToLineContentMap();
    assertNotNull(targetChangedLinesMap);
    assertEquals(2, targetChangedLinesMap.size());
    assertEquals(
        "      // added a dummy line at line number 45 of this file.",
        targetChangedLinesMap.get(45));
    assertEquals(
        "      // (b) Convert to our domain model (dummy modification line 50)",
        targetChangedLinesMap.get(50));

    // Validate changed line number to line content map for source chunk
    Map<Integer, String> sourceChangedLinesMap =
        diffChunk.sourceLineRange().getChangedLineNumberToLineContentMap();
    assertNotNull(sourceChangedLinesMap);
    assertEquals(1, sourceChangedLinesMap.size());
    assertEquals("      // (b) Convert to our domain model", sourceChangedLinesMap.get(49));
  }
}
