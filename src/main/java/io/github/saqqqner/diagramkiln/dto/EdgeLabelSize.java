package io.github.saqqqner.diagramkiln.dto;

/**
 * Caller-estimated space reserved for an edge label; does not measure text or wrap words.
 *
 * @param width
 *          finite positive width in pixels
 * @param height
 *          finite positive height in pixels
 */
public record EdgeLabelSize(double width, double height) {
  /**
   * Validates both reserved dimensions.
   *
   * @param width
   *          finite positive width in pixels
   * @param height
   *          finite positive height in pixels
   * @throws IllegalArgumentException
   *           if either dimension is not finite and positive
   */
  public EdgeLabelSize {
    if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0)
      throw new IllegalArgumentException("Edge label width and height must be finite and positive");
  }
}
