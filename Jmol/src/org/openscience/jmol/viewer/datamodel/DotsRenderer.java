/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.Graphics3D;
import java.awt.Rectangle;

import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.*;

class DotsRenderer extends ShapeRenderer {

  boolean perspectiveDepth;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;

  Geodesic geodesic;

  final static int[] mapNull = Dots.mapNull;

  void initRenderer() {

    this.geodesic = new Geodesic(); // 12 vertices
    geodesic.quadruple(); // 12 * 4 - 6 = 42 vertices
    geodesic.quadruple(); // 42 * 4 - 6 = 162 vertices
    geodesic.quadruple(); // 162 * 4 - 6 = 642 vertices
    //    geodesic.quadruple(); // 642 * 4 - 6 = 2562 vertices

  }

  void render() {
    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int)viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();


    geodesic.transform();
    Dots dots = (Dots)shape;
    if (dots == null)
      return;
    Atom[] atoms = frame.atoms;
    int[][] dotsConvexMaps = dots.dotsConvexMaps;
    short[] colixesConvex = dots.colixesConvex;
    int displayModelIndex = this.displayModelIndex;
    for (int i = dots.dotsConvexMax; --i >= 0; ) {
      int[] map = dotsConvexMaps[i];
      if (map != null && map != mapNull) {
        Atom atom = atoms[i];
        if (displayModelIndex < 0 || displayModelIndex == atom.modelIndex)
          renderConvex(atom, colixesConvex[i], map);
      }
    }
    Dots.Torus[] tori = dots.tori;
    for (int i = dots.torusCount; --i >= 0; ) {
      Dots.Torus torus = tori[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[torus.indexI].modelIndex)
        renderTorus(torus, atoms, colixesConvex, dotsConvexMaps);
    }
    Dots.Cavity[] cavities = dots.cavities;
    if (false) {
      System.out.println("concave surface rendering currently disabled");
      return;
    }
    for (int i = dots.cavityCount; --i >= 0; ) {
      Dots.Cavity cavity = cavities[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[cavity.ixI].modelIndex)
        renderCavity(cavities[i], atoms, colixesConvex, dotsConvexMaps);
    }
  }

  void renderConvex(Atom atom, short colix, int[] visibilityMap) {
    geodesic.calcScreenPoints(visibilityMap,
                              atom.getVanderwaalsRadiusFloat(),
                              atom.getScreenX(), atom.getScreenY(),
                              atom.getScreenZ());
    if (geodesic.screenCoordinateCount > 0)
      g3d.plotPoints(colix == 0 ? atom.colixAtom : colix,
                     geodesic.screenCoordinateCount,
                     geodesic.screenCoordinates);
  }

  Point3f pointT = new Point3f();
  Point3f pointT1 = new Point3f();
  Matrix3f matrixT = new Matrix3f();
  Matrix3f matrixT1 = new Matrix3f();
  Matrix3f matrixRot = new Matrix3f();
  AxisAngle4f aaT = new AxisAngle4f();
  AxisAngle4f aaT1 = new AxisAngle4f();

  static final float torusStepAngle = 2 * (float)Math.PI / 64;

  void renderTorus(Dots.Torus torus,
                   Atom[] atoms, short[] colixes, int[][] dotsConvexMaps) {
    if (dotsConvexMaps[torus.indexI] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixI, colixes, atoms, torus.indexI),
                      false);
    if (dotsConvexMaps[torus.indexJ] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixJ, colixes, atoms, torus.indexJ),
                      true);
  }

  short getColix(short colix, short[] colixes, Atom[] atoms, int index) {
    if (colix != 0)
      return colix;
    if (colixes[index] != 0)
      return colixes[index];
    return atoms[index].colixAtom;
  }

  void renderTorusHalf(Dots.Torus torus, short colix, boolean renderJHalf) {
    g3d.setColix(colix);
    long probeMap = torus.probeMap;

    int torusDotCount1 =
      (int)(getTorusOuterDotCount() * torus.outerAngle / (2 * Math.PI));
    float stepAngle1 = torus.outerAngle / torusDotCount1;
    if (renderJHalf)
      stepAngle1 = -stepAngle1;
    aaT1.set(torus.tangentVector, 0);

    aaT.set(torus.axisVector, 0);
    int step = getTorusIncrement();
    for (int i = 0; probeMap != 0; i += step, probeMap <<= step) {
      if (probeMap >= 0)
        continue;
      aaT.angle = i * torusStepAngle;
      matrixT.set(aaT);
      matrixT.transform(torus.radialVector, pointT);
      pointT.add(torus.center);

      for (int j = torusDotCount1; --j >= 0; ) {
        aaT1.angle = j * stepAngle1;
        matrixT1.set(aaT1);
        matrixT1.transform(torus.outerRadial, pointT1);
        matrixT.transform(pointT1);
        pointT1.add(pointT);
        g3d.drawPixel(viewer.transformPoint(pointT1));
      }
    }
  }

  int getTorusIncrement() {
    if (scalePixelsPerAngstrom <= 5)
      return 16;
    if (scalePixelsPerAngstrom <= 10)
      return 8;
    if (scalePixelsPerAngstrom <= 20)
      return 4;
    if (scalePixelsPerAngstrom <= 40)
      return 2;
    return 1;
  }

  int getTorusOuterDotCount() {
    int dotCount = 8;
    if (scalePixelsPerAngstrom > 5) {
      dotCount = 16;
      if (scalePixelsPerAngstrom > 10) {
        dotCount = 32;
        if (scalePixelsPerAngstrom > 20) {
          dotCount = 64;
        }
      }
    }
    return dotCount;
  }

  /**
   * So, I need some help with this.
   * I cannot think of a good way to render this cavity.
   * The shapes are spherical triangle, but are very irregular.
   * In the center of aromatic rings there are 2-4 ... which looks ugly
   * So, if you have an idea how to render this, please let me know.
   */

  final static byte nearI = (byte)(1 << 0);
  final static byte nearJ = (byte)(1 << 1);
  final static byte nearK = (byte)(1 << 2);

  final static byte[] nearAssociations = {
    nearI | nearJ | nearK,

    nearI, nearJ, nearK,
    nearI | nearJ, nearJ | nearK, nearK | nearI,
    nearI, nearJ, nearJ, nearK, nearK, nearI,
    // index 13 starts here
    nearI, nearJ, nearK,
    nearI | nearJ, nearJ | nearK, nearK | nearI,
    nearI, nearJ, nearJ, nearK, nearK, nearI,
  };

  void renderCavity(Dots.Cavity cavity,
                    Atom[] atoms, short[] colixes, int[][] dotsConvexMaps) {
    Point3f[] points = cavity.points;
    if (dotsConvexMaps[cavity.ixI] != null) {
      g3d.setColix(getColix(cavity.colixI, colixes, atoms, cavity.ixI));
      renderCavityThird(points, 0);
    }
    if (dotsConvexMaps[cavity.ixJ] != null) {
      g3d.setColix(getColix(cavity.colixJ, colixes, atoms, cavity.ixJ));
      renderCavityThird(points, 1);
    }
    if (dotsConvexMaps[cavity.ixK] != null) {
      g3d.setColix(getColix(cavity.colixK, colixes, atoms, cavity.ixK));
      renderCavityThird(points, 2);
    }
  }

  void renderCavityThird(Point3f[] points, int which) {
    Point3i screen;
    for (int i = points.length; --i >= 0; ) {
      if ((nearAssociations[i] & (1 << which)) != 0) {
        screen = viewer.transformPoint(points[i]);
        g3d.drawPixel(screen);
      }
    }
  }

  final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  final static float oneFifth = 2 * (float)Math.PI / 5;
  final static float oneTenth = oneFifth / 2;
  
  final static short[] faceIndicesInitial = {
    0, 1, 2,
    0, 2, 3,
    0, 3, 4,
    0, 4, 5,
    0, 5, 1,

    1, 6, 2,
    2, 7, 3,
    3, 8, 4,
    4, 9, 5,
    5, 10, 1,


    6, 1, 10,
    7, 2, 6,
    8, 3, 7,
    9, 4, 8,
    10, 5, 9,

    11, 6, 10,
    11, 7, 6,
    11, 8, 7,
    11, 9, 8,
    11, 10, 9,
  };

  /****************************************************************
   * This code constructs a geodesic sphere which is used to
   * represent the vanderWaals and Connolly dot surfaces
   * One geodesic sphere is constructed. It is a unit sphere
   * with radius of 1.0 <p>
   * Many times a sphere is constructed with lines of latitude and
   * longitude. With this type of rendering, the atom has north and
   * south poles. And the faces are not regularly shaped ... at the
   * poles they are triangles but elsewhere they are quadrilaterals. <p>
   * I think that a geodesic sphere is more appropriate for this type
   * of application. The geodesic sphere does not have poles and 
   * looks the same in all orientations ... as a sphere should. All
   * faces are equilateral triangles. <p>
   * The geodesic sphere is constructed by starting with an icosohedron, 
   * a platonic solid with 12 vertices and 20 equilateral triangles
   * for faces. The call to the method <code>quadruple</code> will
   * split each triangular face into 4 faces by creating a new vertex
   * at the midpoint of each edge. These midpoints are still in the
   * plane, so they are then 'pushed out' to the surface of the
   * enclosing sphere by normalizing their length back to 1.0<p>
   * Individual atoms construct bitmaps to determine which dots are
   * visible and which are obscured. Each bit corresponds to a single
   * dot.<p>
   * The sequence of vertex counts is 12, 42, 162, 642. The vertices
   * are stored so that when atoms are small they can choose to display
   * only the first n bits where n is one of the above vertex counts.<p>
   * The vertices of the 'one true sphere' are rotated to the current
   * molecular rotation at the beginning of the repaint cycle. That way,
   * individual atoms only need to scale the unit vector to the vdw
   * radius for that atom. <p>
   * (If necessary, this on-the-fly scaling could be eliminated by
   * storing multiple geodesic spheres ... one per vdw radius. But
   * I suspect there are bigger performance problems with the saddle
   * and convex connolly surfaces.)<p>
   * I experimented with rendering the dots with light shading. However
   * I found that it was much harder to look at. The dots in the front
   * are lighter, but on a white background they are harder to see. The
   * end result is that I tended to focus on the back side of the sphere
   * of dots ... which made rotations very strange. So I turned off
   * shading of dot surfaces.
   ****************************************************************/


  class Geodesic {

    Vector3f[] vertices;
    Vector3f[] verticesTransformed;
    //    byte[] intensitiesTransformed;
    int screenCoordinateCount;
    int[] screenCoordinates;
    //    byte[] intensities;
    short[] faceIndices;

    Geodesic() {
      vertices = new Vector3f[12];
      vertices[0] = new Vector3f(0, 0, halfRoot5);
      for (int i = 0; i < 5; ++i) {
        vertices[i+1] = new Vector3f((float)Math.cos(i * oneFifth),
                                     (float)Math.sin(i * oneFifth),
                                     0.5f);
        vertices[i+6] = new Vector3f((float)Math.cos(i * oneFifth + oneTenth),
                                     (float)Math.sin(i * oneFifth + oneTenth),
                                     -0.5f);
      }
      vertices[11] = new Vector3f(0, 0, -halfRoot5);
      for (int i = 12; --i >= 0; )
        vertices[i].normalize();
      faceIndices = faceIndicesInitial;
      verticesTransformed = new Vector3f[12];
      for (int i = 12; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
      screenCoordinates = new int[3 * 12];
      //      intensities = new byte[12];
      //      intensitiesTransformed = new byte[12];
    }

    void transform() {
      for (int i = vertices.length; --i >= 0; ) {
        Vector3f t = verticesTransformed[i];
        viewer.transformVector(vertices[i], t);
        //        intensitiesTransformed[i] =
        //          Shade3D.calcIntensity((float)t.x, (float)t.y, (float)t.z);
      }
    }

    void calcScreenPoints(int[] visibilityMap, float radius,
			  int x, int y, int z) {
      int dotCount = 12;
      if (scalePixelsPerAngstrom > 5) {
        dotCount = 42;
        if (scalePixelsPerAngstrom > 10) {
          dotCount = 162;
          if (scalePixelsPerAngstrom > 20) {
            dotCount = 642;
            //		  if (scalePixelsPerAngstrom > 32)
            //		      dotCount = 2562;
          }
        }
      }

      float scaledRadius = viewer.scaleToPerspective(z, radius);
      int icoordinates = 0;
      //      int iintensities = 0;
      int iDot = visibilityMap.length << 5;
      screenCoordinateCount = 0;
      if (iDot > dotCount)
        iDot = dotCount;
      while (--iDot >= 0) {
        if (! getBit(visibilityMap, iDot))
          continue;
        //        intensities[iintensities++] = intensitiesTransformed[iDot];
        Vector3f vertex = verticesTransformed[iDot];
        screenCoordinates[icoordinates++] = x
          + (int)((scaledRadius*vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = y
          + (int)((scaledRadius*vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = z
          + (int)((scaledRadius*vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
        ++screenCoordinateCount;
      }
    }

    short iVertexNew;
    Hashtable htVertex;
    
    void quadruple() {
      htVertex = new Hashtable();
      int nVerticesOld = vertices.length;
      short[] faceIndicesOld = faceIndices;
      int nFaceIndicesOld = faceIndicesOld.length;
      int nEdgesOld = nVerticesOld + nFaceIndicesOld/3 - 2;
      int nVerticesNew = nVerticesOld + nEdgesOld;
      Vector3f[] verticesNew = new Vector3f[nVerticesNew];
      System.arraycopy(vertices, 0, verticesNew, 0, nVerticesOld);
      vertices = verticesNew;
      verticesTransformed = new Vector3f[nVerticesNew];
      for (int i = nVerticesNew; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
      screenCoordinates = new int[3 * nVerticesNew];
      //      intensitiesTransformed = new byte[nVerticesNew];
      //      intensities

      short[] faceIndicesNew = new short[4 * nFaceIndicesOld];
      faceIndices = faceIndicesNew;
      iVertexNew = (short)nVerticesOld;
      
      int iFaceNew = 0;
      for (int i = 0; i < nFaceIndicesOld; ) {
        short iA = faceIndicesOld[i++];
        short iB = faceIndicesOld[i++];
        short iC = faceIndicesOld[i++];
        short iAB = getVertex(iA, iB);
        short iBC = getVertex(iB, iC);
        short iCA = getVertex(iC, iA);
        
        faceIndicesNew[iFaceNew++] = iA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iCA;

        faceIndicesNew[iFaceNew++] = iB;
        faceIndicesNew[iFaceNew++] = iBC;
        faceIndicesNew[iFaceNew++] = iAB;

        faceIndicesNew[iFaceNew++] = iC;
        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iBC;

        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iBC;
      }
      if (iFaceNew != faceIndicesNew.length) {
        System.out.println("que?");
        throw new NullPointerException();
      }
      if (iVertexNew != nVerticesNew) {
        System.out.println("huh? " + " iVertexNew=" + iVertexNew +
                           "nVerticesNew=" + nVerticesNew);
        throw new NullPointerException();
      }
      htVertex = null;
      //      bitmap = allocateBitmap(nVerticesNew);
    }
    
    private short getVertex(short i1, short i2) {
      if (i1 > i2) {
        short t = i1;
        i1 = i2;
        i2 = t;
      }
      Integer hashKey = new Integer((i1 << 16) + i2);
      Short iv = (Short)htVertex.get(hashKey);
      if (iv != null)
        return iv.shortValue();
      Vector3f vertexNew = new Vector3f(vertices[i1]);
      vertexNew.add(vertices[i2]);
      vertexNew.scale(0.5f);
      vertexNew.normalize();
      htVertex.put(hashKey, new Short(iVertexNew));
      vertices[iVertexNew] = vertexNew;
      return iVertexNew++;
    }
  }

  private final static int[] allocateBitmap(int count) {
    return new int[(count + 31) >> 5];
  }

  private final static void setBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] |= 1 << (~i & 31);
  }

  private final static void clearBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] &= ~(1 << (~i & 31));
  }

  private final static boolean getBit(int[] bitmap, int i) {
    return (bitmap[(i >> 5)] << (i & 31)) < 0;
  }

  private final static void setAllBits(int[] bitmap, int count) {
    int i = count >> 5;
    if ((count & 31) != 0)
      bitmap[i] = 0x80000000 >> (count - 1);
    while (--i >= 0)
      bitmap[i] = -1;
  }
  
  private final static void clearBitmap(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }
    
}

