package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
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
  private final EdgeLabelSize edgeLabelSize;

  private DrawioExporter(LayoutEngine layout, LayoutOptions options, EdgeLabelSize edgeLabelSize) {
    this.layout = layout;
    this.options = options;
    this.edgeLabelSize = edgeLabelSize;
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
   * @throws UnsupportedOperationException
   *           if label placement is enabled but the engine does not support it
   */
  public String toXml(DiagramData diagram) {
    var snapshot = DiagramValidator.snapshot(diagram);
    if (edgeLabelSize != null) {
      var result = layout.layoutWithEdgeLabels(snapshot, options, edgeLabelSize);
      return new DrawioXmlWriter().write(snapshot, result);
    }
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
   * @throws UnsupportedOperationException
   *           if label placement is enabled but the engine does not support it
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
   * @throws UnsupportedOperationException
   *           if label placement is enabled but the engine does not support it
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
    private LayoutOptions.Builder options = LayoutOptions.builder();
    private EdgeLabelSize edgeLabelSize;

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
     * Enables explicit-size edge-label placement. Per-edge dimensions override this fallback.
     * Existing custom engines must implement the labeled operation. Sizes reserve space; they do
     * not measure text or guarantee automatic word wrapping.
     *
     * @param fallbackSize
     *          non-null reserved dimensions for labels without an explicit size
     * @return this builder
     * @throws IllegalArgumentException
     *           if fallbackSize is null
     */
    public Builder edgeLabels(EdgeLabelSize fallbackSize) {
      if (fallbackSize == null)
        throw new IllegalArgumentException("edge label fallback size must not be null");
      edgeLabelSize = fallbackSize;
      return this;
    }

    /**
     * Disables label-aware placement, restoring the default operation. The last call to this method
     * or edgeLabels wins; other builder settings do not reset this choice.
     *
     * @return this builder
     */
    public Builder withoutEdgeLabels() {
      edgeLabelSize = null;
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
      options.direction(direction);
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
      options.nodeSize(width, height);
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
      this.options = LayoutOptions.builder().direction(options.direction())
          .nodeSize(options.nodeWidth(), options.nodeHeight()).nodeSpacing(options.nodeSpacing())
          .layerSpacing(options.layerSpacing()).groupPadding(options.groupPadding())
          .feedbackEdges(options.feedbackEdges()).preserveNodeOrder(options.preserveNodeOrder())
          .edgeNodeSpacing(options.edgeNodeSpacing()).edgeSpacing(options.edgeSpacing())
          .edgeSpacingBetweenLayers(options.edgeSpacingBetweenLayers())
          .portSpacing(options.portSpacing()).layeredOptions(options.layered());
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
      return new DrawioExporter(layout, options.build(), edgeLabelSize);
    }
  }
}
