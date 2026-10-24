package io.github.saqqqner.diagramkiln.internal.validation;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.dto.LabeledLayoutResult;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import java.util.HashMap;

/** Checks label coverage and absolute bounds at the labeled layout SPI boundary. */
public final class EdgeLabelGeometryValidator {
  private static final double EPSILON = 0.00001;

  private EdgeLabelGeometryValidator() {
  }

  public static void validate(DiagramData data, LabeledLayoutResult result) {
    require(result != null, "Layout engine returned null labeled result");
    GeometryValidator.validate(data, result.geometry());
    var index = new DiagramIndex(data);
    var nodes = new HashMap<String, PositionedNode>();
    result.geometry().nodes().forEach(node -> nodes.put(node.id(), node));
    var seen = new boolean[index.edges().size()];
    for (var label : result.labels()) {
      int edgeIndex = label.edgeIndex();
      require(edgeIndex >= 0 && edgeIndex < seen.length, "Unknown label edge index: " + edgeIndex);
      require(!seen[edgeIndex], "Duplicate label for edge " + edgeIndex);
      seen[edgeIndex] = true;
      var edge = index.edges().get(edgeIndex);
      require(!edge.label().isEmpty(), "Unexpected label for empty edge " + edgeIndex);
      require(
          finite(label.x(), label.y(), label.width(), label.height(), label.x() + label.width(),
              label.y() + label.height()) && label.width() > 0 && label.height() > 0,
          "Invalid label bounds for edge " + edgeIndex);
      String owner = index.edgeParentId(edge);
      if (owner != null) {
        var parent = nodes.get(owner);
        var group = (DiagramGroup) index.node(owner);
        require(
            label.x() >= parent.x() - EPSILON
                && label.y() >= parent.y() + group.groupStyle().headerHeight() - EPSILON
                && label.x() + label.width() <= parent.x() + parent.width() + EPSILON
                && label.y() + label.height() <= parent.y() + parent.height() + EPSILON,
            "Label is outside its owning group content bounds for edge " + edgeIndex);
      }
    }
    for (int i = 0; i < seen.length; i++)
      require(seen[i] == !index.edges().get(i).label().isEmpty(), "Missing label for edge " + i);
  }

  private static boolean finite(double... values) {
    for (double value : values)
      if (!Double.isFinite(value))
        return false;
    return true;
  }

  private static void require(boolean condition, String message) {
    if (!condition)
      throw new IllegalArgumentException(message);
  }
}
