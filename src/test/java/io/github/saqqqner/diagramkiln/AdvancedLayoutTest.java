package io.github.saqqqner.diagramkiln;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.config.Alignment;
import io.github.saqqqner.diagramkiln.config.CycleBreaking;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayerConstraint;
import io.github.saqqqner.diagramkiln.config.LayeredLayoutOptions;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.config.NodeLayout;
import io.github.saqqqner.diagramkiln.config.NodePlacement;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.NodeSize;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.layout.ElkLayout;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdvancedLayoutTest {
  private final NodeStyle style = NodeStyle.defaults();

  @Test
  void lastConstraintRespectsFlowDirectionAndContainingGraph() {
    var last = NodeLayout.builder().layerConstraint(LayerConstraint.LAST).build();
    var data = Diagrams.diagram("LAST")
        .group("g", "Group",
            g -> g.node("a", "A").node("b", "B").node("c", "C").node("end", "End")
                .node("short", "Short sink", style, last).edge("a", "b", "").edge("b", "c", "")
                .edge("c", "end", "").edge("a", "short", ""))
        .build();
    for (var direction : Direction.values()) {
      var result = new ElkLayout().layout(data, LayoutOptions.builder().direction(direction)
          .layered(l -> l.cycleBreaking(CycleBreaking.DFS_NODE_ORDER)).build());
      var end = find(result, "end");
      var constrained = find(result, "short");
      if (direction == Direction.RIGHT || direction == Direction.LEFT)
        assertEquals(end.x(), constrained.x(), 0.001);
      else
        assertEquals(end.y(), constrained.y(), 0.001);
    }
  }

  @Test
  void rejectsLeafLayoutMetadataOnGroups() {
    class InvalidGroup implements DiagramGroup, LayoutDiagramNode {
      public String id() {
        return "invalid-group";
      }
      public String label() {
        return "Invalid group";
      }
      public DiagramData diagram() {
        return Diagrams.diagram("nested").build();
      }
      public NodeLayout layout() {
        throw new AssertionError("Invalid group metadata must not be read");
      }
    }
    var error = assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("invalid").nodes(List.of(new InvalidGroup())).build());
    assertTrue(error.getMessage().contains("invalid-group"));
    assertTrue(error.getMessage().contains("leaf layout"));
  }

  @Test
  void acceptsAllBundledStrategiesOnNestedCyclicGraphs() {
    var data = Diagrams.diagram("strategies")
        .group("g", "Group",
            g -> g.node("a", "A").node("b", "B").node("c", "C").edge("a", "b", "")
                .edge("b", "c", "").edge("c", "a", "back"))
        .node("outside", "Outside").edge("c", "outside", "cross").build();
    var engine = new ElkLayout();
    for (var strategy : CycleBreaking.values()) {
      var result = engine.layout(data,
          LayoutOptions.builder().layered(l -> l.cycleBreaking(strategy)).build());
      assertEquals(4, result.edges().size(), strategy.name());
    }
    for (var placement : NodePlacement.values()) {
      var result = engine.layout(data,
          LayoutOptions.builder().layered(l -> l.nodePlacement(placement)).build());
      assertEquals(4, result.edges().size(), placement.name());
    }
    for (var alignment : Alignment.values()) {
      var result = engine.layout(data,
          LayoutOptions.builder().layered(l -> l.alignment(alignment)).build());
      assertEquals(4, result.edges().size(), alignment.name());
    }
    assertEquals(
        java.util.Arrays.stream(org.eclipse.elk.alg.layered.options.CycleBreakingStrategy.values())
            .map(Enum::name).toList(),
        java.util.Arrays.stream(CycleBreaking.values()).map(Enum::name).toList());
    assertEquals(
        java.util.Arrays.stream(org.eclipse.elk.alg.layered.options.NodePlacementStrategy.values())
            .map(Enum::name).toList(),
        java.util.Arrays.stream(NodePlacement.values()).map(Enum::name).toList());
    assertEquals(java.util.Arrays.stream(org.eclipse.elk.core.options.Alignment.values())
        .map(Enum::name).toList(),
        java.util.Arrays.stream(Alignment.values()).map(Enum::name).toList());
  }

  @Test
  void validatesAdvancedSettingsAndCapturesCallbackImmediately() {
    var retained = new AtomicReference<LayeredLayoutOptions.Builder>();
    var builder = LayoutOptions.builder().edgeNodeSpacing(80).edgeSpacing(81)
        .edgeSpacingBetweenLayers(82).portSpacing(20).layered(l -> {
          retained.set(l);
          l.cycleBreaking(CycleBreaking.DFS_NODE_ORDER).nodePlacement(NodePlacement.NETWORK_SIMPLEX)
              .alignment(Alignment.CENTER).thoroughness(50);
        });
    retained.get().thoroughness(1);
    var options = builder.build();
    builder.edgeNodeSpacing(0).layered(l -> l.thoroughness(2));
    assertEquals(80, options.edgeNodeSpacing());
    assertEquals(50, options.layered().thoroughness());
    var seen = new AtomicReference<LayoutOptions>();
    DrawioExporter.builder().layoutOptions(options).nodeSize(180, 60).layout((data, settings) -> {
      seen.set(settings);
      return new LayoutResult(List.of(), List.of());
    }).build().toXml(Diagrams.diagram("empty").build());
    assertEquals(81, seen.get().edgeSpacing());
    assertEquals(82, seen.get().edgeSpacingBetweenLayers());
    assertEquals(20, seen.get().portSpacing());
    assertEquals(options.layered(), seen.get().layered());
    for (double invalid : new double[]
    {-1, Double.NaN, Double.POSITIVE_INFINITY
    }) {
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().edgeNodeSpacing(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().edgeSpacing(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().edgeSpacingBetweenLayers(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().portSpacing(invalid).build());
    }
    assertThrows(IllegalArgumentException.class, () -> LayoutOptions.builder().layered(null));
    assertThrows(IllegalArgumentException.class,
        () -> LayoutOptions.builder().layeredOptions(null));
    assertThrows(IllegalArgumentException.class,
        () -> LayeredLayoutOptions.builder().thoroughness(0).build());
    assertThrows(IllegalArgumentException.class,
        () -> LayeredLayoutOptions.builder().alignment(null).build());
    assertThrows(IllegalArgumentException.class,
        () -> LayeredLayoutOptions.builder().nodePlacement(null).build());
    assertThrows(IllegalArgumentException.class,
        () -> LayeredLayoutOptions.builder().cycleBreaking(null).build());
    assertThrows(IllegalArgumentException.class,
        () -> NodeLayout.builder().layerConstraint(null).build());
    // This legacy call must remain unambiguous after adding node-layout overloads.
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("null style").node("n", "N", null).build());
  }

  @Test
  void preservesLayoutMetadataTogetherWithExplicitSizeAcrossSnapshots() {
    var first = NodeLayout.builder().layerConstraint(LayerConstraint.FIRST).build();
    class AppNode implements SizedDiagramNode, LayoutDiagramNode {
      NodeLayout settings = first;
      int reads;
      public String id() {
        return "custom";
      }
      public String label() {
        return "Custom";
      }
      public NodeSize size() {
        return new NodeSize(220, 90);
      }
      public NodeLayout layout() {
        reads++;
        return settings;
      }
    }
    var input = new AppNode();
    var data = Diagrams.diagram("snapshot").nodes(List.of(input)).build();
    assertEquals(1, input.reads);
    input.settings = null;
    var frozen = data.nodes().get(0);
    assertEquals(first, ((LayoutDiagramNode) frozen).layout());
    assertEquals(new NodeSize(220, 90), ((SizedDiagramNode) frozen).size());
    String xml = DrawioExporter.create().toXml(data);
    assertTrue(xml.contains("220.0"));
    assertEquals(1, input.reads);
    var error = assertThrows(IllegalArgumentException.class, () -> DrawioExporter.create()
        .toXml(Diagrams.diagram("invalid").nodes(List.of(input)).build()));
    assertTrue(error.getMessage().contains("custom"));
  }

  @Test
  void forcesASeparateShortSourceIntoFirstLayer() {
    var first = NodeLayout.builder().layerConstraint(LayerConstraint.FIRST).build();
    var data = Diagrams.diagram("FIRST").node("a", "A").node("b", "B").node("c", "C")
        .node("short", "Short source", style, first).node("end", "End").edge("a", "b", "")
        .edge("b", "c", "").edge("c", "end", "").edge("short", "end", "").build();
    var result = new ElkLayout().layout(data, statusOptions());
    assertEquals(find(result, "a").x(), find(result, "short").x(), 0.001);
    assertTrue(find(result, "end").x() > find(result, "short").x());
    assertTrue(DrawioExporter.builder().layoutOptions(statusOptions()).build().export(data)
        .drawioFragment().startsWith("#R"));
  }

  @Test
  void supportsFirstWithSizeInsideNestedGraphsAndCrossConnections() {
    var first = NodeLayout.builder().layerConstraint(LayerConstraint.FIRST).build();
    var data = Diagrams.diagram("Nested")
        .group("g", "Group",
            g -> g.node("a", "A", style, 220, 90, first).node("b", "B").node("c", "C")
                .edge("a", "b", "").edge("b", "c", "").edge("c", "b", "back"))
        .node("outside", "Outside").edge("c", "outside", "cross").build();
    var result = new ElkLayout().layout(data, statusOptions());
    assertTrue(find(result, "a").x() < find(result, "b").x());
    assertEquals(220, find(result, "a").width());
    String xml = DrawioExporter.builder().layoutOptions(statusOptions()).build().toXml(data);
    assertTrue(xml.contains("cross"));
    assertTrue(xml.contains("back"));
  }

  @Test
  void portAndBetweenLayerEdgeSpacingAffectActualRoutes() {
    var data = Diagrams.diagram("parallel").node("a", "A", 180, 200).node("b", "B", 180, 60)
        .edge("a", "b", "1").edge("a", "b", "2").edge("a", "b", "3").build();
    var engine = new ElkLayout();
    var baseline = engine.layout(data, LayoutOptions.builder().build());
    var spaced = engine.layout(data,
        LayoutOptions.builder().portSpacing(40).edgeSpacingBetweenLayers(80).build());
    assertTrue(find(spaced, "b").x() - find(spaced, "a").x() > find(baseline, "b").x()
        - find(baseline, "a").x());
    var ports = spaced.edges().stream().map(e -> e.points().get(0).y()).sorted().toList();
    assertTrue(ports.get(1) - ports.get(0) >= 40 - 0.001);
    assertTrue(ports.get(2) - ports.get(1) >= 40 - 0.001);
  }

  @Test
  void withinLayerClearancesAffectNestedBackEdgesAndSelfLoops() {
    var data = Diagrams.diagram("Spacing")
        .group("g", "Group",
            g -> g.node("a", "A").node("b", "B").node("c", "C").edge("a", "b", "")
                .edge("b", "c", "").edge("c", "a", "back").edge("a", "a", "loop1")
                .edge("a", "a", "loop2"))
        .build();
    var engine = new ElkLayout();
    var baseline = engine.layout(data, LayoutOptions.builder().build());
    var clearNodes = engine.layout(data, LayoutOptions.builder().edgeNodeSpacing(80).build());
    double baselineBackY = baseline.edges().get(2).points().stream().mapToDouble(p -> p.y()).min()
        .orElseThrow();
    double backY = clearNodes.edges().get(2).points().stream().mapToDouble(p -> p.y()).min()
        .orElseThrow();
    assertTrue(find(clearNodes, "b").y() - backY >= 80);
    assertTrue(find(baseline, "b").y() - baselineBackY < 80);
    var clearEdges = engine.layout(data, LayoutOptions.builder().edgeSpacing(80).build());
    double firstLoopY = clearEdges.edges().get(3).points().stream().mapToDouble(p -> p.y()).min()
        .orElseThrow();
    double secondLoopY = clearEdges.edges().get(4).points().stream().mapToDouble(p -> p.y()).min()
        .orElseThrow();
    assertEquals(80, firstLoopY - secondLoopY, 0.001);
    assertTrue(find(clearEdges, "g").height() > find(baseline, "g").height());
  }

  private LayoutOptions statusOptions() {
    return LayoutOptions.builder().nodeSize(180, 60).nodeSpacing(80).layerSpacing(75)
        .edgeNodeSpacing(80).edgeSpacing(80).edgeSpacingBetweenLayers(80).portSpacing(20)
        .feedbackEdges(true)
        .layered(l -> l.cycleBreaking(CycleBreaking.DFS_NODE_ORDER)
            .nodePlacement(NodePlacement.NETWORK_SIMPLEX).alignment(Alignment.CENTER)
            .thoroughness(50))
        .build();
  }

  private PositionedNode find(LayoutResult result, String id) {
    return result.nodes().stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
  }
}
