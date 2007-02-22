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

public class TextFormat {

  public static DecimalFormat[] formatters = new DecimalFormat[10];

  private static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
      "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };

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
          0, i)), decimalDigits - 1) + "E" + (n >= 0 ? "+" : "") + n);
    }
    
    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    DecimalFormat formatter = formatters[decimalDigits];
    if (formatter == null)
      formatter = formatters[decimalDigits] = new DecimalFormat(
          formattingStrings[decimalDigits]);
    return formatter.format(value);
  }

  public static String format(float value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    return format(formatDecimal(value, precision), width,
        0, alignLeft, zeroPad);
  }

  public static String format(String value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    if (precision != Integer.MAX_VALUE && precision > 0 && precision < value.length())
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
    for (int i = padLength; --i > 0;) // this is correct, not >= 0
      sb.append(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return "" + sb;
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

  /**
   * generic string formatter  based on formatLabel in Atom
   * 
   * 
   * @param strFormat   .... %width.precisionKEY....
   * @param key      any string to match
   * @param strT     replacement string or null
   * @param floatT   replacement float or Float.NaN
   * @return         formatted string
   */
  
  public static String formatString(String strFormat, String key, String strT,
                                    float floatT) {
    if (strFormat == null || strFormat.length() == 0)
      return null;
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
      } catch (IndexOutOfBoundsException ioobe) {
        ich = ichPercent;
        break;
      }
    }
    strLabel += strFormat.substring(ich);
    if (strLabel.length() == 0)
      return null;
    return strLabel;
  }

}
