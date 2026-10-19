# Release and publication

Local release coordinates: `io.github.saqqqner:diagram-kiln:1.0.0`.
Branches: main and dev. First release tag: `1.0.0` (without a prefix). Both branches start at the
released state. A subsequent development version has not been assigned. Continue development
on dev; main contains the released state.

Remote publication is not configured. Artifacts are installed in the local Maven repository.

## Build artifacts

```powershell
./mvnw.cmd clean install
```

The command checks Java sources, formatting, strict Javadoc and an isolated Maven project that
consumes the packaged artifact and its transitive dependencies. It produces:

- target/diagram-kiln-1.0.0.jar
- target/diagram-kiln-1.0.0-sources.jar
- target/diagram-kiln-1.0.0-javadoc.jar

The local release bundle is target/releases/diagram-kiln-1.0.0.zip: three JARs, the POM,
README, changelog and SHA256SUMS. Generated target contents are excluded from Git.

The JAR contains Automatic-Module-Name: io.github.saqqqner.diagramkiln. There is no full JPMS module-info;
regular classpath/Maven consumption is supported. Runtime dependencies remain separate artifacts;
their classes are not copied into the library JAR.

Build plugin and runtime dependency versions are pinned. project.build.outputTimestamp fixes
archive entry timestamps and Javadoc omits the generation date. Reproducibility means identical
artifacts for identical sources, JDK and Maven; different JDKs can generate different Javadoc HTML.
Update the timestamp deliberately when preparing a release.

## Before remote publication

1. Select a library license, add its text and configure licenses in the POM.
2. Set real project/SCM URLs and author metadata. No invented metadata is included.
3. Select the target repository and confirm publication rights for namespace io.github.saqqqner there.
4. Use an agreed release version and matching source tag. The first local release uses 1.0.0
   and tag 1.0.0. Rebuild and verify if sources change before remote publication; never replace
   the contents of an already published Maven version.
5. For Maven Central, configure PGP signing, a Central Portal token and the publication plugin
   according to current documentation. For a private repository, configure its endpoint/server id.
   Keep keys/tokens in local settings or CI secrets, outside source control.
6. Verify the complete artifact bundle, then publish as a separate action.

Central requirements: [metadata, sources, Javadoc, signatures and checksums](https://central.sonatype.org/publish/requirements/),
[Central Portal Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/).
Choose signing/upload configuration after selecting the destination.

## Release checks

clean verify runs the main tests and the isolated consumer in src/it/consumer. Invoker installs
the tested artifacts in target/it-repository and runs the consumer from target/it, without using
the producer's target/classes. The existing local Maven cache supplies release dependencies,
not a fallback copy of the artifact under test.

.github/workflows/ci.yml configures JDK 17 and 21. The workflow can run after the source is placed
in a GitHub repository with Actions enabled.
