package io.github.saqqqner.diagramkiln;

import java.io.StringReader;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirtyNodeDiagramTest {
  @Test
  void exportsThirtyStyledLeavesWithGroupsCrossEdgesLoopsAndParallelEdges() throws Exception {
    var builder = Diagrams.diagram("30 nodes");
    builder.group("system", "System", all -> {
      for (int g = 0; g < 6; g++) {
        int groupNumber = g;
        all.group("g" + g, "Group " + g, group -> {
          for (int n = 0; n < 5; n++) {
            group.node("n" + groupNumber + "_" + n, "Node " + (groupNumber * 5 + n),
                NodeStyle.builder().shape(NodeShape.values()[n % 3]).rounded(true).build());
            if (n > 0)
              group.edge("n" + groupNumber + "_0", "n" + groupNumber + "_" + n, "");
          }
        });
        if (g > 0)
          all.edge("n0_4", "n" + g + "_0", "", EdgeStyle.builder().dashed(true).build());
      }
      all.edge("n0_0", "n0_1", "parallel").edge("n2_3", "n2_3", "loop").edge("g0", "g1", "");
    });
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var doc = factory.newDocumentBuilder()
        .parse(new InputSource(new StringReader(DrawioExporter.create().toXml(builder.build()))));
    var cells = doc.getElementsByTagName("mxCell");
    var ids = new HashSet<String>();
    int leaves = 0, groups = 0, edges = 0;
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      assertTrue(ids.add(cell.getAttribute("id")));
      if (cell.getAttribute("vertex").equals("1")) {
        if (cell.getAttribute("style").contains("swimlane;"))
          groups++;
        else
          leaves++;
      }
      if (cell.getAttribute("edge").equals("1"))
        edges++;
    }
    assertEquals(30, leaves);
    assertEquals(7, groups);
    assertEquals(32, edges);
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      if (cell.hasAttribute("parent"))
        assertTrue(ids.contains(cell.getAttribute("parent")));
      if (cell.hasAttribute("source"))
        assertTrue(ids.contains(cell.getAttribute("source")));
      if (cell.hasAttribute("target"))
        assertTrue(ids.contains(cell.getAttribute("target")));
    }
    var geometries = doc.getElementsByTagName("mxGeometry");
    for (int i = 0; i < geometries.getLength(); i++) {
      var geometry = (Element) geometries.item(i);
      for (String attr : new String[]
      {"x", "y", "width", "height"
      }) {
        if (geometry.hasAttribute(attr))
          assertTrue(Double.isFinite(Double.parseDouble(geometry.getAttribute(attr))));
      }
    }
  }
}
