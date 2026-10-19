import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;

/** Java 17 source-file launcher for reproducible API compatibility preparation. */
class ApiCompat {
  static final String SHA = "9404843d3e27f0baa9c06015b04104e2a83499fd";
  static final String COORDINATES = "io.github.saqqqner:diagram-kiln:1.0.1";

  public static void main(String[] args) throws Exception {
    try {
      switch (args[0]) {
        case "bootstrap" -> bootstrap(Path.of(args[1]).toAbsolutePath().normalize(),
            Path.of(args[2]).toAbsolutePath().normalize(),
            args.length > 3 ? Path.of(args[3]) : null);
        case "validate" -> validate(args[1], args[2], Path.of(args[3]));
        case "consumer" -> consumer(args);
        default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
      }
    }
    catch (Exception e) {
      System.err.println("API compatibility: " + e.getMessage());
      System.err.println("See docs/API-COMPATIBILITY.md; prepare the pinned baseline with "
          + "java tools/ApiCompat.java bootstrap <repository> <cache>.");
      System.exit(1);
    }
  }

  static void bootstrap(Path repository, Path cache, Path suppliedSource) throws Exception {
    if (Runtime.version().feature() != 17) {
      throw new IllegalArgumentException(
          "Baseline bootstrap requires JDK 17; found " + Runtime.version());
    }
    if (cache.startsWith(repository.resolve("target"))) {
      throw new IllegalArgumentException("Baseline cache must be outside current target/");
    }
    Files.createDirectories(cache);
    Path source = suppliedSource == null
        ? cache.resolve("source")
        : suppliedSource.toAbsolutePath();
    if (suppliedSource == null && !Files.exists(source)) {
      run(repository, null, List.of("git", "clone", "--no-hardlinks", "--no-checkout",
          repository.toString(), source.toString()));
      run(source, null, List.of("git", "checkout", "--detach", SHA));
    }
    if (source.toRealPath().equals(repository.toRealPath())) {
      throw new IllegalArgumentException("Baseline sources must use a separate checkout");
    }
    if (!capture(source, List.of("git", "rev-parse", "HEAD")).strip().equals(SHA)) {
      throw new IllegalArgumentException("Baseline checkout must be at " + SHA);
    }
    if (!capture(source, List.of("git", "status", "--porcelain")).isBlank()) {
      throw new IllegalArgumentException(
          "Baseline checkout contains modifications or untracked files");
    }
    List<String> command = maven(source);
    List<String> build = new ArrayList<>(command);
    build.addAll(
        List.of("-B", "-ntp", "-DskipTests", "clean", "package", "dependency:build-classpath",
            "-DincludeScope=runtime", "-Dmdep.outputFile=" + cache.resolve("classpath.txt")));
    run(source, cache.resolve("bootstrap.log"), build);
    Path jar = cache.resolve("diagram-kiln-1.0.1.jar");
    Files.copy(source.resolve("target/diagram-kiln-1.0.1.jar"), jar,
        StandardCopyOption.REPLACE_EXISTING);
    Properties provenance = new Properties();
    provenance.setProperty("coordinates", COORDINATES);
    provenance.setProperty("sourceSha", SHA);
    provenance.setProperty("jarSha256", hash(jar));
    provenance.setProperty("javaVersion", System.getProperty("java.version"));
    List<String> version = new ArrayList<>(command);
    version.add("--version");
    provenance.setProperty("mavenVersion", capture(source, version).strip());
    provenance.setProperty("classpathFile", "classpath.txt");
    provenance.setProperty("classpathSha256", hash(cache.resolve("classpath.txt")));
    try (var out = Files.newOutputStream(cache.resolve("provenance.properties"))) {
      provenance.store(out, "Pinned DiagramKiln API baseline; local checksum is not attestation");
    }
    validate(jar.toString(), cache.resolve("provenance.properties").toString(),
        cache.resolve("validation"));
    System.out.println("Prepared baseline: " + jar);
  }

  static Properties validate(String jarValue, String provenanceValue, Path report)
      throws Exception {
    Path jar = requiredFile(jarValue, "apiCompat.baselineJar");
    Path provenanceFile = requiredFile(provenanceValue, "apiCompat.baselineProvenance");
    Properties provenance = load(provenanceFile);
    require(provenance, "coordinates", COORDINATES);
    require(provenance, "sourceSha", SHA);
    require(provenance, "jarSha256", hash(jar));
    if (!provenance.getProperty("javaVersion", "").matches("17(?:\\..*)?")) {
      throw new IllegalArgumentException("Baseline provenance must record JDK 17");
    }
    if (!provenance.getProperty("mavenVersion", "").contains("Apache Maven")) {
      throw new IllegalArgumentException("Baseline provenance must record Maven version");
    }
    try (JarFile archive = new JarFile(jar.toFile())) {
      var entry = archive.getJarEntry("META-INF/maven/io.github.saqqqner/diagram-kiln/pom.properties");
      if (entry == null) {
        throw new IllegalArgumentException("Baseline JAR has no Maven coordinates");
      }
      Properties coordinates = new Properties();
      try (var input = archive.getInputStream(entry)) {
        coordinates.load(input);
      }
      require(coordinates, "groupId", "io.github.saqqqner");
      require(coordinates, "artifactId", "diagram-kiln");
      require(coordinates, "version", "1.0.1");
    }
    Path classpath = provenanceFile.getParent()
        .resolve(provenance.getProperty("classpathFile", "classpath.txt")).normalize();
    if (!Files.isRegularFile(classpath)) {
      throw new IllegalArgumentException("Baseline runtime classpath missing: " + classpath);
    }
    require(provenance, "classpathSha256", hash(classpath));
    checkClasspath(Files.readString(classpath).strip(), jar);
    Files.createDirectories(report);
    Files.copy(provenanceFile, report.resolve("baseline-provenance.properties"),
        StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(report.resolve("baseline.txt"), "JAR: " + jar + "\nCoordinates: "
        + COORDINATES + "\nSource: " + SHA + "\nSHA-256: " + hash(jar) + "\n");
    provenance.setProperty("resolvedClasspath", Files.readString(classpath).strip());
    return provenance;
  }

  static void consumer(String[] args) throws Exception {
    Path report = Path.of(args[3]).toAbsolutePath();
    Properties baseline = validate(args[1], args[2], report);
    Path current = requiredFile(args[4], "current packaged JAR");
    Path cpFile = requiredFile(args[5], "current runtime classpath");
    String currentDependencies = Files.readString(cpFile).strip();
    checkClasspath(currentDependencies, Path.of(args[1]));
    String oldCp = Path.of(args[1]).toAbsolutePath() + File.pathSeparator
        + baseline.getProperty("resolvedClasspath");
    String newCp = current + File.pathSeparator + currentDependencies;
    Files.writeString(report.resolve("current.txt"), "JAR: " + current + "\nSHA-256: "
        + hash(current) + "\nRuntime dependencies: " + currentDependencies + "\n");
    Path source = Path.of(args[6]);
    Path oldOutput = report.resolve("consumer-old");
    Path newOutput = report.resolve("consumer-new");
    Files.createDirectories(oldOutput);
    Files.createDirectories(newOutput);
    compile(source, oldOutput, oldCp);
    run(Path.of(".").toAbsolutePath(), report.resolve("historical-runtime.log"), List.of(java(),
        "-cp", oldOutput + File.pathSeparator + newCp, "HistoricalConsumer", current.toString()));
    compile(source, newOutput, newCp);
    Files.writeString(report.resolve("historical-consumer.txt"),
        "PASS: Java 17 source compiled against baseline.\n"
            + "PASS: baseline bytecode ran against current packaged JAR (no baseline on classpath).\n"
            + "PASS: same source compiled separately against current packaged JAR.\n"
            + "Old compile classpath: " + oldCp + "\nNew compile/runtime classpath: " + newCp
            + "\n");
  }

  static void compile(Path source, Path output, String classpath) throws IOException {
    int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17",
        "-encoding", "UTF-8", "-classpath", classpath, "-d", output.toString(), source.toString());
    if (result != 0) {
      throw new IllegalArgumentException("Historical consumer compilation failed: " + source);
    }
  }

  static void checkClasspath(String classpath, Path forbiddenJar) throws IOException {
    for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
      Path dependency = Path.of(entry);
      if (!Files.isRegularFile(dependency)) {
        throw new IllegalArgumentException("Unresolved runtime dependency: " + dependency
            + "; bootstrap online first, then use prepared caches for -o");
      }
      if (dependency.toRealPath().equals(forbiddenJar.toRealPath())
          || dependency.getFileName().toString().startsWith("diagram-kiln-")) {
        throw new IllegalArgumentException(
            "Library JAR must not occur in dependency classpath: " + dependency);
      }
    }
  }

  static Path requiredFile(String value, String name) {
    if (value == null || value.isBlank() || value.contains("${")) {
      throw new IllegalArgumentException("Missing " + name + "; supply an absolute path");
    }
    Path file = Path.of(value);
    if (!file.isAbsolute() || !Files.isRegularFile(file)) {
      throw new IllegalArgumentException(
          "Missing/invalid " + name + ": " + file + "; supply an existing absolute file path");
    }
    return file.normalize();
  }

  static Properties load(Path file) throws IOException {
    Properties result = new Properties();
    try (var input = Files.newInputStream(file)) {
      result.load(input);
    }
    return result;
  }

  static void require(Properties values, String key, String expected) {
    if (!expected.equals(values.getProperty(key))) {
      throw new IllegalArgumentException(
          "Invalid " + key + ": expected " + expected + ", found " + values.getProperty(key));
    }
  }

  static String hash(Path file) throws Exception {
    return HexFormat.of()
        .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
  }

  static String java() {
    return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java")
        .toString();
  }

  static boolean isWindows() {
    return System.getProperty("os.name").startsWith("Windows");
  }

  static List<String> maven(Path source) {
    return isWindows()
        ? List.of("cmd.exe", "/d", "/c", source.resolve("mvnw.cmd").toString())
        : List.of("bash", source.resolve("mvnw").toString());
  }

  static String capture(Path directory, List<String> command) throws Exception {
    Process process = process(directory, command).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
      throw new IllegalArgumentException("Command failed: " + command + "\n" + output);
    }
    return output;
  }

  static void run(Path directory, Path log, List<String> command) throws Exception {
    ProcessBuilder builder = process(directory, command);
    if (log == null) {
      builder.inheritIO();
    }
    else {
      Files.createDirectories(log.getParent());
      builder.redirectOutput(log.toFile());
    }
    if (builder.start().waitFor() != 0) {
      throw new IllegalArgumentException(
          "Command failed: " + command + (log == null ? "" : "; see " + log));
    }
  }

  static ProcessBuilder process(Path directory, List<String> command) {
    ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
        .redirectErrorStream(true);
    builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
    return builder;
  }
}
