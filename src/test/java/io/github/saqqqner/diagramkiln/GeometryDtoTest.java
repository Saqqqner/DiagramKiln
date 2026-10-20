package io.github.saqqqner.diagramkiln;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometryDtoTest {
  @Test
  void rejectsMissingLayoutListsWithFieldMessages() {
    assertEquals("layout nodes must not be null",
        assertThrows(IllegalArgumentException.class, () -> new LayoutResult(null, List.of()))
            .getMessage());
    assertEquals("layout edges must not be null",
        assertThrows(IllegalArgumentException.class, () -> new LayoutResult(List.of(), null))
            .getMessage());
  }

  @Test
  void rejectsNullLayoutElementsWithFieldMessages() {
    assertEquals("layout nodes must not contain null", assertThrows(IllegalArgumentException.class,
        () -> new LayoutResult(Arrays.asList((PositionedNode) null), List.of())).getMessage());
    assertEquals("layout edges must not contain null", assertThrows(IllegalArgumentException.class,
        () -> new LayoutResult(List.of(), Arrays.asList((RoutedEdge) null))).getMessage());
  }

  @Test
  void rejectsMissingRoutePointsWithEdgeContext() {
    assertEquals("edge 7 points must not be null",
        assertThrows(IllegalArgumentException.class, () -> new RoutedEdge(7, null)).getMessage());
    assertEquals("edge 7 points must not contain null", assertThrows(IllegalArgumentException.class,
        () -> new RoutedEdge(7, Arrays.asList((Point) null))).getMessage());
  }

  @Test
  void propagatesGeometryValidationFromCustomEngine() {
    var exporter = DrawioExporter.builder()
        .layout((data, options) -> new LayoutResult(null, List.of())).build();
    assertEquals("layout nodes must not be null", assertThrows(IllegalArgumentException.class,
        () -> exporter.toXml(Diagrams.diagram("Empty").build())).getMessage());
  }

  @Test
  void copiesGeometryListsAndKeepsEmptyResultsValid() {
    var node = new PositionedNode("a", 0, 0, 160, 60);
    var point = new Point(0, 0);
    var points = new ArrayList<>(List.of(point));
    var route = new RoutedEdge(0, points);
    var nodes = new ArrayList<>(List.of(node));
    var edges = new ArrayList<>(List.of(route));
    var result = new LayoutResult(nodes, edges);
    points.clear();
    nodes.clear();
    edges.clear();
    assertEquals(List.of(point), route.points());
    assertEquals(List.of(node), result.nodes());
    assertEquals(List.of(route), result.edges());
    assertThrows(UnsupportedOperationException.class, () -> result.nodes().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.edges().clear());
    assertThrows(UnsupportedOperationException.class, () -> route.points().clear());
    assertEquals(List.of(), new LayoutResult(List.of(), List.of()).nodes());
  }
}
