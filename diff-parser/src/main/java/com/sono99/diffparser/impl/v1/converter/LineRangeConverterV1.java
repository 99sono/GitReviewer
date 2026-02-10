package com.sono99.diffparser.impl.v1.converter;

import com.sono99.diffparser.api.v1.model.LineRangeV1;
import com.sono99.diffparser.impl.generic.model.LineRange;
import com.sono99.diffparser.impl.v1.service.ConversionServiceV1;
import com.sono99.diffparser.impl.v1.service.NodeConverterV1;
import org.springframework.stereotype.Component;

/** Converter to transform a generic {@link LineRange} to a {@link LineRangeV1}. */
@Component
public class LineRangeConverterV1 implements NodeConverterV1<LineRange, LineRangeV1> {

  /**
   * Converts a generic {@link LineRange} to a {@link LineRangeV1}.
   *
   * @param source the generic model
   * @param conversionService the conversion service
   * @return the v1 DTO
   */
  @Override
  public LineRangeV1 convert(LineRange source, ConversionServiceV1 conversionService) {
    return new LineRangeV1(
        source.startLineIndex(), source.endLineIndex(), convertChangeType(source.changeType()));
  }

  /**
   * Converts the change type from generic to v1.
   *
   * @param source the generic change type
   * @return the v1 change type
   */
  private LineRangeV1.ChangeType convertChangeType(LineRange.ChangeType source) {
    return switch (source) {
      case INSERT -> LineRangeV1.ChangeType.INSERT;
      case DELETE -> LineRangeV1.ChangeType.DELETE;
      case CHANGE -> LineRangeV1.ChangeType.CHANGE;
      case EQUAL -> LineRangeV1.ChangeType.EQUAL;
    };
  }

  /**
   * Gets the class of the source type.
   *
   * @return the source type class
   */
  @Override
  public Class<LineRange> getSourceType() {
    return LineRange.class;
  }

  /**
   * Gets the class of the target type.
   *
   * @return the target type class
   */
  @Override
  public Class<LineRangeV1> getTargetType() {
    return LineRangeV1.class;
  }
}
