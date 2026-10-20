"""Central release checks and automatic upload; credentials remain in environment/memory."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile

REPOSITORY = "Saqqqner/DiagramKiln"
COORDINATES = "io.github.saqqqner:diagram-kiln"
CENTRAL = "https://central.sonatype.com"


def require_tag(version):
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        raise ValueError("Expected numeric release version")


def require_stable_release(release, tag):
    if release.get("draft") or release.get("prerelease") or release.get("tag_name") != tag:
        raise ValueError("Expected a published stable GitHub release matching the tag")


def is_published(version):
    require_tag(version)
    url = f"https://repo.maven.apache.org/maven2/io/github/saqqqner/diagram-kiln/{version}/diagram-kiln-{version}.pom"
    try:
        with urllib.request.urlopen(url, timeout=30):
            return True
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return False
        raise RuntimeError(f"Central availability check failed HTTP {error.code}; refusing upload") from None
    except (urllib.error.URLError, TimeoutError):
        raise RuntimeError("Central availability unknown; refusing upload") from None


def verify_bundle(bundle, version, homedir):
    require_tag(version)
    prefix = f"io/github/saqqqner/diagram-kiln/{version}/"
    artifacts = [f"diagram-kiln-{version}{suffix}" for suffix in [".pom", ".jar", "-sources.jar", "-javadoc.jar"]]
    algorithms = ["md5", "sha1", "sha256", "sha512"]
    with zipfile.ZipFile(bundle) as archive:
        files = {name: archive.read(name) for name in archive.namelist() if not name.endswith("/")}
        if len(files) != len([n for n in archive.namelist() if not n.endswith("/")]):
            raise ValueError("Duplicate bundle member")
    allowed = {prefix + name + suffix for name in artifacts for suffix in ["", ".asc", *["." + a for a in algorithms], *[".asc." + a for a in algorithms]]}
    if set(files) - allowed:
        raise ValueError("Unexpected bundle coordinates or files")
    for name in artifacts:
        for suffix in ["", ".asc", *["." + a for a in algorithms]]:
            if not files.get(prefix + name + suffix):
                raise ValueError("Missing artifact, signature or checksum: " + name + suffix)
        if not files[prefix + name + ".asc"].startswith(b"-----BEGIN PGP SIGNATURE-----"):
            raise ValueError("Expected an armored detached signature")
        for suffix in ["", ".asc"]:
            for algorithm in algorithms:
                checksum = files.get(prefix + name + suffix + "." + algorithm)
                if checksum and checksum.decode("ascii").strip().lower() != hashlib.new(algorithm, files[prefix + name + suffix]).hexdigest():
                    raise ValueError("Bundle checksum mismatch")
    with tempfile.TemporaryDirectory(prefix="central-verify-") as directory:
        for name in artifacts:
            path = Path(directory) / name
            path.write_bytes(files[prefix + name])
            signature = Path(str(path) + ".asc")
            signature.write_bytes(files[prefix + name + ".asc"])
            result = subprocess.run(["gpg", "--batch", "--homedir", str(homedir), "--status-fd", "1", "--verify", str(signature), str(path)], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            status = result.stdout
            valid = [line.split() for line in status.splitlines() if line.startswith(b"[GNUPG:] VALIDSIG ")]
            if not valid or any(marker in status for marker in [b"EXPKEYSIG", b"EXPSIG", b"KEYEXPIRED", b"REVKEYSIG", b"BADSIG", b"ERRSIG"]):
                raise ValueError("Signature missing, expired, revoked or invalid")
            fingerprint = os.environ.get("MAVEN_GPG_KEY_FINGERPRINT", "").replace(" ", "").upper().encode()
            if fingerprint and not any(fingerprint in [line[2].upper(), line[-1].upper()] for line in valid):
                raise ValueError("Signature does not match the configured fingerprint")
    print("Verified four artifacts, four detached signatures and all checksums")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def central_request(path, token, data=None, content_type=None):
    request = urllib.request.Request(CENTRAL + path, data=data, method="POST", headers={"Authorization": "Bearer " + token, "Content-Type": content_type or "application/json", "User-Agent": "DiagramKiln-Central-CI"})
    try:
        with urllib.request.build_opener(NoRedirect()).open(request, timeout=60) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Central request HTTP {error.code}; inspect Portal before retry") from None
    except (urllib.error.URLError, TimeoutError):
        raise RuntimeError("Central request outcome may be uncertain; inspect Portal before retry") from None


def publish_bundle(bundle, version, commit, receipt, deployment_id=None):
    require_tag(version)
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Expected immutable commit SHA")
    username = os.environ.get("CENTRAL_TOKEN_USERNAME")
    password = os.environ.get("CENTRAL_TOKEN_PASSWORD")
    if not username or not password:
        raise ValueError("Missing Central token secret names; no upload attempted")
    token = base64.b64encode((username + ":" + password).encode()).decode()
    name = f"{COORDINATES}:{version}@{commit}"
    if deployment_id:
        deployment_id = str(uuid.UUID(deployment_id))
    else:
        boundary = "central-" + uuid.uuid4().hex
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="bundle"; filename="central-bundle.zip"\r\nContent-Type: application/octet-stream\r\n\r\n').encode() + Path(bundle).read_bytes() + f"\r\n--{boundary}--\r\n".encode()
        path = "/api/v1/publisher/upload?" + urllib.parse.urlencode({"name": name, "publishingType": "AUTOMATIC"})
        # A failed/uncertain upload is never retried here.
        deployment_id = str(uuid.UUID(central_request(path, token, body, "multipart/form-data; boundary=" + boundary).decode().strip()))
    receipt = Path(receipt)
    receipt.write_text(json.dumps({"deployment_id": deployment_id, "coordinates": f"{COORDINATES}:{version}", "commit": commit}, indent=2), encoding="utf-8")
    print("Central deployment ID: " + deployment_id)
    deadline = time.monotonic() + 2400
    last_state = None
    while time.monotonic() < deadline:
        status = json.loads(central_request("/api/v1/publisher/status?" + urllib.parse.urlencode({"id": deployment_id}), token))
        if status.get("deploymentName") != name:
            raise ValueError("Deployment does not match this release/commit")
        state = status.get("deploymentState")
        if state != last_state:
            print("Central deployment state: " + str(state))
            last_state = state
        if state == "PUBLISHED":
            return
        if state not in {"PENDING", "VALIDATING", "VALIDATED", "PUBLISHING"}:
            raise RuntimeError("Central validation/publication stopped; inspect deployment " + deployment_id)
        time.sleep(10)
    raise RuntimeError("Central wait timed out; resume using deployment ID " + deployment_id)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["check", "verify", "publish"])
    parser.add_argument("--version", required=True)
    parser.add_argument("--commit")
    parser.add_argument("--bundle", default="target/central-publishing/central-bundle.zip")
    parser.add_argument("--homedir")
    parser.add_argument("--receipt", default="target/central-publishing/deployment.json")
    parser.add_argument("--deployment-id", default="")
    args = parser.parse_args()
    require_tag(args.version)
    if args.action == "check":
        request = urllib.request.Request(f"https://api.github.com/repos/{REPOSITORY}/releases/tags/{args.version}", headers={"Accept": "application/vnd.github+json", "Authorization": "Bearer " + os.environ["GITHUB_TOKEN"]})
        with urllib.request.urlopen(request, timeout=30) as response:
            require_stable_release(json.load(response), args.version)
        exists = is_published(args.version)
        with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
            output.write("already_published=" + str(exists).lower() + "\n")
        print("Version already exists in Central; skipping publication" if exists else "Stable release verified; Central version is unused")
    elif args.action == "verify":
        home = Path(args.homedir)
        home.mkdir(mode=0o700, parents=True, exist_ok=True)
        key = os.environ.get("MAVEN_GPG_KEY")
        if not key:
            raise ValueError("Missing MAVEN_GPG_KEY")
        subprocess.run(["gpg", "--batch", "--quiet", "--homedir", str(home), "--import"], input=key.encode(), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        verify_bundle(Path(args.bundle), args.version, home)
    else:
        if os.environ.get("CENTRAL_AUTO_PUBLISH") != "true":
            raise ValueError("Automatic publishing policy is not enabled")
        if is_published(args.version):
            print("Already published; no upload attempted")
            return
        publish_bundle(Path(args.bundle), args.version, args.commit, Path(args.receipt), args.deployment_id or None)


if __name__ == "__main__":
    main()
