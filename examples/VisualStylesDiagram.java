import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** Reproducible browser fixtures: real ELK layout and controlled crossing comparisons. */
public final class VisualStylesDiagram {
  public static void main(String[] args) throws Exception {
    var blue = NodeStyle.builder().rounded(true).arcSize(12).fillColor("#DEEBFF")
        .strokeColor("#0052CC").strokeWidth(2).bold(true).build();
    var green = NodeStyle.builder().rounded(true).arcSize(45).fillColor("#E8F5E9")
        .strokeColor("#388E3C").strokeWidth(2).bold(true).build();
    var cross = EdgeStyle.builder().strokeColor("#EF6C00").strokeWidth(3).rounded(true).arcSize(24)
        .jumpStyle(JumpStyle.GAP).jumpSize(10).endArrow(Arrow.CLASSIC).endSize(12).build();
    var data = Diagrams.diagram("LayoutOptions + real ELK")
        .group("first", "Subsystem A",
            g -> g.node("a", "arcSize = 12", blue).node("b", "Approved", blue).edge("a", "b", "",
                cross))
        .group("second", "Subsystem B",
            g -> g.node("c", "arcSize = 45", green).node("d", "Finished", green).edge("c", "d", "",
                cross))
        .edge("b", "c", "Transfer", cross).edge("first", "second", "",
            EdgeStyle.builder().strokeColor("#1976D2").strokeWidth(3).endSize(12).build())
        .build();
    var options = LayoutOptions.builder().direction(Direction.DOWN).nodeSize(180, 60)
        .nodeSpacing(50).layerSpacing(90).groupPadding(30).build();
    write("visual-layout.drawio",
        DrawioExporter.builder().layoutOptions(options).build().toXml(data));

    var builder = Diagrams.diagram("Line jump comparison - fixed crossing geometry");
    var nodes = new ArrayList<PositionedNode>();
    var routes = new ArrayList<RoutedEdge>();
    for (var jump : JumpStyle.values()) {
      int i = jump.ordinal();
      String prefix = "p" + i;
      double x = 20 + i % 2 * 480, y = 20 + i / 2 * 370;
      builder.group(prefix, jump + " | jumpSize=16 | rounded bends",
          GroupStyle.builder().headerHeight(32).bold(true).build(),
          g -> g.node(prefix + "l", "arcSize=12", blue).node(prefix + "r", "endSize=2", blue)
              .node(prefix + "t", "arcSize=45", green).node(prefix + "b", "endSize=12", green)
              .edge(prefix + "l", prefix + "r", "",
                  EdgeStyle.builder().strokeColor("#1976D2").strokeWidth(3).endSize(2).build())
              .edge(prefix + "t", prefix + "b", "",
                  EdgeStyle.builder().strokeColor("#EF6C00").strokeWidth(3).jumpStyle(jump)
                      .jumpSize(16).endSize(12).build())
              .edge(prefix + "l", prefix + "b", "", EdgeStyle.builder().strokeColor("#388E3C")
                  .strokeWidth(3).rounded(true).arcSize(30).endSize(12).build()));
      nodes.add(new PositionedNode(prefix, x, y, 440, 330));
      nodes.add(new PositionedNode(prefix + "l", x + 20, y + 160, 80, 40));
      nodes.add(new PositionedNode(prefix + "r", x + 340, y + 160, 80, 40));
      nodes.add(new PositionedNode(prefix + "t", x + 180, y + 60, 80, 40));
      nodes.add(new PositionedNode(prefix + "b", x + 180, y + 260, 80, 40));
      routes.add(
          new RoutedEdge(i * 3, List.of(new Point(x + 100, y + 180), new Point(x + 340, y + 180))));
      routes.add(new RoutedEdge(i * 3 + 1,
          List.of(new Point(x + 220, y + 100), new Point(x + 220, y + 260))));
      routes.add(new RoutedEdge(i * 3 + 2,
          List.of(new Point(x + 100, y + 180), new Point(x + 140, y + 180),
              new Point(x + 140, y + 230), new Point(x + 220, y + 230),
              new Point(x + 220, y + 260))));
    }
    write("visual-jumps.drawio", DrawioExporter.builder()
        .layout((d, o) -> new LayoutResult(nodes, routes)).build().toXml(builder.build()));
  }

  private static void write(String name, String xml) throws Exception {
    Path path = Path.of("examples", name);
    Files.writeString(path, xml, StandardCharsets.UTF_8);
    System.out.println(path.toAbsolutePath());
  }
}
