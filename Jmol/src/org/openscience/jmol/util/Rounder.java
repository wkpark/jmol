package org.openscience.jmol.util;

import java.text.DecimalFormat;

public class Rounder {
  //Rounds according to...
  public final static int FIX = 0; // ...12334.345  -> 12334.345.
  public final static int EXP = 1; // ...12334.345  -> 1.2334E4
  
  public static String rounds(double number, int ndec, int scheme) {
    String pattern="0";
    switch(scheme) {
    case FIX:
      if (ndec !=0) {
	pattern = pattern +".";
      }
      for (int i=0; i<ndec; i++) {
	pattern = pattern + "0";
      } 
      break;
    case EXP:
      pattern = pattern + ".";
      for (int i=0; i<ndec; i++) {
	pattern = pattern + "0";
      } 
      pattern = pattern + "E0";
      break;
    }

    DecimalFormat myFormatter = new DecimalFormat(pattern);
    return myFormatter.format(number);
  }

} //end class Rounder
