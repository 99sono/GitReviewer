package com.sono99.diffparser.impl.v1.service;

/**
 * Generic interface for converting objects from a source type to a target type within the v1 API
 * layer. Implementations of this interface are managed by {@link ConversionServiceV1}.
 *
 * @param <S> the source type to convert from
 * @param <T> the target type to convert to
 */
public interface NodeConverterV1<S, T> {

  /**
   * Converts the source object to the target type.
   *
   * @param source the source object
   * @param conversionService the conversion service for nested conversions
   * @return the converted object
   */
  T convert(S source, ConversionServiceV1 conversionService);

  /**
   * Gets the class of the source type.
   *
   * @return the source type class
   */
  Class<S> getSourceType();

  /**
   * Gets the class of the target type.
   *
   * @return the target type class
   */
  Class<T> getTargetType();
}
