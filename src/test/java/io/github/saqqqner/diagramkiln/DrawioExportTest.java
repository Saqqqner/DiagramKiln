package io.github.saqqqner.diagramkiln;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawioExportTest {
  @Test
  void producesMatchingRepresentationsFromOneNondeterministicLayout() throws Exception {
    var calls = new AtomicInteger();
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      int offset = calls.incrementAndGet() * 20;
      return new LayoutResult(List.of(new PositionedNode("a", offset, 0, 160, 60)), List.of());
    }).build();
    var data = Diagrams.diagram("Page").node("a", "A").build();
    var first = exporter.export(data);
    assertEquals(1, calls.get());
    assertEquals(first.xml(), decode(first.drawioFragment()));
    assertEquals(first.xml(), decode(first.drawioFragment()));
    assertEquals(1, calls.get());
    var second = exporter.export(data);
    assertEquals(2, calls.get());
    assertNotEquals(first.xml(), second.xml());
    assertEquals(first.xml(), decode(first.drawioFragment()));
    assertEquals(second.xml(), decode(second.drawioFragment()));
    String separateXml = exporter.toXml(data);
    String separateFragment = exporter.toDrawioFragment(data);
    assertEquals(4, calls.get());
    assertNotEquals(separateXml, decode(separateFragment));
  }

  @Test
  void snapshotsMutableInputsBeforeCallingTheEngine() throws Exception {
    var title = new String[]
    {"Captured"
    };
    var nodes = new ArrayList<DiagramNode>(List.of(new DefaultNode("a", "Original")));
    DiagramData input = new DiagramData() {
      public String sectionName() {
        return title[0];
      }
      public List<DiagramNode> nodes() {
        return nodes;
      }
      public List<DiagramEdge> edges() {
        return List.of();
      }
    };
    var exporter = DrawioExporter.builder().layout((snapshot, options) -> {
      title[0] = "Changed";
      nodes.clear();
      assertEquals("Captured", snapshot.sectionName());
      assertEquals("Original", snapshot.nodes().get(0).label());
      return new LayoutResult(List.of(new PositionedNode("a", 0, 0, 160, 60)), List.of());
    }).build();
    var result = exporter.export(input);
    assertTrue(result.xml().contains("Captured"));
    assertTrue(result.xml().contains("Original"));
    assertEquals(result.xml(), decode(result.drawioFragment()));
    assertTrue(nodes.isEmpty());
  }

  @Test
  void rejectsInvalidInputBeforeLayoutAndPropagatesEngineErrors() {
    var calls = new AtomicInteger();
    var exporter = DrawioExporter.builder().layout((data, options) -> {
      calls.incrementAndGet();
      return null;
    }).build();
    assertThrows(IllegalArgumentException.class, () -> exporter.export(null));
    assertEquals(0, calls.get());
    var data = Diagrams.diagram("Page").node("a", "A").build();
    assertThrows(IllegalArgumentException.class, () -> exporter.export(data));
    assertEquals(1, calls.get());
    var incomplete = DrawioExporter.builder()
        .layout((d, o) -> new LayoutResult(List.of(), List.of())).build();
    assertThrows(IllegalArgumentException.class, () -> incomplete.export(data));
    var invalid = DrawioExporter.builder()
        .layout(
            (d, o) -> new LayoutResult(List.of(new PositionedNode("a", 0, 0, -1, 60)), List.of()))
        .build();
    assertThrows(IllegalArgumentException.class, () -> invalid.export(data));
    var failure = new UnsupportedOperationException("custom failure");
    var failing = DrawioExporter.builder().layout((d, o) -> {
      throw failure;
    }).build();
    assertEquals(failure,
        assertThrows(UnsupportedOperationException.class, () -> failing.export(data)));
  }

  @Test
  void resultConstructorValidatesNullsWithoutClaimingPairValidation() {
    assertThrows(IllegalArgumentException.class, () -> new DrawioExport(null, "fragment"));
    assertThrows(IllegalArgumentException.class, () -> new DrawioExport("xml", null));
    var result = new DrawioExport("arbitrary XML", "arbitrary fragment");
    assertEquals("arbitrary XML", result.xml());
    assertEquals("arbitrary fragment", result.drawioFragment());
  }

  @Test
  void safelySharesCompletedStringsBetweenThreads() throws Exception {
    var calls = new AtomicInteger();
    var result = DrawioExporter.builder().layout((data, options) -> {
      calls.incrementAndGet();
      return new LayoutResult(List.of(), List.of());
    }).build().export(Diagrams.diagram("Empty").build());
    var executor = Executors.newFixedThreadPool(4);
    try {
      var readers = new ArrayList<Callable<String>>();
      for (int i = 0; i < 32; i++)
        readers.add(() -> result.xml() + result.drawioFragment());
      for (var future : executor.invokeAll(readers))
        assertEquals(result.xml() + result.drawioFragment(), future.get());
    }
    finally {
      executor.shutdownNow();
    }
    assertEquals(1, calls.get());
  }

  @Test
  void roundTripsNestedUnicodeAndGroupConnections() throws Exception {
    var data = Diagrams.diagram("Unicode %20 + & 🚀")
        .group("g1", "First", g -> g.node("a", "A < > & \" ' ! ~ () *\nСтатус"))
        .group("g2", "Second", g -> g.group("inner", "Inner", h -> h.node("b", "B")))
        .edge("a", "b", "Cross + %20").edge("g1", "g2", "Groups").build();
    var result = DrawioExporter.create().export(data);
    assertEquals(result.xml(), decode(result.drawioFragment()));
    assertTrue(result.xml().contains("swimlane;"));
    assertTrue(result.xml().contains("parent=\"node-"));
    assertTrue(result.xml().contains("&amp;"));
  }

  @Test
  void supportsEmptyAndLargerDiagrams() throws Exception {
    var exporter = DrawioExporter.create();
    var empty = exporter.export(Diagrams.diagram("").build());
    assertEquals(empty.xml(), decode(empty.drawioFragment()));
    var builder = Diagrams.diagram("Large");
    for (int i = 0; i < 120; i++)
      builder.node("n" + i, "Node % + 🚀 " + i);
    var result = exporter.export(builder.build());
    assertEquals(result.xml(), decode(result.drawioFragment()));
    assertTrue(result.drawioFragment().length() < result.xml().length());
  }

  private String decode(String fragment) throws Exception {
    var inflater = new Inflater(true);
    try {
      inflater.setInput(Base64.getDecoder()
          .decode(URLDecoder.decode(fragment.substring(2), StandardCharsets.UTF_8)));
      var output = new ByteArrayOutputStream();
      var buffer = new byte[4096];
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        assertTrue(count > 0 || inflater.finished(), "Incomplete DEFLATE stream");
        output.write(buffer, 0, count);
      }
      assertEquals(0, inflater.getRemaining());
      return URLDecoder.decode(output.toString(StandardCharsets.US_ASCII).replace("+", "%2B"),
          StandardCharsets.UTF_8);
    }
    finally {
      inflater.end();
    }
  }
}
