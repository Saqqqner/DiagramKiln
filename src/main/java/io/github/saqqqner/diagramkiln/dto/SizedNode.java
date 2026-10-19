package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.SizedDiagramNode;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/**
 * A leaf DTO with explicit dimensions; graph validation occurs at DSL build or export.
 *
 * @param id
 *          globally unique id
 * @param label
 *          non-null plain text
 * @param style
 *          immutable appearance
 * @param size
 *          non-null immutable pixel dimensions
 */
public record SizedNode(String id, String label, NodeStyle style,
    NodeSize size) implements SizedDiagramNode {
  /**
   * Rejects a missing size; ID, label and appearance are validated when capturing the diagram.
   *
   * @param id
   *          globally unique id
   * @param label
   *          non-null plain text
   * @param style
   *          immutable appearance
   * @param size
   *          non-null immutable pixel dimensions
   * @throws IllegalArgumentException
   *           if size is null
   */
  public SizedNode {
    if (size == null)
      throw new IllegalArgumentException("node " + id + " size must not be null");
  }

  /**
   * Uses the default node appearance.
   *
   * @param id
   *          globally unique id
   * @param label
   *          plain text label
   * @param size
   *          non-null immutable pixel dimensions
   */
  public SizedNode(String id, String label, NodeSize size) {
    this(id, label, NodeStyle.defaults(), size);
  }
}
