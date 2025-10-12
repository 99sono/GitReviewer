package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.AbstractJavaNodeV1;
import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link CompilationUnitNode} to a {@link CompilationUnitNodeV1}.
 */
@Component
public class CompilationUnitNodeConverterV1
    implements NodeConverterV1<CompilationUnitNode, CompilationUnitNodeV1> {

  @Override
  public CompilationUnitNodeV1 convert(
      CompilationUnitNode source, ConversionServiceV1 conversionService) {
    @SuppressWarnings("unchecked")
    List<AbstractJavaNodeV1<?>> children =
        (List<AbstractJavaNodeV1<?>>)
            (List<?>)
                source.getChildren().stream()
                    .map(child -> conversionService.convert(child, AbstractJavaNodeV1.class))
                    .collect(Collectors.toList());
    return new CompilationUnitNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        source.getRepositoryPath(),
        children);
  }

  @Override
  public Class<CompilationUnitNode> getSourceType() {
    return CompilationUnitNode.class;
  }

  @Override
  public Class<CompilationUnitNodeV1> getTargetType() {
    return CompilationUnitNodeV1.class;
  }
}
