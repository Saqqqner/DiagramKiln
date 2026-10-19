package io.github.saqqqner.diagramkiln.layout;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;

/**
 * Layout SPI. Returned points use page coordinates, including each edge's start and end.
 */
@FunctionalInterface
public interface LayoutEngine {
  /**
   * Computes bounds and routes for every node, group and edge.
   *
   * @param diagram
   *          validated immutable diagram tree
   * @param options
   *          validated direction and leaf dimensions
   * @return page-coordinate bounds and routes; edge order is root edges followed recursively by
   *         group edges in group order; each route includes its start, bends and end
   * @throws IllegalArgumentException
   *           if the supplied input cannot be laid out
   */
  LayoutResult layout(DiagramData diagram, LayoutOptions options);
}
