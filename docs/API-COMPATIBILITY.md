# API compatibility verification

The opt-in `api-compat` profile compares the packaged library with
`io.github.saqqqner:diagram-kiln:1.0.1`, built from commit
`9404843d3e27f0baa9c06015b04104e2a83499fd`. Ordinary `verify` does not require this
baseline and never tries to download release 1.0.1 from Maven Central.

## Prepare the baseline

Use JDK 17, Git and the repository's Maven Wrapper. From the repository root on Windows:

```powershell
$repo = (Get-Location).Path
$cache = Join-Path $repo '.superpowers/api-compat/baseline'
java tools/ApiCompat.java bootstrap $repo $cache
./mvnw.cmd clean verify -Papi-compat "-DapiCompat.baselineJar=$cache/diagram-kiln-1.0.1.jar" "-DapiCompat.baselineProvenance=$cache/provenance.properties"
```

On Linux/macOS:

```bash
repo="$PWD"
cache="$repo/.superpowers/api-compat/baseline"
java tools/ApiCompat.java bootstrap "$repo" "$cache"
bash ./mvnw clean verify -Papi-compat \
  "-DapiCompat.baselineJar=$cache/diagram-kiln-1.0.1.jar" \
  "-DapiCompat.baselineProvenance=$cache/provenance.properties"
```

The helper creates a separate checkout at the pinned full SHA. It does not switch the
current checkout, install or publish the baseline, or move tags. Its cache is outside
the current `target/`, so current-project `clean` preserves it. Bootstrap requires exactly
JDK 17 and rejects modifications or untracked files in a supplied baseline checkout. To reuse an
independently checked-out baseline (as CI does), provide its path as a fourth argument:

```bash
java tools/ApiCompat.java bootstrap "$repo" "$cache" /absolute/path/to/baseline-sources
```

The cache contains the baseline JAR, `classpath.txt`, `provenance.properties`, build log
and a separate source checkout. Provenance records coordinates, source SHA, JDK and Maven
versions, and SHA-256 checksums of the JAR and classpath file. Keep the dependency artifacts
referenced by that classpath available. The profile rejects absent files, mismatched
coordinates, source identity or checksums, and unresolved dependency paths with diagnostics.

An explicitly supplied trusted baseline must include the corresponding provenance and
classpath files in this format. A self-recorded checksum detects subsequent modification;
it cannot independently attest that an arbitrary supplied JAR came from the pinned sources.
CI builds its own baseline from those sources.

## What the gate covers

The pinned `japicmp-maven-plugin` 0.26.1 compares public and protected API throughout
`io.github.saqqqner.diagramkiln`, automatically including future supported subpackages and nested
builders. The complete `internal` subtree is excluded. Binary and analyzer-detected source
incompatibilities fail; compatible additions and implementation-only changes pass.
Missing classes are errors. Baseline and current analyzer dependency versions are supplied
separately; update the frozen baseline properties only when intentionally changing baseline.

A Java 17 historical consumer separately compiles against the old JAR and its dependency
classpath, runs that bytecode with only the current library and its runtime dependencies,
then compiles the same source against the new JAR into a different directory. It exercises
DSL/builders, nested groups, cross-group connections, DTO records, application input
interfaces and a custom `LayoutEngine`. It verifies the library's actual code source is the
current packaged JAR. There is no XML byte-for-byte equality promise.

Run the analyzer's regression fixtures with the same profile configuration:

```bash
java tools/ApiCompatFixtures.java .
java tools/ApiCompatFixtures.java --self-test
java src/api-compat/BaselineValidationTest.java .
```

These generate isolated JAR pairs for removed constructors, methods and protected members,
return and generic-signature changes, record constructors/accessors, enum removal and
interface/SPI evolution. They also require compatible additions, private/package-private
changes and changes at both the internal package root and deep below it to pass. Each
expected failure must have its specific compatibility reason in an XML report, so an
unrelated Maven resolution failure cannot satisfy a negative fixture.
Each run uses fresh fixture/report directories; the self-test rejects stale XML paired
with an unrelated failure. A missing-class fixture checks strict dependency resolution;
enum and SPI consumers also demonstrate the corresponding Java source compilation failure.

Reports are written under `target/api-compat/`: analyzer XML and HTML, baseline/current
identities, provenance, historical compilation/classpath evidence, runtime log and fixture
reports. Inspect these when a gate fails; do not add broad exclusions to pass a release.
Any narrowly justified exception needs an exact change, rationale and review.

Structural analysis and representative consumers do not prove that every Java source
compiles: new overloads can introduce ambiguities. Behavioral compatibility, reflection,
serialization, exact geometry and every transitive dependency interaction remain outside
this gate; the ordinary functional tests retain their role.

## Offline verification and CI

After online bootstrap and verification, add `-o` to the Maven verification command.
For analyzer fixtures use `java tools/ApiCompatFixtures.java . -o`. Offline operation
requires the prepared baseline, both releases' runtime dependency artifacts, Maven Wrapper
distribution, and all build/analyzer plugins in the configured caches. The profile performs
no hidden checkout or download; cold caches need online preparation first. Run prepared
offline verification without `clean`: cleaning also removes the ordinary packaged consumer's
isolated repository and its cached build plugins. After cleaning, prepare that consumer online
again before using offline verification.
The packaged consumer explicitly inherits the parent's offline flag through the invoker's
filtered properties; `-o` therefore applies to that child Maven process as well.

The separate [API compatibility release workflow](../.github/workflows/api-compat.yml)
runs manually through `workflow_dispatch`
and on numeric version tags such as `1.1.0`; ordinary push/pull-request CI remains unchanged.
Its `API compatibility (JDK 17)` job builds baseline sources in a second checkout at
the pinned SHA, runs the profile and fixtures, and uploads diagnostic reports even after
failure. Run it before publishing a release and inspect its result. This release check is
not a required pull-request branch-protection check; workflow YAML alone does not enforce
release policy or publication ordering.

The selected analyzer's configuration was checked against the
[0.26.1 release source](https://github.com/siom79/japicmp/tree/japicmp-base-0.26.1)
and its executable fixtures. Its version remains an independent POM property.
