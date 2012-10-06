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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.jmol.script.ScriptVariable;



public class Escape {

  public static String escapeColor(int argb) {
    return "[x" + getHexColorFromRGB(argb) + "]";
  }

  public static String getHexColorFromRGB(int argb) {
    if (argb == 0)
      return null;
    String r  = "00" + Integer.toHexString((argb >> 16) & 0xFF);
    r = r.substring(r.length() - 2);
    String g  = "00" + Integer.toHexString((argb >> 8) & 0xFF);
    g = g.substring(g.length() - 2);
    String b  = "00" + Integer.toHexString(argb & 0xFF);
    b = b.substring(b.length() - 2);
    return r + g + b;
  }

  
  /**
   * must be its own, because of the possibility of being null
   * @param xyz
   * @return  {x y z}
   */
  public static String escapePt(Tuple3f xyz) {
    if (xyz == null)
      return "null";
    return "{" + xyz.x + " " + xyz.y + " " + xyz.z + "}";
  }

  public static String matrixToScript(Object m) {
    return TextFormat.replaceAllCharacters(m.toString(), "\n\r ","").replace('\t',' ');
  }
  
  @SuppressWarnings("unchecked")
  public static String escape(Object x) {
    if (x instanceof String)
      return escapeStr((String) x);
    if (x instanceof List<?>)
      return escapeVar((ArrayList<ScriptVariable>) x);
    if (x instanceof BitSet) 
      return escapeBs((BitSet) x, true);
    if (x instanceof Matrix3f) 
      return TextFormat.simpleReplace(((Matrix3f) x).toString(), "\t", ",\t");
    if (x instanceof Matrix4f) 
      return TextFormat.simpleReplace(((Matrix4f) x).toString(), "\t", ",\t");
    if (x instanceof Tuple3f)
      return escapePt((Tuple3f) x);
    if (x instanceof Point4f) {
      Point4f xyzw = (Point4f) x;
      return "{" + xyzw.x + " " + xyzw.y + " " + xyzw.z + " " + xyzw.w + "}";
    }
    if (x instanceof AxisAngle4f) {
      AxisAngle4f a = (AxisAngle4f) x;
      return "{" + a.x + " " + a.y + " " + a.z + " " + (float) (a.angle * 180d/Math.PI) + "}";
    }    
    if (x instanceof String[])
      return escapeStrA((String[]) x, true);
    if (x instanceof int[] 
          || x instanceof int[][]
          || x instanceof float[]
          || x instanceof double[]
          || x instanceof float[][]
          || x instanceof float[][][]) 
      return toJSON(null, x);
    if (x instanceof Point3f[])
      return escapeArray(x);
    if (x instanceof Map)
      return escapeMap((Map<String, Object>) x);
    return (x == null ? "null" : x.toString());
  }

//static String ESCAPE_SET = " ,./;:_+-~=><?'!@#$%^&*";
//static int nEscape = ESCAPE_SET.length();

  private final static String escapable = "\\\\\tt\rr\nn\"\""; 

  public static String escapeStr(String str) {
    if (str == null)
      return "\"\"";
    boolean haveEscape = false;
    int i = 0;
    for (; i < escapable.length(); i += 2)
      if (str.indexOf(escapable.charAt(i)) >= 0) {
        haveEscape = true;
        break;
      }
    if (haveEscape)
      while (i < escapable.length()) {
        int pt = -1;
        char ch = escapable.charAt(i++);
        char ch2 = escapable.charAt(i++);
        StringXBuilder sb = new StringXBuilder();
        int pt0 = 0;
        while ((pt = str.indexOf(ch, pt + 1)) >= 0) {
          sb.append(str.substring(pt0, pt)).appendC('\\').appendC(ch2);
          pt0 = pt + 1;
        }
        sb.append(str.substring(pt0, str.length()));
        str = sb.toString();
      }
    for (i = str.length(); --i >= 0;)
      if (str.charAt(i) > 0x7F)
        str = str.substring(0, i) + unicode(str.charAt(i))
            + str.substring(i + 1);
    return "\"" + str + "\"";
  }

  private static String unicode(char c) {
    String s = "0000" + Integer.toHexString(c);
    return "\\u" + s.substring(s.length() - 4);
  }

  public static String escapeVar(ArrayList<ScriptVariable> list) {
    if (list == null)
      return escapeStr("");
    StringXBuilder s = new StringXBuilder();
    s.append("[");
    for (int i = 0; i < list.size(); i++) {
      if (i > 0)
        s.append(", ");
      s.append(escapeNice(list.get(i).asString()));
    }
    s.append("]");
    return s.toString();
  }

  public static String escapeMap(Map<String, Object> ht) {
    StringXBuilder sb = new StringXBuilder();
    sb.append("{ ");
    String sep = "";
    for (Map.Entry<String, Object> entry : ht.entrySet()) {
      String key = entry.getKey();
      sb.append(sep).append(escapeStr(key)).appendC(':');
      Object val = entry.getValue();
      if (!(val instanceof ScriptVariable))
        val = ScriptVariable.getVariable(val);
      sb.append(((ScriptVariable)val).escape());
      sep = ","; 
    }
    sb.append(" }");
    return sb.toString();
  }
  
  /**
   * 
   * @param f
   * @param asArray -- FALSE allows bypassing of escape(Object f); TRUE: unnecssary
   * @return tabular string
   */
  public static String escapeFloatA(float[] f, boolean asArray) {
    if (asArray)
      return toJSON(null, f); // or just use escape(f)
    StringXBuilder sb = new StringXBuilder();
    for (int i = 0; i < f.length; i++) {
      if (i > 0)
        sb.appendC('\n');
      sb.appendF(f[i]);
    }
    return sb.toString();
  }

  public static String escapeFloatAA(float[][] f, boolean addSemi) {
    StringXBuilder sb = new StringXBuilder();
    String eol = (addSemi ? ";\n" : "\n");
    for (int i = 0; i < f.length; i++)
      if (f[i] != null) {
        if (i > 0)
          sb.append(eol);
        for (int j = 0; j < f[i].length; j++)
          sb.appendF(f[i][j]).appendC('\t');
      }
    return sb.toString();
  }

  public static String escapeFloatAAA(float[][][] f, boolean addSemi) {
    StringXBuilder sb = new StringXBuilder();
    String eol = (addSemi ? ";\n" : "\n");
    if (f[0] == null || f[0][0] == null)
      return "0 0 0" + eol;
    sb.appendI(f.length).append(" ")
      .appendI(f[0].length).append(" ")
      .appendI(f[0][0].length);
    for (int i = 0; i < f.length; i++)
      if (f[i] != null) {
        sb.append(eol);
        for (int j = 0; j < f[i].length; j++)
          if (f[i][j] != null) {
            sb.append(eol);
            for (int k = 0; k < f[i][j].length; k++)
              sb.appendF(f[i][j][k]).appendC('\t');
          }
      }
    return sb.toString();
  }

  /**
   * 
   * @param list
   *          list of strings to serialize
   * @param nicely TODO
   * @return serialized array
   */
  public static String escapeStrA(String[] list, boolean nicely) {
    if (list == null)
      return escapeStr("");
    StringXBuilder s = new StringXBuilder();
    s.append("[");
    for (int i = 0; i < list.length; i++) {
      if (i > 0)
        s.append(", ");
      s.append(nicely ? escapeNice(list[i]) : escapeStr(list[i]));
    }
    s.append("]");
    return s.toString();
  }

  public static String escapeArray(Object x) {
    // from isosurface area or volume calc
    if (x == null)
      return escapeStr("");
    if (x instanceof Float)
      return "" + x;
    StringXBuilder s = new StringXBuilder();
    s.append("[");
    if (x instanceof double[]) {
      double[] dlist = (double[]) x;
      for (int i = 0; i < dlist.length; i++) {
        if (i > 0)
          s.append(", ");
        s.appendD(dlist[i]);
      }
    } else if (x instanceof float[]) {
      float[] flist = (float[]) x;
      for (int i = 0; i < flist.length; i++) {
        if (i > 0)
          s.append(", ");
        s.appendF(flist[i]);
      }
    } else if (x instanceof int[]) {
      int[] ilist = (int[]) x;
      for (int i = 0; i < ilist.length; i++) {
        if (i > 0)
          s.append(", ");
        s.appendI(ilist[i]);
      }
    } else if (x instanceof Point3f[]) {
      Point3f[] plist = (Point3f[]) x;
      s = new StringXBuilder();
      s.append("[");
      for (int i = 0; i < plist.length; i++) {
        if (i > 0)
          s.append(", ");
        s.append(escapePt(plist[i]));
      }
      return s.append("]").toString();
    }
    s.append("]");
    return s.toString();

  }

  private static String escapeNice(String s) {
    if (s == null)
      return "null";
    float f = Parser.parseFloatStrict(s);
    return (Float.isNaN(f) ? escapeStr(s) : s);
  }

  public static Object unescapePointOrBitsetOrMatrixOrArray(String s) {
    if (s.charAt(0) == '{')
      return unescapePoint(s);
    if ((isStringArray(s)
        || s.startsWith("[{") && s.indexOf("[{") == s.lastIndexOf("[{"))
        && s.indexOf(',') < 0 && s.indexOf('.') < 0 && s.indexOf('-') < 0)
      return unescapeBitset(s);
    if (s.startsWith("[["))
      return unescapeMatrix(s);
    return s;
  }

  public static boolean isStringArray(String s) {
    return s.startsWith("({") && s.lastIndexOf("({") == 0
        && s.indexOf("})") == s.length() - 2;
  }
  public static Object unescapePoint(String strPoint) {
    if (strPoint == null || strPoint.length() == 0)
      return strPoint;
    String str = strPoint.replace('\n', ' ').trim();
    if (str.charAt(0) != '{' || str.charAt(str.length() - 1) != '}')
      return strPoint;
    float[] points = new float[5];
    int nPoints = 0;
    str = str.substring(1, str.length() - 1);
    int[] next = new int[1];
    for (; nPoints < 5; nPoints++) {
      points[nPoints] = Parser.parseFloatNext(str, next);
      if (Float.isNaN(points[nPoints])) {
        if (next[0] >= str.length() || str.charAt(next[0]) != ',')
          break;
        next[0]++;
        nPoints--;
      }
    }
    if (nPoints == 3)
      return Point3f.new3(points[0], points[1], points[2]);
    if (nPoints == 4)
      return Point4f.new4(points[0], points[1], points[2], points[3]);
    return strPoint;
  }

  public static BitSet unescapeBitset(String str) {
      char ch;
      int len;
      if (str == null || (len = (str = str.trim()).length()) < 4
          || str.equalsIgnoreCase("({null})") 
          || (ch = str.charAt(0)) != '(' && ch != '[' 
          || str.charAt(len - 1) != (ch == '(' ? ')' : ']')
          || str.charAt(1) != '{' || str.indexOf('}') != len - 2)
        return null;
      len -= 2;
      for (int i = len; --i >= 2;)
        if (!Character.isDigit(ch = str.charAt(i)) && ch != ' ' && ch != '\t'
            && ch != ':')
          return null;
      int lastN = len;
      while (Character.isDigit(str.charAt(--lastN))) {
        // loop
      }
      if (++lastN == len)
        lastN = 0;
      else
        try {
          lastN = Integer.parseInt(str.substring(lastN, len));
        } catch (NumberFormatException e) {
          return null;
        }
      BitSet bs = BitSetUtil.newBitSet(lastN);
      lastN = -1;
      int iPrev = -1;
      int iThis = -2;
      for (int i = 2; i <= len; i++) {
        switch (ch = str.charAt(i)) {
        case '\t':
        case ' ':
        case '}':
          if (iThis < 0)
            break;
          if (iThis < lastN)
            return null;
          lastN = iThis;
          if (iPrev < 0)
            iPrev = iThis;
          bs.setBits(iPrev, iThis + 1);
          iPrev = -1;
          iThis = -2;
          break;
        case ':':
          iPrev = lastN = iThis;
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
      return (iPrev >= 0 ? null : bs);
    }

  public static Object unescapeMatrix(String strMatrix) {
    if (strMatrix == null || strMatrix.length() == 0)
      return strMatrix;
    String str = strMatrix.replace('\n', ' ').trim();
    if (str.lastIndexOf("[[") != 0 || str.indexOf("]]") != str.length() - 2)
      return strMatrix;
    float[] points = new float[16];
    str = str.substring(2, str.length() - 2).replace('[',' ').replace(']',' ').replace(',',' ');
    int[] next = new int[1];
    int nPoints = 0;
    for (; nPoints < 16; nPoints++) {
      points[nPoints] = Parser.parseFloatNext(str, next);
      if (Float.isNaN(points[nPoints])) {
        break;
      }
    }
    if (!Float.isNaN(Parser.parseFloatNext(str, next)))
      return strMatrix; // overflow
    if (nPoints == 9)
      return Matrix3f.newA(points);
    if (nPoints == 16)
      return Matrix4f.newA(points);
    return strMatrix;
  }
/*
  public static Object unescapeArray(String strArray) {
    if (strArray == null || strArray.length() == 0)
      return strArray;
    String str = strArray.replace('\n', ' ').replace(',', ' ').trim();
    if (str.lastIndexOf("[") != 0 || str.indexOf("]") != str.length() - 1)
      return strArray;
    float[] points = Parser.parseFloatArray(str);
    for (int i = 0; i < points.length; i++)
      if (Float.isNaN(points[i]))
        return strArray;
    return points;
  }
*/
  public static String escapeBs(BitSet bs, boolean isAtoms) {
    char chOpen = (isAtoms ? '(' : '[');
    char chClose = (isAtoms ? ')' : ']');
    if (bs == null)
      return chOpen + "{}" + chClose;
    StringXBuilder s = new StringXBuilder();
    s.append(chOpen + "{");
    int imax = bs.length();
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
    s.append("}").appendC(chClose);
    return s.toString();
  }

  private static String packageJSONSb(String infoType, StringXBuilder sb) {
    return packageJSON(infoType, sb.toString());
  }

  private static String packageJSON(String infoType, String info) {
    if (infoType == null)
      return info;
    return "\"" + infoType + "\": " + info;
  }

  private static String fixString(String s) {
    if (s == null || s.indexOf("{\"") == 0) //don't doubly fix JSON strings when retrieving status
      return s;
    s = TextFormat.simpleReplace(s, "\"", "''");
    s = TextFormat.simpleReplace(s, "\n", " | ");
    return "\"" + s + "\"";
  }

  @SuppressWarnings("unchecked")
  public static String toJSON(String infoType, Object info) {

    //Logger.debug(infoType+" -- "+info);

    StringXBuilder sb = new StringXBuilder();
    String sep = "";
    if (info == null)
      return packageJSON(infoType, (String) null);
    if (info instanceof Integer || info instanceof Float || info instanceof Double)
      return packageJSON(infoType, info.toString());
    if (info instanceof String)
      return packageJSON(infoType, fixString((String) info));
    if (info instanceof String[]) {
      sb.append("[");
      int imax = ((String[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(fixString(((String[]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof int[]) {
      sb.append("[");
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendI(((int[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof float[]) {
      sb.append("[");
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendF(((float[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof double[]) {
      sb.append("[");
      int imax = ((double[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendD(((double[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof Point3f[]) {
      sb.append("[");
      int imax = ((Point3f[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep);
        addJsonTuple(sb, ((Point3f[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof String[][]) {
      sb.append("[");
      int imax = ((String[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((String[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof int[][]) {
      sb.append("[");
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((int[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof float[][]) {
      sb.append("[");
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((float[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof float[][][]) {
      sb.append("[");
      int imax = ((float[][][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((float[][][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof List) {
      sb.append("[ ");
      int imax = ((List<?>) info).size();
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((List<?>) info).get(i)));
        sep = ",";
      }
      sb.append(" ]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof Matrix4f) {
      float[] x = new float[4];
      Matrix4f m4 = (Matrix4f) info;
      sb.appendC('[');
      for (int i = 0; i < 4; i++) {
        if (i > 0)
          sb.appendC(',');
        m4.getRow(i, x);
        sb.append(toJSON(null, x));
      }
      sb.appendC(']');
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof Matrix3f) {
      float[] x = new float[3];
      Matrix3f m3 = (Matrix3f) info;
      sb.appendC('[');
      for (int i = 0; i < 3; i++) {
        if (i > 0)
          sb.appendC(',');
        m3.getRow(i, x);
        sb.append(toJSON(null, x));
      }
      sb.appendC(']');
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof Tuple3f) {
      addJsonTuple(sb, (Tuple3f) info);
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof AxisAngle4f) {
      sb.append("[")
      .appendF(((AxisAngle4f) info).x).append(",")
      .appendF(((AxisAngle4f) info).y).append(",")
      .appendF(((AxisAngle4f) info).z).append(",")
      .appendF((float)(((AxisAngle4f) info).angle * 180d/Math.PI)).append("]");
    return packageJSONSb(infoType, sb);
    }
    if (info instanceof Point4f) {
      sb.append("[")
        .appendF(((Point4f) info).x).append(",")
        .appendF(((Point4f) info).y).append(",")
        .appendF(((Point4f) info).z).append(",")
        .appendF(((Point4f) info).w).append("]");
      return packageJSONSb(infoType, sb);
    }
    if (info instanceof Map) {
      sb.append("{ ");
      Iterator<String> e = ((Map<String, ?>) info).keySet().iterator();
      while (e.hasNext()) {
        String key = e.next();
        sb.append(sep)
            .append(packageJSON(key, toJSON(null, ((Map<?, ?>) info).get(key))));
        sep = ",";
      }
      sb.append(" }");
      return packageJSONSb(infoType, sb);
    }
    return packageJSON(infoType, fixString(info.toString()));
  }

  private static void addJsonTuple(StringXBuilder sb, Tuple3f pt) {
    sb.append("[")
    .appendF(pt.x).append(",")
    .appendF(pt.y).append(",")
    .appendF(pt.z).append("]");
  }

  public static String toReadableNoName(Object info) {
    return toReadable(null, info);
  }

  public static String toReadable(String name, Object info) {
    StringXBuilder sb =new StringXBuilder();
    String sep = "";
    if (info == null)
      return "null";
    if (info instanceof String)
      return packageReadable(name, null, escapeStr((String) info));
    if (info instanceof String[]) {
      sb.append("[");
      int imax = ((String[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(escapeStr(((String[]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageReadableSb(name, "String[" + imax + "]", sb);
    }
    if (info instanceof int[]) {
      sb.append("[");
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendI(((int[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageReadableSb(name, "int[" + imax + "]", sb);
    }
    if (info instanceof float[]) {
      sb.append("[");
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendF(((float[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageReadableSb(name, "float[" + imax + "]", sb);
    }
    if (info instanceof Point3f[]) {
      sb.append("[");
      int imax = ((Point3f[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(escapePt(((Point3f[])info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageReadableSb(name, "point3f[" + imax + "]", sb);
    }
    if (info instanceof String[][]) {
      sb.append("[");
      int imax = ((String[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((String[][]) info)[i]));
        sep = ",\n";
      }
      sb.append("]");
      return packageReadableSb(name, "String[" + imax + "][]", sb);
    }
    if (info instanceof int[][]) {
      sb.append("[");
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((int[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageReadableSb(name, "int[" + imax + "][]", sb);
    }
    if (info instanceof float[][]) {
      sb.append("[\n");
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((float[][]) info)[i]));
        sep = ",\n";
      }
      sb.append("]");
      return packageReadableSb(name, "float[][]", sb);
    }
    if (info instanceof List<?>) {
      int imax = ((List<?>) info).size();
      for (int i = 0; i < imax; i++) {
        sb.append(toReadable(name + "[" + (i + 1) + "]", ((List<?>) info).get(i)));
      }
      return packageReadableSb(name, "List[" + imax + "]", sb);
    }
    if (info instanceof Matrix3f
        || info instanceof Tuple3f
        || info instanceof Point4f
        || info instanceof AxisAngle4f) {
      sb.append(escape(info));
      return packageReadableSb(name, null, sb);
    }
    if (info instanceof Map<?, ?>) {
      Iterator<?> e = ((Map<?, ?>) info).keySet().iterator();
      while (e.hasNext()) {
        String key = (String) e.next();
        sb.append(toReadable((name == null ? "" : name + ".") + key,
            ((Map<?, ?>) info).get(key)));
      }
      return sb.toString();
    }
    return packageReadable(name, null, info.toString());
  }

  private static String packageReadableSb(String infoName, String infoType,
                                        StringXBuilder sb) {
    return packageReadable(infoName, infoType, sb.toString());
  }
  
  private static String packageReadable(String infoName, String infoType,
                                        String info) {
    String s = (infoType == null ? "" : infoType + "\t");
    if (infoName == null)
      return s + info;
    return "\n" + infoName + "\t" + (infoType == null ? "" : "*" + infoType + "\t") + info;
  }

  public static String escapeModelFileNumber(int iv) {
    return "" + (iv / 1000000) + "." + (iv % 1000000);
  }

  public static String encapsulateData(String name, Object data) {
    return "  DATA \"" + name + "\"\n" + 
        (data instanceof float[][] ?
          escapeFloatAA((float[][]) data, true) + ";\n"
          : data instanceof float[][][] ?
              escapeFloatAAA((float[][][]) data, true) + ";\n"
          : data) + "    END \"" + name + "\";\n";
  }

  public static String escapeXml(Object value) {
    if (value instanceof String)
      return XmlUtil.wrapCdata(value.toString());
    String s = "" + value;
    if (s.length() == 0 || s.charAt(0) != '[')
      return s;
    return XmlUtil.wrapCdata(toReadable(null, value));
  }

  public static String unescapeUnicode(String s) {
    int ichMax = s.length();
    StringXBuilder sb = StringXBuilder.newN(ichMax);
    int ich = 0;
    while (ich < ichMax) {
      char ch = s.charAt(ich++);
      if (ch == '\\' && ich < ichMax) {
        ch = s.charAt(ich++);
        switch (ch) {
        case 'u':
          if (ich < ichMax) {
            int unicode = 0;
            for (int k = 4; --k >= 0 && ich < ichMax;) {
              char chT = s.charAt(ich);
              int hexit = getHexitValue(chT);
              if (hexit < 0)
                break;
              unicode <<= 4;
              unicode += hexit;
              ++ich;
            }
            ch = (char) unicode;
          }
        }
      }
      sb.appendC(ch);
    }
    return sb.toString();
  }
  
  public static int getHexitValue(char ch) {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return 10 + ch - 'a';
    else if (ch >= 'A' && ch <= 'F')
      return 10 + ch - 'A';
    else
      return -1;
  }

  public static String[] unescapeStringArray(String data) {
    //TODO -- should recognize '..' as well as "..." ?
    if (data == null || !data.startsWith("[") || !data.endsWith("]"))
      return null;
    List<String> v = new ArrayList<String>();
    int[] next = new int[1];
    next[0] = 1;
    while (next[0] < data.length()) {
      String s = Parser.getQuotedStringNext(data, next);
      if (s == null)
        return null;
      v.add(s);
      while (next[0] < data.length() && data.charAt(next[0]) != '"')
        next[0]++;
    }    
    return v.toArray(new String[v.size()]);
  }

  public static String escapeUrl(String url) {
    url = TextFormat.simpleReplace(url, "\n", "");
    url = TextFormat.simpleReplace(url, "%", "%25");
    url = TextFormat.simpleReplace(url, "[", "%5B");
    url = TextFormat.simpleReplace(url, "]", "%5D");
    url = TextFormat.simpleReplace(url, " ", "%20");
    return url;
  }
}
