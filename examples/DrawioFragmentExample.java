import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** Generates equivalent XML and #R URL outputs for an editable, nested draw.io document. */
public final class DrawioFragmentExample {
  public static void main(String[] args) throws Exception {
    var status = NodeStyle.builder().rounded(true).arcSize(12).fillColor("#DEEBFF")
        .strokeColor("#0052CC").strokeWidth(2).fontSize(14).bold(true).build();
    var transition = EdgeStyle.builder().rounded(true).jumpStyle(JumpStyle.GAP)
        .strokeColor("#EF6C00").strokeWidth(2).endSize(10).build();
    var data = Diagrams.diagram("DiagramKiln: XML + #R")
        .group("draft", "Подсистема A",
            g -> g.node("new", "Готовность 100% + 🚀\n! ' ( ) ~ *", status))
        .group("done", "Подсистема B",
            g -> g.node("ready", "Принято <OK> & %20\n? # / = \"", status))
        .edge("new", "ready", "Передать + проверить", transition).build();
    var exporter = DrawioExporter.builder().layoutOptions(LayoutOptions.builder()
        .direction(Direction.RIGHT).nodeSize(280, 100).groupPadding(24).layerSpacing(110).build())
        .build();
    String xml = exporter.toXml(data);
    String fragment = exporter.toDrawioFragment(data);
    Files.writeString(Path.of("examples/fragment-example.drawio"), xml, StandardCharsets.UTF_8);
    Files.writeString(Path.of("examples/fragment-example-url.txt"),
        "https://app.diagrams.net/?splash=0" + fragment, StandardCharsets.UTF_8);
    System.out.println("XML chars: " + xml.length() + "; fragment chars: " + fragment.length());
  }
}
