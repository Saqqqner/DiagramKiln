package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.NodeLayout;

/**
 * Optional leaf capability for layout metadata, independent of node appearance and size. Groups
 * cannot implement this capability. Do not mutate while input is captured.
 */
public interface LayoutDiagramNode extends DiagramNode {
  /** @return non-null immutable per-leaf layout settings */
  NodeLayout layout();
}
