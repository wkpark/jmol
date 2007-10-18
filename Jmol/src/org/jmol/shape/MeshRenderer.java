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

import java.util.BitSet;

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
    slabbing = viewer.getSlabEnabled();
    vertices = mesh.vertices; //because DRAW might have a text associated with it
    colix = mesh.colix;
    if (mesh == null || mesh.visibilityFlags == 0 || !g3d.setColix(colix)
        || (vertexCount = mesh.vertexCount) == 0)
      return false;
    normixes = mesh.normixes;
    if (normixes == null || vertices == null)
      return false; 
    //this can happen when user switches windows 
    // during a surface calculation
    lighting = mesh.lighting;
    frontOnly = !slabbing && mesh.frontOnly && !mesh.isTwoSided;
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

  //isosurface,meshRenderer::render1 (just about everything)
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
        g3d.fillSphereCentered(4, screens[i]);
  }

  protected BitSet bsFaces = new BitSet();
  protected void renderTriangles(boolean fill, boolean iShowTriangles) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    colix = mesh.colix;
    //vertexColixes are only isosurface properties of IsosurfaceMesh, not Mesh
    g3d.setColix(colix);
    boolean generateSet = (isGenerator && fill);
    if (generateSet)
      bsFaces.clear();
    for (int i = mesh.polygonCount; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (iB == iC) {
        int diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 6 : 3);
        g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, screens[iA],
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
          if (generateSet) {
            bsFaces.set(i);
            continue;
          }
          if (iShowTriangles) {
            g3d.fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
                colix, normixes[iB], screens[iC], colix, normixes[iC], 0.1f);
            continue;
          }
          g3d.fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC]);
          continue;
        }
        g3d.drawTriangle(screens[iA], screens[iB], screens[iC], 7);
        continue;
      case 4:
        int iD = vertexIndexes[3];
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0
            && transformedVectors[normixes[iD]].z < 0)
          continue;
        if (fill) {
          if (generateSet) {
            bsFaces.set(i);
            continue;
          }
          g3d.fillQuadrilateral(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC],
              screens[iD], colix, normixes[iD]);
          continue;
        }
        g3d.drawQuadrilateral(colix, screens[iA], screens[iB], screens[iC],
            screens[iD]);
      }
    }
    if (generateSet)
      renderExport();
   }

  protected void renderExport() {
    //not implemented for this yet.
      g3d.renderIsosurface(mesh.vertices, mesh.colix, null,
          mesh.getVertexNormals(), mesh.polygonIndexes, bsFaces, mesh.vertexCount,
          mesh.polygonCount);
  }
   
}
