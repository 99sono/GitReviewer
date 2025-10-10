package com.sono99.javaparser.impl.v1.converter;

import com.sono99.javaparser.api.v1.model.PackageDeclarationNodeV1;
import com.sono99.javaparser.impl.generic.model.PackageDeclarationNode;
import com.sono99.javaparser.impl.v1.service.ConversionServiceV1;
import com.sono99.javaparser.impl.v1.service.NodeConverterV1;
import java.util.Collections;
import org.springframework.stereotype.Component;

/**
 * Converter to transform a generic {@link PackageDeclarationNode} to a {@link
 * PackageDeclarationNodeV1}.
 */
@Component
public class PackageDeclarationNodeConverterV1
    implements NodeConverterV1<PackageDeclarationNode, PackageDeclarationNodeV1> {

  @Override
  public PackageDeclarationNodeV1 convert(
      PackageDeclarationNode source, ConversionServiceV1 conversionService) {
    return new PackageDeclarationNodeV1(
        source.getStartLine(),
        source.getEndLine(),
        source.getPackageName(),
        source.getJavaChunk(),
        source.getOriginalNode(),
        Collections.emptyList());
  }

  @Override
  public Class<PackageDeclarationNode> getSourceType() {
    return PackageDeclarationNode.class;
  }

  @Override
  public Class<PackageDeclarationNodeV1> getTargetType() {
    return PackageDeclarationNodeV1.class;
  }
}
