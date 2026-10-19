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
are in points. Stroke widths and font sizes must be finite and positive; `headerHeight=0` is valid.
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

The direction(), nodeSize() methods and the three-argument LayoutOptions constructor remain
available. The last exporter-builder setting wins: layoutOptions() replaces all fields, while a
subsequent direction() or nodeSize() call overrides only the respective fields.

Style semantics were checked against [draw.io](https://www.drawio.com/docs/manual/styles/connector-styles/)
and [mxGraph](https://jgraph.github.io/mxgraph/docs/js-api/files/util/mxConstants-js.html).

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
to source DTOs or builders do not affect completed results.

## Custom layout

The default engine is ELK Layered with RIGHT direction and 160 by 60 leaves. DOWN, LEFT and UP
are also available. Containers grow to fit children, padding and headers.

A custom layout.LayoutEngine receives DiagramData and config.LayoutOptions. It returns a
LayoutResult with one PositionedNode per leaf **and group**, and one RoutedEdge per connection.
Rectangles and Points use absolute page coordinates in pixels. First/last route points must lie
on the visible endpoint boundaries; intermediate points are bends. Edge indices follow root edges()
first, then nested edges recursively in group order. The exporter validates geometry and converts
it into parent-relative draw.io coordinates. Internal edges belong to their common container and
move with it.

## Errors and limitations

IllegalArgumentException covers null required values, blank/duplicate ids, unknown endpoints,
invalid XML 1.0 characters, invalid styles/dimensions and invalid custom layout geometry. Empty
labels are allowed. XML infrastructure failures are wrapped in IllegalStateException.

Each export contains one page. Labels are escaped plain text, without HTML interpretation.
Leaf bounds are fixed; increase nodeSize for longer text or larger fonts. Automatic text
measurement and independent edge-label overlap optimization are not implemented. Practical
nesting depth is limited by JVM resources. The default exporter is reusable; custom layout
engines own their thread-safety contract.


## Run the repository examples

Run these commands from the project root (Windows):

```powershell
./mvnw.cmd package dependency:copy-dependencies "-DincludeScope=runtime"
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.1.0-SNAPSHOT.jar;target/dependency/*" -d target/examples examples/NestedCraftingDiagram.java
java -cp "target/examples;target/diagram-kiln-1.1.0-SNAPSHOT.jar;target/dependency/*" NestedCraftingDiagram
```

On Linux/macOS, use `bash ./mvnw` and replace the classpath separator `;` with `:`.
The example writes `examples/nested-crafting.drawio`; pass an output path as its first
argument to select another destination. Other examples are listed in the [README](../README.md#examples-and-documentation).
