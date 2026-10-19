package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.style.GroupStyle;

/**
 * A diagram node owning a nested diagram. Ownership must form a tree, not a cycle.
 */
public interface DiagramGroup extends DiagramNode {
  /**
   * {@return the nested diagram; never null}
   */
  DiagramData diagram();

  @Override
  default String label() {
    return diagram().sectionName();
  }

  /**
   * {@return immutable container appearance; never null}
   */
  default GroupStyle groupStyle() {
    return GroupStyle.defaults();
  }
}
