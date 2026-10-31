package io.github.saqqqner.diagramkiln.internal.geometry;

import io.github.saqqqner.diagramkiln.dto.Point;
import java.util.List;

/** Computes finite label anchors from validated final routes. */
public final class EdgeLabelGeometry {
  private EdgeLabelGeometry() {
  }

  public static Point midpoint(List<Point> points) {
    var lengths = new double[points.size() - 1];
    double total = 0;
    for (int i = 0; i < lengths.length; i++) {
      var first = points.get(i);
      var second = points.get(i + 1);
      double dx = second.x() - first.x();
      double dy = second.y() - first.y();
      requireFinite(dx, dy);
      double length = Math.hypot(dx, dy);
      total += length;
      requireFinite(length, total);
      lengths[i] = length;
    }
    if (total == 0)
      return points.get(0);
    double remaining = total / 2;
    for (int i = 0; i < lengths.length; i++) {
      double length = lengths[i];
      if (length == 0)
        continue;
      if (remaining <= length) {
        var first = points.get(i);
        var second = points.get(i + 1);
        double fraction = remaining / length;
        double x = first.x() + (second.x() - first.x()) * fraction;
        double y = first.y() + (second.y() - first.y()) * fraction;
        requireFinite(x, y);
        return new Point(x, y);
      }
      remaining -= length;
    }
    return points.get(points.size() - 1);
  }

  private static void requireFinite(double... values) {
    for (double value : values)
      if (!Double.isFinite(value))
        throw new IllegalArgumentException("Route midpoint arithmetic is not finite");
  }
}
