/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.viewer.Token;

/**
 * For defining the state, mostly
 * 
 */
public class Escape {

  public static String escape(Object x) {
    if (x instanceof String)
      return Escape.escape("" + x);
    return x.toString();
  }

  public static String escapeColor(int argb) {
    return "[x" + Graphics3D.getHexColorFromRGB(argb) + "]";
  }

  public static String escape(Point4f xyzw) {
    return "{" + xyzw.x + " " + xyzw.y + " " + xyzw.z + " " + xyzw.w + "}";
  }

  public static String escape(Tuple3f xyz) {
    return "{" + xyz.x + " " + xyz.y + " " + xyz.z +"}";
  }

  public static String escape(float[] f) {
   StringBuffer sb = new StringBuffer();
   for (int i = 0; i < f.length; i++) {
     if (i > 0)
       sb.append('\n');
     sb.append(""+f[i]);
   }
   return sb.toString();
  }

  public static String escape(String str) {
    if (str == null)
      return "\"\"";
    int pt = -2;
    while ((pt = str.indexOf("\"", pt + 2)) >= 0)
      str = str.substring(0, pt) + '\\' + str.substring(pt);
    str = str.replace('\n','\1');
    str = TextFormat.simpleReplace(str, "\1", "\\n");
    for (int i = str.length(); --i >= 0;)
      if (str.charAt(i) > 0x7F)
        str = str.substring(0, i) + unicode(str.charAt(i))
            + str.substring(i + 1);
    return "\"" + str + "\"";
  }

  
  static String ESCAPE_SET = " ,./;:_+-~=><?'!@#$%^&*";
  static int nEscape = ESCAPE_SET.length();

  /**
   * Serialize a simple string-based array as a single 
   * string followed by a .split(x) where x is some character
   * not in the string. A bit kludgy, but it works. 
   * 
   * 
   * @param list list of strings to serialize
   * @return     serialized array
   */
  public static String escape(String[] list) {
    if (list == null || list.length == 0)
      return escape("");
    int pt = 0;
    char ch = ESCAPE_SET.charAt(0);
    for (int i = 0; i < list.length; i++) {
      String item = list[i];
      if (item.indexOf(ch) >= 0) {
        pt++;
        if (pt == nEscape)
          break;
        ch = ESCAPE_SET.charAt(pt);
        i = -1;
      }  
    }
    String sch = "" + ch;
    if (pt == nEscape)
      sch = "--" + Math.random() + "--";
    int nch = sch.length();
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < list.length; i++)
      s.append(sch).append(list[i]);
    return escape(s.toString().substring(nch))+".split(\"" + sch + "\")";
  }

  private static String unicode(char c) {
    String s = "0000" + Integer.toHexString(c);
    return "\\u" + s.substring(s.length() - 4);
  }
  
  public static Object unescapePointOrBitsetAsToken(String s) {
    if (s == null || s.length() == 0)
      return s;
    Object v = s;
    if (s.charAt(0) == '{')
      v = Escape.unescapePoint(s);
    else if (s.indexOf("({") == 0)
      v = Escape.unescapeBitset(s);
    if (s.indexOf("[{") == 0)
      v = new BondSet(Escape.unescapeBitset(s));
    if (v instanceof Point3f)
      return new Token(Token.point3f, v);
    if (v instanceof Point4f)
      return new Token(Token.point4f, v);
    if (v instanceof BitSet)
      return new Token(Token.bitset, v);
    return s;
    
    

  }
  
  public static Object unescapePoint(String strPoint) {
    if (strPoint == null || strPoint.length() == 0 
        || strPoint.charAt(0) != '{' || strPoint.charAt(strPoint.length() - 1) != '}')
      return strPoint;
    float[] points = new float[5];
    int nPoints = 0;
    String str = strPoint.substring(1, strPoint.length() - 1);
    int[] next = new int[1];
    for (; nPoints < 5;nPoints++) {
      points[nPoints] = Parser.parseFloat(str, next);
      if (Float.isNaN(points[nPoints])) {
        if (next[0] >= str.length() || str.charAt(next[0]) != ',')
          break;
        next[0]++;
        nPoints--;
      }
    }
    if (nPoints == 3)
      return new Point3f(points[0], points[1], points[2]);
    if (nPoints == 4)
      return new Point4f(points[0], points[1], points[2], points[3]);
    return strPoint;    
  }

  public static BitSet unescapeBitset(String strBitset) {
    if (strBitset == "{null}")
      return null;
    BitSet bs = new BitSet();
    int len = strBitset.length();
    int iPrev = -1;
    int iThis = -2;
    char ch;
    if (len < 3)
      return bs;
    for (int i = 0; i < len; i++) {
      switch (ch = strBitset.charAt(i)) {
      case '}':
      case '{':
      case ' ':
        if (iThis < 0)
          break;
        if (iPrev < 0) 
          iPrev = iThis;
        for (int j = iPrev; j<= iThis; j++)
          bs.set(j);
        iPrev = -1;
        iThis = -2;
        break;
      case ':':
        iPrev = iThis;
        iThis = -2;
        break;
      default:
        if (Character.isDigit(ch)) {
          if (iThis < 0)
            iThis = 0;
          iThis = (iThis << 3) + (iThis << 1) + (ch - '0');
        }
      }
    }
    return bs;
  }

  public static String escape(BitSet bs, boolean isAtoms) {
    char chOpen = (isAtoms ? '(' : '[');   
    char chClose = (isAtoms ? ')' : ']');   
    if (bs == null)
      return chOpen + "{}" + chClose;
    StringBuffer s = new StringBuffer(chOpen + "{");
    int imax = bs.size();
    int iLast = -1;
    int iFirst = -2;
    int i = -1;
    while (++i <= imax) {
      boolean isSet = bs.get(i);
      if (i == imax || iLast >= 0 && !isSet) {
        if (iLast >= 0 && iFirst != iLast)
          s.append((iFirst == iLast - 1 ? " " : ":") + iLast);
        if (i == imax)
          break;
        iLast = -1;
      }
      if (bs.get(i)) {
        if (iLast < 0) {
          s.append((iFirst == -2 ? "" : " ") + i);
          iFirst = i;
        }
        iLast = i;
      }
    }
    s.append("}").append(chClose);
    return s.toString();
  }

  public static String escape(BitSet bs) {
    return escape(bs, true);
  }
}
