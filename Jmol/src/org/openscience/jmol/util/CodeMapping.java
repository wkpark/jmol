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
import java.util.*;

/*
 *  Map Unicode with Postcript code.  
 */
public class CodeMapping {

  public final static Vector unicode = new Vector(100);  // Vector of string
  public final static Vector postscriptcode = new Vector(100); // Vector of string

  static{

    unicode.addElement("\u03B1");
    postscriptcode.addElement("a");  //alpha

    unicode.addElement("\u0391");
    postscriptcode.addElement("A");  //ALPHA

    unicode.addElement("\u03B2");
    postscriptcode.addElement("b");  //beta

    unicode.addElement("\u0392");
    postscriptcode.addElement("B");  //BETA

    unicode.addElement("\u03B3");
    postscriptcode.addElement("g");  //gamma

    unicode.addElement("\u0393");
    postscriptcode.addElement("G");  //GAMMA

    unicode.addElement("\u03B4");
    postscriptcode.addElement("d");  //delta

    unicode.addElement("\u0394");
    postscriptcode.addElement("D");  //DELTA

    unicode.addElement("\u03B5");
    postscriptcode.addElement("e");  //epsilon

    unicode.addElement("\u0395");
    postscriptcode.addElement("E");  //EPSILON

    unicode.addElement("\u03B6");
    postscriptcode.addElement("z");  //zeta

    unicode.addElement("\u0396");
    postscriptcode.addElement("Z");  //ZETA

    unicode.addElement("\u03B7");
    postscriptcode.addElement("j");  //eta

    unicode.addElement("\u0397");
    postscriptcode.addElement("J");  //ETA

    unicode.addElement("\u03B8");
    postscriptcode.addElement("q");  //theta

    unicode.addElement("\u0398");
    postscriptcode.addElement("Q");  //THETA

    unicode.addElement("\u03B9");
    postscriptcode.addElement("i");  //iota

    unicode.addElement("\u0399");
    postscriptcode.addElement("I");  //IOTA

    unicode.addElement("\u03BA");
    postscriptcode.addElement("k");  //kappa

    unicode.addElement("\u039A");
    postscriptcode.addElement("K");  //KAPPA

    unicode.addElement("\u03BB");
    postscriptcode.addElement("l");  //lamda

    unicode.addElement("\u039B");
    postscriptcode.addElement("L");  //LAMDA

    unicode.addElement("\u03BC");
    postscriptcode.addElement("m");  //mu

    unicode.addElement("\u039C");
    postscriptcode.addElement("M");  //MU

    unicode.addElement("\u03BD");
    postscriptcode.addElement("n");  //nu

    unicode.addElement("\u039D");
    postscriptcode.addElement("N");  //NU

    unicode.addElement("\u03BE");
    postscriptcode.addElement("x");  //xi

    unicode.addElement("\u039E");
    postscriptcode.addElement("X");  //XI

    unicode.addElement("\u03BF");
    postscriptcode.addElement("o");  //omicron

    unicode.addElement("\u039F");
    postscriptcode.addElement("O");  //OMICRON

    unicode.addElement("\u03C0");
    postscriptcode.addElement("p");  //pi

    unicode.addElement("\u03A0");
    postscriptcode.addElement("P");  //PI

    unicode.addElement("\u03C1");
    postscriptcode.addElement("r");  //rho

    unicode.addElement("\u03C1");
    postscriptcode.addElement("R");  //RHO

    unicode.addElement("\u03A1");
    postscriptcode.addElement("s");  //sigma

    unicode.addElement("\u03A3");
    postscriptcode.addElement("S");  //SIGMA

    unicode.addElement("\u03C4");
    postscriptcode.addElement("t");  //tau

    unicode.addElement("\u03A4");
    postscriptcode.addElement("T");  //TAU

    unicode.addElement("\u03C5");
    postscriptcode.addElement("u");  //upsilon

    unicode.addElement("\u03A5");
    postscriptcode.addElement("U");  //UPSILON

    unicode.addElement("\u03C6");
    postscriptcode.addElement("f");  //phi

    unicode.addElement("\u03A6");
    postscriptcode.addElement("F");  //PHI

    unicode.addElement("\u03C7");
    postscriptcode.addElement("c");  //chi

    unicode.addElement("\u03A7");
    postscriptcode.addElement("C");  //CHI

    unicode.addElement("\u03C8");
    postscriptcode.addElement("h");  //psi

    unicode.addElement("\u03A8");
    postscriptcode.addElement("H");  //PSI

    unicode.addElement("\u03C9");
    postscriptcode.addElement("w");  //omega

    unicode.addElement("\u03A9");
    postscriptcode.addElement("W");  //OMEGA


    //Must be the last one of the list
    unicode.addElement("?");
    postscriptcode.addElement("");  //UNKNOWN

  }
  


  /*
   * Return the unicode sequence (something like "\u456B") for the given 
   * PostScript code (something like "L").
   * For example: 
   *   getUnicode("L")
   * will return
   *  "\u039B"
   */
  public static String getUnicode(String PostScriptCode) {
    int index = -1;
    for (Enumeration e = postscriptcode.elements() ; e.hasMoreElements() ;) {
      index++;
      if(PostScriptCode.equals(e.nextElement())) {
	break;
      }
    }
    return (String)(unicode.elementAt(index));
  }
}
