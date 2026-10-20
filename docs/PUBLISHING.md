# Release and Central publication

DiagramKiln uses `io.github.saqqqner:diagram-kiln`, Java package and automatic module
name `io.github.saqqqner.diagramkiln`. Java 17+ is required. The release line is 1.1.0;
development is 1.2.0-SNAPSHOT. Tags are numeric versions without a prefix.
The technical `DrawioExporter` API and `.drawio` format remain unchanged in purpose.

## Build and artifacts

Use `./mvnw.cmd clean verify` on Windows or `bash ./mvnw clean verify` on Linux/macOS.
Verification includes unit tests, strict Javadoc, formatting and a separate consumer of
the packaged artifact. Normal verification does not sign or publish. A local install uses
`./mvnw.cmd clean install` and produces main, sources and Javadoc JARs under `target/`.
Runtime dependencies remain separate; the JAR is not shaded. There is an automatic module
name but no explicit JPMS descriptor. MIT LICENSE is preserved and packaged in the JAR.

The opt-in `central-release` profile requires a non-SNAPSHOT literal POM version and matching
SCM tag. Maven GPG Plugin 3.2.8 uses the BC signer; Central Publishing Plugin 0.12.0 creates
the bundle. Keep dependencies/plugin versions in the POM properties. Never move a release
tag or overwrite a Maven Central version during normal release work.

## Prerequisites and the first-publication hold

The publisher reported namespace `io.github.saqqqner` as Verified. Account permissions,
Portal token authentication and live signing still require a private end-to-end check.
No credentials, key generation or actual Central upload are part of CI configuration.

Publication is paused unless repository Actions variable `CENTRAL_PUBLISH_ENABLED` is exactly
`true`. Leave it unset until the final project name, release metadata, signing key and token
are approved and ready. This one-time activation enables automatic future stable releases;
it is not a per-release manual approval. Repository variables are separate from secrets.

Open **Saqqqner/DiagramKiln > Settings > Secrets and variables > Actions > Secrets >
New repository secret** and add these yourself. Do not send values in chat or commit them.

| Secret | Value |
| --- | --- |
| `CENTRAL_TOKEN_USERNAME` | Username from a generated Central Portal user token |
| `CENTRAL_TOKEN_PASSWORD` | Password from that same token, not an account/GitHub password |
| `MAVEN_GPG_KEY` | Complete ASCII-armored TSK private-key export, including BEGIN/END lines |
| `MAVEN_GPG_PASSPHRASE` | Passphrase protecting that key |
| `MAVEN_GPG_KEY_FINGERPRINT` | Optional full fingerprint if the exported keyring has multiple keys |

Use a signing-capable, non-expired, non-revoked OpenPGP key. The public key must be discoverable
through a Central-supported keyserver such as `keyserver.ubuntu.com` or `keys.openpgp.org`.
The proposed GnuPG plan is a separate RSA4096 primary signing key with public GitHub/noreply
identity, one-year expiry, a protected home outside repositories and an agreed encrypted
offline backup. Obtain approval before creating/exporting it. Enter the passphrase directly
in pinentry; never pass it on the command line or in a plaintext passphrase file.

## Automatic GitHub Release to Central chain

`.github/workflows/central.yml` handles published stable GitHub Releases, manual dispatch
and reusable `workflow_call`. Drafts, prereleases and PR events cannot publish. The trusted
helpers are loaded from the default branch, then source is checked out by validated immutable
release commit. The validator requires a numeric tag, matching group/artifact/version/SCM
metadata, ancestry on main and the expected signing/no-upload profile. A published stable
GitHub release must already exist for that tag.

The pipeline checks Central first: an existing version is a no-op; HTTP errors other than
404 fail closed. JDK17 and JDK21 each run full clean verification without publication secrets.
After both pass, JDK17 rebuilds the release with signing and
`-Pcentral-release -Dcentral.skipUpload=true -Dmaven.install.skip=true clean deploy`.
Four artifacts (POM, main, sources, Javadoc), four detached signatures and all checksums are
required. GnuPG cryptographically verifies the signatures and rejects expired/revoked keys
in a temporary keyring imported from the provided secret; that keyring is removed afterwards.
BC performs the signing, so setup-java's separate GnuPG import is not used.

The historical release POM deliberately retains `autoPublish=false`. CI does not mutate it.
Instead, the exact verified bundle is uploaded through the official Central Publisher API
with `publishingType=AUTOMATIC`, equivalent to the requested autoPublish=true policy. Central
validates it and automatically publishes only if validation passes. The job waits for PUBLISHED;
validation failure stops the job. Central token credentials are scoped to that API step and
remain in process memory, never printed or written to settings. Signing secrets are scoped
to the build/verification step. Do not enable shell tracing or Maven debug/effective-settings
output while secrets are available.

Temporary Maven settings use server id `central` with environment references; no settings.xml
secret is needed. API publication does not use Maven credentials. Bundles and deployment
receipts are retained as Actions artifacts for seven days; keyrings/settings/logs are excluded.
GitHub release assets and Git tags are not changed by this workflow.

## Manual preparation and safe retry

Workflow files must first be committed to the default branch. Adding them does not replay the
already-published 1.1.0 release event. Open **Actions > Publish stable release to Central >
Run workflow**, select the default branch and tag `1.1.0`, then choose `bundle`. This is allowed
while publication is disabled, requires signing secrets and performs no upload.

After activation and signed-bundle review, choose `publish` for the first existing-release
upload. Confirm the checkbox only after inspecting Central Portal for a pending deployment.
Versions are immutable; the public Central POM check and per-tag concurrency prevent ordinary
duplicates, but public-index propagation is not an idempotency guarantee for pending uploads.
Never blindly rerun an uncertain upload.

The job prints a public deployment ID and immediately saves it in `deployment.json`. If a run
times out after upload, supply that ID in the manual `deployment_id` field. This polls the same
deployment and verifies its coordinate/commit name. This separate recovery job requires only
the Portal token; it does not rebuild, sign or upload another bundle. If an
upload fails before returning an ID, inspect Portal for the matching coordinate/version/commit
before confirming a new upload. An automatic job is never internally retried after an uncertain
upload. A failed deployment's diagnostics are available in Portal; raw responses/credentials
are not printed in Actions logs.

## Releases created by another workflow

Events created using the repository's GITHUB_TOKEN generally do not trigger another workflow.
No separate release-creation workflow currently exists here. If one is added, call this
reusable workflow after its successful GitHub release job; do not generate a PAT just to
chain events:

```yaml
  central:
    needs: github_release
    uses: ./.github/workflows/central.yml
    with:
      release_tag: ${{ needs.github_release.outputs.tag }}
    secrets:
      CENTRAL_TOKEN_USERNAME: ${{ secrets.CENTRAL_TOKEN_USERNAME }}
      CENTRAL_TOKEN_PASSWORD: ${{ secrets.CENTRAL_TOKEN_PASSWORD }}
      MAVEN_GPG_KEY: ${{ secrets.MAVEN_GPG_KEY }}
      MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
      MAVEN_GPG_KEY_FINGERPRINT: ${{ secrets.MAVEN_GPG_KEY_FINGERPRINT }}
```

The caller must publish the stable GitHub Release before invoking this job. The same gates,
activation variable, concurrency and existing-version checks apply.

## Local manual profile

`config/central-settings.xml.example` references Central token environment variables for an
explicitly authorized local Maven upload. Local profile usage remains manual by default:
`./mvnw.cmd -Pcentral-release "-Dcentral.skipUpload=true" deploy` creates the signed bundle
without uploading. The normal local POM still has autoPublish=false. Prefer the reviewed CI
chain for automatic release publication; do not enable upload on development snapshots.

Official references:
[Central Publisher API](https://central.sonatype.org/publish/publish-portal-api/),
[Maven plugin / skipUpload](https://central.sonatype.org/publish/publish-portal-maven/),
[signing requirements](https://central.sonatype.org/publish/requirements/gpg/),
[Maven GPG parameters](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html),
[GITHUB_TOKEN event behavior](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).
