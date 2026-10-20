package io.github.saqqqner.diagramkiln.config;

/** Public leaf layer constraints. ELK separate-layer values are reserved for internal use. */
public enum LayerConstraint {
  /** Does not constrain layer assignment. */
  NONE,
  /** Places a leaf in the first layer of its containing graph. */
  FIRST,
  /** Places a leaf in the last layer of its containing graph. */
  LAST
}
