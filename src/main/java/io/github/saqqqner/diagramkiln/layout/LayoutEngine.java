package io.github.saqqqner.diagramkiln.layout;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;

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

  /**
   * Computes label-aware geometry when explicitly enabled by the caller. Existing engines remain
   * functional interfaces and fail explicitly unless they override this operation. The default
   * implementation does not call the legacy layout method.
   *
   * @param diagram
   *          validated immutable diagram tree
   * @param options
   *          validated layout settings
   * @param fallbackSize
   *          non-null reserved size for nonempty labels without explicit dimensions
   * @return page-coordinate geometry and one box per nonempty label, keyed by traversal index
   * @throws UnsupportedOperationException
   *           if the engine does not support label placement
   * @throws IllegalArgumentException
   *           if the supplied input cannot be laid out
   */
  default LabeledLayoutResult layoutWithEdgeLabels(DiagramData diagram, LayoutOptions options,
      EdgeLabelSize fallbackSize) {
    throw new UnsupportedOperationException("Layout engine does not support edge-label placement");
  }
}
