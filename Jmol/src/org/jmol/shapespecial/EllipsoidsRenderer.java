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

  private Ellipsoids ellipsoids;
  private boolean drawDots, drawArcs, drawAxes, drawFill, drawBall;
  private boolean wireframeOnly;
  private int dotCount;
  private int[] coords;
  private Vector3f[] axes;
  private final float[] lengths = new float[3];
  private int diameter;
    
  protected void render() {
    ellipsoids = (Ellipsoids) shape;
    if (ellipsoids.mads == null)
      return;
    wireframeOnly = (viewer.getWireframeRotation() && viewer.getInMotion());
    drawAxes = viewer.getBooleanProperty("ellipsoidAxes");
    drawArcs = viewer.getBooleanProperty("ellipsoidArcs");
    drawBall = viewer.getBooleanProperty("ellipsoidBall") && !wireframeOnly;
    drawDots = viewer.getBooleanProperty("ellipsoidDots") && !wireframeOnly;
    drawFill = viewer.getBooleanProperty("ellipsoidFill") && !wireframeOnly;

    /* general logic:
     * 
     * 
     * 1) octant and DOTS are incompatible; octant preferred over dots
     * 2) If not BALL, ARCS, or DOTS, the rendering defaults to AXES
     * 3) If DOTS, then turn off ARCS and FILL
     * 
     * note that FILL serves to provide a cut-out for BALL and a 
     * filling for ARCS
     */
       
    if (drawBall)
      drawDots = false;
    if (!drawDots && !drawArcs && !drawBall)
      drawAxes = true;
    if (drawDots) {
      drawArcs = false;
      drawFill = false;
    }
  
    if (drawDots) {
      dotCount = ((Integer) viewer.getParameter("ellipsoidDotCount")).intValue();
      if (coords == null || coords.length != dotCount * 3)
        coords = new int[dotCount * 3];
    }
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
    coords = null;
  }

  private final Point3i[] screens = new Point3i[32];
  private final short[] normixes = new short[32];
  private final Point3f[] points = new Point3f[6];
  {
    for (int i = 0; i < points.length; i++)
      points[i] = new Point3f();
    for (int i = 0; i < screens.length; i++)
      screens[i] = new Point3i();
  }

  private static int[] axisPoints = {-1, 1, -2, 2, -3, 3};
  
  // octants are sets of three axisPoints references in proper rotation order
  // axisPoints[octants[i]] indicates the axis and direction (pos/neg)
  private static int[] octants = {
    0, 3, 5,
    0, 5, 2, //arc
    0, 2, 4,
    0, 4, 3, //arc
    1, 5, 2,
    1, 3, 5, //arc
    1, 4, 3,
    1, 2, 4  //arc
  };
 
  private void render1(Atom atom, short mad, Object[] ellipsoid) {
    s0.set(atom.screenX, atom.screenY, atom.screenZ);
    axes = (Vector3f[]) ellipsoid[0];
    float[] af = (float[]) ellipsoid[1];
    float f = mad / 100.0f * 4f;
    for (int i = 3; --i >= 0; )
      lengths[i] = af[i] * f;
    if (drawAxes || drawArcs || drawBall) 
      setAxes(atom, 1.0f);
    diameter = viewer.scaleToScreen(atom.screenZ, wireframeOnly ? 1 : 100);
    if (drawDots)
      renderDots(atom);
    if (drawAxes && !drawBall)
      renderAxes();  
    if (drawArcs && !drawBall)
      renderArcs(atom);
    if (drawBall) {
      renderBall(atom);
      if (drawArcs || drawAxes) {
        g3d.setColix(viewer.getColixBackgroundContrast());
        //setAxes(atom, 1.0f);
        if (drawAxes)
          renderAxes();
        if (drawArcs)
          renderArcs(atom);
        g3d.setColix(colix);
      }
    }
  }

  private void setAxes(Atom atom, float f) {
    for (int i = 0; i < 6; i++) {
      int iAxis = axisPoints[i];
      int i012 = Math.abs(iAxis) - 1;
      points[i].scaleAdd(f * lengths[i012] * (iAxis < 0 ? -1 : 1), axes[i012], atom);
      viewer.transformPoint(points[i], screens[i]);
    }
  }

  private void renderAxes() {
    if (Logger.debugging)
      g3d.setColix(Graphics3D.RED);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[0], screens[1]);
    if (Logger.debugging)
      g3d.setColix(Graphics3D.GREEN);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[2], screens[3]);
    if (Logger.debugging)
      g3d.setColix(Graphics3D.BLUE);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[4], screens[5]);
    if (Logger.debugging) {
      g3d.setColix(viewer.getColixBackgroundContrast());
      for (int i = 0; i < 6; i++) {
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[i],
            screens[(i + 2) % 6]);
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[i],
            screens[(i + 3) % 6]);
      }
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[1], screens[2]);
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[3], screens[4]);
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, screens[5], screens[0]);
    }
  }
  
  private void renderDots(Point3f ptAtom) {
    for (int i = 0; i < coords.length;) {
      float fx = (float) Math.random();
      float fy = (float) Math.random();
      fx *= (Math.random() > 0.5 ? -1 : 1);
      fy *= (Math.random() > 0.5 ? -1 : 1);
      float fz = (float) Math.sqrt(1 - fx * fx - fy * fy);
      if (Float.isNaN(fz))
        continue;
      fz = (float) (Math.random() > 0.5 ? -1 : 1) * fz;
      pt1.scaleAdd(fx * lengths[0], axes[0], ptAtom);
      pt1.scaleAdd(fy * lengths[1], axes[1], pt1);
      pt1.scaleAdd(fz * lengths[2], axes[2], pt1);
      viewer.transformPoint(pt1, s1);
      coords[i++] = s1.x;
      coords[i++] = s1.y;
      coords[i++] = s1.z;
    }
    g3d.drawPoints(dotCount, coords);
  }

  private void renderArcs(Point3f ptAtom) {
    for (int i = 1; i < 8; i += 2) {
      int pt = i*3;
      renderArc(ptAtom, octants[pt], octants[pt + 1]);
      renderArc(ptAtom, octants[pt + 1], octants[pt + 2]);
      renderArc(ptAtom, octants[pt + 2], octants[pt]);      
    }
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
  private final static float[] cossin = new float[36];
  static {
    for (int i = 5, pt = 0; i <= 90; i += 5) {
      cossin[pt++] = (float) Math.cos(i * toRadians);
      cossin[pt++] = (float) Math.sin(i * toRadians);
    }
  }
  
  private void renderArc(Point3f ptAtom, int ptA, int ptB) {
    v1.set(points[ptA]);
    v1.sub(ptAtom);
    v2.set(points[ptB]);
    v2.sub(ptAtom);
    float d1 = v1.length();
    float d2 = v2.length();
    v1.normalize();
    v2.normalize();
    v3.cross(v1, v2);
    pt1.set(points[ptA]);
    s1.set(screens[ptA]);
    boolean fillArc = drawFill && !drawBall;
    short normix = ellipsoids.g3d.get2SidedNormix(v3);
    if (!fillArc && !wireframeOnly)
      screens[6].set(s1);
    for (int i = 0, pt = 0; i < 18; i++, pt += 2) {
      pt2.scaleAdd(cossin[pt] * d1, v1, ptAtom);
      pt2.scaleAdd(cossin[pt + 1] * d2, v2, pt2);
      viewer.transformPoint(pt2, s2);
      if (fillArc)
        g3d.fillTriangle(s0, colix, normix, s1, colix, normix, s2, colix,
            normix);
      else if (wireframeOnly)
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, diameter, s1, s2);
      else
        screens[i + 7].set(s2);
      pt1.set(pt2);
      s1.set(s2);
    }
    if (!fillArc && !wireframeOnly)
      for (int i = 0; i < 18; i++)
        g3d.fillHermite(5, diameter, diameter, diameter, 
            screens[i == 0 ? i + 6 : i + 5], 
            screens[i + 6], 
            screens[i + 7], 
            screens[i == 17 ? i + 7 : i + 8]);
  }

  private void renderBall(Point3f ptAtom) {
    int iCutout = -1;
    int zMin = Integer.MAX_VALUE;
    if (drawFill)
      for (int i = 0; i < 8; i++) {
        int ptA = octants[i * 3];
        int ptB = octants[i * 3 + 1];
        int ptC = octants[i * 3 + 2];
        int z = screens[ptA].z + screens[ptB].z + screens[ptC].z;
        if (z < zMin) {
          zMin = z;
          iCutout = i;
        }
      }

    for (int i = 0; i < 8; i++) {
      int ptA = octants[i * 3];
      int ptB = octants[i * 3 + 1];
      int ptC = octants[i * 3 + 2];
      boolean isSwapped = (axisPoints[ptA] < 0);
      renderBall(ptAtom, axisPoints[ptA], axisPoints[ptB], axisPoints[ptC],
          isSwapped, iCutout == i);
    }
  }

  Vector3f a = new Vector3f();
  Vector3f b = new Vector3f();
  Vector3f c = new Vector3f();
  
  private void renderBall(Point3f ptAtom, int axisA, int axisB, int axisC,
                           boolean isSwapped, boolean cutoutOnly) {
    int nSegments = 16;
    int i;
    a.set(axes[i = (Math.abs(axisA) - 1)]);
    float la = lengths[i];
    b.set(axes[i = (Math.abs(axisB) - 1)]);
    float lb = lengths[i];
    c.set(axes[i = (Math.abs(axisC) - 1)]);
    float lc = lengths[i];
    if (axisA < 0)
      a.scale(-1);
    if (axisB < 0)
      b.scale(-1);
    if (axisC < 0)
      c.scale(-1);

    if (cutoutOnly) {
      renderCutout(ptAtom, a, b, la, lb, nSegments, isSwapped);
      renderCutout(ptAtom, a, c, la, lc, nSegments, isSwapped);
      renderCutout(ptAtom, c, b, lc, lb, nSegments, isSwapped);
      return;
    }
    short n2 = 0;
    short normix = 0;
    for (int ifx = 0, ify = 0, scrPt = 0; ifx < nSegments; ifx++) {
      float fx = ifx * 1f / (nSegments - 1);
      for (ify = 0; ify < nSegments; ify++) {
        float fy = ify * 1f / (nSegments - 1);
        float fz = (float) Math.sqrt(1 - fx * fx - fy * fy);
        if (Float.isNaN(fz)) {
          fy = (float) Math.sqrt(1 - fx * fx);
          fz = 0;
        }
        pt1.scaleAdd(fx * la, a, ptAtom);
        pt1.scaleAdd(fy * lb, b, pt1);
        pt1.scaleAdd(fz * lc, c, pt1);
        viewer.transformPoint(pt1, s1);
        v1.set(pt1);
        v1.sub(ptAtom);
        normix = g3d.getNormix(v1);
        scrPt = ify + 6;
        if (ify != 0) {
          if (ifx != 0) {
            if (isSwapped)
              g3d.fillQuadrilateral(s2, colix, n2, s1, colix, normix,
                  screens[scrPt], colix, normixes[scrPt], screens[scrPt - 1],
                  colix, normixes[scrPt - 1]);
            else
              g3d.fillQuadrilateral(screens[scrPt - 1], colix,
                  normixes[scrPt - 1], screens[scrPt], colix, normixes[scrPt],
                  s1, colix, normix, s2, colix, n2);
          }
          screens[scrPt - 1].set(s2);
          normixes[scrPt - 1] = n2;
        }
        s2.set(s1);
        n2 = normix;
      }
      screens[scrPt].set(s2);
      normixes[scrPt] = n2;
    }
  }

  private void renderCutout(Point3f ptAtom, Vector3f a, Vector3f b, 
                            float la, float lb, int nSegments, boolean isSwapped) {
    if (isSwapped)
      v3.cross(a, b);
    else
      v3.cross(b, a);
    short normix = ellipsoids.g3d.get2SidedNormix(v3);
    for (int ify = 0; ify < nSegments; ify++) {
      float fy = ify * 1f / (nSegments - 1);
      float fz = (float) Math.sqrt(1 - fy * fy);
      pt1.scaleAdd(fy * la, a, ptAtom);
      viewer.transformPoint(pt1, s1);
      pt1.scaleAdd(fz * lb, b, pt1);
      viewer.transformPoint(pt1, s2);
      if (ify > 0) {
        g3d.fillTriangle(s1, colix, normix, s2, colix, normix, screens[7],
            colix, normix);
        g3d.fillTriangle(s1, colix, normix, screens[7], colix, normix,
            screens[6], colix, normix);
      }
      screens[6].set(s1);
      screens[7].set(s2);
    }
  }

}
