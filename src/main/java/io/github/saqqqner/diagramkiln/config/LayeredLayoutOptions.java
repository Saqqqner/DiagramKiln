package io.github.saqqqner.diagramkiln.config;

/**
 * Immutable settings specific to the bundled ELK Layered adapter.
 *
 * @param cycleBreaking
 *          cycle breaking strategy
 * @param nodePlacement
 *          node placement strategy
 * @param alignment
 *          alignment selection
 * @param thoroughness
 *          positive search effort; larger values can cost more time
 */
public record LayeredLayoutOptions(CycleBreaking cycleBreaking, NodePlacement nodePlacement,
    Alignment alignment, int thoroughness) {
  /**
   * Validates strategies and search effort.
   *
   * @param cycleBreaking
   *          non-null strategy
   * @param nodePlacement
   *          non-null strategy
   * @param alignment
   *          non-null alignment
   * @param thoroughness
   *          strictly positive search effort
   */
  public LayeredLayoutOptions {
    if (cycleBreaking == null || nodePlacement == null || alignment == null)
      throw new IllegalArgumentException("Layered strategies and alignment must not be null");
    if (thoroughness <= 0)
      throw new IllegalArgumentException("thoroughness must be positive");
  }

  /** @return the ELK 0.11 defaults used by legacy exports */
  public static LayeredLayoutOptions defaults() {
    return builder().build();
  }

  /** @return a new mutable builder */
  public static Builder builder() {
    return new Builder();
  }

  /** Mutable builder; built values are immutable. */
  public static final class Builder {
    private CycleBreaking cycleBreaking = CycleBreaking.GREEDY;
    private NodePlacement nodePlacement = NodePlacement.BRANDES_KOEPF;
    private Alignment alignment = Alignment.AUTOMATIC;
    private int thoroughness = 7;
    private Builder() {
    }
    /**
     * @param value
     *          non-null strategy
     * @return this builder
     */
    public Builder cycleBreaking(CycleBreaking value) {
      cycleBreaking = value;
      return this;
    }
    /**
     * @param value
     *          non-null strategy
     * @return this builder
     */
    public Builder nodePlacement(NodePlacement value) {
      nodePlacement = value;
      return this;
    }
    /**
     * @param value
     *          non-null alignment
     * @return this builder
     */
    public Builder alignment(Alignment value) {
      alignment = value;
      return this;
    }
    /**
     * @param value
     *          positive search effort
     * @return this builder
     */
    public Builder thoroughness(int value) {
      thoroughness = value;
      return this;
    }
    /** @return validated immutable settings */
    public LayeredLayoutOptions build() {
      return new LayeredLayoutOptions(cycleBreaking, nodePlacement, alignment, thoroughness);
    }
  }
}
