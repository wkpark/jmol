/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
    this.allModels = allModels;
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
            povrayColor(viewer.getColorBackground()) + " }\n");
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
   * @param color The color to convert
   *
   * @return A string representaion of the color in povray rgb format.
   */
  protected String povrayColor(Color color) {
    return "rgb<" +
      color.getRed() / 255f + "," +
      color.getGreen() / 255f + "," +
      color.getBlue() / 255f + ">";
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
    Color color = viewer.getAtomColor(i);
    float r = color.getRed() / 255f;
    float g = color.getGreen() / 255f;
    float b = color.getBlue() / 255f;
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
    Color color1 = viewer.getBondColor1(i);
    Color color2 = viewer.getBondColor2(i);
    float r1 = color1.getRed() / 255f;
    float g1 = color1.getGreen() / 255f;
    float b1 = color1.getBlue() / 255f;
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

    out(color1.equals(color2) ? "1" : "2");
    out("(");
    out(x1 + "," + y1 + "," + z1 + ",");
    out(x2 + "," + y2 + "," + z2 + ",");
    out(radius + ",");
    out(r1 + "," + g1 + "," + b1);
    if (!color1.equals(color2)) {
      float r2 = color2.getRed() / 255f;
      float g2 = color2.getGreen() / 255f;
      float b2 = color2.getBlue() / 255f;
      out("," + r2 + "," + g2 + "," + b2);
    }
    out(")\n");
  }

  void writePolymer(int modelIndex, int i) throws IOException {
    Point3f[] points = viewer.getPolymerLeadMidPoints(modelIndex, i);
    if (points != null) {
      out("sphere_sweep {\n");
      out(" linear_spline\n");
      out(" " + points.length + "\n");
      for (int j = 0; j < points.length; j++) {
        Point3f point = points[j];
        transformMatrix.transform(point, point1);
        double d = 0.3; //TODO
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
}
