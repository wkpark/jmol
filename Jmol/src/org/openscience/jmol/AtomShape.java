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

package org.openscience.jmol;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.Kernel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.AlphaComposite;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Float;
import java.awt.RenderingHints;
import java.util.Enumeration;
import java.util.HashMap;

import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;

/**
 * Graphical representation of an atom.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class AtomShape implements Shape {

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
      int z = (int)atom.getScreenPosition().z;
      int zOther = (int)otherAtom.getScreenPosition().z;
      if ((showHydrogens || !otherAtom.isHydrogen()) &&
          (z > zOther) ||
          ((z==zOther) && (atom.getAtomNumber() > otherAtom.getAtomNumber())))
        renderBond(g2, atom, otherAtom);
    }
  }
  
  public double getZ() {
    return atom.getScreenPosition().z;
  }
  
  private static AtomRenderer atomRenderer;
  private static boolean showAtoms;
  private static boolean showHydrogens;
  private static boolean showBonds;
  private static boolean fastRendering;
  private static boolean drawBondsToAtomCenters;
  private static int bondDrawMode;
  private static int atomDrawMode;
  private static int labelMode;
  private static DisplaySettings settings;
  private static double halfBondWidth;
  private static Color outlineColor;
  private static boolean darkerOutlineColor = true;
  private static Color pickedColor;
  private static Color backgroundColor;
  private static Graphics2D g2;
  private static ColorProfile colorProfile;

  public static void prepareRendering(Graphics g, DisplaySettings ds) {
    g2 = (Graphics2D) g;
    settings = ds;
    atomDrawMode = settings.getAtomDrawMode();
    bondDrawMode = settings.getBondDrawMode();
    showAtoms = settings.getShowAtoms();
    showHydrogens = settings.getShowHydrogens();
    showBonds = settings.getShowBonds();
    labelMode = settings.getLabelMode();
    fastRendering = settings.getFastRendering();
    drawBondsToAtomCenters = settings.getDrawBondsToAtomCenters();
    halfBondWidth = 0.5 *
      settings.getBondWidth() * settings.getBondScreenScale();
    outlineColor = settings.getOutlineColor();
    pickedColor = settings.getPickedColor();
    // backgroundColor should be in settings
    backgroundColor = DisplayPanel.getBackgroundColor();
    if (settings.getAtomColorProfile() == DisplaySettings.ATOMCHARGE) {
        colorProfile = new ChargeColorProfile();
    } else {
        colorProfile = new DefaultColorProfile();
    }
  }

  private static BondRenderer shadingBondRenderer = new ShadingBondRenderer();

  public void renderLabel() {
    if (labelMode == DisplaySettings.NOLABELS)
      return;
    int x = (int) atom.getScreenPosition().x;
    int y = (int) atom.getScreenPosition().y;
    int z = (int) atom.getScreenPosition().z;
    int diameter =
      (int) (2.0f
        * settings.getCircleRadius(z, atom.getType().getVdwRadius()));
    int radius = diameter >> 1;

    int j = 0;
    String s = null;
    Font font = new Font("Helvetica", Font.PLAIN, radius);
    g2.setFont(font);
    FontMetrics fontMetrics = g2.getFontMetrics(font);
    int k = fontMetrics.getAscent();
    g2.setColor(settings.getTextColor());
    
    String label = null;
    switch (labelMode) {
    case DisplaySettings.SYMBOLS:
      if (atom.getType() != null) {
        label = atom.getType().getRoot();
      }
      break;

    case DisplaySettings.TYPES:
      if (atom.getType() != null) {
        label = atom.getType().getName();
      }
      break;

    case DisplaySettings.NUMBERS:
      label = Integer.toString(atom.getAtomNumber() + 1);
      break;

    }
    if (label != null) {
      j = fontMetrics.stringWidth(label);
      g2.drawString(label, x - j / 2, y + k / 2);
    }
    if (!settings.getPropertyMode().equals("")) {

      // check to make sure this atom has this property:
      Enumeration propIter = atom.getProperties().elements();
      while (propIter.hasMoreElements()) {
        PhysicalProperty p = (PhysicalProperty) propIter.nextElement();
        if (p.getDescriptor().equals(settings.getPropertyMode())) {
        
          // OK, we had this property.  Let's draw the value on
          // screen:
          font = new Font("Helvetica", Font.PLAIN, radius / 2);
          g2.setFont(font);
          g2.setColor(settings.getTextColor());
          s = p.stringValue();
          if (s.length() > 5) {
            s = s.substring(0, 5);
          }
          k = 2 + (int) (radius / 1.4142136f);
          g2.drawString(s, x + k, y - k);
        }
      }
    }
  }

  public void renderBond(Graphics2D g2, Atom atom1, Atom atom2) {

    Color color1 = colorProfile.getColor(atom1);
    Color color2 = colorProfile.getColor(atom2);

    int x1 = (int) atom1.getScreenPosition().x;
    int y1 = (int) atom1.getScreenPosition().y;
    int x2 = (int) atom2.getScreenPosition().x;
    int y2 = (int) atom2.getScreenPosition().y;
    int dx = x2 - x1;
    int dx2 = dx * dx;
    int dy = y2 - y1;
    int dy2 = dy * dy;
    if (dx2 + dy2 < 4) {
      // if magnitude is very small
      // ... and avoid divide by zero when magnitude==0
      return;
    }
    if (fastRendering ||
        ((bondDrawMode == DisplaySettings.LINE) && drawBondsToAtomCenters)) {
      if (color1.equals(color2)) {
        g2.setColor(color1);
        g2.drawLine(x1, y1, x2, y2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        g2.setColor(color1);
        g2.drawLine(x1, y1, xMid, yMid);
        g2.setColor(color2);
        g2.drawLine(xMid, yMid, x2, y2);
      }
      return;
    }
    if (false && bondDrawMode == DisplaySettings.SHADING) {
      shadingBondRenderer.paint(g2, atom2, atom1, settings);
      shadingBondRenderer.paint(g2, atom1, atom2, settings);
      return;
    }
    int z1 = (int) atom1.getScreenPosition().z;
    int z2 = (int) atom2.getScreenPosition().z;
    int dz = z2 - z1;
    int dz2 = dz * dz;
    int diameter1 =
      (int) (2.0f
        * settings.getCircleRadius(z1, atom1.getType().getVdwRadius()));
    int radius1 = diameter1 >> 1;
    int diameter2 =
      (int) (2.0f
        * settings.getCircleRadius(z2, atom2.getType().getVdwRadius()));
    int radius2 = diameter2 >> 1;

    int magnitude = (int) Math.sqrt(dx2 + dy2);
    int bondOrder = Bond.getBondOrder(atom1, atom2);
    // calculate entry/exit radius of the bond connection points
    double cosine = magnitude / Math.sqrt(dx2 + dy2 + dz2);
    int radius1Bond = (int)(radius1 * cosine);
    int radius2Bond = (int)(radius2 * cosine);
    int arcFactor = 1;
    int x1Bond = x1 + ((radius1Bond + arcFactor) * dx) / magnitude;
    int y1Bond = y1 + ((radius1Bond + arcFactor) * dy) / magnitude;
    int x2Bond = x2 - ((radius2Bond + arcFactor) * dx) / magnitude;
    int y2Bond = y2 - ((radius2Bond + arcFactor) * dy) / magnitude;

    if (bondDrawMode == DisplaySettings.LINE) {
      if ((bondOrder == 1) || (bondOrder == 3)) {
        drawBondLine(x1Bond, y1Bond, color1,
                     x2Bond, y2Bond, color2,
                     dx, dy, magnitude, 0);
      }
      if ((bondOrder == 2) || (bondOrder == 3)) {
        int bondSeparation = (bondOrder == 2) ? 2 : 4;
        drawBondLine(x1Bond, y1Bond, color1,
                     x2Bond, y2Bond, color2,
                     dx, dy, magnitude, bondSeparation);
        drawBondLine(x1Bond, y1Bond, color1,
                     x2Bond, y2Bond, color2,
                     dx, dy, magnitude, -bondSeparation);
      }
      return;
    }

    if ((atomDrawMode != DisplaySettings.WIREFRAME) &&
        (magnitude < radius1 + radius2Bond)) {
      // the shapes are solid and the front atom (radius1) has
      // completely obscured the bond
      return;
    }

    if ((bondOrder == 1) || (bondOrder == 3)) {
      drawBondRectangle(x1Bond, y1Bond, color1,
                        x2Bond, y2Bond, color2,
                        dx, dy, magnitude, 0);
    }
    if ((bondOrder == 2) || (bondOrder == 3)) {
      int bondSeparation = (bondOrder == 2) ? 2 : 4;
      drawBondRectangle(x1Bond, y1Bond, color1,
                        x2Bond, y2Bond, color2,
                        dx, dy, magnitude, bondSeparation);
      drawBondRectangle(x1Bond, y1Bond, color1,
                        x2Bond, y2Bond, color2,
                        dx, dy, magnitude, -bondSeparation);
    }
  }

  private void drawBondLine(int x1Bond, int y1Bond, Color color1,
                            int x2Bond, int y2Bond, Color color2,
                            int dx, int dy, int magnitude, int separation) {
    // offset from the centerline by the bond separation factor
    int sepUp = (int) (separation * halfBondWidth);
    int sepDn = sepUp - (int) ((separation * 2) * halfBondWidth);
    x1Bond += (sepDn * dy) / magnitude;
    y1Bond += (sepUp * dx) / magnitude;
    x2Bond -= (sepUp * dy) / magnitude;
    y2Bond -= (sepDn * dx) / magnitude;
    if (color1.equals(color2)) {
      g2.setColor(color1);
      g2.drawLine(x1Bond, y1Bond, x2Bond, y2Bond);
    } else {
      // calculate the midpoint
      int xMid = (x1Bond + x2Bond) / 2;
      int yMid = (y1Bond + y2Bond) / 2;
      // draw the two line segments
      g2.setColor(color1);
      g2.drawLine(x1Bond, y1Bond, xMid, yMid);
      g2.setColor(color2);
      g2.drawLine(xMid, yMid, x2Bond, y2Bond);
    }
  }

  private final static int[] xPoints = new int[4];
  private final static int[] yPoints = new int[4];

  private void drawBondRectangle(int x1Bond, int y1Bond, Color color1,
                                 int x2Bond, int y2Bond, Color color2,
                                 int dx, int dy,
                                 int magnitude, int separation) {
    // offset from the centerline by the bond separation factor
    int sepUp = (int) (separation * halfBondWidth);
    int sepDn = sepUp - (int) ((separation * 2) * halfBondWidth);
    x1Bond += (sepDn * dy) / magnitude;
    y1Bond += (sepUp * dx) / magnitude;
    x2Bond -= (sepUp * dy) / magnitude;
    y2Bond -= (sepDn * dx) / magnitude;
    // offsets for the width of the bond rectangle
    int xHalfWidth = (int)(halfBondWidth * dy / magnitude);
    int yHalfWidth = (int)(halfBondWidth * dx / magnitude);
    int xFullWidth = (int)(halfBondWidth * 2 * dy / magnitude);
    int yFullWidth = (int)(halfBondWidth * 2 * dx / magnitude);

    int x1Top = x1Bond + xHalfWidth;
    int y1Top = y1Bond - yHalfWidth;
    int x1Bot = x1Top - xFullWidth;
    int y1Bot = y1Top + yFullWidth;

    int x2Top = x2Bond + xHalfWidth;
    int y2Top = y2Bond - yHalfWidth;
    int x2Bot = x2Top - xFullWidth;
    int y2Bot = y2Top + yFullWidth;

    int xMidTop = (x1Top + x2Top) / 2;
    int yMidTop = (y1Top + y2Top) / 2;
    int xMidBot = (x1Bot + x2Bot) / 2;
    int yMidBot = (y1Bot + y2Bot) / 2;

    if (color1.equals(color2)) {
      xPoints[0] = x2Top; yPoints[0] = y2Top;
      xPoints[1] = x2Bot; yPoints[1] = y2Bot;
      xPoints[2] = x1Bot; yPoints[2] = y1Bot;
      xPoints[3] = x1Top; yPoints[3] = y1Top;

      g2.setColor(color1);
      if (bondDrawMode == DisplaySettings.WIREFRAME) {
        g2.drawPolygon(xPoints, yPoints, 4);
      } else {
        g2.fillPolygon(xPoints, yPoints, 4);
      }
    } else {

      xPoints[0] = x2Top; yPoints[0] = y2Top;
      xPoints[1] = x2Bot; yPoints[1] = y2Bot;
      xPoints[2] = xMidBot; yPoints[2] = yMidBot;
      xPoints[3] = xMidTop; yPoints[3] = yMidTop;

      g2.setColor(color2);
      if (bondDrawMode == DisplaySettings.WIREFRAME) {
        g2.drawPolygon(xPoints, yPoints, 4);
      } else {
        g2.fillPolygon(xPoints, yPoints, 4);
      }

      xPoints[0] = x1Top; yPoints[0] = y1Top;
      xPoints[1] = x1Bot; yPoints[1] = y1Bot;
      g2.setColor(color1);
      if (bondDrawMode == DisplaySettings.WIREFRAME) {
        g2.drawPolygon(xPoints, yPoints, 4);
      } else {
        g2.fillPolygon(xPoints, yPoints, 4);
      }
    }
    if ((bondDrawMode != DisplaySettings.WIREFRAME) &&
        (bondDrawMode != DisplaySettings.SHADING)) {
      if (!darkerOutlineColor || color1.equals(color2)) {
        g2.setColor(darkerOutlineColor ? getDarker(color1) : outlineColor);
        g2.drawLine(x1Top, y1Top, x2Top, y2Top);
        g2.drawLine(x1Bot, y1Bot, x2Bot, y2Bot);
      } else {
        g2.setColor(getDarker(color1));
        g2.drawLine(x1Top, y1Top, xMidTop, yMidTop);
        g2.drawLine(x1Bot, y1Bot, xMidBot, yMidBot);
        g2.setColor(getDarker(color2));
        g2.drawLine(x2Top, y2Top, xMidTop, yMidTop);
        g2.drawLine(x2Bot, y2Bot, xMidBot, yMidBot);
      }
    }
  }

  private Color getDarker(Color color) {
    // later this will be cached to prevent garbage generation
    // for now, this is fine
    return color.darker();
  }

  private void renderAtom() {
    if (!showAtoms || (!showHydrogens && atom.isHydrogen()))
      return;
    int x = (int) atom.getScreenPosition().x;
    int y = (int) atom.getScreenPosition().y;
    int z = (int) atom.getScreenPosition().z;
    int diameter =
      (int) (2.0f
        * settings.getCircleRadius(z, atom.getType().getVdwRadius()));
    int radius = diameter >> 1;

    if (!fastRendering && settings.isAtomPicked(atom)) {
      int halo = radius + 5;
      int halo2 = 2 * halo;
      g2.setColor(pickedColor);
      g2.fillOval(x - halo, y - halo, halo2, halo2);
    }
    Color color = colorProfile.getColor(atom);
    g2.setColor(color);
    if (diameter <= 3) {
      if (diameter > 0) {
        if ((atomDrawMode == DisplaySettings.WIREFRAME) && (diameter == 3))
          g2.drawRect(x - radius, y - radius, 2, 2);
        else
          g2.fillRect(x - radius, y - radius, diameter, diameter);
      }
    } else {
      if (! fastRendering && (atomDrawMode == DisplaySettings.SHADING)) {
        renderShadedAtom(x, y, diameter, color);
        return;
      }
      // the area *drawn* by an oval is 1 larger than the area
      // *filled* by an oval
      --diameter;
      if (!fastRendering &&
          (atomDrawMode != DisplaySettings.WIREFRAME)) {
        g2.fillOval(x - radius, y - radius, diameter, diameter);
        g2.setColor(getDarker(color));
      }
      g2.drawOval(x - radius, y - radius, diameter, diameter);
    }
  }

  final private static int minCachedImage = 4;
  final private static int maxCachedImage = 50;
  final private static HashMap ballImages = new HashMap();

  private void renderShadedAtom(int x, int y, int diameter, Color color) {
    if (! ballImages.containsKey(color)) {
      loadShadedCache(color);
    }
    Image[] shadedImages = (Image[]) ballImages.get(color);
    int radius = diameter / 2;
    if (diameter < minCachedImage) {
      // the area drawn by an oval is 1 larger than the area
      // filled by an oval
      --diameter;
      g2.setColor(color);
      g2.fillOval(x - radius, y - radius, diameter, diameter);
      g2.setColor(getDarker(color));
      g2.drawOval(x - radius, y - radius, diameter, diameter);
    } else if (diameter < maxCachedImage) {
      g2.drawImage(shadedImages[diameter], x - radius, y - radius, null);
    } else {
      Ellipse2D circle =
        new Ellipse2D.Float((float)(x-radius), (float)(y-radius),
                            (float)diameter, (float)diameter);
      g2.setClip(circle);
      g2.drawImage(shadedImages[0], x - radius, y - radius,
                   diameter, diameter, null);
      g2.setClip(null);
    }
  }
  
  private void loadShadedCache(Color color) {
    Image shadedImages[] = new Image[maxCachedImage];
    for (int d = minCachedImage; d < maxCachedImage; ++d) {
      shadedImages[d] = generateShadedSphere(color, d, true);
    }
    shadedImages[0] = generateShadedSphere(color, 50, false);
    ballImages.put(color, shadedImages);
    }

  private Image generateShadedSphere(Color color, int diameter,
                                     boolean applyCircleMask) {
    // Image img = sphereSetup(color, diameter, settings);
    return gradientPaintSphere(color, diameter, applyCircleMask);
  }

  private static Color colorTransparent = new Color(0, 0, 0, 0);
  private static Color colorGradScale = null;
  private static BufferedImage gradScale = new BufferedImage(150, 1, 
                                           BufferedImage.TYPE_INT_ARGB);
  private static Graphics2D g2Scale = gradScale.createGraphics();

  private void checkGradientScale(Color color) {
    if (color.equals(colorGradScale))
      return;
    Color lighter = Color.white;
    Color darker = getDarker(color).darker();
    GradientPaint gp;
    int lightPct = 3;
    int lighteningPct = 15;
    int colorPct = 55;
    int darkeningPct = 85;

    g2Scale.setPaint(lighter);
    g2Scale.drawLine(0, 0, lightPct, 0);

    gp = new GradientPaint(lightPct, 0, lighter,
                             lighteningPct, 0, color);
    g2Scale.setPaint(gp);
    g2Scale.drawLine(lightPct, 0, lighteningPct, 0);

    g2Scale.setPaint(color);
    g2Scale.drawLine(lighteningPct, 0, colorPct, 0);

    gp = new GradientPaint(colorPct, 0, color,
                             darkeningPct, 0, darker);
    g2Scale.setPaint(gp);
    g2Scale.drawLine(colorPct, 0, darkeningPct, 0);

    g2Scale.setPaint(darker);
    g2Scale.drawLine(darkeningPct, 0, 149, 0);
  }

  private BufferedImage gradientPaintSphere(Color color,
                                            int diameter,
                                            boolean applyCircleMask) {
    checkGradientScale(color);
    BufferedImage bi = new BufferedImage(diameter, diameter,
                                         BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bi.createGraphics();
    g.setPaint(colorTransparent);
    g.fillRect(0, 0, diameter, diameter);
    float shiny = 25f * diameter / 100;

    for (int i = 0; i < diameter; ++ i) {
      double i2Shiny = i-shiny; i2Shiny *= i2Shiny;
      for (int j = 0; j < diameter; ++j) {
        double j2Shiny = j-shiny; j2Shiny *= j2Shiny;
        double mag = Math.sqrt(i2Shiny + j2Shiny);
        int pct = (int)(mag * 100 / diameter);
        int co = gradScale.getRGB(pct, 0);
        bi.setRGB(i, j, co);
      }
    }
    if (applyCircleMask)
      applyCircleMask(g, diameter);
    return bi;
  }

  private static byte[] mapRGBA;
  private static IndexColorModel cmMask;

  private void applyCircleMask(Graphics2D g, int diameter) {
    // mth 2002 nov 123
    // a 4 bit greyscale mask would be sufficient here, but there
    // was a bug in my JVM (or a bug in my head) which prevented it
    // from working
    if (mapRGBA == null) {
      mapRGBA = new byte[256];
      for (int i = 0; i < 256; ++ i) {
        mapRGBA[i] = (byte) i;
      }
      cmMask = new IndexColorModel(8, 256, mapRGBA, mapRGBA, mapRGBA, mapRGBA);
    }
    BufferedImage biMask = new BufferedImage(diameter, diameter,
                                             BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D gMask = biMask.createGraphics();
    gMask.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
    gMask.setPaint(Color.black);
    gMask.fillRect(0, 0, diameter, diameter);
    gMask.setPaint(Color.white);
    gMask.fillOval(0, 0, diameter, diameter);
    WritableRaster rasterMask = biMask.getRaster();
    BufferedImage biAlphaMask =
      new BufferedImage(cmMask, rasterMask, false, null);
    g.setComposite(AlphaComposite.DstIn);
    g.drawImage(biAlphaMask, 0, 0, null);
  }

  /**
   * Creates a shaded atom image.
   *
   * @param ballColor color for the sphere.
   * @param settings the display settings used for the light source.
   * @return an image of a shaded sphere.
   */
  private static Image sphereSetup(Color ballColor, int diameter,
      DisplaySettings settings) {

    float v1[] = new float[3];
    float v2[] = new float[3];
    int radius = (diameter + 1) / 2; // round it up
    int j = -1;

    // Create our own version of an IndexColorModel:
    int model[] = new int[diameter * diameter];

    // Normalize the lightsource vector:
    float[] lsSettings = settings.getLightSourceVector();
    float[] lightsource = new float[3];
    for (int i = 0; i < 3; ++ i)
      lightsource[i] = lsSettings[i];
    normalize(lightsource);
    for (int k1 = -(diameter - radius); k1 < radius; k1++) {
      for (int k2 = -(diameter - radius); k2 < radius; k2++) {
        j++;
        v1[0] = k2;
        v1[1] = k1;
        float len1 = (float) Math.sqrt(k2 * k2 + k1 * k1);
        if (len1 <= radius) {
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
    Image result =
      imageComponent.createImage(new MemoryImageSource(diameter,
                                                       diameter,
                                                       model,
                                                       0, diameter));
    return result;
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

