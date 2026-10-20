package io.github.saqqqner.diagramkiln.dto;

import java.util.List;

/**
 * A complete page-coordinate polyline, including source and target points.
 *
 * @param edgeIndex
 *          root edges first, then recursively nested edges in group order
 * @param points
 *          ordered source, bend and target points; at least two required at export
 */
public record RoutedEdge(int edgeIndex, List<Point> points) {
  /**
   * Copies the route point list; points are immutable pixel coordinates.
   *
   * @param edgeIndex
   *          root edges first, then recursively nested edges in group order
   * @param points
   *          ordered source, bend and target points; at least two required at export
   * @throws IllegalArgumentException
   *           if points is null or contains null
   */
  public RoutedEdge {
    if (points == null)
      throw new IllegalArgumentException("edge " + edgeIndex + " points must not be null");
    if (points.stream().anyMatch(point -> point == null))
      throw new IllegalArgumentException("edge " + edgeIndex + " points must not contain null");
    points = List.copyOf(points);
  }
}
