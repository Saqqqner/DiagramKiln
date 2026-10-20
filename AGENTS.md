# Working on DiagramKiln

DiagramKiln is a Java 17+ library that converts a Java diagram DSL or application DTOs
into editable draw.io XML, using ELK for layout. Maven coordinates are
`io.github.saqqqner:diagram-kiln`; the base package and automatic module name remain
`io.github.saqqqner.diagramkiln`. Historical 1.0.x releases used Maven groupId `io.github.saqqqner`.

## Repository map

- `src/main/java/io/github/saqqqner/diagramkiln`: public input interfaces, DSL and exporter.
- `config`, `dto`, `style`, `layout`: layout settings, immutable data, appearance and layout engines.
- `internal.xml`, `internal.validation`, `internal.geometry`: XML/compression, validation/indexing
  and shape geometry implementation; not public API.
- `src/test/java`: library tests; `src/it/consumer`: a separate packaged Maven consumer.
- `examples`: runnable usage examples and draw.io fixtures.
- `docs/USER-GUIDE.md`: detailed behavior; `docs/CODE-STYLE.md`: coding conventions.

## Build and verification

Run commands from the repository root with JDK 17 or newer. Use the Maven Wrapper.

| Task | Windows | Linux/macOS |
| --- | --- | --- |
| Full verification | `./mvnw.cmd clean verify` | `bash ./mvnw clean verify` |
| Install locally | `./mvnw.cmd clean install` | `bash ./mvnw clean install` |
| Format sources | `./mvnw.cmd formatter:format` | `bash ./mvnw formatter:format` |

The verify lifecycle runs unit tests, strict Javadoc, formatting validation and a consumer
against the packaged JAR and its transitive dependencies. Add focused regression coverage
when behavior changes. For export or layout changes, check nested groups and cross-group
connections as well as flat diagrams; inspect the result in draw.io when rendering changes.
For documentation-only changes, verify links and compile/run changed Java examples.
Report checks actually run and any checks that remain unavailable.

## Implementation conventions

Follow applicable Java rules in `docs/CODE-STYLE.md`; keep detailed style rules there.
Keep public contracts and DSL/exporter entry points at the feature root, with implementation
responsibilities in the existing subpackages. Preserve public API compatibility unless a
breaking change is explicitly requested.

Preserve these contracts when editing layout or export code:

- Node and group IDs are globally unique across the complete diagram tree.
- Connections may reference nodes or groups across nesting levels.
- Input snapshots and built styles/settings are immutable; do not retain mutable input data.
- Layout coordinates are absolute; XML geometry is relative to its owning container.
- Labels are escaped plain text. Save XML as UTF-8 and preserve Unicode in URL fragments.

Keep dependency and plugin versions in POM properties. Independent components retain their
own versions; do not assign a shared version number to unrelated dependencies or plugins.
Before changing third-party integrations, use Context7 when available: resolve the library ID,
then check documentation for the pinned version. Verify unclear ELK/mxGraph behavior against
official sources and the actual dependencies. If documentation tools are unavailable, state
that limitation and use available official documentation or source code.

## Documentation and generated files

Write all project documentation and Javadoc in English. Keep README focused on library users;
put advanced usage in `docs/USER-GUIDE.md` and release details in `docs/PUBLISHING.md`.
Keep design specifications and implementation plans in ignored `.superpowers/specs/` and
`.superpowers/plans/`; do not add them to public repository documentation.
Keep agent progress journals and temporary verification records in ignored `.superpowers/`.
Do not commit generated `target/` output, local Maven caches or credentials.

## Release work

Follow `docs/PUBLISHING.md`. The project uses MIT; preserve the root LICENSE and its packaged
copy. Use release tags without a `v` prefix. Do not publish artifacts, push branches or move
release tags unless the user requests that release action. Never replace an already published
Maven version.
