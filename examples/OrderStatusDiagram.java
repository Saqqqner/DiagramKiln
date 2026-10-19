import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run from the project root; an optional argument selects the output file. */
public final class OrderStatusDiagram {
  public DiagramData describe() {
    var status = NodeStyle.builder().rounded(true).arcSize(12).fillColor("#DEEBFF")
        .strokeColor("#0052CC").strokeWidth(2).fontSize(14).bold(true).build();
    var isolated = NodeStyle.builder().rounded(true).arcSize(12).fillColor("#F4F5F7")
        .strokeColor("#C1C7D0").strokeWidth(2).fontSize(14).bold(true).build();
    var transition = EdgeStyle.builder().strokeColor("#34495E").strokeWidth(2)
        .endArrow(Arrow.CLASSIC).endSize(2).rounded(true).jumpStyle(JumpStyle.GAP).jumpSize(6)
        .build();
    return Diagrams.diagram("Статусы заявки").node("draft", "Черновик", status)
        .node("review", "На согласовании", status).node("approved", "Согласована", status)
        .node("rejected", "Отклонена", status).node("completed", "Завершена", status)
        .node("unused", "Не используется", isolated)
        .edge("draft", "review", "Отправить", transition)
        .edge("review", "approved", "Согласовать", transition)
        .edge("review", "rejected", "Отклонить", transition)
        .edge("rejected", "draft", "Доработать", transition)
        .edge("approved", "completed", "Завершить", transition).build();
  }

  public static void main(String[] args) throws Exception {
    var options = LayoutOptions.builder().direction(Direction.RIGHT).nodeSize(200, 80)
        .nodeSpacing(50).layerSpacing(100).feedbackEdges(true).preserveNodeOrder(true).build();
    var exporter = DrawioExporter.builder().layoutOptions(options).build();
    Path output = Path.of(args.length == 0 ? "examples/order-status.drawio" : args[0]);
    var result = exporter.export(new OrderStatusDiagram().describe());
    Files.writeString(output, result.xml(), StandardCharsets.UTF_8);
    Files.writeString(Path.of(output + ".url"),
        "https://app.diagrams.net/?splash=0" + result.drawioFragment(), StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }
}
