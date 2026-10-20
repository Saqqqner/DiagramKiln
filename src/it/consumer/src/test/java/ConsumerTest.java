import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.SizedDiagramNode;
import io.github.saqqqner.diagramkiln.dto.NodeSize;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.config.Alignment;
import io.github.saqqqner.diagramkiln.config.CycleBreaking;
import io.github.saqqqner.diagramkiln.config.NodePlacement;
import io.github.saqqqner.diagramkiln.config.NodeLayout;
import io.github.saqqqner.diagramkiln.config.LayerConstraint;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerTest {
  @Test
  void consumesAdvancedLayoutAndLegacyConstructors() {
    var first = NodeLayout.builder().layerConstraint(LayerConstraint.FIRST).build();
    var data = Diagrams.diagram("Advanced").node("a", "First", NodeStyle.defaults(), 180, 60, first)
        .node("b", "Last").edge("a", "b", "").build();
    var options = LayoutOptions.builder().edgeNodeSpacing(80).edgeSpacing(80)
        .edgeSpacingBetweenLayers(80).portSpacing(20)
        .layered(l -> l.cycleBreaking(CycleBreaking.DFS_NODE_ORDER)
            .nodePlacement(NodePlacement.NETWORK_SIMPLEX).alignment(Alignment.CENTER)
            .thoroughness(50))
        .build();
    assertTrue(
        DrawioExporter.builder().layoutOptions(options).build().toXml(data).contains("First"));
    assertEquals(10, new LayoutOptions(Direction.RIGHT, 160, 60).edgeSpacing());
    assertEquals(10, new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20).edgeSpacing());
    assertEquals(10, new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20, true).edgeSpacing());
    assertEquals(10,
        new LayoutOptions(Direction.RIGHT, 160, 60, 40, 80, 20, true, true).edgeSpacing());
  }

  record SizedStatus(String id, String label, NodeSize size) implements SizedDiagramNode {
  }

  @Test
  void consumesMixedSizesAndReusableResult() throws Exception {
    var dto = new SizedStatus("dto", "Application DTO", new NodeSize(240, 90));
    var data = Diagrams.diagram("New API")
        .group("g", "Group",
            g -> g.nodes(List.of(dto)).node("leaf", "Leaf", 180, 110).edge("dto", "leaf", ""))
        .node("default", "Default").edge("g", "default", "Groups").build();
    var result = DrawioExporter.create().export(data);
    assertTrue(result.xml().contains("width=\"240.0\""));
    assertTrue(result.xml().contains("height=\"110.0\""));
    assertTrue(result.drawioFragment().startsWith("#R"));
    var inflater = new Inflater(true);
    byte[] compressed = Base64.getDecoder()
        .decode(URLDecoder.decode(result.drawioFragment().substring(2), StandardCharsets.UTF_8));
    try (var input = new InflaterInputStream(new ByteArrayInputStream(compressed), inflater)) {
      String encodedXml = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
      assertEquals(result.xml(),
          URLDecoder.decode(encodedXml.replace("+", "%2B"), StandardCharsets.UTF_8));
    }
    finally {
      inflater.end();
    }
  }

  @Test
  void consumesPackagedLibraryWithTransitiveElkDependencies() throws Exception {
    var location = Path
        .of(DrawioExporter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    assertTrue(location.toString().endsWith(".jar"));
    try (var jar = new JarFile(location.toFile())) {
      assertEquals("io.github.saqqqner.diagramkiln",
          jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
      var coordinates = new Properties();
      var pomProperties = jar
          .getJarEntry("META-INF/maven/io.github.saqqqner/diagram-kiln/pom.properties");
      assertNotNull(pomProperties);
      try (var input = jar.getInputStream(pomProperties)) {
        coordinates.load(input);
      }
      assertEquals("io.github.saqqqner", coordinates.getProperty("groupId"));
      assertEquals("diagram-kiln", coordinates.getProperty("artifactId"));
      var license = jar.getJarEntry("META-INF/LICENSE");
      assertNotNull(license);
      try (var input = jar.getInputStream(license)) {
        assertTrue(
            new String(input.readAllBytes(), StandardCharsets.UTF_8).startsWith("MIT License"));
      }
    }
    var data = Diagrams.diagram("Consumer")
        .group("first", "First",
            g -> g.node("a", "A", NodeStyle.builder().shape(NodeShape.ELLIPSE).build()))
        .group("second", "Second",
            g -> g.node("b", "B", NodeStyle.builder().rounded(true).arcSize(12).build()))
        .edge("a", "b", "cross", EdgeStyle.builder().rounded(true).arcSize(24).endSize(2)
            .jumpStyle(JumpStyle.GAP).jumpSize(6).build())
        .edge("first", "second", "groups").build();
    var options = LayoutOptions.builder().direction(Direction.DOWN).nodeSize(200, 80)
        .nodeSpacing(50).layerSpacing(100).groupPadding(24).feedbackEdges(true)
        .preserveNodeOrder(true).build();
    assertTrue(options.feedbackEdges());
    assertTrue(options.preserveNodeOrder());
    String xml = DrawioExporter.builder().layoutOptions(options).build().toXml(data);
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    assertEquals("mxfile", document.getDocumentElement().getTagName());
    assertEquals(8, document.getElementsByTagName("mxCell").getLength());
    assertTrue(xml.contains("shape=ellipse;"));
    assertTrue(xml.contains("swimlane;"));
    assertTrue(xml.contains("jumpStyle=gap;jumpSize=6.0;endSize=2.0;"));
    assertTrue(xml.contains("arcSize=12.0;"));
    String fragment = DrawioExporter.builder().layoutOptions(options).build()
        .toDrawioFragment(data);
    assertTrue(fragment.startsWith("#R"));
    assertTrue(fragment.length() < xml.length());
  }
}
