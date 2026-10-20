package io.github.saqqqner.diagramkiln.config;

/** Supported ELK 0.11.0 choices, independent of ELK types. */
public enum CycleBreaking {
  /** Uses a greedy heuristic to minimize reversed edges. */
  GREEDY,
  /** Uses depth-first traversal in edge order. */
  DEPTH_FIRST,
  /** Uses initial node positions; requires meaningful initial coordinates. */
  INTERACTIVE,
  /** Uses node model order. */
  MODEL_ORDER,
  /** Uses a greedy heuristic with model order as a tie-breaker. */
  GREEDY_MODEL_ORDER,
  /** Uses strongly connected components and connectivity. */
  SCC_CONNECTIVITY,
  /** Uses strongly connected components with model order and node type preferences. */
  SCC_NODE_TYPE,
  /** Uses depth-first traversal in node model order. */
  DFS_NODE_ORDER,
  /** Uses breadth-first traversal in node model order. */
  BFS_NODE_ORDER
}
