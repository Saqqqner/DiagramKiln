import java.nio.file.Path;
import java.util.List;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramEdge;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.DiagramNode;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.DefaultDiagram;
import io.github.saqqqner.diagramkiln.dto.DefaultEdge;
import io.github.saqqqner.diagramkiln.dto.DefaultGroup;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** Uses only 1.0.1 APIs; compile old, run unchanged bytecode on new, compile new. */
public class HistoricalConsumer {
  record AppNode(String id, String label) implements DiagramNode {
  }
  record AppEdge(String sourceId, String targetId) implements DiagramEdge {
  }
  record AppGroup(String id, DiagramData diagram) implements DiagramGroup {
  }
  record AppDiagram(String sectionName, List<DiagramNode> nodes,
      List<DiagramEdge> edges) implements DiagramData {
  }

  static final class AppLayout implements LayoutEngine {
    public LayoutResult layout(DiagramData diagram, LayoutOptions options) {
      return new LayoutResult(List.of(new PositionedNode("custom", 10, 20, 120, 60)), List.of());
    }
  }

  public static void main(String[] args) throws Exception {
    Path actual = Path
        .of(DrawioExporter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
        .toRealPath();
    if (!actual.equals(Path.of(args[0]).toRealPath()) || !actual.toString().endsWith(".jar")) {
      throw new AssertionError("Expected current packaged JAR, loaded " + actual);
    }
    var diagram = Diagrams.diagram("Historical consumer")
        .group("left", "Left", g -> g.group("inner", "Inner", i -> i.node("a", "A")))
        .group("right", "Right", g -> g.node("b", "B", NodeStyle.builder().rounded(true).build()))
        .edge("a", "b", "cross-group").build();
    check(DrawioExporter.create().toXml(diagram).contains("mxfile"));
    check(DrawioExporter.builder().nodeSize(140, 70).build().toDrawioFragment(diagram)
        .startsWith("#R"));
    var nested = new DefaultDiagram("Nested", List.of(new DefaultNode("dto", "DTO")), List.of());
    var group = new DefaultGroup("dtoGroup", nested);
    var direct = new DefaultDiagram("Direct", List.of(group, new DefaultNode("other", "Other")),
        List.of(new DefaultEdge("dto", "other", "DTO edge")));
    check(group.id().equals("dtoGroup") && nested.nodes().size() == 1);
    check(DrawioExporter.create().toXml(direct).contains("DTO edge"));
    var custom = new AppDiagram("Custom", List.of(new AppNode("custom", "Application")), List.of());
    check(DrawioExporter.builder().layout(new AppLayout()).build().toXml(custom)
        .contains("Application"));
    var customGroup = new AppGroup("appGroup", custom);
    var application = new AppDiagram("Application DTOs",
        List.of(customGroup, new AppNode("appOther", "Other")),
        List.of(new AppEdge("custom", "appOther")));
    check(DrawioExporter.create().toXml(application).contains("Application"));
    var point = new Point(1, 2);
    var routed = new RoutedEdge(0, List.of(point, new Point(3, 4)));
    check(
        point.x() == 1 && point.y() == 2 && routed.edgeIndex() == 0 && routed.points().size() == 2);
    System.out.println("Historical bytecode linked and exported using " + actual);
  }

  private static void check(boolean condition) {
    if (!condition) {
      throw new AssertionError("Historical consumer contract failed");
    }
  }
}
