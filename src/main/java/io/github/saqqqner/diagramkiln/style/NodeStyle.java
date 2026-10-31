package io.github.saqqqner.diagramkiln.style;

/**
 * Immutable, validated node appearance. Values do not resize text automatically.
 *
 * @param shape
 *          visible node shape
 * @param rounded
 *          whether rectangle corners are rounded
 * @param fillColor
 *          fill color (#RRGGBB or none)
 * @param strokeColor
 *          outline color (#RRGGBB or none)
 * @param strokeWidth
 *          outline width in pixels
 * @param dashed
 *          whether the outline is dashed
 * @param fontColor
 *          text color (#RRGGBB)
 * @param fontSize
 *          text size in pixels
 * @param bold
 *          whether text is bold
 * @param arcSize
 *          relative rectangle corner rounding in percent, from 0 to 100; effective radius is capped
 *          at half the smaller dimension and used only when rounded is true
 */
public record NodeStyle(NodeShape shape, boolean rounded, String fillColor, String strokeColor,
    double strokeWidth, boolean dashed, String fontColor, double fontSize, boolean bold,
    double arcSize) {
  /**
   * Creates a style with the legacy 15 percent corner rounding.
   *
   * @param shape
   *          visible node shape
   * @param rounded
   *          whether rectangle corners are rounded
   * @param fillColor
   *          fill color
   * @param strokeColor
   *          outline color
   * @param strokeWidth
   *          outline width in pixels
   * @param dashed
   *          whether the outline is dashed
   * @param fontColor
   *          text color
   * @param fontSize
   *          text size in pixels
   * @param bold
   *          whether text is bold
   */
  public NodeStyle(NodeShape shape, boolean rounded, String fillColor, String strokeColor,
      double strokeWidth, boolean dashed, String fontColor, double fontSize, boolean bold) {
    this(shape, rounded, fillColor, strokeColor, strokeWidth, dashed, fontColor, fontSize, bold,
        15);
  }

  /**
   * Validates appearance values.
   *
   * @param shape
   *          visible node shape
   * @param rounded
   *          whether rectangle corners are rounded
   * @param fillColor
   *          fill color (#RRGGBB or none)
   * @param strokeColor
   *          outline color (#RRGGBB or none)
   * @param strokeWidth
   *          outline width in pixels
   * @param dashed
   *          whether the outline is dashed
   * @param fontColor
   *          text color (#RRGGBB)
   * @param fontSize
   *          text size in pixels
   * @param bold
   *          whether text is bold
   * @param arcSize
   *          relative rectangle corner rounding in percent, from 0 to 100; effective radius is
   *          capped at half the smaller dimension and used only when rounded is true
   */
  public NodeStyle {
    if (shape == null)
      throw new IllegalArgumentException("shape must not be null");
    StyleValidation.color(fillColor, "fillColor", true);
    StyleValidation.color(strokeColor, "strokeColor", true);
    StyleValidation.number(strokeWidth, "strokeWidth", false);
    StyleValidation.color(fontColor, "fontColor", false);
    StyleValidation.number(fontSize, "fontSize", false);
    StyleValidation.number(arcSize, "arcSize", true);
    if (arcSize > 100)
      throw new IllegalArgumentException("arcSize must be between 0 and 100");
  }

  /**
   * {@return the default appearance}
   */
  public static NodeStyle defaults() {
    return builder().build();
  }

  /**
   * {@return a mutable builder initialized with default values}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable appearance builder. Completed styles are independent of later changes.
   */
  public static final class Builder {
    private NodeShape shape = NodeShape.RECTANGLE;
    private boolean rounded = false;
    private String fillColor = "#FFFFFF";
    private String strokeColor = "#000000";
    private double strokeWidth = 1;
    private boolean dashed = false;
    private String fontColor = "#000000";
    private double fontSize = 12;
    private boolean bold = false;
    private double arcSize = 15;

    private Builder() {
    }

    /**
     * Sets visible node shape.
     *
     * @param value
     *          visible node shape
     * @return this builder
     */
    public Builder shape(NodeShape value) {
      shape = value;
      return this;
    }

    /**
     * Sets whether rectangle corners are rounded.
     *
     * @param value
     *          whether rectangle corners are rounded
     * @return this builder
     */
    public Builder rounded(boolean value) {
      rounded = value;
      return this;
    }

    /**
     * Sets relative rectangle rounding, effective only for rounded rectangles.
     *
     * @param value
     *          percentage from 0 to 100, capped at half the smaller dimension when rendered
     * @return this builder
     */
    public Builder arcSize(double value) {
      arcSize = value;
      return this;
    }

    /**
     * Sets fill color (#RRGGBB or none).
     *
     * @param value
     *          fill color (#RRGGBB or none)
     * @return this builder
     */
    public Builder fillColor(String value) {
      fillColor = value;
      return this;
    }

    /**
     * Sets outline color (#RRGGBB or none).
     *
     * @param value
     *          outline color (#RRGGBB or none)
     * @return this builder
     */
    public Builder strokeColor(String value) {
      strokeColor = value;
      return this;
    }

    /**
     * Sets outline width in pixels.
     *
     * @param value
     *          outline width in pixels
     * @return this builder
     */
    public Builder strokeWidth(double value) {
      strokeWidth = value;
      return this;
    }

    /**
     * Sets whether the outline is dashed.
     *
     * @param value
     *          whether the outline is dashed
     * @return this builder
     */
    public Builder dashed(boolean value) {
      dashed = value;
      return this;
    }

    /**
     * Sets text color (#RRGGBB).
     *
     * @param value
     *          text color (#RRGGBB)
     * @return this builder
     */
    public Builder fontColor(String value) {
      fontColor = value;
      return this;
    }

    /**
     * Sets text size in pixels.
     *
     * @param value
     *          text size in pixels
     * @return this builder
     */
    public Builder fontSize(double value) {
      fontSize = value;
      return this;
    }

    /**
     * Sets whether text is bold.
     *
     * @param value
     *          whether text is bold
     * @return this builder
     */
    public Builder bold(boolean value) {
      bold = value;
      return this;
    }

    /**
     * Creates a validated immutable appearance.
     *
     * @return the appearance
     * @throws IllegalArgumentException
     *           if a color, size or enum value is invalid
     */
    public NodeStyle build() {
      return new NodeStyle(shape, rounded, fillColor, strokeColor, strokeWidth, dashed, fontColor,
          fontSize, bold, arcSize);
    }
  }
}
