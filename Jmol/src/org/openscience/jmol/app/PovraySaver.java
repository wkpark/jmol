/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.openscience.jmol.app;

import org.jmol.api.*;
import org.jmol.viewer.JmolConstants;

import java.util.Date;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4f;

public class PovraySaver {

  BufferedWriter bw;
  JmolViewer viewer;
  boolean allModels;
  int screenWidth;
  int screenHeight;
  
  Matrix4f transformMatrix;

  public PovraySaver(
          JmolViewer viewer, OutputStream out,
          boolean allModels, int width, int height) {
    this.bw = new BufferedWriter(new OutputStreamWriter(out), 8192);
    this.viewer = viewer;
    this.allModels = allModels || (viewer.getDisplayModelIndex() == -1);
    this.screenWidth = width;
    this.screenHeight = height;
  }

  void out(String str) throws IOException {
    bw.write(str);
  }

  public void writeFrame() throws IOException {
    float zoom = viewer.getRotationRadius() * 2;
    zoom *= 1.1f; // for some reason I need a little more margin
    zoom /= viewer.getZoomPercent() / 100f;

    transformMatrix = viewer.getUnscaledTransformMatrix();
    if ((screenWidth <= 0) || (screenHeight <= 0)) {
        screenWidth = viewer.getScreenWidth();
        screenHeight = viewer.getScreenHeight();
    }
    int minScreenDimension =
      screenWidth < screenHeight ? screenWidth : screenHeight;

    Date now = new Date();
    SimpleDateFormat sdf =
      new SimpleDateFormat("EEE, MMMM dd, yyyy 'at' h:mm aaa");

    String now_st = sdf.format(now);

    out("//******************************************************\n");
    out("// Jmol generated povray script.\n");
    out("//\n");
    out("// This script was generated on :\n");
    out("// " + now_st + "\n");
    out("//******************************************************\n");
    out("\n");
    out("\n");
    out("//******************************************************\n");
    out("// Declare the resolution, camera, and light sources.\n");
    out("//******************************************************\n");
    out("\n");
    out("// NOTE: if you plan to render at a different resolution,\n");
    out("// be sure to update the following two lines to maintain\n");
    out("// the correct aspect ratio.\n" + "\n");
    out("#declare Width = "+ screenWidth + ";\n");
    out("#declare Height = "+ screenHeight + ";\n");
    out("#declare minScreenDimension = " + minScreenDimension + ";\n");
    out("#declare Ratio = Width / Height;\n");
    out("#declare zoom = " + zoom + ";\n");
    //    out("#declare wireRadius = 1 / minScreenDimension * zoom;\n");
    out("#declare showAtoms = true;\n");
    out("#declare showBonds = true;\n");
    out("#declare showPolymers = false;\n");
    out("camera{\n");
    out("  location < 0, 0, zoom>\n" + "\n");
    out("  // Ratio is negative to switch povray to\n");
    out("  // a right hand coordinate system.\n");
    out("\n");
    out("  right < -Ratio , 0, 0>\n");
    out("  look_at < 0, 0, 0 >\n");
    out("}\n");
    out("\n");

    out("background { color " +
            povrayColor(viewer.getBackgroundArgb()) + " }\n");
    out("\n");

    out("light_source { < 0, 0, zoom> " + " rgb <1.0,1.0,1.0> }\n");
    out("light_source { < -zoom, zoom, zoom> "
        + " rgb <1.0,1.0,1.0> }\n");
    out("\n");
    out("\n");

    out("//***********************************************\n");
    out("// macros for common shapes\n");
    out("//***********************************************\n");
    out("\n");
    
    writeMacros();
    
    out("//***********************************************\n");
    out("// List of all of the atoms\n");
    out("//***********************************************\n");
    out("\n");
    
    out("#if (showAtoms)\n");
    if (allModels) {
      out("#switch (clock)\n");
      for (int m = 0; m < viewer.getModelCount(); m++) {
        out("#range (" + (m + 0.9) + "," + (m + 1.1) + ")\n");
        for (int i = 0; i < viewer.getAtomCount(); i++) {
          writeAtom(m, i);   
        }
        out("#break\n");
      }
      out("#end\n");
    } else {
      for (int i = 0; i < viewer.getAtomCount(); i++)
        writeAtom(viewer.getDisplayModelIndex(), i);
    }
    out("#end\n");
    
    out("\n");
    out("//***********************************************\n");
    out("// The list of bonds\n");
    out("//***********************************************\n");
    out("\n");
    
    out("#if (showBonds)\n");
    if (allModels) {
      out("#switch (clock)\n");
      for (int m = 0; m < viewer.getModelCount(); m++) {
        out("#range (" + (m + 0.9) + "," + (m + 1.1) + ")\n");
        for (int i = 0; i < viewer.getBondCount(); i++) {
          writeBond(m, i);   
        }
        out("#break\n");
      }
      out("#end\n");
    } else {
      for (int i = 0; i < viewer.getBondCount(); ++i)
        writeBond(viewer.getDisplayModelIndex(), i);
    }
    out("#end\n");
    
    out("\n");
    out("//***********************************************\n");
    out("// The list of polymers\n");
    out("//***********************************************\n");
    out("\n");
    
    out("#if (showPolymers)\n");
    if (allModels) {
      out("#switch (clock)\n");
      for (int m = 0; m < viewer.getModelCount(); m++) {
        out("#range (" + (m + 0.9) + "," + (m + 1.1) + ")\n");
        for (int i = 0; i < viewer.getPolymerCountInModel(m); i++) {
          writePolymer(m, i);
        }
        out("#break\n");
      }
      out("#end\n");
    } else {
      for (int i = 0; i < viewer.getPolymerCountInModel(viewer.getDisplayModelIndex()); i++) {
        writePolymer(viewer.getDisplayModelIndex(), i);
      }
    }
    out("#end\n");
  }

  public synchronized void writeFile() {

    try {
      writeFrame();
      bw.close();
    } catch (IOException e) {
      System.out.println("Got IOException " + e + " trying to write frame.");
    }
  }

  /**
   * Takes a java colour and returns a String representing the
   * colour in povray eg 'rgb<1.0,0.0,0.0>'
   *
   * @param argb The color to convert
   *
   * @return A string representaion of the color in povray rgb format.
   */
  protected String povrayColor(int argb) {
    return "rgb<" +
      getRed(argb) + "," +
      getGrn(argb) + "," +
      getBlu(argb) + ">";
  }

  void writeMacros() throws IOException {
    out("#default { finish {\n" +
        " ambient .2 diffuse .6 specular 1 roughness .001 metallic}}\n\n");
    writeMacrosAtom();
    //writeMacrosRing();
    //writeMacrosWire();
    //writeMacrosDoubleWire();
    //writeMacrosTripleWire();
    writeMacrosBond();
    writeMacrosDoubleBond();
    writeMacrosTripleBond();
    writeMacrosHydrogenBond();
    writeMacrosAromaticBond();
  }
  void writeMacrosAtom() throws IOException {
    out("#macro atom(X,Y,Z,RADIUS,R,G,B)\n" +
        " sphere{<X,Y,Z>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" + 
        "#end\n\n");
  }
  void writeMacrosRing() throws IOException {
    out("#macro ring(X,Y,Z,RADIUS,R,G,B)\n" +
        " torus{RADIUS,wireRadius pigment{rgb<R,G,B>}" +
        " translate<X,Z,-Y> rotate<90,0,0>}\n" +
        "#end\n\n");
  }
  void writeMacrosBond() throws IOException {
    out("#macro bond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        " cylinder{<X1,Y1,Z1>,<X2,Y2,Z2>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        " sphere{<X1,Y1,Z1>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" + 
        " sphere{<X2,Y2,Z2>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        "#end\n\n");
    out("#macro bond2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local xc = (X1 + X2) / 2;\n" +
        "#local yc = (Y1 + Y2) / 2;\n" +
        "#local zc = (Z1 + Z2) / 2;\n" +
        " cylinder{<X1,Y1,Z1>,<xc,yc,zc>,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}\n" +
        " cylinder{<xc,yc,zc>,<X2,Y2,Z2>,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        " sphere{<X1,Y1,Z1>,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}\n" +
        " sphere{<X2,Y2,Z2>,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        "#end\n\n");
  }
  void writeMacrosDoubleBond() throws IOException {
    out("#macro dblbond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "bond1(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R,G,B)\n" +
        "bond1(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,RADIUS,R,G,B)\n" +
        "#end\n\n");
    out("#macro dblbond2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "bond2(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
  }
  void writeMacrosTripleBond() throws IOException {
    out("#macro trpbond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 5/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "bond1(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R,G,B)\n" +
        "bond1(X1     ,Y1     ,Z1,X2     ,Y2     ,Z2,RADIUS,R,G,B)\n" +
        "bond1(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,RADIUS,R,G,B)\n" +
        "#end\n\n");
    out("#macro trpbond2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 5/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "bond2(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1     ,Y1     ,Z1,X2     ,Y2     ,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
  }
  void writeMacrosHydrogenBond() throws IOException {
    out("#macro hbond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = (X2 - X1) / 10;\n" +
        "#local dy = (Y2 - Y1) / 10;\n" +
        "#local dz = (Z2 - Z1) / 10;\n" +
        " cylinder{<X1+dx  ,Y1+dy  ,Z1+dz  >,<X1+3*dx,Y1+3*dy,Z1+3*dz>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        " cylinder{<X1+4*dx,Y1+4*dy,Z1+4*dz>,<X2-4*dx,Y2-4*dy,Z2-4*dz>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        " cylinder{<X2-3*dx,Y2-3*dy,Z2-3*dz>,<X2-dx  ,Y2-dy  ,Z2-dz  >,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        "#end\n\n");
    out("#macro hbond2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = (X2 - X1) / 10;\n" +
        "#local dy = (Y2 - Y1) / 10;\n" +
        "#local dz = (Z2 - Z1) / 10;\n" +
        "#local xc = (X1 + X2) / 2;\n" +
        "#local yc = (Y1 + Y2) / 2;\n" +
        "#local zc = (Z1 + Z2) / 2;\n" +
        " cylinder{<X1+dx  ,Y1+dy  ,Z1+dz  >,<X1+3*dx,Y1+3*dy,Z1+3*dz>,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}\n" +
        " cylinder{<X1+4*dx,Y1+4*dy,Z1+4*dz>,<xc     ,yc     ,zc     >,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}\n" +
        " cylinder{<xc     ,yc     ,zc     >,<X2-4*dx,Y2-4*dy,Z2-4*dz>,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        " cylinder{<X2-3*dx,Y2-3*dy,Z2-3*dz>,<X2-dx  ,Y2-dy  ,Z2-dz  >,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        "#end\n\n");
  }
  void writeMacrosAromaticBond() throws IOException {
    out("#macro abond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = (X2 - X1) / 12;\n" +
        "#local dy = (Y2 - Y1) / 12;\n" +
        "#local dz = (Z2 - Z1) / 12;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        " bond1(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R,G,B)\n" +
        " cylinder{<X1-offX+2*dx,Y1-offY+2*dy,Z1+2*dz>,<X1-offX+5*dx,Y1-offY+5*dy,Z1+5*dz>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}" +
        " cylinder{<X2-offX-2*dx,Y2-offY-2*dy,Z2-2*dz>,<X2-offX-5*dx,Y2-offY-5*dy,Z2-5*dz>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}" +
        "#end\n\n");
    out("#macro abond2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = (X2 - X1) / 12;\n" +
        "#local dy = (Y2 - Y1) / 12;\n" +
        "#local dz = (Z2 - Z1) / 12;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        " bond2(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        " cylinder{<X1-offX+2*dx,Y1-offY+2*dy,Z1+2*dz>,<X1-offX+3.5*dx,Y1-offY+3.5*dy,Z1+3.5*dz>,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}" +
        " cylinder{<X1-offX+5*dx,Y1-offY+5*dy,Z1+5*dz>,<X1-offX+3.5*dx,Y1-offY+3.5*dy,Z1+3.5*dz>,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}" +
        " cylinder{<X2-offX-5*dx,Y2-offY-5*dy,Z2-5*dz>,<X2-offX-3.5*dx,Y2-offY-3.5*dy,Z2-3.5*dz>,RADIUS\n" +
        "  pigment{rgb<R1,G1,B1>}}" +
        " cylinder{<X2-offX-2*dx,Y2-offY-2*dy,Z2-2*dz>,<X2-offX-3.5*dx,Y2-offY-3.5*dy,Z2-3.5*dz>,RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}" +
        "#end\n\n");
  }
  void writeMacrosWire() throws IOException {
    out("#macro wire1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        " cylinder{<X1,Y1,Z1>,<X2,Y2,Z2>,wireRadius\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        "#end\n\n");
    out("#macro wire2(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local xc = (X1 + X2) / 2;\n" +
        "#local yc = (Y1 + Y2) / 2;\n" +
        "#local zc = (Z1 + Z2) / 2;\n" +
        " cylinder{<X1,Y1,Z1>,<xc,yc,zc>,wireRadius\n" +
        "  pigment{rgb<R1,G1,B1>}}\n" +
        " cylinder{<xc,yc,zc>,<X2,Y2,Z2>,wireRadius\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        "#end\n\n");
  }
  void writeMacrosDoubleWire() throws IOException {
    out("#macro dblwire1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "wire1(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R,G,B)\n" +
        "wire1(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,RADIUS,R,G,B)\n" +
        "#end\n\n");
    out("#macro dblwire2(X1,Y1,Z1,X2,Y2,Z2,"+
        "RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 3/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "wire2(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
  }
  void writeMacrosTripleWire() throws IOException {
    out("#macro trpwire1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 5/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "wire1(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,RADIUS,R,G,B)\n" +
        "wire1(X1     ,Y1     ,Z1,X2     ,Y2     ,Z2,RADIUS,R,G,B)\n" +
        "wire1(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,RADIUS,R,G,B)\n" +
        "#end\n\n");
    out("#macro trpwire2(X1,Y1,Z1,X2,Y2,Z2,"+
        "RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#local dx = X2 - X1;\n" +
        "#local dy = Y2 - Y1;\n" +
        "#local mag2d = sqrt(dx*dx + dy*dy);\n" +
        "#local separation = 5/2 * RADIUS;\n" +
        "#if (dx + dy)\n" +
        " #local offX = separation * dy / mag2d;\n" +
        " #local offY = separation * -dx / mag2d;\n" +
        "#else\n" +
        " #local offX = 0;\n" +
        " #local offY = separation;\n" +
        "#end\n" +
        "wire2(X1+offX,Y1+offY,Z1,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1     ,Y1     ,Z1,X2     ,Y2     ,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1-offX,Y1-offY,Z1,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
  }

  Point3f point1 = new Point3f();
  Point3f point2 = new Point3f();
  Point3f pointC = new Point3f();

  void writeAtom(int modelIndex, int i) throws IOException {
  	int model = viewer.getAtomModelIndex(i);
  	if (model != modelIndex) {
  	  return;
  	}
    float radius = (float)viewer.getAtomRadius(i);
    if (radius == 0)
      return;
    transformMatrix.transform(viewer.getAtomPoint3f(i), point1);
    float x = (float)point1.x;
    float y = (float)point1.y;
    float z = (float)point1.z;
    int argb = viewer.getAtomArgb(i);
    float r = getRed(argb);
    float g = getGrn(argb);
    float b = getBlu(argb);
    out("atom("+x+","+y+","+z+","+radius+","+r+","+g+","+b+")\n");
  }

  void writeBond(int modelIndex, int i) throws IOException {
  	int model = viewer.getBondModelIndex(i);
  	if (model != modelIndex) {
  	  return;
  	}
    float radius = (float)viewer.getBondRadius(i);
    if (radius == 0)
      return;
    transformMatrix.transform(viewer.getBondPoint3f1(i), point1);
    float x1 = (float)point1.x;
    float y1 = (float)point1.y;
    float z1 = (float)point1.z;
    transformMatrix.transform(viewer.getBondPoint3f2(i), point2);
    float x2 = (float)point2.x;
    float y2 = (float)point2.y;
    float z2 = (float)point2.z;
    int argb1 = viewer.getBondArgb1(i);
    int argb2 = viewer.getBondArgb2(i);
    float r1 = getRed(argb1);
    float g1 = getGrn(argb1);
    float b1 = getBlu(argb1);
    int order = viewer.getBondOrder(i);
    
    switch (order) {
    case 1:
      out("bond");
      break;
    
    case 2:
      out("dblbond");
      break;
    
    case 3:
      out("trpbond");
      break;
    
    case JmolConstants.BOND_AROMATIC:
      //out("bond");
      //TODO: Render aromatic bond as in Jmol : a full cylinder and a dashed cylinder
      // The problem is to place correctly the two cylinders !
      out("abond");
      break;
      
    default:
      if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
        out("hbond");   
      } else {
        return;
      }
    }

    out(argb1 == argb2 ? "1" : "2");
    out("(");
    out(x1 + "," + y1 + "," + z1 + ",");
    out(x2 + "," + y2 + "," + z2 + ",");
    out(radius + ",");
    out(r1 + "," + g1 + "," + b1);
    if (argb1 != argb2) {
      float r2 = getRed(argb2);
      float g2 = getGrn(argb2);
      float b2 = getBlu(argb2);
      out("," + r2 + "," + g2 + "," + b2);
    }
    out(")\n");
  }

  void writePolymer(int modelIndex, int i) throws IOException {
    Point3f[] points = viewer.getPolymerLeadMidPoints(modelIndex, i);
    Point3f[] controls = computeControlPoints(points);
    if (controls != null) {
      out("sphere_sweep {\n");
      out(" b_spline\n");
      out(" " + controls.length + "\n");
      for (int j = 0; j < controls.length; j++) {
        Point3f point = controls[j];
        transformMatrix.transform(point, point1);
        double d = 0.2; //TODO
        out(" <" + point1.x + "," + point1.y + "," + point1.z + ">," + d + "\n");
      }
      Color color = Color.BLUE; //TODO
      float r = color.getRed() / 255f;
      float g = color.getGreen() / 255f;
      float b = color.getBlue() / 255f;
      out(" pigment{rgb<" + r + "," + g + "," + b + ">}\n");
      out("}\n");
    }
  }
  
  /**
   * Computes the control points of a b-spline that goes through a set of points
   * 
   * @param path Set of n points to go through
   * @return Set of n+2 control points
   */
  Point3f[] computeControlPoints(Point3f[] path) {
  	// NOTE :
  	// I digged this code out from a program I wrote more than ten years ago
  	// The program was in C++ and with not many comments
  	// I don't remember well how it is working
  	// Hence the lack of explanations in the code :-)
  	Point3f[] controls = null;
  	if ((path != null) && (path.length >= 2)) {
  	
  	  // Determine if it is a closed loop
  	  int length = path.length;
  	  boolean loop =
  	    (path[0].x == path[length - 1].x) &&
		(path[0].y == path[length - 1].y) &&
		(path[0].z == path[length - 1].z);
  	  
  	  if (!loop) {
  	    if (length > 2) {
  	    
  	      // Create vectors for computations
  	      Point3d[] values = new Point3d[length + 2];
  	      Point3d[] results1 = new Point3d[length + 2];
  	      Point3d[] results2 = new Point3d[length + 2];
  	      
  	      // Initialize vectors for computations
  	      values[0] = new Point3d(path[0]);
  	      for (int i = 0; i < length; i++) {
  	        values[i + 1] = new Point3d(
  	            6 * path[i].x, 6 * path[i].y, 6 * path[i].z);
  	      }
  	      values[length + 1] = new Point3d(
  	          values[length].x / 6.0, values[length].y / 6.0, values[length].z / 6.0);
  	      
  	      for (int i = 0; i < length + 2; i++) {
  	      	results1[i] = new Point3d(0, 0, 0);
  	      	results2[i] = new Point3d(0, 0, 0);
  	      }
  	      results2[0].set(1, 1, 1);
  	      results1[1].set(values[0]);
  	      
  	      // Computation of control points
  	      for (int i = 2; i < length + 2; i++) {
  	        results1[i].x = values[i - 1].x - 4 * results1[i - 1].x - results1[i - 2].x;
  	        results1[i].y = values[i - 1].y - 4 * results1[i - 1].y - results1[i - 2].y;
  	        results1[i].z = values[i - 1].z - 4 * results1[i - 1].z - results1[i - 2].z;
  	        results2[i].x = -4 * results2[i - 1].x - results2[i - 2].x;
  	        results2[i].y = -4 * results2[i - 1].y - results2[i - 2].y;
  	        results2[i].z = -4 * results2[i - 1].z - results2[i - 2].z;
  	      }
  	      double xA = (values[length + 1].x - results1[length].x) / results2[length].x;
  	      double yA = (values[length + 1].y - results1[length].y) / results2[length].y;
  	      double zA = (values[length + 1].z - results1[length].z) / results2[length].z;
  	      
  	      // Creation of the control points array
  	      Point3f[] points = new Point3f[length + 2];
  	      for (int i = 0; i < length + 2; i++) {
  	        points[i] = new Point3f(
  	            (float) (results1[i].x + xA * results2[i].x),
  	            (float) (results1[i].y + yA * results2[i].y),
  	            (float) (results1[i].z + zA * results2[i].z));
  	      }
  	      controls = points;
  	    } else {
  	      Point3f[] points = new Point3f[length + 2];
  	      points[0] = new Point3f(
  	          2 * path[0].x - path[1].x,
			  2 * path[0].y - path[1].y,
			  2 * path[0].z - path[1].z);
  	      points[1] = new Point3f(path[0]);
  	      points[2] = new Point3f(path[1]);
  	      points[3] = new Point3f(
  	          2 * path[1].x - path[0].x,
			  2 * path[1].y - path[0].y,
			  2 * path[1].z - path[0].z);
  	      controls = points;
  	    }
  	    
  	  } else {
  	    if (length > 3) {
  	    
    	  // Create vectors for computations
    	  Point3d[] values = new Point3d[length + 2];
    	  Point3d[] results1 = new Point3d[length + 2];
    	  Point3d[] results2 = new Point3d[length + 2];
    	  Point3d[] results3 = new Point3d[length + 2];
    	  Point3f[] points = new Point3f[length + 2];

  	      // Initialize vectors for computations
  	      for (int i = 0; i < length - 1; i++) {
  	        values[i] = new Point3d(
  	            6 * path[i].x, 6 * path[i].y, 6 * path[i].z);
  	        points[i] = new Point3f(0, 0, 0);
  	        results1[i] = new Point3d(0, 0, 0);
  	        results2[i] = new Point3d(0, 0, 0);
  	        results3[i] = new Point3d(0, 0, 0);
  	      }
  	      results2[0].set(1, 1, 1);
  	      results3[0].set(1, 1, 1);

  	      // Computation of control points
  	      for (int i = 2; i < length - 1; i++) {
  	        results1[i].x = values[i - 2].x - results1[i - 2].x - 4 * results1[i - 1].x;
  	        results1[i].y = values[i - 2].y - results1[i - 2].y - 4 * results1[i - 1].y;
  	        results1[i].z = values[i - 2].z - results1[i - 2].z - 4 * results1[i - 1].z;
  	        results2[i].x = - results2[i - 2].x - 4 * results2[i - 1].x;
  	        results2[i].y = - results2[i - 2].y - 4 * results2[i - 1].y;
  	        results2[i].z = - results2[i - 2].z - 4 * results2[i - 1].z;
  	        results3[i].x = - results3[i - 2].x - 4 * results3[i - 1].x;
  	        results3[i].y = - results3[i - 2].y - 4 * results3[i - 1].y;
  	        results3[i].z = - results3[i - 2].z - 4 * results3[i - 1].z;
  	      }
          double ax1 = 1 + results2[length - 3].x + 4 * results2[length - 2].x;
          double ay1 = 1 + results2[length - 3].y + 4 * results2[length - 2].y;
          double az1 = 1 + results2[length - 3].z + 4 * results2[length - 2].z;
          double ax2 = 4 + results2[length - 2].x;
          double ay2 = 4 + results2[length - 2].y;
          double az2 = 4 + results2[length - 2].z;
          double bx1 = 1 + results3[length - 3].x + 4 * results3[length - 2].x;
          double by1 = 1 + results3[length - 3].y + 4 * results3[length - 2].y;
          double bz1 = 1 + results3[length - 3].z + 4 * results3[length - 2].z;
          double bx2 = 1 + results3[length - 2].x;
          double by2 = 1 + results3[length - 2].y;
          double bz2 = 1 + results3[length - 2].z;
          double cx1 = values[length - 3].x - results1[length - 3].x - 4 * results1[length - 2].x;
          double cy1 = values[length - 3].y - results1[length - 3].y - 4 * results1[length - 2].y;
          double cz1 = values[length - 3].z - results1[length - 3].z - 4 * results1[length - 2].z;
          double cx2 = values[length - 2].x - results1[length - 2].x;
          double cy2 = values[length - 2].y - results1[length - 2].y;
          double cz2 = values[length - 2].z - results1[length - 2].z;
          points[0].set(
              (float) ((cx1 * bx2 - cx2 * bx1) / (ax1 * bx2 - ax2 * bx1)),
			  (float) ((cy1 * by2 - cy2 * by1) / (ay1 * by2 - ay2 * by1)),
			  (float) ((cz1 * bz2 - cz2 * bz1) / (az1 * bz2 - az2 * bz1)));
          points[1].set(
              (float) ((cx1 * ax2 - cx2 * ax1) / (ax2 * bx1 - ax1 * bx2)),
			  (float) ((cy1 * ay2 - cy2 * ay1) / (ay2 * by1 - ay1 * by2)),
			  (float) ((cz1 * az2 - cz2 * az1) / (az2 * bz1 - az1 * bz2)));
          for (int i = 2; i < length - 1; i++) {
          	points[i].set(
          	    (float) (results1[i].x + results2[i].x * points[0].x + results3[i].x * points[1].x),
				(float) (results1[i].y + results2[i].y * points[0].y + results3[i].y * points[1].y),
				(float) (results1[i].z + results2[i].z * points[0].z + results3[i].z * points[1].z));
          }
          points[length - 1] = new Point3f(points[0]);
          points[length    ] = new Point3f(points[1]);
          points[length + 1] = new Point3f(points[2]);
          controls = points;
  	    } else {
  	      Point3f[] points = new Point3f[4];
  	      points[0] = new Point3f(
  	          2 * path[0].x - path[1].x,
			  2 * path[0].y - path[1].y,
			  2 * path[0].z - path[1].z);
  	      points[1] = new Point3f(path[0]);
  	      points[2] = new Point3f(path[1]);
  	      points[3] = new Point3f(
  	          2 * path[1].x - path[0].x,
			  2 * path[1].y - path[0].y,
			  2 * path[1].z - path[0].z);
  	      controls = points;
  	    }
  	  }
  	}
  	return controls;
  }

  float getRed(int argb) {
    return ((argb >> 16) & 0xFF) / 255f;
  }

  float getGrn(int argb) {
    return ((argb >> 8) & 0xFF) / 255f;
  }

  float getBlu(int argb) {
    return (argb & 0xFF) / 255f;
  }
}
