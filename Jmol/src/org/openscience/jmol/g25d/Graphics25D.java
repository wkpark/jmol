/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.g25d;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.Java12;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

final public class Graphics25D {
  DisplayControl control;
  Image img;
  Graphics g;
  int width, height;

  public Graphics25D(DisplayControl control) {
    this.control = control;
    this.g = g;
  }

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    System.out.println("Graphics25D.setSize(" + width + "," + height + ")");
    if (g != null)
      g.dispose();
    if (width == 0 || height == 0) {
      img = null;
      g = null;
      return;
    }
    if (control.jvm12orGreater) {
      img = Swing25D.allocateImage(control.getAwtComponent(), width, height);
      g = Swing25D.createGraphics(img);
    } else {
      img = Awt25D.allocateImage(control.getAwtComponent(), width, height);
      g = Awt25D.createGraphics(img);
    }
  }

  public Image getScreenImage() {
    return img;
  }

  public void setColor(Color color) {
    g.setColor(color);
  }

  public void drawPolygon(int[] ax, int[] ay, int numPoints) {
    g.drawPolygon(ax, ay, numPoints);
  }

  public void fillPolygon(int[] ax, int[] ay, int numPoints) {
    g.fillPolygon(ax, ay, numPoints);
  }

  public void drawImage(Image image, int x, int y, ImageObserver observer) {
    g.drawImage(image, x, y, observer);
  }

  public void drawImage(Image image, int x, int y, int width, int height,
                        ImageObserver observer) {
    g.drawImage(image, x, y, width, height, observer);
  }

  public void drawImage(Image image, int x, int y) {
    g.drawImage(image, x, y, null);
  }

  public void drawOval(int x, int y, int width, int height) {
    g.drawOval(x, y, width, height);
  }

  public void drawOval(int x, int y, int z, int width, int height) {
    g.drawOval(x, y, width, height);
  }

  public void fillOval(int x, int y, int z, int width, int height) {
    g.fillOval(x, y, width, height);
  }

  public void fillOval(int x, int y, int width, int height) {
    g.fillOval(x, y, width, height);
  }

  public void drawRect(int x, int y, int width, int height) {
    g.drawRect(x, y, width, height);
  }

  public void fillRect(int x, int y, int width, int height) {
    g.fillRect(x, y, width, height);
  }

  public void drawString(String str, int xBaseline, int yBaseline) {
    g.drawString(str, xBaseline, yBaseline);
  }

  public void enableAntialiasing(boolean enableAntialiasing) {
    control.java12.enableAntialiasing(g, enableAntialiasing);
  }

  public void dottedStroke() {
    control.java12.dottedStroke(g);
  }

  public void defaultStroke() {
    control.java12.defaultStroke(g);
  }

  public void setFont(Font font) {
    g.setFont(font);
  }

  public FontMetrics getFontMetrics(Font font) {
    return g.getFontMetrics(font);
  }

  public void setClip(Shape shape) {
    g.setClip(shape);
  }

  public void setClip(int x, int y, int width, int height) {
    g.setClip(x, y, width, height);
  }

  // 3D specific routines
  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    g.drawLine(x1, y1, x2, y2);
  }

  public void drawPixel(int x, int y, int z) {
    g.drawLine(x, y, x, y);
  }

  public void drawPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    g.drawPolygon(ax, ay, numPoints);
  }

  public void fillPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    g.fillPolygon(ax, ay, numPoints);
  }

}
