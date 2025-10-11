package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.ImportDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.model.ImportDeclarationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link ImportDeclarationNode} to a {@link
 * ImportDeclarationNodeV1}.
 */
@Component
public class ImportDeclarationNodeConverterV1
    implements NodeConverterV1<ImportDeclarationNode, ImportDeclarationNodeV1> {

  @Override
  public ImportDeclarationNodeV1 convert(
      ImportDeclarationNode source, ConversionServiceV1 conversionService) {
    return new ImportDeclarationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getImportName(),
        source.isStatic(),
        source.isAsterisk(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        java.util.List.of());
  }

  @Override
  public Class<ImportDeclarationNode> getSourceType() {
    return ImportDeclarationNode.class;
  }

  @Override
  public Class<ImportDeclarationNodeV1> getTargetType() {
    return ImportDeclarationNodeV1.class;
  }
}
