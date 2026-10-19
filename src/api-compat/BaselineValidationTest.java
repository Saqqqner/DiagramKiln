import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Standalone negative tests; never resolve Maven artifacts or trust a fixture as a baseline. */
class BaselineValidationTest {
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath();
    Path output = root.resolve("target/api-compat/validation-tests");
    Files.createDirectories(output);
    Path jar = output.resolve("baseline.jar");
    try (var archive = new JarOutputStream(Files.newOutputStream(jar))) {
      archive.putNextEntry(new JarEntry("META-INF/maven/io.github.saqqqner/diagram-kiln/pom.properties"));
      archive.write("groupId=io.github.saqqqner\nartifactId=diagram-kiln\nversion=1.0.1\n".getBytes());
      archive.closeEntry();
    }
    Path dependency = output.resolve("dependency.jar");
    Files.writeString(dependency, "fixture");
    Path classpath = output.resolve("classpath.txt");
    Files.writeString(classpath, dependency.toString());
    Properties valid = new Properties();
    valid.setProperty("coordinates", "io.github.saqqqner:diagram-kiln:1.0.1");
    valid.setProperty("sourceSha", "9404843d3e27f0baa9c06015b04104e2a83499fd");
    valid.setProperty("jarSha256", hash(jar));
    valid.setProperty("javaVersion", "17.0.20");
    valid.setProperty("mavenVersion", "Apache Maven 3.9.9");
    valid.setProperty("classpathFile", "classpath.txt");
    valid.setProperty("classpathSha256", hash(classpath));
    Path provenance = output.resolve("provenance.properties");
    store(valid, provenance);
    check(root, output, jar.toString(), provenance, true, "");
    check(root, output, output.resolve("missing.jar").toString(), provenance, false,
        "apiCompat.baselineJar");
    for (String key : List.of("coordinates", "sourceSha", "jarSha256", "classpathSha256")) {
      Properties invalid = new Properties();
      invalid.putAll(valid);
      invalid.setProperty(key, "incorrect");
      store(invalid, provenance);
      check(root, output, jar.toString(), provenance, false, key);
    }
    store(valid, provenance);
    Files.writeString(jar, "tampered");
    check(root, output, jar.toString(), provenance, false, "jarSha256");
    System.out.println("PASS baseline provenance: valid, missing, identity and checksum failures");
  }

  static void check(Path root, Path output, String jar, Path provenance, boolean succeeds,
      String diagnostic) throws Exception {
    Path log = output.resolve("last-validation.log");
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    int status = new ProcessBuilder(java, root.resolve("tools/ApiCompat.java").toString(),
        "validate", jar, provenance.toString(), output.resolve("report").toString())
        .redirectErrorStream(true).redirectOutput(log.toFile()).start().waitFor();
    if ((status == 0) != succeeds || !Files.readString(log).contains(diagnostic)) {
      throw new AssertionError(
          "Expected " + (succeeds ? "success" : diagnostic) + ": " + Files.readString(log));
    }
  }

  static String hash(Path path) throws Exception {
    return HexFormat.of()
        .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
  }

  static void store(Properties values, Path path) throws Exception {
    try (var output = Files.newOutputStream(path)) {
      values.store(output, "Validation test fixture");
    }
  }
}
