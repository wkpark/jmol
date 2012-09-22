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

import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class VibrationThread extends JmolThread {

  /**
   * 
   */
  private final TransformManager transformManager;
  private final Viewer viewer;

  public VibrationThread(TransformManager transformManager, Viewer viewer) {
    this.transformManager = transformManager;
    this.viewer = viewer;
    setMyName("VibrationThread");
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    long lastRepaintTime = startTime;
    try {
      do {
        long currentTime = System.currentTimeMillis();
        int elapsed = (int) (currentTime - lastRepaintTime);
        int sleepTime = 33 - elapsed;
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
        //
        lastRepaintTime = currentTime = System.currentTimeMillis();
        elapsed = (int) (currentTime - startTime);
        float t = (float) (elapsed % transformManager.vibrationPeriodMs) / transformManager.vibrationPeriodMs;
        transformManager.setVibrationT(t);
        viewer.refresh(3, "VibrationThread:run()");
      } while (!isInterrupted());
    } catch (Exception e) { //may be arithmetic %0/0
    }
  }
}