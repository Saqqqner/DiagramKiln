package io.github.saqqqner.diagramkiln.internal;

import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.DiagramNode;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;

import java.util.Locale;

/**
 * Serializes validated typed properties without accepting raw mxGraph fragments.
 */
final class StyleWriter {
  private StyleWriter() {
  }

  static String node(DiagramNode node) {
    if (node instanceof DiagramGroup group) {
      var s = group.groupStyle();
      return "swimlane;horizontal=1;container=1;collapsible=0;recursiveResize=0;startSize="
          + s.headerHeight() + ";fillColor=" + s.fillColor() + ";" + common(s.strokeColor(),
              s.strokeWidth(), s.dashed(), s.fontColor(), s.fontSize(), s.bold());
    }
    var s = node.style();
    String shape = switch (s.shape()) {
      case RECTANGLE -> "rectangle";
      case ELLIPSE -> "ellipse";
      case DIAMOND -> "rhombus";
    };
    return "shape=" + shape + ";rounded=" + flag(s.rounded()) + ";arcSize=" + s.arcSize()
        + ";absoluteArcSize=0;fillColor=" + s.fillColor() + ";" + common(s.strokeColor(),
            s.strokeWidth(), s.dashed(), s.fontColor(), s.fontSize(), s.bold());
  }

  static String edge(EdgeStyle s) {
    return "noEdgeStyle=1;orthogonalLoop=0;rounded=" + flag(s.rounded()) + ";arcSize=" + s.arcSize()
        + ";jumpStyle=" + s.jumpStyle().name().toLowerCase(Locale.ROOT) + ";jumpSize="
        + s.jumpSize() + ";endSize=" + s.endSize() + ";exitPerimeter=0;entryPerimeter=0;"
        + "startArrow=" + s.startArrow().name().toLowerCase(Locale.ROOT) + ";endArrow="
        + s.endArrow().name().toLowerCase(Locale.ROOT) + ";startFill=1;endFill=1;" + common(
            s.strokeColor(), s.strokeWidth(), s.dashed(), s.fontColor(), s.fontSize(), s.bold());
  }

  private static String common(String stroke, double width, boolean dashed, String font,
      double fontSize, boolean bold) {
    return "whiteSpace=wrap;html=0;strokeColor=" + stroke + ";strokeWidth=" + width + ";dashed="
        + flag(dashed) + ";fontColor=" + font + ";fontSize=" + fontSize + ";fontStyle=" + flag(bold)
        + ";";
  }

  private static int flag(boolean value) {
    return value ? 1 : 0;
  }
}
