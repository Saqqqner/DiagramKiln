package io.github.saqqqner.diagramkiln.layout;

import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.options.SizeConstraint;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.DiagramGroup;
import io.github.saqqqner.diagramkiln.SizedDiagramNode;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;
import io.github.saqqqner.diagramkiln.internal.DiagramIndex;
import io.github.saqqqner.diagramkiln.internal.DiagramValidator;
import io.github.saqqqner.diagramkiln.internal.NodeGeometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * ELK Layered adapter. Each invocation owns its graph and returns page-coordinate geometry.
 */
public final class ElkLayout implements LayoutEngine {
  private static final String ALGORITHM = "org.eclipse.elk.layered";

  /**
   * Creates a reusable compound layout adapter.
   */
  public ElkLayout() {
  }

  @Override
  public LayoutResult layout(DiagramData diagram, LayoutOptions options) {
    if (options == null)
      throw new IllegalArgumentException("layout options must not be null");
    var data = DiagramValidator.snapshot(diagram);
    var index = new DiagramIndex(data);
    if (index.nodes().isEmpty())
      return new LayoutResult(List.of(), List.of());
    registerAlgorithm();
    var graph = ElkGraphUtil.createGraph();
    configure(graph, options);
    var elkNodes = new LinkedHashMap<String, ElkNode>();
    for (var node : index.nodes()) {
      String parentId = index.parentId(node.id());
      var elk = ElkGraphUtil.createNode(parentId == null ? graph : elkNodes.get(parentId));
      elk.setIdentifier(node.id());
      if (node instanceof DiagramGroup group) {
        configure(elk, options);
        elk.setProperty(CoreOptions.PADDING,
            new ElkPadding(group.groupStyle().headerHeight() + options.groupPadding(),
                options.groupPadding(), options.groupPadding(), options.groupPadding()));
        if (group.diagram().nodes().isEmpty())
          elk.setDimensions(options.nodeWidth(),
              options.nodeHeight() + group.groupStyle().headerHeight());
      }
      else if (node instanceof SizedDiagramNode sized) {
        var size = sized.size();
        elk.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, SizeConstraint.fixed());
        elk.setDimensions(size.width(), size.height());
      }
      else
        elk.setDimensions(options.nodeWidth(), options.nodeHeight());
      elkNodes.put(node.id(), elk);
    }
    var elkEdges = new ArrayList<ElkEdge>();
    for (var edge : index.edges()) {
      var elk = ElkGraphUtil.createSimpleEdge(elkNodes.get(edge.sourceId()),
          elkNodes.get(edge.targetId()));
      elkEdges.add(elk);
    }
    new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());
    var nodes = new LinkedHashMap<String, PositionedNode>();
    elkNodes.forEach((id, node) -> {
      var origin = origin(node, graph);
      nodes.put(id,
          new PositionedNode(id, origin.x(), origin.y(), node.getWidth(), node.getHeight()));
    });
    var edges = new ArrayList<RoutedEdge>();
    for (int i = 0; i < elkEdges.size(); i++) {
      var edge = elkEdges.get(i);
      if (edge.getSections().size() != 1)
        throw new IllegalStateException(
            "Expected one ELK route section for edge " + i + ", got " + edge.getSections().size());
      var offset = origin(edge.getContainingNode(), graph);
      var section = edge.getSections().get(0);
      var points = new ArrayList<Point>();
      points.add(new Point(section.getStartX() + offset.x(), section.getStartY() + offset.y()));
      for (var bend : section.getBendPoints())
        points.add(new Point(bend.getX() + offset.x(), bend.getY() + offset.y()));
      points.add(new Point(section.getEndX() + offset.x(), section.getEndY() + offset.y()));
      var input = index.edges().get(i);
      points.set(0, NodeGeometry.attach(points.get(0), nodes.get(input.sourceId()),
          index.node(input.sourceId())));
      int last = points.size() - 1;
      points.set(last, NodeGeometry.attach(points.get(last), nodes.get(input.targetId()),
          index.node(input.targetId())));
      edges.add(new RoutedEdge(i, points));
    }
    return new LayoutResult(new ArrayList<>(nodes.values()), edges);
  }

  private void configure(ElkNode node, LayoutOptions options) {
    node.setProperty(CoreOptions.ALGORITHM, ALGORITHM);
    node.setProperty(CoreOptions.DIRECTION,
        org.eclipse.elk.core.options.Direction.valueOf(options.direction().name()));
    node.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
    node.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
    node.setProperty(CoreOptions.SPACING_NODE_NODE, options.nodeSpacing());
    node.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, options.layerSpacing());
  }

  private Point origin(ElkNode node, ElkNode root) {
    double x = 0, y = 0;
    for (var current = node; current != null && current != root; current = current.getParent()) {
      x += current.getX();
      y += current.getY();
    }
    return new Point(x, y);
  }

  private void registerAlgorithm() {
    var metadata = LayoutMetaDataService.getInstance();
    synchronized (metadata) {
      if (metadata.getAlgorithmData(ALGORITHM) == null)
        metadata.registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
    }
  }
}
