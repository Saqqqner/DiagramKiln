package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.config.LayeredLayoutOptions;
import io.github.saqqqner.diagramkiln.internal.validation.DiagramValidator;
import io.github.saqqqner.diagramkiln.internal.xml.DrawioXmlWriter;
import io.github.saqqqner.diagramkiln.layout.ElkLayout;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;

/**
 * Converts Java diagram data to editable draw.io XML or a compressed #R URL fragment. Inputs are
 * validated and snapshotted for each export; callers must not mutate DTOs during capture. Reusable
 * with the default ELK adapter. A custom LayoutEngine owns its thread-safety contract.
 */
public final class DrawioExporter {
  private final LayoutEngine layout;
  private final LayoutOptions options;

  private DrawioExporter(LayoutEngine layout, LayoutOptions options) {
    this.layout = layout;
    this.options = options;
  }

  /**
   * {@return a reusable exporter using ELK, RIGHT direction and 160 by 60 pixel nodes}
   */
  public static DrawioExporter create() {
    return builder().build();
  }

  /**
   * {@return a mutable configuration builder with the default export settings}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns XML with a UTF-8 declaration. Save it using UTF-8.
   *
   * @param diagram
   *          non-null tree with globally unique ids
   * @return an uncompressed, editable single-page draw.io document
   * @throws IllegalArgumentException
   *           if the input graph or returned layout geometry is invalid
   */
  public String toXml(DiagramData diagram) {
    var snapshot = DiagramValidator.snapshot(diagram);
    var result = layout.layout(snapshot, options);
    return new DrawioXmlWriter().write(snapshot, result);
  }

  /**
   * Returns a URI-safe #R fragment containing the complete draw.io document, using the same layout
   * settings as toXml. Performs one fresh layout per invocation. Append the result to the editor
   * URL; no network request or file upload is performed by this method. The fragment contains the
   * diagram data and is not encrypted. Large diagrams may exceed browser or application URL limits;
   * use toXml for file export in that case.
   *
   * @param diagram
   *          non-null tree with globally unique ids
   * @return #R followed by URI-escaped Base64 of raw-DEFLATE-compressed URI-encoded XML
   * @throws IllegalArgumentException
   *           if the input graph or returned layout geometry is invalid
   */
  public String toDrawioFragment(DiagramData diagram) {
    return new DrawioXmlWriter().writeFragment(toXml(diagram));
  }

  /**
   * Eagerly creates matching XML and a URI-safe draw.io fragment from one layout invocation and one
   * XML serialization. No file or network I/O is performed. Each call is independent; reading the
   * result performs no further layout or compression. Custom engines retain their existing
   * thread-safety responsibility. Call {@link #toXml} when only XML is needed, to avoid
   * compression.
   *
   * @param diagram
   *          non-null tree with globally unique ids; do not mutate during capture
   * @return two immutable strings describing the same captured input and geometry
   * @throws IllegalArgumentException
   *           if input or layout geometry is invalid
   */
  public DrawioExport export(DiagramData diagram) {
    String xml = toXml(diagram);
    return new DrawioExport(xml, new DrawioXmlWriter().writeFragment(xml));
  }

  /**
   * Mutable exporter configuration. Completed exporters retain their settings.
   */
  public static final class Builder {
    private LayoutEngine layout = new ElkLayout();
    private Direction direction = Direction.RIGHT;
    private double width = 160;
    private double height = 60;
    private double nodeSpacing = 40;
    private double layerSpacing = 80;
    private double groupPadding = 20;
    private boolean feedbackEdges = false;
    private boolean preserveNodeOrder = false;
    private double edgeNodeSpacing = 10;
    private double edgeSpacing = 10;
    private double edgeSpacingBetweenLayers = 10;
    private double portSpacing = 10;
    private LayeredLayoutOptions layered = LayeredLayoutOptions.defaults();

    private Builder() {
    }

    /**
     * Selects the layout implementation.
     *
     * @param layout
     *          non-null engine; custom implementations own thread safety
     * @return this builder
     */
    public Builder layout(LayoutEngine layout) {
      this.layout = layout;
      return this;
    }

    /**
     * Sets the main layout direction.
     *
     * @param direction
     *          non-null direction
     * @return this builder
     */
    public Builder direction(Direction direction) {
      this.direction = direction;
      return this;
    }

    /**
     * Sets leaf bounds; containers grow to fit contents and headers.
     *
     * @param width
     *          finite positive width in pixels
     * @param height
     *          finite positive height in pixels
     * @return this builder
     */
    public Builder nodeSize(double width, double height) {
      this.width = width;
      this.height = height;
      return this;
    }

    /**
     * Replaces all layout settings. Subsequent direction/nodeSize calls override those fields; a
     * subsequent layoutOptions call replaces all fields again.
     *
     * @param options
     *          non-null immutable layout settings
     * @return this builder
     * @throws IllegalArgumentException
     *           if options is null
     */
    public Builder layoutOptions(LayoutOptions options) {
      if (options == null)
        throw new IllegalArgumentException("layout options must not be null");
      direction = options.direction();
      width = options.nodeWidth();
      height = options.nodeHeight();
      nodeSpacing = options.nodeSpacing();
      layerSpacing = options.layerSpacing();
      groupPadding = options.groupPadding();
      feedbackEdges = options.feedbackEdges();
      preserveNodeOrder = options.preserveNodeOrder();
      edgeNodeSpacing = options.edgeNodeSpacing();
      edgeSpacing = options.edgeSpacing();
      edgeSpacingBetweenLayers = options.edgeSpacingBetweenLayers();
      portSpacing = options.portSpacing();
      layered = options.layered();
      return this;
    }

    /**
     * Validates configuration and creates a reusable exporter.
     *
     * @return the configured exporter
     * @throws IllegalArgumentException
     *           for null configuration or invalid dimensions
     */
    public DrawioExporter build() {
      if (layout == null)
        throw new IllegalArgumentException("layout engine must not be null");
      return new DrawioExporter(layout,
          new LayoutOptions(direction, width, height, nodeSpacing, layerSpacing, groupPadding,
              feedbackEdges, preserveNodeOrder, edgeNodeSpacing, edgeSpacing,
              edgeSpacingBetweenLayers, portSpacing, layered));
    }
  }
}
