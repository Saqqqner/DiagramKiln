package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.dto.DefaultEdge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramsTest {
  @Test
  void buildsNodesAndEdgesInInsertionOrderIncludingForwardReferences() {
    DiagramData data = Diagrams.diagram("Статусы").edge("new", "paid", "Оплата")
        .node("new", "Новый").node("paid", "Оплачен").build();
    assertEquals("Статусы", data.sectionName());
    assertEquals(List.of("new", "paid"), data.nodes().stream().map(DiagramNode::id).toList());
    assertEquals("Новый", data.nodes().get(0).label());
    assertEquals(new DefaultEdge("new", "paid", "Оплата"), data.edges().get(0));
  }

  @Test
  void acceptsCustomRecordsAndCopiesCollections() {
    record Status(String id, String label) implements DiagramNode {
    }
    record Transition(String sourceId, String targetId) implements DiagramEdge {
    }
    var nodes = new ArrayList<>(List.of(new Status("a", "A"), new Status("b", "B")));
    var edges = new ArrayList<>(List.of(new Transition("a", "b")));
    DiagramData data = Diagrams.diagram("DTO").nodes(nodes).edges(edges).build();
    nodes.clear();
    edges.clear();
    assertEquals(2, data.nodes().size());
    assertEquals("", data.edges().get(0).label());
    assertThrows(UnsupportedOperationException.class, () -> data.nodes().clear());
    assertThrows(UnsupportedOperationException.class, () -> data.edges().clear());
  }

  @Test
  void builderReuseDoesNotModifyEarlierResults() {
    var builder = Diagrams.diagram("x").node("a", "A");
    var first = builder.build();
    builder.node("b", "B");
    assertEquals(1, first.nodes().size());
    assertEquals(2, builder.build().nodes().size());
  }

  @Test
  void snapshotsMutableNodePropertiesAtBuildTime() {
    String[] label =
    {"Before"
    };
    DiagramNode node = new DiagramNode() {
      public String id() {
        return "a";
      }
      public String label() {
        return label[0];
      }
    };
    var data = Diagrams.diagram("x").nodes(List.of(node)).build();
    label[0] = "After";
    assertEquals("Before", data.nodes().get(0).label());
  }

  @Test
  void acceptsEmptyGraphAndEmptyLabels() {
    assertTrue(Diagrams.diagram("").build().nodes().isEmpty());
    assertEquals("", Diagrams.diagram("x").node("a", "").build().nodes().get(0).label());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings =
  {" ", "\t"
  })
  void rejectsInvalidNodeIds(String id) {
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram("x").node(id, "A").build());
  }

  @Test
  void rejectsDuplicateIdsAndMissingEndpoints() {
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", "A").node("a", "Another").build());
    var error = assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", "A").edge("a", "missing", "").build());
    assertTrue(error.getMessage().contains("missing"));
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", "A").edge("missing", "a", "").build());
  }

  @Test
  void rejectsNullsWithIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram(null).build());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", null).build());
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram("x").nodes(null));
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram("x").edges(null));
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").nodes(Arrays.asList((DiagramNode) null)).build());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").edges(Arrays.asList((DiagramEdge) null)).build());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", "A").edge("a", "a", null).build());
  }

  @ParameterizedTest
  @ValueSource(strings =
  {"\u0000", "\u0001", "\uFFFF", "\uD800"
  })
  void rejectsForbiddenXmlCharacters(String invalid) {
    assertThrows(IllegalArgumentException.class, () -> Diagrams.diagram(invalid).build());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("x").node("a", invalid).build());
  }
}
