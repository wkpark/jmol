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
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Component;

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

  public void maybeEnableAntialiasing(Graphics g) {
    if (useGraphics2D && wantsAntialias) {
      Graphics2D g2d = (Graphics2D) g;
      if (wantsAntialiasAlways || !inMotion) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
      } else {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_SPEED);
      }
    }
  }

  private BasicStroke dottedStroke = null;
  public void maybeDottedStroke(Graphics g) {
    if (useGraphics2D) {
      if (dottedStroke == null) {
        dottedStroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND, 0,
                                       new float[] {3, 3}, 0);
      }
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(dottedStroke);
    }
  }

  public boolean inMotion = false;

  public void setInMotion(boolean inMotion) {
    if (this.inMotion != inMotion && control.getWireframeRotation())
      setFastRendering(inMotion);
    if (this.inMotion && !inMotion) {
      if (control.getWireframeRotation() ||
          (useGraphics2D && wantsAntialias && !wantsAntialiasAlways))
        refresh();
    }
    this.inMotion = inMotion;
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

  public boolean holdRepaint;
  public boolean repaintPending;

  public void setHoldRepaint(boolean holdRepaint) {
    if (this.holdRepaint != holdRepaint) {
      this.holdRepaint = holdRepaint;
      if (!holdRepaint && repaintPending)
        control.awtComponent.repaint();
    }
  }

  Object monitorRepaint = new Object();

  public void refreshFirmly() {
    control.awtComponent.repaint();
  }

  public void refresh() {
    if (repaintPending)
      return;
    repaintPending = true;
    if (! holdRepaint)
      control.awtComponent.repaint();
  }

  public void requestRepaintAndWait() {
    synchronized(monitorRepaint) {
      control.awtComponent.repaint();
      try {
        monitorRepaint.wait();
      } catch (InterruptedException e) {
      }
    }
  }

  public void notifyRepainted() {
    synchronized(monitorRepaint) {
      repaintPending = false;
      monitorRepaint.notify();
    }
  }
  
}
