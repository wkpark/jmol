package org.jmol.g3d;

import javajs.vec.P3i;

import org.jmol.util.JmolFont;

class TextString extends P3i {
  
  String text;
  JmolFont font;
  int argb, bgargb;

  void setText(String text, JmolFont font, int argb, int bgargb, int x, int y, int z) {
    this.text = text;
    this.font = font;
    this.argb = argb;
    this.bgargb = bgargb;
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  @Override
  public String toString() {
    return super.toString() + " " + text;
  }
}
