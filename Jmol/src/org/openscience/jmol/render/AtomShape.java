/*
 * Copyright 2002 The Jmol Development Team
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

package org.openscience.jmol.render;

import org.openscience.jmol.*;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.Kernel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Float;
import java.awt.RenderingHints;
import java.util.Enumeration;
import java.util.Hashtable;

import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;

/**
 * Graphical representation of an atom.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomShape implements Shape {

  Atom atom;
  
  AtomShape(Atom atom) {
    this.atom = atom;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Atom shape for ");
    buffer.append(atom);
    buffer.append(": z = ");
    buffer.append(getZ());
    return buffer.toString();
  }

  public void render(Graphics g) {
    renderBonds();
    renderAtom();
    renderLabel();
  }
  
  public void renderBonds() {
    if (!showBonds || (!showHydrogens && atom.isHydrogen()))
      return;
    Enumeration bondIter = atom.getBondedAtoms();
    while (bondIter.hasMoreElements()) {
      Atom otherAtom = (Atom) bondIter.nextElement();
      int z = atom.screenZ;
      int zOther = otherAtom.screenZ;
      if ((showHydrogens || !otherAtom.isHydrogen()) &&
          (z > zOther) ||
          ((z==zOther) && (atom.getAtomNumber() > otherAtom.getAtomNumber())))
        renderBond(g, atom, otherAtom);
    }
  }
  
  public int getZ() {
    return atom.screenZ;
  }
  
  private static AtomRenderer atomRenderer;
  private static boolean showAtoms;
  private static boolean showHydrogens;
  private static boolean showBonds;
  private static boolean wireframeRotation;
  private static boolean drawBondsToAtomCenters;
  private static int bondDrawMode;
  private static int atomDrawMode;
  private static int labelMode;
  private static boolean mouseDragged;
  private static DisplayControl control;
  private static float halfBondWidth;
  private static boolean showDarkerOutline;
  private static Color outlineColor;
  private static Color pickedColor;
  private static Color backgroundColor;
  private static Graphics g;
  private static ColorProfile colorProfile;

  public static void prepareRendering(Graphics gc, Rectangle rectClip,
                                      DisplayControl ctrl) {
    g = gc;
    control = ctrl;
    atomDrawMode = control.getAtomDrawMode();
    bondDrawMode = control.getBondDrawMode();
    showAtoms = control.getShowAtoms();
    showHydrogens = control.getShowHydrogens();
    showBonds = control.getShowBonds();
    labelMode = control.getLabelMode();
    wireframeRotation = control.getFastRendering();
    drawBondsToAtomCenters = control.getDrawBondsToAtomCenters();
    halfBondWidth = control.scaleToScreen(0, 0.5f * control.getBondWidth());
    showDarkerOutline = control.getShowDarkerOutline();
    outlineColor = control.getOutlineColor();
    pickedColor = control.getPickedColor();
    backgroundColor = control.getBackgroundColor();
    mouseDragged = control.isMouseDragged();
    if (control.getAtomColorProfile() == DisplayControl.ATOMCHARGE) {
        colorProfile = new ChargeColorProfile();
    } else {
        colorProfile = new DefaultColorProfile();
    }
  }

  //private static BondRenderer shadingBondRenderer=new ShadingBondRenderer();

  public void renderLabel() {
    if (labelMode == DisplayControl.NOLABELS)
      return;
    int x = atom.screenX;
    int y = atom.screenY;
    int z = atom.screenZ;
    int diameter = atom.screenDiameter;
    int radius = diameter >> 1;

    int j = 0;
    String s = null;
    Font font = new Font("Helvetica", Font.PLAIN, radius);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    int k = fontMetrics.getAscent();
    g.setColor(control.getTextColor());
    
    String label = null;
    switch (labelMode) {
    case DisplayControl.SYMBOLS:
      if (atom.getType() != null) {
        label = atom.getSymbol();
      }
      break;

    case DisplayControl.TYPES:
      if (atom.getType() != null) {
        label = atom.getType().getName();
      }
      break;

    case DisplayControl.NUMBERS:
      label = Integer.toString(atom.getAtomNumber() + 1);
      break;

    }
    if (label != null) {
      j = fontMetrics.stringWidth(label);
      g.drawString(label, x - j / 2, y + k / 2);
    }
    if (!control.getPropertyMode().equals("")) {

      // check to make sure this atom has this property:
      Enumeration propIter = atom.getProperties().elements();
      while (propIter.hasMoreElements()) {
        PhysicalProperty p = (PhysicalProperty) propIter.nextElement();
        if (p.getDescriptor().equals(control.getPropertyMode())) {
        
          // OK, we had this property.  Let's draw the value on
          // screen:
          font = new Font("Helvetica", Font.PLAIN, radius / 2);
          g.setFont(font);
          g.setColor(control.getTextColor());
          s = p.stringValue();
          if (s.length() > 5) {
            s = s.substring(0, 5);
          }
          k = 2 + (int) (radius / 1.4142136f);
          g.drawString(s, x + k, y - k);
        }
      }
    }
  }

  // When this variable is set then a wireframe atom will behave
  // as though it is translucent. This allows you to see that the bonds
  // are being clipped when they are obscured by the atom
  private static final boolean showCoveredBonds = false;

  public void renderBond(Graphics g, Atom atom1, Atom atom2) {
    Color color1 = colorProfile.getColor(atom1);
    Color color2 = colorProfile.getColor(atom2);
    int x1 = atom1.screenX, y1 = atom1.screenY;
    int x2 = atom2.screenX, y2 = atom2.screenY;
    int dx = x2 - x1, dx2 = dx * dx;
    int dy = y2 - y1, dy2 = dy * dy;
    int magnitude2 = dx2 + dy2;
    if ((magnitude2 <= 2) || (wireframeRotation && magnitude2 <= 49))
      return; // also avoid divide by zero when magnitude == 0
    if (showAtoms &&
        (atomDrawMode != DisplayControl.WIREFRAME) &&
        (magnitude2 <= 16))
      return; // the pixels from the atoms will nearly cover the bond
    // technically, we should draw a bond (actually little more than a dot)
    // when:
    //  atomDrawMode == DisplayControl.WIREFRAME
    //  && showCoveredBonds == true
    //  && and the centers of the bonds are very close
    //  && the diameter of the atom1 is >= 3
    // ... but I'm not going to do it right now
    if ( //fastRendering || // fastRendering isn't any faster this way
        ((bondDrawMode == DisplayControl.LINE) && drawBondsToAtomCenters)) {
      if (color1.equals(color2)) {
        g.setColor(color1);
        g.drawLine(x1, y1, x2, y2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        g.setColor(color2);
        g.drawLine(xMid, yMid, x2, y2);
        g.setColor(color1);
        g.drawLine(x1, y1, xMid, yMid);
      }
      return;
    }
    int z1 = atom1.screenZ;
    int z2 = atom2.screenZ;
    int dz = z2 - z1;
    int dz2 = dz * dz;
    int diameter1, radius1, diameter2, radius2;
    if (drawBondsToAtomCenters) {
      diameter1 = radius1 = diameter2 = radius2 = 0;
    } else {
      diameter1 = atom1.screenDiameter;
      radius1 = diameter1 >> 1;
      diameter2 = atom2.screenDiameter;
      radius2 = diameter2 >> 1;
    }

    int magnitude = (int) Math.sqrt(magnitude2);
    int bondOrder = Bond.getBondOrder(atom1, atom2);
    double cosine = magnitude / Math.sqrt(magnitude2 + dz2);
    int radius1Bond = (int)(radius1 * cosine);
    int radius2Bond = (int)(radius2 * cosine);
    if (((atomDrawMode != DisplayControl.WIREFRAME) || !showCoveredBonds) &&
        (magnitude < radius1 + radius2Bond)) {
      // the shapes are solid and the front atom (radius1) has
      // completely obscured the bond
      return;
    }

    int arcFactor = 1;
    int x1Bond = x1 + ((radius1Bond - arcFactor) * dx) / magnitude;
    int y1Bond = y1 + ((radius1Bond - arcFactor) * dy) / magnitude;
    int x2Bond = x2 - ((radius2Bond - arcFactor) * dx) / magnitude;
    int y2Bond = y2 - ((radius2Bond - arcFactor) * dy) / magnitude;
    int x1Edge;
    int y1Edge;
    if ((atomDrawMode == DisplayControl.WIREFRAME) && showCoveredBonds) {
      x1Edge = x1Bond;
      y1Edge = y1Bond;
    } else {
      x1Edge = x1 + ((radius1 - arcFactor) * dx) / magnitude;
      y1Edge = y1 + ((radius1 - arcFactor) * dy) / magnitude;
    }

    if (wireframeRotation ||
        (bondDrawMode == DisplayControl.LINE) ||
        (halfBondWidth < .75f)) {
      drawLineBond(g,
                   x1Bond, y1Bond, color1,
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2,
                   dx, dy, magnitude, bondOrder, halfBondWidth);
      return;
    }
    if (bondDrawMode != DisplayControl.SHADING) {
      drawRectBond(g,
                   x1Bond, y1Bond, color1,
                   (halfBondWidth < 1.5f) ? null : getOutline(color1),
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2,
                   (halfBondWidth < 1.5f) ? null : getOutline(color2),
                   bondDrawMode != DisplayControl.WIREFRAME,
                   dx, dy, magnitude, bondOrder, halfBondWidth, halfBondWidth);
      return;
    }
    // drawing shaded bonds
    if (mouseDragged || (int)halfBondWidth < 2) {
      drawRectBond(g,
                   x1Bond, y1Bond, color1, getDarker(color1),
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2, getDarker(color2),
                   true,
                   dx, dy, magnitude, bondOrder, halfBondWidth, halfBondWidth);
      return;
    }
    Color darker1 = getDarker(color1), outline1 = darker1;
    Color bright1 = color1;
    Color darker2 = getDarker(color2), outline2 = darker2;
    Color bright2 = color2;

    int atom1R = darker1.getRed(),   range1R = bright1.getRed() - atom1R;
    int atom1G = darker1.getGreen(), range1G = bright1.getGreen() - atom1G;
    int atom1B = darker1.getBlue(),  range1B = bright1.getBlue() - atom1B;
    int atom2R = darker2.getRed(),   range2R = bright2.getRed() - atom2R;
    int atom2G = darker2.getGreen(), range2G = bright2.getGreen() - atom2G;
    int atom2B = darker2.getBlue(),  range2B = bright2.getBlue() - atom2B;

    int numPasses = (int)halfBondWidth;
    float widthT = halfBondWidth;
    for (int i = 0; i < numPasses; ++i, widthT -= 1.0) {
      // numPasses must be > 1 because of test above
      float pct = (float) i / (numPasses - 1);
      int r1 = atom1R + (int)(pct * range1R);
      int g1 = atom1G + (int)(pct * range1G);
      int b1 = atom1B + (int)(pct * range1B);
      int r2 = atom2R + (int)(pct * range2R);
      int g2 = atom2G + (int)(pct * range2G);
      int b2 = atom2B + (int)(pct * range2B);

      // Bitwise masking to make color model:
      int model1 = 0xFF << 24 | r1 << 16 | g1 << 8 | b1;
      Color co1 = new Color(model1);
      int model2 = 0xFF << 24 | r2 << 16 | g2 << 8 | b2;
      Color co2 = new Color(model2);

      drawRectBond(g,
                   x1Bond, y1Bond, co1, outline1,
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, co2, outline2,
                   true, dx, dy, magnitude, bondOrder,
                   widthT, halfBondWidth);
      // only draw the outline the first time around
      outline1 = outline2 = null;
    }
  }

  private boolean isVisible(int x1, int y1, int xEdge, int yEdge,
                            int x2, int y2) {
    int dxEdge = xEdge - x1, dyEdge = yEdge - y1;
    int dx2 = x2 - x1, dy2 = y2 - y1;
    return (dx2*dx2 + dy2*dy2) > (dxEdge*dxEdge + dyEdge*dyEdge);
  }

  private static final int separationIncrement = 4;

  private void drawLineBond(final Graphics g,
                            int x1, int y1, final Color color1,
                            int xEdge, int yEdge,
                            int x2, int y2, final Color color2,
                            final int dx, final int dy, final int magnitude,
                            int bondOrder, final float halfBondWidth) {
    if (! isVisible(x1, y1, xEdge, yEdge, x2, y2))
      return;
    int sepUp = (int) (separationIncrement * halfBondWidth);
    int sepDn = sepUp - (int)((separationIncrement * 2) * halfBondWidth);
    int xOffset = (sepDn * dy) / magnitude;
    int yOffset = (sepUp * dx) / magnitude;
    if (bondOrder == 2) {
      int xHalfOffset = xOffset/2, yHalfOffset = yOffset/2;
      x1 -= xHalfOffset; y1 -= yHalfOffset;
      x2 -= xHalfOffset; y2 -= yHalfOffset;
      xEdge -= xHalfOffset; yEdge -= yHalfOffset;
    } else if (bondOrder == 3) {
      x1 -= xOffset; y1 -= yOffset;
      x2 -= xOffset; y2 -= yOffset;
      xEdge -= xOffset; yEdge -= yOffset;
    } else if (bondOrder > 3) {
      bondOrder = 3; // just for protection against a wild parameter value
    }
    while (true) {
      int xTemp, yTemp;
      int xMid = (x1 + x2) / 2;
      int yMid = (y1 + y2) / 2;
      if (color1.equals(color2) || 
          !isVisible(x1, y1, xEdge, yEdge, xMid, yMid)) {
        xTemp = xEdge; yTemp = yEdge;
      } else {
        g.setColor(color1);
        g.drawLine(xEdge, yEdge, xMid, yMid);
        xTemp = xMid; yTemp = yMid;
      }
      g.setColor(color2);
      g.drawLine(xTemp, yTemp, x2, y2);      
      if (--bondOrder <= 0) // also catch initial parameter values <= 0
        return;
      x1 += xOffset; y1 += yOffset;
      x2 += xOffset; y2 += yOffset;
      xEdge += xOffset; yEdge += yOffset;
    }
  }

  private static final int[] xBondRectPoints = new int[4];
  private static final int[] yBondRectPoints = new int[4];

  private void drawRectBond(final Graphics g,
                            final int x1, final int y1,
                            final Color color1, final Color color1Outline,
                            final int xEdge, final int yEdge,
                            final int x2, final int y2,
                            final Color color2, final Color color2Outline,
                            final boolean boolFill,
                            final int dx, final int dy, final int magnitude,
                            int bondOrder, final float halfBondWidth,
                            final float separationWidth) {
    if (! isVisible(x1, y1, xEdge, yEdge, x2, y2))
      return;
    // offsets for the width of the bond rectangle
    int xHalfWidth = (int)(halfBondWidth * dy / magnitude);
    int yHalfWidth = (int)(halfBondWidth * dx / magnitude);
    int xFullWidth = (int)(halfBondWidth * 2 * dy / magnitude);
    int yFullWidth = (int)(halfBondWidth * 2 * dx / magnitude);

    int x1Top = x1 + xHalfWidth, x1Bot = x1Top - xFullWidth;
    int y1Top = y1 - yHalfWidth, y1Bot = y1Top + yFullWidth;
    int x2Top = x2 + xHalfWidth, x2Bot = x2Top - xFullWidth;
    int y2Top = y2 - yHalfWidth, y2Bot = y2Top + yFullWidth;
    int xEdgeTop = xEdge + xHalfWidth, xEdgeBot = xEdgeTop - xFullWidth;
    int yEdgeTop = yEdge - yHalfWidth, yEdgeBot = yEdgeTop + yFullWidth;
    int xMidTop = (x1Top + x2Top) / 2, yMidTop = (y1Top + y2Top) / 2;
    int xMidBot = (x1Bot + x2Bot) / 2, yMidBot = (y1Bot + y2Bot) / 2;

    int sepUp = (int) (separationIncrement * separationWidth);
    int sepDn = sepUp - (int)((separationIncrement * 2) * separationWidth);
    int xOffset = (sepDn * dy) / magnitude;
    int yOffset = (sepUp * dx) / magnitude;
    if (bondOrder == 2) {
      int xHalfOffset = xOffset/2;
      x1Top -=    xHalfOffset; x1Bot -=    xHalfOffset;
      x2Top -=    xHalfOffset; x2Bot -=    xHalfOffset;
      xEdgeTop -= xHalfOffset; xEdgeBot -= xHalfOffset;
      xMidTop -=  xHalfOffset; xMidBot -=  xHalfOffset;
      int yHalfOffset = yOffset/2;
      y1Top -=    yHalfOffset; y1Bot -=    yHalfOffset;
      y2Top -=    yHalfOffset; y2Bot -=    yHalfOffset;
      yEdgeTop -= yHalfOffset; yEdgeBot -= yHalfOffset;
      yMidTop -=  yHalfOffset; yMidBot -=  yHalfOffset;
    } else if (bondOrder == 3) {
      x1Top -=    xOffset; x1Bot -=    xOffset;
      x2Top -=    xOffset; x2Bot -=    xOffset;
      xEdgeTop -= xOffset; xEdgeBot -= xOffset;
      xMidTop -=  xOffset; xMidBot -=  xOffset;

      y1Top -=    yOffset; y1Bot -=    yOffset;
      y2Top -=    yOffset; y2Bot -=    yOffset;
      yEdgeTop -= yOffset; yEdgeBot -= yOffset;
      yMidTop -=  yOffset; yMidBot -=  yOffset;
    } else if (bondOrder > 3) {
      bondOrder = 3; // just in case
    }

    while (true) {
      if (color1.equals(color2) ||
          !isVisible(x1Top, y1Top, xEdgeTop, yEdgeTop, xMidTop, yMidTop)) {
        xBondRectPoints[0] = xEdgeTop; yBondRectPoints[0] = yEdgeTop;
        xBondRectPoints[1] = xEdgeBot; yBondRectPoints[1] = yEdgeBot;
      } else { // two different bond colors
        xBondRectPoints[0] = xMidTop; yBondRectPoints[0] = yMidTop;
        xBondRectPoints[1] = xMidBot; yBondRectPoints[1] = yMidBot;
        xBondRectPoints[2] = xEdgeBot; yBondRectPoints[2] = yEdgeBot;
        xBondRectPoints[3] = xEdgeTop; yBondRectPoints[3] = yEdgeTop;
        g.setColor(color1);
        if (boolFill) 
          g.fillPolygon(xBondRectPoints, yBondRectPoints, 4);
        else
          g.drawPolygon(xBondRectPoints, yBondRectPoints, 4);
      }
      xBondRectPoints[2] = x2Bot; yBondRectPoints[2] = y2Bot; 
      xBondRectPoints[3] = x2Top; yBondRectPoints[3] = y2Top;
      g.setColor(color2);
      if (boolFill) 
        g.fillPolygon(xBondRectPoints, yBondRectPoints, 4);
      else
        g.drawPolygon(xBondRectPoints, yBondRectPoints, 4);

      // don't draw outlines if we did not fill
      if (boolFill && color1Outline != null) {
        int xOutlineTop, yOutlineTop, xOutlineBot, yOutlineBot;
        if (color1Outline.equals(color2Outline) ||
            !isVisible(x1Top, y1Top, xEdgeTop, yEdgeTop, xMidTop, yMidTop)) {
          xOutlineTop = xEdgeTop; yOutlineTop = yEdgeTop;
          xOutlineBot = xEdgeBot; yOutlineBot = yEdgeBot;
        } else {
          g.setColor(color1Outline);
          g.drawLine(xEdgeTop, yEdgeTop, xMidTop, yMidTop);
          g.drawLine(xEdgeBot, yEdgeBot, xMidBot, yMidBot);
          xOutlineTop = xMidTop; yOutlineTop = yMidTop;
          xOutlineBot = xMidBot; yOutlineBot = yMidBot;
        }
        g.setColor(color2Outline);
        g.drawLine(xOutlineTop, yOutlineTop, x2Top, y2Top);
        g.drawLine(xOutlineBot, yOutlineBot, x2Bot, y2Bot);
      }
      if (--bondOrder <= 0) // also catch a crazy parameter value
        return;
      x1Top +=    xOffset; x1Bot +=    xOffset;
      x2Top +=    xOffset; x2Bot +=    xOffset;
      xEdgeTop += xOffset; xEdgeBot += xOffset;
      xMidTop +=  xOffset; xMidBot +=  xOffset;

      y1Top +=    yOffset; y1Bot +=    yOffset;
      y2Top +=    yOffset; y2Bot +=    yOffset;
      yEdgeTop += yOffset; yEdgeBot += yOffset;
      yMidTop +=  yOffset; yMidBot +=  yOffset;
    }
  }

  static Hashtable htDarker = new Hashtable();
  private Color getDarker(Color color) {
    Color darker = (Color) htDarker.get(color);
    if (darker == null) {
      darker = color.darker();
      htDarker.put(color, darker);
    }
    return darker;
  }

  private Color getOutline(Color color) {
    return showDarkerOutline ? getDarker(color) : outlineColor;
  }

  private void renderAtom() {
    if (!showAtoms || (!showHydrogens && atom.isHydrogen()))
      return;
    int x = atom.screenX;
    int y = atom.screenY;
    int z = atom.screenZ;
    int diameter = atom.screenDiameter;
    int radius = diameter >> 1;

    if (!wireframeRotation && control.isAtomPicked(atom)) {
      int halo = radius + 5;
      int halo2 = 2 * halo;
      g.setColor(pickedColor);
      g.fillOval(x - halo, y - halo, halo2, halo2);
    }
    Color color = colorProfile.getColor(atom);
    g.setColor(color);
    if (diameter <= 3) {
      if (diameter > 0) {
        if ((atomDrawMode == DisplayControl.WIREFRAME) && (diameter == 3))
          g.drawRect(x - radius, y - radius, 2, 2);
        else
          g.fillRect(x - radius, y - radius, diameter, diameter);
      }
    } else {
      if (! wireframeRotation && (atomDrawMode == DisplayControl.SHADING)) {
        renderShadedAtom(x, y, diameter, color);
        return;
      }
      // the area *drawn* by an oval is 1 larger than the area
      // *filled* by an oval
      --diameter;
      if (! wireframeRotation &&
          (atomDrawMode != DisplayControl.WIREFRAME)) {
        g.fillOval(x - radius, y - radius, diameter, diameter);
        g.setColor(getOutline(color));
      }
      g.drawOval(x - radius, y - radius, diameter, diameter);
    }
  }

  private static final int minCachedSize = 4;
  private static final int maxCachedSize = 50;
  private static final Hashtable ballImages = new Hashtable();
  private static final int scalableSize = 47;
  private static final int maxSmoothedSize = 200;
  // I am getting severe graphical artifacts around the edges when
  // rendering hints are turned on. Therefore, I am adding a margin
  // to shaded rendering in order to cut down on edge effects
  private static final int artifactMargin = 4;
  private static final int minShadingBufferSize = maxCachedSize;
  private static final int maxShadingBufferSize =
    maxSmoothedSize + artifactMargin*2;
    
  
  private void renderShadedAtom(int x, int y, int diameter, Color color) {
    if (! ballImages.containsKey(color)) {
      loadShadedSphereCache(color);
    }
    Image[] shadedImages = (Image[]) ballImages.get(color);
    int radius = diameter / 2;
    if (diameter < minCachedSize) {
      // the area drawn by an oval is 1 larger than the area
      // filled by an oval
      --diameter;
      g.setColor(color);
      g.fillOval(x - radius, y - radius, diameter, diameter);
      g.setColor(getDarker(color));
      g.drawOval(x - radius, y - radius, diameter, diameter);
    } else if (diameter < maxCachedSize) {
      // images in the cache have a margin of 1
      g.drawImage(shadedImages[diameter],
                   x - radius - 1, y - radius - 1, null);
    } else if (diameter < maxSmoothedSize) {
      // warning ... must fix this for the applet
      drawScaledShadedAtom((Graphics2D) g, shadedImages[0], x, y,
                           diameter, artifactMargin);
    } else {
      // too big ... just forget the smoothing
      Ellipse2D circle =
        new Ellipse2D.Float((float)(x-radius), (float)(y-radius),
                            (float)diameter, (float)diameter);
      g.setClip(circle);
      g.drawImage(shadedImages[0], x - radius, y - radius,
                   diameter, diameter, null);
      g.setClip(null);
    }
  }
  
  private void loadShadedSphereCache(Color color) {
    Image shadedImages[] = new Image[maxCachedSize];
    ballImages.put(color, shadedImages);
    boolean oldJvm = false;
    if (oldJvm) {
      for (int d = minCachedSize; d < maxCachedSize; ++d) {
        shadedImages[d] = sphereSetup(color, d+2,
                                      control.getLightSource(), false);
      }
      shadedImages[0] = sphereSetup(color, scalableSize,
                                    control.getLightSource(), false);
      return;
    }
    /*
    for (int d = minCachedSize; d < maxCachedSize; ++d) {
      shadedImages[d] = sphereSetup(color, d+2, settings, false);
      BufferedImage bi = new BufferedImage(d+2, d+2,
                                           BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = bi.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.drawImage(shadedImages[d], 0, 0, null);
      applyCircleMask(g2, d, 1);
      shadedImages[d] = bi;
    }
    shadedImages[0] = sphereSetup(color, scalableSize, settings, false);
    return;
    */
    shadedImages[0] = sphereSetup(color, scalableSize,
                                  control.getLightSource(), false);
    for (int d = minCachedSize; d < maxCachedSize; ++d) {
      BufferedImage bi = new BufferedImage(d+2, d+2,
                                           BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = bi.createGraphics();
      drawScaledShadedAtom(g2, shadedImages[0], d/2+1, d/2+1, d, 1);
      shadedImages[d] = bi;
    }
}
  private static byte[] mapRGBA;
  private static IndexColorModel cmMask;
  private static int sizeMask = 0;
  private static BufferedImage biMask = null;
  private static Graphics2D g2Mask;
  private static WritableRaster rasterMask;
  private static BufferedImage biAlphaMask;

  private void applyCircleMask(Graphics2D g, int diameter, int margin) {
    // mth 2002 nov 12
    // a 4 bit greyscale mask would/should be sufficient here, but there
    // was a bug in my JVM (or a bug in my head) which prevented it
    // from working
    if (mapRGBA == null) {
      mapRGBA = new byte[256];
      for (int i = 0; i < 256; ++ i) {
        mapRGBA[i] = (byte) i;
      }
      cmMask = new IndexColorModel(8, 256, mapRGBA, mapRGBA, mapRGBA, mapRGBA);
    }
    int size = diameter + 2*margin;
    if (size > sizeMask) {
      sizeMask = size * 2;
      if (sizeMask < minShadingBufferSize)
        sizeMask = minShadingBufferSize;
      if (sizeMask > maxShadingBufferSize)
        sizeMask = maxShadingBufferSize;
      biMask = new BufferedImage(sizeMask, sizeMask,
                                 BufferedImage.TYPE_BYTE_GRAY);
      g2Mask = biMask.createGraphics();
      g2Mask.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      rasterMask = biMask.getRaster();
      biAlphaMask = new BufferedImage(cmMask, rasterMask, false, null);
    }
    g2Mask.setColor(Color.black);
    g2Mask.fillRect(0, 0, size, size);
    g2Mask.setColor(Color.white);
    g2Mask.fillOval(margin, margin, diameter, diameter);

    Composite foo = g.getComposite();
    g.setComposite(AlphaComposite.DstIn);
    g.drawImage(biAlphaMask, 0, 0, null);
    g.setComposite(foo);
  }

  private static int sizeShadingBuffer = 0;
  private static BufferedImage biShadingBuffer = null;
  private static Graphics2D g2ShadingBuffer = null;

  void drawScaledShadedAtom(Graphics2D g2, Image image,
                            int x, int y, int diameter, int margin) {
    final int size = diameter + 2*margin;
    if (size > sizeShadingBuffer) {
      sizeShadingBuffer = size * 2; // leave some room to grow
      if (sizeShadingBuffer < minShadingBufferSize)
        sizeShadingBuffer = minShadingBufferSize;
      if (sizeShadingBuffer > maxShadingBufferSize)
        sizeShadingBuffer = maxShadingBufferSize;
      biShadingBuffer = new BufferedImage(sizeShadingBuffer, sizeShadingBuffer,
                                          BufferedImage.TYPE_INT_ARGB);
      g2ShadingBuffer = biShadingBuffer.createGraphics();
      g2ShadingBuffer.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    g2ShadingBuffer.drawImage(image, 0, 0, size, size, null);
    applyCircleMask(g2ShadingBuffer, diameter, margin);
    int radius = diameter / 2;
    int upperleftX = x - radius - margin;
    int upperleftY = y - radius - margin;
    int lowerrightX = upperleftX + diameter + margin;
    int lowerrightY = upperleftY + diameter + margin;
    g2.setClip(upperleftX, upperleftY, size, size);
    g2.drawImage(biShadingBuffer, upperleftX, upperleftY, null);
    g2.setClip(null);
    //    g2.drawImage(biShadingBuffer,
    //                 upperleftX, upperleftY, lowerrightX, lowerrightY,
    //                 0, 0, size, size,
    //                 null);
  }

  /**
   * Creates a shaded atom image.
   */
  private static Image sphereSetup(Color ballColor, int diameter,
      float[] lightSource, boolean doNotClip) {

    float v1[] = new float[3];
    float v2[] = new float[3];
    int radius = (diameter + 1) / 2; // round it up
    int j = -1;

    // Create our own version of an IndexColorModel:
    int model[] = new int[diameter*diameter];

    // Normalize the lightsource vector:
    float[] lightsource = new float[3];
    for (int i = 0; i < 3; ++ i)
      lightsource[i] = lightSource[i];
    normalize(lightsource);
    for (int k1 = -(diameter - radius); k1 < radius; k1++) {
      for (int k2 = -(diameter - radius); k2 < radius; k2++) {
        j++;
        v1[0] = k2;
        v1[1] = k1;
        float len1 = (float) Math.sqrt(k2 * k2 + k1 * k1);
        if (doNotClip || len1 <= radius) {
          int red2 = 0;
          int green2 = 0;
          int blue2 = 0;
          v1[2] = radius * (float) Math.cos(Math.asin(len1 / radius));
          normalize(v1);
          float len2 = (float) Math.abs((double) (v1[0] * lightsource[0]
                         + v1[1] * lightsource[1] + v1[2] * lightsource[2]));
          if (len2 < 0.995f) {
            red2 = (int) (ballColor.getRed() * len2);
            green2 = (int) (ballColor.getGreen() * len2);
            blue2 = (int) (ballColor.getBlue() * len2);
          } else {
            v2[0] = lightsource[0] + 0.0f;
            v2[1] = lightsource[1] + 0.0f;
            v2[2] = lightsource[2] + 1.0f;
            normalize(v2);
            float len3 = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
            float len4 = 8.0f * len3 * len3 - 7.0f;
            float len5 = 100.0f * len4;
            len5 = Math.max(len5, 0.0f);
            red2 = (int) (ballColor.getRed() * 155 * len2 + 100.0 + len5);
            green2 = (int) (ballColor.getGreen() * 155 * len2 + 100.0 + len5);
            blue2 = (int) (ballColor.getBlue() * 155 * len2 + 100.0 + len5);
          }
          red2 = Math.min(red2 + 32, 255);
          green2 = Math.min(green2 + 32, 255);
          blue2 = Math.min(blue2 + 32, 255);


          // Bitwise masking to make model:
          model[j] = 0xff000000 | red2 << 16 | green2 << 8 | blue2;
        } else {
          model[j] = 0x00000000;
        }
      }
    }
    return
      imageComponent.createImage(new MemoryImageSource(diameter, diameter,
                                                       model, 0,
                                                       diameter));
  }

  /**
   * normalizes the float[3] vector in place
   */
  private static void normalize(float v[]) {

    float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (Math.abs(len - 0.0) < Double.MIN_VALUE) {
      v[0] = 0.0f;
      v[1] = 0.0f;
      v[2] = 0.0f;
    } else {
      v[0] = v[0] / len;
      v[1] = v[1] / len;
      v[2] = v[2] / len;
    }
  }

  /**
   * Sets the Component where all atoms will be drawn.
   *
   * @param c the Component
   */
  public static void setImageComponent(Component c) {
    imageComponent = c;
  }
  private static Component imageComponent;

}

