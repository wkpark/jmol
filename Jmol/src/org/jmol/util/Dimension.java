package org.jmol.util;

public class Dimension {

  public int height;
  public int width;
  
  public Dimension set(int w, int h) {
    width = w;
    height = h;
    return this;
  }

}
