/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-05 07:42:12 -0500 (Fri, 05 Jun 2009) $
 * $Revision: 10958 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;

public class Variable extends Token {

  final private static Variable vT = new Variable(on, 1, "true");
  final private static Variable vF = new Variable(off, 0, "false");
  public static Variable vAll = new Variable(all, "all");

  public int index = Integer.MAX_VALUE;

  public String name;

  private final static int FLAG_CANINCREMENT = 1;
  private final static int FLAG_LOCALVAR = 2;

  public int flags = ~FLAG_CANINCREMENT & FLAG_LOCALVAR;

  public Variable() {
    tok = string;
    value = "";

  }

  public Variable(int tok) {
    this.tok = tok;
  }

  public Variable(int tok, int intValue, Object value) {
    super(tok, intValue, value);
  }

  public Variable(int tok, Object value) {
    super(tok, value);
  }

  public Variable(int tok, int intValue) {
    super(tok, intValue);
  }

  public Variable(BitSet bs, int index) {
    value = bs;
    this.index = index;
    tok = bitset;
  }

  public Variable(Token theToken) {
    tok = theToken.tok;
    intValue = theToken.intValue;
    value = theToken.value;
  }

  public static Variable getVariable(Object x) {
    if (x == null)
      return null;
    if (x instanceof Variable)
      return (Variable) x;
    if (x instanceof String) 
      x = Escape.unescapePointOrBitsetAsVariable((String) x);
    if (x instanceof Boolean)
      return getBoolean(((Boolean)x).booleanValue());
    if (x instanceof Integer)
      return new Variable(integer, ((Integer) x).intValue());
    if (x instanceof Float)
      return new Variable(decimal, x);
    if (x instanceof float[]) {
      float[] f = (float[]) x;
      String[] s = new String[f.length];
      for (int i = f.length; --i >= 0; )
        s[i] = "" + f[i];
      return new Variable(list, s);
    }
    if (x instanceof String[])
      return new Variable(list, x);
    if (x instanceof String)
      return new Variable(string, x);
    if (x instanceof Vector3f)
      return new Variable(point3f, new Point3f((Vector3f) x));
    if (x instanceof Point3f)
      return new Variable(point3f, x);
    if (x instanceof Point4f)
      return new Variable(point4f, x);
    if (x instanceof BitSet)
      return new Variable(bitset, x);
    if (x instanceof Quaternion)
      return new Variable(point4f, ((Quaternion)x).toPoint4f());
    return null;
  }

  public Variable set(Variable v) {
    index = v.index;
    intValue = v.intValue;
    tok = v.tok;
    if (tok == Token.list) {
      int n = ((String[])v.value).length;
      value = new String[n];
      System.arraycopy(v.value, 0, value, 0, n);
    } else {
      value = v.value;
    }
    return this;
  }

  public Variable setName(String name) {
    this.name = name;
    flags |= FLAG_CANINCREMENT;
    //System.out.println("Variable: " + name + " " + intValue + " " + value);
    return this;
  }

  public Variable setGlobal() {
    flags &= ~FLAG_LOCALVAR;
    return this;
  }

  public boolean canIncrement() {
    return tokAttr(flags, FLAG_CANINCREMENT);
  }

  public boolean increment(int n) {
    if (!canIncrement())
      return false;
    switch (tok) {
    case integer:
      intValue += n;
      break;
    case decimal:
      value = new Float(((Float) value).floatValue() + n);
      break;
    default:
      value = nValue(this);
      if (value instanceof Integer) {
        tok = integer;
        intValue = ((Integer) value).intValue();
      } else {
        tok = decimal;
      }
    }
    return true;
  }

  public static Variable getVariableSelected(int index, Object value) {
    Variable v = new Variable(bitset, value);
    v.index = index;
    return v;
  }

  public boolean asBoolean() {
    return bValue(this);
  }

  public int asInt() {
    return iValue(this);
  }

  public float asFloat() {
    return fValue(this);
  }

  public String asString() {
    return sValue(this);
  }

  public Object getValAsObj() {
    return (tok == integer ? new Integer(intValue) : value);
  }

  // math-related Token static methods

  final static Point3f pt0 = new Point3f();

  final public static Variable intVariable(int intValue) {
    return new Variable(integer, intValue);
  }

  static Object oValue(Variable x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return new Integer(x.intValue);
    case string:
      //return tValue((String) x.value).value;
    default:
      return x.value;
    }
  }

  static Object nValue(Token x) {
    int iValue = 0;
    switch (x == null ? nada : x.tok) {
    case integer:
      iValue = x.intValue;
      break;
    case decimal:
      return x.value;
    case string:
      if (((String) x.value).indexOf(".") >= 0)
        return new Float(toFloat((String) x.value));
      iValue = (int) toFloat((String) x.value);
    }
    return new Integer(iValue);
  }

  static boolean bValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case list:
      return fValue(x) != 0;
    case bitset:
      return iValue(x) != 0;
    case point3f:
    case point4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  static int iValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case list:
    case string:
    case point3f:
    case point4f:
      return (int) fValue(x);
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static float fValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case list:
      int i = x.intValue;
      String[] list = (String[]) x.value;
      if (i == Integer.MAX_VALUE)
        return list.length;
    case string:
      return toFloat(sValue(x));
    case bitset:
      return iValue(x);
    case point3f:
      return ((Point3f) x.value).distance(pt0);
    case point4f:
      return Graphics3D.distanceToPlane((Point4f) x.value, pt0);
    default:
      return 0;
    }
  }

  static float toFloat(String s) {
    if (s.equalsIgnoreCase("true"))
      return 1;
    if (s.equalsIgnoreCase("false") || s.length() == 0)
      return 0;
    return Parser.parseFloatStrict(s);
  }

  static String sValue(Token x) {
    if (x == null)
      return "";
    int i;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case point3f:
      return Escape.escape((Point3f) x.value);
    case point4f:
      return Escape.escape((Point4f) x.value);
    case bitset:
      return Escape.escape(bsSelect(x), !(x.value instanceof BondSet));
    case list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i <= 0)
        i = list.length - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case decimal:
    default:
      return "" + x.value;
    }
  }

  static String sValue(Variable x) {
    if (x == null)
      return "";
    int i;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case point3f:
      return Escape.escape((Point3f) x.value);
    case point4f:
      return Escape.escape((Point4f) x.value);
    case bitset:
      return Escape.escape(bsSelect(x), !(x.value instanceof BondSet));
    case list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i <= 0)
        i = list.length - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case decimal:
    default:
      return "" + x.value;
    }
  }

  static int sizeOf(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case string:
      return ((String) x.value).length();
    case list:
      return x.intValue == Integer.MAX_VALUE ? ((String[]) x.value).length
          : sizeOf(selectItem(x));
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static String typeOf(Variable x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return "boolean";
    case integer:
      return "integer";
    case decimal:
      return "decimal";
    case point3f:
      return "point";
    case point4f:
      return "plane";
    case string:
      return "string";
    case list:
      return "array";
    case bitset:
      return "bitset";
    default:
      return "?";
    }
  }

  static String[] concatList(Variable x1, Variable x2) {
    String[] list1 = (x1.tok == list ? (String[]) x1.value : TextFormat.split(
        sValue(x1), "\n"));
    String[] list2 = (x2.tok == list ? (String[]) x2.value : TextFormat.split(
        sValue(x2), "\n"));
    String[] list = new String[list1.length + list2.length];
    int pt = 0;
    for (int i = 0; i < list1.length; i++)
      list[pt++] = list1[i];
    for (int i = 0; i < list2.length; i++)
      list[pt++] = list2[i];
    return list;
  }

  static BitSet bsSelect(Token token) {
    token = selectItem(token, Integer.MIN_VALUE);
    return (BitSet) token.value;
  }

  static BitSet bsSelect(Variable var) {
    if (var.index == Integer.MAX_VALUE)
      var = selectItem(var);
    return (BitSet) var.value;
  }

  static BitSet bsSelect(Token token, int n) {
    token = selectItem(token);
    token = selectItem(token, 1);
    token = selectItem(token, n);
    return (BitSet) token.value;
  }

  static Variable selectItem(Variable var) {
    if (var.index != Integer.MAX_VALUE)
      return var;
    return (Variable) selectItem(var, Integer.MIN_VALUE);
  }

  static Token selectItem(Token var) {
    return selectItem(var, Integer.MIN_VALUE);
  }

  static Variable selectItem(Variable var, int i2) {
    return (Variable) selectItem((Token) var, i2);
  }

  static Token selectItem(Token tokenIn, int i2) {
    if (tokenIn.tok != bitset && tokenIn.tok != list && tokenIn.tok != string)
      return tokenIn;

    // negative number is a count from the end

    BitSet bs = null;
    String[] st = null;
    String s = null;

    int i1 = tokenIn.intValue;
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      if (i2 == Integer.MIN_VALUE)
        i2 = i1;
      return new Variable(tokenIn.tok, i2, tokenIn.value);
    }
    int len = 0;
    boolean isInputSelected = (tokenIn instanceof Variable && ((Variable) tokenIn).index != Integer.MAX_VALUE);
    Variable tokenOut = new Variable(tokenIn.tok, Integer.MAX_VALUE);

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        tokenOut.value = new BondSet((BitSet) tokenIn.value,
            ((BondSet) tokenIn.value).getAssociatedAtoms());
        bs = (BitSet) tokenOut.value;
        len = BitSetUtil.cardinalityOf(bs);
        break;
      }
      bs = BitSetUtil.copy((BitSet) tokenIn.value);
      len = (isInputSelected ? 1 : BitSetUtil.cardinalityOf(bs));
      tokenOut.value = bs;
      break;
    case list:
      st = (String[]) tokenIn.value;
      len = st.length;
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    if (i1 <= 0)
      i1 = len + i1;
    if (i1 < 1)
      i1 = 1;
    if (i2 == 0)
      i2 = len;
    else if (i2 < 0)
      i2 = len + i2;

    if (i2 > len)
      i2 = len;
    else if (i2 < i1)
      i2 = i1;

    switch (tokenIn.tok) {
    case bitset:
      if (isInputSelected) {
        if (i1 > 1)
          bs.clear();
        break;
      }
      len = BitSetUtil.length(bs);
      int n = 0;
      for (int j = 0; j < len; j++)
        if (bs.get(j) && (++n < i1 || n > i2))
          bs.clear(j);
      break;
    case string:
      if (i1 < 1 || i1 > len)
        tokenOut.value = "";
      else
        tokenOut.value = s.substring(i1 - 1, i2);
      break;
    case list:
      if (i1 < 1 || i1 > len || i2 > len)
        return new Variable(string, "");
      if (i2 == i1)
        return tValue(st[i1 - 1]);
      String[] list = new String[i2 - i1 + 1];
      for (int i = 0; i < list.length; i++)
        list[i] = st[i + i1 - 1];
      tokenOut.value = list;
      break;
    }
    return tokenOut;
  }

  static Variable tValue(String str) {
    Object v = Escape.unescapePointOrBitsetAsVariable(str);
    if (!(v instanceof String))
      return (Variable) v;
    String s = (String) v;
    if (s.toLowerCase() == "true")
      return getBoolean(true);
    if (s.toLowerCase() == "false")
      return getBoolean(false);
    float f = Parser.parseFloatStrict(s);
    return (Float.isNaN(f) ? new Variable(string, v) 
        : s.indexOf(".") < 0 ? new Variable(integer, (int) f)
        : new Variable(decimal, new Float(f)));
  }

  public String toString() {
    return super.toString() + "[" + name + "] index =" + index;
  }

  public boolean setSelectedValue(int selector, Variable var) {
    if (selector == Integer.MAX_VALUE || tok != string && tok != list)
      return false;
    String s = sValue(var);
    switch (tok) {
    case list:
      String[] array = (String[]) value;
      if (selector <= 0)
        selector = array.length + selector;
      if (--selector < 0)
        selector = 0;
      String[] arrayNew = array;
      if (arrayNew.length <= selector) {
        value = arrayNew = ArrayUtil.ensureLength(array, selector + 1);
        for (int i = array.length; i <= selector; i++)
          arrayNew[i] = "";
      }
      arrayNew[selector] = s;
      break;
    case string:
      String str = (String) value;
      int pt = str.length();
      if (selector <= 0)
        selector = pt + selector;
      if (--selector < 0)
        selector = 0;
      while (selector >= str.length())
        str += " ";
      str = str.substring(0, selector) + s + str.substring(selector + 1);
      break;
    }
    return true;
  }

  public String escape() {
    switch (tok) {
    case Token.on:
      return "true";
    case Token.off:
      return "false";
    case Token.integer:
      return "" + intValue;
    case Token.bitset:
      return Escape.escape((BitSet)value);
    case Token.list:
      return Escape.escape((String[])value);
    case Token.point3f:
      return Escape.escape((Point3f)value);
    case Token.point4f:
      return Escape.escape((Point4f)value);
    default:
      return Escape.escape(value);
    }
  }

  public static Variable getBoolean(boolean value) {
    return new Variable(value ? vT : vF);
  }
  
  public static Object sprintf(String strFormat, Variable var) {
    int[] vd = (strFormat.indexOf("d") >= 0 || strFormat.indexOf("i") >= 0 ? new int[1]
        : null);
    String[] vs = (strFormat.indexOf("s") >= 0 ? new String[1] : null);
    float[] vf = (strFormat.indexOf("f") >= 0 ? new float[1] : null);
    Point3f[] vp = (strFormat.indexOf("p") >= 0 && var.tok == Token.point3f ? new Point3f[1]
        : null);
    Point4f[] vq = (strFormat.indexOf("q") >= 0 && var.tok == Token.point4f ? new Point4f[1]
        : null);
    Object[] of = new Object[] { vd, vs, vf, vp, vq };
    if (var.tok != Token.list)
      return sprintf(strFormat, var, of, vd, vs, vf, vp, vq);
    String[] list = (String[]) var.value;
    String[] list2 = new String[list.length];
    for (int i = 0; i < list.length; i++) {
      String s = strFormat;
      list2[i] = sprintf(s, tValue(list[i]), of, vd, vs, vf, vp, vq);
    }
    return list2;
  }

  private static String sprintf(String strFormat, Variable var, Object[] of, 
                                int[] vd, String[] vs, float[] vf, 
                                Point3f[] vp, Point4f[] vq) {
    if (vd != null)
      vd[0] = iValue(var);
    if (vs != null)
      vs[0] = sValue(var);
    if (vf != null)
      vf[0] = fValue(var);
    if (vp != null)
      vp[0]= (Point3f) var.value;
    if (vq != null)
      vq[0]= (Point4f) var.value;    
    return TextFormat.sprintf(strFormat, of );
  }
  


}
