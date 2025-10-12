package com.sono99.javaparser.impl.generic.service;

import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;

/**
 * Fast unit tests for JavaParserService using manual instantiation and dependency injection. Tests
 * the service behavior in isolation without Spring context overhead for rapid development and
 * debugging. Extends AbstractParseAnimalTest to share test logic while providing fast unit-level
 * testing capabilities.
 *
 * <p>Uses manual service instantiation and setter injection for maximum speed and control during
 * development and debugging scenarios.
 */
class JavaParserServiceParseAnimalTest extends AbstractParseAnimalTest {

  /**
   * Creates a JavaParserService instance using manual instantiation for fast unit testing. Manually
   * injects SourceContentUtils dependency to avoid Spring context overhead.
   *
   * @return manually configured JavaParserService for unit testing
   */
  @Override
  protected JavaParserService createParserService() {
    JavaParserService parserService = new JavaParserService();
    SourceContentUtils utils = new SourceContentUtils();
    parserService.setSourceUtils(utils);
    return parserService;
  }

  // Tests are found in the parent class

}
