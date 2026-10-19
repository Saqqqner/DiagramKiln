import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

/** Styled, nested example with node-to-node and group-to-group connections. */
public final class NestedCraftingDiagram {
  public DiagramData describe() {
    var phase = NodeStyle.builder().fillColor("#E3F2FD").strokeColor("#1565C0").rounded(true)
        .bold(true).build();
    var material = NodeStyle.builder().shape(NodeShape.ELLIPSE).fillColor("#FFF3E0")
        .strokeColor("#E65100").build();
    var transfer = EdgeStyle.builder().strokeColor("#EF6C00").dashed(true).endArrow(Arrow.OPEN)
        .build();
    var section = GroupStyle.builder().fillColor("#E8F5E9").strokeColor("#388E3C").bold(true)
        .headerHeight(36).build();
    return Diagrams.diagram("Крафт железной кирки")
        .group("crafting", "Крафт железной кирки", root -> root
            .group("mining", "1. Добыча железной руды", section,
                g -> g.node("drilling", "Бурение", phase).node("ore", "Железная руда", material)
                    .edge("drilling", "ore", "Добыть"))
            .group("smelting", "2. Переплавка железной руды", section,
                g -> g.node("furnace", "Загрузить печь", phase)
                    .group("output", "Результат", h -> h.node("ingot", "Железный слиток", material))
                    .edge("furnace", "ingot", "Переплавить"))
            .edge("ore", "furnace", "Передать руду", transfer).edge("mining", "smelting", "",
                EdgeStyle.builder().strokeColor("#1976D2").strokeWidth(2).build()))
        .build();
  }

  public static void main(String[] args) throws Exception {
    Path output = Path.of(args.length == 0 ? "examples/nested-crafting.drawio" : args[0]);
    Files.writeString(output, DrawioExporter.create().toXml(new NestedCraftingDiagram().describe()),
        StandardCharsets.UTF_8);
    System.out.println(output.toAbsolutePath());
  }
}
