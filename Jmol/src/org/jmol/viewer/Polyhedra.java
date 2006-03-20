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

  int polyhedronCount;
  Polyhedron[] polyhedrons = new Polyhedron[32];
  float radius;
  int drawEdges;
  
  final static int EDGES_NONE = 0;
  final static int EDGES_ALL = 1;
  final static int EDGES_FRONT = 2;
  final static float MAX_FACTOR = 1.85f;
  
  boolean nBondOption = false;
  boolean doFacets = false;
  
  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    
    if (propertyName.equalsIgnoreCase("nbonds")) {
      nBondOption = true;
      deletePolyhedra(bs);
      if(value instanceof Integer)
        buildBondsPolyhedra(bs, ((Integer)value).intValue());
      else
        buildBondsPolyhedra(bs);
      nBondOption = false;
      return;
    }

    if (propertyName.equalsIgnoreCase("facets")) {
      doFacets = true;
      deletePolyhedra(bs);
      if(value instanceof Integer)
        buildBondsPolyhedra(bs, ((Integer)value).intValue());
      else
        buildBondsPolyhedra(bs);
      doFacets = false;
      return;
    }

    if ("bonds" == propertyName) {
      deletePolyhedra(bs);
      if(value instanceof Integer)
        buildBondsPolyhedra(bs, ((Integer)value).intValue());
      else
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
      colix = Graphics3D.getColix(value);
      // System.out.println("color polyhedra:" + colix);
      
      setColix(colix,
               (colix != Graphics3D.UNRECOGNIZED) ? null : (String) value,
               bs);
      return;
    }
    
    if ("translucency" == propertyName) {
      colix = Graphics3D.getColix(value);
      setTranslucent("translucent" == value, bs);
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

  void setColix(short colix, String palette, BitSet bs) {
    for (int i = polyhedronCount; --i >= 0; ) {
      Polyhedron p = polyhedrons[i];
      if (p == null)
        continue;
      int atomIndex = p.centralAtom.atomIndex;
      if (bs.get(atomIndex)) {
        p.polyhedronColix =
          ((colix != Graphics3D.UNRECOGNIZED)
           ? colix
           : viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette));
      }
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
    if (polyhedronCount == polyhedrons.length)
      polyhedrons = (Polyhedron[])Util.doubleLength(polyhedrons);
    polyhedrons[polyhedronCount++] = p;
  }

  void buildBondsPolyhedra(BitSet bs) {
    buildBondsPolyhedra(bs, -1);
  }

  void buildBondsPolyhedra(BitSet bs, int nBonds) {
    for (int i = frame.atomCount; --i >= 0; ) {
      if (bs.get(i)) {
        Polyhedron p = constructBondsPolyhedron(i, nBonds);
        if (p != null)
          savePolyhedron(p);
      }
    }
  }

  Atom[] otherAtoms = new Atom[6];

  final static boolean CHECK_ELEMENT = false;

  Polyhedron constructBondsPolyhedron(int atomIndex, int nBonds) {
    Atom atom = frame.getAtomAt(atomIndex);
    Bond[] bonds = atom.bonds;
    byte bondedElementNumber = -1;
    if (bonds == null)
      return null;
    int bondCount = bonds.length;
    if ((doFacets || nBondOption || bondCount == 4 || bondCount == 6) 
        && (nBonds < 0 || nBonds == bondCount)) {
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
      if (nBondOption) 
        return validatePolyhedronNew(atom, bondCount, otherAtoms);
  
      if (doFacets) 
        return validatePolyhedronFacets(atom, bondCount, otherAtoms);
  
      return validatePolyhedron(atom, bondCount, otherAtoms);
    }
    return null;
  }

/////////////start of nBondOption/////////  

  short[] normixesT = new short[85];
  byte[] facesT = new byte[255];
  final static int FACE_COUNT_MAX = 85;
  Polyhedron validatePolyhedronNew(Atom centralAtom, int vertexCount,
                                   Atom[] otherAtoms) {
    int faceCount = 0;
    int facetCount = 0;
    int nOthers = vertexCount;
    Point3f[] points = new Point3f[vertexCount];
    Vector3f normal = new Vector3f();
    Point3f pointCentral = centralAtom.point3f;
    float distMax = 0;
    float dist = 0;
    for (int i = 0; i < nOthers; i++) {
      points[i] = otherAtoms[i].point3f;
      if ((dist = pointCentral.distance(points[i])) > distMax)
        distMax = dist;
    }
    //simply define a face to be when all three distances are < MAX_FACTOR * longest central
    distMax *= MAX_FACTOR;
    out: for (int i = 0; i < nOthers - 2; i++) {
      for (int j = i + 1; j < nOthers - 1; j++) {
        if (points[i].distance(points[j]) < distMax)
          for (int k = j + 1; k < nOthers; k++)
            if (points[i].distance(points[k]) < distMax
                && points[j].distance(points[k]) < distMax) {
              if (faceCount == FACE_COUNT_MAX)
                break out;
              facesT[facetCount++] = (byte) i;
              facesT[facetCount++] = (byte) j;
              facesT[facetCount++] = (byte) k;
              g3d.calcNormalizedNormal(points[i], points[j], points[k], normal);
              normixesT[faceCount++] = g3d.getNormix(normal);
            }
      }
    }

    return new Polyhedron(centralAtom, vertexCount, faceCount, otherAtoms,
        normixesT, facesT);
  }
  
  Polyhedron validatePolyhedronFacets(Atom centralAtom, int vertexCount,
                                   Atom[] otherAtoms) {
       int faceCount = 0;
       int facetCount = 0;
       int nOthers = vertexCount;
       int ptCenter = nOthers;
       Point3f[] points = new Point3f[nOthers + 1];
       
       Vector3f normal = new Vector3f();
       Point3f pointCentral = centralAtom.point3f;
       float distMax = 0;
       float dist = 0;
       points[ptCenter] = new Point3f(pointCentral);
       for (int i = 0; i < ptCenter; i++) {
         points[i] = otherAtoms[i].point3f;
         if ((dist = pointCentral.distance(points[i])) > distMax)
           distMax = dist;  
       }
       System.out.println("distMax " + distMax);
       //simply define a face to be when all three distances are < MAX_FACTOR * longest central
       distMax *= MAX_FACTOR;
       out:
       for (int i = 0; i < ptCenter - 2; i++)
           for (int j = i + 1; j < ptCenter - 1; j++)
             if (points[i].distance(points[j]) < distMax)
           for (int k = j + 1; k < ptCenter; k++)
             if (points[i].distance(points[k]) < distMax
                  && points[j].distance(points[k]) < distMax) {
               if(faceCount == FACE_COUNT_MAX)
                 break out;
               facesT[facetCount++] = (byte)ptCenter;
               facesT[facetCount++] = (byte)j;
               facesT[facetCount++] = (byte)k;
               g3d.calcNormalizedNormal(points[ptCenter], points[j], points[k], normal);
               normixesT[faceCount++] = g3d.getNormix(normal);

               facesT[facetCount++] = (byte)i;
               facesT[facetCount++] = (byte)ptCenter;
               facesT[facetCount++] = (byte)k;
               g3d.calcNormalizedNormal(points[i], points[ptCenter], points[k], normal);
               normixesT[faceCount++] = g3d.getNormix(normal);

               facesT[facetCount++] = (byte)i;
               facesT[facetCount++] = (byte)j;
               facesT[facetCount++] = (byte)ptCenter;
               g3d.calcNormalizedNormal(points[i], points[j], points[ptCenter], normal);
               normixesT[faceCount++] = g3d.getNormix(normal);
             }
       
       return new Polyhedron(centralAtom, vertexCount, faceCount,
                             otherAtoms, normixesT, facesT);
     }
/////////////end of nBondOption/////////  
  
  
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

  class Polyhedron {
    final Atom centralAtom;
    final Atom[] vertices;
    boolean visible;
    final short[] normixes;
    short polyhedronColix;
    byte[] faces;
    int faceCount;
    boolean iHaveFaces = false;

    Polyhedron(Atom centralAtom, int vertexCount, int faceCount,
               Atom[] otherAtoms, short[] normixes) {
      this.centralAtom = centralAtom;
      this.vertices = new Atom[vertexCount];
      this.visible = true;
      this.polyhedronColix = colix;
      this.normixes = new short[faceCount];
      for (int i = vertexCount; --i >= 0; )
        vertices[i] = otherAtoms[i];
      for (int i = faceCount; --i >= 0; )
        this.normixes[i] = normixes[i];
    }
    
    Polyhedron(Atom centralAtom, int vertexCount, int faceCount,
        Atom[] otherAtoms, short[] normixes, byte[] faces) {
      this.centralAtom = centralAtom;
      boolean iNeedCentral = (faces[0] == vertexCount);
      this.vertices = new Atom[vertexCount + (iNeedCentral? 1 : 0)];
      this.visible = true;
      this.polyhedronColix = colix;
      this.normixes = new short[faceCount];
      this.faceCount = faceCount;
      this.faces = new byte[faceCount * 3];
      for (int i = vertexCount; --i >= 0;)
        vertices[i] = otherAtoms[i];
      if (iNeedCentral)
        vertices[vertexCount] = centralAtom;
      for (int i = faceCount; --i >= 0;)
        this.normixes[i] = normixes[i];
      for (int i = faceCount * 3; --i >= 0;)
        this.faces[i] = faces[i];
      iHaveFaces = true;
    }
   
  }
}
