package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramEdge;
import io.github.saqqqner.diagramkiln.DiagramNode;

import java.util.List;

/**
 * Copied input lists. Arbitrary nested DTO values are snapshotted by the DSL or exporter.
 *
 * @param sectionName
 *          page or group title
 * @param nodes
 *          direct children, with no null elements
 * @param edges
 *          directly declared connections, with no null elements
 */
public record DefaultDiagram(String sectionName, List<DiagramNode> nodes,
    List<DiagramEdge> edges) implements DiagramData {
  /**
   * Copies input lists and rejects null lists or elements.
   *
   * @param sectionName
   *          page or group title
   * @param nodes
   *          direct children, with no null elements
   * @param edges
   *          directly declared connections, with no null elements
   */
  public DefaultDiagram {
    if (nodes == null || edges == null) {
      throw new IllegalArgumentException("nodes and edges must not be null");
    }
    if (nodes.stream().anyMatch(n -> n == null) || edges.stream().anyMatch(e -> e == null)) {
      throw new IllegalArgumentException("nodes and edges must not contain null");
    }
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
  }
}
