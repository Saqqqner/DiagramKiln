package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.style.NodeStyle;

/**
 * An element of a diagram tree. Ids must be nonblank and unique across all nodes and groups.
 */
public interface DiagramNode {
  /**
   * {@return nonblank id, unique across all nodes and groups in the diagram tree}
   */
  String id();

  /**
   * {@return non-null plain text label; an empty string is allowed}
   */
  String label();

  /**
   * {@return immutable node appearance; never null}
   */
  default NodeStyle style() {
    return NodeStyle.defaults();
  }
}
