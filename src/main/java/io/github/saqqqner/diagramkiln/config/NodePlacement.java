package io.github.saqqqner.diagramkiln.config;

/** Supported ELK 0.11.0 choices, independent of ELK types. */
public enum NodePlacement {
  /** Uses simple node placement. */
  SIMPLE,
  /** Retains initial cross-axis positions; requires meaningful initial coordinates. */
  INTERACTIVE,
  /** Places linear segments while reducing bends. */
  LINEAR_SEGMENTS,
  /** Uses Brandes-Koepf node placement (the default). */
  BRANDES_KOEPF,
  /** Uses network simplex node placement. */
  NETWORK_SIMPLEX
}
