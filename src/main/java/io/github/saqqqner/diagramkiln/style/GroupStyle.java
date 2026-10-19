package io.github.saqqqner.diagramkiln.style;

/**
 * Immutable, validated group appearance. Values do not resize text automatically.
 *
 * @param fillColor
 *          header fill color (#RRGGBB or none)
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
 * @param headerHeight
 *          horizontal header height in pixels; zero hides it
 */
public record GroupStyle(String fillColor, String strokeColor, double strokeWidth, boolean dashed,
    String fontColor, double fontSize, boolean bold, double headerHeight) {
  /**
   * Validates appearance values.
   *
   * @param fillColor
   *          header fill color (#RRGGBB or none)
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
   * @param headerHeight
   *          horizontal header height in pixels; zero hides it
   */
  public GroupStyle {
    StyleValidation.color(fillColor, "fillColor", true);
    StyleValidation.color(strokeColor, "strokeColor", true);
    StyleValidation.number(strokeWidth, "strokeWidth", false);
    StyleValidation.color(fontColor, "fontColor", false);
    StyleValidation.number(fontSize, "fontSize", false);
    StyleValidation.number(headerHeight, "headerHeight", true);
  }

  /**
   * {@return the default appearance}
   */
  public static GroupStyle defaults() {
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
    private String fillColor = "#FFFFFF";
    private String strokeColor = "#000000";
    private double strokeWidth = 1;
    private boolean dashed = false;
    private String fontColor = "#000000";
    private double fontSize = 12;
    private boolean bold = false;
    private double headerHeight = 28;

    private Builder() {
    }

    /**
     * Sets header fill color (#RRGGBB or none).
     *
     * @param value
     *          header fill color (#RRGGBB or none)
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
     * Sets horizontal header height in pixels; zero hides it.
     *
     * @param value
     *          horizontal header height in pixels; zero hides it
     * @return this builder
     */
    public Builder headerHeight(double value) {
      headerHeight = value;
      return this;
    }

    /**
     * Creates a validated immutable appearance.
     *
     * @return the appearance
     * @throws IllegalArgumentException
     *           if a color, size or enum value is invalid
     */
    public GroupStyle build() {
      return new GroupStyle(fillColor, strokeColor, strokeWidth, dashed, fontColor, fontSize, bold,
          headerHeight);
    }
  }
}
