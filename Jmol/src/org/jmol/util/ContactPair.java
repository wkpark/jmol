package org.jmol.util;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;

public class ContactPair {
  public int iAtom1, iAtom2;
  public float radius1, radius2;
  public Point3f pt1, pt2;
  public Point3f pt;

  public ContactPair(Atom[] atoms, int i1, int i2, float vdw1, float vdw2) {
    iAtom1 = i1;
    iAtom2 = i2;
    radius1 = vdw1;
    radius2 = vdw2;
    pt1 = atoms[i1];
    pt2 = atoms[i2];

    //     ------d------------
    //    i1--------|->vdw1
    //        vdw2<-|--------i2
    //              pt

    Vector3f v = new Vector3f(pt2);
    v.sub(pt1);
    float dAB = v.length();
    float f = (vdw1 - vdw2 + dAB) / (2 * dAB);

    //NOT float f = (vdw1*vdw1 - vdw2*vdw2 + dAB*dAB) / (2 * dAB*dAB);
    // as that would be for truly planar section, but it is not quite planar

    pt = new Point3f();
    pt.scaleAdd(f, v, pt1);
  }

  public void switchAtoms() {
    Point3f pt = pt1;
    pt1 = pt2;
    pt2 = pt;
    int i = iAtom1;
    iAtom1 = iAtom2;
    iAtom2 = i;
    float r = radius1;
    radius1 = radius2;
    radius2 = r;
  }

  @Override
  public String toString() {
    return "[" + iAtom1 + "," + iAtom2 + "]";
  }

}
