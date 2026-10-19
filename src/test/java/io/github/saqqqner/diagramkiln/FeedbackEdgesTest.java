package io.github.saqqqner.diagramkiln;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.layout.ElkLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackEdgesTest {
  @Test
  void preservesDefaultsConstructorsAndExporterSettings() {
    assertFalse(new LayoutOptions(Direction.RIGHT, 160, 60).feedbackEdges());
    assertFalse(new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20).feedbackEdges());
    assertFalse(LayoutOptions.builder().build().feedbackEdges());
    var builder = LayoutOptions.builder().feedbackEdges(true);
    var options = builder.build();
    builder.feedbackEdges(false);
    assertTrue(options.feedbackEdges());
    var observed = new AtomicReference<LayoutOptions>();
    var exporter = DrawioExporter.builder().layoutOptions(options).direction(Direction.DOWN)
        .nodeSize(200, 80).layout((data, settings) -> {
          observed.set(settings);
          return new LayoutResult(List.of(), List.of());
        });
    exporter.build().toXml(Diagrams.diagram("empty").build());
    assertTrue(observed.get().feedbackEdges());
    exporter.layoutOptions(LayoutOptions.builder().build()).build()
        .toXml(Diagrams.diagram("empty").build());
    assertFalse(observed.get().feedbackEdges());
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void keepsCyclicTransitionsOnFlowSides(Direction direction) {
    var data = cycle("a", "b", "c");
    var result = new ElkLayout().layout(data,
        LayoutOptions.builder().direction(direction).feedbackEdges(true).build());
    assertFlowSides(data, result, direction);
  }

  @Test
  void appliesFeedbackRoutingInsideNestedGroups() {
    var data = Diagrams.diagram("nested")
        .group("outer", "Outer", outer -> outer.group("inner", cycle("a", "b", "c")))
        .node("external", "External").edge("c", "external", "cross group").build();
    var result = new ElkLayout().layout(data, LayoutOptions.builder().feedbackEdges(true).build());
    assertFlowSides(cycle("a", "b", "c"),
        new LayoutResult(result.nodes(), result.edges().subList(1, 4)), Direction.RIGHT);
    assertTrue(
        DrawioExporter.builder().layoutOptions(LayoutOptions.builder().feedbackEdges(true).build())
            .build().toXml(data).contains("cross group"));
  }

  @Test
  void writesLeftEntryAnchorsForStatusCycle() throws Exception {
    var data = cycle("draft", "review", "rejected");
    String xml = DrawioExporter.builder()
        .layoutOptions(LayoutOptions.builder().feedbackEdges(true).build()).build().toXml(data);
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    var cells = document.getElementsByTagName("mxCell");
    int edges = 0;
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (org.w3c.dom.Element) cells.item(i);
      if (cell.getAttribute("edge").equals("1")) {
        assertTrue(cell.getAttribute("style").contains("entryX=0.0;"));
        assertTrue(cell.getAttribute("style").contains("exitX=1.0;"));
        edges++;
      }
    }
    assertEquals(3, edges);
  }

  private DiagramData cycle(String a, String b, String c) {
    return Diagrams.diagram("Cycle").node(a, a).node(b, b).node(c, c).edge(a, b, "forward")
        .edge(b, c, "reject").edge(c, a, "revise").build();
  }

  private void assertFlowSides(DiagramData data, LayoutResult result, Direction direction) {
    for (int i = 0; i < data.edges().size(); i++) {
      var edge = data.edges().get(i);
      var points = result.edges().get(i).points();
      var source = find(result, edge.sourceId());
      var target = find(result, edge.targetId());
      var first = points.get(0);
      var last = points.get(points.size() - 1);
      switch (direction) {
        case RIGHT -> {
          assertEquals(source.x() + source.width(), first.x(), 0.001);
          assertEquals(target.x(), last.x(), 0.001);
        }
        case LEFT -> {
          assertEquals(source.x(), first.x(), 0.001);
          assertEquals(target.x() + target.width(), last.x(), 0.001);
        }
        case DOWN -> {
          assertEquals(source.y() + source.height(), first.y(), 0.001);
          assertEquals(target.y(), last.y(), 0.001);
        }
        case UP -> {
          assertEquals(source.y(), first.y(), 0.001);
          assertEquals(target.y() + target.height(), last.y(), 0.001);
        }
      }
    }
  }

  private PositionedNode find(LayoutResult result, String id) {
    return result.nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow();
  }
}
