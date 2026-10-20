package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.LayoutDiagramNode;
import io.github.saqqqner.diagramkiln.SizedDiagramNode;
import io.github.saqqqner.diagramkiln.config.NodeLayout;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/**
 * Immutable leaf carrying explicit dimensions and layout metadata; graph validation occurs at
 * capture.
 *
 * @param id
 *          globally unique identifier
 * @param label
 *          plain text label
 * @param style
 *          immutable node appearance
 * @param size
 *          non-null explicit pixel dimensions
 * @param layout
 *          non-null immutable layout metadata
 */
public record SizedLayoutNode(String id, String label, NodeStyle style, NodeSize size,
    NodeLayout layout) implements LayoutDiagramNode, SizedDiagramNode {
  /**
   * Rejects missing metadata; other graph properties are validated when captured.
   *
   * @param id
   *          globally unique identifier
   * @param label
   *          plain text label
   * @param style
   *          immutable node appearance
   * @param size
   *          non-null explicit pixel dimensions
   * @param layout
   *          non-null immutable layout metadata
   */
  public SizedLayoutNode {
    if (layout == null || size == null)
      throw new IllegalArgumentException("node " + id + " layout and size must not be null");
  }
}
