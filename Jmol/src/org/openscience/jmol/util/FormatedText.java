/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.util;

import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.Integer;
import java.util.*;
import java.awt.*;

public class FormatedText { 
  
  //Font markers
  public final static int FONT_SYMBOL = 0;  // Greek characters
  public final static int FONT_NORMAL = 1;  // Latin characters
  
  //Position markers
  public final static int POS_EXP_P = 10;    // Superscript (exponent) position
  public final static int POS_EXP_M = 11;    // Superscript (exponent) position

  String text;
  int fontSize;
  int width;
  Vector textDef = new Vector(0);

  public FormatedText(String text, int fontSize) {
    this.text = text;
    this.fontSize = fontSize;
    parse();
    computeWidth();
  }
  
  /**
   * This methode parse the String passed in argument and
   * return a Vector containing easily usable formating information
   *
   * spaces are used as token separator
   * /N     : switch to NORMAL font
   * /S     : switch to SYMBOL font
   * ^      : put in exponent 
   * /      : (backslash space) put a space character
   * \u2A68 : insert a unicode character
   *
   * For instance:
   *
   * parse("Wavelength / /S l / /N ( cm^-1 _ )", 20) will return
   *
   * Vector(0) = FONT_NORMAL     (int)
   * Vector(1) = "Wavelength"    (String)
   * Vector(2) = " "
   * Vector(3) = FONT_SYMBOL
   * Vector(4) = "l"             (this will give a lambda)
   * Vector(5) = " "
   * Vector(6) = FONT_NORMAL
   * Vector(7) = "(cm"
   * Vector(8) = POS_EXP_P
   * Vector(9) = "-1"
   * Vector(10)= POS_EXP_M
   * Vector(11)= ")"
   * Vector(12)= 234             (String width)
   * Vector(13)= 22              (String height)
   */
  private Vector parse() {
        
    String s;
    StringTokenizer st;
    
    int length=0;
    int height=0;
    
    textDef.addElement(new Integer(FONT_NORMAL)); // By default, we are in NORMAL mode.
    
    st = new StringTokenizer(text, "\\^ _",true);  // Remember that "\" must be escaped --> "\\"
    while (st.hasMoreTokens()) {
      s = st.nextToken();
      
      if (s.equals("\\")) {    // We found a backslash. We use this to introduce a PostScript symbol.
	s = st.nextToken();
	if (s.equals("S")) {   // If the character following the slash is a "S", we enter in SYMBOL mode.
	  textDef.addElement(new Integer(FONT_SYMBOL));
	} else if (s.equals("N")) {   // If the letter is "N", we enter in NORMAL mode.
	  textDef.addElement(new Integer(FONT_NORMAL));	    
	} else if (s.equals(" ")) {  // If the letter is an "space", a space is drawn.
	  textDef.addElement(" ");
	} else if (s.equals("\\")) {  // If the letter is a "backslash", a backslash is drawn.
	  textDef.addElement("\\");
	}
      } else if (s.equals("^")) {   // If a "^" is found, we *increase* the superscript (exponent) position by 1 level.
	textDef.addElement(new Integer(POS_EXP_P));
      } else if (s.equals("_")) {   // If a "_" is found, we *decrease* the superscript (exponent) position by 1 level.
	textDef.addElement(new Integer(POS_EXP_M));
      } else if (s.equals(" ")) { // a space is simply ignored. Use "\ " to draw a space
	//Do nothing
      } else {
	textDef.addElement(s); 
      }
    } //end while
    
    return textDef;
  } //end parse 
  
  private void computeWidth() {
    Frame dummyFrame = new Frame();
    Object token;
    Font f = new Font("Times-Roman", Font.PLAIN, fontSize);
    width=0;
    for (Enumeration e = textDef.elements() ; e.hasMoreElements() ;) {
      
      token = e.nextElement();
      if(token instanceof Integer) {
	switch (((Integer)token).intValue()) {
	case FONT_NORMAL:
	  f = new Font("Times-Roman", Font.PLAIN, fontSize);
	  break;
	case FONT_SYMBOL:
	  f = new Font("Symbol", Font.PLAIN, fontSize);
	  break;
	}
      } else if (token instanceof String) {
	FontMetrics fm = dummyFrame.getFontMetrics(f);
	width = width + fm.stringWidth((String)token);
      }
    } //for on textDef elements
  } //end computeWidth()
  
  public int getWidth() {
    return width;
  }

  public Vector getTextDef() {
    return textDef;
  }
  
  public int getFontSize() {
    return fontSize;
  }

} // end class TextParser
