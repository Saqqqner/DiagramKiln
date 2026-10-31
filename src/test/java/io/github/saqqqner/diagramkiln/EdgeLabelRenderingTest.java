package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedEdgeLabel;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class EdgeLabelRenderingTest {
  @Test
  void avoidsDrawioRelativeAnchorRoundingForFractionalAndDegenerateRoutes() throws Exception {
    var data = Diagrams.diagram("Rendering").group("g", "Group",
        group -> group.node("a", "A").node("b", "B").edge("a", "b", "Editable")).build();
    // Odd lengths, a subpixel initial bend, repeated points, and a zero-length route.
    for (var route : List.of(List.of(new Point(160, 180), new Point(261, 180)),
        List.of(new Point(160, 180), new Point(160, 180.1), new Point(160.2, 180.1),
            new Point(261, 180.1)),
        List.of(new Point(160, 180), new Point(160, 180), new Point(261, 180)),
        List.of(new Point(160, 180), new Point(160, 180)))) {
      for (double shift : new double[]
      {0, 1000
      }) {
        var points = route.stream().map(p -> new Point(p.x() + shift, p.y() + shift)).toList();
        var geometry = new LayoutResult(
            List.of(new PositionedNode("g", 100 + shift, 100 + shift, 500, 300),
                new PositionedNode("a", 160 + shift, 160 + shift, 100, 40),
                new PositionedNode("b", 160 + shift, 160 + shift, 101, 40)),
            List.of(new RoutedEdge(0, points)));
        var result = new LabeledLayoutResult(geometry,
            List.of(new PositionedEdgeLabel(0, 280 + shift, 210 + shift, 80, 20)));
        LayoutEngine engine = new LayoutEngine() {
          public LayoutResult layout(DiagramData diagram, LayoutOptions options) {
            fail("Labeled operation required");
            return null;
          }
          public LabeledLayoutResult layoutWithEdgeLabels(DiagramData diagram,
              LayoutOptions options, EdgeLabelSize size) {
            return result;
          }
        };
        var xml = DrawioExporter.builder().layout(engine).edgeLabels(new EdgeLabelSize(80, 20))
            .build().toXml(data);
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
        var cells = document.getElementsByTagName("mxCell");
        int inspectedEdges = 0;
        for (int i = 0; i < cells.getLength(); i++) {
          var cell = (Element) cells.item(i);
          if (!cell.getAttribute("edge").equals("1"))
            continue;
          inspectedEdges++;
          assertEquals("Editable", cell.getAttribute("value"));
          var box = (Element) cell.getElementsByTagName("mxGeometry").item(0);
          // draw.io's absolute mode avoids Math.round(relativeDistance) in getPoint.
          assertEquals("0", box.getAttribute("relative"));
          var offset = (Element) box.getElementsByTagName("mxPoint").item(0);
          assertEquals("offset", offset.getAttribute("as"));
          var start = points.get(0);
          var end = points.get(points.size() - 1);
          for (double scale : new double[]
          {0.01, 0.1, 1, 2
          }) {
            // mxGraphView.updateEdgeLabelOffset uses the two endpoint average in this mode.
            double renderedX = (start.x() + (end.x() - start.x()) / 2
                + Double.parseDouble(offset.getAttribute("x"))) * scale;
            double renderedY = (start.y() + (end.y() - start.y()) / 2
                + Double.parseDouble(offset.getAttribute("y"))) * scale;
            assertEquals((320 + shift) * scale, renderedX, 1e-9);
            assertEquals((220 + shift) * scale, renderedY, 1e-9);
          }
        }
        assertEquals(1, inspectedEdges);
      }
    }
  }
}
