package com.sono99.javaparser.api.v1.service;

import com.sono99.javaparser.api.v1.model.CompilationUnitNodeV1;
import java.io.InputStream;

/**
 * Public interface for the Java parsing service, providing the external API for parsing Java source
 * code into enriched AST representations. This interface hides internal implementation details and
 * allows for extensibility through alternative backends. All implementations must handle errors
 * gracefully and provide comprehensive metadata.
 */
public interface JavaParsingServiceV1 {

  /**
   * Parses Java source code from an InputStream and returns the enriched content representation.
   *
   * @param source the InputStream containing the Java source code
   * @param sourceName the logical name of the source (e.g., file name or PR path for logging)
   * @return CompilationUnitNodeV1 containing the parsed AST, nodes, and metadata
   * @throws RuntimeException if parsing fails due to syntax errors or IO issues
   */
  CompilationUnitNodeV1 parse(InputStream source, String sourceName);

  /**
   * Parses Java source code from a String and returns the enriched content representation.
   *
   * @param source the String containing the Java source code
   * @param repositoryPath the path to the source file in the repository
   * @return CompilationUnitNodeV1 containing the parsed AST, nodes, and metadata
   * @throws RuntimeException if parsing fails due to syntax errors
   */
  CompilationUnitNodeV1 parseCompilationUnit(String source, String repositoryPath);

  /**
   * Determines if the given InputStream can be parsed as Java source code. Performs basic
   * validation like extension check if sourceName provided, or header inspection.
   *
   * @param source the InputStream to check
   * @param sourceName the logical name for extension check (optional)
   * @return true if the content is likely valid Java source, false otherwise
   */
  boolean canParse(InputStream source, String sourceName);
}
