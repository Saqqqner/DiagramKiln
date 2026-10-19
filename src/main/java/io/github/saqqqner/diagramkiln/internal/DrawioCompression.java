package io.github.saqqqner.diagramkiln.internal;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;

/**
 * Implements Graph.compress-compatible encoding for draw.io URL fragments.
 */
final class DrawioCompression {
  private DrawioCompression() {
  }

  static String fragment(String xml) {
    // draw.io decodes URI escapes after inflation; form-encoded spaces must use %20.
    byte[] encoded = URLEncoder.encode(xml, StandardCharsets.UTF_8).replace("+", "%20")
        .getBytes(StandardCharsets.US_ASCII);
    var deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    try {
      deflater.setInput(encoded);
      deflater.finish();
      var output = new ByteArrayOutputStream();
      var buffer = new byte[4096];
      while (!deflater.finished()) {
        int count = deflater.deflate(buffer);
        output.write(buffer, 0, count);
      }
      String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
      return "#R" + URLEncoder.encode(base64, StandardCharsets.UTF_8);
    }
    finally {
      deflater.end();
    }
  }

}
