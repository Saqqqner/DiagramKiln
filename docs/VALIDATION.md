# Validation report

Verified locally on October 19, 2026: Windows 11, Temurin JDK 21.0.12.1 and Maven Wrapper 3.9.11.
Compilation targets Java 17 through --release 17. CI is configured for JDK 17/21; the remote
workflow has not run. Public Javadoc is in English.

Release coordinates: `io.github.saqqqner:diagram-kiln:1.0.0`. Packages and automatic module name use
`io.github.saqqqner.diagramkiln`. The release clean install passed 70 tests, strict Javadoc, format
validation and an isolated Maven consumer of version 1.0.0. All five Java examples compiled
against the release JAR; the 30-node scenario also ran against that JAR.

## Thirty-node scenario

examples/ThirtyNodeDiagram.java builds 30 leaves, seven containers across two levels, and 32
connections: local edges, cross-group edges, a container connection, a loop and a parallel edge.
It uses all three node shapes, rounded corners, fills and dashed transitions.

Measured in a separate local JVM process using the release JAR:

| Metric | Result |
| --- | --- |
| First toXml invocation, including ELK initialization | 1092.0 ms |
| Median of five subsequent invocations | 25.8 ms |
| UTF-8 XML | 39,589 bytes |
| Leaves / containers / connections | 30 / 7 / 32 |

These are measurements of this fixture on the local machine, not performance guarantees for
arbitrary graphs. Direction, topology, edge count and nesting affect ELK cost. The JUnit test
checks element counts, unique ids, parent/source/target references and finite geometry.
The release run produced target/release-thirty.drawio; the checked-in example is
examples/thirty-nodes.drawio.

The XML model was opened in draw.io. All 30 leaves and seven containers display, including
cross-group edges, a loop and parallel transitions. Screenshot: examples/thirty-nodes-preview.png.
This topology produces a tall layout, requiring a small zoom for an overview. ELK does not
guarantee compact placement for arbitrary topologies.

To repeat after package dependency:copy-dependencies with runtime scope:

```powershell
javac --release 17 -encoding UTF-8 -cp "target/diagram-kiln-1.0.0.jar;target/dependency/*" -d target/examples examples/ThirtyNodeDiagram.java
java -cp "target/examples;target/diagram-kiln-1.0.0.jar;target/dependency/*" ThirtyNodeDiagram
```

## Automated checks

- 70 main-project tests cover DSL construction, snapshots, nesting, styles, real ELK, XML,
  invalid input and custom geometry.
- An isolated Maven consumer verifies the packaged JAR and transitive runtime dependencies.
- doclint=all, failOnWarnings=true and Java format validation.
- Dense parallel routes verify rounded boundaries at arcSize 0, 12, 15, 35 and 100, including
  the effective radius cap at half the smaller dimension.
- All JumpStyle values, arrow/jump sizes, rounded bends, ELK route preservation and legacy
  style/layout constructors are covered.
- Real ELK tests verify node/layer spacing and group/header padding. Other tests verify immutable
  layout settings, validation and setter precedence.
- Independent read-only reviews of architecture, API, XML, encoding, tests and packaging found
  no actionable issues.

XML models are inspected through Extras → Edit Diagram. Automated XML parsing additionally
checks the complete mxfile envelope. Automatic text measurement and independent edge-label
collision optimization are outside the current implementation.

## Browser checks: styles and LayoutOptions

examples/VisualStylesDiagram.java runs against the packaged JAR and produces two fixtures.
Both were imported through Edit Diagram and visually inspected on October 19, 2026:

- examples/visual-layout.drawio uses real ELK, DOWN direction, 180 by 60 leaves, spacing 50/90
  and group padding 30. It shows rounding values 12/45, local transitions, a connection between
  leaves in different groups and a separate blue container connection.
  Screenshot: examples/visual-layout-preview.png.
- examples/visual-jumps.drawio uses identical fixed geometry for NONE/ARC/GAP/SHARP comparisons.
  Curved bridges, gaps and angular bridges display, along with green rounded bends (arcSize=30)
  and different target arrow sizes (2/12). Screenshot: examples/visual-jumps-preview.png.

The crossing fixture deliberately uses a custom LayoutEngine to guarantee intersections. The
other fixture and automated tests cover actual ELK settings. No implementation changes were
needed after these visual checks.

## XML and #R output

DrawioFragmentTest independently inflates raw DEFLATE with JDK Inflater and checks equality with
the source XML, including nested groups, styles, Cyrillic text, emoji, literal %20, percent signs
and plus signs. Empty diagrams, 120-node inputs, supplied/invalid geometry and one layout call
per fragment are covered.

examples/DrawioFragmentExample.java was run against the packaged JAR after URI encoding was
simplified. XML: 2613 characters; fragment: 1252. The generated URL in
examples/fragment-example-url.txt was opened directly in draw.io without manual XML import.
Both groups, their connection, literal 100% +, %20, <OK>, emoji, line breaks and punctuation
! ' ( ) ~ * ? # / = and double quotes display correctly.
Screenshot: examples/fragment-example-preview.png.

The build checks 50 Java files after package-info removal. Local release artifacts are installed
in the local Maven repository. No remote publication was performed.
