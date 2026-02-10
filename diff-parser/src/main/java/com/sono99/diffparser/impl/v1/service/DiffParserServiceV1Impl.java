package com.sono99.diffparser.impl.v1.service;

import com.sono99.diffparser.api.v1.model.ParsedDiffV1;
import com.sono99.diffparser.api.v1.service.DiffParserServiceV1;
import com.sono99.diffparser.impl.generic.model.ParsedDiff;
import com.sono99.diffparser.impl.generic.service.DiffParserService;
import com.sono99.diffparser.impl.v1.converter.DiffConverterV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the v1 DiffParserService API. Converts generic diff parsing results to
 * sanitized v1 DTOs.
 */
@Service
public class DiffParserServiceV1Impl implements DiffParserServiceV1 {

  @Autowired private DiffParserService genericService;
  @Autowired private DiffConverterV1 converter;

  @Override
  public ParsedDiffV1 parseUnifiedDiff(String diffContent) {
    ParsedDiff genericResult = genericService.parseUnifiedDiff(diffContent);
    return converter.convertParsedDiff(genericResult);
  }
}
