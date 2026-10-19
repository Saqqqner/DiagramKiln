package io.github.saqqqner.diagramkiln.layout;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ElkLayoutTest {
  private final ElkLayout engine = new ElkLayout();

  @ParameterizedTest
  @EnumSource(Direction.class)
  void laysOutChainInRequestedDirection(Direction direction) {
    var data = Diagrams.diagram("Chain").node("a", "A").node("b", "B").node("c", "C")
        .edge("a", "b", "").edge("b", "c", "").build();
    var layout = engine.layout(data, new LayoutOptions(direction, 160, 60));
    PositionedNode a = layout.nodes().get(0), b = layout.nodes().get(1), c = layout.nodes().get(2);
    switch (direction) {
      case RIGHT -> {
        assertTrue(a.x() + a.width() < b.x());
        assertTrue(b.x() + b.width() < c.x());
      }
      case LEFT -> {
        assertTrue(c.x() + c.width() < b.x());
        assertTrue(b.x() + b.width() < a.x());
      }
      case DOWN -> {
        assertTrue(a.y() + a.height() < b.y());
        assertTrue(b.y() + b.height() < c.y());
      }
      case UP -> {
        assertTrue(c.y() + c.height() < b.y());
        assertTrue(b.y() + b.height() < a.y());
      }
    }
    assertEquals(2, layout.edges().size());
    assertRouted(layout, data);
  }

  @Test
  void branchingNodesDoNotOverlapAndRespectCustomSizes() {
    var data = Diagrams.diagram("Branch").node("a", "A").node("b", "B").node("c", "C")
        .edge("a", "b", "one").edge("a", "c", "two").build();
    var layout = engine.layout(data, new LayoutOptions(Direction.RIGHT, 220, 80));
    for (var node : layout.nodes()) {
      assertEquals(220, node.width());
      assertEquals(80, node.height());
      for (var other : layout.nodes()) {
        if (!node.id().equals(other.id())) {
          assertTrue(node.x() + node.width() <= other.x() || other.x() + other.width() <= node.x()
              || node.y() + node.height() <= other.y() || other.y() + other.height() <= node.y());
        }
      }
    }
    assertRouted(layout, data);
  }

  @Test
  void routesCyclesSelfLoopsAndParallelEdgesIndependently() {
    var data = Diagrams.diagram("Cycle").node("a", "A").node("b", "B").edge("a", "b", "first")
        .edge("a", "b", "second").edge("b", "a", "back").edge("a", "a", "loop").build();
    var result = engine.layout(data, new LayoutOptions(Direction.RIGHT, 160, 60));
    assertRouted(result, data);
    assertNotEquals(result.edges().get(0).points(), result.edges().get(1).points());
    assertTrue(result.edges().get(3).points().size() >= 4);
  }

  @Test
  void acceptsEmptyAndDisconnectedGraphs() {
    var empty = engine.layout(Diagrams.diagram("Empty").build(),
        new LayoutOptions(Direction.RIGHT, 160, 60));
    assertEquals(List.of(), empty.nodes());
    assertEquals(List.of(), empty.edges());
    var result = engine.layout(Diagrams.diagram("Islands").node("a", "A").node("b", "B").build(),
        new LayoutOptions(Direction.RIGHT, 160, 60));
    assertEquals(2, result.nodes().size());
    assertNotEquals(new Point(result.nodes().get(0).x(), result.nodes().get(0).y()),
        new Point(result.nodes().get(1).x(), result.nodes().get(1).y()));
  }

  private static void assertRouted(LayoutResult result, DiagramData data) {
    assertEquals(data.nodes().size(), result.nodes().size());
    assertEquals(data.edges().size(), result.edges().size());
    for (int i = 0; i < data.edges().size(); i++) {
      var edge = result.edges().get(i);
      assertEquals(i, edge.edgeIndex());
      assertTrue(edge.points().size() >= 2);
      edge.points().forEach(p -> {
        assertTrue(Double.isFinite(p.x()));
        assertTrue(Double.isFinite(p.y()));
      });
      var input = data.edges().get(i);
      var source = result.nodes().stream().filter(n -> n.id().equals(input.sourceId())).findFirst()
          .orElseThrow();
      var target = result.nodes().stream().filter(n -> n.id().equals(input.targetId())).findFirst()
          .orElseThrow();
      assertOnBoundary(edge.points().get(0), source);
      assertOnBoundary(edge.points().get(edge.points().size() - 1), target);
    }
  }

  private static void assertOnBoundary(Point point, PositionedNode node) {
    double x = point.x() - node.x(), y = point.y() - node.y(), eps = 0.001;
    assertTrue(x >= -eps && x <= node.width() + eps && y >= -eps && y <= node.height() + eps);
    assertTrue(Math.abs(x) < eps || Math.abs(x - node.width()) < eps || Math.abs(y) < eps
        || Math.abs(y - node.height()) < eps);
  }
}
