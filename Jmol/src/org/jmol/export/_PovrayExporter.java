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

import java.io.IOException;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.util.BitSetUtil;
import org.jmol.viewer.Viewer;

/*
 * Contributed by pim schravendijk <pimlists@googlemail.com>
 * Jmol 11.3.30
 * Oct 2008
 * 
 */

public class _PovrayExporter extends _Exporter {

  private int nBytes;

  public _PovrayExporter() {
    use2dBondOrderCalculation = true;
  }

  private void output(String data) {
    nBytes += data.length();
    try {
      bw.write(data);
    } catch (IOException e) {
      // ignore for now
    }
  }

  public String finalizeOutput() {
    super.finalizeOutput();
    return getAuxiliaryFileData();
  }

  public void getHeader() {
    nBytes = 0;
    float zoom = viewer.getRotationRadius() * 2;
    zoom *= 1.1f; // for some reason I need a little more margin
    zoom /= viewer.getZoomPercentFloat() / 100f;
    int minScreenDimension = Math.min(screenWidth, screenHeight);

    output("// ******************************************************\n");
    output("// Created by Jmol " + Viewer.getJmolVersion() + "\n");
    output("//\n");
    output("// This script was generated on " + getExportDate() + "\n");
    output("// ******************************************************\n");
    output("\n/*  **** Jmol Embedded Script **** \n");
    output(viewer.getStateInfo());
    output("\n*/\n");
    output("\n");
    output("// ******************************************************\n");
    output("// Declare the resolution, camera, and light sources.\n");
    output("// ******************************************************\n");
    output("\n");
    output("// NOTE: if you plan to render at a different resolution,\n");
    output("// be sure to update the following two lines to maintain\n");
    output("// the correct aspect ratio.\n" + "\n");
    output("#declare Width = " + screenWidth + ";\n");
    output("#declare Height = " + screenHeight + ";\n");
    output("#declare minScreenDimension = " + minScreenDimension + ";\n");
    output("#declare showAtoms = true;\n");
    output("#declare showBonds = true;\n");
    output("camera{\n");
    output("  orthographic\n");
    output("  location < " + screenWidth / 2f + ", " + screenHeight / 2f
        + ", 0>\n" + "\n");
    output("  // Negative right for a right hand coordinate system.\n");
    output("\n");
    output("  sky < 0, -1, 0 >\n");
    output("  right < -" + screenWidth + ", 0, 0>\n");
    output("  up < 0, " + screenHeight + ", 0 >\n");
    output("  look_at < " + screenWidth / 2f + ", " + screenHeight / 2f
        + ", 1000 >\n");
    output("}\n");
    output("\n");

    output("background { color rgb <"
        + rgbFractionalFromColix(viewer.getObjectColix(0), ',') + "> }\n");
    output("// " + viewer.getBackgroundArgb() + " \n");
    output("// " + rgbFractionalBackground(',') + " \n");
    output("\n");

    // light source

    tempP1.set(Graphics3D.getLightSource());
    output("// " + tempP1 + " \n");
    float distance = Math.max(screenWidth, screenHeight);
    output("light_source { <" + tempP1.x * distance + "," + tempP1.y * distance
        + ", " + (-1 * tempP1.z * distance) + "> " + " rgb <0.6,0.6,0.6> }\n");
    output("\n");
    output("\n");

    output("// ***********************************************\n");
    output("// macros for common shapes\n");
    output("// ***********************************************\n");
    output("\n");

    writeMacros();
  }

  public void getFooter() {
    // no footer
  }

  private String getAuxiliaryFileData() {
    return "# Created by: Jmol " + Viewer.getJmolVersion()
        + "\n# Creation date: " + getExportDate() 
        + "\n# File created: "  + fileName + " (" + nBytes + " bytes)" 
        + "\n# Jmol state: " + fileName + ".spt" 
        + "\nInput_File_Name=" + fileName 
        + "\nOutput_to_File=true"
        + "\nOutput_File_Type=%FILETYPE%"
        + "\nOutput_File_Name=%OUTPUTFILENAME%" 
        + "\nWidth=" + screenWidth 
        + "\nHeight=" + screenHeight
        + "\nAntialias=true"
        + "\nAntialias_Threshold=0.1" 
        + "\nDisplay=true"
        + "\nPause_When_Done=true" 
        + "\nVerbose=false" + "\n";

  }

  public void renderAtom(Atom atom, short colix) {
    fillSphereCentered(atom.screenDiameter, atom.screenX, atom.screenY,
        atom.screenZ, colix);
  }

  public void fillCylinder(Point3f atom1, Point3f atom2, short colix1,
                           short colix2, byte endcaps, int madBond,
                           int bondOrder) {

    if (colix1 == colix2) {
      renderJoint(atom1, colix1, endcaps, madBond);
      renderCylinder(atom1, atom2, colix1, endcaps, madBond);
      renderJoint(atom2, colix2, endcaps, madBond);
      return;
    }

    tempV2.set(atom2);
    tempV2.add(atom1);
    tempV2.scale(0.5f);
    tempP1.set(tempV2);
    renderJoint(atom1, colix1, endcaps, madBond);
    renderCylinder(atom1, tempP1, colix1, endcaps, madBond);
    renderCylinder(tempP1, atom2, colix2, endcaps, madBond);
    renderJoint(atom2, colix2, endcaps, madBond);
  }

  public void renderCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    short d = viewer.scaleToScreen((int) pt1.z, madBond);
    if (pt1.distance(pt2) == 0) {
      fillSphereCentered(colix, d, pt1);
      return;
    }
    String color = rgbFractionalFromColix(colix, ',');
    //transformPoint is not needed when bonds are rendered via super.fillCylinders
    //viewer.transformPoint(pt1, tempP1);
    //viewer.transformPoint(pt2, tempP2);
    float radius1 = d / 2f;
    float radius2 = viewer.scaleToScreen((int) pt2.z, madBond / 2);

    // (float)viewer.getBondRadius(i);

    output("b(" + pt1.x + "," + pt1.y + "," + pt1.z + "," + radius1 + ","
        + pt2.x + "," + pt2.y + "," + pt2.z + "," + radius2 + "," + color + ","
        + translucencyFractionalFromColix(colix) + ")\n");
  }

  private void renderJoint(Point3f pt, short colix, byte endcaps, int madBond) {
    // povray by default creates flat endcaps, therefore
    //   joints are only needed in the other cases.
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      String color = rgbFractionalFromColix(colix, ',');
      float radius = viewer.scaleToScreen((int) pt.z, madBond / 2);
      output("s(" + pt.x + "," + pt.y + "," + pt.z + "," + radius + "," + color
          + "," + translucencyFractionalFromColix(colix) + ")\n");
    }
  }

  private void writeMacros() {
    output("#default { finish {\n" + "  ambient "
        + (float) Graphics3D.getAmbientPercent() / 100f + "\n" + "  diffuse "
        + (float) Graphics3D.getDiffusePercent() / 100f + "\n" + "  specular "
        + (float) Graphics3D.getSpecularPercent() / 100f + "\n"
        + "  roughness .00001\n  metallic\n  phong 0.9\n  phong_size 120\n}}"
        + "\n\n");

    writeMacrosAtom();
    //    writeMacrosRing();
    writeMacrosBond();
    writeMacrosJoint();
    writeMacrosTriangle();
  }

  private void writeMacrosAtom() {
    output("#macro a(X,Y,Z,RADIUS,R,G,B,T)\n" + " sphere{<X,Y,Z>,RADIUS\n"
        + "  pigment{rgbt<R,G,B,T>}\n" + "  no_shadow}\n" + "#end\n\n");
  }

  /*
   private void writeMacrosRing() {
   // This type of ring does not take into account perspective effects!
   output("#macro o(X,Y,Z,RADIUS,R,G,B,T)\n"
   + " torus{RADIUS,wireRadius pigment{rgbt<R,G,B,T>}\n"
   + " translate<X,Z,-Y> rotate<90,0,0>\n" + "  no_shadow}\n"
   + "#end\n\n");
   }
   */
  private void writeMacrosBond() {
    // We always use cones here, in orthographic mode this will give us
    //  cones with two equal radii, in perspective mode Jmol will calculate
    //  the cone radii for us.
    output("#macro b(X1,Y1,Z1,RADIUS1,X2,Y2,Z2,RADIUS2,R,G,B,T)\n"
        + " cone{<X1,Y1,Z1>,RADIUS1,<X2,Y2,Z2>,RADIUS2\n"
        + "  pigment{rgbt<R,G,B,T>}\n" + "  no_shadow}\n" + "#end\n\n");
  }

  private void writeMacrosJoint() {
    output("#macro s(X,Y,Z,RADIUS,R,G,B,T)\n" + " sphere{<X,Y,Z>,RADIUS\n"
        + "  pigment{rgbt<R,G,B,T>}\n" + "  no_shadow}\n" + "#end\n\n");
  }

  private void writeMacrosTriangle() {
    output("#macro r(X1,Y1,Z1,X2,Y2,Z2,X3,Y3,Z3,R,G,B,T)\n"
        + " triangle{<X1,Y1,Z1>,<X2,Y2,Z2>,<X3,Y3,Z3>\n"
        + "  pigment{rgbt<R,G,B,T>}\n" + "  no_shadow}\n" + "#end\n\n");
  }

  private String triad(Tuple3f pt) {
    if (Float.isNaN(pt.x))
      return "0,0,0";
    return pt.x + "," + pt.y + "," + pt.z;
  }

  private String color4(short colix) {
    return rgbFractionalFromColix(colix, ',') + ","
        + translucencyFractionalFromColix(colix);
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, Vector3f[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int faceVertexMax) {
    if (nVertices == 0)
      return;
    int nFaces = 0;
    for (int i = BitSetUtil.length(bsFaces); --i >= 0;)
      if (bsFaces.get(i))
        nFaces += (faceVertexMax == 4 && indices[i].length == 4 ? 2 : 1);
    if (nFaces == 0)
      return;
    Hashtable htColixes = new Hashtable();
    String color;

    output("mesh2 {\n");

    output("vertex_vectors { " + nVertices);
    for (int i = 0; i < nVertices; i++) {
      //if (i % 10 == 0)
      //output("\n");
      viewer.transformPoint(vertices[i], tempP1);
      output(", <" + triad(tempP1) + ">");
      output(" //" + i + "\n");

    }
    output("\n}\n");

    boolean haveNormals = (normals != null);
    if (haveNormals) {
      output("normal_vectors { " + nVertices);
      for (int i = 0; i < nVertices; i++) {
        //        if (i % 10 == 0)
        //          output("\n");
        output(", <" + triad(getNormal(vertices[i], normals[i])) + ">");
        output(" //" + i + "\n");
      }
      output("\n}\n");
    }

    if (colixes != null) {
      int nColix = 0;
      for (int i = 0; i < nVertices; i++) {
        color = color4(colixes[i]);
        if (!htColixes.containsKey(color))
          htColixes.put(color, new Integer(nColix++));
      }
      String[] list = new String[nColix];
      Enumeration e = htColixes.keys();
      while (e.hasMoreElements()) {
        color = (String) e.nextElement();
        list[((Integer) htColixes.get(color)).intValue()] = color;
      }

      output("texture_list { " + nColix);
      for (int i = 0; i < nColix; i++)
        output("\n, texture{pigment{rgbt<" + list[i] + ">}}");
      output("\n}\n");
    }
    output("face_indices { " + nFaces);
    //int p = 0; 
    for (int i = BitSetUtil.length(bsFaces); --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      //if ((p++) % 10 == 0)
      //  output("\n");
      output(", <" + indices[i][0] + "," + indices[i][1] + "," + indices[i][2]
          + ">");
      if (colixes != null) {
        color = color4(colixes[indices[i][0]]);
        output("," + ((Integer) htColixes.get(color)).intValue());
        color = color4(colixes[indices[i][1]]);
        output("," + ((Integer) htColixes.get(color)).intValue());
        color = color4(colixes[indices[i][2]]);
        output("," + ((Integer) htColixes.get(color)).intValue());
      }
      output(" //\n");
      if (faceVertexMax == 4 && indices[i].length == 4) {
        output(", <" + indices[i][0] + "," + indices[i][2] + ","
            + indices[i][3] + ">");
        if (colixes != null) {
          color = color4(colixes[indices[i][0]]);
          output("," + ((Integer) htColixes.get(color)).intValue());
          color = color4(colixes[indices[i][2]]);
          output("," + ((Integer) htColixes.get(color)).intValue());
          color = color4(colixes[indices[i][3]]);
          output("," + ((Integer) htColixes.get(color)).intValue());
        }
        output(" //\n");
      }
    }
    output("\n}\n");

    if (colixes == null) {
      output("pigment{rgbt<" + color4(colix) + ">}\n");
    }

    output("}\n");

    /*
     mesh2 {
     vertex_vectors {
     9, 
     <0,0,0>, <0.5,0,0>, <0.5,0.5,0>,
     <1,0,0>, <1,0.5,0>, <1,1,0>   
     <0.5,1,0>, <0,1,0>, <0,0.5,0> 
     }
     normal_vectors {
     9,
     <-1,-1,0>, <0,-1,0>, <0,0,1>,
     <1,-1,0>, <1,0,0>, <1,1,0>,
     <0,1,0>, <-1,1,0>, <-1,0,0>
     }
     texture_list {
     3,
     texture{pigment{rgb <0,0,1>}},
     texture{pigment{rgb 1}},
     texture{pigment{rgb <1,0,0>}}
     }
     face_indices {
     8, 
     <0,1,2>,0,1,2,  <1,3,2>,1,0,2,
     <3,4,2>,0,1,2,  <4,5,2>,1,0,2,
     <5,6,2>,0,1,2,  <6,7,2>,1,0,2,
     <7,8,2>,0,1,2,  <8,0,2>,1,0,2
     }
     }
     */

  }

  private Point3f getNormal(Point3f pt, Vector3f normal) {
    tempP1.set(pt);
    tempP1.add(normal);
    viewer.transformPoint(pt, tempP2);
    viewer.transformPoint(tempP1, tempP3);
    tempP3.sub(tempP2);
    return tempP3;
  }

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3f screenA, Point3f screenB) {
    if (screenA.distance(screenB) == 0) {
      fillSphereCentered(diameter, screenA.x, screenA.y, screenA.z, colix);
      return;
    }
    float radius1 = diameter / 2f;
    float radius2 = radius1;
    output("b(" + triad(screenA) + "," + radius1 + "," + triad(screenB) + ","
        + radius2 + "," + color4(colix) + ")\n");
  }

  public void fillScreenedCircleCentered(short colix, int diameter, int x,
                                         int y, int z) {
    //halos
    String color = rgbFractionalFromColix(colix, ',');
    float r = diameter / 2.0f;
    output("b(" + x + "," + y + "," + z + "," + r + "," + x + "," + y + ","
        + (z + 1) + "," + r + "," + color + ",0.8)\n");
  }

  public void drawPixel(short colix, int x, int y, int z) {
    //measures, meshRibbon
    fillSphereCentered(1.5f, x, y, z, colix);
  }

  public void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    //cartoons, mesh, isosurface
    output("r(" + triad(ptA) + "," + triad(ptB) + "," + triad(ptC) + ","
        + color4(colix) + ")\n");
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    output("b(" + triad(screenBase) + "," + (diameter / 2f) + ","
        + triad(screenTip) + ",0" + "," + color4(colix) + ")\n");
  }

  public void fillSphereCentered(short colix, int diameter, Point3f pt) {
    //cartoons, rockets, trace:    
    output("a(" + triad(pt) + "," + (diameter / 2.0f) + "," + color4(colix)
        + ")\n");
  }

  private void fillSphereCentered(float diameter, float x, float y, float z,
                                  short colix) {
    output("a(" + x + "," + y + "," + z + "," + (diameter / 2.0f) + ","
        + color4(colix) + ")\n");
  }

  public void plotText(int x, int y, int z, short colix, short bgcolix,
                       String text, Font3D font3d) {
    // TODO

  }

  // not implemented: 

  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd, Point3f s0,
                          Point3f s1, Point3f s2, Point3f s3) {
    //cartoons, rockets, trace:
    //System.out.println("pov fileHermite cartoons rockets trace "+this);
  }

  public void drawHermite(short colix, int tension, Point3f s0, Point3f s1,
                          Point3f s2, Point3f s3) {
    //strands, meshribbon:
    //System.out.println("pov drawhermite "+this);
  }

  public void drawHermite(short colix, boolean fill, boolean border,
                          int tension, Point3f s0, Point3f s1, Point3f s2,
                          Point3f s3, Point3f s4, Point3f s5, Point3f s6,
                          Point3f s7, int aspectRatio) {
    //cartoons, meshRibbons:
    //System.out.println("pov draw hermite -- cartoons, meshribbons "+this);
  }

}
