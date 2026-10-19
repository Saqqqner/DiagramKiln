import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.config.Direction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run from the project root; an optional argument selects the output file. */
public final class OrderStatusDiagram {
  public DiagramData describe() {
    return Diagrams.diagram("Статусы заказа").node("new", "Новый").node("processing", "В обработке")
        .node("completed", "Завершён").node("cancelled", "Отменён")
        .edge("new", "processing", "Принять").edge("processing", "completed", "Завершить")
        .edge("new", "cancelled", "Отменить").edge("processing", "cancelled", "Отменить").build();
  }

  public static void main(String[] args) throws Exception {
    var exporter = DrawioExporter.builder().direction(Direction.RIGHT).nodeSize(160, 60).build();
    Path output = Path.of(args.length == 0 ? "examples/order-status.drawio" : args[0]);
    Files.writeString(output, exporter.toXml(new OrderStatusDiagram().describe()),
        StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }
}
