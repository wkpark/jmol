/* $Author: hansonr $
 * $Date: 2007-09-09 21:37:07 -0500 (Sun, 09 Sep 2007) $
 * $Revision: 8231 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Vector;

class ScriptFunction {

  // / functions

  /*
   * functions are either local or global (static). The idea there is that a set
   * of applets might share a set of functions. The default is global; prefix
   * underscore makes them local.
   * 
   * functions have contexts. Or, more specifically, contexts may have associated
   * functions.
   * 
   * Bob Hanson -- 11.3.29
   */

  int pt0;
  int chpt0;
  int cmdpt0 = -1;
  String name;
  String script;
  Token[][] aatoken;
  short[] lineNumbers;
  int[] lineIndices;
  int nParameters;
  Vector names = new Vector();
  ScriptVariable returnValue;

  ScriptFunction(String name) {
    this.name = name;
  }

  void setVariables(Hashtable contextVariables, Vector params) {
    int nParams = (params == null ? 0 : params.size());
    for (int i = names.size(); --i >= 0;) {
      String name = ((String) names.get(i)).toLowerCase();
      contextVariables.put(name, (i < nParameters && i < nParams ? 
          new ScriptVariable().set((ScriptVariable) params.get(i)) 
          : (new ScriptVariable(Token.string, "")).setName(name)));
    }
    contextVariables.put("_retval", ScriptVariable.intVariable(0));
  }

  void addVariable(String name, boolean isParameter) {
    names.add(name);
    if (isParameter)
      nParameters++;
  }

  public String toString() {
    StringBuffer s = new StringBuffer("/*\n * ");
    s.append(name).append("\n */\nfunction ").append(name).append("(");
    for (int i = 0; i < nParameters; i++) {
      if (i > 0)
        s.append(", ");
      s.append(names.get(i));
    }
    s.append(") {\n");
    if (script != null)
      s.append(script);
    if (script == null || script.length() > 0
        && script.charAt(script.length() - 1) != '\n')
      s.append("\n");
    s.append("}\n\n");
    return s.toString();
  }

  static void setFunction(ScriptFunction function, String script,
                          int ichCurrentCommand, int pt, short[] lineNumbers,
                          int[] lineIndices, Vector lltoken) {
    int cmdpt0 = function.cmdpt0;
    int chpt0 = function.chpt0;
    int nCommands = pt - cmdpt0;
    function.script = script.substring(chpt0, ichCurrentCommand);
    Token[][] aatoken = function.aatoken = new Token[nCommands][];
    function.lineIndices = new int[nCommands];
    function.lineNumbers = new short[nCommands];
    short line0 = (short) (lineNumbers[cmdpt0] - 1);
    for (int i = 0; i < nCommands; i++) {
      function.lineNumbers[i] = (short) (lineNumbers[cmdpt0 + i] - line0);
      function.lineIndices[i] = lineIndices[cmdpt0 + i] - chpt0;
      aatoken[i] = (Token[]) lltoken.get(cmdpt0 + i);
      if (aatoken[i].length > 0) {
        Token tokenCommand = aatoken[i][0];
        if (Token.tokAttr(tokenCommand.tok, Token.flowCommand))
          tokenCommand.intValue -= (tokenCommand.intValue < 0 ? -cmdpt0
              : cmdpt0);
      }
    }
    for (int i = pt; --i >= cmdpt0;) {
      lltoken.remove(i);
      lineIndices[i] = 0;
    }
  }

}
