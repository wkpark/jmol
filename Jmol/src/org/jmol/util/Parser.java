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



public class Parser {

  public final static float FLOAT_MIN_SAFE = 2E-45f; 
  // Float.MIN_ VALUE is not reliable with JavaScript because of the float/double difference there
  
  /// general static string-parsing class ///

  // next[0] tracks the pointer within the string so these can all be static.
  // but the methods parseFloat, parseInt, parseToken, parseTrimmed, and getTokens do not require this.

  /**
   * parses a "dirty" string for floats. If there are non-float tokens, 
   * they are ignored. A bitset is used to assign values only to specific 
   * atoms in the set, not changing the values of the data array for other atoms.
   * thus, a data set can be incrementally added to in this way.
   * 
   *  @param str     the string to parse
   *  @param bs      the atom positions to assign
   *  @param data    the (sparce) array to fill
   * @return  number of floats
   */
  public static int parseStringInfestedFloatArray(String str, BS bs, float[] data) {
    return parseFloatArrayBsData(getTokens(str), bs, data);
  }

  public static float[] parseFloatArray(String str) {
    return parseFloatArrayNext(str, new int[1], null, null, null);
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
      String[] tokens = getTokens(str);
      if (f == null)
        f = new float[tokens.length];
      n = parseFloatArrayBsData(tokens, null, f);
    }
    if (f == null)
      return new float[0];
    for (int i = n; i < f.length; i++)
      f[i] = Float.NaN;
    return f;
  }
  
  public static int parseFloatArrayBsData(String[] tokens, BS bs, float[] data) {
    int len = data.length;
    int nTokens = tokens.length;
    int n = 0;
    int max = 0;
    boolean haveBitSet = (bs != null);
    for (int i = (haveBitSet ? bs.nextSetBit(0) : 0); i >= 0 && i < len && n < nTokens; i = (haveBitSet ? bs.nextSetBit(i + 1) : i + 1)) {
      float f;
      while (Float.isNaN(f = parseFloatStr(tokens[n++])) 
          && n < nTokens) {
      }
      if (!Float.isNaN(f))
        data[(max = i)] = f;
      if (n == nTokens)
        break;
    }
    return max + 1;
  }

  private static String fixDataString(String str) {
    str = str.replace(';', str.indexOf('\n') < 0 ? '\n' : ' ');
    str = TextFormat.trim(str, "\n \t");
    str = TextFormat.simpleReplace(str, "\n ", "\n");
    str = TextFormat.simpleReplace(str, "\n\n", "\n");
    return str;    
  }
  
  public static float[][] parseFloatArray2d(String str) {
    str = fixDataString(str);
    int[] lines = markLines(str, '\n');
    int nLines = lines.length;
    float[][] data = ArrayUtil.newFloat2(nLines);
    for (int iLine = 0, pt = 0; iLine < nLines; pt = lines[iLine++]) {
      String[] tokens = getTokens(str.substring(pt, lines[iLine]));
      parseFloatArrayData(tokens, data[iLine] = new float[tokens.length]);
    }
    return data;
  }

  public static float[][][] parseFloatArray3d(String str) {
    str = fixDataString(str);
    int[] lines = markLines(str, '\n');
    int nLines = lines.length;
    String[] tokens = getTokens(str.substring(0, lines[0]));
    if (tokens.length != 3)
      return new float[0][0][0];
    int nX = parseInt(tokens[0]);
    int nY = parseInt(tokens[1]);
    int nZ = parseInt(tokens[2]);
    if (nX < 1 || nY < 1 || nZ < 1)
      return new float[1][1][1];
    float[][][] data = ArrayUtil.newFloat3(nX, nY);
    int iX = 0;
    int iY = 0;
    for (int iLine = 1, pt = lines[0]; iLine < nLines && iX < nX; pt = lines[iLine++]) {
      tokens = getTokens(str.substring(pt, lines[iLine]));
      if (tokens.length < nZ)
        continue;
      parseFloatArrayData(tokens, data[iX][iY] = new float[tokens.length]);
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

  /**
   * 
   * @param f
   * @param bs
   * @param data
   */
  public static void setSelectedFloats(float f, BS bs, float[] data) {
    boolean isAll = (bs == null);
    int i0 = (isAll ? 0 : bs.nextSetBit(0));
    for (int i = i0; i >= 0 && i < data.length; i = (isAll ? i + 1 : bs.nextSetBit(i + 1)))
      data[i] = f;
  }
  
  public static float[] extractData(String data, int field, int nBytes,
                                    int firstLine) {
    return parseFloatArrayFromMatchAndField(data, null, 0, 0, null, field,
        nBytes, null, firstLine);
  }

  /**
   * the major lifter here.
   * 
   * @param str         string containing the data 
   * @param bs          selects specific rows of the data 
   * @param fieldMatch  a free-format field pointer, or a column pointer
   * @param fieldMatchColumnCount specifies a column count -- not free-format
   * @param matchData   an array of data to match (atom numbers)
   * @param field       a free-format field pointer, or a column pointer
   * @param fieldColumnCount specifies a column count -- not free-format
   * @param data        float array to modify or null if size unknown
   * @param firstLine   first line to parse (1 indicates all)
   * @return            data
   */
  public static float[] parseFloatArrayFromMatchAndField(
                                                         String str,
                                                         BS bs,
                                                         int fieldMatch,
                                                         int fieldMatchColumnCount,
                                                         int[] matchData,
                                                         int field,
                                                         int fieldColumnCount,
                                                         float[] data, int firstLine) {
    float f;
    int i = -1;
    boolean isMatch = (matchData != null);
    int[] lines = markLines(str, (str.indexOf('\n') >= 0 ? '\n' : ';'));
    int iLine = (firstLine <= 1 || firstLine >= lines.length ? 0 : firstLine - 1);
    int pt = (iLine == 0 ? 0 : lines[iLine - 1]);
    int nLines = lines.length;
    if (data == null)
      data = new float[nLines - iLine];
    int len = data.length;
    int minLen = (fieldColumnCount <= 0 ? Math.max(field, fieldMatch) : Math
        .max(field + fieldColumnCount, fieldMatch + fieldMatchColumnCount) - 1);
    boolean haveBitSet = (bs != null);
    for (; iLine < nLines; iLine++) {
      String line = str.substring(pt, lines[iLine]).trim();
      pt = lines[iLine];
      String[] tokens = (fieldColumnCount <= 0 ? getTokens(line) : null);
      // check for inappropriate data -- line too short or too few tokens or NaN for data
      // and parse data
      if (fieldColumnCount <= 0) {
        if (tokens.length < minLen
            || Float.isNaN(f = parseFloatStr(tokens[field - 1])))
          continue;
      } else {
        if (line.length() < minLen
            || Float.isNaN(f = parseFloatStr(line.substring(field - 1, field
                + fieldColumnCount - 1))))
          continue;
      }
      int iData;
      if (isMatch) {
        iData = parseInt(tokens == null ? line.substring(fieldMatch - 1,
            fieldMatch + fieldMatchColumnCount - 1) : tokens[fieldMatch - 1]);
        // in the fieldMatch column we have an integer pointing into matchData
        // we replace that number then with the corresponding number in matchData
        if (iData == Integer.MIN_VALUE || iData < 0 || iData >= len
            || (iData = matchData[iData]) < 0)
          continue;
        // and we set bs to indicate we are updating that value
        if (haveBitSet)
          bs.set(iData);
      } else {
        // no match data
        // bs here indicates the specific data elements that need filling
        if (haveBitSet) 
          i = bs.nextSetBit(i + 1);
        else
          i++;
        if (i < 0 || i >= len)
          return data;
        iData = i;
      }
      data[iData] = f;
      //System.out.println("data[" + iData + "] = " + data[iData]);
    }
    return data;
  }
  
  /**
   * parses a string array for floats. Returns NaN for nonfloats.
   * 
   *  @param tokens  the strings to parse
   *  @param data    the array to fill
   */
  public static void parseFloatArrayData(String[] tokens, float[] data) {
    parseFloatArrayDataN(tokens, data, data.length);
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
      data[i] = (i >= tokens.length ? Float.NaN : parseFloatStr(tokens[i]));
  }
 
  public static float parseFloatStr(String str) {
    return parseFloatNext(str, new int[] {0});
  }

  public static float parseFloatStrict(String str) {
    // checks trailing characters and does not allow "1E35" to be float
    int cch = str.length();
    if (cch == 0)
      return Float.NaN;
    return parseFloatChecked(str, cch, new int[] {0}, true);
  }

  public static int parseInt(String str) {
    return parseIntNext(str, new int[] {0});
  }

  public static String[] getTokens(String line) {
    return getTokensAt(line, 0);
  }

  public static String parseToken(String str) {
    return parseTokenNext(str, new int[] {0});
  }

  public static String parseTrimmed(String str) {
    return parseTrimmedRange(str, 0, str.length());
  }
  
  public static String parseTrimmedAt(String str, int ichStart) {
    return parseTrimmedRange(str, ichStart, str.length());
  }
  
  public static String parseTrimmedRange(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax < cch)
      cch = ichMax;
    if (cch < ichStart)
      return "";
    return parseTrimmedChecked(str, ichStart, cch);
  }

  public static int[] markLines(String data, char eol) {
    int nLines = 0;
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        nLines++;
    int[] lines = new int[nLines + 1];
    lines[nLines--] = data.length();
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        lines[nLines--] = i + 1;
    return lines;
  }

  public static float parseFloatNext(String str, int[] next) {
    int cch = str.length();
    if (next[0] < 0 || next[0] >= cch)
      return Float.NaN;
    return parseFloatChecked(str, cch, next, false);
  }

  public static float parseFloatRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichMax, next, false);
  }

  private final static float[] decimalScale = { 
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

  private final static float[] tensScale = { 10f, 100f, 1000f, 10000f, 100000f, 1000000f };

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
  private static float parseFloatChecked(String str, int ichMax, int[] next,
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
        if (iscale  < decimalScale.length) {
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
      value = ival2 * decimalScale[iscale - 1];
      if (nzero > 1) {
        if (nzero - 2 < decimalScale.length) {
          value *= decimalScale[nzero - 2];
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
      if (exponent > 0 && exponent <= tensScale.length)
        value *= tensScale[exponent - 1];
      else if (exponent < 0 && -exponent <= decimalScale.length)
        value *= decimalScale[-exponent - 1];
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
        && checkTrailingText(str, next[0], ichMax) ? value : Float.NaN);
  }


//  private static float parseFloatCheckedOld(String str, int ichMax, int[] next, boolean isStrict) {
//    float value = 0;
//    int ich = next[0];
//    if (isStrict && str.indexOf('\n') != str.lastIndexOf('\n'))
//        return Float.NaN;
//    while (ich < ichMax && isWhiteSpace(str, ich))
//      ++ich;
//    boolean negative = false;
//    if (ich < ichMax && str.charAt(ich) == '-') {
//      ++ich;
//      negative = true;
//    }
//    char ch = 0;
//    int ich0 = ich;
//    boolean isDecimal = false;
//    while (ich < ichMax && (Character.isDigit(ch = str.charAt(ich)) || ch == '.')) {
//      ++ich;
//      if (ch == '.') {
//        if (isDecimal)
//          return Float.NaN;
//        isDecimal = true;
//      }
//    }
//    boolean isDouble = false;
//    boolean isExponent = false;
//    if (ich != ich0 && ich < ichMax && (ch == 'E' || ch == 'e' || (isDouble = (ch == 'D')))) {
//      isExponent = true;
//      if (++ich >= ichMax || ((ch = str.charAt(ich)) == '+' || ch == '-') && ++ich >= ichMax)
//        return Float.NaN;
//      while (ich < ichMax && Character.isDigit(str.charAt(ich))) {
//        ++ich;
//      }
//    }
//    next[0] = ich;
//    if (ich0 == ich)
//      return Float.NaN;
//    value = fVal(isDouble ? str.substring(ich0, ich).replace('D','E') : str.substring(ich0, ich)); 
//    if (value == Float.POSITIVE_INFINITY)
//      value= Float.MAX_VALUE;
//    if (negative)
//      value = -value;
//    return (!isStrict 
//        || (!isExponent || isDecimal) && checkTrailingText(str, next[0], ichMax) 
//        ? value : Float.NaN);
//  }

  private static boolean checkTrailingText(String str, int ich, int ichMax) {
    //number must be pure -- no additional characters other than white space or ;
    char ch;
    while (ich < ichMax && (Character.isWhitespace(ch = str.charAt(ich)) || ch == ';'))
      ++ich;
    return (ich == ichMax);
  }
  
  public static int parseIntNext(String str, int[] next) {
    int cch = str.length();
    if (next[0] < 0 || next[0] >= cch)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, cch, next);
  }

  public static int parseIntRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichMax, next);
  }

  private static int parseIntChecked(String str, int ichMax, int[] next) {
    boolean digitSeen = false;
    int value = 0;
    int ich = next[0];
    if (ich < 0)
      return Integer.MIN_VALUE;
    int ch;
    while (ich < ichMax && isWhiteSpace(str, ich))
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

  public static String[] getTokensAt(String line, int ich) {
    if (line == null)
      return null;
    int cchLine = line.length();
    if (ich < 0 || ich > cchLine)
      return null;
    int tokenCount = countTokens(line, ich);
    String[] tokens = new String[tokenCount];
    int[] next = new int[1];
    next[0] = ich;
    for (int i = 0; i < tokenCount; ++i)
      tokens[i] = parseTokenChecked(line, cchLine, next);
    return tokens;
  }

  private static int countTokens(String line, int ich) {
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
    return parseTokenChecked(str, cch, next);
  }

  public static String parseTokenRange(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] < 0 || next[0] >= ichMax)
      return null;
    return parseTokenChecked(str, ichMax, next);
  }

  private static String parseTokenChecked(String str, int ichMax, int[] next) {
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

  private static String parseTrimmedChecked(String str, int ich, int ichMax) {
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    int ichLast = ichMax - 1;
    while (ichLast >= ich && isWhiteSpace(str, ichLast))
      --ichLast;
    if (ichLast < ich)
      return "";
    return str.substring(ich, ichLast + 1);
  }

  public static String concatTokens(String[] tokens, int iFirst, int iEnd) {
    String str = "";
    String sep = "";
    for (int i = iFirst; i < iEnd; i++) {
      if (i < tokens.length) {
        str += sep + tokens[i];
        sep = " ";
      }
    }
    return str;
  }
  
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
  
  private static boolean isWhiteSpace(String str, int ich) {
    char ch;
    return (ich >= 0 && ((ch = str.charAt(ich)) == ' ' || ch == '\t' || ch == '\n'));
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

  public static int parseIntRadix(String s, int i) throws NumberFormatException {
    /**
     * 
     * JavaScript uses parseIntRadix
     * 
     * @j2sNative
     * 
     *    i = parseInt(s, i);
     *    if (isNaN(i))
     *      throw new NumberFormatException("Not a Number : "+s);
     *    return i;
     */
    {
      return Integer.parseInt(s, i);
    }
  }

  public static float approx(float f, float n) {
    return Math.round (f * n) / n;
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
  
}
