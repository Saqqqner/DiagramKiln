# Validation report

## Version 1.1.0

### Release preparation

The release uses `io.github.saqqqner:diagram-kiln:1.1.0`; Java packages and automatic module
name remain `io.github.saqqqner.diagramkiln`. A clean local JDK 21 verification passed 122 main tests,
three packaged-consumer tests, strict Javadoc and formatting of 70 Java files. Both complete
README examples compiled with `--release 17` and ran against the release JAR; public signatures
of all 46 non-internal classes match the preceding development build. Independent diff review
found no actionable findings.

Publication preparation uses a profile with `skipUpload=true` and manual final publication.
No Central upload or signing-key validation was performed. Namespace verification and signing
remain prerequisites. Local privacy checks covered published branch/tag history, tracked and
proposed files, the release JAR contents and all six example previews. Historical Git identities
and root copyright attribution were rewritten with the owner's authorization. Old backups,
reflogs, private app refs and external clones/caches are outside the published-ref cleanup.


### Approved cleanup verification

Verified on October 20, 2026 with Temurin JDK 21.0.12.1 and Maven Wrapper 3.9.11:

- After `clean`, offline `verify` passed 122 main tests and three packaged-consumer tests,
  strict Javadoc and formatting validation of 70 Java files. The consumer's existing local
  dependency/plugin cache was restored after cleaning to avoid network downloads.
- Null geometry regressions first reproduced four failures, then passed with explicit
  `IllegalArgumentException` validation. Settings tests preserve replacement, late overrides,
  deferred validation and exporter independence when reusing a builder.
- Both complete quick-start Java examples compile with `--release 17` against the packaged
  development JAR and run on JDK 21. The first writes valid `mxfile` XML; the invalid-input
  example reports `Cannot create diagram: Unknown target node: missing`.
- Public `javap` signatures of all 46 non-internal packaged classes match the pre-cleanup JAR.
  Independent read-only diff review found no actionable findings. Changed documentation's local
  file links resolve.

This cleanup was not rerun on a JDK 17 runtime or through the draw.io browser UI. It changes
validation and configuration ownership, without changing layout/rendering or public signatures.

### Earlier feature verification

Verified locally on October 20, 2026, on Windows with Temurin JDK 17.0.20.1 and
JDK 21.0.12.1, using Maven Wrapper 3.9.11. The checks cover explicit leaf dimensions,
the reusable `DrawioExport` result, layout settings and internal package organization.

- At this earlier verification point, the main suite contained 115 tests and the separate
  packaged Maven consumer had three tests.
  `verify` passes on both JDK 17 and 21, including strict Javadoc and formatting.
  Coverage includes mixed shapes and sizes, all layout directions, nested groups,
  cross-group/self/parallel connections, immutable snapshots and custom layout geometry.
- Paired exports use one layout invocation and return XML that exactly matches the
  independently inflated fragment, including Unicode and large inputs.
- Independent reviews of each feature and the combined changes found no remaining
  actionable findings.
- All six runnable Java examples and all six README Java blocks compile and run against
  the packaged JAR on JDK 17. Local Markdown links resolve.

The paired URL from `examples/MixedSizeDiagram.java` was opened in draw.io. Nested editable
groups, cross-group and group-to-group connections, different shapes and explicit dimensions
display correctly. The Arrange panel confirmed a 240 by 90 node. A second browser fixture
showed 30 leaves, seven containers and 32 connections with mixed leaf dimensions. Its tall
topology still requires zooming to read individual labels.

A separate native file-open check could not be completed through the browser's file chooser.
The XML and URL payload are verified to contain the same model by automated decoding tests.
Remote CI results are tracked separately in the
[GitHub Actions runs](https://github.com/Saqqqner/DiagramKiln/actions).

### Feedback edges and node order

The layout DSL now exposes `feedbackEdges` and `preserveNodeOrder`, both disabled by default.
Regression coverage verifies legacy constructors, exporter configuration snapshots, feedback
routes in all four directions, XML entry/exit anchors, same-layer node order in RIGHT/DOWN
flow, nested groups, cross-group connections and self-loops. The pinned ELK 0.11.0 hierarchy
importer initializes its maximum model-order bound only at the root; the adapter seeds
that bound for nested graphs before import. A regression test reproduces the former null
unboxing failure and verifies nested ordering after the workaround.

`examples/OrderStatusDiagram.java` compiles against the packaged JAR with Java 17 and exports
XML plus a matching URL. In draw.io, the backward transition from rejected to draft enters
the left side of draft. Approved appears above rejected in the same layer, matching node
declaration order. Node ordering does not constrain layer assignment or disconnected
component packing, and feedback routing does not force one shared center attachment.
The updated fixture is [order-status.drawio](../examples/order-status.drawio), with a
[browser preview](../examples/order-status-preview.png).

### Advanced Layered settings

The first remote Java 21 build of this extension passed all 115 tests but failed strict
Javadoc with 12 missing main descriptions. A clean local JDK 21 package reproduced the
failure. Method descriptions now use prose or inline return tags; clean verification is
required separately on each JDK to avoid reusing generated documentation.

All nine cycle-breaking strategies, five node-placement strategies and six alignments from
the pinned ELK 0.11.0 dependency run on a nested cyclic graph with a cross-group connection.
Regression coverage checks immutable callback capture, optional leaf metadata combined with
explicit size, FIRST positioning, nested LAST positioning in all four directions, and group
rejection before reading leaf metadata. Route-sensitive tests cover all four new spacing
settings, including nested backward edges and parallel self-loops.

The ELK 0.11.0 hierarchical importer also leaves the cycle-breaking group count unset on
nested graphs. The adapter supplies zero because it does not assign cycle-breaking group
IDs; together with the existing maximum model-order bound this supports MODEL_ORDER and
related strategies in nested layouts. This workaround was checked against dependency sources.

The updated status example uses DFS_NODE_ORDER, NETWORK_SIMPLEX, CENTER, thoroughness 50,
80-pixel edge clearances, 20-pixel port spacing and FIRST on its initial status. Its packaged
Java 17 example exports successfully and opens in draw.io: the initial status is leftmost,
the rejected-to-draft transition enters from the left, and approved remains above rejected.
The [browser preview](../examples/order-status-preview.png) records this check. Interactive
strategies are selectable but the DSL does not provide initial coordinates for position
preservation. Existing constructor overloads are exercised by the packaged consumer.

### Internal package organization

Implementation classes are grouped under `internal.xml`, `internal.validation` and
`internal.geometry`; supported API packages remain unchanged. The seven relocated class
bodies are unchanged apart from imports and the internal visibility required for geometry
validation across package boundaries. Packaged classes use only the new internal paths,
and the Javadoc artifact excludes all internal subpackages. The status example's XML is
byte-for-byte identical before and after relocation. Full verification continues to cover
the packaged consumer and public API usage.

## Version 1.0.0 validation history

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
