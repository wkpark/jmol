
/*
 * Copyright 2002 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;


import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.PlainDocument;
import javax.swing.text.Document;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

public class DecimalNumberField extends JTextField {

  private Toolkit toolkit;
  private NumberFormat doubleFormatter;

  public DecimalNumberField(int value, int columns) {
    super(columns);
    toolkit = Toolkit.getDefaultToolkit();
    doubleFormatter = NumberFormat.getNumberInstance(Locale.US);
    setValue(value);
  }

  public double getValue() {

    double retVal = 0.0;
    try {
      retVal = doubleFormatter.parse(getText()).doubleValue();
    } catch (ParseException e) {

      // This should never happen because insertString allows
      // only properly formatted data to get in the field.
      toolkit.beep();
    }
    return retVal;
  }

  public void setValue(int value) {
    setText(doubleFormatter.format(value));
  }

  protected Document createDefaultModel() {
    return new DecimalNumberDocument();
  }

  protected class DecimalNumberDocument extends PlainDocument {

    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException {

      char[] source = str.toCharArray();
      char[] result = new char[source.length];
      int j = 0;

      String dot = new String(".");
      char[] cdot = dot.toCharArray();

      for (int i = 0; i < result.length; i++) {
        if (Character.isDigit(source[i]) || (source[i] == cdot[0])) {
          result[j++] = source[i];
        } else {
          toolkit.beep();
          System.err
              .println("Attempting to insert illegal character in decimal: "
                + source[i]);
        }
      }
      super.insertString(offs, new String(result, 0, j), a);
    }
  }
}
