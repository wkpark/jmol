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

package org.openscience.jmol.viewer.g3d;

import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3i;

public class Dots3D {

  JmolViewer viewer;
  Graphics3D g3d;
  public Dots3D(JmolViewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.icosohedron = new Icosohedron();
    icosohedron.quadruple();
    icosohedron.quadruple();
    icosohedron.quadruple();
  }

  final static int numLatitude = 16;
  final static int numLongitude = 16;

  Icosohedron icosohedron;

  float[] afDots;

  void init() {
    afDots = new float[(numLatitude + 1) * (numLongitude + 1 + 1)];
    int i = 0;
    for (double phi = 0.0; phi <= Math.PI; phi += Math.PI/numLatitude) {
      afDots[i++] = (float) (Math.cos(phi - Math.PI/2));
      System.out.println("phi=" + afDots[i - 1]);
      double r = Math.sin(phi);
      for (double theta = 0.0; theta <= Math.PI; theta+=Math.PI/numLongitude) {
        afDots[i++] = (float) (Math.cos(theta) * r / 2 + 0.5);
      }
    }
  }

  void transform() {
    icosohedron.transform();
  }

  void render0(short colix, int diameter, int x, int y, int z) {
    int argb = Colix.getArgb(colix);
    double radius = diameter / 2.0;
    for (double phi = 0.0; phi <= Math.PI/2; phi += Math.PI/12) {
      double r = radius * Math.cos(phi);
      double yD = radius * Math.sin(phi);
      int yT = (int)(yD + (yD >= 0 ? 0.5 : -0.5));
      for (double theta = 0.0; theta <= Math.PI/2; theta+=Math.PI/12) {
        double xD = r * Math.sin(theta);
        int xT = (int)(xD + (xD >= 0 ? 0.5 : -0.5));
        int h = (int)(r * Math.cos(theta));
        g3d.plotPixelClipped(argb, x + xT, y + yT, z - h);
        g3d.plotPixelClipped(argb, x + xT, y - yT, z - h);
        g3d.plotPixelClipped(argb, x - xT, y + yT, z - h);
        g3d.plotPixelClipped(argb, x - xT, y - yT, z - h);
      }
    }
  }

  void render(short colix, int diameter, int x, int y, int z) {
    double radius = diameter / 2.0;
    icosohedron.calcScreenPoints(radius, x, y, z);
    short[] faceIndices = icosohedron.faceIndices;
    g3d.plotPoints(colix,
                   icosohedron.vertices.length,
                   icosohedron.screenCoordinates);
      
    /*
    for (int i = faceIndices.length; i > 0; ) {
      Point3i point1 = screenPoints[faceIndices[--i]];
      Point3i point2 = screenPoints[faceIndices[--i]];
      Point3i point3 = screenPoints[faceIndices[--i]];

      //      if ((i / 3) < 10)
      //        continue;

      g3d.drawLine(colix, point1.x, point1.y, point1.z,
                   point2.x, point2.y, point2.z);
      g3d.drawLine(colix, point2.x, point2.y, point2.z,
                   point3.x, point3.y, point3.z);
      g3d.drawLine(colix, point1.x, point1.y, point1.z,
                   point3.x, point3.y, point3.z);
    }
    */
  }

  final static double halfRoot5 = 0.5 * Math.sqrt(5);
  final static double oneFifth = 2 * Math.PI / 5;
  final static double oneTenth = oneFifth / 2;
  
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

  class Icosohedron {

    Vector3d[] vertices;
    Vector3d[] verticesTransformed;
    int[] screenCoordinates;
    short[] faceIndices;

    Icosohedron() {
      vertices = new Vector3d[12];
      vertices[0] = new Vector3d(0, 0, halfRoot5);
      for (int i = 0; i < 5; ++i) {
        vertices[i+1] = new Vector3d(Math.cos(i * oneFifth),
                                 Math.sin(i * oneFifth),
                                 0.5);
        vertices[i+6] = new Vector3d(Math.cos(i * oneFifth + oneTenth),
                                 Math.sin(i * oneFifth + oneTenth),
                                 -0.5);
      }
      vertices[11] = new Vector3d(0, 0, -halfRoot5);
      for (int i = 12; --i >= 0; )
        vertices[i].normalize();
      faceIndices = faceIndicesInitial;
      verticesTransformed = new Vector3d[12];
      for (int i = 12; --i >= 0; )
        verticesTransformed[i] = new Vector3d();
      screenCoordinates = new int[3 * 12];
    }

    void transform() {
      for (int i = vertices.length; --i >= 0; )
        viewer.transformVector(vertices[i], verticesTransformed[i]);
    }

    void calcScreenPoints(double radius, int x, int y, int z) {
      radius = 1;
      for (int i = vertices.length, icoordinates = screenCoordinates.length;
           --i >= 0; ) {
        Vector3d point = verticesTransformed[i];
        screenCoordinates[--icoordinates] = z + (int)(radius * point.z);
        screenCoordinates[--icoordinates] = y + (int)(radius * point.y);
        screenCoordinates[--icoordinates] = x + (int)(radius * point.x);
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
      Vector3d[] verticesNew = new Vector3d[nVerticesNew];
      System.arraycopy(vertices, 0, verticesNew, 0, nVerticesOld);
      vertices = verticesNew;
      verticesTransformed = new Vector3d[nVerticesNew];
      for (int i = nVerticesNew; --i >= 0; )
        verticesTransformed[i] = new Vector3d();
      screenCoordinates = new int[3 * nVerticesNew];

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
      Vector3d vertexNew = new Vector3d(vertices[i1]);
      vertexNew.add(vertices[i2]);
      vertexNew.scale(0.5);
      vertexNew.normalize();
      htVertex.put(hashKey, new Short(iVertexNew));
      vertices[iVertexNew] = vertexNew;
      return iVertexNew++;
    }
  }
}

