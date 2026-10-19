package io.github.saqqqner.diagramkiln;

import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompoundExportTest {
  @Test
  void exportsExtendedStylesWhilePreservingElkRoutes() throws Exception {
    var node = NodeStyle.builder().rounded(true).arcSize(12).build();
    for (var jump : JumpStyle.values()) {
      var edge = EdgeStyle.builder().jumpStyle(jump).jumpSize(8).endSize(2).rounded(true)
          .arcSize(24).build();
      var data = Diagrams.diagram("r").node("a", "A", node).node("b", "B", node)
          .edge("a", "b", "main").edge("a", "a", "styled", edge).build();
      var plain = Diagrams.diagram("r").node("a", "A", node).node("b", "B", node)
          .edge("a", "b", "main").edge("a", "a", "styled").build();
      var xml = parse(DrawioExporter.create().toXml(data));
      assertStyle(byLabel(xml, "A"), "arcSize=12.0;", "absoluteArcSize=0;");
      assertStyle(byLabel(xml, "styled"),
          "jumpStyle=" + jump.name().toLowerCase(java.util.Locale.ROOT) + ";", "jumpSize=8.0;",
          "endSize=2.0;", "rounded=1;", "arcSize=24.0;", "noEdgeStyle=1;");
      var geometry = child(byLabel(xml, "styled"), "mxGeometry");
      assertTrue(geometry.getElementsByTagName("mxPoint").getLength() > 0);
      assertTrue(child(byLabel(parse(DrawioExporter.create().toXml(plain)), "styled"), "mxGeometry")
          .isEqualNode(geometry));
    }
  }

  @Test
  void rejectsGroupBoundsSmallerThanItsHeader() {
    var data = Diagrams.diagram("r").group("g", "G", g -> {
    }).build();
    var geometry = new LayoutResult(List.of(new PositionedNode("g", 0, 0, 160, 10)), List.of());
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().layout((d, o) -> geometry).build().toXml(data));
  }
  @Test
  void writesParentRelativeChildAndLocalRouteGeometry() throws Exception {
    var data = Diagrams.diagram("r")
        .group("g", "G", g -> g.node("a", "A").node("b", "B").edge("a", "b", "local"))
        .node("c", "C").edge("a", "c", "cross").build();
    var geometry = new LayoutResult(
        List.of(new PositionedNode("g", 100, 200, 400, 400),
            new PositionedNode("a", 140, 260, 160, 60), new PositionedNode("b", 340, 360, 160, 60),
            new PositionedNode("c", 600, 260, 160, 60)),
        List.of(new RoutedEdge(0, List.of(new Point(300, 290), new Point(600, 290))),
            new RoutedEdge(1, List.of(new Point(300, 290), new Point(320, 290), new Point(320, 390),
                new Point(340, 390)))));
    var xml = parse(DrawioExporter.builder().layout((d, o) -> geometry).build().toXml(data));
    var group = byLabel(xml, "G");
    var a = byLabel(xml, "A");
    var b = byLabel(xml, "B");
    assertEquals(group.getAttribute("id"), a.getAttribute("parent"));
    assertEquals(group.getAttribute("id"), b.getAttribute("parent"));
    assertEquals("40.0", child(a, "mxGeometry").getAttribute("x"));
    assertEquals("60.0", child(a, "mxGeometry").getAttribute("y"));
    var local = byLabel(xml, "local");
    assertEquals(group.getAttribute("id"), local.getAttribute("parent"));
    var points = local.getElementsByTagName("mxPoint");
    assertEquals("220.0", ((Element) points.item(0)).getAttribute("x"));
    assertEquals("90.0", ((Element) points.item(0)).getAttribute("y"));
    assertEquals("190.0", ((Element) points.item(1)).getAttribute("y"));
    assertEquals("1", byLabel(xml, "cross").getAttribute("parent"));
    assertEquals(a.getAttribute("id"), local.getAttribute("source"));
  }

  @Test
  void serializesTypedAppearanceWithoutOverridingRoutingOrTextMode() throws Exception {
    var node = NodeStyle.builder().shape(NodeShape.ELLIPSE).fillColor("#DDEEFF")
        .strokeColor("#123456").strokeWidth(2).dashed(true).fontColor("#112233").fontSize(16)
        .bold(true).rounded(true).build();
    var group = GroupStyle.builder().fillColor("#ABCDEF").strokeColor("#654321").strokeWidth(3)
        .dashed(true).fontColor("#445566").fontSize(18).bold(true).headerHeight(42).build();
    var edge = EdgeStyle.builder().strokeColor("#FF8800").strokeWidth(4).dashed(true)
        .fontColor("#778899").fontSize(14).bold(true).startArrow(Arrow.DIAMOND).endArrow(Arrow.OPEN)
        .build();
    var data = Diagrams.diagram("r")
        .group("g", "Styled group", group,
            g -> g.node("a", "A", node).node("b", "B", node).edge("a", "b", "transfer", edge))
        .build();
    var xml = parse(DrawioExporter.create().toXml(data));
    assertStyle(byLabel(xml, "A"), "shape=ellipse;", "fillColor=#DDEEFF;", "strokeColor=#123456;",
        "strokeWidth=2.0;", "dashed=1;", "fontColor=#112233;", "fontSize=16.0;", "fontStyle=1;",
        "html=0;");
    assertStyle(byLabel(xml, "Styled group"), "swimlane;", "startSize=42.0;", "fillColor=#ABCDEF;",
        "strokeColor=#654321;", "strokeWidth=3.0;", "fontSize=18.0;", "fontColor=#445566;",
        "fontStyle=1;");
    assertStyle(byLabel(xml, "transfer"), "strokeColor=#FF8800;", "strokeWidth=4.0;", "dashed=1;",
        "fontColor=#778899;", "fontSize=14.0;", "fontStyle=1;", "startArrow=diamond;",
        "endArrow=open;", "noEdgeStyle=1;");
  }

  @Test
  void exportsDeepGroupsCrossEdgesAndContainerConnections() throws Exception {
    var data = Diagrams.diagram("r")
        .group("outer", "Outer", g -> g.group("inner", "Inner", h -> h.node("a", "A")))
        .group("other", "Other", g -> g.node("b", "B")).edge("a", "b", "cross")
        .edge("outer", "other", "containers").edge("outer", "a", "descendant").build();
    var xml = parse(DrawioExporter.create().toXml(data));
    assertEquals(byLabel(xml, "Outer").getAttribute("id"),
        byLabel(xml, "Inner").getAttribute("parent"));
    assertEquals(byLabel(xml, "Inner").getAttribute("id"),
        byLabel(xml, "A").getAttribute("parent"));
    assertEquals(byLabel(xml, "A").getAttribute("id"),
        byLabel(xml, "cross").getAttribute("source"));
    assertEquals(byLabel(xml, "B").getAttribute("id"),
        byLabel(xml, "cross").getAttribute("target"));
  }

  private static void assertStyle(Element cell, String... fragments) {
    for (String fragment : fragments)
      assertTrue(cell.getAttribute("style").contains(fragment), fragment);
  }

  private static Document parse(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static Element byLabel(Document doc, String label) {
    var cells = doc.getElementsByTagName("mxCell");
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      if (cell.getAttribute("value").equals(label))
        return cell;
    }
    throw new AssertionError("Missing cell: " + label);
  }

  private static Element child(Element parent, String tag) {
    return (Element) parent.getElementsByTagName(tag).item(0);
  }
}
