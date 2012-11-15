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

import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptEvaluator;
import org.jmol.viewer.Viewer;

public class ScriptDelayThread extends JmolThread {

  private Viewer viewer;
  private long millis;
  private ScriptEvaluator eval;
  private ScriptContext sc;

  public ScriptDelayThread(ScriptEvaluator eval, Viewer viewer, long millis) {
    this.viewer = viewer;
    this.eval = eval;
    this.millis = millis;
    eval.scriptLevel--;
    eval.pushContext2(null);
    sc = eval.thisContext;
    sc.pc++;
  }

  @Override
  public void run() {

    long timeBegin = System.currentTimeMillis();
    int delayMax;
    if (millis < 0)
      millis = -millis;
    else if ((delayMax = viewer.getDelayMaximum()) > 0 && millis > delayMax)
      millis = delayMax;
    millis -= System.currentTimeMillis() - timeBegin;

    /**
     * when the timeout has completed, we need to call eval.resumeEval(sc, false)
     * 
     * @j2sNative
     *
     * this.viewer.applet._delay(this.eval, this.sc, this.millis);
     * 
     */

    {
      int seconds = (int) millis / 1000;
      millis -= seconds * 1000;
      if (millis <= 0)
        millis = 1;
      while (seconds >= 0 && millis > 0 && !eval.interruptExecution
          && eval.currentThread == Thread.currentThread()) {
        viewer.popHoldRepaintWhy("delay");
        try {
          Thread.sleep((seconds--) > 0 ? 1000 : millis);
        } catch (InterruptedException e) {
        }
        viewer.pushHoldRepaintWhy("delay");
      }
    }
  }

  public void reset() {
    interrupt();
  }

}