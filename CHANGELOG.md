# Changelog

## 1.1.0 — 2026-10-20

- Maven groupId changed to io.github.saqqqner; Java packages and automatic module name remain
  io.github.saqqqner.diagramkiln. Existing 1.0.x release coordinates remain historical.
- Opt-in Central Portal publishing profile with signing and local-only bundle generation by default.
- Internal implementation organized into XML, validation and geometry packages.
- Optional feedback-edge routing in the layout DSL for backward transitions.
- Optional preservation of declared node order within layers, including nested groups.
- Explicit per-node dimensions in the DSL and application DTOs, with global layout fallback.
- Eager paired XML and URL-fragment export from one layout execution.
- Typed Layered strategies, alignment and search effort, with edge and port spacing settings.
- Per-leaf FIRST/LAST layer constraints in the DSL and optional application DTO interface.
- Complete method Javadoc descriptions for clean builds with strict JDK 21 checks.
- Reject null geometry lists and elements with descriptive IllegalArgumentException messages.
- Delegate exporter configuration to the layout settings builder without changing public API.
- Clarify stable/development installation and validation error handling in the quick start.

## 1.0.1 — 2026-10-19

- Document compact record constructor parameters so strict Javadoc succeeds on Java 17.

## 1.0.0 — 2026-10-19

- draw.io-compatible compressed #R URL fragment output alongside plain XML export.
- Java DSL and interfaces for arbitrary application DTOs.
- Nested groups with globally scoped ids and cross-hierarchy connections.
- ELK Layered placement, container sizing and orthogonal routes.
- Typed reusable node, group and edge styles, including ellipse/diamond attachment.
- Configurable node/edge arc sizes, rounded edge bends, crossing jumps and target arrow size.
- Immutable layout options DSL for direction, leaf size, layer/node spacing and group padding.
- Editable, uncompressed draw.io XML preserving group ownership and local routes.
- Java 17 baseline; source/Javadoc artifacts, Maven Wrapper and packaged-consumer checks.
