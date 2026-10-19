package io.github.saqqqner.diagramkiln.config;

/**
 * Validated layout configuration; container bounds are calculated from contents.
 *
 * @param direction
 *          non-null main flow direction
 * @param nodeWidth
 *          finite positive leaf width in pixels
 * @param nodeHeight
 *          finite positive leaf height in pixels
 * @param nodeSpacing
 *          finite nonnegative minimum spacing within a layer, in pixels
 * @param layerSpacing
 *          finite nonnegative minimum spacing between layers, in pixels
 * @param groupPadding
 *          finite nonnegative inner group padding, in addition to its header, in pixels
 */
public record LayoutOptions(Direction direction, double nodeWidth, double nodeHeight,
    double nodeSpacing, double layerSpacing, double groupPadding) {
  /**
   * Creates settings with legacy spacing (40/80) and group padding (20).
   *
   * @param direction
   *          non-null main flow direction
   * @param nodeWidth
   *          finite positive width in pixels
   * @param nodeHeight
   *          finite positive height in pixels
   */
  public LayoutOptions(Direction direction, double nodeWidth, double nodeHeight) {
    this(direction, nodeWidth, nodeHeight, 40, 80, 20);
  }

  /**
   * Validates that direction is set and leaf dimensions are finite and positive.
   *
   * @param direction
   *          non-null main flow direction
   * @param nodeWidth
   *          finite positive leaf width in pixels
   * @param nodeHeight
   *          finite positive leaf height in pixels
   * @param nodeSpacing
   *          finite nonnegative minimum spacing within a layer, in pixels
   * @param layerSpacing
   *          finite nonnegative minimum spacing between layers, in pixels
   * @param groupPadding
   *          finite nonnegative inner group padding, in addition to its header, in pixels
   */
  public LayoutOptions {
    if (direction == null)
      throw new IllegalArgumentException("direction must not be null");
    if (!Double.isFinite(nodeWidth) || !Double.isFinite(nodeHeight) || nodeWidth <= 0
        || nodeHeight <= 0) {
      throw new IllegalArgumentException("Node width and height must be finite and positive");
    }
    nonnegative(nodeSpacing, "nodeSpacing");
    nonnegative(layerSpacing, "layerSpacing");
    nonnegative(groupPadding, "groupPadding");
  }

  private void nonnegative(double value, String name) {
    if (!Double.isFinite(value) || value < 0)
      throw new IllegalArgumentException(name + " must be finite and nonnegative");
  }

  /**
   * {@return a builder with RIGHT direction, 160 by 60 nodes and legacy spacing}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable layout configuration builder; completed settings are immutable.
   */
  public static final class Builder {
    private Direction direction = Direction.RIGHT;
    private double width = 160;
    private double height = 60;
    private double nodeSpacing = 40;
    private double layerSpacing = 80;
    private double groupPadding = 20;

    private Builder() {
    }

    /**
     * Sets the main layout direction at every nesting level.
     *
     * @param value
     *          non-null direction
     * @return this builder
     */
    public Builder direction(Direction value) {
      direction = value;
      return this;
    }

    /**
     * Sets fixed leaf bounds; groups grow to fit their contents.
     *
     * @param width
     *          finite positive width in pixels
     * @param height
     *          finite positive height in pixels
     * @return this builder
     */
    public Builder nodeSize(double width, double height) {
      this.width = width;
      this.height = height;
      return this;
    }

    /**
     * Sets minimum spacing between nodes within each layer. ELK may require larger gaps.
     *
     * @param value
     *          finite nonnegative distance in pixels
     * @return this builder
     */
    public Builder nodeSpacing(double value) {
      nodeSpacing = value;
      return this;
    }

    /**
     * Sets minimum spacing between successive layers. ELK may require larger gaps.
     *
     * @param value
     *          finite nonnegative distance in pixels
     * @return this builder
     */
    public Builder layerSpacing(double value) {
      layerSpacing = value;
      return this;
    }

    /**
     * Sets inner padding of nonempty groups. Top padding is added below the group header. Empty
     * groups retain the configured node dimensions plus header height.
     *
     * @param value
     *          finite nonnegative distance in pixels
     * @return this builder
     */
    public Builder groupPadding(double value) {
      groupPadding = value;
      return this;
    }

    /**
     * Validates and captures layout settings.
     *
     * @return immutable settings
     * @throws IllegalArgumentException
     *           for null direction or invalid dimensions or spacing
     */
    public LayoutOptions build() {
      return new LayoutOptions(direction, width, height, nodeSpacing, layerSpacing, groupPadding);
    }
  }
}
