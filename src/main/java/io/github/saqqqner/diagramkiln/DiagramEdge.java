package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.style.EdgeStyle;

/**
 * A directed connection. Parallel edges and self-loops are supported.
 */
public interface DiagramEdge {
  /**
   * {@return global id of the source node or group}
   */
  String sourceId();

  /**
   * {@return global id of the target node or group}
   */
  String targetId();

  /**
   * {@return non-null plain text connection label; empty by default}
   */
  default String label() {
    return "";
  }

  /**
   * {@return immutable connection appearance; never null}
   */
  default EdgeStyle style() {
    return EdgeStyle.defaults();
  }
}
