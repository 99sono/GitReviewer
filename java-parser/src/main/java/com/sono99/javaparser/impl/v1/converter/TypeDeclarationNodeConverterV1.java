package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.AbstractJavaNodeV1;
import com.sono99.javaparser.api.v1.model.JavadocNodeV1;
import com.sono99.javaparser.api.v1.model.TypeDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.model.TypeDeclarationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link TypeDeclarationNode} to a {@link TypeDeclarationNodeV1}.
 */
@Component
public class TypeDeclarationNodeConverterV1
    implements NodeConverterV1<TypeDeclarationNode, TypeDeclarationNodeV1> {

  @Override
  public TypeDeclarationNodeV1 convert(
      TypeDeclarationNode source, ConversionServiceV1 conversionService) {
    @SuppressWarnings("unchecked")
    List<AbstractJavaNodeV1<?>> children =
        (List<AbstractJavaNodeV1<?>>)
            (List<?>)
                source.getChildren().stream()
                    .map(child -> conversionService.convert(child, AbstractJavaNodeV1.class))
                    .collect(Collectors.toList());
    return new TypeDeclarationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getName(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        source.getJavadoc() == null
            ? null
            : conversionService.convert(source.getJavadoc(), JavadocNodeV1.class),
        children);
  }

  @Override
  public Class<TypeDeclarationNode> getSourceType() {
    return TypeDeclarationNode.class;
  }

  @Override
  public Class<TypeDeclarationNodeV1> getTargetType() {
    return TypeDeclarationNodeV1.class;
  }
}
