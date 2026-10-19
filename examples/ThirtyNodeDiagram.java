import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** A repeatable load example with 30 leaves, seven containers and 32 connections. */
public final class ThirtyNodeDiagram {
  public static DiagramData describe() {
    String[] fills =
    {"#E3F2FD", "#FFF3E0", "#F3E5F5"
    };
    var cross = EdgeStyle.builder().strokeColor("#EF6C00").dashed(true).build();
    return Diagrams.diagram("30 узлов: проверка библиотеки")
        .group("system", "Система: 30 узлов", all -> {
          for (int g = 0; g < 6; g++) {
            int groupNumber = g;
            all.group("g" + g, "Подсистема " + (g + 1),
                GroupStyle.builder().fillColor(fills[g % 3]).bold(true).build(), group -> {
                  for (int n = 0; n < 5; n++) {
                    group.node("n" + groupNumber + "_" + n, "Объект " + (groupNumber * 5 + n + 1),
                        NodeStyle.builder().shape(NodeShape.values()[n % 3]).rounded(true)
                            .fillColor(fills[groupNumber % 3]).build());
                    if (n > 0)
                      group.edge("n" + groupNumber + "_0", "n" + groupNumber + "_" + n, "");
                  }
                });
            if (g > 0)
              all.edge("n0_4", "n" + g + "_0", "", cross);
          }
          all.edge("n0_0", "n0_1", "parallel").edge("n2_3", "n2_3", "loop").edge("g0", "g1", "");
        }).build();
  }

  public static void main(String[] args) throws Exception {
    var data = describe();
    var exporter = DrawioExporter.create();
    long start = System.nanoTime();
    String xml = exporter.toXml(data);
    double firstMs = (System.nanoTime() - start) / 1_000_000.0;
    double[] warmMs = new double[5];
    for (int i = 0; i < warmMs.length; i++) {
      start = System.nanoTime();
      exporter.toXml(data);
      warmMs[i] = (System.nanoTime() - start) / 1_000_000.0;
    }
    Arrays.sort(warmMs);
    Path output = Path.of(args.length == 0 ? "examples/thirty-nodes.drawio" : args[0]);
    Files.writeString(output, xml, StandardCharsets.UTF_8);
    System.out.printf(java.util.Locale.ROOT,
        "30 leaves, 7 groups, 32 edges; first export %.1f ms; warm median %.1f ms; XML %d bytes%n",
        firstMs, warmMs[2], xml.getBytes(StandardCharsets.UTF_8).length);
    System.out.println(output.toAbsolutePath());
  }
}
