# Project conventions

- Keep specifications in `docs/specs/` and implementation plans in `docs/plans/`, in English.
- Keep agent progress journals and local verification records under `.superpowers/`.
  This directory is ignored by Git; do not place these working records in public project docs.

- Follow the applicable Java rules in `docs/CODE-STYLE.md`, imported from omokane.
- Use Context7 to check third-party library APIs and examples before changing their integration.
  Resolve library IDs first. Relevant sources: Eclipse Layout Kernel and mxGraph.
  Verify version-specific details against the actual pinned dependencies and their official sources.
- Build and test with `./mvnw.cmd clean verify` on Windows or `bash ./mvnw clean verify`
  on Unix; format Java with the `formatter:format` goal. The verify lifecycle includes
  a separate packaged consumer under src/it/consumer and strict Javadoc.
- Write all Javadoc in English. README and user instructions may be in Russian.
- Public input contracts and DSL/exporter entry points live at the feature root. Implementation
  responsibilities use `config`, `dto`, `style`, `layout` and `internal` subpackages.
