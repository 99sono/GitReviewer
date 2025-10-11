package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.AbstractJavaNodeV1;
import com.sono99.javaparser.api.v1.model.AnnotationNodeV1;
import com.sono99.javaparser.api.v1.model.JavadocNodeV1;
import com.sono99.javaparser.api.v1.model.MethodDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.model.MethodDeclarationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link MethodDeclarationNode} to a {@link
 * MethodDeclarationNodeV1}.
 */
@Component
public class MethodDeclarationNodeConverterV1
    implements NodeConverterV1<MethodDeclarationNode, MethodDeclarationNodeV1> {

  @Override
  public MethodDeclarationNodeV1 convert(
      MethodDeclarationNode source, ConversionServiceV1 conversionService) {
    @SuppressWarnings("unchecked")
    List<AbstractJavaNodeV1<?>> children =
        (List<AbstractJavaNodeV1<?>>)
            (List<?>)
                source.getChildren().stream()
                    .map(child -> conversionService.convert(child, child.getClass()))
                    .collect(Collectors.toList());

    return new MethodDeclarationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getName(),
        source.getSignature(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        source.getJavadoc() == null
            ? null
            : conversionService.convert(source.getJavadoc(), JavadocNodeV1.class),
        source.getAnnotations().stream()
            .map(annotation -> conversionService.convert(annotation, AnnotationNodeV1.class))
            .toList(),
        children);
  }

  @Override
  public Class<MethodDeclarationNode> getSourceType() {
    return MethodDeclarationNode.class;
  }

  @Override
  public Class<MethodDeclarationNodeV1> getTargetType() {
    return MethodDeclarationNodeV1.class;
  }
}
