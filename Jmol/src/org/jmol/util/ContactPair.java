package org.jmol.util;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;

public class ContactPair {
  public float[] radii = new float[2];
  public Point3f[] pts = new Point3f[2];
  public Point3f pt;
  public double volume = 0;
  public float score;
  public float d;
  public float chord;

  public ContactPair(Atom[] atoms, int i1, int i2, float R, float r) {
    radii[0] = R;
    radii[1] = r;
    pts[0] = atoms[i1];
    pts[1] = atoms[i2];

    //     ------d------------
    //    i1--------|->vdw1
    //        vdw2<-|--------i2
    //              pt

    Vector3f v = new Vector3f(pts[1]);
    v.sub(pts[0]);
    d = v.length();
    float f = (R - r + d) / (2 * d);

    //NOT float f = (vdw1*vdw1 - vdw2*vdw2 + dAB*dAB) / (2 * dAB*dAB);
    // as that would be for truly planar section, but it is not quite planar

    pt = new Point3f();
    pt.scaleAdd(f, v, pts[0]);

    // http://mathworld.wolfram.com/Sphere-SphereIntersection.html
    //  volume = pi * (R + r - d)^2 (d^2 + 2dr - 3r^2 + 2dR + 6rR - 3R^2)/(12d)

    volume = (R + r - d);
    volume *= Math.PI * volume
        * (d * d + 2 * d * r - 3 * r * r + 2 * d * R + 6 * r * R - 3 * R * R)
        / 12 / d;

    // chord check:
    double a = (d * d - r * r + R * R);
    chord = (float) Math.sqrt(4 * d * d * R * R - a * a) / d;
  }

  public void switchAtoms() {
    Point3f pt = pts[0];
    pts[0] = pts[1];
    pts[1] = pt;
    float r = radii[0];
    radii[0] = radii[1];
    radii[1] = r;
  }

}
