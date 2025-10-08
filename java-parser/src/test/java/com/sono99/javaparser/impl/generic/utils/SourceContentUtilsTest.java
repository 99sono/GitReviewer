package com.sono99.javaparser.impl.generic.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for SourceContentUtils service and SourceContentHelper. Tests text processing
 * utilities and cached line operations for Java source code manipulation.
 */
class SourceContentUtilsTest {

  private final SourceContentUtils utils = new SourceContentUtils();

  /**
   * Tests that SourceContentUtils can create a SourceContentHelper from a source string. Verifies
   * the helper contains correct line count and original source.
   */
  @Test
  void shouldCreateHelperFromSource() {
    // Given
    String source = "line1\nline2\nline3";

    // When
    var helper = utils.createHelper(source);

    // Then
    assertThat(helper).isNotNull();
    assertThat(helper.getTotalLines()).isEqualTo(3);
    assertThat(helper.getOriginalSource()).isEqualTo(source);
  }

  /**
   * Tests extraction of a single line from cached source content. Verifies single line extraction
   * returns just the line content without terminator.
   */
  @Test
  void helperShouldExtractSingleLine() {
    // Given
    String source = "line1\nline2\nline3";
    var helper = utils.createHelper(source);
    int startLine = 2;
    int endLine = 2;

    // When
    String result = utils.extractSourceRange(helper, startLine, endLine);

    // Then
    assertThat(result).isEqualTo("line2");
  }

  /**
   * Tests extraction of multiple consecutive lines from cached source content. Verifies that
   * terminators are included between lines but not at the end.
   */
  @Test
  void helperShouldExtractMultipleLines() {
    // Given
    String source = "line1\nline2\nline3\nline4";
    var helper = utils.createHelper(source);
    int startLine = 2;
    int endLine = 3;

    // When
    String result = utils.extractSourceRange(helper, startLine, endLine);

    // Then
    assertThat(result).isEqualTo("line2\nline3");
  }

  /**
   * Tests extraction with Windows line endings (CRLF) to ensure proper terminator detection.
   * Verifies that \r\n terminators are preserved correctly in extracted ranges.
   */
  @Test
  void helperShouldExtractWithWindowsLineEndings() {
    // Given
    String source = "line1\r\nline2\r\nline3";
    var helper = utils.createHelper(source);
    int startLine = 1;
    int endLine = 2;

    // When
    String result = utils.extractSourceRange(helper, startLine, endLine);

    // Then
    assertThat(result).isEqualTo("line1\r\nline2");
  }

  /** Tests extraction with empty source file. */
  @Test
  void helperShouldHandleEmptySource() {
    // Given
    String source = "";
    var helper = utils.createHelper(source);
    int startLine = 1;
    int endLine = 1;

    // When
    String result = utils.extractSourceRange(helper, startLine, endLine);

    // Then
    assertThat(result).isEmpty();
  }

  /** Tests extraction with out-of-bounds start line. */
  @Test
  void helperShouldHandleOutOfBoundsStartLine() {
    // Given
    String source = "line1\nline2";
    var helper = utils.createHelper(source);
    int startLine = 5; // Beyond source length
    int endLine = 5;

    // When/Then - Should not throw, return empty
    String result = utils.extractSourceRange(helper, startLine, endLine);
    assertThat(result).isEmpty();
  }

  /** Tests getting line content from cached helper. */
  @Test
  void shouldGetLineFromHelper() {
    // Given
    String source = "line1\nline2\nline3";
    var helper = utils.createHelper(source);

    // When
    String result = utils.getLine(helper, 2);

    // Then
    assertThat(result).isEqualTo("line2");
  }

  /** Tests getting total line count from helper. */
  @Test
  void shouldGetTotalLinesFromHelper() {
    // Given
    String source = "line1\nline2\nline3";
    var helper = utils.createHelper(source);

    // When
    int count = utils.getTotalLines(helper);

    // Then
    assertThat(count).isEqualTo(3);
  }

  /** Tests getting original source from helper. */
  @Test
  void shouldGetOriginalSourceFromHelper() {
    // Given
    String source = "line1\nline2\nline3";
    var helper = utils.createHelper(source);

    // When
    String result = utils.getOriginalSource(helper);

    // Then
    assertThat(result).isEqualTo(source);
  }

  /** Tests rejecting null helper for extractSourceRange. */
  @Test
  void shouldRejectNullHelperForExtract() {
    assertThatThrownBy(() -> utils.extractSourceRange(null, 1, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("SourceContentHelper cannot be null");
  }

  /** Tests rejecting invalid line ranges. */
  @Test
  void shouldRejectInvalidLineRanges() {
    String source = "line1\nline2\nline3";
    var helper = utils.createHelper(source);

    // Zero start line
    assertThatThrownBy(() -> utils.extractSourceRange(helper, 0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Start line must be >= 1, got: 0");

    // End before start
    assertThatThrownBy(() -> utils.extractSourceRange(helper, 2, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("End line (1) must be >= start line (2)");
  }

  /** Tests rejecting null helper for getLine. */
  @Test
  void shouldHandleNullHelperForGetLine() {
    assertThat(utils.getLine(null, 1)).isNull();
  }

  /** Tests rejecting null helper for getTotalLines. */
  @Test
  void shouldHandleNullHelperForGetTotalLines() {
    assertThat(utils.getTotalLines(null)).isEqualTo(0);
  }

  /** Tests rejecting null helper for getOriginalSource. */
  @Test
  void shouldHandleNullHelperForGetOriginalSource() {
    assertThat(utils.getOriginalSource(null)).isNull();
  }
}
