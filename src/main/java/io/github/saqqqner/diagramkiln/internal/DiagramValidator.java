package io.github.saqqqner.diagramkiln.internal;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramEdge;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.DiagramNode;
import io.github.saqqqner.diagramkiln.dto.DefaultDiagram;
import io.github.saqqqner.diagramkiln.dto.DefaultEdge;
import io.github.saqqqner.diagramkiln.dto.DefaultGroup;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Validates arbitrary DTO implementations and reads each property into a stable snapshot.
 */
public final class DiagramValidator {
  private DiagramValidator() {
  }

  public static DefaultDiagram snapshot(DiagramData data) {
    var result = capture(data);
    var index = new DiagramIndex(result);
    for (var edge : index.edges()) {
      if (index.node(edge.sourceId()) == null)
        throw new IllegalArgumentException("Unknown source node: " + edge.sourceId());
      if (index.node(edge.targetId()) == null)
        throw new IllegalArgumentException("Unknown target node: " + edge.targetId());
    }
    return result;
  }

  public static DefaultDiagram capture(DiagramData data) {
    return capture(data, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private static DefaultDiagram capture(DiagramData data, Set<DiagramData> active) {
    if (data == null)
      throw new IllegalArgumentException("diagram must not be null");
    if (!active.add(data))
      throw new IllegalArgumentException("Cyclic diagram ownership");
    String name = text(data.sectionName(), "sectionName");
    var inputNodes = data.nodes();
    var inputEdges = data.edges();
    if (inputNodes == null || inputEdges == null) {
      throw new IllegalArgumentException("nodes and edges must not be null");
    }
    var nodes = new ArrayList<DiagramNode>();
    var edges = new ArrayList<DiagramEdge>();
    for (var node : inputNodes) {
      if (node == null)
        throw new IllegalArgumentException("nodes must not contain null");
      String id = id(node.id(), "node.id");
      if (node instanceof DiagramGroup group) {
        var nested = capture(group.diagram(), active);
        var style = group.groupStyle();
        requireStyle(style, "group " + id);
        nodes.add(new DefaultGroup(id, text(group.label(), "group.label"), nested, style));
      }
      else {
        var style = node.style();
        requireStyle(style, "node " + id);
        nodes.add(new DefaultNode(id, text(node.label(), "node.label"), style));
      }
    }
    for (var edge : inputEdges) {
      if (edge == null)
        throw new IllegalArgumentException("edges must not contain null");
      String source = id(edge.sourceId(), "edge.sourceId");
      String target = id(edge.targetId(), "edge.targetId");
      var style = edge.style();
      requireStyle(style, "edge");
      edges.add(new DefaultEdge(source, target, text(edge.label(), "edge.label"), style));
    }
    active.remove(data);
    return new DefaultDiagram(name, nodes, edges);
  }

  private static void requireStyle(Object style, String context) {
    if (style == null)
      throw new IllegalArgumentException(context + " style must not be null");
  }

  private static String id(String value, String field) {
    text(value, field);
    if (value.isBlank())
      throw new IllegalArgumentException(field + " must not be blank");
    return value;
  }

  private static String text(String value, String field) {
    if (value == null)
      throw new IllegalArgumentException(field + " must not be null");
    boolean valid = value.codePoints()
        .allMatch(c -> c == 9 || c == 10 || c == 13 || (c >= 0x20 && c <= 0xD7FF)
            || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF));
    if (!valid)
      throw new IllegalArgumentException(field + " contains a character forbidden in XML 1.0");
    return value;
  }
}
