/*
 * @(#)DecimalNumberField.java    1.0 98/10/30
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import javax.swing.*; 
import javax.swing.text.*; 

import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

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
                if (Character.isDigit(source[i]) || source[i] == cdot[0] )
                    result[j++] = source[i];
                else {
                    toolkit.beep();
                    System.err.println("Attempting to insert illegal character in decimal: " + source[i]);
                }
            }
            super.insertString(offs, new String(result, 0, j), a);
        }
    }    
}
