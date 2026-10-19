package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.dto.DefaultEdge;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawioExporterTest {
  private static DiagramData chain() {
    return Diagrams.diagram("Статусы заказа").node("a", "Новый").node("b", "Оплачен")
        .edge("a", "b", "Оплатить").build();
  }

  @Test
  void exportsEditableDrawioWithRealElkGeometry() throws Exception {
    Document xml = parse(DrawioExporter.create().toXml(chain()));
    assertEquals("mxfile", xml.getDocumentElement().getTagName());
    assertEquals("Статусы заказа", element(xml, "diagram", 0).getAttribute("name"));
    var nodes = cells(xml, "vertex");
    var edges = cells(xml, "edge");
    assertEquals(2, nodes.size());
    assertEquals(1, edges.size());
    var first = child(nodes.get(0), "mxGeometry");
    var second = child(nodes.get(1), "mxGeometry");
    assertEquals(160, number(first, "width"));
    assertEquals(60, number(first, "height"));
    assertTrue(number(first, "x") + 160 < number(second, "x"));
    assertEquals(nodes.get(0).getAttribute("id"), edges.get(0).getAttribute("source"));
    assertEquals(nodes.get(1).getAttribute("id"), edges.get(0).getAttribute("target"));
    assertEquals("Оплатить", edges.get(0).getAttribute("value"));
    assertEquals("1", child(edges.get(0), "mxGeometry").getAttribute("relative"));
  }

  @Test
  void roundTripsPlainTextAndWhitespaceWithoutHtmlInterpretation() throws Exception {
    String text = "<b>Привет & \"мир\"</b>\nВторая\tстрока\r🙂";
    var data = Diagrams.diagram(text).node("0", text).node("1", "").edge("0", "1", text).build();
    Document xml = parse(DrawioExporter.create().toXml(data));
    assertEquals(text, element(xml, "diagram", 0).getAttribute("name"));
    assertEquals(text, cells(xml, "vertex").get(0).getAttribute("value"));
    assertEquals(text, cells(xml, "edge").get(0).getAttribute("value"));
    assertTrue(cells(xml, "vertex").get(0).getAttribute("style").contains("html=0;"));
    var ids = new HashSet<String>();
    var allCells = xml.getElementsByTagName("mxCell");
    for (int i = 0; i < allCells.getLength(); i++) {
      assertTrue(ids.add(((Element) allCells.item(i)).getAttribute("id")));
    }
    assertEquals(5, ids.size());
    assertNotEquals("0", cells(xml, "vertex").get(0).getAttribute("id"));
    assertNotEquals("1", cells(xml, "vertex").get(1).getAttribute("id"));
  }

  @Test
  void transfersExactRouteAndEndpointAnchorsFromLayoutEngine() throws Exception {
    LayoutEngine engine = (data, options) -> fixedLayout(
        List.of(new Point(160, 30), new Point(190, 30), new Point(190, 120), new Point(220, 120)));
    Document xml = parse(DrawioExporter.builder().layout(engine).build().toXml(chain()));
    var edge = cells(xml, "edge").get(0);
    String style = edge.getAttribute("style");
    assertTrue(style.contains("noEdgeStyle=1;"));
    assertTrue(style.contains("exitPerimeter=0;"));
    assertTrue(style.contains("entryPerimeter=0;"));
    assertTrue(style.contains("exitX=1.0;exitY=0.5;"));
    assertTrue(style.contains("entryX=0.0;entryY=0.5;"));
    var points = child(edge, "mxGeometry").getElementsByTagName("mxPoint");
    assertEquals(2, points.getLength());
    assertEquals(190, number((Element) points.item(0), "x"));
    assertEquals(30, number((Element) points.item(0), "y"));
    assertEquals(190, number((Element) points.item(1), "x"));
    assertEquals(120, number((Element) points.item(1), "y"));
  }

  @Test
  void keepsParallelAndLoopEdgesInRealElkExports() throws Exception {
    var data = Diagrams.diagram("Routes").node("a", "A").node("b", "B").edge("a", "b", "first")
        .edge("a", "b", "second").edge("b", "a", "back").edge("a", "a", "loop").build();
    Document xml = parse(DrawioExporter.create().toXml(data));
    var edges = cells(xml, "edge");
    assertEquals(4, edges.size());
    assertEquals(edges.get(3).getAttribute("source"), edges.get(3).getAttribute("target"));
    assertTrue(edges.get(3).getElementsByTagName("mxPoint").getLength() >= 2);
    assertNotEquals(edges.get(0).getAttribute("style"), edges.get(1).getAttribute("style"));
    for (var edge : edges)
      assertTrue(edge.getAttribute("style").contains("noEdgeStyle=1;"));
  }

  @Test
  void forwardsDirectionAndSizeToActualLayout() throws Exception {
    Document xml = parse(DrawioExporter.builder().direction(Direction.DOWN).nodeSize(200, 80)
        .build().toXml(chain()));
    var nodes = cells(xml, "vertex");
    var a = child(nodes.get(0), "mxGeometry");
    var b = child(nodes.get(1), "mxGeometry");
    assertEquals(200, number(a, "width"));
    assertEquals(80, number(a, "height"));
    assertTrue(number(a, "y") + 80 < number(b, "y"));
  }

  @Test
  void exportsEmptyDiagramAndCanBeReused() throws Exception {
    var exporter = DrawioExporter.create();
    assertEquals(0,
        cells(parse(exporter.toXml(Diagrams.diagram("Empty").build())), "vertex").size());
    assertEquals(2, cells(parse(exporter.toXml(chain())), "vertex").size());
    assertEquals(2, cells(parse(exporter.toXml(chain())), "vertex").size());
  }

  @Test
  void acceptsCustomDiagramRecords() throws Exception {
    record Status(String id, String label) implements DiagramNode {
    }
    record Transition(String sourceId, String targetId) implements DiagramEdge {
    }
    record StatusData(String sectionName, List<Status> nodes,
        List<Transition> edges) implements DiagramData {
    }
    var data = new StatusData("DTO", List.of(new Status("a", "A"), new Status("b", "B")),
        List.of(new Transition("a", "b")));
    var xml = parse(DrawioExporter.create().toXml(data));
    assertEquals(2, cells(xml, "vertex").size());
    assertEquals("", cells(xml, "edge").get(0).getAttribute("value"));
  }

  @Test
  void snapshotsMutableCustomDataBeforeCallingLayout() throws Exception {
    String[] label =
    {"Original"
    };
    DiagramNode node = new DiagramNode() {
      public String id() {
        return "a";
      }
      public String label() {
        return label[0];
      }
    };
    DiagramData data = new DiagramData() {
      public String sectionName() {
        return "Snapshot";
      }
      public List<DiagramNode> nodes() {
        return List.of(node);
      }
      public List<DiagramEdge> edges() {
        return List.of();
      }
    };
    var exporter = DrawioExporter.builder().layout((snapshot, options) -> {
      label[0] = "Changed";
      assertEquals("Original", snapshot.nodes().get(0).label());
      return new LayoutResult(List.of(new PositionedNode("a", 10, 10, 160, 60)), List.of());
    }).build();
    assertEquals("Original",
        cells(parse(exporter.toXml(data)), "vertex").get(0).getAttribute("value"));
  }

  @Test
  void validatesCustomDataBeforeInvokingLayout() {
    DiagramData invalid = new DiagramData() {
      public String sectionName() {
        return "Invalid";
      }
      public List<DiagramNode> nodes() {
        return List.of();
      }
      public List<DiagramEdge> edges() {
        return List.of(new DefaultEdge("missing", "absent", ""));
      }
    };
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      throw new AssertionError("Invalid data reached layout engine");
    }).build();
    assertThrows(IllegalArgumentException.class, () -> exporter.toXml(invalid));
    assertThrows(IllegalArgumentException.class, () -> DrawioExporter.create().toXml(null));
  }

  @ParameterizedTest
  @ValueSource(doubles =
  {0, -1, Double.NaN, Double.POSITIVE_INFINITY
  })
  void rejectsInvalidDimensions(double size) {
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().nodeSize(size, 60).build());
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().nodeSize(160, size).build());
  }

  @Test
  void rejectsNullConfigurationAndIncompleteLayout() {
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().direction(null).build());
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().layout(null).build());
    assertBadLayout(null);
    assertBadLayout(new LayoutResult(List.of(), List.of()));
    var nodes = fixedLayout(List.of(new Point(160, 30), new Point(220, 120))).nodes();
    assertBadLayout(new LayoutResult(nodes,
        List.of(new RoutedEdge(2, List.of(new Point(160, 30), new Point(220, 120))))));
    assertBadLayout(
        new LayoutResult(nodes, List.of(new RoutedEdge(0, List.of(new Point(160, 30))))));
    assertBadLayout(new LayoutResult(nodes,
        List.of(new RoutedEdge(0, List.of(new Point(Double.NaN, 30), new Point(220, 120))))));
    assertBadLayout(fixedLayout(List.of(new Point(80, 30), new Point(220, 120))));
    assertBadLayout(
        new LayoutResult(List.of(new PositionedNode("a", Double.NaN, 0, 160, 60), nodes.get(1)),
            List.of(new RoutedEdge(0, List.of(new Point(160, 30), new Point(220, 120))))));
  }

  private static LayoutResult fixedLayout(List<Point> points) {
    return new LayoutResult(
        List.of(new PositionedNode("a", 0, 0, 160, 60), new PositionedNode("b", 220, 90, 160, 60)),
        List.of(new RoutedEdge(0, points)));
  }

  private static void assertBadLayout(LayoutResult layout) {
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().layout((data, options) -> layout).build().toXml(chain()));
  }

  private static Document parse(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static Element element(Document document, String tag, int index) {
    return (Element) document.getElementsByTagName(tag).item(index);
  }

  private static Element child(Element parent, String tag) {
    return (Element) parent.getElementsByTagName(tag).item(0);
  }

  private static double number(Element element, String attribute) {
    return Double.parseDouble(element.getAttribute(attribute));
  }

  private static List<Element> cells(Document document, String type) {
    var result = new ArrayList<Element>();
    var cells = document.getElementsByTagName("mxCell");
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      if (cell.getAttribute(type).equals("1"))
        result.add(cell);
    }
    return result;
  }
}
