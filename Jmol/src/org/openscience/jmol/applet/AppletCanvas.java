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
package org.openscience.jmol.applet;

import org.openscience.jmol.Atom;
import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.render.ChemFrameRenderer;
import org.openscience.jmol.render.MeasureRenderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Canvas;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

class AppletCanvas extends Canvas {

  private DisplayControl control;

  private Image bufferOffscreen;
  private Graphics graphicsOffscreen;

  private Dimension dimCurrent;
  private Rectangle rectClip = new Rectangle();

  public void setDisplayControl(DisplayControl control) {
    this.control = control;
    addComponentListener(new MyComponentListener());
  }

  public void updateSize() {
    Dimension dimT = getSize();
    if (dimT.equals(dimCurrent))
      return;
    dimCurrent = dimT;
    if ((dimCurrent.width > 0) && (dimCurrent.height > 0)) {
      bufferOffscreen = allocateBuffer();
      graphicsOffscreen = bufferOffscreen.getGraphics();
    } else {
      dimCurrent = null;
      bufferOffscreen = null;
      if (graphicsOffscreen != null) {
        graphicsOffscreen.dispose();
        graphicsOffscreen = null;
      }
    }
    control.setScreenDimension(dimCurrent);
    control.scaleFitToScreen();
  }

  private Image allocateBuffer() {
    System.out.println("allocateBuffer");
    return createImage(dimCurrent.width, dimCurrent.height);
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics g) {
    if (bufferOffscreen == null) {
      System.out.println("bufferOffscreen==null");
      return;
    }
    // transfer the clipping rectangle to our offscreen buffer
    // also, we will use rectClip later in the rendering process
    if (control.jvm12orGreater)
      g.getClipBounds(rectClip);
    else
      rectClip = g.getClipBounds();
    if (rectClip.width == 0 || rectClip.height == 0) {
      System.out.println("?Que?");
      rectClip.setBounds(0, 0, dimCurrent.width, dimCurrent.height);
    }
    graphicsOffscreen.setClip(rectClip);
    renderBuffer(graphicsOffscreen);
    g.drawImage(bufferOffscreen, 0, 0, null);
  }

  ChemFrameRenderer frameRenderer = new ChemFrameRenderer();
  MeasureRenderer measureRenderer = new MeasureRenderer();

  void renderBuffer(Graphics g) {
    g.setColor(control.getColorBackground());
    g.fillRect(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
    if (control.getFrame() != null) {
      control.setGraphicsContext(g, rectClip);
      frameRenderer.paint(g, control);
      measureRenderer.paint(g, rectClip, control);
      Rectangle rect = control.getRubberBandSelection();
      if (rect != null) {
        g.setColor(control.getColorRubberband());
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
      }
    }
    control.notifyRepainted();
  }

  // Make sure AWT knows we are using a buffered image.
  public boolean isDoubleBuffered() {
    return true;
  }

  // Make sure AWT knows we repaint the entire canvas.
  public boolean isOpaque() {
    return true;
  }

  class MyComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      updateSize();
    }
    public void componentShown(ComponentEvent e) {
      updateSize();
    }
  }
}
