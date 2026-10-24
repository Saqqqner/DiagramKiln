package io.github.saqqqner.diagramkiln.dto;

import java.util.List;

/**
 * Immutable companion to layout geometry with one box for every nonempty edge label.
 *
 * @param geometry
 *          all node bounds and edge routes in page coordinates
 * @param labels
 *          label boxes keyed by traversal edge index; order is arbitrary
 */
public record LabeledLayoutResult(LayoutResult geometry, List<PositionedEdgeLabel> labels) {
  /**
   * Copies the label list; full coverage and bounds are checked by export.
   *
   * @param geometry
   *          non-null node bounds and edge routes
   * @param labels
   *          non-null list without null elements
   * @throws IllegalArgumentException
   *           if geometry, labels or any label is null
   */
  public LabeledLayoutResult {
    if (geometry == null)
      throw new IllegalArgumentException("labeled layout geometry must not be null");
    if (labels == null)
      throw new IllegalArgumentException("layout labels must not be null");
    if (labels.stream().anyMatch(label -> label == null))
      throw new IllegalArgumentException("layout labels must not contain null");
    labels = List.copyOf(labels);
  }
}
