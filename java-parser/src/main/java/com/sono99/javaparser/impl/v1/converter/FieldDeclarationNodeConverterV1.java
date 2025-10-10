package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.AnnotationNodeV1;
import com.sono99.javaparser.api.v1.model.FieldDeclarationNodeV1;
import com.sono99.javaparser.api.v1.model.JavadocNodeV1;
import com.sono99.javaparser.impl.generic.model.FieldDeclarationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link FieldDeclarationNode} to a {@link
 * FieldDeclarationNodeV1}.
 */
@Component
public class FieldDeclarationNodeConverterV1
    implements NodeConverterV1<FieldDeclarationNode, FieldDeclarationNodeV1> {

  @Override
  public FieldDeclarationNodeV1 convert(
      FieldDeclarationNode source, ConversionServiceV1 conversionService) {
    return new FieldDeclarationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getName(),
        source.getType(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        source.getJavadoc() == null
            ? null
            : conversionService.convert(source.getJavadoc(), JavadocNodeV1.class),
        source.getAnnotations().stream()
            .map(annotation -> conversionService.convert(annotation, AnnotationNodeV1.class))
            .collect(Collectors.toList()),
        java.util.List.of());
  }

  @Override
  public Class<FieldDeclarationNode> getSourceType() {
    return FieldDeclarationNode.class;
  }

  @Override
  public Class<FieldDeclarationNodeV1> getTargetType() {
    return FieldDeclarationNodeV1.class;
  }
}
