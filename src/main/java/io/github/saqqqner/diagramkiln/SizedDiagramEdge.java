package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;

/**
 * Optional input capability for a caller-sized edge label. Capture preserves this metadata;
 * placement uses it only when edge-label layout is enabled.
 */
public interface SizedDiagramEdge extends DiagramEdge {
  /**
   * Returns immutable reserved dimensions, read once per capture of this object.
   *
   * @return non-null label dimensions in pixels; custom engines control their final geometry
   */
  EdgeLabelSize labelSize();
}
