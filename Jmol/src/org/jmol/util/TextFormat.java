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
    if (decimalDigits < 0)
      return "" + value;
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
    return TextFormat.format(TextFormat.formatDecimal(value, precision), width,
        0, alignLeft, zeroPad);
  }

  public static String format(String value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    if (precision > value.length())
      value = value.substring(0, precision);
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
      sb.append(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return "" + sb;
  }

}
