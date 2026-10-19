package io.github.saqqqner.diagramkiln;

import java.util.List;

/**
 * Diagram input contract; applications may implement it with their own DTOs.
 */
public interface DiagramData {
  /**
   * {@return non-null page title, or group title when nested}
   */
  String sectionName();

  /**
   * {@return non-null ordered direct children, including groups; no null elements}
   */
  List<? extends DiagramNode> nodes();

  /**
   * {@return non-null ordered connections; endpoints may reference any global id}
   */
  List<? extends DiagramEdge> edges();
}
