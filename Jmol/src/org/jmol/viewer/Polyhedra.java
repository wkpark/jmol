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

class Polyhedra extends SelectionIndependentShape {

  int polyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];
  byte defaultAlpha = OPAQUE;
  float radius;

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
    if ("color" == propertyName) {
      colix = g3d.getColix(value);
      //      System.out.println("color polyhedra:" + colix);
      setColix(colix, bs);
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
      Polyhedron p = new Polyhedron(atom, bondCount, otherAtoms);
      return p;
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
    AtomIterator withinIterator = frame.getWithinIterator(atom, radius);
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
    Polyhedron p = null;
    if (otherAtomCount == 4 || otherAtomCount == 6)
      p = new Polyhedron(atom, otherAtomCount, otherAtoms);
    for (int i = 6; --i >= 0; )
      otherAtoms[i] = null;
    return p;
  }

  final static byte TRANSLUCENT = (byte)0x80;
  final static byte OPAQUE      = (byte)0xFF;

  class Polyhedron {
    final Atom centralAtom;
    final Atom[] vertices;
    boolean visible;
    byte alpha;
    short polyhedronColix;

    Polyhedron(Atom centralAtom, int vertexCount, Atom[] otherAtoms) {
      this.centralAtom = centralAtom;
      this.vertices = new Atom[vertexCount];
      this.visible = true;
      this.alpha = defaultAlpha;
      this.polyhedronColix = colix;
      if (vertexCount == 6)
        copyOctahedronVertices(otherAtoms);
      else {
        for (int i = vertexCount; --i >= 0; )
          this.vertices[i] = otherAtoms[i];
      }
    }

    void copyOctahedronVertices(Atom[] otherAtoms) {
      int i;
      i = getFarthestAtomIndex(centralAtom.point3f, otherAtoms);
      vertices[0] = otherAtoms[i];
      otherAtoms[i] = null;
      foo(0, 5, otherAtoms);
      foo(0, 1, otherAtoms);
      foo(1, 3, otherAtoms);
      foo(1, 2, otherAtoms);
      foo(1, 4, otherAtoms);
    }
  
    void foo(int i, int j, Atom[] atoms) {
      int k = getFarthestAtomIndex(vertices[i].point3f, atoms);
      vertices[j] = atoms[k];
      atoms[k] = null;
    }
    
    int getFarthestAtomIndex(Point3f point3f, Atom[] atoms) {
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
    return indexFarthest;
    }
  }
}
