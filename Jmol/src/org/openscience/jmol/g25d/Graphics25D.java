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
  Graphics g;

  public Graphics25D(DisplayControl control, Graphics g) {
    this.control = control;
    this.g = g;
  }

  public void dispose() {
    g.dispose();
    g = null;
  }

  public void setColor(Color color) {
    g.setColor(color);
  }

  //  public void drawLine(int x1, int y1, int x2, int y2) {
  //    g.drawLine(x1, y1, x2, y2);
  //  }

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
