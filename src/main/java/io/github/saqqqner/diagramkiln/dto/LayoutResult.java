package io.github.saqqqner.diagramkiln.dto;

import java.util.List;

/**
 * Immutable lists of page-coordinate geometry returned by a layout engine.
 *
 * @param nodes
 *          all node and group bounds
 * @param edges
 *          routes identified by traversal edge index
 */
public record LayoutResult(List<PositionedNode> nodes, List<RoutedEdge> edges) {
  /**
   * Copies non-null geometry lists without null elements; full coverage and geometry are checked by
   * the exporter.
   *
   * @param nodes
   *          all node and group bounds
   * @param edges
   *          routes identified by traversal edge index
   * @throws IllegalArgumentException
   *           if either list is null or contains null
   */
  public LayoutResult {
    if (nodes == null)
      throw new IllegalArgumentException("layout nodes must not be null");
    if (edges == null)
      throw new IllegalArgumentException("layout edges must not be null");
    if (nodes.stream().anyMatch(node -> node == null))
      throw new IllegalArgumentException("layout nodes must not contain null");
    if (edges.stream().anyMatch(edge -> edge == null))
      throw new IllegalArgumentException("layout edges must not contain null");
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
  }
}
