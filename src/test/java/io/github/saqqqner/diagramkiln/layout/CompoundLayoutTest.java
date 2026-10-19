package io.github.saqqqner.diagramkiln.layout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.internal.DiagramIndex;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompoundLayoutTest {
  @Test
  void appliesSpacingAndContainerPaddingToRealElk() {
    var data = Diagrams.diagram("r").group("g", "G",
        g -> g.node("a", "A").node("b", "B").node("c", "C").edge("a", "b", "").edge("a", "c", ""))
        .build();
    var options = LayoutOptions.builder().nodeSpacing(95).layerSpacing(150).groupPadding(45)
        .build();
    var result = engine.layout(data, options);
    verify(data, result);
    var nodes = map(result);
    var a = nodes.get("a");
    var b = nodes.get("b");
    var c = nodes.get("c");
    var group = nodes.get("g");
    assertTrue(b.x() - a.x() - a.width() >= 150 - 0.00001);
    assertTrue(Math.abs(b.y() - c.y()) - b.height() >= 95 - 0.00001);
    assertTrue(a.x() - group.x() >= 45 - 0.00001);
    assertTrue(Math.min(a.y(), Math.min(b.y(), c.y())) - group.y() >= 28 + 45 - 0.00001);
  }
  @ParameterizedTest
  @ValueSource(doubles =
  {0, 12, 15, 35, 100
  })
  void anchorsDenseConnectionsOnRoundedCorners(double arcSize) {
    var rounded = NodeStyle.builder().rounded(true).arcSize(arcSize).build();
    var builder = Diagrams.diagram("r").node("a", "A", rounded).node("b", "B", rounded);
    for (int i = 0; i < 12; i++)
      builder.edge("a", "b", "edge " + i);
    var result = engine.layout(builder.build(), new LayoutOptions(Direction.RIGHT, 160, 60));
    var source = map(result).get("a");
    boolean foundCorner = false;
    double radius = Math.min(30, 60 * arcSize / 100);
    for (var edge : result.edges()) {
      var start = edge.points().get(0);
      double y = Math.min(start.y() - source.y(), source.y() + 60 - start.y());
      if (y < radius) {
        foundCorner = true;
        double dx = start.x() - (source.x() + 160 - radius);
        double dy = radius - y;
        assertEquals(radius * radius, dx * dx + dy * dy, 0.00001);
      }
    }
    assertTrue(arcSize == 0 || foundCorner, "Fixture must exercise a rounded corner");
  }
  private final ElkLayout engine = new ElkLayout();

  @ParameterizedTest
  @EnumSource(Direction.class)
  void routesBetweenNestedNodesAndGroups(Direction direction) {
    var data = Diagrams.diagram("r")
        .group("g1", "Mining", g -> g.node("a", "A").node("b", "B").edge("a", "b", "local"))
        .group("g2", "Smelting", g -> g.node("c", "C").node("d", "D").edge("c", "d", "local"))
        .edge("b", "c", "cross").edge("g1", "g2", "groups").edge("b", "c", "parallel")
        .edge("a", "a", "loop").build();
    var result = engine.layout(data, new LayoutOptions(direction, 160, 60));
    verify(data, result);
    assertEquals(6, result.nodes().size());
    var nodes = map(result);
    var first = nodes.get("g1");
    var second = nodes.get("g2");
    assertTrue(first.x() + first.width() <= second.x() || second.x() + second.width() <= first.x()
        || first.y() + first.height() <= second.y() || second.y() + second.height() <= first.y());
  }

  @Test
  void supportsThreeLevelsEmptyGroupsAndHeaderPadding() {
    var data = Diagrams.diagram("r")
        .group("outer", "Outer", GroupStyle.builder().headerHeight(54).build(),
            g -> g.group("middle", "Middle", h -> h.group("inner", "Inner", j -> j.node("a", "A"))))
        .group("empty", "Empty", g -> {
        }).node("b", "B").edge("a", "b", "cross").build();
    var result = engine.layout(data, new LayoutOptions(Direction.RIGHT, 160, 60));
    verify(data, result);
    var nodes = map(result);
    assertTrue(nodes.get("middle").y() >= nodes.get("outer").y() + 54);
    assertTrue(nodes.get("empty").height() >= 28);
  }

  @Test
  void connectsContainersToTheirOwnDescendantsInBothDirections() {
    var data = Diagrams.diagram("r")
        .group("g", "G", g -> g.group("inner", "Inner", h -> h.node("a", "A")))
        .edge("g", "a", "down").edge("a", "g", "up").edge("g", "inner", "nested groups")
        .edge("g", "g", "group loop").build();
    verify(data, engine.layout(data, new LayoutOptions(Direction.RIGHT, 160, 60)));
  }

  @ParameterizedTest
  @EnumSource(NodeShape.class)
  void anchorsConnectionsOnVisibleShapeBoundary(NodeShape shape) {
    var style = NodeStyle.builder().shape(shape).build();
    var data = Diagrams.diagram("r").node("a", "A", style).node("b", "B", style)
        .node("c", "C", style).edge("a", "b", "one").edge("a", "c", "two").edge("b", "a", "back")
        .edge("a", "a", "loop").build();
    verify(data, engine.layout(data, new LayoutOptions(Direction.RIGHT, 160, 60)));
  }

  private static Map<String, PositionedNode> map(LayoutResult result) {
    return result.nodes().stream()
        .collect(Collectors.toMap(PositionedNode::id, Function.identity()));
  }

  private static void verify(DiagramData data, LayoutResult result) {
    var index = new DiagramIndex(data);
    var nodes = map(result);
    assertEquals(index.nodes().size(), nodes.size());
    assertEquals(index.edges().size(), result.edges().size());
    for (var node : result.nodes()) {
      assertTrue(Double.isFinite(node.x()) && Double.isFinite(node.y()));
      assertTrue(node.width() > 0 && node.height() > 0);
      String parentId = index.parentId(node.id());
      if (parentId != null) {
        var parent = nodes.get(parentId);
        double header = ((DiagramGroup) index.node(parentId)).groupStyle().headerHeight();
        assertTrue(node.x() >= parent.x() - 0.001);
        assertTrue(node.y() >= parent.y() + header - 0.001);
        assertTrue(node.x() + node.width() <= parent.x() + parent.width() + 0.001);
        assertTrue(node.y() + node.height() <= parent.y() + parent.height() + 0.001);
      }
    }
    for (var route : result.edges()) {
      assertTrue(route.points().size() >= 2);
      var edge = index.edges().get(route.edgeIndex());
      checkBoundary(route.points().get(0), nodes.get(edge.sourceId()),
          index.node(edge.sourceId()) instanceof DiagramGroup
              ? NodeShape.RECTANGLE
              : index.node(edge.sourceId()).style().shape());
      checkBoundary(route.points().get(route.points().size() - 1), nodes.get(edge.targetId()),
          index.node(edge.targetId()) instanceof DiagramGroup
              ? NodeShape.RECTANGLE
              : index.node(edge.targetId()).style().shape());
      route.points().forEach(p -> assertTrue(Double.isFinite(p.x()) && Double.isFinite(p.y())));
    }
  }

  private static void checkBoundary(Point p, PositionedNode node, NodeShape shape) {
    double x = Math.abs((p.x() - node.x() - node.width() / 2) / (node.width() / 2));
    double y = Math.abs((p.y() - node.y() - node.height() / 2) / (node.height() / 2));
    double boundary = switch (shape) {
      case RECTANGLE -> Math.max(x, y);
      case ELLIPSE -> x * x + y * y;
      case DIAMOND -> x + y;
    };
    assertEquals(1, boundary, 0.00001, "Endpoint is not on " + shape + " for " + node.id());
  }
}
