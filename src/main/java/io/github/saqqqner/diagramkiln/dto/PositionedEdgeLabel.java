package io.github.saqqqner.diagramkiln.dto;

/**
 * Final label bounds in absolute page coordinates. Coverage and geometry are checked by export.
 *
 * @param edgeIndex
 *          root-first traversal index of the labeled edge
 * @param x
 *          finite left coordinate in pixels
 * @param y
 *          finite top coordinate in pixels
 * @param width
 *          finite positive width in pixels
 * @param height
 *          finite positive height in pixels
 */
public record PositionedEdgeLabel(int edgeIndex, double x, double y, double width, double height) {
}
