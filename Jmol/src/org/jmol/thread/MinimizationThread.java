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

import org.jmol.api.MinimizerInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class MinimizationThread extends JmolThread {
  
  private final MinimizerInterface minimizer;

  public MinimizationThread(MinimizerInterface minimizer, Viewer viewer) {
    super(viewer, "MinimizationThread");
    this.minimizer = minimizer;
  }
  
  @Override
  protected boolean checkContinue() {
    return continuing && minimizer.minimizationOn() && !checkInterrupted();
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (checkContinue())
      switch (mode) {
      case INIT:
        lastRepaintTime = startTime;
        //should save the atom coordinates
        if (this.minimizer.startMinimization())
          viewer.startHoverWatcher(false);
        else
          continuing = false;
        return;
      case MAIN:
        currentTime = System.currentTimeMillis();
        int elapsed = (int) (currentTime - lastRepaintTime);
        int sleepTime = 33 - elapsed;
        if (!runSleep(sleepTime, CHECK1))
          return;
        //$FALL-THROUGH$
      case CHECK1:
        lastRepaintTime = currentTime = System.currentTimeMillis();
        if (!this.minimizer.stepMinimization())
          this.minimizer.endMinimization();
        if (isJS) {
          mode = MAIN;
          break;
        }
        return;
      case FINISH:
        restartHover();
        return;
      }
  }

  @Override
  protected void oops(Exception e) {
    if (this.minimizer.minimizationOn())
      Logger.error(e.getMessage());
  }
  

}