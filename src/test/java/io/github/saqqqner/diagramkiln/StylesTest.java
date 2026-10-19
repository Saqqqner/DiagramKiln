package io.github.saqqqner.diagramkiln;

import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StylesTest {
  @Test
  void validatesExtendedStylesAndKeepsLegacyConstructors() {
    assertEquals(NodeStyle.defaults(), new NodeStyle(NodeShape.RECTANGLE, false, "#FFFFFF",
        "#000000", 1, false, "#000000", 12, false));
    assertEquals(EdgeStyle.defaults(),
        new EdgeStyle("#000000", 1, false, "#000000", 12, false, Arrow.NONE, Arrow.BLOCK));
    assertEquals(15, NodeStyle.defaults().arcSize());
    assertEquals(JumpStyle.NONE, EdgeStyle.defaults().jumpStyle());
    assertEquals(6, EdgeStyle.defaults().jumpSize());
    assertEquals(6, EdgeStyle.defaults().endSize());
    assertEquals(20, EdgeStyle.defaults().arcSize());
    for (double invalid : new double[]
    {-1, Double.NaN, Double.POSITIVE_INFINITY
    }) {
      assertThrows(IllegalArgumentException.class,
          () -> NodeStyle.builder().arcSize(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> EdgeStyle.builder().arcSize(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> EdgeStyle.builder().endSize(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> EdgeStyle.builder().jumpSize(invalid).build());
    }
    assertThrows(IllegalArgumentException.class, () -> NodeStyle.builder().arcSize(101).build());
    assertThrows(IllegalArgumentException.class, () -> EdgeStyle.builder().jumpStyle(null).build());
    assertEquals(0, NodeStyle.builder().arcSize(0).build().arcSize());
    assertEquals(0, EdgeStyle.builder().arcSize(0).endSize(0).jumpSize(0).build().endSize());
  }

  @Test
  void stylesAreImmutableAndSurviveDslSnapshots() {
    var builder = NodeStyle.builder().fillColor("#123abc").shape(NodeShape.DIAMOND);
    var first = builder.build();
    builder.fillColor("none");
    var edge = EdgeStyle.builder().dashed(true).endArrow(Arrow.OPEN).build();
    var data = Diagrams.diagram("r").node("a", "A", first).node("b", "B", first)
        .edge("a", "b", "", edge).build();
    assertEquals("#123abc", data.nodes().get(0).style().fillColor());
    assertEquals(first, data.nodes().get(1).style());
    assertEquals(edge, data.edges().get(0).style());
  }

  @Test
  void validatesValuesAndRejectsRawStyleInjection() {
    for (String color : new String[]
    {"red", "#12345", "#ffffff;html=1", "", null
    }) {
      assertThrows(IllegalArgumentException.class,
          () -> NodeStyle.builder().fillColor(color).build());
    }
    assertThrows(IllegalArgumentException.class,
        () -> NodeStyle.builder().fontColor("none").build());
    assertThrows(IllegalArgumentException.class, () -> NodeStyle.builder().shape(null).build());
    assertThrows(IllegalArgumentException.class, () -> EdgeStyle.builder().endArrow(null).build());
    for (double invalid : new double[]
    {0, -1, Double.NaN, Double.POSITIVE_INFINITY
    }) {
      assertThrows(IllegalArgumentException.class,
          () -> EdgeStyle.builder().strokeWidth(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> NodeStyle.builder().fontSize(invalid).build());
    }
    assertThrows(IllegalArgumentException.class,
        () -> GroupStyle.builder().headerHeight(-1).build());
    assertEquals(0, GroupStyle.builder().headerHeight(0).build().headerHeight());
    assertThrows(IllegalArgumentException.class,
        () -> Diagrams.diagram("r").node("a", "A", null).build());
  }
}
