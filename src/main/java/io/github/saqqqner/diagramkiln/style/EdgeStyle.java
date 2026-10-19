package io.github.saqqqner.diagramkiln.style;

/**
 * Immutable, validated edge appearance. Values do not resize text automatically.
 *
 * @param strokeColor
 *          outline color (#RRGGBB or none)
 * @param strokeWidth
 *          outline width in pixels
 * @param dashed
 *          whether the outline is dashed
 * @param fontColor
 *          text color (#RRGGBB)
 * @param fontSize
 *          text size in points
 * @param bold
 *          whether text is bold
 * @param startArrow
 *          source arrowhead
 * @param endArrow
 *          target arrowhead
 * @param jumpStyle
 *          how draw.io renders crossings; independent of ELK routing
 * @param jumpSize
 *          crossing bridge or gap size in pixels; finite and nonnegative
 * @param endSize
 *          target arrowhead size in pixels; finite and nonnegative
 * @param rounded
 *          whether bends are rendered with rounded joins
 * @param arcSize
 *          absolute bend rounding size in pixels; finite and nonnegative
 */
public record EdgeStyle(String strokeColor, double strokeWidth, boolean dashed, String fontColor,
    double fontSize, boolean bold, Arrow startArrow, Arrow endArrow, JumpStyle jumpStyle,
    double jumpSize, double endSize, boolean rounded, double arcSize) {
  /**
   * Creates a style with legacy arrow size, sharp bends and no crossing jumps.
   *
   * @param strokeColor
   *          outline color
   * @param strokeWidth
   *          outline width in pixels
   * @param dashed
   *          whether the outline is dashed
   * @param fontColor
   *          text color
   * @param fontSize
   *          text size in points
   * @param bold
   *          whether text is bold
   * @param startArrow
   *          source arrowhead
   * @param endArrow
   *          target arrowhead
   */
  public EdgeStyle(String strokeColor, double strokeWidth, boolean dashed, String fontColor,
      double fontSize, boolean bold, Arrow startArrow, Arrow endArrow) {
    this(strokeColor, strokeWidth, dashed, fontColor, fontSize, bold, startArrow, endArrow,
        JumpStyle.NONE, 6, 6, false, 20);
  }

  /**
   * Validates appearance values.
   *
   * @param strokeColor
   *          outline color (#RRGGBB or none)
   * @param strokeWidth
   *          outline width in pixels
   * @param dashed
   *          whether the outline is dashed
   * @param fontColor
   *          text color (#RRGGBB)
   * @param fontSize
   *          text size in points
   * @param bold
   *          whether text is bold
   * @param startArrow
   *          source arrowhead
   * @param endArrow
   *          target arrowhead
   * @param jumpStyle
   *          how draw.io renders crossings; independent of ELK routing
   * @param jumpSize
   *          crossing bridge or gap size in pixels; finite and nonnegative
   * @param endSize
   *          target arrowhead size in pixels; finite and nonnegative
   * @param rounded
   *          whether bends are rendered with rounded joins
   * @param arcSize
   *          absolute bend rounding size in pixels; finite and nonnegative
   */
  public EdgeStyle {
    StyleValidation.color(strokeColor, "strokeColor", true);
    StyleValidation.number(strokeWidth, "strokeWidth", false);
    StyleValidation.color(fontColor, "fontColor", false);
    StyleValidation.number(fontSize, "fontSize", false);
    if (startArrow == null)
      throw new IllegalArgumentException("startArrow must not be null");
    if (endArrow == null)
      throw new IllegalArgumentException("endArrow must not be null");
    if (jumpStyle == null)
      throw new IllegalArgumentException("jumpStyle must not be null");
    StyleValidation.number(jumpSize, "jumpSize", true);
    StyleValidation.number(endSize, "endSize", true);
    StyleValidation.number(arcSize, "arcSize", true);
  }

  /**
   * {@return the default appearance}
   */
  public static EdgeStyle defaults() {
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
    private String strokeColor = "#000000";
    private double strokeWidth = 1;
    private boolean dashed = false;
    private String fontColor = "#000000";
    private double fontSize = 12;
    private boolean bold = false;
    private Arrow startArrow = Arrow.NONE;
    private Arrow endArrow = Arrow.BLOCK;
    private JumpStyle jumpStyle = JumpStyle.NONE;
    private double jumpSize = 6;
    private double endSize = 6;
    private boolean rounded = false;
    private double arcSize = 20;

    private Builder() {
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
     * Sets text size in points.
     *
     * @param value
     *          text size in points
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
     * Sets source arrowhead.
     *
     * @param value
     *          source arrowhead
     * @return this builder
     */
    public Builder startArrow(Arrow value) {
      startArrow = value;
      return this;
    }

    /**
     * Sets target arrowhead.
     *
     * @param value
     *          target arrowhead
     * @return this builder
     */
    public Builder endArrow(Arrow value) {
      endArrow = value;
      return this;
    }

    /**
     * Sets how draw.io renders connection crossings. Jumps depend on connection drawing order.
     *
     * @param value
     *          non-null crossing style
     * @return this builder
     */
    public Builder jumpStyle(JumpStyle value) {
      jumpStyle = value;
      return this;
    }

    /**
     * Sets the crossing bridge or gap size. draw.io interprets this size as whole pixels.
     *
     * @param value
     *          finite nonnegative size in pixels
     * @return this builder
     */
    public Builder jumpSize(double value) {
      jumpSize = value;
      return this;
    }

    /**
     * Sets the target arrowhead size, ignored when endArrow is NONE.
     *
     * @param value
     *          finite nonnegative size in pixels
     * @return this builder
     */
    public Builder endSize(double value) {
      endSize = value;
      return this;
    }

    /**
     * Sets whether draw.io rounds joins along the ELK route.
     *
     * @param value
     *          whether bends are rounded
     * @return this builder
     */
    public Builder rounded(boolean value) {
      rounded = value;
      return this;
    }

    /**
     * Sets absolute bend rounding, used only when rounded is true.
     *
     * @param value
     *          finite nonnegative arc size in pixels
     * @return this builder
     */
    public Builder arcSize(double value) {
      arcSize = value;
      return this;
    }

    /**
     * Creates a validated immutable appearance.
     *
     * @return the appearance
     * @throws IllegalArgumentException
     *           if a color, size or enum value is invalid
     */
    public EdgeStyle build() {
      return new EdgeStyle(strokeColor, strokeWidth, dashed, fontColor, fontSize, bold, startArrow,
          endArrow, jumpStyle, jumpSize, endSize, rounded, arcSize);
    }
  }
}
