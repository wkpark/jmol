/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 09:53:35 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7491 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.shape;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import org.jmol.g3d.Graphics3D;

public abstract class MeshRenderer extends ShapeRenderer {

  protected Point3f[] vertices;
  protected short[] normixes;
  protected Point3i[] screens;
  protected Vector3f[] transformedVectors;
  protected int vertexCount;
  protected int lighting;
  protected boolean frontOnly;
  protected short colix;
  
  protected Mesh mesh;

  protected void render() {
    MeshCollection mc = (MeshCollection) shape;
    for (int i = mc.meshCount; --i >= 0;)
      render1(mc.meshes[i]);
  }

  //draw, isosurface, molecular orbitals
  public boolean render1(Mesh mesh) {  //used by mps renderer
    this.mesh = mesh;
    if (!setVariables())
      return false;
    transform();
    render2();
    viewer.freeTempScreens(screens);
    return true;
  }
  
  private boolean setVariables() {
    vertices = mesh.vertices; //because DRAW might have a text associated with it
    colix = mesh.colix;
    if (mesh == null || mesh.visibilityFlags == 0 || !isGenerator && !g3d.setColix(colix)
        || (vertexCount = mesh.vertexCount) == 0)
      return false;
    normixes = mesh.normixes;
    if (normixes == null || vertices == null)
      return false; 
    //this can happen when user switches windows 
    // during a surface calculation
    lighting = mesh.lighting;
    frontOnly = mesh.frontOnly && !mesh.isTwoSided;
    screens = viewer.allocTempScreens(vertexCount);
    transformedVectors = g3d.getTransformedVertexVectors();
    return true;
  }

  // all of the following methods are overridden in subclasses
  // DO NOT change parameters without first checking for the
  // same method in a subclass.
  
  protected void transform() {
    for (int i = vertexCount; --i >= 0;)
      viewer.transformPoint(vertices[i], screens[i]);
  }
  
  protected boolean isPolygonDisplayable(int i) {
    return true;
  }

  //draw, isosurface,meshRenderer::render1
  protected void render2() {
    if (mesh.showPoints)
      renderPoints();
    if (mesh.drawTriangles)
      renderTriangles(false, false);
    if (mesh.fillTriangles)
      renderTriangles(true, mesh.showTriangles);
  }
  
  protected void renderPoints() {
    for (int i = vertexCount; --i >= 0;)
      if (!frontOnly || transformedVectors[normixes[i]].z >= 0)
        fillSphereCentered(4, screens[i]);
  }

  protected void renderTriangles(boolean fill, boolean iShowTriangles) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    colix = mesh.colix;
    //vertexColixes are only isosurface properties of IsosurfaceMesh, not Mesh
    if (!isGenerator)
      g3d.setColix(colix);
    for (int i = mesh.polygonCount; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (iB == iC) {
        int diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 6 : 3);
        fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, screens[iA],
            screens[iB]);
        continue;
      }
      switch (vertexIndexes.length) {
      case 3:
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0)
          continue;
        if (fill) {
          if (!isGenerator && iShowTriangles) {
            g3d.fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
                colix, normixes[iB], screens[iC], colix, normixes[iC], 0.1f);
            continue;
          }
          fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC]);
          continue;
        }
        drawTriangle(screens[iA], screens[iB], screens[iC], 7);
        continue;
      case 4:
        int iD = vertexIndexes[3];
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0
            && transformedVectors[normixes[iD]].z < 0)
          continue;
        if (fill) {
          fillQuadrilateral(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC],
              screens[iD], colix, normixes[iD]);
          continue;
        }
        drawQuadrilateral(colix, screens[iA], screens[iB], screens[iC],
            screens[iD]);
      }
    }
  }  

  ////////////////////////////////////////////////////////////////////
  
  //cartoons...?
  protected void fillCylinder(byte endcaps, int diameter,
                           Point3i screenA, Point3i screenB) {
    if (isGenerator)System.out.println("ERROR--missing function fillCylinder "+ this);
    g3d.fillCylinder(endcaps, diameter, screenA, screenB);
  }

  //rockets
  protected void fillCone(byte endcap, int diameter,
                         Point3f screenBase, Point3f screenTip) {
    if (isGenerator)System.out.println("ERROR--missing function fillCone "+ this);
    g3d.fillCone(endcap, diameter, screenBase,
        screenTip);
  }
  
  //strands
  protected void drawHermite(int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    if (isGenerator)System.out.println("ERROR--missing function drawHermite "+ this);
    g3d.drawHermite(tension, s0, s1, s2, s3);
  }

  //cartoons, meshribbon
  protected void drawHermite(boolean fill, boolean border,
                          int tension, Point3i s0, Point3i s1, Point3i s2,
                          Point3i s3, Point3i s4, Point3i s5, Point3i s6,
                          Point3i s7, int aspectRatio) {
    if (isGenerator)System.out.println("ERROR--missing function drawHermite2 "+ this);
    g3d.drawHermite(fill, border, tension, s0, s1, s2, s3, s4, s5, s6,
        s7, aspectRatio);
  }
  
  //cartoons, rockets, trace:
  protected void fillHermite(int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    if (isGenerator)System.out.println("ERROR--missing function fillHermite "+ this);
    g3d.fillHermite(tension, diameterBeg, diameterMid, diameterEnd,
        s0, s1, s2, s3);
  }

  //backbone, cartoon
  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB,
                              int yB, int zB) {
    if (isGenerator)System.out.println("ERROR--missing function fillCylinder "+ this);
    g3d.fillCylinder(colixA, colixB, endcaps, diameter, xA, yA, zA, xB, yB, zB);
  }

  //cartoons
  protected void fillTriangle(Point3i ptA, Point3i ptB, Point3i ptC) {
    if (isGenerator)System.out.println("ERROR--missing function fillTriangle "+ this);
    g3d.fillTriangle(ptA, ptB, ptC);
  }
  
  //backbone
  protected void drawLine(short colixA, short colixB, int xA, int yA, int zA, int xB, int yB, int zB) {
    if (isGenerator)System.out.println("ERROR--missing function drawLine "+ this);
    g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
  }
  
  //rockets
  protected void fillCylinderBits(byte endcaps, int diameter,
                                           Point3f screenA, Point3f screenB) {
    if (isGenerator)System.out.println("ERROR--missing function fillCylinderBits "+ this);
    g3d.fillCylinderBits(endcaps, diameter, screenA, screenB);
  }

  //rockets
  protected void fillTriangle(Point3f ptA, Point3f ptB, Point3f ptC) {
    if (isGenerator)System.out.println("ERROR--missing function fillTriangle3 "+ this);
    g3d.fillTriangle(ptA, ptB, ptC);
  }

  //rockets
  protected void fillQuadrilateral(Point3f ptA, Point3f ptB, Point3f ptC, Point3f ptD) {
    if (isGenerator)System.out.println("ERROR--missing function drawQuadrilateral "+ this);
    g3d.fillQuadrilateral(ptA, ptB, ptC, ptD);
  }

  //cartoons, rockets, trace, meshRenderer::render2 (Draw, Isosurface)
  protected void fillSphereCentered(int diameter, Point3i pt) {
    if (isGenerator)System.out.println("ERROR--missing function fillSphereCentered "+ this);
    g3d.fillSphereCentered(diameter, pt);
  }

  //via render2: 
  protected void fillTriangle(Point3i screenA, short colixA, short normixA,
                              Point3i screenB, short colixB, short normixB,
                              Point3i screenC, short colixC, short normixC) {
    if (isGenerator)System.out.println("ERROR--missing function fillTriangle5"+ this);
    g3d.fillTriangle(screenA, colixA, normixA, screenB, colixB, normixB,
        screenC, colixC, normixC);
  }

  protected void drawTriangle(Point3i screenA, Point3i screenB,
                                       Point3i screenC, int check) {
    if (isGenerator)System.out.println("ERROR--missing function drawTriangle5"+ this);
    g3d.drawTriangle(screenA, screenB, screenC, check);
  }

  protected void fillQuadrilateral(Point3i screenA, short colixA, short normixA,
                                   Point3i screenB, short colixB, short normixB,
                              Point3i screenC, short colixC, short normixC,
                              Point3i screenD, short colixD, short normixD) {
    if (isGenerator)System.out.println("ERROR--missing function drawQuadrilateral2 "+ this);
    g3d.fillQuadrilateral(screenA, colixA, normixA, screenB, colixB, normixB,
        screenC, colixC, normixC, screenD, colixD, normixD);
  }


  protected void drawQuadrilateral(short colix, Point3i screenA, Point3i screenB,
                                       Point3i screenC, Point3i screenD) {
    if (isGenerator)System.out.println("ERROR--missing function drawQuadrilateral2 "+ this);
    g3d.drawQuadrilateral(colix, screenA, screenB, screenC, screenD);
  }

}
