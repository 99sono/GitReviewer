package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.AnnotationNodeV1;
import com.sono99.javaparser.impl.generic.model.AnnotationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import org.springframework.stereotype.Component;

/** Converter to transform a generic {@link AnnotationNode} to a {@link AnnotationNodeV1}. */
@Component
public class AnnotationNodeConverterV1
    implements NodeConverterV1<AnnotationNode, AnnotationNodeV1> {

  @Override
  public AnnotationNodeV1 convert(AnnotationNode source, ConversionServiceV1 conversionService) {
    return new AnnotationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getName(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        java.util.List.of());
  }

  @Override
  public Class<AnnotationNode> getSourceType() {
    return AnnotationNode.class;
  }

  @Override
  public Class<AnnotationNodeV1> getTargetType() {
    return AnnotationNodeV1.class;
  }
}
