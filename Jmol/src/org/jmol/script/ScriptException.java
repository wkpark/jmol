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

import org.jmol.util.Logger;

public class ScriptException extends Exception {

  protected final ScriptEvaluator eval;
  protected String message;
  private String untranslated;

  ScriptException(ScriptEvaluator scriptEvaluator, String msg, String untranslated, boolean isError) {
    eval = scriptEvaluator;
    message = msg;
    if (!isError) // ScriptInterruption
      return;
    eval.errorType = msg;
    eval.iCommandError = eval.pc;
    this.untranslated = (untranslated == null ? msg : untranslated);
    if (message == null) {
      message = "";
      return;
    }
    String s = eval.getScriptContext().getContextTrace(null, true).toString();
    while (eval.thisContext != null && !eval.thisContext.isTryCatch)
      eval.popContext(false, false);
    message += s;
    this.untranslated += s;
    if (eval.thisContext != null || eval.isSyntaxCheck
        || msg.indexOf("file recognized as a script file:") >= 0)
      return;
    Logger.error("eval ERROR: " + toString());
    if (eval.viewer.autoExit)
      eval.viewer.exitJmol();
  }

  protected String getErrorMessageUntranslated() {
    return untranslated;
  }

  @Override
  public String toString() {
    return message;
  }
}