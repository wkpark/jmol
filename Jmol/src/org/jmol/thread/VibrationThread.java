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

  private final TransformManager transformManager;

  public VibrationThread(TransformManager transformManager, Viewer viewer) {
    super();
    setViewer(viewer, "VibrationThread");
    this.transformManager = transformManager;
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    int elapsed;
    while (true)
      switch (mode) {
      case INIT:
        lastRepaintTime = startTime = System.currentTimeMillis();
        viewer.startHoverWatcher(false);
        //$FALL-THROUGH$
      case MAIN:
        elapsed = (int) (System.currentTimeMillis() - lastRepaintTime);
        sleepTime = 33 - elapsed;
        if (!runSleep(sleepTime, CHECK1))
          return;
        //$FALL-THROUGH$
      case CHECK1:
        lastRepaintTime = System.currentTimeMillis();
        elapsed = (int) (lastRepaintTime - startTime);
        float t = (float) (elapsed % transformManager.vibrationPeriodMs)
            / transformManager.vibrationPeriodMs;
        transformManager.setVibrationT(t);
        viewer.refresh(3, "VibrationThread:run()");
        mode = (checkInterrupted() ? FINISH : MAIN);
        break;
      case FINISH:
        restartHover();
        return;
      }
  }

}