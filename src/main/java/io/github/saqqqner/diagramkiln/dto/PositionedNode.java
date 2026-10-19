package io.github.saqqqner.diagramkiln.dto;

/**
 * Page-coordinate bounds for a leaf or group.
 *
 * @param id
 *          input node or group id
 * @param x
 *          left bound in pixels
 * @param y
 *          top bound in pixels
 * @param width
 *          positive width in pixels
 * @param height
 *          positive height in pixels
 */
public record PositionedNode(String id, double x, double y, double width, double height) {
}
