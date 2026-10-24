package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedEdgeLabel;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class EdgeLabelExportTest {
  private final EdgeLabelSize fallback = new EdgeLabelSize(120, 30);

  @Test
  void serializesEditableLabelWithFinalRouteMidpointAndTranslationInvariantOffset()
      throws Exception {
    var diagram = nestedDiagram();
    for (double shift : new double[]
    {0, 1000
    }) {
      var exporter = DrawioExporter.builder().layout(fixedEngine(shift)).edgeLabels(fallback)
          .build();
      var result = exporter.export(diagram);
      var document = parse(result.xml());
      assertEquals(7, document.getElementsByTagName("mxCell").getLength());
      var group = cell(document, "Group");
      var edge = cell(document, "Согласование 100% +\n第二行");
      assertEquals(group.getAttribute("id"), edge.getAttribute("parent"));
      var geometry = (Element) edge.getElementsByTagName("mxGeometry").item(0);
      assertEquals("1", geometry.getAttribute("relative"));
      assertEquals(0, number(geometry, "x"));
      assertEquals(0, number(geometry, "y"));
      // The custom engine's final dimensions, not the 120x30 input hint, are authoritative.
      assertEquals(80, number(geometry, "width"));
      assertEquals(20, number(geometry, "height"));
      var offset = (Element) geometry.getElementsByTagName("mxPoint").item(0);
      assertEquals("offset", offset.getAttribute("as"));
      assertEquals(0, number(offset, "x"));
      assertEquals(-60, number(offset, "y"));
      // Hand-derived route midpoint is (320,280); desired label center is (320,220).
      assertEquals(320 + shift, 320 + shift + number(offset, "x"));
      assertEquals(220 + shift, 280 + shift + number(offset, "y"));
      var bends = (Element) geometry.getElementsByTagName("Array").item(0);
      var firstBend = (Element) bends.getElementsByTagName("mxPoint").item(0);
      assertEquals(180, number(firstBend, "x"));
      assertEquals(80, number(firstBend, "y"));
      var empty = cell(document, "");
      var emptyGeometry = (Element) empty.getElementsByTagName("mxGeometry").item(0);
      assertFalse(emptyGeometry.hasAttribute("width"));
      assertEquals(0, emptyGeometry.getElementsByTagName("mxPoint").getLength());
      assertEquals(result.xml(), inflate(result.drawioFragment()));
    }
  }

  @Test
  void builderOrderingAndReuseSelectExactlyOneOperationPerExport() {
    var legacyCalls = new AtomicInteger();
    var labeledCalls = new AtomicInteger();
    var empty = Diagrams.diagram("Empty").build();
    LayoutEngine engine = new LayoutEngine() {
      public LayoutResult layout(DiagramData data, LayoutOptions options) {
        legacyCalls.incrementAndGet();
        return new LayoutResult(List.of(), List.of());
      }
      public LabeledLayoutResult layoutWithEdgeLabels(DiagramData data, LayoutOptions options,
          EdgeLabelSize requested) {
        assertEquals(fallback, requested);
        assertEquals(Direction.DOWN, options.direction());
        assertEquals(200, options.nodeWidth());
        labeledCalls.incrementAndGet();
        return new LabeledLayoutResult(new LayoutResult(List.of(), List.of()), List.of());
      }
    };
    var builder = DrawioExporter.builder().layout(engine).edgeLabels(fallback)
        .layoutOptions(LayoutOptions.builder().build()).direction(Direction.DOWN).nodeSize(200, 90)
        .layout(engine);
    var enabled = builder.build();
    var disabled = builder.withoutEdgeLabels().build();
    enabled.export(empty);
    disabled.export(empty);
    builder.edgeLabels(fallback).build().export(empty);
    assertEquals(2, labeledCalls.get());
    assertEquals(1, legacyCalls.get());
    assertThrows(IllegalArgumentException.class, () -> builder.edgeLabels(null));
  }

  @Test
  void oldCustomEngineFailsExplicitlyOnlyWhenOptedIn() {
    var calls = new AtomicInteger();
    LayoutEngine legacy = (data, options) -> {
      calls.incrementAndGet();
      return new LayoutResult(List.of(), List.of());
    };
    var empty = Diagrams.diagram("Empty").build();
    assertThrows(UnsupportedOperationException.class,
        () -> DrawioExporter.builder().layout(legacy).edgeLabels(fallback).build().toXml(empty));
    assertEquals(0, calls.get());
    DrawioExporter.builder().layout(legacy).edgeLabels(fallback).withoutEdgeLabels().build()
        .toXml(empty);
    assertEquals(1, calls.get());
  }

  @Test
  void rejectsIncompleteCustomResultsBeforeXmlSerialization() {
    var diagram = nestedDiagram();
    var valid = fixedGeometry(0);
    for (LabeledLayoutResult invalid : new LabeledLayoutResult[]
    {null, new LabeledLayoutResult(valid.geometry(), List.of()),
        new LabeledLayoutResult(new LayoutResult(List.of(), List.of()), valid.labels())
    }) {
      LayoutEngine engine = labeledOnly(invalid);
      assertThrows(IllegalArgumentException.class, () -> DrawioExporter.builder().layout(engine)
          .edgeLabels(fallback).build().toXml(diagram));
    }
  }

  @Test
  void propagatesCustomLabeledEngineFailure() {
    var failure = new IllegalStateException("Custom layout failed");
    LayoutEngine engine = new LayoutEngine() {
      public LayoutResult layout(DiagramData data, LayoutOptions options) {
        fail("Legacy operation must not be called");
        return null;
      }
      public LabeledLayoutResult layoutWithEdgeLabels(DiagramData data, LayoutOptions options,
          EdgeLabelSize size) {
        throw failure;
      }
    };
    var exporter = DrawioExporter.builder().layout(engine).edgeLabels(fallback).build();
    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> exporter.toXml(Diagrams.diagram("Empty").build())));
  }

  @Test
  void rejectsOffsetOverflowWithEdgeContextBeforeReturningXml() {
    var data = Diagrams.diagram("Overflow").node("a", "A").node("b", "B").edge("a", "b", "Label")
        .build();
    var geometry = new LayoutResult(
        List.of(new PositionedNode("a", -1e308, 0, 1, 1), new PositionedNode("b", -1e308, 0, 1, 1)),
        List.of(new RoutedEdge(0, List.of(new Point(-1e308, 0), new Point(-1e308, 0)))));
    var result = new LabeledLayoutResult(geometry,
        List.of(new PositionedEdgeLabel(0, 1e308, 0, 1, 1)));
    var error = assertThrows(IllegalArgumentException.class, () -> DrawioExporter.builder()
        .layout(labeledOnly(result)).edgeLabels(fallback).build().toXml(data));
    assertTrue(error.getMessage().contains("edge 0"), error.getMessage());
  }

  @Test
  void defaultAndExplicitlyDisabledExportsRemainEqualAndIgnoreSizing() {
    var unsized = Diagrams.diagram("Default").node("a", "A").node("b", "B").edge("a", "b", "Text")
        .build();
    var sized = Diagrams
        .diagram("Default").node("a", "A").node("b", "B").edge("a", "b", "Text",
            io.github.saqqqner.diagramkiln.style.EdgeStyle.defaults(), new EdgeLabelSize(500, 100))
        .build();
    var expected = DrawioExporter.create().toXml(unsized);
    assertEquals(expected, DrawioExporter.create().toXml(sized));
    assertEquals(expected,
        DrawioExporter.builder().edgeLabels(fallback).withoutEdgeLabels().build().toXml(sized));
  }

  private DiagramData nestedDiagram() {
    return Diagrams.diagram("Nested").group("g", "Group", g -> g.node("a", "A").node("b", "B")
        .edge("a", "b", "Согласование 100% +\n第二行").edge("b", "a", "")).build();
  }

  private LayoutEngine fixedEngine(double shift) {
    return labeledOnly(fixedGeometry(shift));
  }

  private LayoutEngine labeledOnly(LabeledLayoutResult result) {
    return new LayoutEngine() {
      public LayoutResult layout(DiagramData data, LayoutOptions options) {
        fail("Enabled export must use labeled operation exactly once");
        return null;
      }
      public LabeledLayoutResult layoutWithEdgeLabels(DiagramData data, LayoutOptions options,
          EdgeLabelSize size) {
        return result;
      }
    };
  }

  private LabeledLayoutResult fixedGeometry(double shift) {
    var geometry = new LayoutResult(
        List.of(new PositionedNode("g", 100 + shift, 100 + shift, 500, 300),
            new PositionedNode("a", 140 + shift, 160 + shift, 100, 40),
            new PositionedNode("b", 400 + shift, 160 + shift, 100, 40)),
        List.of(
            new RoutedEdge(0,
                List.of(new Point(240 + shift, 180 + shift), new Point(280 + shift, 180 + shift),
                    new Point(280 + shift, 280 + shift), new Point(400 + shift, 280 + shift),
                    new Point(400 + shift, 180 + shift))),
            new RoutedEdge(1, List.of(new Point(400 + shift, 190 + shift),
                new Point(240 + shift, 190 + shift)))));
    return new LabeledLayoutResult(geometry,
        List.of(new PositionedEdgeLabel(0, 280 + shift, 210 + shift, 80, 20)));
  }

  private Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }

  private Element cell(Document document, String value) {
    var cells = document.getElementsByTagName("mxCell");
    for (int i = 0; i < cells.getLength(); i++) {
      var cell = (Element) cells.item(i);
      if (cell.hasAttribute("value") && cell.getAttribute("value").equals(value))
        return cell;
    }
    throw new AssertionError("Missing cell " + value);
  }

  private double number(Element element, String attribute) {
    return Double.parseDouble(element.getAttribute(attribute));
  }

  private String inflate(String fragment) throws Exception {
    byte[] bytes = Base64.getDecoder()
        .decode(URLDecoder.decode(fragment.substring(2), StandardCharsets.UTF_8));
    var inflater = new Inflater(true);
    try (var input = new InflaterInputStream(new ByteArrayInputStream(bytes), inflater)) {
      return URLDecoder.decode(new String(input.readAllBytes(), StandardCharsets.UTF_8),
          StandardCharsets.UTF_8);
    }
    finally {
      inflater.end();
    }
  }
}
