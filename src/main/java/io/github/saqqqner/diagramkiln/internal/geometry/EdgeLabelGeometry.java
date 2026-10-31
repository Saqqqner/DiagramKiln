package io.github.saqqqner.diagramkiln.internal.geometry;

import io.github.saqqqner.diagramkiln.dto.Point;
import java.util.List;

/** Computes the endpoint anchor used by draw.io's absolute edge-label geometry. */
public final class EdgeLabelGeometry {
  private EdgeLabelGeometry() {
  }

  public static Point midpoint(List<Point> points) {
    var first = points.get(0);
    var last = points.get(points.size() - 1);
    // Match mxGraphView.updateEdgeLabelOffset, including its intermediate differences.
    double dx = last.x() - first.x();
    double dy = last.y() - first.y();
    if (!Double.isFinite(dx) || !Double.isFinite(dy))
      throw new IllegalArgumentException("Endpoint midpoint arithmetic is not finite");
    return new Point(first.x() + dx / 2, first.y() + dy / 2);
  }
}
