package io.github.saqqqner.diagramkiln;

import io.github.saqqqner.diagramkiln.config.LayoutOptions;
import io.github.saqqqner.diagramkiln.dto.EdgeLabelSize;
import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyLayoutCompatibilityTest {
  @TempDir
  Path directory;

  @Test
  void linksOldCompiledImplementationAndLambdaAgainstTheNewDefaultMethod() throws Exception {
    // Freeze the pre-1.2 SPI: compile callers without seeing the new default operation.
    var spi = directory.resolve("LayoutEngine.java");
    Files.writeString(spi, """
        package io.github.saqqqner.diagramkiln.layout;
        import io.github.saqqqner.diagramkiln.DiagramData;
        import io.github.saqqqner.diagramkiln.config.LayoutOptions;
        import io.github.saqqqner.diagramkiln.dto.LayoutResult;
        @FunctionalInterface
        public interface LayoutEngine {
          LayoutResult layout(DiagramData diagram, LayoutOptions options);
        }
        """);
    var caller = directory.resolve("LegacyEngine.java");
    Files.writeString(caller, """
        import io.github.saqqqner.diagramkiln.*;
        import io.github.saqqqner.diagramkiln.config.LayoutOptions;
        import io.github.saqqqner.diagramkiln.dto.LayoutResult;
        import io.github.saqqqner.diagramkiln.layout.LayoutEngine;
        import java.util.List;
        public class LegacyEngine implements LayoutEngine {
          public LayoutResult layout(DiagramData diagram, LayoutOptions options) {
            return new LayoutResult(List.of(), List.of());
          }
          public static LayoutEngine lambda() {
            return (diagram, options) -> new LayoutResult(List.of(), List.of());
          }
        }
        """);
    assertEquals(0,
        ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-classpath",
            System.getProperty("java.class.path"), "-d", directory.toString(), spi.toString(),
            caller.toString()));
    // The old interface must not be present at runtime, only the already compiled clients.
    Files.delete(directory.resolve("io/github/saqqqner/diagramkiln/layout/LayoutEngine.class"));
    try (var loader = new URLClassLoader(new java.net.URL[]
    {directory.toUri().toURL()
    }, getClass().getClassLoader())) {
      var type = loader.loadClass("LegacyEngine");
      var concrete = (LayoutEngine) type.getConstructor().newInstance();
      var lambda = (LayoutEngine) type.getMethod("lambda").invoke(null);
      var data = Diagrams.diagram("Legacy binary").build();
      for (var engine : new LayoutEngine[]
      {concrete, lambda
      }) {
        assertTrue(
            DrawioExporter.builder().layout(engine).build().toXml(data).contains("mxGraphModel"));
        assertThrows(UnsupportedOperationException.class, () -> engine.layoutWithEdgeLabels(data,
            LayoutOptions.builder().build(), new EdgeLabelSize(80, 20)));
      }
    }
  }
}
