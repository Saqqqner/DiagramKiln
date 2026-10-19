# Release and publication

Local release coordinates: `io.github.saqqqner:diagram-kiln:1.0.1`.
The `main` branch and tag `1.0.1` contain the release. The `dev` branch develops
`1.1.0-SNAPSHOT`. Tags use no prefix. Run the build on the matching branch to select
release or snapshot coordinates.

Remote publication is not configured. Artifacts are installed in the local Maven repository.

## Build artifacts

```powershell
./mvnw.cmd clean install
```

The command checks Java sources, formatting, strict Javadoc and an isolated Maven project that
consumes the packaged artifact and its transitive dependencies. It produces:

- `target/diagram-kiln-<version>.jar`
- `target/diagram-kiln-<version>-sources.jar`
- `target/diagram-kiln-<version>-javadoc.jar`

The local release bundle is target/releases/diagram-kiln-1.0.1.zip: three JARs, the POM,
README, LICENSE, changelog, documentation, examples and SHA256SUMS.
Generated target contents are excluded from Git.

The project uses the MIT License. The main JAR includes META-INF/LICENSE and the POM declares MIT.

The JAR contains Automatic-Module-Name: io.github.saqqqner.diagramkiln. There is no full JPMS module-info;
regular classpath/Maven consumption is supported. Runtime dependencies remain separate artifacts;
their classes are not copied into the library JAR.

Build plugin and runtime dependency versions are pinned in the POM properties block. The project
version remains a literal Maven coordinate: 1.0.1 on main, 1.1.0-SNAPSHOT on dev.
project.build.outputTimestamp fixes
archive entry timestamps and Javadoc omits the generation date. Reproducibility means identical
artifacts for identical sources, JDK and Maven; different JDKs can generate different Javadoc HTML.
Update the timestamp deliberately when preparing a release.

## Before remote publication

1. Verify that LICENSE and the MIT license metadata are included in the release bundle.
2. Set real project/SCM URLs and author metadata. No invented metadata is included.
3. Select the target repository and confirm publication rights for namespace io.github.saqqqner there.
4. Use an agreed release version and matching source tag. The current local release uses 1.0.1
   and tag 1.0.1. Rebuild and verify if sources change before remote publication; never replace
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
