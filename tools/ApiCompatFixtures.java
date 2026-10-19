import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;

/** Exercises the actual profile's analyzer configuration on isolated known JAR pairs. */
class ApiCompatFixtures {
  record Fixture(String name, String oldSource, String newSource, String reason) {
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && args[0].equals("--self-test")) {
      try {
        checkOutcome(1, "CONSTRUCTOR_REMOVED", "stale XML CONSTRUCTOR_REMOVED",
            "Unrelated dependency resolution failure");
        throw new AssertionError("Stale report incorrectly accepted unrelated Maven failure");
      }
      catch (IllegalArgumentException expected) {
        System.out.println("PASS stale XML cannot satisfy an unrelated Maven failure");
      }
      return;
    }
    Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
    boolean offline = args.length > 1 && args[1].equals("-o");
    Path base = root.resolve("target/api-compat/fixtures");
    Path output = base.resolve("run-" + UUID.randomUUID());
    Files.createDirectories(output);
    Files.deleteIfExists(base.resolve("summary.txt"));
    Files.writeString(base.resolve("latest-run.txt"), output.toString());
    String prefix = "package io.github.saqqqner.diagramkiln; ";
    List<Fixture> fixtures = List.of(
        new Fixture("constructor", "public class Fixture { public Fixture(int x) {} }",
            "public class Fixture { public Fixture() {} }", "CONSTRUCTOR_REMOVED"),
        new Fixture("method", "public class Fixture { public void old() {} }",
            "public class Fixture {}", "METHOD_REMOVED"),
        new Fixture("protected", "public class Fixture { protected void old() {} }",
            "public class Fixture {}", "METHOD_REMOVED"),
        new Fixture("return", "public class Fixture { public String value() { return null; } }",
            "public class Fixture { public Object value() { return null; } }",
            "METHOD_RETURN_TYPE_CHANGED"),
        new Fixture("generic",
            "public class Fixture { public java.util.List<String> value() " + "{ return null; } }",
            "public class Fixture { public java.util.List<Integer> value() " + "{ return null; } }",
            "METHOD_RETURN_TYPE_GENERICS_CHANGED"),
        new Fixture("record-constructor", "public record Fixture(String name) {}",
            "public record Fixture(String name, int count) {}", "CONSTRUCTOR_REMOVED"),
        new Fixture("record-accessor", "public record Fixture(String name) {}",
            "public record Fixture(Object name) { public Fixture(String name) "
                + "{ this((Object) name); } }",
            "METHOD_RETURN_TYPE_CHANGED"),
        new Fixture("enum", "public enum Fixture { FIRST, SECOND }",
            "public enum Fixture { FIRST }", "FIELD_REMOVED"),
        new Fixture("spi", "public interface Fixture { void old(); }",
            "public interface Fixture { void old(); void added(); }", "METHOD_ADDED_TO_INTERFACE"),
        new Fixture("missing-class", "public class Fixture extends Missing {} class Missing {}",
            "public class Fixture extends Missing {} class Missing {}", "MISSING_CLASS"),
        new Fixture("compatible-addition", "public class Fixture {}",
            "public class Fixture { public void added() {} }", ""),
        new Fixture("private-package", "public class Fixture { private int old; } class Hidden {}",
            "public class Fixture { private String replacement; }", ""),
        new Fixture("internal-root",
            "package io.github.saqqqner.diagramkiln.internal; "
                + "public class Fixture { public void old() {} }",
            "package io.github.saqqqner.diagramkiln.internal; public class Fixture {}", ""),
        new Fixture("internal-subtree",
            "package io.github.saqqqner.diagramkiln.internal.deep.nested; "
                + "public class Fixture { public void old() {} }",
            "package io.github.saqqqner.diagramkiln.internal.deep.nested; public class Fixture {}", ""));
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var pom = factory.newDocumentBuilder().parse(root.resolve("pom.xml").toFile());
    Properties properties = new Properties();
    var propertyNodes = pom.getElementsByTagName("properties").item(0).getChildNodes();
    for (int i = 0; i < propertyNodes.getLength(); i++) {
      if (propertyNodes.item(i) instanceof Element property) {
        properties.setProperty(property.getTagName(), property.getTextContent());
      }
    }
    Element plugin = null;
    var plugins = pom.getElementsByTagName("plugin");
    for (int i = 0; i < plugins.getLength(); i++) {
      Element candidate = (Element) plugins.item(i);
      if (candidate.getElementsByTagName("artifactId").item(0).getTextContent()
          .equals("japicmp-maven-plugin")) {
        plugin = candidate;
      }
    }
    if (plugin == null) {
      throw new AssertionError("Missing api-compat analyzer plugin; fixtures cannot enforce gate");
    }
    StringBuilder summary = new StringBuilder();
    for (Fixture fixture : fixtures) {
      Path directory = output.resolve(fixture.name());
      Files.createDirectories(directory);
      String oldSource = fixture.oldSource().startsWith("package")
          ? fixture.oldSource()
          : prefix + fixture.oldSource();
      String newSource = fixture.newSource().startsWith("package")
          ? fixture.newSource()
          : prefix + fixture.newSource();
      Path oldJar = jar(directory.resolve("old"), oldSource);
      Path newJar = jar(directory.resolve("new"), newSource);
      var generated = factory.newDocumentBuilder().newDocument();
      Element project = generated.createElement("project");
      generated.appendChild(project);
      add(generated, project, "modelVersion", "4.0.0");
      add(generated, project, "groupId", "fixture");
      add(generated, project, "artifactId", fixture.name());
      add(generated, project, "version", "1");
      Element build = add(generated, project, "build", null);
      add(generated, build, "outputDirectory", directory.resolve("new/classes").toString());
      Element generatedPlugins = add(generated, build, "plugins", null);
      Element copy = (Element) generated.importNode(plugin, true);
      generatedPlugins.appendChild(copy);
      // Keep all real policy parameters; only replace artifacts, classpaths and report location.
      var executions = copy.getElementsByTagName("executions");
      if (executions.getLength() > 0) {
        copy.removeChild(executions.item(0));
      }
      Element configuration = (Element) copy.getElementsByTagName("configuration").item(0);
      for (String tag : List.of("oldClassPathDependencies", "newClassPathDependencies")) {
        configuration.removeChild(configuration.getElementsByTagName(tag).item(0));
        Element classpath = add(generated, configuration, tag, null);
        Element dependency = add(generated, classpath, "dependency", null);
        add(generated, dependency, "systemPath",
            (tag.startsWith("old") ? oldJar : newJar).toString());
      }
      ((Element) configuration.getElementsByTagName("oldVersion").item(0))
          .getElementsByTagName("path").item(0).setTextContent(oldJar.toString());
      ((Element) configuration.getElementsByTagName("newVersion").item(0))
          .getElementsByTagName("path").item(0).setTextContent(newJar.toString());
      configuration.getElementsByTagName("outputDirectory").item(0)
          .setTextContent(directory.resolve("reports").toString());
      copy.getElementsByTagName("version").item(0)
          .setTextContent(properties.getProperty("japicmp-maven-plugin.version"));
      var transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(generated),
          new StreamResult(directory.resolve("pom.xml").toFile()));
      List<String> command = new ArrayList<>();
      if (System.getProperty("os.name").startsWith("Windows")) {
        command.addAll(List.of("cmd.exe", "/d", "/c", root.resolve("mvnw.cmd").toString()));
      }
      else {
        command.addAll(List.of("bash", root.resolve("mvnw").toString()));
      }
      command.addAll(List.of("-B", "-ntp", "-f", directory.resolve("pom.xml").toString()));
      if (offline) {
        command.add("-o");
      }
      command.add("org.apache.maven.plugins:maven-jar-plugin:"
          + properties.getProperty("maven-jar-plugin.version") + ":jar");
      command.add("com.github.siom79.japicmp:japicmp-maven-plugin:"
          + properties.getProperty("japicmp-maven-plugin.version") + ":cmp");
      Path log = directory.resolve("maven.log");
      int status = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
          .redirectOutput(log.toFile()).start().waitFor();
      boolean expectedFailure = !fixture.reason().isEmpty();
      String reports = "";
      if (Files.isDirectory(directory.resolve("reports"))) {
        try (var files = Files.walk(directory.resolve("reports"))) {
          for (Path file : files.filter(p -> p.toString().endsWith(".xml")).toList()) {
            reports += Files.readString(file);
          }
        }
      }
      try {
        checkOutcome(status, fixture.reason(), reports, Files.readString(log));
      }
      catch (IllegalArgumentException failure) {
        throw new AssertionError("Analyzer fixture " + fixture.name() + " expected "
            + (expectedFailure ? fixture.reason() : "compatibility") + "; exit=" + status + "; see "
            + log);
      }
      summary.append("PASS ").append(fixture.name()).append(": ")
          .append(expectedFailure ? "rejected " + fixture.reason() : "compatible").append('\n');
      System.out.println("PASS " + fixture.name());
    }
    sourceCompatibility(output.resolve("spi"),
        "class Use implements io.github.saqqqner.diagramkiln.Fixture { public void old() {} }");
    sourceCompatibility(output.resolve("enum"),
        "class Use { Object value = io.github.saqqqner.diagramkiln.Fixture.SECOND; }");
    Path completedSummary = output.resolve("summary.txt");
    Files.writeString(completedSummary, summary);
    Files.move(completedSummary, base.resolve("summary.txt"),
        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }

  static void checkOutcome(int status, String reason, String reports, String log) {
    boolean expectedFailure = !reason.isEmpty();
    boolean missingClass = reason.equals("MISSING_CLASS");
    if ((status != 0) != expectedFailure || (!missingClass && reports.isBlank())
        || (expectedFailure && !missingClass
            && (!reports.contains(reason) || !log.contains(reason)))
        || (missingClass && !log.contains("Could not load 'io.github.saqqqner.diagramkiln.Missing'"))) {
      throw new IllegalArgumentException("Analyzer result did not match expected outcome");
    }
  }

  static void sourceCompatibility(Path directory, String source) throws Exception {
    Path input = directory.resolve("Use.java");
    Files.writeString(input, source);
    for (String version : List.of("old", "new")) {
      Path output = directory.resolve("consumer-" + version);
      Files.createDirectories(output);
      Path log = directory.resolve("consumer-" + version + ".log");
      int status;
      try (var diagnostics = Files.newOutputStream(log)) {
        status = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics,
            "--release", "17", "-cp", directory.resolve(version + "/fixture.jar").toString(), "-d",
            output.toString(), input.toString());
      }
      if ((status == 0) != version.equals("old")) {
        throw new AssertionError("Expected old source to compile only against old API: " + log);
      }
    }
  }

  static Element add(org.w3c.dom.Document document, Element parent, String name, String text) {
    Element element = document.createElement(name);
    parent.appendChild(element);
    if (text != null) {
      element.setTextContent(text);
    }
    return element;
  }

  static Path jar(Path directory, String source) throws Exception {
    Files.createDirectories(directory);
    Path input = directory.resolve("Fixture.java");
    Files.writeString(input, source, StandardCharsets.UTF_8);
    Path classes = directory.resolve("classes");
    Files.createDirectories(classes);
    int status = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17",
        "-encoding", "UTF-8", "-d", classes.toString(), input.toString());
    if (status != 0) {
      throw new AssertionError("Fixture compilation failed: " + input);
    }
    Path jar = directory.resolve("fixture.jar");
    try (JarOutputStream archive = new JarOutputStream(Files.newOutputStream(jar));
        var paths = Files.walk(classes)) {
      for (Path file : paths.filter(Files::isRegularFile).toList()) {
        if (file.getFileName().toString().equals("Missing.class")) {
          continue;
        }
        archive.putNextEntry(
            new JarEntry(classes.relativize(file).toString().replace(File.separatorChar, '/')));
        Files.copy(file, archive);
        archive.closeEntry();
      }
    }
    return jar;
  }
}
