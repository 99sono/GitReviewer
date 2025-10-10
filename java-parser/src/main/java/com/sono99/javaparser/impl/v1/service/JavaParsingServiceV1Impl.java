package com.sono99.javaparser.impl.v1.service;

import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
import com.sono99.javaparser.api.v1.service.JavaParsingServiceV1;
import com.sono99.javaparser.impl.generic.model.CompilationUnitNode;
import com.sono99.javaparser.impl.generic.service.JavaParserService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JavaParsingServiceV1Impl implements JavaParsingServiceV1 {

  @Autowired private JavaParserService genericService;
  @Autowired private ConversionServiceV1 conversionService;

  @Override
  public CompilationUnitNodeV1 parse(InputStream source, String sourceName) {
    try {
      String sourceCode = new String(source.readAllBytes(), StandardCharsets.UTF_8);
      return parseCompilationUnit(sourceCode, sourceName);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read source from InputStream", e);
    }
  }

  @Override
  public CompilationUnitNodeV1 parseCompilationUnit(String source, String repositoryPath) {
    CompilationUnitNode genericResult = genericService.parseCompilationUnit(source, repositoryPath);
    return conversionService.convert(genericResult, CompilationUnitNodeV1.class);
  }

  @Override
  public boolean canParse(InputStream source, String sourceName) {
    if (sourceName != null && sourceName.endsWith(".java")) {
      return true;
    }
    return false;
  }
}
