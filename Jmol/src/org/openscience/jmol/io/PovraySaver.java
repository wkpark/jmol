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
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

public class PovraySaver {

  BufferedWriter bw;
  JmolViewer viewer;
  
  Matrix4d rotationMatrix;
  Matrix4d transformMatrix;

  public PovraySaver(JmolViewer viewer, OutputStream out) {
    this.bw = new BufferedWriter(new OutputStreamWriter(out), 8192);
    this.viewer = viewer;
  }

  void out(String str) throws IOException {
    bw.write(str);
  }

  public void writeFrame() throws IOException {
    double edge = viewer.getJmolFrame().getRotationRadius() * 2;
    edge *= 1.1; // for some reason I need a little more margin
    edge /= viewer.getZoomPercent() / 100.0;

    rotationMatrix = viewer.getPovRotateMatrix();
    transformMatrix = viewer.getUnscaledTransformMatrix();

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
    out("#declare Width = "+ viewer.getScreenDimension().width + ";\n");
    out("#declare Height = "+ viewer.getScreenDimension().height + ";\n");
    out("#declare Ratio = Width / Height;\n");
    out("#declare zoom = " + edge + ";\n\n");
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
    out("#macro atom(X, Y, Z, RADIUS, R, G, B)\n" +
        " sphere{<X,Y,Z>,RADIUS\n" +
        "  texture{ pigment{rgb<R,G,B>} finish{\n" + 
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "#end\n\n");
    out("#macro bond1(X1, Y1, Z1, X2, Y2, Z2, RADIUS, R, G, B)\n" +
        " cylinder{<X1,Y1,Z1>,<X2,Y2,Z2>,RADIUS\n" +
        "  texture{ pigment{rgb<R,G,B>} finish{\n" +
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "  sphere{<X1,Y1,Z1>,RADIUS\n" +
        "   texture{ pigment{rgb<R,G,B>} finish{\n" + 
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "  sphere{<X2,Y2,Z2>,RADIUS\n" +
        "   texture{ pigment{rgb<R,G,B>} finish{\n" + 
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "#end\n\n");
    out("#macro bond2(X1, Y1, Z1, XC, YC, ZC, X2, Y2, Z2, " +
        "RADIUS, R1, G1, B1, R2, G2, B2)\n" +
        " cylinder{<X1, Y1, Z1>, <XC, YC, ZC>, RADIUS\n" +
        "  texture{ pigment{rgb<R1, G1, B1>} finish{\n" +
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        " cylinder{<XC, YC, ZC>, <X2, Y2, Z2>, RADIUS\n" +
        "  texture{ pigment{rgb<R2,G2,B2>} finish{\n" +
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "  sphere{<X1,Y1,Z1>,RADIUS\n" +
        "   texture{ pigment{rgb<R1,G1,B1>} finish{\n" + 
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "  sphere{<X2,Y2,Z2>,RADIUS\n" +
        "   texture{ pigment{rgb<R2,G2,B2>} finish{\n" + 
        "   ambient .2 diffuse .6 specular 1 roughness .001 metallic}}}\n" +
        "#end\n\n");
  }

  Point3d point1 = new Point3d();
  Point3d point2 = new Point3d();
  Point3d pointC = new Point3d();

  void writeAtom(int i) throws IOException {
    transformMatrix.transform(viewer.getAtomPoint3d(i), point1);
    double radius = viewer.getAtomRadius(i);
    Color color = viewer.getAtomColor(i);
    double r = color.getRed() / 255.0;
    double g = color.getGreen() / 255.0;
    double b = color.getBlue() / 255.0;
    out("atom(" + point1.x + "," + point1.y + "," + point1.z + ",\n" +
        "     " + radius + ",\n" +
        "     " + r + "," + g + "," + b + ")\n");
  }

  void writeBond(int i) throws IOException {
    transformMatrix.transform(viewer.getBondPoint3d1(i), point1);
    transformMatrix.transform(viewer.getBondPoint3d2(i), point2);
    double radius = viewer.getBondRadius(i);
    Color color1 = viewer.getBondColor1(i);
    Color color2 = viewer.getBondColor2(i);
    double r1 = color1.getRed() / 255.0;
    double g1 = color1.getGreen() / 255.0;
    double b1 = color1.getBlue() / 255.0;
    
    if (color1.equals(color2)) {
      out("bond1(" + point1.x + "," + point1.y + "," + point1.z + ",\n" +
          "      " + point2.x + "," + point2.y + "," + point2.z + ",\n" +
          "      " + radius + ",\n" +
          "      " + r1 + "," + g1 + "," + b1 + ")\n");
    } else {
      pointC.set(point1);
      pointC.add(point2);
      pointC.scale(0.5);
      double r2 = color2.getRed() / 255.0;
      double g2 = color2.getGreen() / 255.0;
      double b2 = color2.getBlue() / 255.0;
      out("bond2(" + point1.x + "," + point1.y + "," + point1.z + ",\n" +
          "      " + pointC.x + "," + pointC.y + "," + pointC.z + ",\n" +
          "      " + point2.x + "," + point2.y + "," + point2.z + ",\n" +
          "      " + radius + ",\n" +
          "      " + r1 + "," + g1 + "," + b1 + ",\n" +
          "      " + r2 + "," + g2 + "," + b2 + ")\n");
    }
  }
}
