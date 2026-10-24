package io.github.saqqqner.diagramkiln.layout;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.internal.validation.EdgeLabelGeometryValidator;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeLabelLayoutTest {
  private final ElkLayout engine = new ElkLayout();
  private final EdgeLabelSize fallback = new EdgeLabelSize(140, 30);

  @Test
  void reservesExplicitAndFallbackSizesWithoutDroppingRepeatedOrWhitespaceText() {
    var data = Diagrams.diagram("Sizing").node("a", "A").node("b", "B")
        .edge("a", "b", "Repeated", EdgeStyle.defaults(), new EdgeLabelSize(360, 70))
        .edge("a", "b", "Repeated").edge("a", "b", " ").edge("a", "b", "").build();
    var result = engine.layoutWithEdgeLabels(data, LayoutOptions.builder().build(), fallback);
    EdgeLabelGeometryValidator.validate(data, result);
    assertEquals(3, result.labels().size());
    var explicit = result.labels().stream().filter(l -> l.edgeIndex() == 0).findFirst()
        .orElseThrow();
    assertEquals(360, explicit.width());
    assertEquals(70, explicit.height());
    for (int index : new int[]
    {1, 2
    }) {
      var label = result.labels().stream().filter(l -> l.edgeIndex() == index).findFirst()
          .orElseThrow();
      assertEquals(140, label.width());
      assertEquals(30, label.height());
      assertNotEquals(explicit.y(), label.y(), "Parallel labels must retain independent placement");
    }
    assertTrue(node(result, "b").x() - node(result, "a").x() - 160 >= 360);
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void preservesNestedOwnershipRoutesAndShapeBoundaries(Direction direction) {
    var data = Diagrams.diagram("Compound")
        .group("g1", "First",
            g -> g.node("a", "A", NodeStyle.builder().rounded(true).build())
                .node("b", "B", NodeStyle.builder().shape(NodeShape.DIAMOND).build(), 180, 100)
                .edge("a", "b", "A long local label\n第二行"))
        .group("g2", "Second", g -> g.node("c", "C").node("d", "D").edge("c", "d", "Local"))
        .edge("b", "c", "Repeated").edge("b", "c", "Repeated").edge("g1", "g2", "Groups")
        .edge("a", "a", "Self loop").build();
    var result = engine.layoutWithEdgeLabels(data, new LayoutOptions(direction, 160, 60), fallback);
    assertEquals(6, result.labels().size());
    assertDoesNotThrow(() -> EdgeLabelGeometryValidator.validate(data, result));
    assertTrue(result.labels().stream().allMatch(l -> l.width() == 140 && l.height() == 30));
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void placesAncestorDescendantLabelsWithHeadersAndPadding(Direction direction) {
    var data = Diagrams.diagram("Ancestors")
        .group("outer", "Outer", GroupStyle.builder().headerHeight(54).build(),
            outer -> outer.group("inner", "Inner", inner -> inner.node("a", "A")))
        .edge("inner", "a", "Down").edge("a", "inner", "Up").build();
    var options = LayoutOptions.builder().direction(direction).groupPadding(35).build();
    var result = engine.layoutWithEdgeLabels(data, options, fallback);
    assertDoesNotThrow(() -> EdgeLabelGeometryValidator.validate(data, result));
    assertEquals(2, result.labels().size());
    var outer = node(result, "outer");
    for (var label : result.labels())
      assertTrue(label.y() >= outer.y() + 54 - .00001);
  }

  @Test
  void largerLabelGrowsContainingGroupAndKeepsDirectSpiValidation() {
    DiagramData data = Diagrams.diagram("Growth")
        .group("g", "Group", g -> g.node("a", "A").node("b", "B").edge("a", "b", "Long label"))
        .build();
    var options = LayoutOptions.builder().build();
    var small = engine.layoutWithEdgeLabels(data, options, new EdgeLabelSize(40, 20));
    var large = engine.layoutWithEdgeLabels(data, options, new EdgeLabelSize(400, 80));
    assertTrue(node(large, "g").width() > node(small, "g").width());
    assertThrows(IllegalArgumentException.class,
        () -> engine.layoutWithEdgeLabels(data, options, null));
    var empty = Diagrams.diagram("Empty").build();
    assertThrows(IllegalArgumentException.class,
        () -> engine.layoutWithEdgeLabels(empty, options, null));
    assertThrows(IllegalArgumentException.class,
        () -> engine.layoutWithEdgeLabels(data, null, fallback));
    assertTrue(engine.layoutWithEdgeLabels(empty, options, fallback).labels().isEmpty());
  }

  private PositionedNode node(LabeledLayoutResult result, String id) {
    return result.geometry().nodes().stream().filter(n -> n.id().equals(id)).findFirst()
        .orElseThrow();
  }
}
