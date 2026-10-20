# Release and publication

Historical local release coordinates: `io.github.saqqqner:diagram-kiln:1.0.1`.
Release coordinates: `io.github.saqqqner:diagram-kiln:1.1.0`.
Next development coordinates: `io.github.saqqqner:diagram-kiln:1.2.0-SNAPSHOT`.
The namespace migration changes Maven dependency coordinates; Java packages and the automatic
module name stay `io.github.saqqqner.diagramkiln`. Do not relabel old releases as new-namespace releases.
The `main` branch and tag `1.1.0` contain the release. The `dev` branch develops
`1.2.0-SNAPSHOT`. Tags use no prefix. Run the build on the matching branch to select
release or snapshot coordinates. In a clean checkout, use `git switch --detach 1.1.0`
for the stable release or `git switch dev` for development, then run the build below. Application
dependency versions must match the installed version; see the [quick start](../README.md#install).

Normal builds install locally. The opt-in `central-release` profile prepares Central Portal
artifacts; upload is disabled by default. The existing push/PR CI only verifies the project.
There is no automatic publishing workflow.

## Build artifacts

```powershell
./mvnw.cmd clean install
```

The command checks Java sources, formatting, strict Javadoc and an isolated Maven project that
consumes the packaged artifact and its transitive dependencies. It produces:

- `target/diagram-kiln-<version>.jar`
- `target/diagram-kiln-<version>-sources.jar`
- `target/diagram-kiln-<version>-javadoc.jar`

The local release bundle is target/releases/diagram-kiln-1.1.0.zip: three JARs, the POM,
README, LICENSE, changelog, documentation, examples and SHA256SUMS.
Generated target contents are excluded from Git.

The project uses the MIT License. The main JAR includes META-INF/LICENSE and the POM declares MIT.

The JAR contains Automatic-Module-Name: io.github.saqqqner.diagramkiln. There is no full JPMS module-info;
regular classpath/Maven consumption is supported. Runtime dependencies remain separate artifacts;
their classes are not copied into the library JAR.

Build plugin and runtime dependency versions are pinned in the POM properties block. The project
version remains a literal Maven coordinate: 1.1.0 on main, 1.2.0-SNAPSHOT on dev.
project.build.outputTimestamp fixes
archive entry timestamps and Javadoc omits the generation date. Reproducibility means identical
artifacts for identical sources, JDK and Maven; different JDKs can generate different Javadoc HTML.
Update the timestamp deliberately when preparing a release.

## Central Portal prerequisites

The selected namespace is `io.github.saqqqner`. This choice is not evidence of Verified status
or publishing rights. The Portal account, namespace, token and signing key have not been
inspected; confirm these yourself before release. Do not send secrets in chat or commit them.

1. In [Central Portal](https://central.sonatype.com/), confirm that `io.github.saqqqner` is
   **Verified** and your publisher account has access. Use the matching GitHub identity and
   follow Portal verification instructions if it is absent or unverified. Namespace selection
   alone, or ownership of this repository, is not proof of active Central publishing rights.
2. Confirm the project/developer/SCM metadata in `pom.xml`. It uses the actual repository URL,
   public GitHub handle Saqqqner for developer attribution; no personal name or email is required here.
3. Have the publisher generate a Central Portal user token. Its username/password pair is
   distinct from a GitHub token or normal account password. Store it in a local secret manager.
4. Select a signing-capable, non-expired OpenPGP key. Make its public key discoverable through a
   keyserver supported by Central, and keep the private key outside the repository. Follow
   [Sonatype's signing guide](https://central.sonatype.org/publish/requirements/gpg/).
5. Supply the Maven GPG plugin's BC signer through protected environment variables:
   `MAVEN_GPG_KEY` (exported TSK-format private key), `MAVEN_GPG_PASSPHRASE`, and optionally
   `MAVEN_GPG_KEY_FINGERPRINT`. BC signs in Java and does not require the `gpg` executable.
   Never put the passphrase in a command-line `-Dgpg.passphrase` property or in the POM.

For an authorized upload, [config/central-settings.xml.example](../config/central-settings.xml.example)
references `CENTRAL_TOKEN_USERNAME` and `CENTRAL_TOKEN_PASSWORD` from the environment under
server id `central`. Use it via Maven `-s`, or merge that server entry into your own private
settings while preserving any proxies/mirrors you need. Do not print effective settings or
debug logs while credentials are available. Keep local settings and keys outside Git.

## Local release preparation (no upload)

Keep `dev` at `1.2.0-SNAPSHOT`. The profile deliberately rejects SNAPSHOT versions, including
when upload is disabled. In a separate clean release checkout, after agreeing coordinates and
an unused release version, update the literal POM version, SCM tag and output timestamp to
match that release. A `-Dproject.version=...` argument does not replace the literal version.
Do not move existing tags or reuse an already published version.

After providing signing material privately, run from that release checkout:

```powershell
./mvnw.cmd -B -ntp -Pcentral-release clean verify
./mvnw.cmd -B -ntp -Pcentral-release "-Dcentral.skipUpload=true" deploy
```

The first command runs verification and signs the POM and three JARs. The second additionally
installs locally and creates `target/central-publishing/central-bundle.zip`, with signatures
and checksums. It does not upload: `central.skipUpload` defaults to `true`. A Portal token is
not needed for this local-only bundle. Inspect the archive's coordinates, release metadata,
four artifacts, four `.asc` signatures and checksum files; verify signatures with the intended
public key. An unsigned rehearsal with `-Dgpg.skip=true` is not a publishable bundle.

Normal `verify`/`install` runs do not activate signing or the publisher. The profile pins
Central Publishing Maven Plugin 0.12.0 and Maven GPG Plugin 3.2.8. Source/Javadoc artifacts,
MIT metadata and packaged LICENSE already come from the regular build.

## Upload and manual publication (separate authorization)

Only after namespace verification, release approval and signed-bundle review, an authorized
publisher can explicitly enable upload in the release checkout:

```powershell
./mvnw.cmd -B -ntp -s config/central-settings.xml.example -Pcentral-release "-Dcentral.skipUpload=false" deploy
```

This sends the bundle to Central Portal and waits for validation. `autoPublish=false` keeps
final publication manual: review the deployment in Portal before publishing it. Upload and
publication have not been performed as part of configuration. Ordinary pushes do neither.
Once published, confirm the exact coordinates resolve from Maven Central in a clean consumer.

Official references (checked October 20, 2026):
[namespace verification](https://central.sonatype.org/register/namespace/),
[required artifacts and POM metadata](https://central.sonatype.org/publish/requirements/),
[Portal token](https://central.sonatype.org/publish/generate-portal-token/),
[Central Maven plugin / skipUpload](https://central.sonatype.org/publish/publish-portal-maven/),
[Maven GPG signer parameters](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html).

## Release checks

clean verify runs the main tests and the isolated consumer in src/it/consumer. Invoker installs
the tested artifacts in target/it-repository and runs the consumer from target/it, without using
the producer's target/classes. The existing local Maven cache supplies release dependencies,
not a fallback copy of the artifact under test.

.github/workflows/ci.yml configures JDK 17 and 21. It runs verification on pushes and pull requests,
without activating `central-release` or accessing publication credentials.
