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
package org.openscience.jmol;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Rectangle;

public class RepaintManager {

  DisplayControl control;

  public boolean useGraphics2D = false;
  public boolean wantsGraphics2D = true;
  public boolean wantsAntialias = true;
  public boolean wantsAntialiasAlways = false;

  public RepaintManager(DisplayControl control) {
    this.control = control;
    useGraphics2D = control.jvm12orGreater && wantsGraphics2D;
  }

  public boolean fastRendering = false;
  public void setFastRendering(boolean fastRendering) {
    this.fastRendering = fastRendering;
  }

  public boolean inMotion = false;

  public void setInMotion(boolean inMotion) {
    if (this.inMotion != inMotion && control.getWireframeRotation())
      setFastRendering(inMotion);
    this.inMotion = inMotion;
    if (!inMotion &&
        (control.getWireframeRotation() ||
         (useGraphics2D && wantsAntialias && !wantsAntialiasAlways))) {
      refresh();
    }
  }

  public void setWantsGraphics2D(boolean wantsGraphics2D) {
    if (this.wantsGraphics2D != wantsGraphics2D) {
      this.wantsGraphics2D = wantsGraphics2D;
      useGraphics2D = control.jvm12orGreater && wantsGraphics2D;
      control.flushCachedImages();
      refresh();
    }
  }

  public void setWantsAntialias(boolean wantsAntialias) {
    this.wantsAntialias = wantsAntialias;
    refresh();
  }

  public void setWantsAntialiasAlways(boolean wantsAntialiasAlways) {
    this.wantsAntialiasAlways = wantsAntialiasAlways;
    // no need to refresh in this state since we aren't doing anything
  }

  public Image takeSnapshot() {
    return null;
    //return awtComponent.takeSnapshot();
  }

  public int holdRepaint = 0;
  public boolean repaintPending;

  public void pushHoldRepaint() {
    ++holdRepaint;
  }

  public void popHoldRepaint() {
    if (--holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      control.awtComponent.repaint();
    }
  }

  public void refresh() {
    if (repaintPending)
      return;
    repaintPending = true;
    if (holdRepaint == 0)
      control.awtComponent.repaint();
  }

  public synchronized void requestRepaintAndWait() {
    control.awtComponent.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  public synchronized void notifyRepainted() {
    repaintPending = false;
    notify();
  }

  public void render(Graphics g, Rectangle rectClip) {
    g.setColor(control.getColorBackground());
    g.fillRect(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
    if (control.getFrame() != null) {
      control.setGraphicsContext(g, rectClip);
      control.frameRenderer.paint(g, control);
      // FIXME -- measurements rendered incorrectly
      // this is in the wrong spot because the display of measurements
      // needs to take z-order into account
      if (control.getShowMeasurements())
        control.measureRenderer.paint(g, rectClip, control);
      Rectangle band = control.getRubberBandSelection();
      if (band != null) {
        g.setColor(control.getColorRubberband());
        g.drawRect(band.x, band.y, band.width, band.height);
      }
    }
    notifyRepainted();
  }


}
