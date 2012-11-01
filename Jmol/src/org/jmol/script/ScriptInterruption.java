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

package org.jmol.script;

class ScriptInterruption extends ScriptException {
  public int delayMs;
  public long targetTime;
  private ScriptContext sc;

  ScriptInterruption(ScriptEvaluator eval, int delayMs) {
    super(eval, (eval.viewer.autoExit ? "Interruption Error" : null), null, eval.viewer.autoExit);
    if (eval.viewer.autoExit)
      return;
    this.delayMs = delayMs;
    this.targetTime = System.currentTimeMillis() + delayMs;
    try {
      eval.scriptLevel--;
      eval.pushContext(null);
      sc = eval.thisContext;
    } catch (ScriptException e) {
      // unattainable
    }
  }
  
  public void resumeExecution() throws ScriptException {
    eval.resume(sc);
  }
}