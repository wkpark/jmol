/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package javajs.util;

public class Parser {

  public static int parseInt(String str) {
    return Parser.parseIntNext(str, new int[] {0});
  }

  public static int parseIntNext(String str, int[] next) {
    int cch = str.length();
    if (next[0] < 0 || next[0] >= cch)
      return Integer.MIN_VALUE;
    return Parser.parseIntChecked(str, cch, next);
  }

  public static int parseIntChecked(String str, int ichMax, int[] next) {
    boolean digitSeen = false;
    int value = 0;
    int ich = next[0];
    if (ich < 0)
      return Integer.MIN_VALUE;
    int ch;
    while (ich < ichMax && Parser.isWhiteSpace(str, ich))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == 45) { //"-"
      negative = true;
      ++ich;
    }
    while (ich < ichMax && (ch = str.charAt(ich)) >= 48 && ch <= 57) {
      value = value * 10 + (ch - 48);
      digitSeen = true;
      ++ich;
    }
    if (!digitSeen)// || !checkTrailingText(str, ich, ichMax))
      value = Integer.MIN_VALUE;
    else if (negative)
      value = -value;
    next[0] = ich;
    return value;
  }

  public static boolean isWhiteSpace(String str, int ich) {
    char ch;
    return (ich >= 0 && ((ch = str.charAt(ich)) == ' ' || ch == '\t' || ch == '\n'));
  }

  /**
   * A float parser that is 30% faster than Float.parseFloat(x) and also accepts
   * x.yD+-n
   * 
   * @param str
   * @param ichMax
   * @param next
   *        pointer; incremented
   * @param isStrict
   * @return value or Float.NaN
   */
  public static float parseFloatChecked(String str, int ichMax, int[] next,
                                         boolean isStrict) {
    boolean digitSeen = false;
    int ich = next[0];
    if (isStrict && str.indexOf('\n') != str.lastIndexOf('\n'))
      return Float.NaN;
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      ++ich;
      negative = true;
    }
    // looks crazy, but if we don't do this, Google Closure Compiler will 
    // write code that Safari will misinterpret in a VERY nasty way -- 
    // getting totally confused as to long integers and double values
    
    // This is Safari figuring out the values of the numbers on the line (x, y, then z):
  
    //  ATOM 1241 CD1 LEU A 64 -2.206 36.532 31.576 1.00 60.60 C
    //  e=1408749273
    //  -e =-1408749273
    //  ATOM 1241 CD1 LEU A 64 -2.206 36.532 31.576 1.00 60.60 C
    //  e=-1821066134
    //  e=36.532
    //  ATOM 1241 CD1 LEU A 64 -2.206 36.532 31.576 1.00 60.60 C
    //  e=-1133871366
    //  e=31.576
    //
    //  "e" values are just before and after the "value = -value" statement.
    
    int ch = 0;
    float ival = 0f;
    float ival2 = 0f;
    while (ich < ichMax && (ch = str.charAt(ich)) >= 48 && ch <= 57) {
      ival = (ival * 10f) + (ch - 48)*1f;
      ++ich;
      digitSeen = true;
    }
    boolean isDecimal = false;
    int iscale = 0;
    int nzero = (ival == 0 ? -1 : 0);
    if (ch == '.') {
      isDecimal = true;
      while (++ich < ichMax && (ch = str.charAt(ich)) >= 48 && ch <= 57) {
        digitSeen = true;
        if (nzero < 0) {
          if (ch == 48) { 
            nzero--;
            continue;
          }
          nzero = -nzero;
        } 
        if (iscale  < Parser.decimalScale.length) {
          ival2 = (ival2 * 10f) + (ch - 48)*1f;
          iscale++;
        }
      }
    }
    float value;
    
    // Safari breaks here intermittently converting integers to floats 
    
    if (!digitSeen) {
      value = Float.NaN;
    } else if (ival2 > 0) {
      value = ival2 * Parser.decimalScale[iscale - 1];
      if (nzero > 1) {
        if (nzero - 2 < Parser.decimalScale.length) {
          value *= Parser.decimalScale[nzero - 2];
        } else {
          value *= Math.pow(10, 1 - nzero);
        }
      } else {
        value += ival;
      }
    } else {
      value = ival;
    }
    boolean isExponent = false;
    if (ich < ichMax && (ch == 69 || ch == 101 || ch == 68)) { // E e D
      isExponent = true;
      if (++ich >= ichMax)
        return Float.NaN;
      ch = str.charAt(ich);
      if ((ch == '+') && (++ich >= ichMax))
        return Float.NaN;
      next[0] = ich;
      int exponent = parseIntChecked(str, ichMax, next);
      if (exponent == Integer.MIN_VALUE)
        return Float.NaN;
      if (exponent > 0 && exponent <= Parser.tensScale.length)
        value *= Parser.tensScale[exponent - 1];
      else if (exponent < 0 && -exponent <= Parser.decimalScale.length)
        value *= Parser.decimalScale[-exponent - 1];
      else if (exponent != 0)
        value *= Math.pow(10, exponent);
    } else {
      next[0] = ich; // the exponent code finds its own ichNextParse
    }
    // believe it or not, Safari reports the long-equivalent of the 
    // float value here, then later the float value, after no operation!
    if (negative)
      value = -value;
    if (value == Float.POSITIVE_INFINITY)
      value = Float.MAX_VALUE;
    return (!isStrict || (!isExponent || isDecimal)
        && Parser.checkTrailingText(str, next[0], ichMax) ? value : Float.NaN);
  }

  public final static float[] tensScale = { 10f, 100f, 1000f, 10000f, 100000f, 1000000f };
  public final static float[] decimalScale = { 
  0.1f, 
  0.01f, 
  0.001f, 
  0.0001f, 
  0.00001f,
  0.000001f, 
  0.0000001f, 
  0.00000001f, 
  0.000000001f
  };
  public static boolean checkTrailingText(String str, int ich, int ichMax) {
    //number must be pure -- no additional characters other than white space or ;
    char ch;
    while (ich < ichMax && (Character.isWhitespace(ch = str.charAt(ich)) || ch == ';'))
      ++ich;
    return (ich == ichMax);
  }

  public static float[] parseFloatArray(String str) {
    return Parser.parseFloatArrayNext(str, new int[1], null, null, null);
  }

  public static int parseFloatArrayInfested(String[] tokens, float[] data) {
    int len = data.length;
    int nTokens = tokens.length;
    int n = 0;
    int max = 0;
    for (int i = 0; i >= 0 && i < len && n < nTokens; i++) {
      float f;
      while (Float.isNaN(f = Parser.parseFloat(tokens[n++])) 
          && n < nTokens) {
      }
      if (!Float.isNaN(f))
        data[(max = i)] = f;
      if (n == nTokens)
        break;
    }
    return max + 1;
  }

  /**
   * @param str
   * @param next
   * @param f
   * @param strStart or null
   * @param strEnd   or null
   * @return array of float values
   * 
   */
  public static float[] parseFloatArrayNext(String str, int[] next, float[] f,
                                            String strStart, String strEnd) {
    int n = 0;
    int pt = next[0];
    if (pt >= 0) {
      if (strStart != null) {
        int p = str.indexOf(strStart, pt);
        if (p >= 0)
          next[0] = p + strStart.length();
      }
      str = str.substring(next[0]);
      pt = (strEnd == null ? -1 : str.indexOf(strEnd));
      if (pt < 0)
        pt = str.length();
      else
        str = str.substring(0, pt);
      next[0] += pt + 1;
      String[] tokens = Parser.getTokens(str);
      if (f == null)
        f = new float[tokens.length];
      n = parseFloatArrayInfested(tokens, f);
    }
    if (f == null)
      return new float[0];
    for (int i = n; i < f.length; i++)
      f[i] = Float.NaN;
    return f;
  }

  public static float parseFloatRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichMax, next, false);
  }

  public static float parseFloatNext(String str, int[] next) {
    int cch = (str == null ? -1 : str.length());
    if (next[0] < 0 || next[0] >= cch)
      return Float.NaN;
    return parseFloatChecked(str, cch, next, false);
  }

  public static float parseFloatStrict(String str) {
    // checks trailing characters and does not allow "1E35" to be float
    int cch = str.length();
    if (cch == 0)
      return Float.NaN;
    return parseFloatChecked(str, cch, new int[] {0}, true);
  }

  public static float parseFloat(String str) {
    return parseFloatNext(str, new int[] {0});
  }

  public static int parseIntRadix(String s, int i) throws NumberFormatException {
    /**
     * 
     * JavaScript uses parseIntRadix
     * 
     * @j2sNative
     * 
     *    return Integer.parseIntRadix(s, i);
     *    
     */
    {
      return Integer.parseInt(s, i);
    }
  }

  public static String[] getTokens(String line) {
    return Parser.getTokensAt(line, 0);
  }

  public static String parseToken(String str) {
    return Parser.parseTokenNext(str, new int[] {0});
  }

  public static String parseTrimmed(String str) {
    return Parser.parseTrimmedRange(str, 0, str.length());
  }

  public static String parseTrimmedAt(String str, int ichStart) {
    return Parser.parseTrimmedRange(str, ichStart, str.length());
  }

  public static String parseTrimmedRange(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax < cch)
      cch = ichMax;
    if (cch < ichStart)
      return "";
    return Parser.parseTrimmedChecked(str, ichStart, cch);
  }

  public static String[] getTokensAt(String line, int ich) {
    if (line == null)
      return null;
    int cchLine = line.length();
    if (ich < 0 || ich > cchLine)
      return null;
    int tokenCount = Parser.countTokens(line, ich);
    String[] tokens = new String[tokenCount];
    int[] next = new int[1];
    next[0] = ich;
    for (int i = 0; i < tokenCount; ++i)
      tokens[i] = Parser.parseTokenChecked(line, cchLine, next);
    return tokens;
  }

  public static int countTokens(String line, int ich) {
    int tokenCount = 0;
    if (line != null) {
      int ichMax = line.length();
      while (true) {
        while (ich < ichMax && isWhiteSpace(line, ich))
          ++ich;
        if (ich == ichMax)
          break;
        ++tokenCount;
        do {
          ++ich;
        } while (ich < ichMax && !isWhiteSpace(line, ich));
      }
    }
    return tokenCount;
  }

  public static String parseTokenNext(String str, int[] next) {
    int cch = str.length();
    if (next[0] < 0 || next[0] >= cch)
      return null;
    return Parser.parseTokenChecked(str, cch, next);
  }

  public static String parseTokenRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return null;
    return Parser.parseTokenChecked(str, ichMax, next);
  }

  public static String parseTokenChecked(String str, int ichMax, int[] next) {
    int ich = next[0];
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    int ichNonWhite = ich;
    while (ich < ichMax && !isWhiteSpace(str, ich))
      ++ich;
    next[0] = ich;
    if (ichNonWhite == ich)
      return null;
    return str.substring(ichNonWhite, ich);
  }

  public static String parseTrimmedChecked(String str, int ich, int ichMax) {
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    int ichLast = ichMax - 1;
    while (ichLast >= ich && isWhiteSpace(str, ichLast))
      --ichLast;
    if (ichLast < ich)
      return "";
    return str.substring(ich, ichLast + 1);
  }

  public static double dVal(String s) throws NumberFormatException {
    /**
     * @j2sNative
     * 
     * if(s==null){
     *   throw new NumberFormatException("null");
     * }
     * var d=parseFloat(s);
     * if(isNaN(d))
     *  throw new NumberFormatException("Not a Number : "+s);
     * return d 
     * 
     */
    {
      return Double.valueOf(s).doubleValue();
    }
  }

  public static float fVal(String s) throws NumberFormatException {
    /**
     * @j2sNative
     * 
     * return this.dVal(s);
     */
    {
      return Float.parseFloat(s);
    }
  }

  public static int parseIntRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichMax, next);
  }

  /**
   * parses a string array for floats. Returns NaN for nonfloats.
   * 
   *  @param tokens  the strings to parse
   *  @param data    the array to fill
   */
  public static void parseFloatArrayData(String[] tokens, float[] data) {
    Parser.parseFloatArrayDataN(tokens, data, data.length);
  }

  /**
   * parses a string array for floats. Returns NaN for nonfloats or missing data.
   * 
   *  @param tokens  the strings to parse
   *  @param data    the array to fill
   *  @param nData   the number of elements
   */
  public static void parseFloatArrayDataN(String[] tokens, float[] data, int nData) {
    for (int i = nData; --i >= 0;)
      data[i] = (i >= tokens.length ? Float.NaN : parseFloat(tokens[i]));
  }

  /**
   * 
   *  proper splitting, even for Java 1.3 -- if the text ends in the run,
   *  no new line is appended.
   * 
   * @param text
   * @param run
   * @return  String array
   */
  public static String[] split(String text, String run) {
    if (text.length() == 0)
      return new String[0];
    int n = 1;
    int i = text.indexOf(run);
    String[] lines;
    int runLen = run.length();
    if (i < 0 || runLen == 0) {
      lines = new String[1];
      lines[0] = text;
      return lines;
    }
    int len = text.length() - runLen;
    for (; i >= 0 && i < len; n++)
      i = text.indexOf(run, i + runLen);
    lines = new String[n];
    i = 0;
    int ipt = 0;
    int pt = 0;
    for (; (ipt = text.indexOf(run, i)) >= 0 && pt + 1 < n;) {
      lines[pt++] = text.substring(i, ipt);
      i = ipt + runLen;
    }
    if (text.indexOf(run, len) != len)
      len += runLen;
    lines[pt] = text.substring(i, len);
    return lines;
  }

  public final static float FLOAT_MIN_SAFE = 2E-45f; 
  // Float.MIN_ VALUE is not reliable with JavaScript because of the float/double difference there
  
  /// general static string-parsing class ///

  // next[0] tracks the pointer within the string so these can all be static.
  // but the methods parseFloat, parseInt, parseToken, parseTrimmed, and getTokens do not require this.

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


}
