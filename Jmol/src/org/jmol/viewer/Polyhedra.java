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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Polyhedra extends SelectionIndependentShape {

  int polyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];
  byte defaultAlpha = OPAQUE;
  float radius;
  int drawEdges;

  final static int EDGES_NONE = 0;
  final static int EDGES_ALL = 1;
  final static int EDGES_FRONT = 2;

  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("bonds" == propertyName) {
      deletePolyhedra(bs);
      buildBondsPolyhedra(bs);
      return;
    }
    if ("delete" == propertyName) {
      deletePolyhedra(bs);
      return;
    }
    if ("on" == propertyName) {
      setVisible(true, bs);
      return;
    }
    if ("off" == propertyName) {
      setVisible(false, bs);
      return;
    }
    if ("translucent" == propertyName) {
      defaultAlpha = TRANSLUCENT;
      setAlpha(TRANSLUCENT, bs);
      return;
    }
    if ("opaque" == propertyName) {
      defaultAlpha = OPAQUE;
      setAlpha(OPAQUE, bs);
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
      colix = g3d.getColix(value);
      //      System.out.println("color polyhedra:" + colix);
      setColix(colix, bs);
      return;
    }
    if ("colorScheme" == propertyName) {
      if ("cpk" == value)
        setColix((short)0, bs);
      return;
    }
    if ("radius" == propertyName) {
      radius = ((Float)value).floatValue();
      //      System.out.println("Polyhedra radius=" + radius);
      return;
    }
    if ("expression" == propertyName) {
      //      System.out.println("polyhedra expression");
      BitSet bsVertices = (BitSet)value;
      deletePolyhedra(bs);
      buildRadiusPolyhedra(bs, radius, bsVertices);
      return;
    }
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

  void setAlpha(byte alpha, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.alpha = alpha;
    }
  }

  void setColix(short colix, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.polyhedronColix = colix;
    }
  }

  void savePolyhedron(Polyhedron p) {
    if (polyhedronCount == polyhedrons.length)
      polyhedrons = (Polyhedron[])Util.doubleLength(polyhedrons);
    polyhedrons[polyhedronCount++] = p;
  }

  void buildBondsPolyhedra(BitSet bs) {
    for (int i = frame.atomCount; --i >= 0; ) {
      if (bs.get(i)) {
        Polyhedron p = constructBondsPolyhedron(i);
        if (p != null)
          savePolyhedron(p);
      }
    }
  }

  Atom[] otherAtoms = new Atom[6];

  final static boolean CHECK_ELEMENT = false;

  Polyhedron constructBondsPolyhedron(int atomIndex) {
    Atom atom = frame.getAtomAt(atomIndex);
    Bond[] bonds = atom.bonds;
    byte bondedElementNumber = -1;
    if (bonds == null)
      return null;
    int bondCount = bonds.length;
    if (bondCount == 4 || bondCount == 6) {
      for (int i = bondCount; --i >= 0; ) {
        Bond bond = bonds[i];
        Atom otherAtom = bond.atom1 == atom ? bond.atom2 : bond.atom1;
        if (CHECK_ELEMENT) {
          if (bondedElementNumber == -1)
            bondedElementNumber = otherAtom.elementNumber;
          else if (bondedElementNumber != otherAtom.elementNumber)
            return null;
        }
        otherAtoms[i] = otherAtom;
      }
      return validatePolyhedron(atom, bondCount, otherAtoms);
    }
    return null;
  }

  void buildRadiusPolyhedra(BitSet bs, float radius, BitSet bsVertices) {
    for (int i = frame.atomCount; --i >= 0; ) {
      if (bs.get(i)) {
        Polyhedron p = constructRadiusPolyhedron(i, radius, bsVertices);
        if (p != null)
          savePolyhedron(p);
      }
    }
  }

  Polyhedron constructRadiusPolyhedron(int atomIndex, float radius,
                                         BitSet bsVertices) {
    Atom atom = frame.getAtomAt(atomIndex);
    int otherAtomCount = 0;
    AtomIterator withinIterator = frame.getWithinModelIterator(atom, radius);
    while (withinIterator.hasNext()) {
      Atom other = withinIterator.next();
      if (other == atom)
        continue;
      if (bsVertices != null && !bsVertices.get(other.atomIndex))
        continue;
      if (otherAtomCount < 6)
        otherAtoms[otherAtomCount++] = other;
      else {
        ++otherAtomCount;
        break;
      }
    }
    Polyhedron p = validatePolyhedron(atom, otherAtomCount, otherAtoms);
    for (int i = 6; --i >= 0; )
      otherAtoms[i] = null;
    return p;
  }

  final Vector3f vectorAB = new Vector3f();
  final Vector3f vectorAC = new Vector3f();
  final Vector3f vectorAX = new Vector3f();
  final Vector3f faceNormalABC = new Vector3f();
  //                                         0      1      2      3
  final static byte[] tetrahedronFaces = { 0,1,2, 0,2,3, 0,3,1, 1,3,2 };
  final static byte[] octahedronFaces = {
    0,1,2, 0,2,3, 0,3,4, 0,4,1, 5,2,1, 5,3,2, 5,4,3, 5,1,4 };
  //  0      1      2      3      4      5      6      7

  final static short[] polyhedronNormixes = new short[8];

  Polyhedron validatePolyhedron(Atom centralAtom, int vertexCount,
                                Atom[] otherAtoms) {
    byte[] faces;
    int faceCount;
    if (vertexCount == 6) {
      arrangeOctahedronVertices(centralAtom, otherAtoms);
      faces = octahedronFaces;
      faceCount = 8;
    } else if (vertexCount == 4) {
      arrangeTetrahedronVertices(centralAtom, otherAtoms);
      faces = tetrahedronFaces;
      faceCount = 4;
    } else
      return null;

    Point3f pointCentral = centralAtom.point3f;

    for (int i = 0, j = 0; i < faceCount; ++i) {
      int indexA = faces[j++];
      Point3f pointA = otherAtoms[indexA].point3f;
      int indexB = faces[j++];
      Point3f pointB = otherAtoms[indexB].point3f;
      int indexC = faces[j++];
      Point3f pointC = otherAtoms[indexC].point3f;
      vectorAB.sub(pointB, pointA);
      vectorAC.sub(pointC, pointA);
      faceNormalABC.cross(vectorAB, vectorAC);
      vectorAX.sub(pointCentral, pointA);
      if (faceNormalABC.dot(vectorAX) >= 0)
        return null;

      for (int k = vertexCount; --k >= 0; ) {
        if (k == indexA || k == indexB || k == indexC)
          continue;
        vectorAX.sub(otherAtoms[k].point3f, pointA);
        if (faceNormalABC.dot(vectorAX) >= 0)
          return null;
      }

      polyhedronNormixes[i] = g3d.getNormix(faceNormalABC);
    }
    return new Polyhedron(centralAtom, vertexCount, faceCount,
                          otherAtoms, polyhedronNormixes);
  }

  void arrangeTetrahedronVertices(Atom centralAtom, Atom[] otherAtoms) {
    Point3f pointCentral = centralAtom.point3f;
    Point3f point0 = otherAtoms[0].point3f;
    Point3f point1 = otherAtoms[1].point3f;
    vectorAB.sub(point1, point0);
    vectorAC.sub(pointCentral, point0);
    faceNormalABC.cross(vectorAB, vectorAC);
    Atom atomT = otherAtoms[2];
    vectorAX.sub(atomT.point3f, point0);
    if (faceNormalABC.dot(vectorAX) < 0) {
      otherAtoms[2] = otherAtoms[3];
      otherAtoms[3] = atomT;
    }
    /*

    for (int i = 0, j = 0; i < 4; ) {
      int indexA = tetrahedronFaces[j++];
      Point3f pointA = otherAtoms[indexA].point3f;
      int indexB = tetrahedronFaces[j++];
      Point3f pointB = otherAtoms[indexB].point3f;
      int indexC = tetrahedronFaces[j++];
      Point3f pointC = otherAtoms[indexC].point3f;
      vectorAB.sub(pointB, pointA);
      vectorAC.sub(pointC, pointA);
      faceNormalABC.cross(vectorAB, vectorAC);
      vectorAX.sub(pointCentral, pointA);
      if (faceNormalABC.dot(vectorAX) >= 0)
        return null;
      polyhedronNormixes[i] = g3d.getNormix(faceNormalABC);
    }
    return new Polyhedron(centralAtom, 4, 4, otherAtoms, polyhedronNormixes);
    */
  }

  final Atom[] otherAtomsT = new Atom[6];

  void arrangeOctahedronVertices(Atom centralAtom, Atom[] otherAtoms) {
    for (int i = 6; --i >= 0; )
      otherAtomsT[i] = otherAtoms[i];
    Atom atom0 = otherAtoms[0] = getFarthestAtom(centralAtom, otherAtomsT);
    Atom atom5 = otherAtoms[5] = getFarthestAtom(atom0, otherAtomsT);
    Atom atom1 = otherAtoms[1] = getFarthestAtom(centralAtom, otherAtomsT);
    otherAtoms[3] = getFarthestAtom(atom1, otherAtomsT);
    // atoms 0, 5, 1 now form a plane.
    vectorAB.sub(atom5.point3f, atom0.point3f);
    vectorAC.sub(atom1.point3f, atom0.point3f);
    faceNormalABC.cross(vectorAB, vectorAC);
    Atom atomT = getFarthestAtom(centralAtom, otherAtomsT);
    vectorAX.sub(atomT.point3f, atom0.point3f);
    Atom atom2;
    Atom atom4;
    Atom atomT1 = getFarthestAtom(centralAtom, otherAtomsT);
    if (faceNormalABC.dot(vectorAX) > 0) {
      atom4 = atomT;
      atom2 = atomT1;
    } else {
      atom2 = atomT;
      atom4 = atomT1;
    }
    otherAtoms[2] = atom2;
    otherAtoms[4] = atom4;
  }

  Atom getFarthestAtom(Atom atomA, Atom[] atoms) {
    Point3f point3f = atomA.point3f;
    float dist2Max = 0;
    int indexFarthest = -1;
    for (int i = atoms.length; --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom != null) {
        float dist2 = point3f.distance(atom.point3f);
        if (dist2 > dist2Max) {
          dist2Max = dist2;
          indexFarthest = i;
        }
      }
    }
    Atom atomFarthest = atoms[indexFarthest];
    atoms[indexFarthest] = null;
    return atomFarthest;
  }

  final static byte TRANSLUCENT = (byte)0x80;
  final static byte OPAQUE      = (byte)0xFF;

  class Polyhedron {
    final Atom centralAtom;
    final Atom[] vertices;
    boolean visible;
    byte alpha;
    final short[] normixes;
    short polyhedronColix;

    Polyhedron(Atom centralAtom, int vertexCount, int faceCount,
               Atom[] otherAtoms, short[] normixes) {
      this.centralAtom = centralAtom;
      this.vertices = new Atom[vertexCount];
      this.visible = true;
      this.alpha = defaultAlpha;
      this.polyhedronColix = colix;
      this.normixes = new short[faceCount];
      for (int i = vertexCount; --i >= 0; )
        vertices[i] = otherAtoms[i];
      for (int i = faceCount; --i >= 0; )
        this.normixes[i] = normixes[i];
    }
  }
}
