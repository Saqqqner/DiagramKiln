package io.github.saqqqner.diagramkiln;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.dto.DefaultGroup;
import io.github.saqqqner.diagramkiln.internal.validation.DiagramIndex;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompoundDslTest {
  @Test
  void capturesNestedCallbacksAndResolvesReferencesGlobally() {
    var retained = new AtomicReference<Diagrams.Builder>();
    var data = Diagrams.diagram("root").group("g1", "one", g -> {
      retained.set(g);
      g.node("a", "A").edge("a", "b", "cross");
    }).group("g2", "two", g -> g.group("g3", "three", h -> h.node("b", "B")))
        .edge("g1", "g2", "groups").build();
    var index = new DiagramIndex(data);
    assertEquals(List.of("g1", "a", "g2", "g3", "b"),
        index.nodes().stream().map(DiagramNode::id).toList());
    assertEquals("g3", index.parentId("b"));
    assertEquals("groups", index.edges().get(0).label());
    assertEquals("cross", index.edges().get(1).label());
    retained.get().node("later", "Later");
    assertEquals(1, ((DiagramGroup) data.nodes().get(0)).diagram().nodes().size());
  }

  @Test
  void acceptsExistingDiagramAndSnapshotsMutableNestedValuesAndStyles() {
    String[] label =
    {"Before"
    };
    NodeStyle style = NodeStyle.builder().fillColor("#123456").build();
    DiagramNode mutable = new DiagramNode() {
      public String id() {
        return "a";
      }
      public String label() {
        return label[0];
      }
      public NodeStyle style() {
        return style;
      }
    };
    var inner = Diagrams.diagram("inner").nodes(List.of(mutable)).build();
    var data = Diagrams.diagram("root")
        .group("g", inner, GroupStyle.builder().headerHeight(42).build()).build();
    label[0] = "After";
    var group = (DiagramGroup) data.nodes().get(0);
    assertEquals("Before", group.diagram().nodes().get(0).label());
    assertEquals("#123456", group.diagram().nodes().get(0).style().fillColor());
    assertEquals(42, group.groupStyle().headerHeight());
    assertEquals("inner", group.label());
  }

  @Test
  void rejectsGlobalDuplicateIdsAndMissingEndpoints() {
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram("r")
        .group("g", "G", g -> g.node("a", "A")).node("a", "Duplicate").build());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("r").group("g", "G", g -> g.node("g", "Duplicate")).build());
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram("r")
        .group("g", "G", g -> g.node("a", "A").edge("a", "missing", "")).build());
  }

  @Test
  void rejectsCyclicOwnershipWithoutStackOverflow() {
    DiagramData[] ref = new DiagramData[1];
    DiagramGroup group = new DiagramGroup() {
      public String id() {
        return "recursive";
      }
      public DiagramData diagram() {
        return ref[0];
      }
    };
    ref[0] = new DiagramData() {
      public String sectionName() {
        return "cycle";
      }
      public List<DiagramNode> nodes() {
        return List.of(group);
      }
      public List<DiagramEdge> edges() {
        return List.of();
      }
    };
    var error = assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.create().toXml(ref[0]));
    assertTrue(error.getMessage().toLowerCase().contains("cycl"));
  }

  @Test
  void acceptsEmptyGroupsAndPreservesCustomGroupLabel() {
    var inner = Diagrams.diagram("inner").build();
    var custom = new DefaultGroup("g", "Custom", inner, GroupStyle.defaults());
    var data = Diagrams.diagram("root").nodes(List.of(custom)).build();
    assertEquals("Custom", data.nodes().get(0).label());
    assertTrue(((DiagramGroup) data.nodes().get(0)).diagram().nodes().isEmpty());
  }
}
