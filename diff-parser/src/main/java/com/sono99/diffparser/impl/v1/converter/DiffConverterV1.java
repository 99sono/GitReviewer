package com.sono99.diffparser.impl.v1.converter;

import com.sono99.diffparser.api.v1.model.ParsedDiffV1;
import com.sono99.diffparser.api.v1.model.TopLevelDiffResultV1;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.generic.model.TopLevelDiffResult;
import com.sono99.diffparser.impl.v1.service.ConversionServiceV1;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter to transform generic diff models to v1 API DTOs. Handles the conversion of ParsedDiff
 * and TopLevelDiffResult objects while sanitizing internal implementation details.
 *
 * <p>This converter ensures that the public v1 API exposes only stable, sanitized DTOs without
 * leaking internal model references or implementation details.
 */
@Component
public class DiffConverterV1 {

  @Autowired private ConversionServiceV1 conversionService;

  /**
   * Converts a generic ParsedDiff to a ParsedDiffV1.
   *
   * @param source the generic ParsedDiff
   * @return the v1 ParsedDiffV1
   */
  public ParsedDiffV1 convertParsedDiff(ParsedDiff source) {
    TopLevelDiffResultV1 topLevelResult = convertTopLevelDiffResult(source.topLevelResult());
    return new ParsedDiffV1(topLevelResult, source.getOriginalDiffContent(), source.isValid());
  }

  /**
   * Converts a generic TopLevelDiffResult to a TopLevelDiffResultV1.
   *
   * @param source the generic TopLevelDiffResult
   * @return the v1 TopLevelDiffResultV1
   */
  public TopLevelDiffResultV1 convertTopLevelDiffResult(TopLevelDiffResult source) {
    @SuppressWarnings("unchecked")
    List<com.sono99.diffparser.api.v1.model.DiffedFileV1> diffedFiles =
        (List<com.sono99.diffparser.api.v1.model.DiffedFileV1>)
            (List<?>)
                source.diffedFiles().stream()
                    .map(
                        file ->
                            conversionService.convert(
                                file, com.sono99.diffparser.api.v1.model.DiffedFileV1.class))
                    .toList();
    return new TopLevelDiffResultV1(
        diffedFiles,
        source.diffHeader(),
        source.totalFilesChanged(),
        source.totalAdditions(),
        source.totalDeletions());
  }
}
