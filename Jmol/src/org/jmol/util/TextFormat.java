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

import java.text.DecimalFormat;
import javax.vecmath.Point3f;

public class TextFormat {

  private final static DecimalFormat[] formatters = new DecimalFormat[10];

  private final static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
      "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };

  private final static Boolean[] useNumberLocalization = new Boolean[1];
  {
    useNumberLocalization[0] = Boolean.TRUE;
  }
  
  public static void setUseNumberLocalization(boolean TF) {
    useNumberLocalization[0] = (TF ? Boolean.TRUE : Boolean.FALSE);
  }

  public static String formatDecimal(float value, int decimalDigits) {
    if (decimalDigits == Integer.MAX_VALUE)
      return "" + value;
    if (decimalDigits < 0) {
      decimalDigits = -decimalDigits;
      if (decimalDigits > formattingStrings.length)
        decimalDigits = formattingStrings.length;
      if (value == 0)
        return formattingStrings[decimalDigits] + "E+0";
      //scientific notation
      int n = 0;
      double d;
      if (Math.abs(value) < 1) {
        n = 10;
        d = value * 1e-10;
      } else {
        n = -10;
        d = value * 1e10;
      }
      String s = ("" + d).toUpperCase();
      int i = s.indexOf("E");
      n = Parser.parseInt(s.substring(i + 1)) + n;
      return (i < 0 ? "" + value : formatDecimal(Parser.parseFloat(s.substring(
          0, i)), decimalDigits - 1)
          + "E" + (n >= 0 ? "+" : "") + n);
    }

    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    DecimalFormat formatter = formatters[decimalDigits];
    if (formatter == null)
      formatter = formatters[decimalDigits] = new DecimalFormat(
          formattingStrings[decimalDigits]);
    String s = formatter.format(value);
    return (Boolean.TRUE.equals(useNumberLocalization[0]) ? s : s.replace(',','.'));
  }

  public static String format(float value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    return format(formatDecimal(value, precision), width, 0, alignLeft, zeroPad);
  }

  public static String format(String value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    if (precision != Integer.MAX_VALUE && precision > 0
        && precision < value.length())
      value = value.substring(0, precision);
    else if (precision < 0 && -precision < value.length())
      value = value.substring(value.length() + precision);

    int padLength = width - value.length();
    if (padLength <= 0)
      return value;
    boolean isNeg = (zeroPad && !alignLeft && value.charAt(0) == '-');
    char padChar = (zeroPad ? '0' : ' ');
    char padChar0 = (isNeg ? '-' : padChar);

    StringBuffer sb = new StringBuffer();
    if (alignLeft)
      sb.append(value);
    sb.append(padChar0);
    for (int i = padLength; --i > 0;)
      // this is correct, not >= 0
      sb.append(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return sb.toString();
  }

  public static String formatString(String strFormat, String key, String strT) {
    return formatString(strFormat, key, strT, Float.NaN);
  }

  public static String formatString(String strFormat, String key, float floatT) {
    return formatString(strFormat, key, null, floatT);
  }

  public static String formatString(String strFormat, String key, int intT) {
    return formatString(strFormat, key, "" + intT, Float.NaN);
  }
  
  public static String sprintf(String strFormat, Object[] values) {
    if (values == null)
      return strFormat;
    for (int o = 0; o < values.length; o++)
      if (values[o] != null) {
        if (values[o] instanceof String[]) {
          String[] sVal = (String[]) values[o];
          for (int i = 0; i < sVal.length; i++)
            strFormat = formatString(strFormat, "s", sVal[i], Float.NaN, true);
        } else if (values[o] instanceof float[]) {
          float[] fVal = (float[]) values[o];
          for (int i = 0; i < fVal.length; i++)
            strFormat = formatString(strFormat, "f", null, fVal[i], true);
        } else if (values[o] instanceof int[]) {
          int[] iVal = (int[]) values[o];
          for (int i = 0; i < iVal.length; i++)
            strFormat = formatString(strFormat, "d", "" + iVal[i], Float.NaN,
                true);
        } else if (values[o] instanceof Point3f[]) {
          Point3f[] pVal = (Point3f[]) values[o];
          for (int i = 0; i < pVal.length; i++) {
            strFormat = formatString(strFormat, "p", null, pVal[i].x, true);
            strFormat = formatString(strFormat, "p", null, pVal[i].y, true);
            strFormat = formatString(strFormat, "p", null, pVal[i].z, true);
          }
        }
      }
    return strFormat;
  }

  public static String sprintf(String strFormat, String[] sVal, float[] fVal) {
    return sprintf(strFormat, new Object[] {sVal, fVal});
  }
  
  public static String sprintf(String strFormat, String[] sVal, float[] fVal, 
                               int[] iVal) {
    return sprintf(strFormat, new Object[] {sVal, fVal, iVal});
  }

  public static String formatString(String strFormat, String key, String strT,
                                    float floatT) {
    return formatString(strFormat, key, strT, floatT, false);
  }

  /**
   * generic string formatter  based on formatLabel in Atom
   * 
   * 
   * @param strFormat   .... %width.precisionKEY....
   * @param key      any string to match
   * @param strT     replacement string or null
   * @param floatT   replacement float or Float.NaN
   * @param doOne    mimic sprintf    
   * @return         formatted string
   */

  private static String formatString(String strFormat, String key, String strT,
                                    float floatT, boolean doOne) {
    if (strFormat == null)
      return null;
    if ("".equals(strFormat))
      return "";
    int len = key.length();
    if (strFormat.indexOf("%") < 0 || len == 0 || strFormat.indexOf(key) < 0)
      return strFormat;

    String strLabel = "";
    int ich, ichPercent, ichKey;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0
        && (ichKey = strFormat.indexOf(key, ichPercent + 1)) >= 0;) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ichKey > ichPercent + 6) {
        strLabel += '%';
        continue;//%12.10x
      }
      try {
        boolean alignLeft = false;
        if (strFormat.charAt(ich) == '-') {
          alignLeft = true;
          ++ich;
        }
        boolean zeroPad = false;
        if (strFormat.charAt(ich) == '0') {
          zeroPad = true;
          ++ich;
        }
        char ch;
        int width = 0;
        while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
          width = (10 * width) + (ch - '0');
          ++ich;
        }
        int precision = Integer.MAX_VALUE;
        if (strFormat.charAt(ich) == '.') {
          ++ich;
          if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
            precision = ch - '0';
            ++ich;
          }
        }
        String st = strFormat.substring(ich, ich + len);
        if (!st.equals(key)) {
          ich = ichPercent + 1;
          strLabel += '%';
          continue;
        }
        ich += len;
        if (!Float.isNaN(floatT))
          strLabel += TextFormat.format(floatT, width, precision, alignLeft,
              zeroPad);
        else if (strT != null)
          strLabel += TextFormat.format(strT, width, precision, alignLeft,
              zeroPad);
        if (doOne)
          break;
      } catch (IndexOutOfBoundsException ioobe) {
        ich = ichPercent;
        break;
      }
    }
    strLabel += strFormat.substring(ich);
    //if (strLabel.length() == 0)
      //return null;
    return strLabel;
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

  /**
   * Does a clean replace of any of the characters in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String replaceAllCharacters(String str, String strFrom,
                                            String strTo) {
    for (int i = strFrom.length(); --i >= 0;) {
      String chFrom = strFrom.substring(i, i + 1);
      str = simpleReplace(str, chFrom, strTo);
    }
    return str;
  }
  
  /**
   * Does a clean replace of any of the characters in str with chrTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param chTo
   * @return  replaced string
   */
  public static String replaceAllCharacters(String str, String strFrom,
                                            char chTo) {
    for (int i = strFrom.length(); --i >= 0;)
      str = str.replace(strFrom.charAt(i), chTo);
    return str;
  }
  
  /**
   * Does a clean replace of strFrom in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String simpleReplace(String str, String strFrom, String strTo) {
    if (str == null || str.indexOf(strFrom) < 0 || strFrom.equals(strTo))
      return str;
    int fromLength = strFrom.length();
    if (fromLength == 0)
      return str;
    boolean isOnce = (strTo.indexOf(strFrom) >= 0);
    int ipt;
    while (str.indexOf(strFrom) >= 0) {
      StringBuffer s = new StringBuffer();
      int ipt0 = 0;
      while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
        s.append(str.substring(ipt0, ipt)).append(strTo);
        ipt0 = ipt + fromLength;
      }
      s.append(str.substring(ipt0));
      str = s.toString();
      if (isOnce)
        break;
    }

    return str;
  }

  public static String trim(String str, String chars) {
    if (chars.length() == 0)
      return str.trim();
    int len = str.length();
    int k = 0;
    while (k < len && chars.indexOf(str.charAt(k)) >= 0)
      k++;
    int m = str.length() - 1;
    while (m > k && chars.indexOf(str.charAt(m)) >= 0)
      m--;
    return str.substring(k, m + 1);
  }

  public static String[] split(String text, char ch) {
    return split(text, "" + ch);
  }
  
  public static void lFill(StringBuffer s, String s1, String s2) {
    s.append(s2);
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
  }
  
  public static void rFill(StringBuffer s, String s1, String s2) {
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
    s.append(s2);
  }
  
  public static String safeTruncate(float f, int n) {
    if (f > -0.001 && f < 0.001)
      f = 0;
    return (f + "         ").substring(0,n);
  }

  public static boolean isWild(String s) {
    return s.indexOf("*") >= 0 || s.indexOf("?") >= 0;
  }

  public static boolean isMatch(String s, String strWildcard,
                                boolean checkStar, boolean allowInitialStar) {

    int ich = 0;
    int cchWildcard = strWildcard.length();
    int cchs = s.length();
    if (cchs == 0 || cchWildcard == 0)
      return false;
    boolean isStar0 = (checkStar && allowInitialStar ? strWildcard.charAt(0) == '*' : false);
    if (isStar0 && strWildcard.charAt(cchWildcard - 1) == '*')
      return (cchWildcard < 3 || s.indexOf(strWildcard.substring(1,
          cchWildcard - 1)) >= 0); 
    String qqq = "????";
    while (qqq.length() < s.length())
      qqq += qqq;
    if (checkStar) {
      if (allowInitialStar && isStar0)
        strWildcard = qqq + strWildcard.substring(1);
      if (strWildcard.charAt(ich = strWildcard.length() - 1) == '*')
        strWildcard = strWildcard.substring(0, ich) + qqq;
      cchWildcard = strWildcard.length();
    }

    if (cchWildcard < cchs)
      return false;

    ich = 0;

    // atom name variant (trimLeadingMarks == false)

    // -- each ? matches ONE character if not at end
    // -- extra ? at end ignored

    //group3 variant (trimLeadingMarks == true)

    // -- each ? matches ONE character if not at end
    // -- extra ? at beginning reduced to match length
    // -- extra ? at end ignored

    while (cchWildcard > cchs) {
      if (allowInitialStar && strWildcard.charAt(ich) == '?') {
        ++ich;
      } else if (strWildcard.charAt(ich + cchWildcard - 1) != '?') {
        return false;
      }
      --cchWildcard;
    }

    for (int i = cchs; --i >= 0;) {
      char charWild = strWildcard.charAt(ich + i);
      if (charWild == '?')
        continue;
      if (charWild != s.charAt(i))
        return false;
    }
    return true;
  }
}
