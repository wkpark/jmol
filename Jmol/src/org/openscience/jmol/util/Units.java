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

public class Units {
  
  //constant in SI
  public final static double C_PLANCK = 6.62606876E-34; // Plank constant (J.s)
  public final static double C_BOHR = 0.5291772083E-10; // Bohr radius (m)
  public final static double C_CE =  1.602176462E-19; // Elementary charge (Coulomb)
  public final static double C_C = 299792458 ; // Light velocity in vacuum (m/s)

  //UNITS
  //Length
  public final static int LENGTH_START_INDEX=0;
  public final static int ANGSTROM = 0;
  public final static int BOHR = 1;
  public final static int METER = 2;
  public final static int CENTI_METER = 3;
  public final static int MILLI_METER = 4;
  public final static int MICRO_METER = 5;
  public final static int NANO_METER = 6;
    

  //Energy
  public final static int ENERGY_START_INDEX=100;
  public final static int JOULE = 100;
  public final static int EV = 101;
  public final static int MILLI_EV = 102;
  //  CMM1 == cm^1  ( The invert wavelength of a photon of a given energy)
  public final static int CENTI_METER_M1 = 103; 
  public final static int MILLI_METER_M1 = 104;
  public final static int MICRO_METER_M1 = 105;
  public final static int NANO_METER_M1 = 106;

  public static double getConversionFactor(int inputUnit, int outputUnit) {
    double factor; // outputValue = inputValue * factor
    if (inputUnit == outputUnit) {
      factor = 1;
      return factor;
    }
    
    factor = getCF(inputUnit, outputUnit);
    if (factor == 0) {
      factor = getCF(outputUnit, inputUnit);
      if (factor != 0) {
	factor = 1/factor;
      } else {
	System.out.println("This conversion is not available");
      }
    } 
    
    return factor;
  } //end getConvertionFactor(...)

  private static double getCF(int inputUnit, int outputUnit) {
    double factor = 0;
    
    // We store only the superior part of the conversion table:
    // In this example, A, B, C ,D, E are length and 
    // F, G, H, I are energies for exampe
    //
    //     -------------------
    //     |A|B|C|D|E|F|G|H|I|
    //   ---------------------
    //   A |1|x|x|x|x| | | | |
    //   B | |1|x|x|x| | | | |
    //   C | | |1|x|x| | | | |
    //   D | | | |1|x| | | | |
    //   E | | | | |1| | | | |
    //   F | | | | | |1|x|x|x|
    //   F | | | | | | |1|x|x|
    //   H | | | | | | | |1|x|
    //   I | | | | | | | | |1|
    //
    // We get this structure
    //
    // switch (inputUnit) {
    // case B:
    //   switch(outputUnit) {
    //   case A:
    //   }
    // case C:
    //   switch(outputUnit) {
    //   case A:
    //   case B:
    //   }
    // case D:
    //   switch(outputUnit) {
    //   case A:
    //   case B:
    //   case C:
    //   }    
    // case E:
    //   switch(outputUnit) {
    //   case A:
    //   case B:
    //   case C:
    //   case D:
    //   }    
    // case F:
    //   ...
    //   ...you get it I think
    // }

    switch (inputUnit) {
      // Lengths
    case BOHR:
      switch (outputUnit) {
      case ANGSTROM: factor = C_BOHR; break;
      }
      break;
    case METER:
      switch (outputUnit) {
      case ANGSTROM: factor = 1E10; break;
      case BOHR: factor = 1E10/C_BOHR; break;
      }
      break;
    case CENTI_METER:
      switch (outputUnit) {
      case ANGSTROM: factor = 1E8; break;
      case BOHR: factor = 1E8/C_BOHR; break;
      case METER: factor = 1/100; break;
      }
      break;
    case MILLI_METER:
      switch (outputUnit) {
      case ANGSTROM: factor = 1E7; break;
      case BOHR: factor = 1E7/C_BOHR; break;
      case METER: factor = 1E-3; break;
      case CENTI_METER: factor = 1E-1; break;
      }
      break;
    case MICRO_METER:
      switch (outputUnit) {
      case ANGSTROM: factor = 1E10; break;
      case BOHR: factor = 1E10/C_BOHR; break;
      case METER: factor = 1E-6 ; break;
      case CENTI_METER: factor = 1E-4; break; 
      case MILLI_METER: factor = 1E-3; break;
      }
      break;
    case NANO_METER:
      switch (outputUnit) {
      case ANGSTROM: factor = 1E10; break;
      case BOHR: factor = 1E10/C_BOHR; break;
      case METER: factor = 1E-9 ; break;
      case CENTI_METER: factor = 1E-7; break;
      case MILLI_METER: factor = 1E-6; break;
      case MICRO_METER: factor = 1E-3; break;
      }
      break;
      
      // Energies
    case EV:
      switch (outputUnit) {
      case JOULE: factor = C_CE; break;
      }
      break;
    case MILLI_EV:
      switch (outputUnit) {
      case JOULE: factor = C_CE / 1E3; break;
      case EV: factor = 1E-3; break;
      }
      break;
    case CENTI_METER_M1:
      switch (outputUnit) {
      case JOULE: factor = C_PLANCK * C_C * 1E2; break;
      case EV: factor = (C_PLANCK/ C_CE) * C_C * 1E2; break;
      case MILLI_EV: factor = (C_PLANCK/ C_CE) * C_C * 1E5; break;
      }
      break;
    case MILLI_METER_M1:
      switch (outputUnit) {
      case JOULE: factor = C_PLANCK * C_C * 1E3; break;
      case EV: factor = (C_PLANCK/ C_CE) * C_C * 1E3; break;
      case MILLI_EV: factor = (C_PLANCK/ C_CE) * C_C * 1E6; break;
      case CENTI_METER_M1: factor = 1E1; break;
      }
      break;
    case MICRO_METER_M1:
      switch (outputUnit) {
      case JOULE: factor = C_PLANCK * C_C * 1E6; break;
      case EV: factor = (C_PLANCK/ C_CE) * C_C * 1E6; break;
      case MILLI_EV: factor = (C_PLANCK/ C_CE) * C_C * 1E9; break;
      case CENTI_METER_M1: factor = 1E4; break;
      case MILLI_METER_M1: factor = 1E3; break;
      }
      break;
    case NANO_METER_M1:
      switch (outputUnit) {
      case JOULE: factor = C_PLANCK * C_C * 1E9; break;
      case EV: factor = (C_PLANCK/ C_CE) * C_C * 1E9; break;
      case MILLI_EV: factor = (C_PLANCK/ C_CE) * C_C * 1E12; break;
      case CENTI_METER_M1: factor = 1E7; break;
      case MILLI_METER_M1: factor = 1E6; break;
      case MICRO_METER_M1: factor = 1E3; break;
      }
      break;
    }
    return factor;
  } //end getCF()
  
  public static String[] getLengthList() {
    String[] list = {"angstrom", "bohr", "meter", "centimeter",
		     "millimeter", "micrometer", "nanometer"};
    return list;
  }
  
  public static String[] getEnergyList() {
    String[] list={"Joule", "eV", "meV", 
		   "centimeter^-1", "millimeter^-1",
		   "micrometer^-1", "nanometer^-1"};
    return list;
  }
  
  public static String[] getFormatedEnergyList() {
    String[] list={"Energy \\ (J)", "Energy \\ (eV)", "Energy \\ (meV)", 
		   "\\S l \\N \\ ( cm^-1 )",  "\\S l \\N \\ ( mm^-1 )",
		   "\\S l \\N \\ ( \\S m \\N m^-1 )", "\\S l \\N \\ ( nm^-1 )"};
    return list;
  }

} //end class Units
