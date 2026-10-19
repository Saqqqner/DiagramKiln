package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.dto.NodeSize;

/**
 * A leaf node with explicit pixel dimensions. Groups cannot implement this capability. The bundled
 * ELK adapter preserves the dimensions exactly; custom engines control their returned bounds.
 * Implementations must not mutate while their input is captured.
 */
public interface SizedDiagramNode extends DiagramNode {
  /**
   * {@return the immutable width and height pair; never null}
   */
  NodeSize size();
}
