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
 * @param feedbackEdges
 *          whether ELK routes backward edges around nodes, preserving flow-side attachments
 * @param preserveNodeOrder
 *          whether same-layer nodes retain declaration order within each connected component
 */
public record LayoutOptions(Direction direction, double nodeWidth, double nodeHeight,
    double nodeSpacing, double layerSpacing, double groupPadding, boolean feedbackEdges,
    boolean preserveNodeOrder) {
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
   * Creates settings with feedback-edge routing disabled, preserving legacy behavior.
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
  public LayoutOptions(Direction direction, double nodeWidth, double nodeHeight, double nodeSpacing,
      double layerSpacing, double groupPadding) {
    this(direction, nodeWidth, nodeHeight, nodeSpacing, layerSpacing, groupPadding, false);
  }

  /**
   * Creates settings with automatic node ordering and optional feedback routing.
   *
   * @param direction
   *          non-null flow direction
   * @param nodeWidth
   *          finite positive leaf width
   * @param nodeHeight
   *          finite positive leaf height
   * @param nodeSpacing
   *          finite nonnegative spacing within a layer
   * @param layerSpacing
   *          finite nonnegative spacing between layers
   * @param groupPadding
   *          finite nonnegative group padding
   * @param feedbackEdges
   *          whether to route feedback edges around nodes
   */
  public LayoutOptions(Direction direction, double nodeWidth, double nodeHeight, double nodeSpacing,
      double layerSpacing, double groupPadding, boolean feedbackEdges) {
    this(direction, nodeWidth, nodeHeight, nodeSpacing, layerSpacing, groupPadding, feedbackEdges,
        false);
  }

  /**
   * Validates direction, dimensions and spacing.
   *
   * @param direction
   *          non-null main flow direction
   * @param nodeWidth
   *          finite positive leaf width in pixels
   * @param nodeHeight
   *          finite positive leaf height in pixels
   * @param nodeSpacing
   *          finite nonnegative spacing within a layer
   * @param layerSpacing
   *          finite nonnegative spacing between layers
   * @param groupPadding
   *          finite nonnegative inner group padding
   * @param feedbackEdges
   *          whether backward edges retain flow-side attachments and route around nodes
   * @param preserveNodeOrder
   *          whether same-layer nodes retain declaration order within each connected component
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
    private boolean feedbackEdges = false;
    private boolean preserveNodeOrder = false;

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
     * Routes feedback edges around nodes at every nesting level. With RIGHT flow, ordinary incoming
     * ports remain on the left and outgoing ports on the right, including backward edges. Does not
     * fix the attachment position along that side or control self-loop ports. Disabled by default.
     * Custom layout engines decide how to interpret this setting.
     *
     * @param value
     *          whether to enable ELK feedback-edge routing
     * @return this builder
     */
    public Builder feedbackEdges(boolean value) {
      feedbackEdges = value;
      return this;
    }

    /**
     * Preserves declaration order of real nodes within a layer of each connected component,
     * including nested graphs. With RIGHT flow this is top-to-bottom order. Layer assignment still
     * follows the graph; disconnected component packing and edge/port order are not fixed. May
     * introduce additional crossings. Disabled by default; custom engines interpret the flag.
     *
     * @param value
     *          whether crossing minimization must preserve the node model order
     * @return this builder
     */
    public Builder preserveNodeOrder(boolean value) {
      preserveNodeOrder = value;
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
      return new LayoutOptions(direction, width, height, nodeSpacing, layerSpacing, groupPadding,
          feedbackEdges, preserveNodeOrder);
    }
  }
}
