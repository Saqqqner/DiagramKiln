package io.github.saqqqner.diagramkiln;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.internal.xml.DrawioXmlWriter;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawioFragmentTest {
  @Test
  void roundTripsNestedUnicodeStylesAndLiteralPercentThroughDrawioCodec() throws Exception {
    var data = Diagrams.diagram("Статусы 100% + & 🚀")
        .group("g", "Group", g -> g.node("a", "100% + / = & < > \" ' ! ~ ( ) *\nСтатус 🚀"))
        .node("b", "Done").edge("a", "b", "%20 + transfer",
            EdgeStyle.builder().rounded(true).jumpStyle(JumpStyle.GAP).build())
        .build();
    var exporter = DrawioExporter.create();
    String fragment = exporter.toDrawioFragment(data);
    assertTrue(fragment.startsWith("#R"));
    assertFalse(fragment.contains("+"));
    assertFalse(fragment.contains("/"));
    String decoded = decodeFragment(fragment);
    assertEquals(exporter.toXml(data), decoded);
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(decoded)));
    assertEquals("mxfile", document.getDocumentElement().getTagName());
    assertEquals(data.sectionName(), document.getElementsByTagName("diagram").item(0)
        .getAttributes().getNamedItem("name").getNodeValue());
    assertTrue(decoded.contains("jumpStyle=gap;"));
  }

  @Test
  void writerSupportsBothOutputsWithoutChangingGeometry() throws Exception {
    var data = Diagrams.diagram("A").node("a", "100%").node("b", "B").edge("a", "b", "").build();
    var layout = new LayoutResult(
        List.of(new PositionedNode("a", 0, 0, 160, 60), new PositionedNode("b", 240, 0, 160, 60)),
        List.of(new RoutedEdge(0, List.of(new Point(160, 30), new Point(240, 30)))));
    var writer = new DrawioXmlWriter();
    assertEquals(writer.write(data, layout), decodeFragment(writer.writeFragment(data, layout)));
    assertThrows(IllegalArgumentException.class,
        () -> writer.writeFragment(data, new LayoutResult(List.of(), List.of())));
  }

  @Test
  void handlesEmptyAndLargeGraphsAndRunsLayoutOncePerExport() throws Exception {
    var calls = new AtomicInteger();
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      calls.incrementAndGet();
      return new LayoutResult(List.of(), List.of());
    }).build();
    String fragment = exporter.toDrawioFragment(Diagrams.diagram("empty").build());
    assertEquals(1, calls.get());
    assertTrue(decodeFragment(fragment).contains("mxGraphModel"));
    var builder = Diagrams.diagram("Large");
    for (int i = 0; i < 120; i++)
      builder.node("n" + i, "Node " + i + " % + 🚀 " + "text".repeat(40));
    var data = builder.build();
    var realExporter = DrawioExporter.create();
    var xml = realExporter.toXml(data);
    var compressed = realExporter.toDrawioFragment(data);
    assertTrue(compressed.length() < xml.length());
    assertEquals(xml, decodeFragment(compressed));
    assertThrows(IllegalArgumentException.class, () -> realExporter.toDrawioFragment(null));
  }

  private static String decodeFragment(String fragment) throws Exception {
    byte[] compressed = Base64.getDecoder()
        .decode(URLDecoder.decode(fragment.substring(2), StandardCharsets.UTF_8));
    var inflater = new Inflater(true);
    try {
      inflater.setInput(compressed);
      var output = new ByteArrayOutputStream();
      var buffer = new byte[257];
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        assertTrue(count > 0 || inflater.finished(), "Incomplete raw DEFLATE stream");
        output.write(buffer, 0, count);
      }
      assertEquals(0, inflater.getRemaining());
      String encodedXml = output.toString(StandardCharsets.US_ASCII);
      assertTrue(encodedXml.startsWith("%3C"), "XML must be URI-encoded before compression");
      // URLDecoder handles form data: protect literal plus to emulate decodeURIComponent.
      return URLDecoder.decode(encodedXml.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
    finally {
      inflater.end();
    }
  }
}
