package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.LayoutDiagramNode;
import io.github.saqqqner.diagramkiln.config.NodeLayout;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/**
 * Immutable leaf carrying layout metadata; graph validation occurs at capture.
 *
 * @param id
 *          globally unique identifier
 * @param label
 *          plain text label
 * @param style
 *          immutable node appearance
 * @param layout
 *          non-null immutable layout metadata
 */
public record LayoutNode(String id, String label, NodeStyle style,
    NodeLayout layout) implements LayoutDiagramNode {
  /**
   * Rejects missing metadata; other graph properties are validated when captured.
   *
   * @param id
   *          globally unique identifier
   * @param label
   *          plain text label
   * @param style
   *          immutable node appearance
   * @param layout
   *          non-null immutable layout metadata
   */
  public LayoutNode {
    if (layout == null)
      throw new IllegalArgumentException("node " + id + " layout must not be null");
  }
}
