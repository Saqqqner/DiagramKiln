import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;

/** Demonstrates opt-in label space, nested ownership and explicit plain-text line breaks. */
public final class EdgeLabelDiagram {
  public static void main(String[] args) throws Exception {
    var labelStyle = EdgeStyle.builder().fontSize(16).build();
    var diagram = Diagrams.diagram("Synthetic release workflow")
        .group("review", "Review",
            group -> group.node("draft", "Draft").node("approved", "Approved")
                .edge("draft", "approved", "Проверка требований\nReview requirements", labelStyle,
                    new EdgeLabelSize(260, 60))
                .edge("draft", "draft", "Revise"))
        .node("published", "Published")
        .edge("approved", "published", "Release", labelStyle, new EdgeLabelSize(180, 40))
        .edge("approved", "published", "Release").build();
    // Reserve enough space for the actual font and text. Width does not wrap words automatically.
    var result = DrawioExporter.builder().edgeLabels(new EdgeLabelSize(160, 32)).build()
        .export(diagram);
    var output = Path.of(args.length == 0 ? "target/edge-labels.drawio" : args[0]);
    Files.createDirectories(output.toAbsolutePath().getParent());
    Files.writeString(output, result.xml(), StandardCharsets.UTF_8);
    Files.writeString(Path.of(output + ".url"),
        "https://app.diagrams.net/?splash=0" + result.drawioFragment(), StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }
}
