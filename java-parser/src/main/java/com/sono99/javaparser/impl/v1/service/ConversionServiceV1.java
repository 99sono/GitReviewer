package com.sono99.javaparser.impl.v1.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service that manages and provides conversion between different node types. It discovers all
 * {@link NodeConverterV1} beans and registers them for use.
 */
@Service
public class ConversionServiceV1 {

  private final Map<Class<?>, NodeConverterV1<?, ?>> converterRegistry = new HashMap<>();

  /**
   * Registers all {@link NodeConverterV1} beans found in the Spring context.
   *
   * @param converters the list of converters to register
   */
  @Autowired
  public void registerConverters(List<NodeConverterV1<?, ?>> converters) {
    converters.forEach(this::registerConverter);
  }

  private void registerConverter(NodeConverterV1<?, ?> converter) {
    converterRegistry.put(converter.getSourceType(), converter);
  }

  /**
   * Converts the given source object to the specified target type.
   *
   * @param source the source object to convert
   * @param targetType the target type class
   * @param <T> the source type
   * @param <R> the target type
   * @return the converted object
   * @throws IllegalArgumentException if no converter is found for the source type
   */
  @SuppressWarnings("unchecked")
  public <T, R> R convert(T source, Class<R> targetType) {
    if (source == null) {
      return null;
    }
    NodeConverterV1<T, R> converter =
        (NodeConverterV1<T, R>) converterRegistry.get(source.getClass());
    if (converter == null) {
      // Try to find a converter for a superclass
      for (Class<?> key : converterRegistry.keySet()) {
        if (key.isInstance(source)) {
          converter = (NodeConverterV1<T, R>) converterRegistry.get(key);
          break;
        }
      }
    }

    if (converter == null) {
      throw new IllegalArgumentException("No converter found for " + source.getClass());
    }
    return converter.convert(source, this);
  }
}
