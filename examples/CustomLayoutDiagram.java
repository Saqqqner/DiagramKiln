import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;

/** Demonstrates the layout SPI using fixed geometry for this workflow only. */
public final class CustomLayoutDiagram {
  public static void main(String[] args) throws Exception {
    var diagram = Diagrams.diagram("Custom workflow layout")
        .group("team", "Planning",
            group -> group.node("review", "Review").node("approve", "Approve").edge("review",
                "approve", "Internal review"))
        .node("publish", "Publish").edge("approve", "publish", "Release").build();

    // Custom engines receive a validated snapshot. This preset is specific to the graph above;
    // it intentionally chooses its own bounds instead of using the global layout preferences.
    LayoutEngine fixedWorkflow = (data, options) -> fixedGeometry();
    var result = DrawioExporter.builder().layout(fixedWorkflow).build().export(diagram);
    var output = Path.of(args.length == 0 ? "target/custom-layout.drawio" : args[0]);
    Files.createDirectories(output.toAbsolutePath().getParent());
    Files.writeString(output, result.xml(), StandardCharsets.UTF_8);
    Files.writeString(Path.of(output + ".url"),
        "https://app.diagrams.net/?splash=0" + result.drawioFragment(), StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }

  private static LayoutResult fixedGeometry() {
    return new LayoutResult(
        List.of(new PositionedNode("team", 100, 100, 520, 220),
            new PositionedNode("review", 140, 160, 160, 60),
            new PositionedNode("approve", 400, 160, 160, 60),
            new PositionedNode("publish", 720, 160, 160, 60)),
        // Root edges come first, followed recursively by each group's edges.
        List.of(new RoutedEdge(0, List.of(new Point(560, 190), new Point(720, 190))),
            new RoutedEdge(1, List.of(new Point(300, 190), new Point(400, 190)))));
  }
}
