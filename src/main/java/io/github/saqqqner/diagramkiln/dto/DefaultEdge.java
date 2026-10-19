package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.DiagramEdge;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;

/**
 * A directed connection DTO; endpoints resolve when the complete tree is validated.
 *
 * @param sourceId
 *          global source id
 * @param targetId
 *          global target id
 * @param label
 *          non-null plain text
 * @param style
 *          immutable appearance
 */
public record DefaultEdge(String sourceId, String targetId, String label,
    EdgeStyle style) implements DiagramEdge {
  /**
   * Uses the default connection appearance.
   *
   * @param sourceId
   *          global source id
   * @param targetId
   *          global target id
   * @param label
   *          plain text label
   */
  public DefaultEdge(String sourceId, String targetId, String label) {
    this(sourceId, targetId, label, EdgeStyle.defaults());
  }
}
