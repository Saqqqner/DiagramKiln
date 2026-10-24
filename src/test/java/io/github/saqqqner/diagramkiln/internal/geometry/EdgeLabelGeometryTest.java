package io.github.saqqqner.diagramkiln.internal.geometry;

import io.github.saqqqner.diagramkiln.dto.Point;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdgeLabelGeometryTest {
  @Test
  void findsHalfLengthInsteadOfMiddleVertexOrEndpointAverage() {
    assertEquals(new Point(100, 100), EdgeLabelGeometry
        .midpoint(List.of(new Point(0, 0), new Point(100, 0), new Point(100, 300))));
  }

  @Test
  void skipsZeroLengthSegmentsAndAcceptsAnAllZeroRoute() {
    assertEquals(new Point(10, 15), EdgeLabelGeometry.midpoint(
        List.of(new Point(10, 5), new Point(10, 5), new Point(10, 25), new Point(10, 25))));
    assertEquals(new Point(10, 5),
        EdgeLabelGeometry.midpoint(List.of(new Point(10, 5), new Point(10, 5), new Point(10, 5))));
  }

  @Test
  void handlesLargeFiniteCoordinatesWithoutSquaringOverflow() {
    var midpoint = EdgeLabelGeometry
        .midpoint(List.of(new Point(1e200, 1e200), new Point(3e200, 1e200)));
    assertEquals(2e200, midpoint.x(), 1e185);
    assertEquals(1e200, midpoint.y());
  }

  @Test
  void rejectsFiniteInputWhoseDifferencesOrTotalLengthOverflow() {
    assertThrows(IllegalArgumentException.class,
        () -> EdgeLabelGeometry.midpoint(List.of(new Point(-1e308, 0), new Point(1e308, 0))));
    assertThrows(IllegalArgumentException.class, () -> EdgeLabelGeometry
        .midpoint(List.of(new Point(0, 0), new Point(1e308, 0), new Point(0, 0))));
    assertThrows(IllegalArgumentException.class,
        () -> EdgeLabelGeometry.midpoint(List.of(new Point(0, 0), new Point(1.3e308, 1.3e308))));
  }
}
