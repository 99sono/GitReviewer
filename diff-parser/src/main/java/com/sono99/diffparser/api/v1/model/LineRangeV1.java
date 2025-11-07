package com.sono99.diffparser.api.v1.model;

/**
 * Public API DTO representing a range of modified lines in a diff. Sanitized version of the
 * internal LineRange model.
 *
 * @param startLine the starting line number of the range
 * @param endLine the ending line number of the range
 * @param changeType the type of change (INSERT, DELEATE, CHANGE, EQUAL)
 */
public record LineRangeV1(int startLine, int endLine, ChangeType changeType) {
  public enum ChangeType {
    INSERT,
    DELETE,
    CHANGE,
    EQUAL
  }

  /**
   * Checks if this line range intersects with another range.
   *
   * @param other the other line range
   * @return true if they intersect
   */
  public boolean intersects(LineRangeV1 other) {
    return this.startLine <= other.endLine && other.startLine <= this.endLine;
  }

  /**
   * Checks if a line number is within this range.
   *
   * @param lineNumber the line number to check
   * @return true if within range
   */
  public boolean contains(int lineNumber) {
    return lineNumber >= startLine && lineNumber <= endLine;
  }
}
