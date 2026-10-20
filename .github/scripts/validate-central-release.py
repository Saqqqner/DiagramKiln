"""Validate a release before checking out its code or exposing signing secrets."""

import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def validate_pom(xml, tag):
    pom = ET.fromstring(xml)

    def expect(parent, path, value):
        actual = parent.findtext(path, namespaces=NS)
        if actual != value:
            raise ValueError(f"Unexpected POM {path}: expected {value}")

    expect(pom, "m:groupId", "io.github.saqqqner")
    expect(pom, "m:artifactId", "diagram-kiln")
    expect(pom, "m:version", tag)
    expect(pom, "m:scm/m:tag", tag)
    expect(pom, "m:properties/m:central.skipUpload", "true")
    profile = pom.find("m:profiles/m:profile[m:id='central-release']", NS)
    if profile is None:
        raise ValueError("Missing central-release profile")
    publisher = profile.find("m:build/m:plugins/m:plugin[m:artifactId='central-publishing-maven-plugin']", NS)
    signer = profile.find("m:build/m:plugins/m:plugin[m:artifactId='maven-gpg-plugin']", NS)
    if publisher is None or signer is None:
        raise ValueError("Missing publishing or signing plugin")
    expect(publisher, "m:groupId", "org.sonatype.central")
    expect(publisher, "m:configuration/m:publishingServerId", "central")
    expect(publisher, "m:configuration/m:autoPublish", "false")
    expect(publisher, "m:configuration/m:skipUpload", "${central.skipUpload}")
    expect(publisher, "m:configuration/m:waitUntil", "validated")
    expect(signer, "m:groupId", "org.apache.maven.plugins")
    expect(signer, "m:configuration/m:signer", "bc")
    expect(signer, "m:configuration/m:bestPractices", "true")


def validate_release(tag, mode):
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", tag):
        raise ValueError("Release tag must be numeric major.minor.patch without a prefix")
    if mode not in {"bundle", "publish"}:
        raise ValueError("Mode must be bundle or publish")
    commit = git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}")
    subprocess.run(["git", "merge-base", "--is-ancestor", commit, "refs/remotes/origin/main"], check=True)
    validate_pom(git("show", f"{commit}:pom.xml"), tag)
    return commit


if __name__ == "__main__":
    tag = os.environ["RELEASE_TAG"]
    commit = validate_release(tag, os.environ["RELEASE_MODE"])
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        output.write(f"commit={commit}\n")
    print(f"Validated io.github.saqqqner:diagram-kiln:{tag} at {commit}")
