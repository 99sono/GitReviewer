package com.sono99.javaparser.impl.v1;

import com.sono99.javaparser.api.v1.model.CompilcationUnitNodeV1;
import com.sono99.javaparser.api.v1.service.JavaParsingServiceV1;
import java.io.InputStream;

public class JavaParsingServiceV1Impl implements JavaParsingServiceV1 {

  @Override
  public CompilcationUnitNodeV1 parse(InputStream source, String sourceName) {
    // FIXME: Implement V1 API wrapper once generic parsing service is stable
    // This will convert from internal CompilationUnitNode to V1 DTOs
    throw new UnsupportedOperationException(
        "V1 API not yet implemented - focus on generic service first");
  }

  @Override
  public boolean canParse(InputStream source, String sourceName) {
    // FIXME: Delegate to generic service once available
    // Simple content validation - extension check and basic header inspection
    if (sourceName != null && sourceName.endsWith(".java")) {
      return true;
    }
    // FIXME: Add basic source code inspection
    return false;
  }
}
