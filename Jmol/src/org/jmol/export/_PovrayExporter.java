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
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;

/*
 * Contributed by pim schravendijk <pimlists@googlemail.com>
 * Jmol 11.3.30
 * Oct 2008
 * 
 */

public class _PovrayExporter extends _Exporter {

  int screenWidth;
  int screenHeight;

  Matrix4f transformMatrix;

  Point3f povpt1 = new Point3f();
  Point3f povpt2 = new Point3f();
  Point3f povpt3 = new Point3f();
  Point3i povpti = new Point3i();

  public _PovrayExporter() {
    use2dBondOrderCalculation = true;
  }

  public void getHeader() {

    // frame size formatting should be general part of _Exporter class
    float zoom = viewer.getRotationRadius() * 2;
    zoom *= 1.1f; // for some reason I need a little more margin
    zoom /= viewer.getZoomPercentFloat() / 100f;

    transformMatrix = viewer.getUnscaledTransformMatrix();
    if ((screenWidth <= 0) || (screenHeight <= 0)) {
      screenWidth = viewer.getScreenWidth();
      screenHeight = viewer.getScreenHeight();
    }
    int minScreenDimension = screenWidth < screenHeight ? screenWidth
        : screenHeight;

    output.append(getAuxiliaryFileData());
    output.append("\\-PART II-\\");
    output.append("//******************************************************\n");
    output.append("// Jmol generated povray script.\n");
    output.append("//\n");
    output.append("// This script was generated on :\n");
    output.append("// " + getExportDate() + "\n");
    output.append("//******************************************************\n");
    output.append("\n");
    output.append("\n");
    output.append("//******************************************************\n");
    output.append("// Declare the resolution, camera, and light sources.\n");
    output.append("//******************************************************\n");
    output.append("\n");
    output
        .append("// NOTE: if you plan to render at a different resolution,\n");
    output.append("// be sure to update the following two lines to maintain\n");
    output.append("// the correct aspect ratio.\n" + "\n");
    output.append("#declare Width = " + screenWidth + ";\n");
    output.append("#declare Height = " + screenHeight + ";\n");
    output
        .append("#declare minScreenDimension = " + minScreenDimension + ";\n");
    //    output.append("#declare wireRadius = 1 / minScreenDimension * zoom;\n");
    output.append("#declare showAtoms = true;\n");
    output.append("#declare showBonds = true;\n");
    output.append("camera{\n");
    output.append("  orthographic\n");
    output.append("  location < " + screenWidth / 2f + ", " + screenHeight / 2f
        + ", 0>\n" + "\n");
    output.append("  // Negative right for a right hand coordinate system.\n");
    output.append("\n");
    output.append("  sky < 0, -1, 0 >\n");
    output.append("  right < -" + screenWidth + ", 0, 0>\n");
    output.append("  up < 0, " + screenHeight + ", 0 >\n");
    output.append("  look_at < " + screenWidth / 2f + ", " + screenHeight / 2f
        + ", 1000 >\n");
    output.append("}\n");
    output.append("\n");

    output.append("background { color rgb <"
        + rgbFractionalFromColix(viewer.getObjectColix(0), ',') + "> }\n");
    output.append("// " + viewer.getBackgroundArgb() + " \n");
    output.append("// " + rgbFractionalBackground(',') + " \n");
    output.append("\n");

    // light source
    
    povpt1.set(Graphics3D.getLightSource());
    viewer.transformPoint(povpt1,povpti);

    output.append("light_source { <" + povpt1.x*screenWidth + "," 
        + povpt1.y*screenHeight + ", "
        + povpt1.z + "> " + " rgb <0.6,0.6,0.6> }\n");
    output.append("\n");
    output.append("\n");

    output.append("//***********************************************\n");
    output.append("// macros for common shapes\n");
    output.append("//***********************************************\n");
    output.append("\n");

    writeMacros();
  }

  public void getFooter() {
    // no footer
  }

  private String getAuxiliaryFileData() {
    return 
        "Input_File_Name=%INPUTFILENAME%"
      + "\nOutput_to_File=true"
      + "\nOutput_File_Type=T"
      + "\nOutput_File_Name=%OUTPUTFILENAME%"
      + "\nHeight=" + screenHeight 
      + "\nWidth=" + screenWidth
      + "\nAntialias=true"
      + "\nAntialias_Threshold=0.1"
      + "\nDisplay=true"
      + "\nPause_When_Done=true"
      + "\nVerbose=false"
      + "\n";
    
  }
  

  public void renderAtom(Atom atom, short colix) {
    String color = rgbFractionalFromColix(colix, ',');
    float r = atom.getScreenRadius();
    //    float r = viewer.scaleToPerspective(atom.screenZ, atom.getMadAtom());
    viewer.transformPoint(atom, povpt1);
    output.append("atom(" + povpt1.x + "," + povpt1.y + "," + povpt1.z + ","
        + r + "," + color + "," + translucencyFractionalFromColix(colix)
        + ")\n");
    nBalls++;
  }

  public void renderBond(Point3f atom1, Point3f atom2, short colix1,
                         short colix2, byte endcaps, int madBond, int bondOrder) {

    if (colix1 == colix2) {
      renderJoint(atom1, colix1, endcaps, madBond);
      renderCylinder(atom1, atom2, colix1, endcaps, madBond);
      renderJoint(atom2, colix2, endcaps, madBond);
      return;
    }

    temp2.set(atom2);
    temp2.add(atom1);
    temp2.scale(0.5f);
    tempP.set(temp2);
    renderJoint(atom1, colix1, endcaps, madBond);
    renderCylinder(atom1, tempP, colix1, endcaps, madBond);
    renderCylinder(tempP, atom2, colix2, endcaps, madBond);
    renderJoint(atom2, colix2, endcaps, madBond);
  }

  public void renderCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    nCyl++;
    String color = rgbFractionalFromColix(colix, ',');
    //transformPoint is not needed when bonds are rendered via super.fillCylinders
    //viewer.transformPoint(pt1, povpt1);
    //viewer.transformPoint(pt2, povpt2);
    float radius1 = viewer.scaleToScreen((int) pt1.z, madBond / 2);
    float radius2 = viewer.scaleToScreen((int) pt2.z, madBond / 2);

    // (float)viewer.getBondRadius(i);

    output.append("bond(" + pt1.x + "," + pt1.y + "," + pt1.z + "," + radius1
        + "," + pt2.x + "," + pt2.y + "," + pt2.z + "," + radius2 + "," + color
        + "," + translucencyFractionalFromColix(colix) + ")\n");
  }

  private void renderJoint(Point3f pt, short colix, byte endcaps, int madBond) {
    // povray by default creates flat endcaps, therefore
    //   joints are only needed in the other cases.
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      String color = rgbFractionalFromColix(colix, ',');
      float radius = viewer.scaleToScreen((int) pt.z, madBond / 2);
      output.append("joint(" + pt.x + "," + pt.y + "," + pt.z + "," + radius
          + "," + color + "," + translucencyFractionalFromColix(colix) + ")\n");
    }
  }

  private void writeMacros() {
    output.append("#default { finish {\n" + "  ambient "
        + (float) Graphics3D.getAmbientPercent() / 100f + "\n" + "  diffuse "
        + (float) Graphics3D.getDiffusePercent() / 100f + "\n" + "  specular "
        + (float) Graphics3D.getSpecularPercent() / 100f + "\n"
        + "  roughness .00001\n  metallic\n  phong 0.9\n  phong_size 120\n}}"
        + "\n\n");

    writeMacrosAtom();
    writeMacrosRing();
    //writeMacrosWire();
    //writeMacrosDoubleWire();
    //writeMacrosTripleWire();
    writeMacrosBond();
    writeMacrosJoint();
    //    writeMacrosSingleBond();
    //    writeMacrosDoubleBond();
    //    writeMacrosTripleBond();
    //    writeMacrosHydrogenBond();
    //    writeMacrosAromaticBond();
  }

  private void writeMacrosAtom() {
    output.append("#macro atom(X,Y,Z,RADIUS,R,G,B,T)\n"
        + " sphere{<X,Y,Z>,RADIUS\n" + "  pigment{rgbt<R,G,B,T>}\n"
        + "  no_shadow}\n" + "#end\n\n");
  }

  private void writeMacrosRing() {
    // This type of ring does not take into account perspective effects!
    output
        .append("#macro ring(X,Y,Z,RADIUS,R,G,B,T)\n"
            + " torus{RADIUS,wireRadius pigment{rgbt<R,G,B,T>}\n"
            + " translate<X,Z,-Y> rotate<90,0,0>\n" + "  no_shadow}\n"
            + "#end\n\n");
  }

  private void writeMacrosBond() {
    // We always use cones here, in orthographic mode this will give us
    //  cones with two equal radii, in perspective mode Jmol will calculate
    //  the cone radii for us.
    output.append("#macro bond(X1,Y1,Z1,RADIUS1,X2,Y2,Z2,RADIUS2,R,G,B,T)\n"
        + " cone{<X1,Y1,Z1>,RADIUS1,<X2,Y2,Z2>,RADIUS2\n"
        + "  pigment{rgbt<R,G,B,T>}\n" + "  no_shadow}\n" + "#end\n\n");
  }

  private void writeMacrosJoint() {
    output.append("#macro joint(X,Y,Z,RADIUS,R,G,B,T)\n"
        + " sphere{<X,Y,Z>,RADIUS\n" + "  pigment{rgbt<R,G,B,T>}\n"
        + "  no_shadow}\n" + "#end\n\n");
  }
  
  public void fillSphereCentered(int mad, Point3f pt, short colix) {
    //not a mad -- a number of pixels?
    //TODO
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, short[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int nPoints) {
  }

  public void renderText(Text t) {
  }  
  
  public void drawString(short colix, String str, Font3D font3d, 
                         int xBaseline, int yBaseline, int z, int zSlab) {
  }

  public void fillCylinder(short colix, byte endcaps, int diameter, 
                           Point3i screenA, Point3i screenB) {
  }

  public void drawDottedLine(short colix, Point3i pointA, Point3i pointB) {
    //axes
  }

  public void drawPoints(short colix, int count, int[] coordinates) {
    //dots
  }

  public void drawLine(short colix, Point3i pointA, Point3i pointB) {
    //stars
  }
  
  public void fillScreenedCircleCentered(short colix, int diameter, int x,
                                         int y, int z) {
   //halos 
  }

  public void drawPixel(short colix, int x, int y, int z) {
    //measures
   // System.out.println("pov drawPixel "+this);
  }

  public void drawDashedLine(short colix, int run, int rise, Point3i ptA, Point3i ptB) {
    //measures
    //System.out.println("pov drawDashedLine == measures "+this);
  }

  public void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    //cartoons
    System.out.println("pov fillTriangle - cartoons "+this);
  }

  public void fillQuadrilateral(short colix, Point3f ptA, Point3f ptB, Point3f ptC, Point3f ptD) {
    //rockets
    //System.out.println("pov fillQuadrilateral -- rockets "+this);
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    //rockets
    //System.out.println("pov fillCone rockets "+this);
  }
  
  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3){
    //cartoons, rockets, trace:
    //System.out.println("pov fileHermite cartoons rockets trace "+this);
  }
  
  public void drawHermite(short colix, int tension,
                             Point3i s0, Point3i s1, Point3i s2, Point3i s3){
    //strands, meshribbon:
    //System.out.println("pov drawhermite "+this);
  }

  public void drawHermite(short colix, boolean fill, boolean border, int tension,
                            Point3i s0, Point3i s1, Point3i s2, Point3i s3,
                            Point3i s4, Point3i s5, Point3i s6, Point3i s7,
                            int aspectRatio) {
    //cartoons, meshRibbons:
    //System.out.println("pov draw hermite -- cartoons, meshribbons "+this);
  }
           
          
  public void fillSphereCentered(short colix, int diameter, Point3i pt) {
    //rockets:    
System.out.println("pov fillspherecentered -- rockets"+this);
}


}
