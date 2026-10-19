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
