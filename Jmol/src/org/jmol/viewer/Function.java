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

class Function {
  int pt0;
  int chpt0;
  int cmdpt0= -1;
  String name;
  String script;
  Token[][] aatoken;
  short[] lineNumbers;
  int[] lineIndices;
  int nParameters;
  Vector names = new Vector();
  Token returnValue;
  
  Function(String name) {
    this.name = name;
  }
  
  void setVariables(Hashtable contextVariables, Vector params) {
    int nParams = (params == null ? 0 : params.size());
    for (int i = names.size(); --i >= 0; )
      contextVariables.put((String)names.get(i), 
          (i < nParameters && i < nParams ? params.get(i) : new Token(Token.string, "")));
      contextVariables.put("_retval", Token.intToken(0));
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
    s.append (");\n");
    s.append(script);
    if (script.length() > 0 && script.charAt(script.length() - 1) != '\n')
      s.append("\n");
    s.append("end function;\n\n");
    return s.toString();
  }  
}
