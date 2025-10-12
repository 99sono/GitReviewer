package com.sono99.javaparser.impl.generic.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sono99.javaparser.impl.generic.utils.SourceContentUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for JavaParserService using real Spring context. Tests the service as it would
 * be used in production with proper dependency injection. Validates that Spring can correctly wire
 * and configure the parser service.
 *
 * <p>Extends AbstractParseAnimalTest to inherit all core test logic while providing Spring-specific
 * testing capabilities and configuration validation.
 */
@SpringBootTest
@ContextConfiguration(classes = {JavaParserService.class, SourceContentUtils.class})
class JavaParserServiceParseAnimalIntegrationTest extends AbstractParseAnimalTest {

  @Autowired private JavaParserService parserService;

  /**
   * Creates a JavaParserService instance using Spring dependency injection. Returns the
   * Spring-injected service for integration testing.
   *
   * @return Spring-managed JavaParserService instance
   */
  @Override
  protected JavaParserService createParserService() {
    return parserService;
  }

  /**
   * Test that verifies Spring properly injected the JavaParserService. Ensures that the @Autowired
   * annotation works correctly and the service is properly configured by Spring's dependency
   * injection container.
   */
  @Test
  void shouldVerifySpringInjectionWorks() {
    // Verify that Spring properly injected the service
    assertThat(parserService).isNotNull();
  }

  /**
   * Test that validates the SourceContentUtils dependency is correctly injected by Spring. Uses
   * reflection to access the private field and verify Spring's dependency injection worked
   * correctly for the service's internal dependencies.
   */
  @Test
  void shouldVerifySourceContentUtilsInjectedIntoService() {
    // This test verifies that the SourceContentUtils dependency is correctly
    // injected into JavaParserService by Spring.
    // Since SourceContentUtils is a private field in JavaParserService,
    // we'll use reflection to access it and assert its non-null status.
    try {
      java.lang.reflect.Field sourceUtilsField =
          JavaParserService.class.getDeclaredField("sourceUtils");
      sourceUtilsField.setAccessible(true);
      SourceContentUtils sourceContentUtils =
          (SourceContentUtils) sourceUtilsField.get(parserService);
      assertThat(sourceContentUtils).isNotNull();
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to access sourceUtils field via reflection", e);
    }
  }

  // remaining tests found in the parent class

}
