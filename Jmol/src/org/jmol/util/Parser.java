/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package org.jmol.util;

import javajs.util.ArrayUtil;
import javajs.util.ParserJS;


public class Parser {

  public final static float FLOAT_MIN_SAFE = 2E-45f; 
  // Float.MIN_ VALUE is not reliable with JavaScript because of the float/double difference there
  
  /// general static string-parsing class ///

  // next[0] tracks the pointer within the string so these can all be static.
  // but the methods parseFloat, parseInt, parseToken, parseTrimmed, and getTokens do not require this.

  public static String fixDataString(String str) {
    str = str.replace(';', str.indexOf('\n') < 0 ? '\n' : ' ');
    str = Txt.trim(str, "\n \t");
    str = Txt.simpleReplace(str, "\n ", "\n");
    str = Txt.simpleReplace(str, "\n\n", "\n");
    return str;    
  }
  
//  public static String concatTokens(String[] tokens, int iFirst, int iEnd) {
//    String str = "";
//    String sep = "";
//    for (int i = iFirst; i < iEnd; i++) {
//      if (i < tokens.length) {
//        str += sep + tokens[i];
//        sep = " ";
//      }
//    }
//    return str;
//  }
  
  public static String getQuotedStringAt(String line, int ipt0) {
    int[] next = new int[] { ipt0 };
    return getQuotedStringNext(line, next);
  }
  
  public static String getQuotedStringNext(String line, int[] next) {
    String value = line;
    int i = next[0];
    if (i < 0 || (i = value.indexOf("\"", i)) < 0)
      return "";
    next[0] = ++i;
    value = value.substring(i);
    i = -1;
    while (++i < value.length() && value.charAt(i) != '"')
      if (value.charAt(i) == '\\')
        i++;
    next[0] += i + 1;
    return value.substring(0, i);
  }
  
  public static boolean isOneOf(String key, String semiList) {
    if (semiList.length() == 0)
      return false;
    if (semiList.charAt(0) != ';')
      semiList = ";" + semiList + ";";
    return key.indexOf(";") < 0  && semiList.indexOf(';' + key + ';') >= 0;
  }

  public static String getQuotedAttribute(String info, String name) {
    int i = info.indexOf(name + "=");
    return (i < 0 ? null : getQuotedStringAt(info, i));
  }

  public static float approx(float f, float n) {
    return Math.round (f * n) / n;
  }

  public static float[][] parseFloatArray2d(String str) {
    str = fixDataString(str);
    int[] lines = Parser.markLines(str, '\n');
    int nLines = lines.length;
    float[][] data = ArrayUtil.newFloat2(nLines);
    for (int iLine = 0, pt = 0; iLine < nLines; pt = lines[iLine++]) {
      String[] tokens = ParserJS.getTokens(str.substring(pt, lines[iLine]));
      ParserJS.parseFloatArrayData(tokens, data[iLine] = new float[tokens.length]);
    }
    return data;
  }

  public static float[][][] parseFloatArray3d(String str) {
    str = fixDataString(str);
    int[] lines = Parser.markLines(str, '\n');
    int nLines = lines.length;
    String[] tokens = ParserJS.getTokens(str.substring(0, lines[0]));
    if (tokens.length != 3)
      return new float[0][0][0];
    int nX = ParserJS.parseInt(tokens[0]);
    int nY = ParserJS.parseInt(tokens[1]);
    int nZ = ParserJS.parseInt(tokens[2]);
    if (nX < 1 || nY < 1 || nZ < 1)
      return new float[1][1][1];
    float[][][] data = ArrayUtil.newFloat3(nX, nY);
    int iX = 0;
    int iY = 0;
    for (int iLine = 1, pt = lines[0]; iLine < nLines && iX < nX; pt = lines[iLine++]) {
      tokens = ParserJS.getTokens(str.substring(pt, lines[iLine]));
      if (tokens.length < nZ)
        continue;
      ParserJS.parseFloatArrayData(tokens, data[iX][iY] = new float[tokens.length]);
      if (++iY == nY) {
        iX++;
        iY = 0;
      } 
    }
    if (iX != nX) {
      Logger.info("Error reading 3D data -- nX = " + nX + ", but only " + iX + " blocks read");      
      return new float[1][1][1];
    }
    return data;
  }

  public static int[] markLines(String data, char eol) {
    int nLines = 0;
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        nLines++;
    int[] lines = new int[nLines + 1];
    nLines = 0;
    int pt = 0;
    while ((pt = data.indexOf(eol, pt)) >= 0)
      lines[nLines++] = ++pt;
    lines[nLines] = data.length();
    return lines;
  }

}
