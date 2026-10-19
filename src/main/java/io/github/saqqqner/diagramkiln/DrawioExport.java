package io.github.saqqqner.diagramkiln;

/**
 * Immutable, eagerly generated export representations. Results from {@link DrawioExporter#export}
 * contain a fragment that decodes to the exact XML string. The public constructor only validates
 * nulls; it does not parse or verify arbitrary caller-supplied pairs. Accessors perform no work and
 * a completed result can be shared across threads.
 *
 * @param xml
 *          XML document; save using UTF-8
 * @param drawioFragment
 *          URI-safe draw.io #R fragment
 */
public record DrawioExport(String xml, String drawioFragment) {
  /**
   * Accepts non-null strings without parsing or verifying their correspondence.
   *
   * @param xml
   *          non-null XML representation
   * @param drawioFragment
   *          non-null fragment representation
   * @throws IllegalArgumentException
   *           if either component is null
   */
  public DrawioExport {
    if (xml == null || drawioFragment == null)
      throw new IllegalArgumentException("XML and draw.io fragment must not be null");
  }
}
