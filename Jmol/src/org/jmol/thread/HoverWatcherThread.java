/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

package org.jmol.thread;

import org.jmol.util.Logger;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.MouseState;
import org.jmol.viewer.Viewer;

public class HoverWatcherThread extends JmolThread {
  
  /**
   * 
   */
  private ActionManager actionManager;
  private final MouseState current, moved;
  private final Viewer viewer;

  /**
   * @param actionManager 
   * @param current 
   * @param moved 
   * @param viewer
   */
  public HoverWatcherThread(ActionManager actionManager, MouseState current, MouseState moved, Viewer viewer) {
    this.actionManager = actionManager;
    this.current = current;
    this.moved = moved;
    this.viewer = viewer;
    setMyName("HoverWatcher");
    start();
  }

  @Override
  public void run() {
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    int hoverDelay;
    try {
      while (!interrupted && (hoverDelay = viewer.getHoverDelay()) > 0) {
        Thread.sleep(hoverDelay);
        if (moved.is(current)) {
          // last operation was move
          long currentTime = System.currentTimeMillis();
          int howLong = (int) (currentTime - moved.time);
          if (howLong > hoverDelay && !interrupted) {
            actionManager.checkHover();
          }
        }
      }
    } catch (InterruptedException ie) {
      Logger.debug("Hover interrupted");
    } catch (Exception ie) {
      Logger.debug("Hover Exception: " + ie);
    }
  }
}