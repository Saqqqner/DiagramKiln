# DiagramKiln Code Style

## Java and packages

Target Java 17. Keep the library independent of application frameworks.
Public input interfaces, the DSL and exporter entry points live in
`io.github.saqqqner.diagramkiln`. Group implementation responsibilities in the existing subpackages:

- `config`: validated layout settings.
- `dto`: immutable diagram and geometry records.
- `style`: validated node, group and edge appearance.
- `layout`: the layout engine contract and ELK adapter.
- `internal.xml`: XML serialization, style serialization and compression.
- `internal.validation`: input snapshots, graph indexing and layout validation.
- `internal.geometry`: attachment points and shape boundary calculations.

Create subpackages only for responsibilities the library actually has. Keep implementation
members package-private where possible; expose only what callers or package boundaries need.
Use explicit imports and avoid imports from the current package. Keep tests aligned with the
production packages when package-private access is needed.

## Formatting

Use [config/java-formatter.xml](../config/java-formatter.xml) as the formatting authority:

- Two spaces for indentation; four additional spaces for continuations.
- A 100-character line width and LF line endings.
- `else`, `catch`, `finally` and the `while` in a do/while statement on a new line.
- Array initializer braces on separate lines and wrapped operators before their operands.

Run `./mvnw.cmd formatter:format` on Windows or `bash ./mvnw formatter:format` on
Linux/macOS. The Maven verify lifecycle checks formatting. Keep related methods together
and place called helpers after their callers where practical.

## Data and builders

Use records for immutable data. Copy caller-owned collections when capturing input or
building results. Validate required values, IDs, style values and dimensions at their
existing API boundaries. Completed diagrams, styles and settings must not change when
an input collection or builder is later modified.

Keep mutable configuration in builders and pass completed dependencies through constructors.
Preserve the distinction between public input contracts and internal export/layout data.

## Static methods

- Use ordinary private instance methods for helpers called from instance methods or constructors.
- Keep helpers static when they are called from static methods.
- Preserve public static factories such as `builder()` and `create()`, and constants.

## Javadoc and comments

Write all documentation, Javadoc and comments in English. Explain caller-visible contracts:
required values, units, coordinate systems, snapshot behavior, failure behavior and thread
or resource ownership. Document nontrivial public APIs without repeating trivial accessor
names or adding boilerplate. Keep internal implementation details out of user-facing examples.

## Git

Use Conventional Commits: `<type>: <description>`, with types such as `feat`, `fix`,
`docs`, `test`, `refactor` and `chore`.

`main` contains the release; `dev` contains the next development version. Feature branches
use `feat/<name>` and fixes may use `fix/<name>`. Release tags have no `v` prefix.
See [release instructions](PUBLISHING.md) for release preparation.
