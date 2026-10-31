package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedEdgeLabel;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.internal.validation.EdgeLabelGeometryValidator;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeLabelGeometryTest {
  private final DiagramData data = Diagrams.diagram("Geometry")
      .group("g", "Group",
          g -> g.node("a", "A").node("b", "B").edge("a", "b", "Local").edge("b", "a", ""))
      .node("c", "C").edge("b", "c", "Root").build();
  private final LayoutResult geometry = new LayoutResult(
      List.of(new PositionedNode("g", 100, 100, 500, 300),
          new PositionedNode("a", 140, 160, 100, 40), new PositionedNode("b", 400, 160, 100, 40),
          new PositionedNode("c", 750, 160, 100, 40)),
      List.of(new RoutedEdge(0, List.of(new Point(500, 180), new Point(750, 180))),
          new RoutedEdge(1, List.of(new Point(240, 180), new Point(400, 180))),
          new RoutedEdge(2, List.of(new Point(400, 190), new Point(240, 190)))));
  private final PositionedEdgeLabel root = new PositionedEdgeLabel(0, 610, 130, 100, 30);
  private final PositionedEdgeLabel local = new PositionedEdgeLabel(1, 260, 210, 120, 30);

  @Test
  void acceptsUnorderedCompleteLabelsAndAuthoritativeCustomBounds() {
    assertDoesNotThrow(() -> validate(List.of(local, root)));
    assertDoesNotThrow(() -> validate(List.of(root, new PositionedEdgeLabel(1, 260, 210, 80, 50))));
    assertDoesNotThrow(
        () -> validate(List.of(new PositionedEdgeLabel(0, -100, -100, 80, 30), local)));
  }

  @Test
  void rejectsMissingDuplicateUnknownAndEmptyEdgeLabels() {
    for (var labels : List.of(List.of(root), List.of(root, root, local),
        List.of(root, local, new PositionedEdgeLabel(3, 260, 210, 80, 20)),
        List.of(root, local, new PositionedEdgeLabel(-1, 260, 210, 80, 20)),
        List.of(root, local, new PositionedEdgeLabel(2, 260, 210, 80, 20))))
      assertThrows(IllegalArgumentException.class, () -> validate(labels));
    assertThrows(IllegalArgumentException.class,
        () -> EdgeLabelGeometryValidator.validate(data, null));
  }

  @Test
  void rejectsInvalidBoxesAndEndpointOverflowWithEdgeContext() {
    for (var bad : List.of(new PositionedEdgeLabel(1, Double.NaN, 210, 80, 20),
        new PositionedEdgeLabel(1, 260, Double.POSITIVE_INFINITY, 80, 20),
        new PositionedEdgeLabel(1, 260, 210, 0, 20), new PositionedEdgeLabel(1, 260, 210, 80, -1),
        new PositionedEdgeLabel(1, Double.MAX_VALUE, 210, Double.MAX_VALUE, 20),
        new PositionedEdgeLabel(1, 260, Double.MAX_VALUE, 80, Double.MAX_VALUE))) {
      var error = assertThrows(IllegalArgumentException.class, () -> validate(List.of(root, bad)));
      assertTrue(error.getMessage().contains("1"));
    }
  }

  @Test
  void rejectsHeaderAndOutsideContentButNotOwnRouteIntersection() {
    for (var bad : List.of(new PositionedEdgeLabel(1, 260, 105, 80, 20),
        new PositionedEdgeLabel(1, 590, 210, 80, 20)))
      assertThrows(IllegalArgumentException.class, () -> validate(List.of(root, bad)));
    assertDoesNotThrow(() -> validate(List.of(root, new PositionedEdgeLabel(1, 260, 170, 80, 20))));
  }

  @Test
  void ancestorDescendantUsesXmlOwnerRatherThanDeclarationOrInnerContainer() {
    var nested = Diagrams.diagram("Owner")
        .group("outer", "Outer", GroupStyle.builder().headerHeight(70).build(),
            outer -> outer.group("inner", "Inner", inner -> inner.node("a", "A")))
        .edge("inner", "a", "Ancestor").build();
    var positions = new LayoutResult(
        List.of(new PositionedNode("outer", 100, 100, 600, 400),
            new PositionedNode("inner", 150, 180, 250, 150),
            new PositionedNode("a", 200, 240, 100, 40)),
        List.of(new RoutedEdge(0, List.of(new Point(400, 255), new Point(300, 255)))));
    // Outside inner but inside the XML owner's content is valid.
    assertDoesNotThrow(
        () -> EdgeLabelGeometryValidator.validate(nested, new LabeledLayoutResult(positions,
            List.of(new PositionedEdgeLabel(0, 430, 220, 100, 40)))));
    for (var bad : List.of(new PositionedEdgeLabel(0, 430, 140, 100, 20),
        new PositionedEdgeLabel(0, 690, 220, 100, 40)))
      assertThrows(IllegalArgumentException.class, () -> EdgeLabelGeometryValidator.validate(nested,
          new LabeledLayoutResult(positions, List.of(bad))));
  }

  private void validate(List<PositionedEdgeLabel> labels) {
    EdgeLabelGeometryValidator.validate(data, new LabeledLayoutResult(geometry, labels));
  }
}
