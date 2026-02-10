package com.sono99.diffparser.impl.v1.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for converting objects between types using registered converters. Manages a
 * collection of NodeConverterV1 implementations and provides a unified conversion API for the v1
 * layer, ensuring proper transformation from internal models to public DTOs.
 */
@Service
public class ConversionServiceV1 {

  private final Map<Class<?>, NodeConverterV1<?, ?>> convertersBySourceType;

  /**
   * Constructs the conversion service with the list of available converters. Builds an internal map
   * for efficient lookup by source type.
   *
   * @param converters the list of node converters to register
   */
  @Autowired
  public ConversionServiceV1(List<NodeConverterV1<?, ?>> converters) {
    this.convertersBySourceType =
        converters.stream()
            .collect(Collectors.toMap(NodeConverterV1::getSourceType, Function.identity()));
  }

  /**
   * Converts the given source object to the specified target type. Looks up the appropriate
   * converter based on the source object's class and performs the conversion.
   *
   * @param <T> the target type
   * @param source the source object to convert
   * @param targetType the class of the target type
   * @return the converted object of the target type
   * @throws IllegalArgumentException if no converter is found for the source type or target type
   *     mismatch
   */
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<T> targetType) {
    if (source == null) {
      return null;
    }

    NodeConverterV1<?, ?> converter = convertersBySourceType.get(source.getClass());
    if (converter == null) {
      throw new IllegalArgumentException(
          "No converter found for source type: " + source.getClass());
    }

    if (!converter.getTargetType().equals(targetType)) {
      throw new IllegalArgumentException(
          "Converter target type "
              + converter.getTargetType()
              + " does not match requested type "
              + targetType);
    }

    // Safe cast since we checked the types
    @SuppressWarnings("rawtypes")
    NodeConverterV1 rawConverter = converter;
    return (T) rawConverter.convert(source, this);
  }
}
