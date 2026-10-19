package io.github.saqqqner.diagramkiln.internal.geometry;

import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.DiagramNode;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.style.NodeShape;

/**
 * Shape geometry shared by layout, validation and serialization.
 */
public final class NodeGeometry {
  private NodeGeometry() {
  }

  public static Point attach(Point point, PositionedNode bounds, DiagramNode node) {
    NodeShape shape = shape(node);
    if (shape == NodeShape.RECTANGLE) {
      return rounded(node) ? roundedAttachment(point, bounds, radius(node, bounds)) : point;
    }
    double rx = bounds.width() / 2, ry = bounds.height() / 2;
    double cx = bounds.x() + rx, cy = bounds.y() + ry;
    double x = (point.x() - cx) / rx, y = (point.y() - cy) / ry;
    // Project along the orthogonal segment normal, retaining ELK's port distribution.
    if (Math.abs(x) >= Math.abs(y)) {
      double factor = shape == NodeShape.ELLIPSE
          ? Math.sqrt(Math.max(0, 1 - y * y))
          : Math.max(0, 1 - Math.abs(y));
      return new Point(cx + Math.copySign(rx * factor, x), point.y());
    }
    double factor = shape == NodeShape.ELLIPSE
        ? Math.sqrt(Math.max(0, 1 - x * x))
        : Math.max(0, 1 - Math.abs(x));
    return new Point(point.x(), cy + Math.copySign(ry * factor, y));
  }

  public static boolean onBoundary(Point point, PositionedNode bounds, DiagramNode node) {
    if (rounded(node)) {
      double r = radius(node, bounds);
      double dx = Math.abs(point.x() - bounds.x() - bounds.width() / 2) - bounds.width() / 2 + r;
      double dy = Math.abs(point.y() - bounds.y() - bounds.height() / 2) - bounds.height() / 2 + r;
      double distance = Math.hypot(Math.max(dx, 0), Math.max(dy, 0)) + Math.min(Math.max(dx, dy), 0)
          - r;
      return Math.abs(distance) <= 0.00001;
    }
    double x = Math.abs((point.x() - bounds.x() - bounds.width() / 2) / (bounds.width() / 2));
    double y = Math.abs((point.y() - bounds.y() - bounds.height() / 2) / (bounds.height() / 2));
    double value = switch (shape(node)) {
      case RECTANGLE -> Math.max(x, y);
      case ELLIPSE -> x * x + y * y;
      case DIAMOND -> x + y;
    };
    return Math.abs(value - 1) <= 0.00001;
  }

  private static NodeShape shape(DiagramNode node) {
    return node instanceof DiagramGroup ? NodeShape.RECTANGLE : node.style().shape();
  }

  private static boolean rounded(DiagramNode node) {
    return !(node instanceof DiagramGroup) && node.style().shape() == NodeShape.RECTANGLE
        && node.style().rounded();
  }

  private static double radius(DiagramNode node, PositionedNode bounds) {
    return Math.min(bounds.width(), bounds.height()) * Math.min(0.5, node.style().arcSize() / 100);
  }

  private static Point roundedAttachment(Point point, PositionedNode bounds, double r) {
    double x = point.x() - bounds.x(), y = point.y() - bounds.y();
    if (Math.min(x, bounds.width() - x) < 0.00001) {
      double fromEnd = Math.min(y, bounds.height() - y);
      if (fromEnd < r) {
        double inset = r - Math.sqrt(Math.max(0, r * r - (r - fromEnd) * (r - fromEnd)));
        x = x < bounds.width() / 2 ? inset : bounds.width() - inset;
      }
    }
    else {
      double fromEnd = Math.min(x, bounds.width() - x);
      if (fromEnd < r) {
        double inset = r - Math.sqrt(Math.max(0, r * r - (r - fromEnd) * (r - fromEnd)));
        y = y < bounds.height() / 2 ? inset : bounds.height() - inset;
      }
    }
    return new Point(bounds.x() + x, bounds.y() + y);
  }
}
