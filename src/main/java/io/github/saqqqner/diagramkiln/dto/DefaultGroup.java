package io.github.saqqqner.diagramkiln.dto;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.style.GroupStyle;

/**
 * A container whose contents are snapshotted by the DSL or exporter.
 *
 * @param id
 *          globally unique container id
 * @param label
 *          visible header text
 * @param diagram
 *          nested contents
 * @param groupStyle
 *          immutable container appearance
 */
public record DefaultGroup(String id, String label, DiagramData diagram,
    GroupStyle groupStyle) implements DiagramGroup {
  /**
   * Uses the nested diagram title and default appearance.
   *
   * @param id
   *          globally unique container id
   * @param diagram
   *          nested contents
   */
  public DefaultGroup(String id, DiagramData diagram) {
    this(id, diagram == null ? null : diagram.sectionName(), diagram, GroupStyle.defaults());
  }
}
