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
import org.jmol.g3d.*;
import java.util.BitSet;
import javax.vecmath.Point3f;

class Polyhedra extends SelectionIndependentShape {

  int maxPolyhedron;
  int currentPolyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];

  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bs) {

    if ("on" == propertyName) {
      buildPolyhedra(bs);
      return;
    }
    if ("off" == propertyName) {
      deletePolyhedra(bs);
      return;
    }
    if ("clear" == propertyName) {
      return;
    }
    if ("transparent" == propertyName) {
      setTransparent(true, bs);
      return;
    }
    if ("solid" == propertyName) {
      setTransparent(false, bs);
      return;
    }
    if ("color" == propertyName) {
      colix = g3d.getColix(value);
      return;
    }
  }

  void buildPolyhedra(BitSet bs) {
    for (int i = frame.atomCount; --i >= 0; )
      if (bs.get(i))
        build1(i);
  }

  void build1(int atomIndex) {
    deletePolyhedron(atomIndex);
    Polyhedron p = constructPolyhedron(atomIndex);
    if (p != null)
      savePolyhedron(p);
  }

  void deletePolyhedra(BitSet bs) {
    for (int i = frame.atomCount; --i >= 0; )
      if (bs.get(i))
        deletePolyhedron(i);
  }

  void deletePolyhedron(int atomIndex) {
    for (int i = maxPolyhedron; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p != null && p.centralAtom.atomIndex == atomIndex) {
        --currentPolyhedronCount;
        polyhedrons[i] = null;
        return;
      }
    }
  }

  void setTransparent(boolean transparent, BitSet bs) {
    for (int i = maxPolyhedron; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      if (bs.get(p.centralAtom.atomIndex))
        p.transparent = transparent;
    }
  }

  void savePolyhedron(Polyhedron p) {
    if (currentPolyhedronCount < maxPolyhedron) {
      for (int i = maxPolyhedron; --i >= 0; )
        if (polyhedrons[i] == null) {
          polyhedrons[i] = p;
          break;
        }
    } else {
      if (maxPolyhedron == polyhedrons.length)
        polyhedrons = (Polyhedron[])Util.doubleLength(polyhedrons);
      polyhedrons[maxPolyhedron++] = p;
      ++currentPolyhedronCount;
    }
  }

  Atom[] otherAtoms = new Atom[6];

  Polyhedron constructPolyhedron(int atomIndex) {
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
        if (bondedElementNumber == -1)
          bondedElementNumber = otherAtom.elementNumber;
        else if (bondedElementNumber != otherAtom.elementNumber)
          return null;
        otherAtoms[i] = otherAtom;
      }
      Polyhedron p = new Polyhedron(atom, bondCount, otherAtoms);
      return p;
    } else {
      return null;
    }
  }

  class Polyhedron {
    final Atom centralAtom;
    final Atom[] vertices;
    short colix;
    boolean transparent;

    Polyhedron(Atom centralAtom, int vertexCount, Atom[] otherAtoms) {
      this.centralAtom = centralAtom;
      this.vertices = new Atom[vertexCount];
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
