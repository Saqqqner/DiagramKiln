package io.github.saqqqner.diagramkiln;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.layout.ElkLayout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeOrderTest {
  @Test
  void preservesLegacyDefaultsAndExporterSnapshot() {
    assertFalse(new LayoutOptions(Direction.RIGHT, 160, 60).preserveNodeOrder());
    assertFalse(new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20).preserveNodeOrder());
    assertFalse(new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20, true).preserveNodeOrder());
    var builder = LayoutOptions.builder().preserveNodeOrder(true).feedbackEdges(true);
    var options = builder.build();
    builder.preserveNodeOrder(false);
    assertTrue(options.preserveNodeOrder());
    var observed = new AtomicReference<LayoutOptions>();
    var exporter = DrawioExporter.builder().layoutOptions(options).direction(Direction.RIGHT)
        .nodeSize(200, 80).layout((data, settings) -> {
          observed.set(settings);
          return new LayoutResult(List.of(), List.of());
        });
    exporter.build().toXml(Diagrams.diagram("empty").build());
    assertTrue(observed.get().preserveNodeOrder());
    assertTrue(observed.get().feedbackEdges());
    exporter.layoutOptions(LayoutOptions.builder().build()).build()
        .toXml(Diagrams.diagram("empty").build());
    assertFalse(observed.get().preserveNodeOrder());
  }

  @ParameterizedTest
  @EnumSource(value = Direction.class, names =
  {"RIGHT", "DOWN"
  })
  void keepsDeclaredOrderWithinLayersEvenWhenItCausesCrossings(Direction direction) {
    assertLayerOrder(branches(), direction);
  }

  @Test
  void keepsDeclaredOrderInsideNestedGroups() {
    assertLayerOrder(Diagrams.diagram("Nested")
        .group("outer", "Outer", g -> g.group("inner", branches())).build(), Direction.RIGHT);
  }

  @Test
  void keepsOrderWithCrossGroupConnectionsAndSelfLoops() {
    var data = Diagrams.diagram("Cross group")
        .group("outer", "Outer", g -> g.group("inner", branches())).node("external", "External")
        .edge("d", "external", "cross group").edge("external", "external", "loop").build();
    assertLayerOrder(data, Direction.RIGHT);
    assertTrue(DrawioExporter.builder()
        .layoutOptions(LayoutOptions.builder().preserveNodeOrder(true).feedbackEdges(true).build())
        .build().toXml(data).contains("cross group"));
  }

  private DiagramData branches() {
    return Diagrams.diagram("Branches").node("a", "First source").node("b", "Second source")
        .node("c", "First target").node("d", "Second target").edge("a", "d", "cross")
        .edge("b", "c", "cross").edge("a", "c", "join").build();
  }

  private void assertLayerOrder(DiagramData data, Direction direction) {
    var layout = new ElkLayout().layout(data, LayoutOptions.builder().direction(direction)
        .preserveNodeOrder(true).feedbackEdges(true).build());
    var a = find(layout, "a");
    var b = find(layout, "b");
    var c = find(layout, "c");
    var d = find(layout, "d");
    if (direction == Direction.RIGHT) {
      assertTrue(Math.abs(a.x() - b.x()) < 0.001);
      assertTrue(Math.abs(c.x() - d.x()) < 0.001);
      assertTrue(a.y() + a.height() <= b.y());
      assertTrue(c.y() + c.height() <= d.y());
    }
    else {
      assertTrue(Math.abs(a.y() - b.y()) < 0.001);
      assertTrue(Math.abs(c.y() - d.y()) < 0.001);
      assertTrue(a.x() + a.width() <= b.x());
      assertTrue(c.x() + c.width() <= d.x());
    }
  }

  private PositionedNode find(LayoutResult result, String id) {
    return result.nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow();
  }
}
