package com.sono99.diffparser.impl.generic.model;

/**
 * Represents a range of modified lines in a diff. Used for intersecting with AST nodes for targeted
 * analysis.
 *
 * @param startLine the starting line number of the range
 * @param endLine the ending line number of the range
 * @param changeType the type of change (ADDED, REMOVED, MODIFIED, CONTEXT)
 */
public record LineRange(int startLine, int endLine, ChangeType changeType) {
  /** See {@link com.github.difflib.patch.DeltaType}. */
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
  public boolean intersects(LineRange other) {
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
