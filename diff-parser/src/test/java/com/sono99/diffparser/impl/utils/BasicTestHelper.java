package com.sono99.diffparser.impl.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BasicTestHelper {

  /**
   * Helper method to read the content of a test resource file as a String.
   *
   * @param resourceFileName The name of the resource file (e.g., "git_diff_02.diff").
   * @return The content of the resource file as a String.
   * @throws IOException If an error occurs while reading the file.
   */
  public static String readTestResourceToString(String resourceFileName) throws IOException {
    Path resourcePath = Path.of("src/test/resources/diff-examples/" + resourceFileName);
    return Files.readString(resourcePath);
  }
}
