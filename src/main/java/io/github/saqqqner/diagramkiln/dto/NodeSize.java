package io.github.saqqqner.diagramkiln.dto;

/**
 * Immutable explicit leaf dimensions in pixels; both dimensions override the global fallback.
 *
 * @param width
 *          finite positive width in pixels
 * @param height
 *          finite positive height in pixels
 */
public record NodeSize(double width, double height) {
  /**
   * Validates that both dimensions are finite and positive.
   *
   * @param width
   *          finite positive width in pixels
   * @param height
   *          finite positive height in pixels
   * @throws IllegalArgumentException
   *           if either dimension is not finite and positive
   */
  public NodeSize {
    if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0)
      throw new IllegalArgumentException("Node width and height must be finite and positive");
  }
}
