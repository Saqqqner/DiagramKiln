package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.DefaultDiagram;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedEdgeLabel;
import io.github.saqqqner.diagramkiln.dto.SizedEdge;
import io.github.saqqqner.diagramkiln.internal.validation.DiagramValidator;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class EdgeLabelContractsTest {
  @ParameterizedTest
  @ValueSource(doubles =
  {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
  })
  void rejectsInvalidReservedDimensions(double invalid) {
    assertThrows(IllegalArgumentException.class, () -> new EdgeLabelSize(invalid, 20));
    assertThrows(IllegalArgumentException.class, () -> new EdgeLabelSize(100, invalid));
  }

  @Test
  void preservesPerEdgeSizeAndStyleAcrossNestedSnapshots() {
    var size = new EdgeLabelSize(240, 40);
    var style = EdgeStyle.builder().fontSize(18).build();
    var data = Diagrams.diagram("Root")
        .group("g", "Group",
            group -> group.node("a", "A").node("b", "B").edge("a", "b", "Long label", style, size))
        .build();
    var captured = DiagramValidator.snapshot(DiagramValidator.snapshot(data));
    var group = assertInstanceOf(DiagramGroup.class, captured.nodes().get(0));
    var edge = assertInstanceOf(SizedDiagramEdge.class, group.diagram().edges().get(0));
    assertEquals(size, edge.labelSize());
    assertEquals(style, edge.style());
  }

  @Test
  void capturesSizeOnceAcrossNestedSnapshotsAndDetachesCaller() {
    var reads = new AtomicInteger();
    EdgeLabelSize[] current =
    {new EdgeLabelSize(240, 40)
    };
    SizedDiagramEdge mutable = new SizedDiagramEdge() {
      public String sourceId() {
        return "a";
      }
      public String targetId() {
        return "b";
      }
      public String label() {
        return "Label";
      }
      public EdgeLabelSize labelSize() {
        reads.incrementAndGet();
        return current[0];
      }
    };
    var nested = new DefaultDiagram("Nested",
        List.of(new DefaultNode("a", "A"), new DefaultNode("b", "B")), List.of(mutable));
    var diagram = Diagrams.diagram("Root").group("g", nested).build();
    current[0] = new EdgeLabelSize(1, 2);
    var group = assertInstanceOf(DiagramGroup.class,
        DiagramValidator.snapshot(diagram).nodes().get(0));
    assertEquals(new EdgeLabelSize(240, 40),
        assertInstanceOf(SizedDiagramEdge.class, group.diagram().edges().get(0)).labelSize());
    assertEquals(1, reads.get());
  }

  @Test
  void rejectsMissingSizeAtConstructionButValidatesGraphPropertiesDuringCapture() {
    assertThrows(IllegalArgumentException.class,
        () -> new SizedEdge("a", "b", "Text", EdgeStyle.defaults(), null));
    var invalid = new SizedEdge("", "b", null, null, new EdgeLabelSize(100, 20));
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("Invalid").edges(List.of(invalid)).build());
  }

  @Test
  void rejectsNullCustomSizeBeforeLegacyLayoutEvenForEmptyLabel() {
    record NullSize(String sourceId, String targetId, String label) implements SizedDiagramEdge {
      public EdgeLabelSize labelSize() {
        return null;
      }
    }
    var data = new DefaultDiagram("Invalid", List.of(new DefaultNode("a", "A")),
        List.of(new NullSize("a", "a", "")));
    var exporter = DrawioExporter.builder().layout((diagram, options) -> {
      fail("Invalid input must fail before layout");
      return null;
    }).build();
    var error = assertThrows(IllegalArgumentException.class, () -> exporter.toXml(data));
    assertTrue(error.getMessage().contains("a"));
    assertTrue(error.getMessage().contains("size"));
  }

  @Test
  void copiesLabelListAndRejectsNullGeometryListsAndElements() {
    var geometry = new LayoutResult(List.of(), List.of());
    var label = new PositionedEdgeLabel(0, 10, 20, 80, 30);
    var input = new ArrayList<>(List.of(label));
    var result = new LabeledLayoutResult(geometry, input);
    input.clear();
    assertEquals(List.of(label), result.labels());
    assertThrows(UnsupportedOperationException.class, () -> result.labels().clear());
    assertThrows(IllegalArgumentException.class, () -> new LabeledLayoutResult(null, List.of()));
    assertThrows(IllegalArgumentException.class, () -> new LabeledLayoutResult(geometry, null));
    assertThrows(IllegalArgumentException.class,
        () -> new LabeledLayoutResult(geometry, Arrays.asList(label, null)));
  }

  @Test
  void legacyLambdaWorksAndDefaultLabelOperationFailsWithoutInvokingIt() {
    var calls = new AtomicInteger();
    LayoutEngine legacy = (data, options) -> {
      calls.incrementAndGet();
      return new LayoutResult(List.of(), List.of());
    };
    var empty = Diagrams.diagram("Empty").build();
    assertThrows(UnsupportedOperationException.class, () -> legacy.layoutWithEdgeLabels(empty,
        LayoutOptions.builder().build(), new EdgeLabelSize(80, 20)));
    assertEquals(0, calls.get());
    assertTrue(
        DrawioExporter.builder().layout(legacy).build().toXml(empty).contains("mxGraphModel"));
    assertEquals(1, calls.get());
  }

  @Test
  void legacyNullStyleCallRemainsUnambiguousAndFailsAtBuild() {
    var builder = Diagrams.diagram("Legacy").node("a", "A").edge("a", "a", "Text", null);
    assertThrows(IllegalArgumentException.class, builder::build);
  }
}
