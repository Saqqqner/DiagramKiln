# DiagramKiln user guide

Start with the [README](../README.md) for installation and a runnable first diagram.
This guide covers advanced options and the export contracts.

## XML or compressed URL fragment

```java
var exporter = DrawioExporter.create();

String xml = exporter.toXml(diagram);
String fragment = exporter.toDrawioFragment(diagram); // #R...
String url = "https://app.diagrams.net/?splash=0" + fragment;
```

Both methods use the same settings and perform one layout per invocation. `toXml` returns a plain,
editable `.drawio` document. `toDrawioFragment` returns a URI-safe `#R...` fragment containing the
complete `mxfile` document. Do not apply another URLEncoder pass to the returned fragment.

For both outputs from the exact same captured input and layout, use:

```java
var result = exporter.export(diagram);
String xml = result.xml();
String fragment = result.drawioFragment();
```

`DrawioExport` stores two eagerly computed strings. One `export` invokes the configured
engine once, serializes XML once, and compresses that XML once. Reading either accessor,
even concurrently, performs no additional work. The result retains no DTO, layout engine,
DOM or native compressor. Each new export is independent; there is no diagram cache.
Existing `toXml` does not compress, and calling the two old methods separately still runs
layout twice. Custom engines retain their existing thread-safety responsibility.

Only `DrawioExporter.export` guarantees a matching pair. The public record constructor
rejects null components but accepts arbitrary non-null strings without parsing them.

Encoding is compatible with the draw.io decoder: UTF-8 XML → URI encoding → raw DEFLATE without
a zlib header → Base64 → URI encoding of Base64. The implementation uses JDK `URLEncoder` and
writes spaces in XML as `%20`. Additional punctuation escapes are valid for draw.io's decoder.
Inner URI encoding preserves literal percent signs, plus signs, Cyrillic text and emoji.
The result is a URL fragment, not an XML file with compressed `<diagram>` contents.
URL length limits depend on the browser and application; use XML files for large diagrams.
Generating a fragment makes no network requests.

The internal `DrawioXmlWriter` provides `write(...)` and `writeFragment(...)`; application code
should use `DrawioExporter`. See `examples/DrawioFragmentExample.java` for both outputs.
Encoding was checked against the [draw.io format documentation](https://github.com/jgraph/drawio/wiki/File-Format#compression).

## Nested groups and styles

```java
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.GroupStyle;
import io.github.saqqqner.diagramkiln.style.NodeShape;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

var operation = NodeStyle.builder()
    .fillColor("#E3F2FD").strokeColor("#1565C0").rounded(true).build();
var material = NodeStyle.builder()
    .shape(NodeShape.ELLIPSE).fillColor("#FFF3E0").build();
var section = GroupStyle.builder()
    .fillColor("#E8F5E9").headerHeight(36).bold(true).build();
var transfer = EdgeStyle.builder()
    .strokeColor("#EF6C00").dashed(true).endArrow(Arrow.OPEN).build();

DiagramData diagram = Diagrams.diagram("Iron pickaxe crafting")
    .group("mining", "Ore mining", section, g -> g
        .node("drill", "Drilling", operation)
        .node("ore", "Iron ore", material)
        .edge("drill", "ore", "Mine"))
    .group("smelting", "Smelting", section, g -> g
        .node("furnace", "Furnace", operation)
        .group("output", "Output", h -> h
            .node("ingot", "Iron ingot", material))
        .edge("furnace", "ingot", "Smelt"))
    .edge("ore", "furnace", "Transfer ore", transfer)
    .edge("mining", "smelting", "Next stage")
    .build();
```

Groups can nest recursively. Embed completed data with `group("id", nestedDiagram)` or
`group("id", nestedDiagram, groupStyle)`. The root `sectionName` is the page name. Add one
outer group to draw a frame around the complete process, as in `examples/NestedCraftingDiagram.java`.

All ids, including group ids, must be globally unique within the complete tree. Edge endpoints
may reference leaves or groups at any depth. Connection cycles, self-loops, parallel edges,
ancestor-descendant connections and empty groups are supported. Ownership cycles are rejected.

Group callbacks run immediately and their contents are captured when the callback returns.
References are resolved at the outer build(), allowing references to sibling groups declared later.
Calling a nested builder's own build() validates that independent diagram in its own context.

Styles are immutable and reusable:

| Style | Properties |
| --- | --- |
| `NodeStyle` | `shape`, `rounded`, `arcSize`, `fillColor`, `strokeColor`, `strokeWidth`, `dashed`, `fontColor`, `fontSize`, `bold` |
| `GroupStyle` | Header fill, outline and text appearance; additionally `headerHeight` |
| `EdgeStyle` | Line color/width/dashes, label color/size/bold, `startArrow`, `endArrow`, `endSize`, `rounded`, `arcSize`, `jumpStyle`, `jumpSize` |

Configure node and connection rounding separately:

```java
import io.github.saqqqner.diagramkiln.style.JumpStyle;

var status = NodeStyle.builder()
    .rounded(true).arcSize(12)
    .fillColor("#DEEBFF").strokeColor("#0052CC")
    .strokeWidth(2).fontSize(14).bold(true).build();
var transition = EdgeStyle.builder()
    .strokeColor("#34495E").strokeWidth(2)
    .endArrow(Arrow.CLASSIC).endSize(2)
    .rounded(true).arcSize(20)
    .jumpStyle(JumpStyle.GAP).jumpSize(6).build();
```

`NodeStyle.arcSize` is rectangle rounding in percent (0–100, default 15). The effective radius
is capped at half the smaller dimension; attachment anchors use that radius.
`EdgeStyle.arcSize` controls bend rounding in pixels (default 20). Both require `rounded(true)`;
node rounding does not apply to ellipses or diamonds. `endSize` controls the target arrowhead
(default 6) and is ignored for `Arrow.NONE`.

`JumpStyle` supports `NONE`, `ARC`, `GAP` and `SHARP`. draw.io renders jumps at crossings in
connection drawing order; jumps do not change ELK routing. `jumpSize` defaults to 6 and is
interpreted in whole pixels by draw.io. Arrowhead, jump and edge arc sizes must be finite
and nonnegative.

Shapes: `RECTANGLE`, `ELLIPSE`, `DIAMOND`. Arrowheads: `NONE`, `CLASSIC`, `BLOCK`, `OPEN`, `DIAMOND`.
Colors use `#RRGGBB`; fill and stroke also accept `none`. Dimensions are in pixels and font sizes
are in pixels. Stroke widths and font sizes must be finite and positive; `headerHeight=0` is valid.
DSL calls without an explicit style use defaults().

## Layout DSL

```java
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;

var layout = LayoutOptions.builder()
    .direction(Direction.RIGHT)
    .nodeSize(200, 80)
    .nodeSpacing(50)
    .layerSpacing(100)
    .groupPadding(24)
    .feedbackEdges(true)
    .preserveNodeOrder(true)
    .build();

var exporter = DrawioExporter.builder().layoutOptions(layout).build();
String xml = exporter.toXml(diagram);
```

Settings apply at every nesting level. `nodeSpacing` is the minimum gap between nodes in one
layer; `layerSpacing` is the minimum gap between successive layers along the flow direction.
ELK may enlarge gaps to accommodate routes. `groupPadding` is the inner padding of nonempty
groups; top padding is added below the header. Empty groups retain leaf dimensions plus header height.
Spacing and padding are finite nonnegative pixels; leaf dimensions are strictly positive.
Defaults: RIGHT, 160 by 60, spacing 40/80 and padding 20. Built settings are immutable.

`feedbackEdges` defaults to `false`. Set it to `true` to enable ELK Layered's
`org.eclipse.elk.layered.feedbackEdges` option at every nesting level. ELK assigns ordinary
input/output sides before cycle breaking, so backward transitions route around nodes.
For RIGHT flow, ordinary transitions enter from the left and leave from the right; the
sides rotate with the chosen direction. This does not force a single center attachment,
control self-loop ports or impose explicit constraints on group boundary ports. Custom
layout engines receive the flag and decide how to interpret it. Both the three-argument
and six-argument constructors remain available and disable feedback routing.

`preserveNodeOrder(true)` preserves declaration order of real nodes within a layer of each
connected component, including nested groups. For RIGHT flow this is top to bottom; for
DOWN flow, left to right. Layer assignment still follows the topology. This is not a global
node sequence, an edge/port ordering constraint or a disconnected-component packing rule.
The default is `false`. In the bundled ELK adapter it enables `NODES_AND_EDGES` model-order
preprocessing and forces node order during crossing minimization, potentially adding crossings.
Custom engines decide how to interpret the flag. Legacy constructors disable node-order
preservation.

The direction(), nodeSize() methods and the three-argument LayoutOptions constructor remain
available. The last exporter-builder setting wins: layoutOptions() replaces all fields, while a
subsequent direction() or nodeSize() call overrides only the respective fields.

Style semantics were checked against [draw.io](https://www.drawio.com/docs/manual/styles/connector-styles/)
and [mxGraph](https://jgraph.github.io/mxgraph/docs/js-api/files/util/mxConstants-js.html).

## Advanced Layered settings and leaf constraints

```java
import io.github.saqqqner.diagramkiln.config.Alignment;
import io.github.saqqqner.diagramkiln.config.CycleBreaking;
import io.github.saqqqner.diagramkiln.config.LayerConstraint;
import io.github.saqqqner.diagramkiln.config.NodeLayout;
import io.github.saqqqner.diagramkiln.config.NodePlacement;

var layout = LayoutOptions.builder().direction(Direction.RIGHT).nodeSize(180, 60)
    .nodeSpacing(80).layerSpacing(75)
    .edgeNodeSpacing(80).edgeSpacing(80).edgeSpacingBetweenLayers(80).portSpacing(20)
    .feedbackEdges(true).preserveNodeOrder(true)
    .layered(l -> l.cycleBreaking(CycleBreaking.DFS_NODE_ORDER)
        .nodePlacement(NodePlacement.NETWORK_SIMPLEX)
        .alignment(Alignment.CENTER).thoroughness(50))
    .build();

var first = NodeLayout.builder().layerConstraint(LayerConstraint.FIRST).build();
var diagram = Diagrams.diagram("Statuses")
    .node("created", "Created", NodeStyle.defaults(), first)
    .node("draft", "Draft")
    .edge("created", "draft", "")
    .build();
String xml = DrawioExporter.builder().layoutOptions(layout).build().toXml(diagram);
```

The four new distances are finite nonnegative pixels and default to 10, matching ELK 0.11.0.
`edgeNodeSpacing` and `edgeSpacing` set within-layer edge clearances;
`edgeSpacingBetweenLayers` sets edge-to-edge clearance between layers. These are separate
ELK settings, not universal minimum distances for every route segment. `portSpacing` affects
port distribution; a fixed node size can limit the feasible spacing.

Layered defaults remain `GREEDY`, `BRANDES_KOEPF`, `AUTOMATIC` and thoroughness `7`.
All nine ELK 0.11.0 cycle breakers, five node placement strategies and six alignments are
available as library enums. The `INTERACTIVE` strategies depend on existing coordinates;
this DSL does not supply initial coordinates and therefore does not offer interactive
layout preservation. Alignment follows ELK's alignment option and is not a port anchor.
Higher thoroughness increases search effort and may increase execution time.

The `layered(...)` callback receives a fresh builder and is captured immediately.
Each call replaces the previous Layered settings; `layeredOptions(...)` accepts an already
built immutable value. Custom layout engines receive all settings and decide how to use them.

`NodeLayout` supports `NONE` (default), `FIRST` and `LAST`, relative to the leaf's
containing graph and flow direction. ELK's internal separate-layer variants are not exposed.
Use constraints with model-order cycle breakers, such as `DFS_NODE_ORDER`; incompatible
constraints/topology can fail during layout. A source constrained to FIRST is not automatically
detected: application code selects the initial statuses. When classifying sources, ignore
self-loops and require at least one outgoing non-self transition to avoid marking isolated
statuses as initial.

For application DTOs, implement `LayoutDiagramNode` and return non-null immutable
`NodeLayout` from `layout()`. It can be combined with `SizedDiagramNode`; capture reads
each property once and preserves both size and layout metadata. Leaf settings on
`DiagramGroup` implementations are rejected. Existing `DiagramNode` implementations and
node overloads remain unchanged. Explicitly sized DSL leaves accept settings as the last
argument: `node(id, label, style, width, height, nodeLayout)`.

## Individual node sizes

Use `.node(id, label, width, height)` or `.node(id, label, style, width, height)` to set
an explicit size for one leaf. Both dimensions are finite positive pixels. The full pair
replaces the global size from `LayoutOptions`; ordinary leaves keep the global fallback.

Application DTOs can opt in without changing existing interfaces or record constructors:

```java
import io.github.saqqqner.diagramkiln.SizedDiagramNode;
import io.github.saqqqner.diagramkiln.dto.NodeSize;

record Service(String id, String label, NodeSize size) implements SizedDiagramNode {}
```

For example, `new Service("api", "API", new NodeSize(240, 90))` describes a sized leaf.
`SizedNode` is the supplied DTO, with constructors accepting ID, label, size and optionally
style. Snapshots preserve the immutable size, including nested and repeated capture.
A null size from a custom DTO is rejected before layout with its node ID in the error.

Groups grow around their contents; a group implementing `SizedDiagramNode` is rejected.
Empty groups retain their existing global-size behavior. The bundled ELK adapter preserves
explicit leaf dimensions for every shape and direction. A custom `LayoutEngine` can inspect
the capability and choose its own valid final bounds: exported geometry remains authoritative.
No post-layout resizing is performed, so routed connections retain their attachment points.

## Application DTOs

```java
public record StatusNodeDto(String id, String label) implements DiagramNode {}

public record StatusTransitionDto(String sourceId, String targetId, String label)
    implements DiagramEdge {}

public record StatusDiagramData(
    String sectionName,
    List<StatusNodeDto> nodes,
    List<StatusTransitionDto> edges
) implements DiagramData {
  public StatusDiagramData {
    nodes = nodes == null ? List.of() : List.copyOf(nodes);
    edges = edges == null ? List.of() : List.copyOf(edges);
  }
}
```

Place each public record in its own file. Import interfaces from `io.github.saqqqner.diagramkiln` and
List from java.util. DiagramData returns wildcard lists, so concrete application list types fit.
Pass completed data directly to toXml(data) or toDrawioFragment(data); the DSL is optional.
DiagramNode.style() and DiagramEdge.style() have default implementations. Override them or add
a style record component to supply custom appearance. Groups implement DiagramGroup with id(),
diagram() and optionally groupStyle(); the default label comes from the nested sectionName().

Build and export capture deep immutable snapshots. Do not mutate DTOs during capture. Later changes
to source DTOs or builders do not affect completed results. Directly constructing `DefaultDiagram`
copies its lists; arbitrary objects in those lists are captured later by build or export.

Capture preserves the public input properties, including `SizedDiagramNode.size()` and
`LayoutDiagramNode.layout()`. It does not retain application DTO classes or arbitrary additional
properties. Custom engines should use the supported input contracts; keep any external
configuration they need immutable and resolve it by input ID.

Input IDs identify nodes and groups within the diagram. Exported XML uses generated cell IDs
based on traversal order, so those IDs can change when elements are inserted or reordered.
The exporter does not merge previous XML or preserve manual edits from an earlier export.
Connection routes are matched by traversal index, not by label or by a persistent edge ID.

## Custom layout

The default engine is ELK Layered with RIGHT direction and 160 by 60 leaves. DOWN, LEFT and UP
are also available. Containers grow to fit children, padding and headers.

A custom layout.LayoutEngine receives DiagramData and config.LayoutOptions. It returns a
LayoutResult with one PositionedNode per leaf **and group**, and one RoutedEdge per connection.
Rectangles and Points use absolute page coordinates in pixels. First/last route points must lie
on the visible endpoint boundaries; intermediate points are bends. Edge indices follow root edges()
first, then nested edges recursively in group order. The exporter validates geometry and converts
it into parent-relative draw.io coordinates. Internal edges belong to their common container and
move with it. A custom engine controls the final bounds and interprets the options itself;
it must return every node, group and route exactly once, even if it ignores a layout preference.

[CustomLayoutDiagram.java](../examples/CustomLayoutDiagram.java) is a complete runnable example.
Its engine deliberately supplies fixed geometry for the included workflow only; it is not a
general-purpose layout algorithm. All positions are absolute: the group starts at `(100, 100)`,
its first leaf at `(140, 160)`, and the exported leaf geometry is therefore `(40, 60)`.
The root connection has route index `0`, although the group is declared first. The connection
declared inside that group has index `1`. Both routes include their boundary endpoints.

After building the development JAR and copying runtime dependencies as described below, run
from the repository root on Windows:

```powershell
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.2.0-SNAPSHOT.jar;target/dependency/*" -d target/examples examples/CustomLayoutDiagram.java
java -cp "target/examples;target/diagram-kiln-1.2.0-SNAPSHOT.jar;target/dependency/*" CustomLayoutDiagram target/custom-layout.drawio
```

On Linux/macOS, replace the classpath separator `;` with `:`. For a different built version,
replace the JAR filename. The example writes UTF-8 XML and a matching `.url` file and creates
the output directory if needed. Open the `.drawio` file in draw.io. The nested connection
belongs to the group; the connection to the outside node belongs to the root layer.

Invalid custom geometry fails before XML is returned. For example, omitting the outside node
from the example result causes `IllegalArgumentException: Layout node count does not match
diagram`. Custom-engine exceptions are otherwise propagated. Engine implementations own
their mutable state and thread-safety policy; exporter reuse does not make a custom engine
thread-safe.

## Edge-label-aware layout (1.2.0)

Edge-label placement is disabled by default. Enable it with a fallback reserved size, and
optionally give individual edges their own sizes:

```java
var diagram = Diagrams.diagram("Release")
    .node("draft", "Draft").node("approved", "Approved")
    .edge("draft", "approved", "Review requirements\nApprove release", EdgeStyle.defaults(),
        new EdgeLabelSize(240, 50))
    .build();
var exporter = DrawioExporter.builder().edgeLabels(new EdgeLabelSize(160, 30)).build();
var result = exporter.export(diagram);
```

Import `EdgeLabelSize` from `io.github.saqqqner.diagramkiln.dto`. Both dimensions must be finite,
positive pixels. The bundled ELK engine reserves this space during layout and returns label
bounds; enabled layout may change node positions, group bounds and routes. The XML keeps labels
editable on their original edges. Existing calls retain their previous behavior and geometry.

Sizes are caller estimates, not font measurements. Use enough space for the actual text and
font; explicit newlines are supported, but width does not guarantee automatic word wrapping.
Underestimated boxes can overflow, and layout does not guarantee elimination of every overlap.
An empty string creates no label box. Whitespace-only labels remain labels.

`SizedDiagramEdge.labelSize()` supplies per-edge metadata for application DTOs; `SizedEdge` is
the ready-made immutable implementation. Capture reads this capability once and preserves it.
A missing size is invalid even when placement is disabled or the label is empty. Unsized edges
use the required exporter fallback. The five-argument DSL overload avoids ambiguity with the
existing four-argument style overload.

The last `edgeLabels(size)` or `withoutEdgeLabels()` call wins. Setting the engine, direction,
node size or layout options does not reset this choice. Already-built exporters retain their
settings independently of subsequent builder changes.

Custom engines can override `LayoutEngine.layoutWithEdgeLabels(diagram, options, fallbackSize)`.
The result is `LabeledLayoutResult`, containing the existing `LayoutResult` and one
`PositionedEdgeLabel` for each nonempty label. Bounds are absolute page coordinates; indices
follow the existing root-first edge traversal. Returned dimensions are authoritative. Labels
owned by a group must fit its content area below the header, including when an edge connects
a group to a descendant. Invalid coverage, bounds or overflowing derived offsets fail before
XML is returned. Root-owned labels have no artificial page bounds.

Existing custom engines and lambdas still work with placement disabled. Enabling it on an
engine that does not override the new operation throws `UnsupportedOperationException` instead
of silently ignoring the request.

[EdgeLabelDiagram.java](../examples/EdgeLabelDiagram.java) is a complete synthetic example with
nested, self-loop and parallel connections. After building the development JAR and copying
runtime dependencies, run on Windows:

```powershell
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.2.0-SNAPSHOT.jar;target/dependency/*" -d target/examples examples/EdgeLabelDiagram.java
java -cp "target/examples;target/diagram-kiln-1.2.0-SNAPSHOT.jar;target/dependency/*" EdgeLabelDiagram target/edge-labels.drawio
```

Use `:` instead of `;` on Linux/macOS. The example writes matching UTF-8 XML and a `.url` file.

## Errors and limitations

IllegalArgumentException covers null required values, blank/duplicate ids, unknown endpoints,
invalid XML 1.0 characters, invalid styles/dimensions and invalid custom layout geometry. Empty
labels are allowed. `LayoutResult` and `RoutedEdge` reject null lists and null list elements
immediately with `IllegalArgumentException` identifying the field (and route index for points).
Custom layout engines may propagate their own exceptions; the exporter does not translate
arbitrary engine failures. XML infrastructure failures are wrapped in IllegalStateException.

Each export contains one page. Labels are escaped plain text, without HTML interpretation.
Leaf bounds are fixed; increase the individual size or global nodeSize for longer text or larger
fonts. Automatic text measurement and global collision elimination are not implemented. Practical
nesting depth is limited by JVM resources. The default exporter is reusable; custom layout
engines own their thread-safety contract.


## Run the repository examples

Run these commands from the 1.1.0 release checkout (Windows); on dev, use the
matching snapshot JAR filename:

```powershell
./mvnw.cmd package dependency:copy-dependencies "-DincludeScope=runtime"
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.1.0.jar;target/dependency/*" -d target/examples examples/NestedCraftingDiagram.java
java -cp "target/examples;target/diagram-kiln-1.1.0.jar;target/dependency/*" NestedCraftingDiagram
```

On Linux/macOS, use `bash ./mvnw` and replace the classpath separator `;` with `:`.
The example writes `examples/nested-crafting.drawio`; pass an output path as its first
argument to select another destination. Other examples are listed in the [README](../README.md#examples-and-documentation).
