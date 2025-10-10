package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.JavadocNodeV1;
import com.sono99.javaparser.impl.generic.model.JavadocNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import org.springframework.stereotype.Component;

/** Converter to transform a generic {@link JavadocNode} to a {@link JavadocNodeV1}. */
@Component
public class JavadocNodeConverterV1 implements NodeConverterV1<JavadocNode, JavadocNodeV1> {

  @Override
  public JavadocNodeV1 convert(JavadocNode source, ConversionServiceV1 conversionService) {
    return new JavadocNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        java.util.List.of());
  }

  @Override
  public Class<JavadocNode> getSourceType() {
    return JavadocNode.class;
  }

  @Override
  public Class<JavadocNodeV1> getTargetType() {
    return JavadocNodeV1.class;
  }
}
