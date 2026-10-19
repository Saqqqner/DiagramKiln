package io.github.saqqqner.diagramkiln;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.DefaultDiagram;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.NodeSize;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.dto.SizedNode;
import io.github.saqqqner.diagramkiln.internal.DiagramIndex;
import io.github.saqqqner.diagramkiln.internal.DiagramValidator;
import io.github.saqqqner.diagramkiln.internal.NodeGeometry;
import io.github.saqqqner.diagramkiln.layout.ElkLayout;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NodeDimensionsTest {
  @ParameterizedTest
  @ValueSource(doubles =
  {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
  })
  void rejectsInvalidDimensionsImmediately(double invalid) {
    assertThrows(IllegalArgumentException.class, () -> new NodeSize(invalid, 90));
    assertThrows(IllegalArgumentException.class, () -> new NodeSize(240, invalid));
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("Invalid").node("a", "A", invalid, 90));
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("Invalid").node("a", "A", NodeStyle.defaults(), 240, invalid));
  }

  @Test
  void retainsSizedCapabilityThroughDslAndRepeatedNestedCapture() {
    var styled = NodeStyle.builder().shape(NodeShape.DIAMOND).build();
    var data = Diagrams.diagram("Mixed").node("a", "A", 240, 90).node("fallback", "Fallback")
        .group("group", "Nested", nested -> nested.node("b", "B", styled, 180, 110)).build();
    var captured = DiagramValidator.snapshot(DiagramValidator.snapshot(data));
    assertEquals(new NodeSize(240, 90),
        assertInstanceOf(SizedDiagramNode.class, captured.nodes().get(0)).size());
    assertInstanceOf(DefaultNode.class, captured.nodes().get(1));
    var group = assertInstanceOf(DiagramGroup.class, captured.nodes().get(2));
    var child = assertInstanceOf(SizedDiagramNode.class, group.diagram().nodes().get(0));
    assertEquals(new NodeSize(180, 110), child.size());
    assertEquals(styled, child.style());
    assertEquals(NodeStyle.defaults(), new SizedNode("dto", "DTO", new NodeSize(240, 90)).style());
    assertThrows(IllegalArgumentException.class,
        () -> new SizedNode("null", "Null", NodeStyle.defaults(), null));
  }

  @Test
  void capturesMutableCustomSizeExactlyOnceAndDoesNotRetainCaller() {
    NodeSize[] size =
    {new NodeSize(240, 90)
    };
    var reads = new AtomicInteger();
    SizedDiagramNode mutable = new SizedDiagramNode() {
      public String id() {
        return "a";
      }
      public String label() {
        return "A";
      }
      public NodeSize size() {
        reads.incrementAndGet();
        return size[0];
      }
    };
    var data = Diagrams.diagram("Snapshot").nodes(List.of(mutable)).build();
    size[0] = new NodeSize(1, 2);
    assertEquals(1, reads.get());
    assertEquals(new NodeSize(240, 90),
        assertInstanceOf(SizedDiagramNode.class, DiagramValidator.snapshot(data).nodes().get(0))
            .size());
    assertEquals(1, reads.get());
  }

  @Test
  void rejectsNullCustomSizeAndSizedGroupsBeforeLayout() {
    record NullSize(String id, String label) implements SizedDiagramNode {
      public NodeSize size() {
        return null;
      }
    }
    record SizedGroup(String id, DiagramData diagram) implements DiagramGroup, SizedDiagramNode {
      public NodeSize size() {
        return new NodeSize(240, 90);
      }
    }
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      fail("Invalid input must fail before layout");
      return null;
    }).build();
    for (var invalid : List.<DiagramNode>of(new NullSize("bad", "Bad"),
        new SizedGroup("bad", Diagrams.diagram("Empty").build()))) {
      var error = assertThrows(IllegalArgumentException.class,
          () -> exporter.toXml(new DefaultDiagram("Invalid", List.of(invalid), List.of())));
      assertTrue(error.getMessage().contains("bad"));
    }
  }

  @Test
  void customLayoutReceivesSizeButControlsFinalDimensionsAndRoutes() throws Exception {
    var input = Diagrams.diagram("Custom").node("a", "A", 240, 90).node("b", "B")
        .edge("a", "b", "Route").build();
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      assertEquals(new NodeSize(240, 90),
          assertInstanceOf(SizedDiagramNode.class, data.nodes().get(0)).size());
      assertEquals(160, options.nodeWidth());
      return new LayoutResult(
          List.of(new PositionedNode("a", 0, 0, 100, 40), new PositionedNode("b", 180, 0, 160, 60)),
          List.of(new RoutedEdge(0, List.of(new Point(100, 20), new Point(180, 20)))));
    }).build();
    var xml = geometries(exporter.toXml(input));
    assertDimensions(xml.get("A"), 100, 40);
    assertDimensions(xml.get("B"), 160, 60);
    assertEquals(exporter.toXml(input), decode(exporter.toDrawioFragment(input)));
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void elkPreservesMixedDimensionsAndShapeRoutesAcrossGroups(Direction direction) throws Exception {
    for (int shape = 0; shape < 4; shape++) {
      var style = shapeStyle(shape);
      var data = Diagrams.diagram("Mixed sizes").node("a", "A", style, 240, 90)
          .group("outer", "Outer",
              outer -> outer.group("inner", "Inner", inner -> inner.node("b", "B", style, 180, 110))
                  .node("fallback", "Fallback", style))
          .group("empty", "Empty", empty -> {
          }).edge("a", "b", "cross").edge("a", "b", "parallel").edge("b", "fallback", "nested")
          .edge("a", "a", "self").build();
      var options = LayoutOptions.builder().direction(direction).build();
      var result = new ElkLayout().layout(data, options);
      Map<String, PositionedNode> bounds = new LinkedHashMap<>();
      result.nodes().forEach(node -> bounds.put(node.id(), node));
      assertBounds(bounds.get("a"), 240, 90);
      assertBounds(bounds.get("b"), 180, 110);
      assertBounds(bounds.get("fallback"), 160, 60);
      assertBounds(bounds.get("empty"), 160, 88);
      var index = new DiagramIndex(data);
      for (var node : result.nodes()) {
        String parentId = index.parentId(node.id());
        if (parentId != null) {
          var parent = bounds.get(parentId);
          assertTrue(node.x() >= parent.x() + 20 - 0.00001);
          assertTrue(node.y() >= parent.y() + 48 - 0.00001);
          assertTrue(node.x() + node.width() <= parent.x() + parent.width() - 20 + 0.00001,
              () -> node + " outside " + parent);
          assertTrue(node.y() + node.height() <= parent.y() + parent.height() - 20 + 0.00001,
              () -> node + " outside " + parent);
        }
      }
      for (var left : result.nodes()) {
        for (var right : result.nodes()) {
          if (!left.id().equals(right.id())
              && java.util.Objects.equals(index.parentId(left.id()), index.parentId(right.id())))
            assertTrue(left.x() + left.width() <= right.x() || right.x() + right.width() <= left.x()
                || left.y() + left.height() <= right.y() || right.y() + right.height() <= left.y());
        }
      }
      assertEquals(4, result.edges().size());
      for (var route : result.edges()) {
        var edge = index.edges().get(route.edgeIndex());
        assertTrue(NodeGeometry.onBoundary(route.points().get(0), bounds.get(edge.sourceId()),
            index.node(edge.sourceId())));
        assertTrue(NodeGeometry.onBoundary(route.points().get(route.points().size() - 1),
            bounds.get(edge.targetId()), index.node(edge.targetId())));
      }
      var exporter = DrawioExporter.builder().direction(direction).build();
      String xml = exporter.toXml(data);
      var geometry = geometries(xml);
      assertDimensions(geometry.get("A"), 240, 90);
      assertDimensions(geometry.get("B"), 180, 110);
      assertDimensions(geometry.get("Fallback"), 160, 60);
      assertEquals(bounds.get("b").x() - bounds.get("inner").x(),
          Double.parseDouble(geometry.get("B").getAttribute("x")), 0.00001);
      assertEquals(bounds.get("b").y() - bounds.get("inner").y(),
          Double.parseDouble(geometry.get("B").getAttribute("y")), 0.00001);
      assertEquals(xml, decode(exporter.toDrawioFragment(data)));
    }
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void flatElkUsesConfiguredFallbackAlongsideExactExplicitSizes(Direction direction) {
    var data = Diagrams.diagram("Flat").node("a", "A", 240, 90).node("b", "B", 180, 110)
        .node("c", "C").edge("a", "b", "").edge("b", "c", "").build();
    var result = new ElkLayout().layout(data,
        LayoutOptions.builder().direction(direction).nodeSize(200, 80).build());
    assertBounds(result.nodes().get(0), 240, 90);
    assertBounds(result.nodes().get(1), 180, 110);
    assertBounds(result.nodes().get(2), 200, 80);
  }

  private NodeStyle shapeStyle(int shape) {
    return NodeStyle.builder()
        .shape(shape < 2 ? NodeShape.RECTANGLE : shape == 2 ? NodeShape.ELLIPSE : NodeShape.DIAMOND)
        .rounded(shape == 1).build();
  }

  private void assertBounds(PositionedNode node, double width, double height) {
    assertEquals(width, node.width());
    assertEquals(height, node.height());
  }

  private void assertDimensions(Element geometry, double width, double height) {
    assertEquals(width, Double.parseDouble(geometry.getAttribute("width")));
    assertEquals(height, Double.parseDouble(geometry.getAttribute("height")));
  }

  private Map<String, Element> geometries(String xml) throws Exception {
    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
    var cells = document.getElementsByTagName("mxCell");
    Map<String, Element> result = new LinkedHashMap<>();
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      if (cell.getAttribute("vertex").equals("1"))
        result.put(cell.getAttribute("value"),
            (Element) cell.getElementsByTagName("mxGeometry").item(0));
    }
    return result;
  }

  private String decode(String fragment) throws Exception {
    var compressed = Base64.getDecoder()
        .decode(URLDecoder.decode(fragment.substring(2), StandardCharsets.UTF_8));
    var inflater = new Inflater(true);
    try {
      inflater.setInput(compressed);
      var bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        assertTrue(count > 0);
        bytes.write(buffer, 0, count);
      }
      return URLDecoder.decode(bytes.toString(StandardCharsets.UTF_8).replace("+", "%2B"),
          StandardCharsets.UTF_8);
    }
    finally {
      inflater.end();
    }
  }
}
