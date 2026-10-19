package io.github.saqqqner.diagramkiln.style;

final class StyleValidation {
  private StyleValidation() {
  }

  static void color(String value, String name, boolean allowNone) {
    if (value == null || !(value.matches("#[0-9a-fA-F]{6}") || allowNone && value.equals("none"))) {
      throw new IllegalArgumentException(name + " must be #RRGGBB" + (allowNone ? " or none" : ""));
    }
  }

  static void number(double value, String name, boolean allowZero) {
    if (!Double.isFinite(value) || value < 0 || !allowZero && value == 0) {
      throw new IllegalArgumentException(
          name + " must be finite and " + (allowZero ? "nonnegative" : "positive"));
    }
  }
}
