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

import org.jmol.script.ScriptEvaluator;
import org.jmol.viewer.Viewer;

public class ScriptDelayThread extends JmolThread {

  private long millis;

  public ScriptDelayThread(ScriptEvaluator eval, Viewer viewer, long millis) {
    super();
    setViewer(viewer, "ScriptDelayThread");
    this.millis = millis;
    setEval(eval);
  }

  private int seconds;
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        int delayMax;
        if (millis < 0)
          millis = -millis;
        else if ((delayMax = viewer.getDelayMaximum()) > 0 && millis > delayMax)
          millis = delayMax;
        millis -= System.currentTimeMillis() - startTime;
        if (isJS) {
          seconds = 0;
        } else {
          seconds = (int) millis / 1000;
          millis -= seconds * 1000;
          if (millis <= 0)
            millis = 1;
        }
        if (!isJS)
          viewer.popHoldRepaintWhy("delay INIT");
        //$FALL-THROUGH$
      case MAIN:
        if (interrupted|| eval.interruptExecution
        || !isJS && eval.currentThread != Thread.currentThread()) {
          mode = FINISH;
          break;
        }
        if (!runSleep(seconds-- > 0 ? 1000 : (int) millis, FINISH))
          return;
        if (seconds < 0)
          millis = 0;
        mode = (seconds > 0 || millis > 0 ? MAIN : FINISH);
        break;
      case FINISH:
        if (!isJS)
          viewer.pushHoldRepaintWhy("delay FINISH");
        resumeEval();
        return;
      }
  }
}