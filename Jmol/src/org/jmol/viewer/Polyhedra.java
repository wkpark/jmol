/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2006  The Jmol Development Team
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

  final static float DEFAULT_CENTRAL_ANGLE_MAX = 145f / 180f * 3.1415926f;
  final static float DEFAULT_FACE_NORMAL_MAX = 30f / 180f * 3.1415926f;
  final static float DEFAULT_FACE_CENTER_OFFSET = 0.25f;
  final static int EDGES_NONE = 0;
  final static int EDGES_ALL = 1;
  final static int EDGES_FRONT = 2;

  // Bob, please set these to reasonable values
  final static int MINIMUM_ACCEPTABLE_VERTEX_COUNT = 3;
  final static int MAXIMUM_ACCEPTABLE_VERTEX_COUNT = 20;
  final static int FACE_COUNT_MAX = 85;

  final static boolean debugging = false;
  
  int polyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];
  float radius;
  int acceptableVertexCountCount = 0;
  // assume that 8 is enough ... if you need more just make this array bigger
  int acceptableVertexCounts[] = new int [8];
  float faceCenterOffset;
  float centralAngleMax;
  float faceNormalMax;
  int drawEdges;
  
  boolean isCollapsed;
  
  BitSet bsCenters;
  BitSet bsVertices;
  
  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("init" == propertyName) {
      faceCenterOffset = DEFAULT_FACE_CENTER_OFFSET;
      centralAngleMax = DEFAULT_CENTRAL_ANGLE_MAX;
      faceNormalMax = DEFAULT_FACE_NORMAL_MAX;
      radius = 0.0f;
      acceptableVertexCountCount = 0;
      bsCenters = bsVertices = null;
      isCollapsed = false;
      drawEdges = EDGES_NONE;
      return;
    }
    if ("radius" == propertyName) {
      if (value instanceof Float)
        radius = ((Float)value).floatValue();
      else
        invalidPropertyType(propertyName, value, "Float");
      return;
    }
    if ("bonds" == propertyName) {
      radius = 0; // radius == 0 is the flag for using bonds
      return;
    }
    if ("vertexCount" == propertyName) {
      if (value instanceof Integer) {
        if (acceptableVertexCountCount < acceptableVertexCounts.length) {
          int vertexCount = ((Integer)value).intValue();
          if (vertexCount >= MINIMUM_ACCEPTABLE_VERTEX_COUNT &&
              vertexCount <= MAXIMUM_ACCEPTABLE_VERTEX_COUNT)
            acceptableVertexCounts[acceptableVertexCountCount++] = vertexCount;
        }
      } else {
        invalidPropertyType(propertyName, value, "Integer");
      }
      return;
    }
    if ("potentialCenterSet" == propertyName) {
      if (value instanceof BitSet)
        bsCenters = (BitSet)value;
      else
        invalidPropertyType(propertyName, value, "BitSet");
      return;
    }
    if ("potentialVertexSet" == propertyName) {
      if (value instanceof BitSet)
        bsVertices = (BitSet)value;
      else
        invalidPropertyType(propertyName, value, "BitSet");
      return;
    }
    if ("faceCenterOffset" == propertyName) {
      if (value instanceof Float)
        faceCenterOffset = ((Float)value).floatValue();
      else
        invalidPropertyType(propertyName, value, "Float");
      return ;
    }
    if ("centerAngleMax" == propertyName) {
      if (value instanceof Float)
        centralAngleMax = (((Float)value).floatValue()) / 180f * 3.1415926f;
      else
        invalidPropertyType(propertyName, value, "Float");
      return;
    }
    if ("faceNormalMax" == propertyName) {
      if (value instanceof Float)
        faceNormalMax = (((Float)value).floatValue()) / 180f * 3.1415926f;
      else
        invalidPropertyType(propertyName, value, "Float");
      return;
    }
    if ("generate" == propertyName) {
      if (bsCenters == null)
        bsCenters = bs;
      buildPolyhedra();
      return;
    }
    if ("collapsed" == propertyName) {
      if (bsCenters == null)
        bsCenters = bs;
      isCollapsed = value == Boolean.TRUE;
      setCollapsed(isCollapsed, bsCenters);
      return;
    }
    if ("delete" == propertyName) {
      if (bsCenters == null)
        bsCenters = bs;
      deletePolyhedra(bsCenters);
      return;
    }
    if ("on" == propertyName) {
      if (bsCenters == null)
        bsCenters = bs;
      setVisible(true, bsCenters);
      return;
    }
    if ("off" == propertyName) {
      if (bsCenters == null)
        bsCenters = bs;
      setVisible(false, bsCenters);
      return;
    }
    if ("noedges" == propertyName) {
      drawEdges = EDGES_NONE;
      if (bsCenters == null)
        bsCenters = bs;
      setEdges(drawEdges, bsCenters);
      return;
    }
    if ("edges" == propertyName) {
      drawEdges = EDGES_ALL;
      if (bsCenters == null)
        bsCenters = bs;
      setEdges(drawEdges, bsCenters);
      return;
    }
    if ("frontedges" == propertyName) {
      drawEdges = EDGES_FRONT;
      if (bsCenters == null)
        bsCenters = bs;
      setEdges(drawEdges, bsCenters);
      return;
    }
    if ("color" == propertyName) {
      // remember that this comes from 'color' command, so bsCenters is not set
      colix = Graphics3D.getColix(value);
      setColix(colix,
               (colix != Graphics3D.UNRECOGNIZED) ? null : (String) value,
               bs);
      return;
    }
    if ("translucency" == propertyName) {
      // remember that this comes from 'color' command, so use bs not bsCenters
      setTranslucent("translucent" == value, bs);
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  void deletePolyhedra(BitSet bs) {
    int newCount = 0;
    for (int i = 0; i < polyhedronCount; ++i) {
      Polyhedron p = polyhedrons[i];
      if (! bs.get(p.centralAtom.atomIndex))
        polyhedrons[newCount++] = p;
    }
    for (int i = newCount; i < polyhedronCount; ++i)
      polyhedrons[i] = null;
    polyhedronCount = newCount;
  }

  void setVisible(boolean visible, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.visible = visible;
    }
  }

  void setEdges(int edges, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.edges = edges;
    }
  }

  void setCollapsed(boolean isCollapsed, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.collapsed = isCollapsed;
    }
  }

  void setColix(short colix, String palette, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      int atomIndex = p.centralAtom.atomIndex;
      if (bs.get(atomIndex))
        p.polyhedronColix =
          ((colix != Graphics3D.UNRECOGNIZED)
           ? colix
           : viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette));
    }
  }

  void setTranslucent(boolean isTranslucent, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.polyhedronColix =
          Graphics3D.setTranslucent(p.polyhedronColix, isTranslucent);
    }
  }

  void savePolyhedron(Polyhedron p) {
    // overwrite similar polyhedrons
    for (int i = polyhedronCount; --i >= 0; ) {
      if (p.isSimilarEnoughToDelete(polyhedrons[i])) {
            polyhedrons[i] = p;
            return;
          }
    }
    if (polyhedronCount == polyhedrons.length)
      polyhedrons = (Polyhedron[])Util.doubleLength(polyhedrons);
    polyhedrons[polyhedronCount++] = p;
  }

  void buildPolyhedra() {
    for (int i = frame.atomCount; --i >= 0;) {
      if (bsCenters.get(i)) {
        Polyhedron p = constructPolyhedron(i);
        if (p != null)
          savePolyhedron(p);
      }
    }
  }

  int potentialVertexCount;
  Atom[] potentialVertexAtoms = new Atom[MAXIMUM_ACCEPTABLE_VERTEX_COUNT];

  Polyhedron constructPolyhedron(int atomIndex) {
    Atom atom = frame.getAtomAt(atomIndex);
    if (radius > 0)
      identifyPotentialRadiusVertices(atom);
    else
      identifyPotentialBondsVertices(atom);
    if (acceptableVertexCountCount == 0)
      return validatePolyhedronNew(atom, potentialVertexCount,
                                   potentialVertexAtoms);
    if (potentialVertexCount >= MINIMUM_ACCEPTABLE_VERTEX_COUNT) {
      for (int i = acceptableVertexCountCount; --i >= 0; ) {
        if (potentialVertexCount == acceptableVertexCounts[i]) {
          return validatePolyhedronNew(atom, potentialVertexCount,
                                       potentialVertexAtoms);
        }
      }
    }
    return null;
  }

  void identifyPotentialBondsVertices(Atom atom) {
    potentialVertexCount = 0;
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      Atom otherAtom = bond.atom1 == atom ? bond.atom2 : bond.atom1;
      if (bsVertices != null && !bsVertices.get(otherAtom.atomIndex))
        continue;
      if (potentialVertexCount == potentialVertexAtoms.length)
        break;
      potentialVertexAtoms[potentialVertexCount++] = otherAtom;
    }
  }

  void identifyPotentialRadiusVertices(Atom atom) {
    potentialVertexCount = 0;
    AtomIterator withinIterator = frame.getWithinModelIterator(atom, radius);
    while (withinIterator.hasNext()) {
      Atom otherAtom = withinIterator.next();
      if (otherAtom == atom ||
          bsVertices != null && !bsVertices.get(otherAtom.atomIndex))
        continue;
      if (potentialVertexCount == potentialVertexAtoms.length)
        break;
      potentialVertexAtoms[potentialVertexCount++] = otherAtom;
    }
  }

  private final Vector3f normalT = new Vector3f();
  final short[] normixesT = new short[FACE_COUNT_MAX];
  final byte[] planesT = new byte[3 * FACE_COUNT_MAX];
  final short[] collapsedNormixesT = new short[3 * FACE_COUNT_MAX];
  final Point3f[] collapsedCentersT = new Point3f[FACE_COUNT_MAX];
  final Vector3f[] centerVectors = new Vector3f[3 * FACE_COUNT_MAX];
  private final Vector3f centerSum = new Vector3f();
  {
    for (int i = collapsedCentersT.length; --i >= 0; )
      collapsedCentersT[i] = new Point3f();
  }
  
  Polyhedron validatePolyhedronNew(Atom centralAtom, int vertexCount,
                                   Atom[] otherAtoms) {
    int faceCount = 0;
    Point3f centralAtomPoint = centralAtom.point3f;
    for (int i = vertexCount; --i >= 0;) {
      centerVectors[i] = new Vector3f(otherAtoms[i].point3f);
      centerVectors[i].sub(centralAtomPoint);
    }

    // simply define a face to be when all three central angles 
    // are < centralAngleMax
    // collapsed trick is that introduce a "simple" atom
    // near the center but not quite the center, so that our planes on
    // either side of the facet don't overlap. We step out maxFactor * normal

    // also needed: consideration for faces involving more than three atoms
    out: for (int i = 0; i < vertexCount - 2; i++) {
      for (int j = i + 1; j < vertexCount - 1; j++) {
        if (centerVectors[i].angle(centerVectors[j]) > centralAngleMax)
          continue;
        for (int k = j + 1; k < vertexCount; k++) {
          if (centerVectors[i].angle(centerVectors[k]) > centralAngleMax
              || centerVectors[j].angle(centerVectors[k]) > centralAngleMax)
            continue;
          Point3f pointI = otherAtoms[i].point3f;
          Point3f pointJ = otherAtoms[j].point3f;
          Point3f pointK = otherAtoms[k].point3f;
          getNormalFromCenter(centralAtomPoint, pointI, pointJ, pointK, false,
              normalT);

          centerSum.add(centerVectors[i],centerVectors[j]);
          centerSum.add(centerVectors[k]);
          if (debugging) {
              System.out.println("excluding? " + otherAtoms[i].getInfo() + otherAtoms[j].getInfo() + otherAtoms[k].getInfo() );
              System.out.println("excluding? " + normalT + "\n" + centerSum + "\n" + centerSum.angle(normalT)/3.1415926f*180f);
          }
          if (centerSum.angle(normalT) > faceNormalMax) {
            if (debugging)
              System.out.println("yes");
            continue;
          }
          System.out.println("no -- passes");

          planesT[3 * faceCount + 0] = (byte) i;
          planesT[3 * faceCount + 1] = (byte) j;
          planesT[3 * faceCount + 2] = (byte) k;
          normixesT[faceCount] = g3d.getNormix(normalT);

          // calculate collapsed faces too
          Point3f collapsedCenter = collapsedCentersT[faceCount];
          collapsedCenter.scaleAdd(faceCenterOffset, normalT, centralAtomPoint);
          getNormalFromCenter(pointI, collapsedCenter, pointJ, pointK, true,
              normalT);
          collapsedNormixesT[3 * faceCount + 0] = g3d.getNormix(normalT);

          getNormalFromCenter(pointJ, pointI, collapsedCenter, pointK, true,
              normalT);
          collapsedNormixesT[3 * faceCount + 1] = g3d.getNormix(normalT);

          getNormalFromCenter(pointK, pointI, pointJ, collapsedCenter, true,
              normalT);
          collapsedNormixesT[3 * faceCount + 2] = g3d.getNormix(normalT);

          if (++faceCount == FACE_COUNT_MAX)
            break out;
        }
      }
    }
    if (faceCount < 1)
      return null;
    return new Polyhedron(centralAtom, vertexCount, otherAtoms, faceCount,
        normixesT, planesT, collapsedCentersT, collapsedNormixesT);
  }
  
  private final Point3f ptT = new Point3f();
  private final Point3f ptT2 = new Point3f();
  // note: this shared vector3f is returned and used by callers

  void getNormalFromCenter(Point3f ptCenter, Point3f ptA, Point3f ptB,
                            Point3f ptC, boolean isCollapsed, Vector3f normal) {
    //but which way is it? add N to A and see who is closer to Center, A or N. 
    g3d.calcNormalizedNormal(ptA, ptB, ptC, normal); //still need normal
    ptT.add(ptA, ptB);
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
    if (!isCollapsed && ptCenter.distance(ptT2) < ptCenter.distance(ptT)
        || isCollapsed && ptCenter.distance(ptT) < ptCenter.distance(ptT2))
      normal.scale(-1f);
  }

  class Polyhedron {
    final Atom centralAtom;
    final int vertexCount;
    final Atom[] vertexAtoms;
    final int faceCount;
    final short[] normixes;
    final byte[] planes;
    final Point3f[] collapsedCenters;
    final short[] collapsedNormixes;
    boolean visible;
    short polyhedronColix;
    boolean collapsed;
    int edges;

    Polyhedron(Atom centralAtom, int vertexCount, Atom[] vertexAtoms,
               int faceCount,  short[] normixes, byte[] planes,
               Point3f[] collapsedCenters, short[] collapsedNormixes) {
      System.out.println("new Polyhedron vertexCount = " + vertexCount + ";"
          + " faceCount = " + faceCount);
      this.centralAtom = centralAtom;
      this.vertexCount = vertexCount;
      this.vertexAtoms = new Atom[vertexCount];
      for (int i = vertexCount; --i >= 0; )
        this.vertexAtoms[i] = vertexAtoms[i];

      this.faceCount = faceCount;
      this.normixes = new short[faceCount];
      this.collapsedCenters = new Point3f[faceCount];
      for (int i = faceCount; --i >= 0; ) {
        this.normixes[i] = normixes[i];
        this.collapsedCenters[i] = new Point3f(collapsedCenters[i]);
      }

      this.planes = new byte[faceCount * 3];
      this.collapsedNormixes = new short[faceCount * 3];
      for (int i = faceCount * 3; --i >= 0; ) {
        this.planes[i] = planes[i];
        this.collapsedNormixes[i] = collapsedNormixes[i];
      }

      this.visible = true;
      this.polyhedronColix = 0; // always create with default of 'inherit'
      this.collapsed = isCollapsed;
      this.edges = drawEdges;
    }

    boolean isSimilarEnoughToDelete(Polyhedron p) {
      return centralAtom == p.centralAtom && faceCount == p.faceCount;
    }
  }
}
