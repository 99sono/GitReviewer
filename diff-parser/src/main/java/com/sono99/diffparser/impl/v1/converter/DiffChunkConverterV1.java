package com.sono99.diffparser.impl.v1.converter;

import com.sono99.diffparser.api.v1.model.DiffChunkV1;
import com.sono99.diffparser.api.v1.model.LineRangeV1;
import com.sono99.diffparser.impl.generic.model.DiffChunk;
import com.sono99.diffparser.impl.v1.service.ConversionServiceV1;
import com.sono99.diffparser.impl.v1.service.NodeConverterV1;
import java.util.List;
import org.springframework.stereotype.Component;

/** Converter to transform a generic {@link DiffChunk} to a {@link DiffChunkV1}. */
@Component
public class DiffChunkConverterV1 implements NodeConverterV1<DiffChunk, DiffChunkV1> {

  @Override
  public DiffChunkV1 convert(DiffChunk source, ConversionServiceV1 conversionService) {
    List<LineRangeV1> lineRanges = List.of();
    if (source.sourceLineRange() != null) {
      lineRanges = List.of(conversionService.convert(source.sourceLineRange(), LineRangeV1.class));
    }
    if (source.targetLineRange() != null) {
      LineRangeV1 target = conversionService.convert(source.targetLineRange(), LineRangeV1.class);
      lineRanges = lineRanges.isEmpty() ? List.of(target) : List.of(lineRanges.get(0), target);
    }
    return new DiffChunkV1(
        0, // hunkStartLine - not available
        0, // hunkEndLine - not available
        lineRanges,
        "", // contextBefore - not available
        "", // contextAfter - not available
        ""); // diffText - not available
  }

  @Override
  public Class<DiffChunk> getSourceType() {
    return DiffChunk.class;
  }

  @Override
  public Class<DiffChunkV1> getTargetType() {
    return DiffChunkV1.class;
  }
}
