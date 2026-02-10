package com.sono99.diffparser.impl.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.diffparser.api.v1.model.ParsedDiffV1;
import com.sono99.diffparser.api.v1.service.DiffParserServiceV1;
import com.sono99.diffparser.impl.generic.service.DiffParserService;
import com.sono99.diffparser.impl.v1.converter.DiffConverterV1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for DiffParserServiceV1Impl. Tests the complete v1 API pipeline from input to
 * sanitized DTOs.
 */
@SpringBootTest
@ContextConfiguration(
    classes = {
      DiffParserService.class,
      DiffParserServiceV1Impl.class,
      DiffConverterV1.class,
      ConversionServiceV1.class,
      // Include all converters
      com.sono99.diffparser.impl.v1.converter.DiffedFileConverterV1.class,
      com.sono99.diffparser.impl.v1.converter.DiffChunkConverterV1.class,
      com.sono99.diffparser.impl.v1.converter.LineRangeConverterV1.class,
    })
class DiffParserServiceV1ImplTest {

  @Autowired private DiffParserServiceV1 service;

  /** Test that the v1 service can parse a simple diff and return a valid ParsedDiffV1. */
  @Test
  void shouldParseSimpleDiffSuccessfully() {
    // Given
    String simpleDiff =
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
    ParsedDiffV1 result = service.parseUnifiedDiff(simpleDiff);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isTrue();
    assertThat(result.originalDiffContent()).isEqualTo(simpleDiff);
    assertThat(result.topLevelResult()).isNotNull();
    assertThat(result.topLevelResult().totalFilesChanged()).isEqualTo(1);
    assertThat(result.topLevelResult().totalAdditions()).isEqualTo(2);
    assertThat(result.topLevelResult().totalDeletions()).isEqualTo(1);
  }

  /** Test that the v1 service handles empty input gracefully. */
  @Test
  void shouldHandleEmptyInput() {
    // Given
    String emptyDiff = "";

    // When
    ParsedDiffV1 result = service.parseUnifiedDiff(emptyDiff);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isFalse();
  }
}
