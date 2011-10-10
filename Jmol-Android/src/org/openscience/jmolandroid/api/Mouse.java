/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-02 18:35:13 -0500 (Sun, 02 Oct 2011) $
 * $Revision: 16205 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmolandroid.api;

import org.jmol.api.Event;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.binding.Binding;


/**
 * formerly org.jmol.viewer.MouseManager14
 * 
 * methods required by Jmol that access java.awt.event
 * 
 * private to org.jmol.awt
 * 
 */

class Mouse {

  //private Viewer viewer;
  private ActionManager actionManager;

  /**
   * 
   * @param viewer  UNUSED in Jmol-Android
   * @param actionManager
   */
  Mouse(Viewer viewer, ActionManager actionManager) {
    //this.viewer = viewer;
    this.actionManager = actionManager;
  }

  void clear() {
    // nothing to do here now -- see ActionManager
  }

  void dispose() {
    actionManager.dispose();
  }

  boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time) {
    modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      mousePressed(time, x, y, modifiers, false);
      break;
    case Event.MOUSE_DRAG:
      mouseDragged(time, x, y, modifiers);
      break;
      /*
    case Event.MOUSE_ENTER:
      mouseEntered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      mouseExited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      mouseMoved(time, x, y, modifiers);
      break;
      */
    case Event.MOUSE_UP:
      mouseReleased(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        mouseClicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

/*
  private String keyBuffer = "";

  private void clearKeyBuffer() {
    if (keyBuffer.length() == 0)
      return;
    keyBuffer = "";
    if (viewer.getBooleanProperty("showKeyStrokes"))
      viewer
          .evalStringQuiet("!set echo _KEYSTROKES; set echo bottom left;echo \"\"");
  }
  private void mouseEntered(long time, int x, int y) {
    actionManager.mouseEntered(time, x, y);
  }

  private void mouseExited(long time, int x, int y) {
    actionManager.mouseExited(time, x, y);
  }
*/
  void setMouseMode() {
    //clearKeyBuffer();
    actionManager.setMouseMode();
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    //clearKeyBuffer();
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    actionManager.mouseAction(Binding.CLICKED, time, x, y, 1, modifiers);
  }

  boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
/*  
  private void mouseMoved(long time, int x, int y, int modifiers) {
    clearKeyBuffer();
    if (isMouseDown)
      actionManager.mouseAction(Binding.DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
    else
      actionManager.mouseAction(Binding.MOVED, time, x, y, 0, modifiers);
  }

  private void mouseWheel(long time, int rotation, int modifiers) {
    clearKeyBuffer();
    actionManager.mouseAction(Binding.WHEELED, time, 0, rotation, 0, modifiers);
  }
*/
  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void mousePressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    //clearKeyBuffer();
    isMouseDown = true;
    actionManager.mouseAction(Binding.PRESSED, time, x, y, 0, modifiers);
  }

  private void mouseReleased(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    actionManager.mouseAction(Binding.RELEASED, time, x, y, 0, modifiers);
  }

  private void mouseDragged(long time, int x, int y, int modifiers) {
    if ((modifiers & Binding.MAC_COMMAND) == Binding.MAC_COMMAND)
      modifiers = modifiers & ~Binding.RIGHT | Binding.CTRL; 
    actionManager.mouseAction(Binding.DRAGGED, time, x, y, 0, modifiers);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Binding.LEFT_MIDDLE_RIGHT) == 0) ? (modifiers | Binding.LEFT)
        : modifiers;
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

}
