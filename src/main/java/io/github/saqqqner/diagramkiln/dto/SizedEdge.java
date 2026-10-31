package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.SizedDiagramEdge;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;

/**
 * Edge DTO with explicit reserved label dimensions; graph validation occurs at capture.
 *
 * @param sourceId
 *          globally unique source id
 * @param targetId
 *          globally unique target id
 * @param label
 *          non-null plain text
 * @param style
 *          immutable appearance
 * @param labelSize
 *          non-null reserved pixel dimensions
 */
public record SizedEdge(String sourceId, String targetId, String label, EdgeStyle style,
    EdgeLabelSize labelSize) implements SizedDiagramEdge {
  /**
   * Rejects a missing size; IDs, text and style are validated when capturing the diagram.
   *
   * @param sourceId
   *          globally unique source id
   * @param targetId
   *          globally unique target id
   * @param label
   *          non-null plain text
   * @param style
   *          immutable appearance
   * @param labelSize
   *          non-null reserved pixel dimensions
   * @throws IllegalArgumentException
   *           if labelSize is null
   */
  public SizedEdge {
    if (labelSize == null)
      throw new IllegalArgumentException(
          "edge " + sourceId + " -> " + targetId + " label size must not be null");
  }
}
