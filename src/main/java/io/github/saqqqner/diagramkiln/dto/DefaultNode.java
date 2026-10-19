package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.DiagramNode;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/**
 * A leaf DTO; graph validation occurs at DSL build or export.
 *
 * @param id
 *          globally unique id
 * @param label
 *          non-null plain text
 * @param style
 *          immutable appearance
 */
public record DefaultNode(String id, String label, NodeStyle style) implements DiagramNode {
  /**
   * Uses the default node appearance.
   *
   * @param id
   *          globally unique id
   * @param label
   *          plain text label
   */
  public DefaultNode(String id, String label) {
    this(id, label, NodeStyle.defaults());
  }
}
