import java.io.StringReader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerTest {
  @Test
  void consumesPackagedLibraryWithTransitiveElkDependencies() throws Exception {
    var location = Path
        .of(DrawioExporter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    assertTrue(location.toString().endsWith(".jar"));
    try (var jar = new JarFile(location.toFile())) {
      assertEquals("io.github.saqqqner.diagramkiln",
          jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
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
        .nodeSpacing(50).layerSpacing(100).groupPadding(24).build();
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
