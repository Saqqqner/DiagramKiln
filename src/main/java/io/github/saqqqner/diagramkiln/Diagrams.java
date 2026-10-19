package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.dto.DefaultDiagram;
import io.github.saqqqner.diagramkiln.dto.DefaultEdge;
import io.github.saqqqner.diagramkiln.dto.DefaultGroup;
import io.github.saqqqner.diagramkiln.dto.DefaultNode;
import io.github.saqqqner.diagramkiln.dto.NodeSize;
import io.github.saqqqner.diagramkiln.dto.SizedNode;
import io.github.saqqqner.diagramkiln.internal.DiagramValidator;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Entry point for the Java DSL. Builders are mutable and intended for use by one thread.
 */
public final class Diagrams {
  private Diagrams() {
  }

  /**
   * Starts an ordered diagram description. Validation is deferred to build().
   *
   * @param sectionName
   *          page or container title; empty is allowed
   * @return a new single-threaded builder
   */
  public static Builder diagram(String sectionName) {
    return new Builder(sectionName);
  }

  /**
   * Mutable DSL builder. Completed diagrams are independent immutable snapshots.
   */
  public static final class Builder {
    private final String sectionName;
    private final List<DiagramNode> nodes = new ArrayList<>();
    private final List<DiagramEdge> edges = new ArrayList<>();

    private Builder(String sectionName) {
      this.sectionName = sectionName;
    }

    /**
     * Adds a node with default appearance.
     *
     * @param id
     *          globally unique node id
     * @param label
     *          plain text label
     * @return this builder
     */
    public Builder node(String id, String label) {
      return node(id, label, NodeStyle.defaults());
    }

    /**
     * Adds a node with reusable immutable appearance.
     *
     * @param id
     *          globally unique node id
     * @param label
     *          plain text label
     * @param style
     *          non-null node appearance
     * @return this builder
     */
    public Builder node(String id, String label, NodeStyle style) {
      nodes.add(new DefaultNode(id, label, style));
      return this;
    }

    /**
     * Adds a node with default appearance and explicit dimensions, validated immediately.
     *
     * @param id
     *          globally unique node id
     * @param label
     *          plain text label
     * @param width
     *          finite positive width in pixels
     * @param height
     *          finite positive height in pixels
     * @return this builder
     * @throws IllegalArgumentException
     *           if either dimension is not finite and positive
     */
    public Builder node(String id, String label, double width, double height) {
      return node(id, label, NodeStyle.defaults(), width, height);
    }

    /**
     * Adds a styled leaf with explicit dimensions, validated immediately. The bundled ELK adapter
     * preserves this size; custom layout engines control their returned bounds.
     *
     * @param id
     *          globally unique node id
     * @param label
     *          plain text label
     * @param style
     *          non-null node appearance
     * @param width
     *          finite positive width in pixels
     * @param height
     *          finite positive height in pixels
     * @return this builder
     * @throws IllegalArgumentException
     *           if either dimension is not finite and positive
     */
    public Builder node(String id, String label, NodeStyle style, double width, double height) {
      nodes.add(new SizedNode(id, label, style, new NodeSize(width, height)));
      return this;
    }

    /**
     * Adds a connection with default appearance.
     *
     * @param sourceId
     *          global source id
     * @param targetId
     *          global target id
     * @param label
     *          plain text connection label
     * @return this builder
     */
    public Builder edge(String sourceId, String targetId, String label) {
      return edge(sourceId, targetId, label, EdgeStyle.defaults());
    }

    /**
     * Adds a styled connection; forward and cross-group references are allowed.
     *
     * @param sourceId
     *          global source id
     * @param targetId
     *          global target id
     * @param label
     *          plain text connection label
     * @param style
     *          non-null connection appearance
     * @return this builder
     */
    public Builder edge(String sourceId, String targetId, String label, EdgeStyle style) {
      edges.add(new DefaultEdge(sourceId, targetId, label, style));
      return this;
    }

    /**
     * Adds a container using the nested title and default appearance.
     *
     * @param id
     *          globally unique group id
     * @param diagram
     *          non-null nested contents
     * @return this builder
     */
    public Builder group(String id, DiagramData diagram) {
      return group(id, diagram, GroupStyle.defaults());
    }

    /**
     * Captures nested contents immediately; resolves references at outer build().
     *
     * @param id
     *          globally unique group id
     * @param diagram
     *          non-null nested contents
     * @param style
     *          non-null container appearance
     * @return this builder
     */
    public Builder group(String id, DiagramData diagram, GroupStyle style) {
      var contents = DiagramValidator.capture(diagram);
      nodes.add(new DefaultGroup(id, contents.sectionName(), contents, style));
      return this;
    }

    /**
     * Describes a nested container with default appearance.
     *
     * @param id
     *          globally unique group id
     * @param title
     *          container title
     * @param contents
     *          callback executed immediately; references resolve at outer build()
     * @return this builder
     */
    public Builder group(String id, String title, Consumer<Builder> contents) {
      return group(id, title, GroupStyle.defaults(), contents);
    }

    /**
     * Describes a styled nested container and freezes its callback contents.
     *
     * @param id
     *          globally unique group id
     * @param title
     *          container title
     * @param style
     *          non-null container appearance
     * @param contents
     *          callback executed immediately; references resolve at outer build()
     * @return this builder
     */
    public Builder group(String id, String title, GroupStyle style, Consumer<Builder> contents) {
      if (contents == null)
        throw new IllegalArgumentException("group contents must not be null");
      var nested = new Builder(title);
      contents.accept(nested);
      return group(id, nested.raw(), style);
    }

    /**
     * Adds direct children in iteration order. Values are captured by build().
     *
     * @param nodes
     *          non-null collection of nodes or groups
     * @return this builder
     */
    public Builder nodes(Collection<? extends DiagramNode> nodes) {
      if (nodes == null)
        throw new IllegalArgumentException("nodes must not be null");
      this.nodes.addAll(nodes);
      return this;
    }

    /**
     * Adds connections in iteration order. Values are captured by build().
     *
     * @param edges
     *          non-null collection of connections
     * @return this builder
     */
    public Builder edges(Collection<? extends DiagramEdge> edges) {
      if (edges == null)
        throw new IllegalArgumentException("edges must not be null");
      this.edges.addAll(edges);
      return this;
    }

    /**
     * Captures the entire tree and validates global IDs and all connection endpoints.
     *
     * @return an immutable snapshot, independent of later builder or DTO changes
     * @throws IllegalArgumentException
     *           for invalid data, styles, duplicate ids or ownership cycles
     */
    public DiagramData build() {
      return DiagramValidator.snapshot(raw());
    }

    private DefaultDiagram raw() {
      return new DefaultDiagram(sectionName, nodes, edges);
    }
  }
}
