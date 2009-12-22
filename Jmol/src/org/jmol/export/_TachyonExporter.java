/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.export;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.viewer.Viewer;

/*
 * see http://jedi.ks.uiuc.edu/~johns/raytracer/papers/tachyon.pdf
 * 
 */

public class _TachyonExporter extends __RayTracerExporter {

  boolean wasPerspectiveDepth;
  String lighting;
  
  UseTable textures = new UseTable(" ");
 
  boolean initializeOutput(Viewer viewer, Graphics3D g3d, Object output) {
    //wasPerspectiveDepth = viewer.getPerspectiveDepth();
    //viewer.setPerspectiveDepth(false);
    getLightingInfo();
    return super.initializeOutput(viewer, g3d, output);    
  }
  
  private void getLightingInfo() {
    lighting = " AMBIENT " + round(Graphics3D.getAmbientPercent() / 100f)
        + " DIFFUSE " + round(Graphics3D.getDiffusePercent()/100f) 
        + " SPECULAR " + round(Graphics3D.getSpecularPercent() / 100f);  
  }  
  
  /* 
  public String finalizeOutput() {
    if (wasPerspectiveDepth)
      viewer.setPerspectiveDepth(true);
    return super.finalizeOutput();
  }
  */

  protected void outputHeader() {
    super.outputHeader();
    viewer.transformPoint(center, tempP1);
    tempP1.z = 0;
    output("# ******************************************************\n");
    output("# Created by Jmol " + Viewer.getJmolVersion() + "\n");
    output("#\n");
    output("# This script was generated on " + getExportDate() + "\n");
    output("#\n");
    output("# Requires Tachyon version 0.98.7 or newer\n");
    output("#\n");
    output("# Default tachyon rendering command for this scene:\n");
    output("#   tachyon  -aasamples 12 %s -format TARGA -o %s.tga\n");
    output("#\n");
    output("# ******************************************************\n");
    output("\n");
    outputJmolPerspective();
    output("\n");
    output("Begin_Scene\n");
    output("Resolution " + screenWidth + " " + screenHeight + "\n");
    output("Shader_Mode Medium\n"); // not documented.
    output("  Trans_VMD\n");
    output("  Fog_VMD\n");
    output("End_Shader_Mode\n");
    output("Camera\n");
    output("  PerspectiveMode ORTHOGRAPHIC\n");
    output("  Zoom 3.0\n");
    output("  Aspectratio 1\n");
    output("  Antialiasing 12\n");
    output("  Raydepth 8\n");
    output("  Center " + triad(tempP1) + "\n");
    output("  Viewdir 0 0 1\n");
    output("  Updir   0 -1 0\n");
    output("End_Camera\n");
    output("Directional_Light Direction " + round(Graphics3D.getLightSource()) + " Color 1 1 1\n");
    output("\n");
    output("Background " + rgbFractionalFromColix(viewer.getObjectColix(0), ' ')
        + "\n");
    output("\n");
  }

  protected void outputFooter() {
    output("End_Scene\n");
  }

  private String triad(float x, float y, float z) {
    return (int) x + " " + (int) y + " " + (int) z;
  }

  private String triad(Tuple3f pt) {
    if (Float.isNaN(pt.x))
      return "0 0 0";
    return triad(pt.x, pt.y, pt.z);
  }

  protected String opacityFractionalFromArgb(int argb) {
    int translevel = (argb >> 24) & 0xFF;
    return "" + (translevel == 0 ? 1f : 1 - translevel / 255f);
  }

  private String getTexture(short colix) {
    String code = textures.getDef("tc" + colix);
    if (!code.startsWith(" ")) {
      output("TexDef " + code);
      output(lighting);
      output(" Opacity " + opacityFractionalFromColix(colix));
      output(" Phong Plastic 0.5 Phong_size 40");
      output(" Color " + rgbFractionalFromColix(colix, ' '));
      output(" TexFunc 0\n");
      code = " " + code;
    }
    return code + "\n";
  }

  private String getTexture(int argb) {
    String code = textures.getDef("ta" + argb);
    if (!code.startsWith(" ")) {
      output("TexDef " + code);
      output(lighting);
      output(" Ambient 0 Diffuse 0.65 Specular 0 Opacity "
          + opacityFractionalFromArgb(argb));
      output(" Phong Plastic 0.5 Phong_size 40");
      output(" Color " + rgbFractionalFromArgb(argb, ' '));
      output(" TexFunc 0\n");
    }
    return " " + code + "\n";
  }

  protected void outputCircle(int x, int y, int z, float radius, short colix,
                              boolean doFill) {
    tempV1.set(0,0,-1);
    outputRing(x, y, z, tempV1, radius, colix, doFill);
  }

  protected void outputCircleScreened(int x, int y, int z, float radius, short colix) {
    colix = Graphics3D.getColixTranslucent(colix, true, 0.8f);
    tempV1.set(0,0,-1);
    outputRing(x, y, z, tempV1, radius, colix, true);
  }

  private void outputRing(int x, int y, int z, Vector3f tempV1, float radius,
                          short colix, boolean doFill) {
    String code = getTexture(colix);
    output("Ring Center ");
    output(triad(x, y, z));
    output(" Normal " + triad(tempV1));
    output(" Inner " + (doFill ? 0 : radius * 0.95));
    output(" Outer " + radius);
    output(code);
  }

  protected void outputComment(String comment) {
    output("# ");
    output(comment);
    output("\n");
  }

  protected void outputCone(Point3f screenBase, Point3f screenTip, float radius,
                            short colix) {
    //TODO
  }

  protected void outputCylinder(Point3f screenA, Point3f screenB,
                                      float radius, short colix, boolean withCaps) {
    String code = getTexture(colix);
    output("FCylinder Base ");
    output(triad(screenA));
    output(" Apex ");
    output(triad(screenB));
    output(" Rad " + radius);
    output(code);
    if (withCaps) {
      tempV1.sub(screenA, screenB);
      outputRing((int) screenA.x, (int) screenA.y, (int) screenA.z, tempV1, radius, colix, true);
      tempV1.scale(-1);
      outputRing((int) screenB.x, (int) screenB.y, (int) screenB.z, tempV1, radius, colix, true);
    }
  }  
  
  protected void fillConicalCylinder(Point3f screenA, Point3f screenB,
                                     int madBond, short colix, byte endcaps) {
    // conic sections not implemented in Tachyon
    int diameter = viewer.scaleToScreen((int) ((screenA.z + screenB.z)/2f), madBond);
    fillCylinder(colix, endcaps, diameter, screenA, screenB);
   }


  protected void outputCylinderConical(Point3f screenA, Point3f screenB,
                                       float radius1, float radius2, short colix) {
    //not applicable
  }

  protected void outputEllipsoid(double[] coef, short colix) {
    //TODO
  }

  protected void outputIsosurface(Point3f[] vertices, Vector3f[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes, int nVertices,
                                  int nPolygons, int nFaces, BitSet bsFaces,
                                  int faceVertexMax, short colix, Vector colorList, Hashtable htColixes) {
    if (polygonColixes != null) {
      for (int i = nPolygons; --i >= 0;) {
        if (!bsFaces.get(i))
          continue;
        viewer.transformPoint(vertices[indices[i][0]], tempP1);
        viewer.transformPoint(vertices[indices[i][1]], tempP2);
        viewer.transformPoint(vertices[indices[i][2]], tempP3);
        outputTriangle(tempP1, tempP2, tempP3, colix);
      }
      return;
    }
    String code = getTexture(Graphics3D.BLUE);
    output("VertexArray  Numverts " + nVertices + "\nCoords\n");
    for (int i = 0; i < nVertices; i++) {
      viewer.transformPoint(vertices[i], tempP1);
      output(triad(vertices[i]) + "\n");
    }
    output("\nNormals\n");
    for (int i = 0; i < nVertices; i++) {
      output(triad(getScreenNormal(vertices[i], normals[i])) + "\n");
    }
    output("\nColors\n");
    String rgb = (colixes == null ? rgbFromColix(colix, ' ') : null);
    for (int i = 0; i < nVertices; i++) {
      output(colixes == null ? rgb : rgbFromColix(colixes[i], ' ') + "\n");
    }
    output(code);
    output("\nTriMesh " + nFaces + "\n");
    for (int i = 0; i < nVertices; i++) {
      output(colixes == null ? rgb : rgbFromColix(colixes[i], ' ') + "\n");
    }
    for (int i = nPolygons; --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      output(indices[i][0] + " " + indices[i][1] + " " + indices[i][2] + "\n");
      if (faceVertexMax == 4 && indices[i].length == 4) {
        output(indices[i][0] + " " + indices[i][2] + " " + indices[i][3] + "\n");
      }
    }
    output("\nEnd_VertexArray\n");
  }

  protected void outputSphere(float x, float y, float z, float radius,
                                  short colix) {

    // should be a reference to a names texture
    
    String code = getTexture(colix);
    output("Sphere Center ");
    output(triad(x, y, z));
    output(" Rad " + radius);
    output(code);
  }

  protected void outputTextPixel(int x, int y, int z, int argb) {
    String code = getTexture(argb);
    output("BOX MIN ");
    output(triad(x, y, z));
    output(" MAX ");
    output(triad(x + 1, y + 1, z + 1));
    output(code);
  }
  
  protected void outputTriangle(Point3f ptA, Point3f ptB, Point3f ptC, short colix) {
    String code = getTexture(colix);
    output("TRI");
    output(" V0 " + triad(ptA));
    output(" V1 " + triad(ptB));
    output(" V2 " + triad(ptC));
    output(code);
  }

}
