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

import org.openscience.jmol.g25d.Graphics25D;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Rectangle;

public class RepaintManager {

  DisplayControl control;

  public boolean useGraphics2D = false;
  // mth 2003 05 20
  // Apple JVMs don't work with graphics2d ... disable until they fix it
  public boolean wantsGraphics2D =
    ! System.getProperty("java.vendor").startsWith("Apple Computer");
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
    /*
    if (!inMotion &&
        (control.getWireframeRotation() ||
         (useGraphics2D && wantsAntialias && !wantsAntialiasAlways))) {
      refresh();
    }
    */
    if (!inMotion)
      refresh();
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

  int maxAntialiasCount = 500;

  public boolean enableAntialiasing() {
    return wantsAntialias
      && (control.numberOfAtoms() <= maxAntialiasCount)
      && (!inMotion || wantsAntialiasAlways);
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

  final Rectangle rectOversample = new Rectangle();
  boolean tOversample;

  public void setOversample(boolean tOversample) {
    this.tOversample = tOversample;
  }

  public void render(Graphics25D g25d, Rectangle rectClip) {
    g25d.beginRendering(tOversample);
    if (tOversample) {
      rectOversample.x = rectClip.x << 1;
      rectOversample.y = rectClip.y << 1;
      rectOversample.width = rectClip.width << 1;
      rectOversample.height = rectClip.height << 1;
      rectClip = rectOversample;
    }
    g25d.clearScreenBuffer(control.getColorBackground(),
                           rectClip.x, rectClip.y,
                           rectClip.width, rectClip.height);
    if (control.getFrame() != null) {
      control.setGraphicsContext(g25d, rectClip);
      control.getJmolFrame().render(g25d, control);

      Rectangle band = control.getRubberBandSelection();
      if (band != null)
        g25d.drawRect(control.getColixRubberband(),
                      band.x, band.y, band.width, band.height);
    }
    g25d.endRendering();
    notifyRepainted();
  }
}
