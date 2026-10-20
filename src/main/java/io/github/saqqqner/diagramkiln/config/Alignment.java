package io.github.saqqqner.diagramkiln.config;

/** Supported ELK 0.11.0 choices, independent of ELK types. */
public enum Alignment {
  /** Lets ELK select the alignment. */
  AUTOMATIC,
  /** Aligns towards the left. */
  LEFT,
  /** Aligns towards the right. */
  RIGHT,
  /** Aligns towards the top. */
  TOP,
  /** Aligns towards the bottom. */
  BOTTOM,
  /** Centers the layout. */
  CENTER
}
