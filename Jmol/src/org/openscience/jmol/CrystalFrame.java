package org.openscience.jmol;

import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3f;

class CrystalFrame extends ChemFrame {

  //the 3 primitives vectors
  private float[][] rprimd;

  //The box edges needed to draw the unit cell box
  //boxEdges.elementAt(0) = *origin* of the *first* edge of the box
  //boxEdges.elementAt(1) = *end* of the *first* edge of the box
  //boxEdges.elementAt(2) = *origin* of the *second* edge of the box
  //etc.
  private Vector boxEdges;

  void setRprimd(float[][] rprimd) {
    this.rprimd = rprimd;
  }

  float[][] getRprimd() {
    return this.rprimd;
  }

  void setBoxEdges(Vector boxEdges) {
    this.boxEdges = boxEdges;
  }


  Vector getBoxEdges() {
    return this.boxEdges;
  }

}
