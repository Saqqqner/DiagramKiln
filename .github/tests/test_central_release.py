import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"


def load(name):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


central = load("central_release")
validator = load("validate-central-release")


class ReleaseChecks(unittest.TestCase):
    def test_release_must_be_published_stable_and_match_tag(self):
        valid = {"tag_name": "1.1.0", "draft": False, "prerelease": False}
        central.require_stable_release(valid, "1.1.0")
        for key, value in [("draft", True), ("prerelease", True), ("tag_name", "1.2.0")]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                central.require_stable_release(dict(valid, **{key: value}), "1.1.0")

    def test_central_lookup_only_404_means_absent(self):
        response = io.BytesIO(b"already published")
        with patch.object(central.urllib.request, "urlopen", return_value=response):
            self.assertTrue(central.is_published("1.1.0"))
        for status in [404, 401, 429, 503]:
            error = urllib.error.HTTPError("url", status, "failure", {}, None)
            with patch.object(central.urllib.request, "urlopen", side_effect=error):
                if status == 404:
                    self.assertFalse(central.is_published("1.1.0"))
                else:
                    with self.assertRaises(RuntimeError):
                        central.is_published("1.1.0")

    def test_invalid_refs_and_modes_rejected_before_git(self):
        for tag, mode in [("dev", "bundle"), ("v1.1.0", "bundle"), ("1.2.0-SNAPSHOT", "publish"), ("1.1.0\n", "bundle"), ("1.1.0", "upload")]:
            with self.subTest(tag=tag), patch.object(validator, "git") as git, self.assertRaises(ValueError):
                validator.validate_release(tag, mode)
            git.assert_not_called()


class BundleChecks(unittest.TestCase):
    def bundle(self, directory, omit=None, corrupt=False):
        path = Path(directory) / "bundle.zip"
        prefix = "io/github/saqqqner/diagram-kiln/1.1.0/"
        with zipfile.ZipFile(path, "w") as z:
            for suffix in [".pom", ".jar", "-sources.jar", "-javadoc.jar"]:
                name = "diagram-kiln-1.1.0" + suffix
                data = b"fixture artifact"
                z.writestr(prefix + name, data)
                if name != omit:
                    z.writestr(prefix + name + ".asc", b"-----BEGIN PGP SIGNATURE-----\nfixture\n-----END PGP SIGNATURE-----")
                for algorithm in ["md5", "sha1", "sha256", "sha512"]:
                    digest = hashlib.new(algorithm, data).hexdigest()
                    if corrupt and algorithm == "sha256":
                        digest = "0" * 64
                    z.writestr(prefix + name + "." + algorithm, digest)
        return path

    def test_four_signatures_are_verified_cryptographically(self):
        with tempfile.TemporaryDirectory() as d, patch.object(central.subprocess, "run") as gpg:
            gpg.return_value.stdout = b"[GNUPG:] VALIDSIG " + b"A" * 40 + b" fixture"
            central.verify_bundle(self.bundle(d), "1.1.0", Path(d))
            self.assertEqual(gpg.call_count, 4)
            self.assertTrue(all("--verify" in call.args[0] and call.kwargs["check"] for call in gpg.call_args_list))

    def test_bad_checksum_missing_signature_and_wrong_version_fail(self):
        with tempfile.TemporaryDirectory() as d:
            for omit, corrupt, version in [("diagram-kiln-1.1.0.jar", False, "1.1.0"), (None, True, "1.1.0"), (None, False, "1.2.0")]:
                with self.subTest(omit=omit, corrupt=corrupt), self.assertRaises(ValueError):
                    central.verify_bundle(self.bundle(d, omit, corrupt), version, Path(d))


class PublisherChecks(unittest.TestCase):
    def test_automatic_upload_and_receipt_written_before_poll(self):
        with tempfile.TemporaryDirectory() as d:
            bundle = Path(d) / "bundle.zip"
            bundle.write_bytes(b"fixture bundle")
            receipt = Path(d) / "receipt.json"
            deployment = "12345678-1234-1234-1234-123456789abc"
            calls = []

            def request(path, token, data=None, content_type=None):
                calls.append((path, token, data, content_type))
                if "/upload?" in path:
                    self.assertIn("publishingType=AUTOMATIC", path)
                    self.assertIn(b'name="bundle"', data)
                    return deployment.encode()
                self.assertTrue(receipt.exists())
                return json.dumps({"deploymentName": "io.github.saqqqner:diagram-kiln:1.1.0@" + "a" * 40, "deploymentState": "PUBLISHED"}).encode()

            output = io.StringIO()
            with patch.object(central, "central_request", side_effect=request), patch.dict(central.os.environ, {"CENTRAL_TOKEN_USERNAME": "fixture-user", "CENTRAL_TOKEN_PASSWORD": "fixture-password"}), contextlib.redirect_stdout(output):
                central.publish_bundle(bundle, "1.1.0", "a" * 40, receipt)
            self.assertEqual(len(calls), 2)
            self.assertNotIn("fixture-password", output.getvalue())
            self.assertNotIn("fixture-user", output.getvalue())
            self.assertEqual(json.loads(receipt.read_text())["deployment_id"], deployment)

    def test_resume_does_not_upload_and_rejects_unrelated_deployment(self):
        with tempfile.TemporaryDirectory() as d, patch.dict(central.os.environ, {"CENTRAL_TOKEN_USERNAME": "fixture-user", "CENTRAL_TOKEN_PASSWORD": "fixture-password"}):
            receipt = Path(d) / "receipt.json"
            with patch.object(central, "central_request", return_value=json.dumps({"deploymentName": "other", "deploymentState": "PUBLISHED"}).encode()) as request:
                with self.assertRaises(ValueError):
                    central.publish_bundle(Path(d) / "unused.zip", "1.1.0", "a" * 40, receipt, "12345678-1234-1234-1234-123456789abc")
                self.assertEqual(request.call_count, 1)
                self.assertIn("/status?", request.call_args.args[0])

    def test_validated_is_a_transitional_state_for_automatic_publication(self):
        with tempfile.TemporaryDirectory() as d, patch.dict(central.os.environ, {"CENTRAL_TOKEN_USERNAME": "fixture-user", "CENTRAL_TOKEN_PASSWORD": "fixture-password"}):
            deployment = "12345678-1234-1234-1234-123456789abc"
            name = "io.github.saqqqner:diagram-kiln:1.1.0@" + "a" * 40
            states = [json.dumps({"deploymentName": name, "deploymentState": state}).encode() for state in ["VALIDATED", "PUBLISHING", "PUBLISHED"]]
            with patch.object(central, "central_request", side_effect=states) as request, patch.object(central.time, "sleep"):
                central.publish_bundle(Path(d) / "unused.zip", "1.1.0", "a" * 40, Path(d) / "receipt.json", deployment)
                self.assertEqual(request.call_count, 3)

    def test_uncertain_upload_is_never_retried(self):
        with tempfile.TemporaryDirectory() as d, patch.dict(central.os.environ, {"CENTRAL_TOKEN_USERNAME": "fixture-user", "CENTRAL_TOKEN_PASSWORD": "fixture-password"}):
            p = Path(d) / "bundle.zip"
            p.write_bytes(b"fixture")
            with patch.object(central, "central_request", side_effect=RuntimeError("uncertain")) as request:
                with self.assertRaises(RuntimeError):
                    central.publish_bundle(p, "1.1.0", "a" * 40, Path(d) / "receipt.json")
                self.assertEqual(request.call_count, 1)


if __name__ == "__main__":
    unittest.main()
