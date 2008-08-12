/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-07 20:10:15 -0500 (Sun, 07 Oct 2007) $
 * $Revision: 8384 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import java.awt.Image;
import java.util.BitSet;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.JmolExportInterface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.g3d.Hermite3D;
import org.jmol.shape.ShapeRenderer;

/**
 * Provides high-level graphics primitives for 3D graphics export.
 *
 * @author hansonr, hansonr@stolaf.edu
 * 
 */

final public class Export3D implements JmolRendererInterface {

  
  private Graphics3D g3d;
  private short colix;
  private Hermite3D hermite3d;
  private int width;
  private int height;
  private int slab;

//  private ShapeRenderer shapeRenderer;
  private JmolExportInterface exporter;
  public JmolExportInterface getExporter() {
    return exporter;
  }

  public Export3D() {
    this.hermite3d = new Hermite3D(this);
  }
  
  public void setg3dExporter(Graphics3D g3d, JmolExportInterface exporter) {
    this.g3d = g3d;
    width = g3d.getRenderWidth();
    height = g3d.getRenderHeight();
    slab = g3d.getSlab();

    this.exporter = exporter;
    exporter.setRenderer(this);
  }
  
  public void setSlab(int slabValue) {
    slab = slabValue;
    g3d.setSlab(slabValue);
  }
  
  public void setRenderer(ShapeRenderer shapeRenderer) {
 //   this.shapeRenderer = shapeRenderer;
  }
  
  public void renderBackground() {
    exporter.renderBackground();
  }

  /**
   * draws a screened circle ... every other dot is turned on
   *
   * @param colixFill the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   */
  
  public void fillScreenedCircleCentered(short colixFill, int diameter, int x,
                                           int y, int z) {
    //halos, draw
    if (isClippedZ(z))
      return;
    exporter.fillScreenedCircleCentered(colixFill, diameter, x, y, z);
  }

  /**
   * draws a simple circle (draw circle)
   *
   * @param colix the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   * @param doFill (not implemented in exporters)
   */
  
  public void drawCircleCentered(short colix, int diameter, int x,
                                           int y, int z, boolean doFill) {
    //halos, draw
    if (isClippedZ(z))
      return;
    exporter.drawCircleCentered(colix, diameter, x, y, z, doFill);
  }

  private Point3f ptA = new Point3f();
  private Point3f ptB = new Point3f();
  private Point3f ptC = new Point3f();
  private Point3f ptD = new Point3f();
  /*
  private Point3f ptE = new Point3f();
  private Point3f ptF = new Point3f();
  private Point3f ptG = new Point3f();
  private Point3f ptH = new Point3f();
*/
  private Point3i ptAi = new Point3i();
  private Point3i ptBi = new Point3i();
  
  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void fillSphereCentered(int diameter, int x, int y, int z) {
    ptA.set(x, y, z);
    fillSphereCentered(diameter, ptA);
  }

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center javax.vecmath.Point3i defining the center
   */

  public void fillSphereCentered(int diameter, Point3i center) {
    ptA.set(center.x, center.y, center.z);
    fillSphereCentered(diameter, ptA);
  }

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center a javax.vecmath.Point3f ... floats are casted to ints
   */
  public void fillSphereCentered(int diameter, Point3f center) {
    if (diameter == 0)
      return;
    exporter.fillSphereCentered(colix, diameter, center);
  }

  /**
   * draws a rectangle
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab z for slab check (for set labelsFront)
   * @param rWidth pixel count
   * @param rHeight pixel count
   */
  public void drawRect(int x, int y, int z, int zSlab, int rWidth, int rHeight) {
    // labels (and rubberband, not implemented) and navigation cursor
    if (zSlab != 0 && isClippedZ(zSlab))
      return;
    int w = rWidth - 1;
    int h = rHeight - 1;
    int xRight = x + w;
    int yBottom = y + h;
    if (y >= 0 && y < height)
      drawHLine(x, y, z, w);
    if (yBottom >= 0 && yBottom < height)
      drawHLine(x, yBottom, z, w);
    if (x >= 0 && x < width)
      drawVLine(x, y, z, h);
    if (xRight >= 0 && xRight < width)
      drawVLine(xRight, y, z, h);
  }

  private void drawHLine(int x, int y, int z, int w) {
    // hover, labels only
    int argbCurrent = g3d.getColixArgb(colix);
    if (w < 0) {
      x += w;
      w = -w;
    }
    for (int i = 0; i <= w; i++) {
      exporter.drawTextPixel(argbCurrent, x + i, y, z);
    }
  }

  private void drawVLine(int x, int y, int z, int h) {
    // hover, labels only
    int argbCurrent = g3d.getColixArgb(colix);
    if (h < 0) {
      y += h;
      h = -h;
    }
    for (int i = 0; i <= h; i++) {
      exporter.drawTextPixel(argbCurrent, x, y + i, z);
    }
  }

  /**
   * fills background rectangle for label
   *<p>
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab  z value for slabbing
   * @param widthFill pixel count
   * @param heightFill pixel count
   */
  public void fillRect(int x, int y, int z, int zSlab, int widthFill, int heightFill) {
    // hover and labels only -- slab at atom or front -- simple Z/window clip
    if (isClippedZ(zSlab))
      return;
    ptA.set(x, y, z);
    ptB.set(x + widthFill, y, z);
    ptC.set(x + widthFill, y + heightFill, z);
    ptD.set(x, y + heightFill, z);
    fillQuadrilateral(ptA, ptB, ptC, ptD);
  }
  
  /**
   * draws the specified string in the current font.
   * no line wrapping -- axis, labels, measures
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   * @param zSlab z for slab calculation
   */
  
  public void drawString(String str, Font3D font3d,
                         int xBaseline, int yBaseline, int z, int zSlab) {
    //axis, labels, measures    
    if (str == null)
      return;
    if (isClippedZ(zSlab))
      return;
    drawStringNoSlab(str, font3d, xBaseline, yBaseline, z); 
  }

  /**
   * draws the specified string in the current font.
   * no line wrapping -- echo, frank, hover, molecularOrbital, uccage
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */
  
  public void drawStringNoSlab(String str, Font3D font3d, 
                               int xBaseline, int yBaseline,
                               int z) {
    // echo, frank, hover, molecularOrbital, uccage
    if (str == null)
      return;
    z = Math.max(slab, z);
    if(font3d == null)
      font3d = g3d.getFont3DCurrent();
    else
      g3d.setFont(font3d);
    exporter.plotText(xBaseline, yBaseline,
                z, getColixArgb(colix), str, font3d);
  }
  
  public void drawImage(Image image, int x, int y, int z, int zSlab, 
                        short bgcolix, int width, int height) {
    if (image == null || width == 0 || height == 0)
      return;
    if (isClippedZ(zSlab))
      return;
    z = Math.max(slab, z);
    exporter.plotImage(x, y, z, image, bgcolix, width, height);
  }

  //mostly public drawing methods -- add "public" if you need to

  /* ***************************************************************
   * points
   * ***************************************************************/

  
  public void drawPixel(int x, int y, int z) {
    // measures - render angle
    plotPixelClipped(x, y, z);
  }

  void plotPixelClipped(int x, int y, int z) {
    //circle3D, drawPixel, plotPixelClipped(point3)
    if (g3d.isClipped(x, y, z))
      return;
    exporter.drawPixel(colix, x, y, z);
  }

  public void plotPixelClippedNoSlab(int argb, int x, int y, int z) {
    //from Text3D
    z = Math.max(slab, z);
    exporter.drawTextPixel(argb, x, y, z);
  }

  public void plotPixelClipped(Point3i screen) {
    if (g3d.isClipped(screen.x, screen.y, screen.z))
      return;
    //circle3D, drawPixel, plotPixelClipped(point3)
    exporter.drawPixel(colix, screen.x, screen.y, screen.z);
  }
  
  public void drawPoints(int count, int[] coordinates) {
    for (int i = count * 3; i > 0; ) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (g3d.isClipped(x, y, z))
        continue;
      exporter.drawPixel(colix, x, y, z);
    }
  }

  /* ***************************************************************
   * lines and cylinders
   * ***************************************************************/

  public void drawDashedLine(int run, int rise, Point3i pointA, Point3i pointB) {
    //TODO
    drawLine(pointA, pointB); //Temporary only
    //ptA.set(pointA.x, pointA.y, pointA.z);
    //ptB.set(pointB.x, pointB.y, pointB.z);
    //exporter.drawDashedLine(colix, run, rise, ptA, ptB);
  }

  public void drawDottedLine(Point3i pointA, Point3i pointB) {
    //TODO
     //axes, bbcage only
    drawLine(pointA, pointB); //Temporary only
    //ptA.set(pointA.x, pointA.y, pointA.z);
    //ptB.set(pointB.x, pointB.y, pointB.z);
    //exporter.drawDashedLine(colix, 2, 1, ptA, ptB);
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    // stars
    ptAi.set(x1, y1, z1);
    ptBi.set(x2, y2, z2);
    drawLine(ptAi, ptBi);
  }

  public void drawLine(short colixA, short colixB, int xA, int yA, int zA, int xB, int yB, int zB) {
    //mads[i] < 0 // mesh
    fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_FLAT, 2, xA, yA, zA, xB, yB, zB);
  }

  public void drawLine(Point3i pointA, Point3i pointB) {
    // draw quadrilateral and hermite, stars
    ptA.set(pointA.x, pointA.y, pointA.z);
    ptB.set(pointB.x, pointB.y, pointB.z);
    exporter.fillCylinder(colix, Graphics3D.ENDCAPS_FLAT, 1, ptA, ptB);
  }
  
  public void fillCylinder(short colixA, short colixB, byte endcaps,
                              int mad, int xA, int yA, int zA, int xB, int yB, int zB) {
    /*
     * Use the screen points Jmol determines
     *  
     */
    ptA.set(xA, yA, zA);
    ptB.set(xB, yB, zB);
    exporter.fillCylinder(ptA, ptB, colixA, colixB, endcaps, mad, 1);
  }

  public void fillCylinder(byte endcaps,
                           int mad,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    //vectors
    fillCylinder(colix, colix, endcaps, mad, xA, yA, zA, xB, yB, zB);
  }

  public void fillCylinder(byte endcaps, int diameter, Point3i pointA,
                              Point3i pointB) {
    if (diameter <= 0)
      return;
    ptA.set(pointA.x, pointA.y, pointA.z);
    ptB.set(pointB.x, pointB.y, pointB.z);
    exporter.fillCylinder(colix, endcaps, diameter, ptA, ptB);
  }

  public void fillCylinderBits(byte endcaps, int diameter, Point3f pointA,
                                  Point3f pointB) {
    if (diameter <= 0)
      return;
    exporter.fillCylinder(colix, endcaps, diameter, pointA, pointB);
  }


  public void fillCone(byte endcap, int diameter,
                          Point3i pointBase, Point3i screenTip) {
    ptA.set(pointBase.x, pointBase.y, pointBase.z);
    ptB.set(screenTip.x, screenTip.y, screenTip.z);
    fillCone(endcap, diameter, ptA, ptB);
  }

  public void fillCone(byte endcap, int diameter,
                       Point3f pointBase, Point3f screenTip) {
    // cartoons, rockets
    exporter.fillCone(colix, endcap, diameter, pointBase, screenTip);
  }

  public void drawHermite(int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    //strands
    hermite3d.render(false, tension, 0, 0, 0, s0, s1, s2, s3);
    //int mad = 20; //could be off
    //fillHermite(tension, mad, mad, mad, s0, s1, s2, s3);
  }

  public void drawHermite(boolean fill, boolean border,
                          int tension, Point3i s0, Point3i s1, Point3i s2,
                          Point3i s3, Point3i s4, Point3i s5, Point3i s6,
                          Point3i s7, int aspectRatio) {
    hermite3d.render2(fill, border, tension, s0, s1, s2, s3, s4, s5, s6,
        s7, aspectRatio);
/*
    ptA.set(s0.x, s0.y, s0.z);
    ptB.set(s1.x, s1.y, s1.z);
    ptC.set(s2.x, s2.y, s2.z);
    ptD.set(s3.x, s3.y, s3.z);
    ptE.set(s4.x, s4.y, s4.z);
    ptF.set(s5.x, s5.y, s5.z);
    ptG.set(s6.x, s6.y, s6.z);
    ptH.set(s7.x, s7.y, s7.z);
    exporter.drawHermite(colix, fill, border, tension, ptA, ptB, ptC,
        ptD, ptE, ptF, ptG, ptH, aspectRatio);
        */
  }

  public void fillHermite(int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    /*
    ptA.set(s0.x, s0.y, s0.z);
    ptB.set(s1.x, s1.y, s1.z);
    ptC.set(s2.x, s2.y, s2.z);
    ptD.set(s3.x, s3.y, s3.z);
    exporter.fillHermite(colix, tension, madBeg, madMid, madEnd,
        ptA, ptB, ptC, ptD);
        */
    hermite3d.render(true, tension,
        diameterBeg, diameterMid, diameterEnd,
        s0, s1, s2, s3);
  }
  
  /* ***************************************************************
   * triangles
   * ***************************************************************/

  public void drawTriangle(Point3i pointA, short colixA, Point3i pointB,
                           short colixB, Point3i pointC, short colixC, int check) {
    // primary method for mapped Mesh
    int xA = pointA.x;
    int yA = pointA.y;
    int zA = pointA.z;
    int xB = pointB.x;
    int yB = pointB.y;
    int zB = pointB.z;
    int xC = pointC.x;
    int yC = pointC.y;
    int zC = pointC.z;
    if ((check & 1) == 1)
      drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    if ((check & 2) == 2)
      drawLine(colixB, colixC, xB, yB, zB, xC, yC, zC);
    if ((check & 4) == 4)
      drawLine(colixA, colixC, xA, yA, zA, xC, yC, zC);
  }

  public void drawTriangle(Point3i pointA, Point3i pointB,
                           Point3i pointC, int check) {
/*    // primary method for unmapped monochromatic Mesh
    int xA = pointA.x;
    int yA = pointA.y;
    int zA = pointA.z;
    int xB = pointB.x;
    int yB = pointB.y;
    int zB = pointB.z;
    int xC = pointC.x;
    int yC = pointC.y;
    int zC = pointC.z;
    if ((check & 1) == 1)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xA, yA, zA, xB, yB, zB, false);
    if ((check & 2) == 2)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xB, yB, zB, xC, yC, zC, false);
    if ((check & 4) == 4)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xA, yA, zA, xC, yC, zC, false);
*/  }

  public void drawCylinderTriangle(int xA, int yA, int zA, int xB,
                                   int yB, int zB, int xC, int yC, int zC,
                                   int diameter) {
    // polyhedra
    fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, xA, yA,
        zA, xB, yB, zB);
    fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, xA, yA,
        zA, xC, yC, zC);
    fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, xB, yB,
        zB, xC, yC, zC);
  }

  public void drawfillTriangle(int xA, int yA, int zA, int xB,
                               int yB, int zB, int xC, int yC, int zC) {
/*    // sticks -- sterochemical wedge notation -- not implemented?
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xA,
        yA, zA, xB, yB, zB, false);
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xA,
        yA, zA, xC, yC, zC, false);
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xB,
        yB, zB, xC, yC, zC, false);
        */
    ptA.set(xA, yA, zA);
    ptB.set(xB, yB, zB);
    ptC.set(xC, yC, zC);
    fillTriangle(ptA, ptB, ptC);
  }
  
  public void fillTriangle(Point3i pointA, int intensityA,
                           Point3i pointB, int intensityB,
                           Point3i pointC, int intensityC) {
    ptA.set(pointA.x, pointA.y, pointA.z);
    ptB.set(pointB.x, pointB.y, pointB.z);
    ptC.set(pointC.x, pointC.y, pointC.z);
    exporter.fillTriangle(colix, ptA, ptB, ptC);
  }
  
  public void fillTriangle(Point3i pointA, short colixA, short normixA,
                           Point3i pointB, short colixB, short normixB,
                           Point3i pointC, short colixC, short normixC) {
    // mesh, isosurface
    if (colixA != colixB || colixB != colixC) {
      //shouldn't be here, because that uses renderIsosurface
      return;
    }
    ptA.set(pointA.x, pointA.y, pointA.z);
    ptB.set(pointB.x, pointB.y, pointB.z);
    ptC.set(pointC.x, pointC.y, pointC.z);
    exporter.fillTriangle(colixA, ptA, ptB, ptC);
  }

  public void fillTriangle(short normix,
                           int xpointA, int ypointA, int zpointA,
                           int xpointB, int ypointB, int zpointB,
                           int xpointC, int ypointC, int zpointC) {
    // polyhedra
    ptA.set(xpointA, ypointA, zpointA);
    ptB.set(xpointB, ypointB, zpointB);
    ptC.set(xpointC, ypointC, zpointC);    
    exporter.fillTriangle(colix, ptA, ptB, ptC);
  }

  public void fillTriangle(Point3f pointA, Point3f pointB, Point3f pointC) {
    // rockets
    exporter.fillTriangle(colix, pointA, pointB, pointC);
  }

  public void fillTriangle(Point3i pointA, Point3i pointB, Point3i pointC) {
    // cartoon, hermite

    ptA.set(pointA.x, pointA.y, pointA.z);
    ptB.set(pointB.x, pointB.y, pointB.z);
    ptC.set(pointC.x, pointC.y, pointC.z);
    exporter.fillTriangle(colix, ptA, ptB, ptC);
  }

  public void fillTriangle(Point3i pointA, short colixA,
                                   short normixA, Point3i pointB,
                                   short colixB, short normixB,
                                   Point3i pointC, short colixC,
                                   short normixC, float factor) {
    fillTriangle(pointA, colixA, normixA, 
        pointB, colixB, normixB, pointC, colixC, normixC);
  }

  /* ***************************************************************
   * quadrilaterals
   * ***************************************************************/
  
  public void drawQuadrilateral(short colix, Point3i pointA, Point3i pointB,
                                Point3i pointC, Point3i screenD) {
    //mesh only -- translucency has been checked
    setColix(colix);
    drawLine(pointA, pointB);
    drawLine(pointB, pointC);
    drawLine(pointC, screenD);
    drawLine(screenD, pointA);
  }

  public void fillQuadrilateral(Point3f pointA, Point3f pointB,
                                Point3f pointC, Point3f pointD) {
    // hermite, rockets, cartoons
    exporter.fillTriangle(colix, pointA, pointB, pointC);
    exporter.fillTriangle(colix, pointA, pointC, pointD);
  }

  public void fillQuadrilateral(Point3i pointA, short colixA, short normixA,
                                Point3i pointB, short colixB, short normixB,
                                Point3i pointC, short colixC, short normixC,
                                Point3i screenD, short colixD, short normixD) {
    // mesh
    fillTriangle(pointA, colixA, normixA,
                 pointB, colixB, normixB,
                 pointC, colixC, normixC);
    fillTriangle(pointA, colixA, normixA,
                 pointC, colixC, normixC,
                 screenD, colixD, normixD);
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                                        short[] colixes, Vector3f[] normals,
                                        int[][] indices, BitSet bsFaces, int nVertices,
                                        int faceVertexMax) {
    exporter.renderIsosurface(vertices, colix, colixes, normals,
                              indices, bsFaces, nVertices, faceVertexMax);
  }

  public void renderEllipsoid(int x, int y, int z, int diameter,
                              Matrix3f mToEllipsoidal, double[] coef,
                              Matrix4f mDeriv, int selectedOctant,
                              Point3i[] octantPoints) {
    exporter.renderEllipsoid(colix, x, y, z, diameter, coef, octantPoints);
  }

  /* ***************************************************************
   * g3d-relayed info specifically needed for the renderers
   * ***************************************************************/

  /**
   * is full scene / oversampling antialiasing in effect
   *
   * @return the answer
   */
  public boolean isAntialiased() {
    return false;
  }
  
  public boolean checkTranslucent(boolean isAlphaTranslucent) {
    return true;
  }
  
  public boolean haveTranslucentObjects() {
    return true;
  }
  
  /**
   * gets g3d width
   *
   * @return width pixel count;
   */
  public int getRenderWidth() {
    return g3d.getRenderWidth();
  }

  /**
   * gets g3d height
   *
   * @return height pixel count
   */
  public int getRenderHeight() {
    return g3d.getRenderHeight();
  }

  /**
   * gets g3d slab
   *
   * @return slab
   */
  public int getSlab() {
    return g3d.getSlab();
  }

  /**
   * gets g3d depth
   *
   * @return depth
   */
  public int getDepth() {
    return g3d.getDepth();
  }

  /**
   * sets current color from colix color index
   * @param colix the color index
   * @return true or false if this is the right pass
   */
  public boolean setColix(short colix) {
    this.colix = colix;
    g3d.setColix(colix);
    return true;
  }

  public void setFont(byte fid) {
    g3d.setFont(fid);
  }
  
  public Font3D getFont3DCurrent() {
    return g3d.getFont3DCurrent();
  }


  public boolean isInDisplayRange(int x, int y) {
    return g3d.isInDisplayRange(x, y);
  }
  
  public boolean isClippedZ(int z) {
    return g3d.isClippedZ(z);
  }
  
  public boolean isClippedXY(int diameter, int x, int y) {
    return g3d.isClippedXY(diameter, x, y);
  }

    public int getColixArgb(short colix) {
    return g3d.getColixArgb(colix);
  }

  public String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

  public int calcSurfaceShade(Point3i pointA, Point3i pointB, Point3i pointC) {
    return g3d.calcSurfaceShade(pointA, pointB, pointC);
  }

  public byte getFontFid(String fontFace, float fontSize) {
    return g3d.getFontFid(fontFace, fontSize);
  }

  public boolean isDirectedTowardsCamera(short normix) {
    //polyhedra
    return g3d.isDirectedTowardsCamera(normix);
  }

  public short getNormix(Vector3f vector) {
    return g3d.getNormix(vector);
  }
  
  public short getInverseNormix(short normix) {
    return g3d.getInverseNormix(normix);
  }

  public Vector3f[] getTransformedVertexVectors() {
    return g3d.getTransformedVertexVectors();
  }

  public Vector3f getNormixVector(short normix) {
    return g3d.getNormixVector(normix);
  }

  public Font3D getFont3DScaled(Font3D font, float scale) {
    return g3d.getFont3DScaled(font, scale);
  }

  public byte getFontFid(float fontSize) {
    return g3d.getFontFid(fontSize);
  }
}
