/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.io;

import org.openscience.jmol.viewer.JmolViewer;
import java.util.Date;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4f;
import java.awt.Dimension;

public class PovraySaver {

  BufferedWriter bw;
  JmolViewer viewer;
  
  Matrix4f rotationMatrix;
  Matrix4f transformMatrix;

  public PovraySaver(JmolViewer viewer, OutputStream out) {
    this.bw = new BufferedWriter(new OutputStreamWriter(out), 8192);
    this.viewer = viewer;
  }

  void out(String str) throws IOException {
    bw.write(str);
  }

  public void writeFrame() throws IOException {
    float zoom = (float)viewer.getJmolFrame().getRotationRadius() * 2;
    zoom *= 1.1f; // for some reason I need a little more margin
    zoom /= viewer.getZoomPercent() / 100f;

    rotationMatrix = viewer.getPovRotateMatrix();
    transformMatrix = viewer.getUnscaledTransformMatrix();
    Dimension screenDimension = viewer.getScreenDimension();
    int minScreenDimension = screenDimension.width;
    if (screenDimension.height < minScreenDimension)
      minScreenDimension = screenDimension.height;

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
    out("// NOTE: if you plan to render at a different resoltion,\n");
    out("// be sure to update the following two lines to maintain\n");
    out("// the correct aspect ratio.\n" + "\n");
    out("#declare Width = "+ screenDimension.width + ";\n");
    out("#declare Height = "+ screenDimension.height + ";\n");
    out("#declare minScreenDimension = " + minScreenDimension + ";\n");
    out("#declare Ratio = Width / Height;\n");
    out("#declare zoom = " + zoom + ";\n");
    out("#declare wireRadius = 1 / minScreenDimension * zoom;\n");
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
    
    for (int i = 0; i < viewer.getAtomCount(); i++)
      writeAtom(i);
    
    out("\n");
    out("//***********************************************\n");
    out("// The list of bonds\n");
    out("//***********************************************\n");
    out("\n");
    
    for (int i = 0; i < viewer.getBondCount(); ++i)
      writeBond(i);
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
   * @param col The color to convert
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
    out("#macro atom(X,Y,Z,RADIUS,R,G,B)\n" +
        " sphere{<X,Y,Z>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" + 
        "#end\n\n");
    out("#macro ring(X,Y,Z,RADIUS,R,G,B)\n" +
        " torus{RADIUS,wireRadius pigment{rgb<R,G,B>}" +
        " translate<X,Z,-Y> rotate<90,0,0>}\n" +
        "#end\n\n");
    out("#macro bond1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        " cylinder{<X1,Y1,Z1>,<X2,Y2,Z2>,RADIUS\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        "  sphere{<X1,Y1,Z1>,RADIUS\n" +
        "   pigment{rgb<R,G,B>}}\n" + 
        "  sphere{<X2,Y2,Z2>,RADIUS\n" +
        "   pigment{rgb<R,G,B>}}\n" +
        "#end\n\n");
    out("#macro bond2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        " cylinder{<X1, Y1, Z1>, <XC, YC, ZC>, RADIUS\n" +
        "  pigment{rgb<R1, G1, B1>}}\n" +
        " cylinder{<XC, YC, ZC>, <X2, Y2, Z2>, RADIUS\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        "  sphere{<X1,Y1,Z1>,RADIUS\n" +
        "   pigment{rgb<R1,G1,B1>}}\n" +
        "  sphere{<X2,Y2,Z2>,RADIUS\n" +
        "   pigment{rgb<R2,G2,B2>}}\n" +
        "#end\n\n");
    out("#macro wire1(X1,Y1,Z1,X2,Y2,Z2,RADIUS,R,G,B)\n" +
        " cylinder{<X1,Y1,Z1>,<X2,Y2,Z2>,wireRadius\n" +
        "  pigment{rgb<R,G,B>}}\n" +
        "#end\n\n");
    out("#macro wire2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        " cylinder{<X1, Y1, Z1>, <XC, YC, ZC>, wireRadius\n" +
        "  pigment{rgb<R1, G1, B1>}}\n" +
        " cylinder{<XC, YC, ZC>, <X2, Y2, Z2>, wireRadius\n" +
        "  pigment{rgb<R2,G2,B2>}}\n" +
        "#end\n\n");
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
    out("#macro dblbond2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,"+
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
        "bond2(X1+offX,Y1+offY,Z1,XC+offX,YC+offY,ZC,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1-offX,Y1-offY,Z1,XC-offX,YC-offY,ZC,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
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
    out("#macro trpbond2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,"+
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
        "bond2(X1+offX,Y1+offY,Z1,XC+offX,YC+offY,ZC,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1     ,Y1     ,Z1,XC     ,YC     ,ZC,X2     ,Y2     ,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "bond2(X1-offX,Y1-offY,Z1,XC-offX,YC-offY,ZC,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
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
    out("#macro dblwire2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,"+
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
        "wire2(X1+offX,Y1+offY,Z1,XC+offX,YC+offY,ZC,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1-offX,Y1-offY,Z1,XC-offX,YC-offY,ZC,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
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
    out("#macro trpwire2(X1,Y1,Z1,XC,YC,ZC,X2,Y2,Z2,"+
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
        "wire2(X1+offX,Y1+offY,Z1,XC+offX,YC+offY,ZC,X2+offX,Y2+offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1     ,Y1     ,Z1,XC     ,YC     ,ZC,X2     ,Y2     ,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "wire2(X1-offX,Y1-offY,Z1,XC-offX,YC-offY,ZC,X2-offX,Y2-offY,Z2,\n"+
        "      RADIUS,R1,G1,B1,R2,G2,B2)\n" +
        "#end\n\n");
  }

  Point3f point1 = new Point3f();
  Point3f point2 = new Point3f();
  Point3f pointC = new Point3f();

  void writeAtom(int i) throws IOException {
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
    if (radius > 0)
      out("atom("+x+","+y+","+z+","+radius+","+r+","+g+","+b+")\n");
    else
      out("ring("+x+","+y+","+z+","+(-radius)+","+r+","+g+","+b+")\n");
  }

  void writeBond(int i) throws IOException {
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
    int order = viewer.getBondOrder(i) & 3;
    
    if (order == 2)
      out("dbl");
    else if (order == 3)
      out("trp");

    if (radius > 0) {
      out("bond");
    } else {
      out("wire");
      radius = -radius;
    }

    if (color1.equals(color2)) {
      out("1("+x1+","+y1+","+z1+","+x2+","+y2+","+z2+",\n" +
          "      "+radius+","+r1+","+g1+","+b1+")\n");
    } else {
      pointC.set(point1);
      pointC.add(point2);
      pointC.scale(0.5f);
      float xC = (float)pointC.x;
      float yC = (float)pointC.y;
      float zC = (float)pointC.z;
      float r2 = color2.getRed() / 255f;
      float g2 = color2.getGreen() / 255f;
      float b2 = color2.getBlue() / 255f;
      out("2("+x1+","+y1+","+z1+","+xC+","+yC+","+zC+",\n" +
          "      "+x2+","+y2+","+z2+","+radius+",\n" +
          "      "+r1+","+g1+","+b1+","+r2+","+g2+","+b2+")\n");
    }
  }
}
