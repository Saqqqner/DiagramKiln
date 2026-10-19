package io.github.saqqqner.diagramkiln.style;

/**
 * Visible boundary used when attaching connections.
 */
public enum NodeShape {
  /**
   * A rectangle, optionally with rounded corners.
   */
  RECTANGLE,
  /**
   * An ellipse filling the node bounds.
   */
  ELLIPSE,
  /**
   * A diamond with vertices at the centers of the bounding rectangle's sides.
   */
  DIAMOND
}
