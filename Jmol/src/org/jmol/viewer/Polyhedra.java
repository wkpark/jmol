/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.viewer;

import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

class Polyhedra extends SelectionIndependentShape {

  final static float DEFAULT_MAX_FACTOR = 1.85f;
  final static float DEFAULT_FACE_CENTER_OFFSET = 0.25f;
  final static int EDGES_NONE = 0;
  final static int EDGES_ALL = 1;
  final static int EDGES_FRONT = 2;

  int polyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];
  float radius;
  int nBonds;
  float faceCenterOffset;
  float maxFactor;
  int drawEdges;
  
  boolean isCollapsed;
  boolean iHaveCenterBitSet;
  boolean iHaveVertexBitSet;
  
  BitSet centers;
  BitSet bsVertices;
  
  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bs) {

    //System.out.println(propertyName + " " + value);
    if (propertyName.equalsIgnoreCase("init")) { 
      faceCenterOffset = DEFAULT_FACE_CENTER_OFFSET;
      maxFactor = DEFAULT_MAX_FACTOR;
      radius = 0.0f;
      nBonds = 0;
      bsVertices = null;
      centers = null;
      isCollapsed = iHaveCenterBitSet = iHaveVertexBitSet = false;
      drawEdges = EDGES_NONE;
    }
      
    if (propertyName.equalsIgnoreCase("generate")) {
      if (!iHaveCenterBitSet)
        centers = bs;
      deletePolyhedra();
      buildPolyhedra(radius == 0f);
      return;
    }

    if (propertyName.equalsIgnoreCase("collapsed")) {
      isCollapsed = true;
      return;
    }

    if (propertyName.equalsIgnoreCase("nbonds")) {
      if(value instanceof Integer)
        nBonds = ((Integer)value).intValue();
      else
        nBonds = 100;
      return;
    }

    if (propertyName.equalsIgnoreCase("centers")) {
      centers = (BitSet)value;
      iHaveCenterBitSet = true;
    }

    if (propertyName.equalsIgnoreCase("to")) {
      bsVertices = (BitSet)value;
    }

    if (propertyName.equalsIgnoreCase("faceCenterOffset")) {
      if(value instanceof Float)
        faceCenterOffset = ((Float)value).floatValue();
      return;
    }

    if (propertyName.equalsIgnoreCase("maxFactor")) {
      if(value instanceof Float)
        maxFactor = ((Float)value).floatValue();
      return;
    }
    
    if ("bonds" == propertyName) {
      //deprecated
      return;
    }

    if ("delete" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      deletePolyhedra();
      return;
    }
    if ("on" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(true);
      return;
    }
    if ("off" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(false);
      return;
    }
    if ("noedges" == propertyName) {
      drawEdges = EDGES_NONE;
      return;
    }
    if ("edges" == propertyName) {
      drawEdges = EDGES_ALL;
      return;
    }
    if ("frontedges" == propertyName) {
      drawEdges = EDGES_FRONT;
      return;
    }
    if ("color" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      colix = Graphics3D.getColix(value);
      setColix(colix,
               (colix != Graphics3D.UNRECOGNIZED) ? null : (String) value);
      return;
    }
    
    if ("translucency" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
        colix = Graphics3D.getColix(value);
      setTranslucent("translucent" == value);
      return;
    }
    if ("radius" == propertyName) {
      radius = ((Float)value).floatValue();
      return;
    }
  }

  void deletePolyhedra() {
    int newCount = 0;
    for (int i = 0; i < polyhedronCount; ++i) {
      Polyhedron p = polyhedrons[i];
      if (! centers.get(p.centralAtom.atomIndex))
        polyhedrons[newCount++] = p;
    }
    for (int i = newCount; i < polyhedronCount; ++i)
      polyhedrons[i] = null;
    polyhedronCount = newCount;
  }

  void setVisible(boolean visible) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (centers.get(p.centralAtom.atomIndex))
        p.visible = visible;
    }
  }

  void setColix(short colix, String palette) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      int atomIndex = p.centralAtom.atomIndex;
      if (centers.get(atomIndex)) {
        p.polyhedronColix =
          ((colix != Graphics3D.UNRECOGNIZED)
           ? colix
           : viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette));
      }
    }
  }

  void setTranslucent(boolean isTranslucent) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (centers.get(p.centralAtom.atomIndex))
        p.polyhedronColix =
          Graphics3D.setTranslucent(p.polyhedronColix, isTranslucent);
    }
  }

  void savePolyhedron(Polyhedron p) {
    if (polyhedronCount == polyhedrons.length)
      polyhedrons = (Polyhedron[])Util.doubleLength(polyhedrons);
    polyhedrons[polyhedronCount++] = p;
  }

  void buildPolyhedra(boolean isBonds) {
    for (int i = frame.atomCount; --i >= 0;) {
      if (centers.get(i)) {
        Polyhedron p = (isBonds ? constructBondsPolyhedron(i)
            : constructRadiusPolyhedron(i));
        if (p != null)
          savePolyhedron(p);
      }
    }
  }

  final static int MAX_VERTICES = 85;
  final static int MAX_POINTS = 256;
  final static int FACE_COUNT_MAX = 85;
  Atom[] otherAtoms = new Atom[MAX_VERTICES];
  Polyhedron constructBondsPolyhedron(int atomIndex) {
    Atom atom = frame.getAtomAt(atomIndex);
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return null;
    int bondCount = 0;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      Atom otherAtom = bond.atom1 == atom ? bond.atom2 : bond.atom1;
      if (bsVertices != null && !bsVertices.get(otherAtom.atomIndex))
        continue;
      if (bondCount == MAX_VERTICES)
        break;
      otherAtoms[bondCount++] = otherAtom;
    }
    if (bondCount < 2 || nBonds > 0 && nBonds != bondCount)
      return null;
    return validatePolyhedronNew(atom, bondCount, otherAtoms);
  }

  Polyhedron constructRadiusPolyhedron(int atomIndex) {
    Atom atom = frame.getAtomAt(atomIndex);
    int otherAtomCount = 0;
    AtomIterator withinIterator = frame.getWithinModelIterator(atom, radius);
    while (withinIterator.hasNext()) {
      Atom other = withinIterator.next();
      if (other == atom || bsVertices != null
          && !bsVertices.get(other.atomIndex))
        continue;
      if (otherAtomCount == MAX_VERTICES)
        break;
      otherAtoms[otherAtomCount++] = other;
    }
    if (otherAtomCount < 2 || nBonds > 0 && nBonds != otherAtomCount)
      return null;
    return validatePolyhedronNew(atom, otherAtomCount, otherAtoms);
  }

  short[] normixesT = new short[FACE_COUNT_MAX];
  byte[] planesT = new byte[256];

  Polyhedron validatePolyhedronNew(Atom centralAtom, int vertexCount,
                                   Atom[] otherAtoms) {
    Vector3f normal;
    int planeCount = 0;
    int ipt = 0;
    int ptCenter = vertexCount;
    int nPoints = ptCenter + 1;
    float distMax = 0;

    Point3f[] points = new Point3f[MAX_POINTS];
    points[ptCenter] = centralAtom.point3f;
    otherAtoms[ptCenter] = centralAtom;
    for (int i = 0; i < ptCenter; i++) {
      points[i] = otherAtoms[i].point3f;
      distMax += points[ptCenter].distance(points[i]);
    }
    distMax = distMax / ptCenter * maxFactor;
    
    // simply define a face to be when all three distances 
    // are < MAX_FACTOR * longest central
    // collapsed trick is that introduce a "simple" atom
    // near the center but not quite the center, so that our planes on
    // either side of the facet don't overlap. We step out maxFactor * normal

    // also needed: consideration for faces involving more than three atoms

    out: 
    for (int i = 0; i < ptCenter - 2; i++)
      for (int j = i + 1; j < ptCenter - 1; j++) {
        if (points[i].distance(points[j]) > distMax)
          continue;
        for (int k = j + 1; k < ptCenter; k++) {
          if (points[i].distance(points[k]) > distMax
              || points[j].distance(points[k]) > distMax)
            continue;
          normal = getNormalFromCenter(points[ptCenter], points[i], points[j],
              points[k], true);
          if (isCollapsed) {
            normal.scale(faceCenterOffset);
            points[nPoints] = new Point3f(points[ptCenter]);
            points[nPoints].add(normal);
            otherAtoms[nPoints] = new Atom(points[nPoints]);
            planesT[ipt++] = (byte) nPoints;
            planesT[ipt++] = (byte) j;
            planesT[ipt++] = (byte) k;
            normal = getNormalFromCenter(points[i], points[ptCenter],
                points[j], points[k], false);
            normixesT[planeCount++] = g3d.getNormix(normal);
            planesT[ipt++] = (byte) i;
            planesT[ipt++] = (byte) nPoints;
            planesT[ipt++] = (byte) k;
            normal = getNormalFromCenter(points[j], points[i],
                points[ptCenter], points[k], false);
            normixesT[planeCount++] = g3d.getNormix(normal);
            planesT[ipt++] = (byte) i;
            planesT[ipt++] = (byte) j;
            planesT[ipt++] = (byte) nPoints;
            normal = getNormalFromCenter(points[k], points[i], points[j],
                points[ptCenter], false);
            normixesT[planeCount++] = g3d.getNormix(normal);
            nPoints++;
          } else {
            planesT[ipt++] = (byte) i;
            planesT[ipt++] = (byte) j;
            planesT[ipt++] = (byte) k;
            normixesT[planeCount++] = g3d.getNormix(normal);
          }
          if (planeCount == FACE_COUNT_MAX)
            break out;
        }
      }
    return new Polyhedron(centralAtom, nPoints, planeCount, otherAtoms,
        normixesT, planesT);
  }
  
  Point3f ptT = new Point3f();
  Point3f ptT2 = new Point3f();
  Vector3f getNormalFromCenter(Point3f ptCenter, Point3f ptA, Point3f ptB,
                            Point3f ptC, boolean isSolid) {
    Vector3f normal = new Vector3f();
    g3d.calcNormalizedNormal(ptA, ptB, ptC, normal);
    //but which way is it? add N to A and see who is closer to Center, A or N. 
    ptT.set(ptA);
    ptT.add(ptB);
    ptT.add(ptC);
    ptT.scale(1/3f);
    ptT2.set(normal);
    ptT2.scale(0.1f);
    ptT2.add(ptT);
    //              A      C
    //                \   /
    //                 \ / 
    //                  x pT is center of ABC; ptT2 is offset a bit from that
    //                  |    either closer to x (ok if not opaque) or further
    //                  |    from x (ok if opaque)
    //                  B
    // in the case of facet ABx, the "center" is really the OTHER point, C.
    if (isSolid && ptCenter.distance(ptT2) < ptCenter.distance(ptT)
        || !isSolid && ptCenter.distance(ptT) < ptCenter.distance(ptT2))
      normal.scale(-1f);
    return normal;
  }

  class Polyhedron {
    final Atom centralAtom;
    final Atom[] vertices;
    boolean visible;
    final short[] normixes;
    short polyhedronColix;
    byte[] planes;
    int planeCount;

    Polyhedron(Atom centralAtom, int nPoints, int planeCount,
        Atom[] otherAtoms, short[] normixes, byte[] planes) {
      this.centralAtom = centralAtom;
      this.vertices = new Atom[nPoints];
      this.visible = true;
      this.polyhedronColix = colix;
      this.normixes = new short[planeCount];
      this.planeCount = planeCount;
      this.planes = new byte[planeCount * 3];
      for (int i = nPoints; --i >= 0;)
        vertices[i] = otherAtoms[i];
      for (int i = planeCount; --i >= 0;)
        this.normixes[i] = normixes[i];
      for (int i = planeCount * 3; --i >= 0;)
        this.planes[i] = planes[i];
    }   
  }
}
