import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** Demonstrates mixed leaf dimensions and paired XML/URL export. */
public final class MixedSizeDiagram {
  public static void main(String[] args) throws Exception {
    var operation = NodeStyle.builder().rounded(true).fillColor("#DEEBFF").build();
    var decision = NodeStyle.builder().shape(NodeShape.DIAMOND).fillColor("#FFF2CC").build();
    var storage = NodeStyle.builder().shape(NodeShape.ELLIPSE).fillColor("#D5E8D4").build();
    var transfer = EdgeStyle.builder().rounded(true).jumpStyle(JumpStyle.GAP).build();
    var data = Diagrams.diagram("Order processing % + 🚀")
        .group("sales", "Sales",
            group -> group.node("new", "New order", operation, 240, 90)
                .node("paid", "Paid?", decision, 180, 110).edge("new", "paid", "Payment"))
        .group("warehouse", "Warehouse",
            group -> group.group("dispatch", "Dispatch",
                inner -> inner.node("packed", "Packed", storage, 210, 80).node("shipped", "Shipped")
                    .edge("packed", "shipped", "Send")))
        .edge("paid", "packed", "Prepare order", transfer).edge("sales", "warehouse", "Handover")
        .build();
    var result = DrawioExporter.create().export(data);
    var output = Path.of(args.length == 0 ? "target/mixed-size.drawio" : args[0]);
    Files.writeString(output, result.xml(), StandardCharsets.UTF_8);
    Files.writeString(Path.of(output + ".url"),
        "https://app.diagrams.net/?splash=0" + result.drawioFragment(), StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }
}
