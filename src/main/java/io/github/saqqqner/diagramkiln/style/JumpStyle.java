package io.github.saqqqner.diagramkiln.style;

/**
 * How draw.io renders crossings with previously drawn connections.
 */
public enum JumpStyle {
  /**
   * Leave crossings overlapped.
   */
  NONE,
  /**
   * Draw a curved bridge.
   */
  ARC,
  /**
   * Leave a gap at the crossing.
   */
  GAP,
  /**
   * Draw an angular bridge.
   */
  SHARP
}
