# DiagramKiln

Generate editable draw.io diagrams from Java code. Describe nodes and connections using
a fluent DSL or your own DTOs; DiagramKiln places them with ELK and exports a `.drawio` file.

Useful for status transitions, workflows and diagrams made of interconnected subsystems.
Requires **Java 17+**.

- Automatic placement and connection routing.
- Nested groups, with connections between nodes or groups at any depth.
- Reusable styles for shapes, colors, arrows and line crossings.
- File export as XML or a compressed `#R` fragment for opening a diagram by URL.
- Plain Java input contracts; no application framework required.

## Install

The current version is **1.0.0**. It is not yet published to a public Maven repository.
First build and install it from the project root:

```powershell
./mvnw.cmd clean install
```

On Linux/macOS:

```sh
bash ./mvnw clean install
```

Then add this dependency to your application's `pom.xml`:

```xml
<dependency>
  <groupId>io.github.saqqqner</groupId>
  <artifactId>diagram-kiln</artifactId>
  <version>1.0.0</version>
</dependency>
```

Maven brings in ELK and the other runtime dependencies automatically.

## Create your first diagram

Save this as `OrderDiagram.java` in your application and run its `main` method:

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.github.saqqqner.diagramkiln.Diagrams;
import io.github.saqqqner.diagramkiln.DrawioExporter;

public class OrderDiagram {
  public static void main(String[] args) throws Exception {
    var diagram = Diagrams.diagram("Order statuses")
        .node("new", "New")
        .node("paid", "Paid")
        .node("shipped", "Shipped")
        .edge("new", "paid", "Payment received")
        .edge("paid", "shipped", "Dispatch")
        .build();

    String xml = DrawioExporter.create().toXml(diagram);
    Files.writeString(Path.of("order.drawio"), xml, StandardCharsets.UTF_8);
  }
}
```

Open `order.drawio` in draw.io using **File → Open From → Device**.
Positions and routes are calculated automatically. You can edit the resulting nodes,
connections and labels in draw.io.

## Group related elements

Groups can contain nodes and other groups. Connections can cross group boundaries
or connect the groups themselves:

```java
var diagram = Diagrams.diagram("Order processing")
    .group("sales", "Sales", group -> group
        .node("new", "New order")
        .node("paid", "Paid")
        .edge("new", "paid", "Payment"))
    .group("warehouse", "Warehouse", group -> group
        .node("packed", "Packed")
        .node("shipped", "Shipped")
        .edge("packed", "shipped", "Dispatch"))
    .edge("paid", "packed", "Prepare order")
    .edge("sales", "warehouse", "Handover")
    .build();
```

Node and group IDs must be unique across the whole diagram. Use IDs for connections;
labels are the text shown to the reader. Groups can reference nodes declared later.

## Customize appearance and layout

Create styles once and reuse them across diagrams:

```java
import io.github.saqqqner.diagramkiln.config.Direction;
import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.style.Arrow;
import io.github.saqqqner.diagramkiln.style.EdgeStyle;
import io.github.saqqqner.diagramkiln.style.JumpStyle;
import io.github.saqqqner.diagramkiln.style.NodeStyle;

var status = NodeStyle.builder()
    .fillColor("#DEEBFF").strokeColor("#0052CC")
    .rounded(true).arcSize(12)
    .fontSize(14).bold(true)
    .build();

var transition = EdgeStyle.builder()
    .strokeColor("#34495E").strokeWidth(2)
    .endArrow(Arrow.CLASSIC).endSize(6)
    .rounded(true).jumpStyle(JumpStyle.GAP).jumpSize(6)
    .build();

var diagram = Diagrams.diagram("Order statuses")
    .node("new", "New", status)
    .node("paid", "Paid", status)
    .edge("new", "paid", "Payment", transition)
    .build();

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

Layout defaults are left-to-right flow, 160 × 60 pixel nodes, 40 pixel node spacing,
80 pixel layer spacing and 20 pixel group padding. Increase node size for long labels;
text does not resize nodes automatically.

See the [user guide](docs/USER-GUIDE.md) for all style options, group styling and layout details.

## Use your own data

The DSL is optional. Implement `DiagramNode` (`id`, `label`), `DiagramEdge`
(`sourceId`, `targetId`) and `DiagramData` (`sectionName`, `nodes`, `edges`) with your own
classes or records, then pass the data to `exporter.toXml(data)`.

Styles and edge labels have defaults. Use `DiagramGroup` for nested data.
See the [DTO example](docs/USER-GUIDE.md#application-dtos).

## Open a diagram by URL

```java
String fragment = exporter.toDrawioFragment(diagram);
String url = "https://app.diagrams.net/?splash=0" + fragment;
```

The returned fragment is ready to append to the URL; do not encode it again.
Generation runs locally. For large diagrams, use `.drawio` files to avoid URL length limits.

## Examples and documentation

- [Order statuses](examples/OrderStatusDiagram.java): a flat diagram.
- [Nested crafting process](examples/NestedCraftingDiagram.java): groups and cross-group connections.
- [Thirty-node diagram](examples/ThirtyNodeDiagram.java): a larger example.
- [Visual styles](examples/VisualStylesDiagram.java): shapes, arrows and line crossings.
- [XML and URL export](examples/DrawioFragmentExample.java): both output formats.
- [User guide](docs/USER-GUIDE.md): API details, custom layouts and validation rules.
- [Validation report](docs/VALIDATION.md): checks and browser previews.

Each export creates one page. Labels are plain text. Node sizes are fixed; automatic
text sizing and separate edge-label overlap optimization are not provided.

## Development

Run `./mvnw.cmd clean verify` on Windows or `bash ./mvnw clean verify` on Linux/macOS.
The build checks tests, an isolated Maven consumer, strict Javadoc and formatting.
Format Java with the `formatter:format` goal. See [code style](docs/CODE-STYLE.md) and
[release instructions](docs/PUBLISHING.md).

Public API packages start with `io.github.saqqqner.diagramkiln`; `internal` is not an application API.
Generated API documentation is available at `target/reports/apidocs/index.html` after a build.

## License

[MIT](LICENSE).
