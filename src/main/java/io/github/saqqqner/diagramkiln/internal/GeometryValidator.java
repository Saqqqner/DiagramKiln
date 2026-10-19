package io.github.saqqqner.diagramkiln.internal;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Checks the layout SPI boundary before constructing a document.
 */
final class GeometryValidator {
  private static final double EPSILON = 0.00001;

  private GeometryValidator() {
  }

  static void validate(DiagramData data, LayoutResult layout) {
    var index = new DiagramIndex(data);
    require(layout != null, "Layout engine returned null");
    require(layout.nodes().size() == index.nodes().size(),
        "Layout node count does not match diagram");
    require(layout.edges().size() == index.edges().size(),
        "Layout edge count does not match diagram");
    var expectedIds = new HashSet<String>();
    index.nodes().forEach(n -> expectedIds.add(n.id()));
    var nodes = new HashMap<String, PositionedNode>();
    for (var node : layout.nodes()) {
      require(expectedIds.contains(node.id()), "Layout contains unknown node: " + node.id());
      require(nodes.putIfAbsent(node.id(), node) == null, "Duplicate layout node: " + node.id());
      require(
          finite(node.x(), node.y(), node.width(), node.height(), node.x() + node.width(),
              node.y() + node.height()) && node.width() > 0 && node.height() > 0,
          "Invalid geometry for node: " + node.id());
    }
    for (var node : layout.nodes()) {
      if (index.node(node.id()) instanceof DiagramGroup group) {
        require(node.height() >= group.groupStyle().headerHeight(),
            "Group height is smaller than its header: " + node.id());
      }
      String parentId = index.parentId(node.id());
      if (parentId != null) {
        var parent = nodes.get(parentId);
        var group = (DiagramGroup) index.node(parentId);
        require(
            node.x() >= parent.x() - EPSILON
                && node.y() >= parent.y() + group.groupStyle().headerHeight() - EPSILON
                && node.x() + node.width() <= parent.x() + parent.width() + EPSILON
                && node.y() + node.height() <= parent.y() + parent.height() + EPSILON,
            "Child is outside its parent content bounds: " + node.id());
      }
    }
    var indices = new HashSet<Integer>();
    for (var route : layout.edges()) {
      int routeIndex = route.edgeIndex();
      require(routeIndex >= 0 && routeIndex < index.edges().size(),
          "Unknown edge index: " + routeIndex);
      require(indices.add(routeIndex), "Duplicate route for edge: " + routeIndex);
      require(route.points().size() >= 2, "Edge route must have start and end: " + routeIndex);
      for (var point : route.points()) {
        require(finite(point.x(), point.y()), "Non-finite route point for edge: " + routeIndex);
      }
      var edge = index.edges().get(routeIndex);
      require(
          NodeGeometry.onBoundary(route.points().get(0), nodes.get(edge.sourceId()),
              index.node(edge.sourceId())),
          "Route start must be on source boundary for edge: " + routeIndex);
      require(
          NodeGeometry.onBoundary(route.points().get(route.points().size() - 1),
              nodes.get(edge.targetId()), index.node(edge.targetId())),
          "Route end must be on target boundary for edge: " + routeIndex);
    }
  }

  private static boolean finite(double... values) {
    for (double value : values)
      if (!Double.isFinite(value))
        return false;
    return true;
  }

  private static void require(boolean condition, String message) {
    if (!condition)
      throw new IllegalArgumentException(message);
  }
}
