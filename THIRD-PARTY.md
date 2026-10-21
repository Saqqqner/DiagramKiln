# Third-party components

DiagramKiln's own code is licensed under [MIT](LICENSE). That license does not
replace the licenses of the third-party components below.

## Runtime dependencies

This inventory applies to DiagramKiln 1.1.0 and the current 1.2.0-SNAPSHOT
development dependencies. The published DiagramKiln JAR is a thin JAR: it contains
DiagramKiln classes, not shaded or embedded copies of these libraries. Maven
resolves the libraries as separate artifacts. The versions below were checked
against the resolved runtime classpath, artifact POMs (including their parents),
and embedded manifests and notices.

| Component | Version | License | Upstream source / artifact metadata |
| --- | --- | --- | --- |
| ELK: `org.eclipse.elk.alg.layered`, `org.eclipse.elk.alg.common`, `org.eclipse.elk.core`, `org.eclipse.elk.graph` | 0.11.0 | [EPL-2.0](https://raw.githubusercontent.com/eclipse-elk/elk/v0.11.0/LICENSE.md) | [Versioned source](https://github.com/eclipse-elk/elk/tree/v0.11.0), [parent POM](https://repo.maven.apache.org/maven2/org/eclipse/elk/parent/0.11.0/parent-0.11.0.pom) |
| `org.eclipse.xtext:org.eclipse.xtext.xbase.lib` | 2.36.0 | [EPL-2.0](https://raw.githubusercontent.com/eclipse-xtext/xtext/v2.36.0/LICENSE) | [Versioned source](https://github.com/eclipse-xtext/xtext/tree/v2.36.0), [parent POM](https://repo.maven.apache.org/maven2/org/eclipse/xtext/xtext-parent/2.36.0/xtext-parent-2.36.0.pom) |
| EMF: `org.eclipse.emf.common`, `org.eclipse.emf.ecore`, `org.eclipse.emf.ecore.xmi` | 2.12.0 | [EPL-1.0](https://www.eclipse.org/legal/epl/epl-v10.html); see the additional `ecore` notice below | [Source project](https://github.com/eclipse-emf/org.eclipse.emf), [common](https://repo.maven.apache.org/maven2/org/eclipse/emf/org.eclipse.emf.common/2.12.0/), [ecore](https://repo.maven.apache.org/maven2/org/eclipse/emf/org.eclipse.emf.ecore/2.12.0/), [xmi](https://repo.maven.apache.org/maven2/org/eclipse/emf/org.eclipse.emf.ecore.xmi/2.12.0/) |
| `com.google.guava:guava` | 33.4.8-jre | [Apache-2.0](https://raw.githubusercontent.com/google/guava/v33.4.8/LICENSE) | [Versioned source](https://github.com/google/guava/tree/v33.4.8), [POM](https://repo.maven.apache.org/maven2/com/google/guava/guava/33.4.8-jre/guava-33.4.8-jre.pom) |
| `com.google.guava:failureaccess` | 1.0.3 | Apache-2.0 | [Artifact, POM and sources](https://repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.3/) |
| `com.google.guava:listenablefuture` (empty conflict-avoidance artifact) | 9999.0-empty-to-avoid-conflict-with-guava | Apache-2.0 | [Artifact, POM and sources](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/) |
| `org.jspecify:jspecify` | 1.0.0 | Apache-2.0 | [Artifact, POM and sources](https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/) |
| `com.google.errorprone:error_prone_annotations` | 2.36.0 | Apache-2.0 | [Artifact, POM and sources](https://repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.36.0/) |
| `com.google.j2objc:j2objc-annotations` | 3.0.0 | Apache-2.0 | [Artifact, POM and sources](https://repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/3.0.0/) |

Apache-2.0 text is available [locally](licenses/apache-maven-wrapper/LICENSE.txt).
ELK 0.11.0 and Xbase 2.36.0 use EPL-2.0; the three EMF 2.12.0
artifacts use EPL-1.0. Do not substitute the license of a newer upstream release.
The artifact directories above provide the corresponding published source JARs.
ELK also preserves an `about.html` notice inside its JARs.

`org.eclipse.emf.ecore` 2.12.0 includes additional XML4J/Xerces-derived code in
`org.eclipse.emf.ecore.xml.type.internal.DataValue`, `RegEx`, `XMLCalendar` and
`XMLDuration`. Its `about.html` contains the applicable Apache Software License
conditions and acknowledgment: "This product includes software developed by the
Apache Software Foundation (http://www.apache.org/)." Preserve that complete
upstream notice when redistributing the dependency.

When distributing a product that includes these separate dependencies, preserve
their applicable licenses, copyright and attribution notices, and satisfy their
source-availability requirements. This inventory is not a replacement for those
terms. Recheck the complete set if dependencies are upgraded, modified, bundled,
or shaded. Test dependencies and build plugins are not runtime components in
this table.

## Maven Wrapper (build tooling)

The repository includes Apache Maven Wrapper 3.3.4 scripts, `mvnw` and `mvnw.cmd`,
with their upstream license headers preserved. They are licensed under Apache-2.0,
not DiagramKiln's MIT license. The unmodified upstream [LICENSE](licenses/apache-maven-wrapper/LICENSE.txt)
and [NOTICE](licenses/apache-maven-wrapper/NOTICE.txt) are supplied here; upstream
copies are [LICENSE](https://github.com/apache/maven-wrapper/blob/maven-wrapper-3.3.4/LICENSE)
and [NOTICE](https://github.com/apache/maven-wrapper/blob/maven-wrapper-3.3.4/NOTICE).
These notices describe the Wrapper build tooling, not the authorship or license
of DiagramKiln's own code. The scripts are not bundled into the runtime JAR.

## Documentation and draw.io interoperability

The Javadoc archive includes the standard Javadoc tool's `legal/` notices for its
support files, including jQuery and jQuery UI. Preserve those notices when
redistributing that archive.

DiagramKiln is an independent project and is not affiliated with or endorsed by
draw.io, diagrams.net, or JGraph. These names describe the intended export format
and editor interoperability. DiagramKiln does not bundle the draw.io editor,
mxGraph, or their icon/stencil libraries. The example PNGs are diagram-only
exports of this project's `.drawio` fixtures; they do not include editor UI or
the draw.io logo. Compatibility is limited to the features generated by
DiagramKiln and is not a guarantee for every editor version or feature.
