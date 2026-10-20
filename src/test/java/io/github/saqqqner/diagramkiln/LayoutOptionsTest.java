package io.github.saqqqner.diagramkiln;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LayoutOptionsTest {
  @Test
  void capturesImmutableOptionsAndAppliesLastSetting() {
    var builder = LayoutOptions.builder().direction(Direction.DOWN).nodeSize(200, 80)
        .nodeSpacing(25).layerSpacing(70).groupPadding(18);
    var options = builder.build();
    builder.nodeSpacing(99);
    assertEquals(25, options.nodeSpacing());
    assertEquals(new LayoutOptions(Direction.RIGHT, 160, 60), LayoutOptions.builder().build());
    var observed = new AtomicReference<LayoutOptions>();
    DrawioExporter.builder().layoutOptions(options).direction(Direction.LEFT).nodeSize(220, 90)
        .layout((data, settings) -> {
          observed.set(settings);
          return new LayoutResult(List.of(), List.of());
        }).build().toXml(Diagrams.diagram("empty").build());
    assertEquals(Direction.LEFT, observed.get().direction());
    assertEquals(220, observed.get().nodeWidth());
    assertEquals(25, observed.get().nodeSpacing());
    assertEquals(70, observed.get().layerSpacing());
    assertEquals(18, observed.get().groupPadding());
  }

  @Test
  void replacesEveryOptionAndPreservesOverridesAcrossBuilderReuse() {
    var supplied = LayoutOptions.builder().direction(Direction.DOWN).nodeSize(201, 81)
        .nodeSpacing(21).layerSpacing(71).groupPadding(19).feedbackEdges(true)
        .preserveNodeOrder(true).edgeNodeSpacing(31).edgeSpacing(32).edgeSpacingBetweenLayers(33)
        .portSpacing(34).layered(l -> l.thoroughness(42)).build();
    var seen = new AtomicReference<LayoutOptions>();
    var builder = DrawioExporter.builder().direction(Direction.UP).nodeSize(1, 2)
        .layout((data, options) -> {
          seen.set(options);
          return new LayoutResult(List.of(), List.of());
        }).layoutOptions(supplied);
    var captured = builder.build();
    var overridden = builder.direction(Direction.LEFT).nodeSize(220, 90).build();
    var empty = Diagrams.diagram("Empty").build();
    captured.toXml(empty);
    assertEquals(supplied, seen.get());
    overridden.toXml(empty);
    var expected = new LayoutOptions(Direction.LEFT, 220, 90, 21, 71, 19, true, true, 31, 32, 33,
        34, supplied.layered());
    assertEquals(expected, seen.get());
    builder.layoutOptions(LayoutOptions.builder().build()).build().toXml(empty);
    assertEquals(LayoutOptions.builder().build(), seen.get());
    overridden.toXml(empty);
    assertEquals(expected, seen.get());
  }

  @Test
  void replacementCanRecoverDeferredInvalidOverrides() {
    var options = LayoutOptions.builder().build();
    DrawioExporter.builder().direction(null).nodeSize(Double.NaN, -1).layoutOptions(options).build()
        .toXml(Diagrams.diagram("Empty").build());
    var builder = DrawioExporter.builder().layoutOptions(options).direction(null);
    assertThrows(IllegalArgumentException.class, builder::build);
    builder.direction(Direction.RIGHT).nodeSize(-1, 60);
    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void rejectsInvalidLayoutSpacing() {
    for (double invalid : new double[]
    {-1, Double.NaN, Double.POSITIVE_INFINITY
    }) {
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().nodeSpacing(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().layerSpacing(invalid).build());
      assertThrows(IllegalArgumentException.class,
          () -> LayoutOptions.builder().groupPadding(invalid).build());
    }
    assertEquals(0, LayoutOptions.builder().nodeSpacing(0).layerSpacing(0).groupPadding(0).build()
        .groupPadding());
    assertThrows(IllegalArgumentException.class,
        () -> DrawioExporter.builder().layoutOptions(null));
  }
}
