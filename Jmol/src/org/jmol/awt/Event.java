/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.awt;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.MemoryImageSource;
import java.net.URL;

import org.jmol.viewer.JmolConstants;

public class Event {

  public static final int SHIFT_MASK = InputEvent.SHIFT_MASK;
  public static final int ALT_MASK = InputEvent.ALT_MASK;
  public static final int CTRL_MASK = InputEvent.CTRL_MASK;
  public static final int META_MASK = InputEvent.META_MASK;
  public static final int VK_SHIFT = KeyEvent.VK_SHIFT;
  public static final int VK_ALT = KeyEvent.VK_ALT;
  public static final int VK_CONTROL = KeyEvent.VK_CONTROL;
  public static final int VK_LEFT = KeyEvent.VK_LEFT;
  public static final int VK_RIGHT = KeyEvent.VK_RIGHT;
  public static final int VK_PERIOD = KeyEvent.VK_PERIOD;
  public static final int VK_SPACE = KeyEvent.VK_SPACE;
  public static final int VK_DOWN = KeyEvent.VK_DOWN;
  public static final int VK_UP = KeyEvent.VK_UP;
  
  public static void setCursor(int c, Container display) {
    switch (c) {
    case JmolConstants.CURSOR_HAND:
      c = Cursor.HAND_CURSOR;
      break;
    case JmolConstants.CURSOR_MOVE:
      c = Cursor.MOVE_CURSOR;
      break;
    case JmolConstants.CURSOR_ZOOM:
      c = Cursor.N_RESIZE_CURSOR;
      break;
    case JmolConstants.CURSOR_CROSSHAIR:
      c = Cursor.CROSSHAIR_CURSOR;
      break;
    case JmolConstants.CURSOR_WAIT:
      c = Cursor.WAIT_CURSOR;
      break;
    default:
      display.setCursor(Cursor.getDefaultCursor());
      return;
    }
    display.setCursor(Cursor.getPredefinedCursor(c));
  }

  public static void setTransparentCursor(Container display) {
    int[] pixels = new int[1];
    Image image = Toolkit.getDefaultToolkit().createImage(
        new MemoryImageSource(1, 1, pixels, 0, 1));
    Cursor transparentCursor = Toolkit.getDefaultToolkit()
        .createCustomCursor(image, new Point(0, 0), "invisibleCursor");
    display.setCursor(transparentCursor);
  }

  public static Image createImage(Object data) {
    if (data instanceof URL)
      return Toolkit.getDefaultToolkit().createImage((URL) data);
    if (data instanceof String)
      return Toolkit.getDefaultToolkit().createImage((String) data);
    if (data instanceof byte[])
      return Toolkit.getDefaultToolkit().createImage((byte[]) data);
    return null;
  }

  public static void waitForDisplay(Container display, Image image) throws InterruptedException {
    MediaTracker mediaTracker = new MediaTracker(display);
    mediaTracker.addImage(image, 0);
    mediaTracker.waitForID(0);
  }
}
