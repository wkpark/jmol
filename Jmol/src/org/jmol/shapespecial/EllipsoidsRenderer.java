/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-08 22:18:02 -0500 (Mon, 08 Oct 2007) $
 * $Revision: 8391 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapespecial;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Shape;
import org.jmol.shape.ShapeRenderer;
import org.jmol.util.Logger;

public class EllipsoidsRenderer extends ShapeRenderer {

  Ellipsoids ellipsoids;
  protected void render() {
    ellipsoids = (Ellipsoids) shape;
    if (ellipsoids.mads == null)
      return;
    Atom[] atoms = modelSet.atoms;
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || modelSet.isAtomHidden(i))
        continue;
      Object[] ellipsoid = atom.getEllipsoid();
      if (ellipsoid == null)
        continue;
      colix = Shape.getColix(ellipsoids.colixes, i, atom);
      if (!g3d.setColix(colix))
        continue;
      render1(atom, ellipsoids.mads[i], ellipsoid);
    }
  }

  private final Point3i[] screens = new Point3i[6];
  private final Point3f[] points = new Point3f[6];
  private final Point3f[] ptEllipse = new Point3f[72];
  {
    for (int i = 0; i < 6; i++) {
      screens[i] = new Point3i();
      points[i] = new Point3f();
    }
    for (int i = 0; i < 72; i++)
      ptEllipse[i] = new Point3f();
  }

  private int d;
  private final float[] lengths = new float[3];
  private void render1(Atom atom, short mad, Object[] ellipsoid) {
    //for now a simple object
    Vector3f[] axes = (Vector3f[]) ellipsoid[0];
    float[] lengths = (float[]) ellipsoid[1];
    float f = mad / 100.0f * 4f;
    points[0].scaleAdd(-lengths[0] * f, axes[0], atom);
    points[1].scaleAdd(this.lengths[0] = lengths[0] * f, axes[0], atom);
    points[2].scaleAdd(-lengths[1] * f, axes[1], atom);
    points[3].scaleAdd(this.lengths[1] = lengths[1] * f, axes[1], atom);
    points[4].scaleAdd(-lengths[2] * f, axes[2], atom);
    points[5].scaleAdd(this.lengths[2] = lengths[2] * f, axes[2], atom);
    for (int i = 0; i < 6; i++)
      viewer.transformPoint(points[i], screens[i]);
    d = viewer.scaleToScreen(atom.screenZ, 3);
    d -= (d & 1) ^ 1; // round down to odd value
    renderAxes();  
    s0.set(atom.screenX, atom.screenY, atom.screenZ);
    renderSegments(atom);
  }

  private void renderAxes() {
    if (Logger.debugging)
      g3d.setColix(Graphics3D.RED);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[0], screens[1]);
    if (Logger.debugging)
      g3d.setColix(Graphics3D.GREEN);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[2], screens[3]);
    if (Logger.debugging)
      g3d.setColix(Graphics3D.BLUE);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[4], screens[5]);
    if (Logger.debugging) {
      g3d.setColix(viewer.getColixBackgroundContrast());
      for (int i = 0; i < 6; i++) {
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[i],
            screens[(i + 2) % 6]);
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[i],
            screens[(i + 3) % 6]);
      }
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[1], screens[2]);
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[3], screens[4]);
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[5], screens[0]);
    }
  }
  
  private void renderSegments(Atom atom) {
    renderOctant(atom, 0, 2, 5);
    renderOctant(atom, 2, 4, 1);
    renderOctant(atom, 4, 3, 0);
    renderOctant(atom, 1, 3, 5);
  }
  
  private void renderOctant(Atom atom, int pt1, int pt2, int pt3) {
    renderSegment(atom, pt1, pt2); 
    renderSegment(atom, pt2, pt3); 
    renderSegment(atom, pt3, pt1); 
  }
  
  private final Vector3f v1 = new Vector3f();
  private final Vector3f v2 = new Vector3f();
  private final Vector3f v3 = new Vector3f();
  private final Point3f pt1 = new Point3f();
  private final Point3f pt2 = new Point3f();
  private final Point3i s0 = new Point3i();
  private final Point3i s1 = new Point3i();
  private final Point3i s2 = new Point3i();
  
  private final static float toRadians = (float) Math.PI/180f;
  
  private void renderSegment(Atom atom, int ptA, int ptB) {
    v1.set(points[ptA]);
    v1.sub(atom);
    v2.set(points[ptB]);
    v2.sub(atom);
    float d1 = v1.length();
    float d2 = v2.length();
    v1.normalize();
    v2.normalize();
    v3.cross(v1, v2);
    pt1.set(points[ptA]);
    s1.set(screens[ptA]);
    short normix = ellipsoids.g3d.get2SidedNormix(v3);
    for (int i = 5; i <= 90; i+=5) {
      float cosTheta = (float) Math.cos(i * toRadians);
      float sinTheta = (float) Math.sin(i * toRadians);
      pt2.scaleAdd(cosTheta * d1, v1, atom);
      pt2.scaleAdd(sinTheta * d2, v2, pt2);
      viewer.transformPoint(pt2, s2);
      g3d.fillTriangle(s0, colix, normix, s1, colix, normix, s2, colix, normix);
      //g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, s1, s2);
      pt1.set(pt2);
      s1.set(s2);
    }    
  }
}
