package com.sono99.javaparser.impl.v1.service;

/**
 * Defines a generic interface for converting between different node types, typically from a generic
 * model to a versioned API model.
 *
 * @param <T> the source type
 * @param <R> the target type
 */
public interface NodeConverterV1<T, R> {

  /**
   * Converts the source object to the target type.
   *
   * @param source the source object to convert
   * @param conversionService the conversion service for recursive conversions
   * @return the converted object
   */
  R convert(T source, ConversionServiceV1 conversionService);

  /**
   * Returns the class of the source type.
   *
   * @return the source type class
   */
  Class<T> getSourceType();

  /**
   * Returns the class of the target type.
   *
   * @return the target type class
   */
  Class<R> getTargetType();
}
