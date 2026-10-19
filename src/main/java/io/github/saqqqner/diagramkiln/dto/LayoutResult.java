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
   * Copies the geometry lists; full coverage and geometry are checked by the exporter.
   *
   * @param nodes
   *          all node and group bounds
   * @param edges
   *          routes identified by traversal edge index
   */
  public LayoutResult {
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
  }
}
