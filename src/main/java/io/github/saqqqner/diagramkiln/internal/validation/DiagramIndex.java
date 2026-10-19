package io.github.saqqqner.diagramkiln.internal.validation;

import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramEdge;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.DiagramNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Internal traversal index. Input must already be an acyclic, immutable tree.
 */
public final class DiagramIndex {
  private final List<DiagramNode> nodes = new ArrayList<>();
  private final List<DiagramEdge> edges = new ArrayList<>();
  private final Map<String, String> parents = new HashMap<>();
  private final Map<String, DiagramNode> byId = new HashMap<>();
  private final List<DiagramNode> orderedNodes;
  private final List<DiagramEdge> orderedEdges;

  public DiagramIndex(DiagramData data) {
    visit(data, null);
    orderedNodes = List.copyOf(nodes);
    orderedEdges = List.copyOf(edges);
  }

  private void visit(DiagramData data, String parent) {
    edges.addAll(data.edges());
    for (var node : data.nodes()) {
      if (byId.putIfAbsent(node.id(), node) != null) {
        throw new IllegalArgumentException("Duplicate node id: " + node.id());
      }
      nodes.add(node);
      parents.put(node.id(), parent);
      if (node instanceof DiagramGroup group)
        visit(group.diagram(), node.id());
    }
  }

  public List<DiagramNode> nodes() {
    return orderedNodes;
  }

  public List<DiagramEdge> edges() {
    return orderedEdges;
  }

  public String parentId(String id) {
    return parents.get(id);
  }

  public DiagramNode node(String id) {
    return byId.get(id);
  }

  public String edgeParentId(DiagramEdge edge) {
    var ancestors = new HashSet<String>();
    for (String id = parentId(edge.sourceId()); id != null; id = parentId(id))
      ancestors.add(id);
    for (String id = parentId(edge.targetId()); id != null; id = parentId(id)) {
      if (ancestors.contains(id))
        return id;
    }
    return null;
  }
}
