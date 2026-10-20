package io.github.saqqqner.diagramkiln.config;

/**
 * Immutable per-leaf layout metadata. Custom engines may interpret or ignore it.
 *
 * @param layerConstraint
 *          non-null constraint, relative to the containing graph and flow direction
 */
public record NodeLayout(LayerConstraint layerConstraint) {
  /**
   * Validates the layer constraint. Incompatible graph constraints may still fail during layout.
   *
   * @param layerConstraint
   *          non-null constraint
   */
  public NodeLayout {
    if (layerConstraint == null)
      throw new IllegalArgumentException("layerConstraint must not be null");
  }
  /** @return unconstrained leaf settings */
  public static NodeLayout defaults() {
    return new NodeLayout(LayerConstraint.NONE);
  }
  /** @return a mutable builder */
  public static Builder builder() {
    return new Builder();
  }
  /** Mutable builder; built metadata is immutable. */
  public static final class Builder {
    private LayerConstraint layerConstraint = LayerConstraint.NONE;
    private Builder() {
    }
    /**
     * @param value
     *          non-null constraint
     * @return this builder
     */
    public Builder layerConstraint(LayerConstraint value) {
      layerConstraint = value;
      return this;
    }
    /** @return validated immutable settings */
    public NodeLayout build() {
      return new NodeLayout(layerConstraint);
    }
  }
}
