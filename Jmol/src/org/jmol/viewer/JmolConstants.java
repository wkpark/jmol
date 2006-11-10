/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.util.Logger;

import java.util.Hashtable;
import java.util.BitSet;

final public class JmolConstants {

  // for now, just update this by hand
  // perhaps use ant filter later ... but mth doesn't like it :-(
  public final static String copyright = "(C) 2006 Jmol Development";
  public final static String version = "10.9.97"; //show state  
  public final static String cvsDate = "$Date$";
  public final static String date = cvsDate.substring(7, 23);
    
  public final static boolean officialRelease = false;

  public final static String DEFAULT_HELP_PATH = "http://www.stolaf.edu/academics/chemapps/jmol/docs/index.htm?search=";

  public final static int DEFAULT_PERCENT_VDW_ATOM = 20;
  public final static float DEFAULT_BOND_RADIUS = 0.15f;
  public final static short DEFAULT_BOND_MILLIANGSTROM_RADIUS = (short) (DEFAULT_BOND_RADIUS * 1000);
  //angstroms of slop ... from OpenBabel ... mth 2003 05 26
  public final static float DEFAULT_BOND_TOLERANCE = 0.45f;
  //minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  public final static float DEFAULT_MIN_BOND_DISTANCE = 0.4f;


  public final static int DELETE_BONDS     = 0;
  public final static int MODIFY_ONLY      = 1;
  public final static int CREATE_ONLY      = 2;
  public final static int MODIFY_OR_CREATE = 3;
  public final static int AUTO_BOND        = 4;

  public final static float DEFAULT_MAX_CONNECT_DISTANCE = 100000000f;
  public final static float DEFAULT_MIN_CONNECT_DISTANCE = 0.1f;
  
  private final static String[] connectOperationStrings =
  { "delete", "modify", "create", "modifyOrCreate", "auto" };

  public static int connectOperationFromString(String connectOperationString) {
    int i;
    for (i = connectOperationStrings.length; --i >= 0; )
      if (connectOperationStrings[i].equalsIgnoreCase(connectOperationString))
        break;
    return i;
  }
  
  public static String connectOperationName(int i) {
    return connectOperationStrings[i];
  }
  
  public final static short MAR_DELETED = Short.MIN_VALUE;

  public static final int MOUSE_NONE = -1;
  public static final int MOUSE_ROTATE = 0;
  public static final int MOUSE_ZOOM = 1;
  public static final int MOUSE_XLATE = 2;
  public static final int MOUSE_PICK = 3;
  public static final int MOUSE_DELETE = 4;
  public static final int MOUSE_MEASURE = 5;
  public static final int MOUSE_ROTATE_Z = 6;
  public static final int MOUSE_SLAB_PLANE = 7;
  public static final int MOUSE_POPUP_MENU = 8;

  public final static byte MULTIBOND_NEVER =     0;
  public final static byte MULTIBOND_WIREFRAME = 1;
  public final static byte MULTIBOND_NOTSMALL =  2;
  public final static byte MULTIBOND_ALWAYS =    3;

  public final static short madMultipleBondSmallMaximum = 500;

  /**
   * axes modes
   */

  public final static int AXES_MODE_BOUNDBOX = 0;
  public final static int AXES_MODE_MOLECULAR = 1;
  public final static int AXES_MODE_UNITCELL = 2;

  
  /**
   * picking modes
   */
  public final static int PICKING_OFF    = 0;
  public final static int PICKING_IDENT  = 1;
  public final static int PICKING_LABEL  = 2;
  public final static int PICKING_CENTER = 3;
  public final static int PICKING_DRAW   = 4;
  public final static int PICKING_SPIN   = 5;
  //public final static int PICKING_COORD  = 6;
  //public final static int PICKING_BOND   = 7;
  public final static int PICKING_SELECT_ATOM      =  8;
  public final static int PICKING_SELECT_GROUP     =  9;
  public final static int PICKING_SELECT_CHAIN     = 10;
  public final static int PICKING_SELECT_MOLECULE  = 11;
  public final static int PICKING_SELECT_SITE      = 12;
  public final static int PICKING_SELECT_ELEMENT   = 13;
  public final static int PICKING_MEASURE          = 14;
  public final static int PICKING_MEASURE_DISTANCE = 15;
  public final static int PICKING_MEASURE_ANGLE    = 16;
  public final static int PICKING_MEASURE_TORSION  = 17;

  public final static String[] pickingModeNames = {
    "off", "ident", "label", "center", "draw", "spin",
    "coord NOT IMPLEMENTED", "bond NOT IMPLEMENTED", 
    "atom", "group", "chain", "molecule", "site", "element", 
    "measure", "distance", "angle", "torsion"
  };
 
  public final static int GetPickingMode(String str) {
    for (int i = pickingModeNames.length; --i >= 0; )
      if (str.equalsIgnoreCase(pickingModeNames[i]))
        return i;
    return -1;
  }
  /**
   * picking styles
   */
  public final static int PICKINGSTYLE_SELECT_JMOL = 0;
  public final static int PICKINGSTYLE_SELECT_CHIME = 0;
  public final static int PICKINGSTYLE_SELECT_RASMOL = 1;
  public final static int PICKINGSTYLE_SELECT_PFAAT = 2;
  public final static int PICKINGSTYLE_MEASURE_ON = 3;
  public final static int PICKINGSTYLE_MEASURE_OFF = 4;
  
  public final static String[] pickingStyleNames = {
    "toggle", "selectOrToggle", "extendedSelect", 
    "measure", "measureoff"
  };

  public final static int GetPickingStyle(String str) {
    for (int i = pickingStyleNames.length; --i >= 0; )
      if (str.equalsIgnoreCase(pickingStyleNames[i]))
        return i;
    return -1;
  }

  /**
   * Extended Bond Definition Types
   *
   */
  public final static short BOND_ORDER_NULL      = -1;
  public final static short BOND_COVALENT_SINGLE = 1;
  public final static short BOND_COVALENT_DOUBLE = 2;
  public final static short BOND_COVALENT_TRIPLE = 3;
  public final static short BOND_COVALENT_MASK   = 3;
  public final static short BOND_AROMATIC_MASK = (1 << 2);
  public final static short BOND_AROMATIC      = (1 << 2) | 1;
  public final static short BOND_STEREO_MASK   = (3 << 3);
  public final static short BOND_STEREO_NEAR   = (1 << 3) | 1;
  public final static short BOND_STEREO_FAR    = (2 << 3) | 2;
  public final static short BOND_SULFUR_MASK   = (1 << 5);
  public final static short BOND_HBOND_SHIFT = 6;
  public final static short BOND_HYDROGEN_MASK = (0x0F << BOND_HBOND_SHIFT);
  public final static short BOND_H_REGULAR     = (1 << BOND_HBOND_SHIFT);
  public final static short BOND_H_PLUS_2      = (2 << BOND_HBOND_SHIFT);
  public final static short BOND_H_PLUS_3      = (3 << BOND_HBOND_SHIFT);
  public final static short BOND_H_PLUS_4      = (4 << BOND_HBOND_SHIFT);
  public final static short BOND_H_PLUS_5      = (5 << BOND_HBOND_SHIFT);
  public final static short BOND_H_MINUS_3     = (6 << BOND_HBOND_SHIFT);
  public final static short BOND_H_MINUS_4     = (7 << BOND_HBOND_SHIFT);
  public final static short BOND_H_NUCLEOTIDE  = (8 << BOND_HBOND_SHIFT);
  public final static short BOND_PARTIAL01     = (1 << 10) | 1;
  public final static short BOND_PARTIAL12     = (1 << 11) | 2;
  public final static short BOND_PARTIAL_MASK  = (3 << 10);

  public final static short BOND_ALL_MASK      = (short)0xFFFF;

  final static String[] bondOrderNames = {
    "single", "double", "triple", "aromatic", "hbond", "partial", "partialDouble"
  };

  final static short[] bondOrderValues = {
    BOND_COVALENT_SINGLE, BOND_COVALENT_DOUBLE, BOND_COVALENT_TRIPLE,
    BOND_AROMATIC, BOND_H_REGULAR, BOND_PARTIAL01, BOND_PARTIAL12
  };

  public final static short getBondOrderFromString(String bondOrderString) {
    for (int i = bondOrderNames.length; --i >= 0; ) {
      if (bondOrderNames[i].equalsIgnoreCase(bondOrderString))
        return bondOrderValues[i];
    }
    return BOND_ORDER_NULL;
  }
  
  public final static String getBondOrderNameFromOrder(short order) {
    if (order == BOND_ORDER_NULL)
      return "";
    if ((order & BOND_HYDROGEN_MASK) != 0)
      return "hbond";
    if ((order & BOND_SULFUR_MASK) != 0)
      return "single";
    for (int i = bondOrderValues.length; --i >= 0; ) {
      if (bondOrderValues[i] == order)
        return bondOrderNames[i];
    }
    return "?";
  }


  /* .cube files need this */
  final static float ANGSTROMS_PER_BOHR = 0.5291772f;

  public final static int[] argbsHbondType =
  {
    0xFFFF69B4, // unused - pink
    0xFFFFFF00, // regular yellow
    0xFFFFFFFF, // +2 white
    0xFFFF00FF, // +3 magenta
    0xFFFF0000, // +4 red
    0xFFFFA500, // +5 orange
    0xFF00FFFF, // -3 cyan
    0xFF00FF00, // -4 green
    0xFFFF8080, // nucleotide
  };

  /**
   * The default elementSymbols. Presumably the only entry which may cause
   * confusion is element 0, whose symbol we have defined as "Xx". 
   */
  private final static String[] elementSymbols = {
    "Xx", // 0
    "H",  // 1
    "He", // 2
    "Li", // 3
    "Be", // 4
    "B",  // 5
    "C",  // 6
    "N",  // 7
    "O",  // 8
    "F",  // 9
    "Ne", // 10
    "Na", // 11
    "Mg", // 12
    "Al", // 13
    "Si", // 14
    "P",  // 15
    "S",  // 16
    "Cl", // 17
    "Ar", // 18
    "K",  // 19
    "Ca", // 20
    "Sc", // 21
    "Ti", // 22
    "V",  // 23
    "Cr", // 24
    "Mn", // 25
    "Fe", // 26
    "Co", // 27
    "Ni", // 28
    "Cu", // 29
    "Zn", // 30
    "Ga", // 31
    "Ge", // 32
    "As", // 33
    "Se", // 34
    "Br", // 35
    "Kr", // 36
    "Rb", // 37
    "Sr", // 38
    "Y",  // 39
    "Zr", // 40
    "Nb", // 41
    "Mo", // 42
    "Tc", // 43
    "Ru", // 44
    "Rh", // 45
    "Pd", // 46
    "Ag", // 47
    "Cd", // 48
    "In", // 49
    "Sn", // 50
    "Sb", // 51
    "Te", // 52
    "I",  // 53
    "Xe", // 54
    "Cs", // 55
    "Ba", // 56
    "La", // 57
    "Ce", // 58
    "Pr", // 59
    "Nd", // 60
    "Pm", // 61
    "Sm", // 62
    "Eu", // 63
    "Gd", // 64
    "Tb", // 65
    "Dy", // 66
    "Ho", // 67
    "Er", // 68
    "Tm", // 69
    "Yb", // 70
    "Lu", // 71
    "Hf", // 72
    "Ta", // 73
    "W",  // 74
    "Re", // 75
    "Os", // 76
    "Ir", // 77
    "Pt", // 78
    "Au", // 79
    "Hg", // 80
    "Tl", // 81
    "Pb", // 82
    "Bi", // 83
    "Po", // 84
    "At", // 85
    "Rn", // 86
    "Fr", // 87
    "Ra", // 88
    "Ac", // 89
    "Th", // 90
    "Pa", // 91
    "U",  // 92
    "Np", // 93
    "Pu", // 94
    "Am", // 95
    "Cm", // 96
    "Bk", // 97
    "Cf", // 98
    "Es", // 99
    "Fm", // 100
    "Md", // 101
    "No", // 102
    "Lr", // 103
    "Rf", // 104
    "Db", // 105
    "Sg", // 106
    "Bh", // 107
    "Hs", // 108
    "Mt", // 109
    /*
    "Ds", // 110
    "Uuu",// 111
    "Uub",// 112
    "Uut",// 113
    "Uuq",// 114
    "Uup",// 115
    "Uuh",// 116
    "Uus",// 117
    "Uuo",// 118
    */
  };

  /**
   * one larger than the last elementNumber, same as elementSymbols.length
   */
  public final static int elementNumberMax = elementSymbols.length;

  private static Hashtable htElementMap;

  /**
   * @param elementSymbol First char must be upper case, second char accepts upper or lower case
   * @return elementNumber = atomicNumber + IsotopeNumber*128
   */
  public final static short elementNumberFromSymbol(String elementSymbol) {
    if (htElementMap == null) {
      Hashtable map = new Hashtable();
      for (int elementNumber = elementNumberMax; --elementNumber >= 0;) {
        String symbol = elementSymbols[elementNumber];
        Integer boxed = new Integer(elementNumber);
        map.put(symbol, boxed);
        if (symbol.length() == 2)
          map.put(symbol.toUpperCase(), boxed);
      }
      for (int i = altElementMax; --i >= firstIsotope;) {
        String symbol = altElementSymbols[i];
        Integer boxed = new Integer(altElementNumbers[i]);
        map.put(symbol, boxed);
        if (symbol.length() == 2)
          map.put(symbol.toUpperCase(), boxed);
      }
      htElementMap = map;
    }
    if (elementSymbol == null)
      return 0;
    Integer boxedAtomicNumber = (Integer) htElementMap.get(elementSymbol);
    if (boxedAtomicNumber != null)
      return (short) boxedAtomicNumber.intValue();
    Logger.error("" + elementSymbol + "' is not a recognized symbol");
    return 0;
  }
  
  /**
   * @param elementNumber may be atomicNumber + isotopeNumber*128
   * @return elementSymbol
   */
  public final static String elementSymbolFromNumber(int elementNumber) {
    //Isotopes as atomicNumber + IsotopeNumber * 128
    if (elementNumber >= elementNumberMax) {
      for (int j = altElementMax; --j >= 0;)
        if (elementNumber == altElementNumbers[j])
          return altElementSymbols[j];
      elementNumber %= 128;
    }
    if (elementNumber < 0 || elementNumber >= elementNumberMax)
      elementNumber = 0;
    return elementSymbols[elementNumber];
  }

  /**
   * @param elementNumber may be atomicNumber + isotopeNumber*128
   * @return elementName
   */
  public final static String elementNameFromNumber(int elementNumber) {
    //Isotopes as atomicNumber + IsotopeNumber * 128
    if (elementNumber >= elementNumberMax) {
      for (int j = altElementMax; --j >= 0;)
        if (elementNumber == altElementNumbers[j])
          return altElementNames[j];
      elementNumber %= 128;
    }
    if (elementNumber < 0 || elementNumber >= elementNumberMax)
      elementNumber = 0;
    return elementNames[elementNumber];
  }

  private final static String elementNames[] = {
    "unknown",       //  0
    "hydrogen",      //  1
    "helium",        //  2
    "lithium",       //  3
    "beryllium",     //  4
    "boron",         //  5
    "carbon",        //  6
    "nitrogen",      //  7
    "oxygen",        //  8
    "fluorine",      //  9
    "neon",          // 10
    "sodium",        // 11
    "magnesium",     // 12
    "aluminum",      // 13 aluminium
    "silicon",       // 14
    "phosphorus",    // 15
    "sulfur",        // 16 sulphur
    "chlorine",      // 17
    "argon",         // 18
    "potassium",     // 19
    "calcium",       // 20
    "scandium",      // 21
    "titanium",      // 22
    "vanadium",      // 23
    "chromium",      // 24
    "manganese",     // 25
    "iron",          // 26
    "cobalt",        // 27
    "nickel",        // 28
    "copper",        // 29
    "zinc",          // 30
    "gallium",       // 31
    "germanium",     // 32
    "arsenic",       // 33
    "selenium",      // 34
    "bromine",       // 35
    "krypton",       // 36
    "rubidium",      // 37
    "strontium",     // 38
    "yttrium",       // 39
    "zirconium",     // 40
    "niobium",       // 41
    "molybdenum",    // 42
    "technetium",    // 43
    "ruthenium",     // 44
    "rhodium",       // 45
    "palladium",     // 46
    "silver",        // 47
    "cadmium",       // 48
    "indium",        // 49
    "tin",           // 50
    "antimony",      // 51
    "tellurium",     // 52
    "iodine",        // 53
    "xenon",         // 54
    "cesium",        // 55  caesium
    "barium",        // 56
    "lanthanum",     // 57
    "cerium",        // 58
    "praseodymium",  // 59
    "neodymium",     // 60
    "promethium",    // 61
    "samarium",      // 62
    "europium",      // 63
    "gadolinium",    // 64
    "terbium",       // 66
    "dysprosium",    // 66
    "holmium",       // 67
    "erbium",        // 68
    "thulium",       // 69
    "ytterbium",     // 70
    "lutetium",      // 71
    "hafnium",       // 72
    "tantalum",      // 73
    "tungsten",      // 74
    "rhenium",       // 75
    "osmium",        // 76
    "iridium",       // 77
    "platinum",      // 78
    "gold",          // 79
    "mercury",       // 80
    "thallium",      // 81
    "lead",          // 82
    "bismuth",       // 83
    "polonium",      // 84
    "astatine",      // 85
    "radon",         // 86
    "francium",      // 87
    "radium",        // 88
    "actinium",      // 89
    "thorium",       // 90
    "protactinium",  // 91
    "uranium",       // 92
    "neptunium",     // 93
    "plutonium",     // 94
    "americium",     // 95
    "curium",        // 96
    "berkelium",     // 97
    "californium",   // 98
    "einsteinium",   // 99
    "fermium",       // 100
    "mendelevium",   // 101
    "nobelium",      // 102
    "lawrencium",    // 103
    "rutherfordium", // 104
    "dubnium",       // 105
    "seaborgium",    // 106
    "bohrium",       // 107
    "hassium",       // 108
    "meitnerium"     // 109
  };

  /**
   * @param i index into altElementNames
   * @return elementName
   */
  public final static String altElementNameFromIndex(int i) {
    return altElementNames[i];
  }
  
  /**
   * @param i index into altElementNumbers
   * @return elementNumber (may be atomicNumber + isotopeNumber*128)
   */
  public final static short altElementNumberFromIndex(int i) {
    return altElementNumbers[i];
  }
  
  /**
   * @param i index into altElementSymbols
   * @return elementSymbol
   */
  public final static String altElementSymbolFromIndex(int i) {
    return altElementSymbols[i];
  }
  
  /**
   * @param i index into altElementSymbols
   * @return 2H
   */
  public final static String altIsotopeSymbolFromIndex(int i) {
    int code = altElementNumbers[i]; 
    return (code >> 8) + elementSymbolFromNumber(code );
  }
  
  /**
   * @param atomicAndIsotopeNumber (may be atomicNumber + isotopeNumber*128)
   * @return  index into altElementNumbers
   */
  public final static int altElementIndexFromNumber(int atomicAndIsotopeNumber) {
    for (int i = 0; i < altElementMax; i++)
      if (altElementNumbers[i] == atomicAndIsotopeNumber)
        return i;
    return 0;
  }
    
  // add as we go
  private final static String naturalIsotopes = "1H,12C,14N,";

  public final static boolean isNaturalIsotope(String isotopeSymbol) {
    return (naturalIsotopes.indexOf(isotopeSymbol + ",") >= 0);      
  }

  private final static short[] altElementNumbers = {
    0,
    13,
    16,
    55,
    (2 << 7) + 1, // D = 2*128 + 1 <-- firstIsotope
    (3 << 7) + 1, // T = 3*128 + 1
    (13 << 7) + 6, // 13C
    (14 << 7) + 6, // 14C
    (15 << 7) + 7, // 15N
  };

  
  private final static String[] altElementSymbols = {
    "Xx",
    "Al",
    "S",
    "Cs",
    "D",
    "T",
    "13C",
    "14C",
    "15N",
  };

  private final static String[] altElementNames = {
    "dummy",
    "aluminium",
    "sulphur",
    "caesium",
    "deuterium",
    "tritium",
    "",
    "",
    "",
  };
  
  public final static int[] altArgbsCpk = {
    0xFFFF1493, // Xx 0
    0xFFBFA6A6, // Al 13
    0xFFFFFF30, // S  16
    0xFF57178F, // Cs 55
    0xFFFFFFC0, // D 2H
    0xFFFFFFA0, // T 3H
    0xFF505050, // 13C  6 - darker
    0xFF404040, // 14C  6 - darker still
    0xFF105050, // 15N  7 - darker
  };

  
  /**
   * first entry of an actual isotope int the altElementSymbols, altElementNames, altElementNumbers arrays
   */
  public final static int firstIsotope = 4;
  
  /**
   * length of the altElementSymbols, altElementNames, altElementNumbers arrays
   */
  public final static int altElementMax = altElementNumbers.length;
  
  /**
   * Default table of van der Waals Radii.
   * values are stored as MAR -- Milli Angstrom Radius
   * Used for spacefill rendering of atoms.
   * Values taken from OpenBabel.
   * @see <a href="http://openbabel.sourceforge.net">openbabel.sourceforge.net</a>
   */
  public final static short[] vanderwaalsMars = {
    1000, //   0  Xx big enough to see
    1200, //   1  H
    1400, //   2  He
    1820, //   3  Li
    1700, //   4  Be
    2080, //   5  B
    1950, //   6  C
    1850, //   7  N
    1700, //   8  O
    1730, //   9  F
    1540, //  10  Ne
    2270, //  11  Na
    1730, //  12  Mg
    2050, //  13  Al
    2100, //  14  Si
    2080, //  15  P
    2000, //  16  S
    1970, //  17  Cl
    1880, //  18  Ar
    2750, //  19  K
    1973, //  20  Ca
    1700, //  21  Sc
    1700, //  22  Ti
    1700, //  23  V
    1700, //  24  Cr
    1700, //  25  Mn
    1700, //  26  Fe
    1700, //  27  Co
    1630, //  28  Ni
    1400, //  29  Cu
    1390, //  30  Zn
    1870, //  31  Ga
    1700, //  32  Ge
    1850, //  33  As
    1900, //  34  Se
    2100, //  35  Br
    2020, //  36  Kr
    1700, //  37  Rb
    1700, //  38  Sr
    1700, //  39  Y
    1700, //  40  Zr
    1700, //  41  Nb
    1700, //  42  Mo
    1700, //  43  Tc
    1700, //  44  Ru
    1700, //  45  Rh
    1630, //  46  Pd
    1720, //  47  Ag
    1580, //  48  Cd
    1930, //  49  In
    2170, //  50  Sn
    2200, //  51  Sb
    2060, //  52  Te
    2150, //  53  I
    2160, //  54  Xe
    1700, //  55  Cs
    1700, //  56  Ba
    1700, //  57  La
    1700, //  58  Ce
    1700, //  59  Pr
    1700, //  60  Nd
    1700, //  61  Pm
    1700, //  62  Sm
    1700, //  63  Eu
    1700, //  64  Gd
    1700, //  65  Tb
    1700, //  66  Dy
    1700, //  67  Ho
    1700, //  68  Er
    1700, //  69  Tm
    1700, //  70  Yb
    1700, //  71  Lu
    1700, //  72  Hf
    1700, //  73  Ta
    1700, //  74  W
    1700, //  75  Re
    1700, //  76  Os
    1700, //  77  Ir
    1720, //  78  Pt
    1660, //  79  Au
    1550, //  80  Hg
    1960, //  81  Tl
    2020, //  82  Pb
    1700, //  83  Bi
    1700, //  84  Po
    1700, //  85  At
    1700, //  86  Rn
    1700, //  87  Fr
    1700, //  88  Ra
    1700, //  89  Ac
    1700, //  90  Th
    1700, //  91  Pa
    1860, //  92  U
    1700, //  93  Np
    1700, //  94  Pu
    1700, //  95  Am
    1700, //  96  Cm
    1700, //  97  Bk
    1700, //  98  Cf
    1700, //  99  Es
    1700, // 100  Fm
    1700, // 101  Md
    1700, // 102  No
    1700, // 103  Lr
    1700, // 104  Rf
    1700, // 105  Db
    1700, // 106  Sg
    1700, // 107  Bh
    1700, // 108  Hs
    1700, // 109  Mt
  };

  /**
   * Default table of covalent Radii
   * stored as a short mar ... Milli Angstrom Radius
   * Values taken from OpenBabel.
   * @see <a href="http://openbabel.sourceforge.net">openbabel.sourceforge.net</a>
   */
  private final static short[] covalentMars = {
    0,    //   0  Xx does not bond
    230,  //   1  H
    930,  //   2  He
    680,  //   3  Li
    350,  //   4  Be
    830,  //   5  B
    680,  //   6  C
    680,  //   7  N
    680,  //   8  O
    640,  //   9  F
    1120, //  10  Ne
    970,  //  11  Na
    1100, //  12  Mg
    1350, //  13  Al
    1200, //  14  Si
    750,  //  15  P
    1020, //  16  S
    990,  //  17  Cl
    1570, //  18  Ar
    1330, //  19  K
    990,  //  20  Ca
    1440, //  21  Sc
    1470, //  22  Ti
    1330, //  23  V
    1350, //  24  Cr
    1350, //  25  Mn
    1340, //  26  Fe
    1330, //  27  Co
    1500, //  28  Ni
    1520, //  29  Cu
    1450, //  30  Zn
    1220, //  31  Ga
    1170, //  32  Ge
    1210, //  33  As
    1220, //  34  Se
    1210, //  35  Br
    1910, //  36  Kr
    1470, //  37  Rb
    1120, //  38  Sr
    1780, //  39  Y
    1560, //  40  Zr
    1480, //  41  Nb
    1470, //  42  Mo
    1350, //  43  Tc
    1400, //  44  Ru
    1450, //  45  Rh
    1500, //  46  Pd
    1590, //  47  Ag
    1690, //  48  Cd
    1630, //  49  In
    1460, //  50  Sn
    1460, //  51  Sb
    1470, //  52  Te
    1400, //  53  I
    1980, //  54  Xe
    1670, //  55  Cs
    1340, //  56  Ba
    1870, //  57  La
    1830, //  58  Ce
    1820, //  59  Pr
    1810, //  60  Nd
    1800, //  61  Pm
    1800, //  62  Sm
    1990, //  63  Eu
    1790, //  64  Gd
    1760, //  65  Tb
    1750, //  66  Dy
    1740, //  67  Ho
    1730, //  68  Er
    1720, //  69  Tm
    1940, //  70  Yb
    1720, //  71  Lu
    1570, //  72  Hf
    1430, //  73  Ta
    1370, //  74  W
    1350, //  75  Re
    1370, //  76  Os
    1320, //  77  Ir
    1500, //  78  Pt
    1500, //  79  Au
    1700, //  80  Hg
    1550, //  81  Tl
    1540, //  82  Pb
    1540, //  83  Bi
    1680, //  84  Po
    1700, //  85  At
    2400, //  86  Rn
    2000, //  87  Fr
    1900, //  88  Ra
    1880, //  89  Ac
    1790, //  90  Th
    1610, //  91  Pa
    1580, //  92  U
    1550, //  93  Np
    1530, //  94  Pu
    1510, //  95  Am
    1500, //  96  Cm
    1500, //  97  Bk
    1500, //  98  Cf
    1500, //  99  Es
    1500, // 100  Fm
    1500, // 101  Md
    1500, // 102  No
    1500, // 103  Lr
    1600, // 104  Rf
    1600, // 105  Db
    1600, // 106  Sg
    1600, // 107  Bh
    1600, // 108  Hs
    1600, // 109  Mt
  };

  /****************************************************************
   * ionic radii are looked up using an array of shorts (16 bits each) 
   * that contains the atomic number, the charge, and the radius in two
   * consecutive values, encoded as follows:
   * 
   *   (atomicNumber << 4) + (charge + 4), radiusAngstroms*1000
   * 
   * That is, (atomicNumber * 16 + charge + 4), milliAngstromRadius
   * 
   * This allows for charges from -4 to 11, but we only really have -4 to 7.
   *
   * This data is from
   *  Handbook of Chemistry and Physics. 48th Ed, 1967-8, p. F143
   *  (scanned for Jmol by Phillip Barak, Jan 2004)
   *  
   * Reorganized from two separate arrays 9/2006 by Bob Hanson, who thought
   * it was just too hard to look these up and, if necessary, add or modify.
   * At the same time, the table was split into cations and anions for easier
   * retrieval.
   * 
   *  Bob Hanson is suspicious of Sn(-IV)
   *  
   ****************************************************************/

  public final static int FORMAL_CHARGE_MIN = -4;
  public final static int FORMAL_CHARGE_MAX = 7;
  
  private final static short[] cationLookupTable = {
    (3 << 4) + (1 + 4),   680,  // "Li+1"
    (4 << 4) + (1 + 4),   440,  // "Be+1"
    (4 << 4) + (2 + 4),   350,  // "Be+2"
    (5 << 4) + (1 + 4),   350,  // "B+1"
    (5 << 4) + (3 + 4),   230,  // "B+3"
    (6 << 4) + (4 + 4),   160,  // "C+4"
    (7 << 4) + (1 + 4),   250,  // "N+1"
    (7 << 4) + (3 + 4),   160,  // "N+3"
    (7 << 4) + (5 + 4),   130,  // "N+5"
    (8 << 4) + (1 + 4),   220,  // "O+1"
    (8 << 4) + (6 + 4),   90,   // "O+6"
    (9 << 4) + (7 + 4),   80,   // "F+7"
    (10 << 4) + (1 + 4),  1120, // "Ne+1"
    (11 << 4) + (1 + 4),  970,  // "Na+1"
    (12 << 4) + (1 + 4),  820,  // "Mg+1"
    (12 << 4) + (2 + 4),  660,  // "Mg+2"
    (13 << 4) + (3 + 4),  510,  // "Al+3"
    (14 << 4) + (1 + 4),  650,  // "Si+1"
    (14 << 4) + (4 + 4),  420,  // "Si+4"
    (15 << 4) + (3 + 4),  440,  // "P+3"
    (15 << 4) + (5 + 4),  350,  // "P+5"
    (16 << 4) + (2 + 4),  2190, // "S+2"
    (16 << 4) + (4 + 4),  370,  // "S+4"
    (16 << 4) + (6 + 4),  300,  // "S+6"
    (17 << 4) + (5 + 4),  340,  // "Cl+5"
    (17 << 4) + (7 + 4),  270,  // "Cl+7"
    (18 << 4) + (1 + 4),  1540, // "Ar+1"
    (19 << 4) + (1 + 4),  1330, // "K+1"
    (20 << 4) + (1 + 4),  1180, // "Ca+1"
    (20 << 4) + (2 + 4),  990,  // "Ca+2"
    (21 << 4) + (3 + 4),  732,  // "Sc+3"
    (22 << 4) + (1 + 4),  960,  // "Ti+1"
    (22 << 4) + (2 + 4),  940,  // "Ti+2"
    (22 << 4) + (3 + 4),  760,  // "Ti+3"
    (22 << 4) + (4 + 4),  680,  // "Ti+4"
    (23 << 4) + (2 + 4),  880,  // "V+2"
    (23 << 4) + (3 + 4),  740,  // "V+3"
    (23 << 4) + (4 + 4),  630,  // "V+4"
    (23 << 4) + (5 + 4),  590,  // "V+5"
    (24 << 4) + (1 + 4),  810,  // "Cr+1"
    (24 << 4) + (2 + 4),  890,  // "Cr+2"
    (24 << 4) + (3 + 4),  630,  // "Cr+3"
    (24 << 4) + (6 + 4),  520,  // "Cr+6"
    (25 << 4) + (2 + 4),  800,  // "Mn+2"
    (25 << 4) + (3 + 4),  660,  // "Mn+3"
    (25 << 4) + (4 + 4),  600,  // "Mn+4"
    (25 << 4) + (7 + 4),  460,  // "Mn+7"
    (26 << 4) + (2 + 4),  740,  // "Fe+2"
    (26 << 4) + (3 + 4),  640,  // "Fe+3"
    (27 << 4) + (2 + 4),  720,  // "Co+2"
    (27 << 4) + (3 + 4),  630,  // "Co+3"
    (28 << 4) + (2 + 4),  690,  // "Ni+2"
    (29 << 4) + (1 + 4),  960,  // "Cu+1"
    (29 << 4) + (2 + 4),  720,  // "Cu+2"
    (30 << 4) + (1 + 4),  880,  // "Zn+1"
    (30 << 4) + (2 + 4),  740,  // "Zn+2"
    (31 << 4) + (1 + 4),  810,  // "Ga+1"
    (31 << 4) + (3 + 4),  620,  // "Ga+3"
    (32 << 4) + (2 + 4),  730,  // "Ge+2"
    (32 << 4) + (4 + 4),  530,  // "Ge+4"
    (33 << 4) + (3 + 4),  580,  // "As+3"
    (33 << 4) + (5 + 4),  460,  // "As+5"
    (34 << 4) + (1 + 4),  660,  // "Se+1"
    (34 << 4) + (4 + 4),  500,  // "Se+4"
    (34 << 4) + (6 + 4),  420,  // "Se+6"
    (35 << 4) + (5 + 4),  470,  // "Br+5"
    (35 << 4) + (7 + 4),  390,  // "Br+7"
    (37 << 4) + (1 + 4),  1470, // "Rb+1"
    (38 << 4) + (2 + 4),  1120, // "Sr+2"
    (39 << 4) + (3 + 4),  893,  // "Y+3"
    (40 << 4) + (1 + 4),  1090, // "Zr+1"
    (40 << 4) + (4 + 4),  790,  // "Zr+4"
    (41 << 4) + (1 + 4),  1000, // "Nb+1"
    (41 << 4) + (4 + 4),  740,  // "Nb+4"
    (41 << 4) + (5 + 4),  690,  // "Nb+5"
    (42 << 4) + (1 + 4),  930,  // "Mo+1"
    (42 << 4) + (4 + 4),  700,  // "Mo+4"
    (42 << 4) + (6 + 4),  620,  // "Mo+6"
    (43 << 4) + (7 + 4),  979,  // "Tc+7"
    (44 << 4) + (4 + 4),  670,  // "Ru+4"
    (45 << 4) + (3 + 4),  680,  // "Rh+3"
    (46 << 4) + (2 + 4),  800,  // "Pd+2"
    (46 << 4) + (4 + 4),  650,  // "Pd+4"
    (47 << 4) + (1 + 4),  1260, // "Ag+1"
    (47 << 4) + (2 + 4),  890,  // "Ag+2"
    (48 << 4) + (1 + 4),  1140, // "Cd+1"
    (48 << 4) + (2 + 4),  970,  // "Cd+2"
    (49 << 4) + (3 + 4),  810,  // "In+3"
    (50 << 4) + (2 + 4),  930,  // "Sn+2"
    (50 << 4) + (4 + 4),  710,  // "Sn+4"
    (51 << 4) + (3 + 4),  760,  // "Sb+3"
    (51 << 4) + (5 + 4),  620,  // "Sb+5"
    (52 << 4) + (1 + 4),  820,  // "Te+1"
    (52 << 4) + (4 + 4),  700,  // "Te+4"
    (52 << 4) + (6 + 4),  560,  // "Te+6"
    (53 << 4) + (5 + 4),  620,  // "I+5"
    (53 << 4) + (7 + 4),  500,  // "I+7"
    (55 << 4) + (1 + 4),  1670, // "Cs+1"
    (56 << 4) + (1 + 4),  1530, // "Ba+1"
    (56 << 4) + (2 + 4),  1340, // "Ba+2"
    (57 << 4) + (1 + 4),  1390, // "La+1"
    (57 << 4) + (3 + 4),  1016, // "La+3"
    (58 << 4) + (1 + 4),  1270, // "Ce+1"
    (58 << 4) + (3 + 4),  1034, // "Ce+3"
    (58 << 4) + (4 + 4),  920,  // "Ce+4"
    (59 << 4) + (3 + 4),  1013, // "Pr+3"
    (59 << 4) + (4 + 4),  900,  // "Pr+4"
    (60 << 4) + (3 + 4),  995,  // "Nd+3"
    (61 << 4) + (3 + 4),  979,  // "Pm+3"
    (62 << 4) + (3 + 4),  964,  // "Sm+3"
    (63 << 4) + (2 + 4),  1090, // "Eu+2"
    (63 << 4) + (3 + 4),  950,  // "Eu+3"
    (64 << 4) + (3 + 4),  938,  // "Gd+3"
    (65 << 4) + (3 + 4),  923,  // "Tb+3"
    (65 << 4) + (4 + 4),  840,  // "Tb+4"
    (66 << 4) + (3 + 4),  908,  // "Dy+3"
    (67 << 4) + (3 + 4),  894,  // "Ho+3"
    (68 << 4) + (3 + 4),  881,  // "Er+3"
    (69 << 4) + (3 + 4),  870,  // "Tm+3"
    (70 << 4) + (2 + 4),  930,  // "Yb+2"
    (70 << 4) + (3 + 4),  858,  // "Yb+3"
    (71 << 4) + (3 + 4),  850,  // "Lu+3"
    (72 << 4) + (4 + 4),  780,  // "Hf+4"
    (73 << 4) + (5 + 4),  680,  // "Ta+5"
    (74 << 4) + (4 + 4),  700,  // "W+4"
    (74 << 4) + (6 + 4),  620,  // "W+6"
    (75 << 4) + (4 + 4),  720,  // "Re+4"
    (75 << 4) + (7 + 4),  560,  // "Re+7"
    (76 << 4) + (4 + 4),  880,  // "Os+4"
    (76 << 4) + (6 + 4),  690,  // "Os+6"
    (77 << 4) + (4 + 4),  680,  // "Ir+4"
    (78 << 4) + (2 + 4),  800,  // "Pt+2"
    (78 << 4) + (4 + 4),  650,  // "Pt+4"
    (79 << 4) + (1 + 4),  1370, // "Au+1"
    (79 << 4) + (3 + 4),  850,  // "Au+3"
    (80 << 4) + (1 + 4),  1270, // "Hg+1"
    (80 << 4) + (2 + 4),  1100, // "Hg+2"
    (81 << 4) + (1 + 4),  1470, // "Tl+1"
    (81 << 4) + (3 + 4),  950,  // "Tl+3"
    (82 << 4) + (2 + 4),  1200, // "Pb+2"
    (82 << 4) + (4 + 4),  840,  // "Pb+4"
    (83 << 4) + (1 + 4),  980,  // "Bi+1"
    (83 << 4) + (3 + 4),  960,  // "Bi+3"
    (83 << 4) + (5 + 4),  740,  // "Bi+5"
    (84 << 4) + (6 + 4),  670,  // "Po+6"
    (85 << 4) + (7 + 4),  620,  // "At+7"
    (87 << 4) + (1 + 4),  1800, // "Fr+1"
    (88 << 4) + (2 + 4),  1430, // "Ra+2"
    (89 << 4) + (3 + 4),  1180, // "Ac+3"
    (90 << 4) + (4 + 4),  1020, // "Th+4"
    (91 << 4) + (3 + 4),  1130, // "Pa+3"
    (91 << 4) + (4 + 4),  980,  // "Pa+4"
    (91 << 4) + (5 + 4),  890,  // "Pa+5"
    (92 << 4) + (4 + 4),  970,  // "U+4"
    (92 << 4) + (6 + 4),  800,  // "U+6"
    (93 << 4) + (3 + 4),  1100, // "Np+3"
    (93 << 4) + (4 + 4),  950,  // "Np+4"
    (93 << 4) + (7 + 4),  710,  // "Np+7"
    (94 << 4) + (3 + 4),  1080, // "Pu+3"
    (94 << 4) + (4 + 4),  930,  // "Pu+4"
    (95 << 4) + (3 + 4),  1070, // "Am+3"
    (95 << 4) + (4 + 4),  920,  // "Am+4"
  };
  
  private final static short[] anionLookupTable = {
    (1 << 4) + (-1 + 4),  1540, // "H-1"
    (6 << 4) + (-4 + 4),  2600, // "C-4"
    (7 << 4) + (-3 + 4),  1710, // "N-3"
    (8 << 4) + (-2 + 4),  1320, // "O-2"
    (8 << 4) + (-1 + 4),  1760, // "O-1"
    (9 << 4) + (-1 + 4),  1330, // "F-1"
    (14 << 4) + (-4 + 4), 2710, // "Si-4"
    (14 << 4) + (-1 + 4), 3840, // "Si-1"
    (15 << 4) + (-3 + 4), 2120, // "P-3"
    (16 << 4) + (-2 + 4), 1840, // "S-2"
    (17 << 4) + (-1 + 4), 1810, // "Cl-1"
    (32 << 4) + (-4 + 4), 2720, // "Ge-4"
    (33 << 4) + (-3 + 4), 2220, // "As-3"
    (34 << 4) + (-2 + 4), 1910, // "Se-2"
    (34 << 4) + (-1 + 4), 2320, // "Se-1"
    (35 << 4) + (-1 + 4), 1960, // "Br-1"
    (50 << 4) + (-4 + 4), 2940, // "Sn-4"
    (50 << 4) + (-1 + 4), 3700, // "Sn-1"
    (51 << 4) + (-3 + 4), 2450, // "Sb-3"
    (52 << 4) + (-2 + 4), 2110, // "Te-2"
    (52 << 4) + (-1 + 4), 2500, // "Te-1"
    (53 << 4) + (-1 + 4), 2200, // "I-1"
  };
  
  static BitSet bsCations = new BitSet();
  static BitSet bsAnions = new BitSet();
  static {
    for (int i = 0; i < anionLookupTable.length; i+=2)
      bsAnions.set(anionLookupTable[i]>>4);
    for (int i = 0; i < cationLookupTable.length; i+=2)
      bsCations.set(cationLookupTable[i]>>4);
  }

  public static short getBondingMar(int atomicNumber, int charge) {
    if (charge > 0 && bsCations.get(atomicNumber))
      return getBondingMar(atomicNumber, charge, cationLookupTable);
    if (charge < 0 && bsAnions.get(atomicNumber))
      return getBondingMar(atomicNumber, charge, anionLookupTable);
    return (short) covalentMars[atomicNumber];
  }
  
  public static short getBondingMar(int atomicNumber, int charge, short[] table) {
    // when found, return the corresponding value in ionicMars
    // if atom is not found, just return covalent radius
    // if atom is found, but charge is not found, return next lower charge
    short ionic = (short) ((atomicNumber << 4) + (charge + 4)); 
    int iVal = 0, iMid = 0, iMin = 0, iMax = table.length / 2;
    while (iMin != iMax) {
      iMid = (iMin + iMax) / 2;
      iVal = table[iMid<<1];
      if (iVal > ionic)
        iMax = iMid;
      else if (iVal < ionic)
        iMin = iMid + 1;
      else
        return table[(iMid << 1) + 1];
    }
    // find closest with same element and charge <= this charge
    if (iVal > ionic) 
      iMid--; // overshot
    iVal = table[iMid << 1];
    if (atomicNumber != (iVal >> 4)) 
      iMid++; // must be same element and not a negative charge;
    return table[(iMid << 1) + 1];
  }

  // maximum number of bonds that an atom can have when
  // autoBonding
  // All bonding is done by distances
  // this is only here for truly pathological cases
  public final static int MAXIMUM_AUTO_BOND_COUNT = 20;


  
  private final static String[] paletteNames = {
   /* 0 */ "none", "cpk", "partialCharge", "formalCharge", "temperature",  
   /* 5 */ "fixedTemperature", "surfaceDistance", "structure", "amino", 
   /* 9 */ "shapely", "chain", "group", "monomer", "molecule", "altloc", 
   /*15 */ "insertion", "jmol", "rasmol", 
   /*18 */ "type" // hbonds only
  };
  
  public final static int PALETTE_NONE = 0;
  public final static int PALETTE_CPK = 1;
  public final static int PALETTE_PARTIAL_CHARGE = 2;
  public final static int PALETTE_FORMAL_CHARGE = 3;
  public final static int PALETTE_TEMP = 4;
  
  public final static int PALETTE_FIXEDTEMP = 5;
  public final static int PALETTE_SURFACE = 6;
  public final static int PALETTE_STRUCTURE = 7;
  public final static int PALETTE_AMINO = 8;
  
  public final static int PALETTE_SHAPELY = 9;
  public final static int PALETTE_CHAIN = 10;
  public final static int PALETTE_GROUP = 11;
  public final static int PALETTE_MONOMER = 12;
  public final static int PALETTE_MOLECULE = 13;
  public final static int PALETTE_ALTLOC = 14;
  
  public final static int PALETTE_INSERTION = 15;
  public final static int PALETTE_JMOL = 16;
  public final static int PALETTE_RASMOL = 17;
  public final static int PALETTE_TYPE = 18;
    
  private final static int paletteCount = paletteNames.length;
  
  public final static int getPaletteID(String paletteName) {
    for (int i = 0; i < paletteCount; i++)
      if (paletteNames[i].equalsIgnoreCase(paletteName))
        return i;
    return -1;
  }
  
  public final static String getPaletteName(int pid) {
    return (pid < 0 || pid >= paletteNames.length ? null : paletteNames[pid]);
  }
  
  /**
   * Default table of CPK atom colors.
   * ghemical colors with a few proposed modifications
   */
  public final static int[] argbsCpk = {
    0xFFFF1493, // Xx 0
    0xFFFFFFFF, // H  1
    0xFFD9FFFF, // He 2
    0xFFCC80FF, // Li 3
    0xFFC2FF00, // Be 4
    0xFFFFB5B5, // B  5
    0xFF909090, // C  6 - changed from ghemical
    0xFF3050F8, // N  7 - changed from ghemical
    0xFFFF0D0D, // O  8
    0xFF90E050, // F  9 - changed from ghemical
    0xFFB3E3F5, // Ne 10
    0xFFAB5CF2, // Na 11
    0xFF8AFF00, // Mg 12
    0xFFBFA6A6, // Al 13
    0xFFF0C8A0, // Si 14 - changed from ghemical
    0xFFFF8000, // P  15
    0xFFFFFF30, // S  16
    0xFF1FF01F, // Cl 17
    0xFF80D1E3, // Ar 18
    0xFF8F40D4, // K  19
    0xFF3DFF00, // Ca 20
    0xFFE6E6E6, // Sc 21
    0xFFBFC2C7, // Ti 22
    0xFFA6A6AB, // V  23
    0xFF8A99C7, // Cr 24
    0xFF9C7AC7, // Mn 25
    0xFFE06633, // Fe 26 - changed from ghemical
    0xFFF090A0, // Co 27 - changed from ghemical
    0xFF50D050, // Ni 28 - changed from ghemical
    0xFFC88033, // Cu 29 - changed from ghemical
    0xFF7D80B0, // Zn 30
    0xFFC28F8F, // Ga 31
    0xFF668F8F, // Ge 32
    0xFFBD80E3, // As 33
    0xFFFFA100, // Se 34
    0xFFA62929, // Br 35
    0xFF5CB8D1, // Kr 36
    0xFF702EB0, // Rb 37
    0xFF00FF00, // Sr 38
    0xFF94FFFF, // Y  39
    0xFF94E0E0, // Zr 40
    0xFF73C2C9, // Nb 41
    0xFF54B5B5, // Mo 42
    0xFF3B9E9E, // Tc 43
    0xFF248F8F, // Ru 44
    0xFF0A7D8C, // Rh 45
    0xFF006985, // Pd 46
    0xFFC0C0C0, // Ag 47 - changed from ghemical
    0xFFFFD98F, // Cd 48
    0xFFA67573, // In 49
    0xFF668080, // Sn 50
    0xFF9E63B5, // Sb 51
    0xFFD47A00, // Te 52
    0xFF940094, // I  53
    0xFF429EB0, // Xe 54
    0xFF57178F, // Cs 55
    0xFF00C900, // Ba 56
    0xFF70D4FF, // La 57
    0xFFFFFFC7, // Ce 58
    0xFFD9FFC7, // Pr 59
    0xFFC7FFC7, // Nd 60
    0xFFA3FFC7, // Pm 61
    0xFF8FFFC7, // Sm 62
    0xFF61FFC7, // Eu 63
    0xFF45FFC7, // Gd 64
    0xFF30FFC7, // Tb 65
    0xFF1FFFC7, // Dy 66
    0xFF00FF9C, // Ho 67
    0xFF00E675, // Er 68
    0xFF00D452, // Tm 69
    0xFF00BF38, // Yb 70
    0xFF00AB24, // Lu 71
    0xFF4DC2FF, // Hf 72
    0xFF4DA6FF, // Ta 73
    0xFF2194D6, // W  74
    0xFF267DAB, // Re 75
    0xFF266696, // Os 76
    0xFF175487, // Ir 77
    0xFFD0D0E0, // Pt 78 - changed from ghemical
    0xFFFFD123, // Au 79 - changed from ghemical
    0xFFB8B8D0, // Hg 80 - changed from ghemical
    0xFFA6544D, // Tl 81
    0xFF575961, // Pb 82
    0xFF9E4FB5, // Bi 83
    0xFFAB5C00, // Po 84
    0xFF754F45, // At 85
    0xFF428296, // Rn 86
    0xFF420066, // Fr 87
    0xFF007D00, // Ra 88
    0xFF70ABFA, // Ac 89
    0xFF00BAFF, // Th 90
    0xFF00A1FF, // Pa 91
    0xFF008FFF, // U  92
    0xFF0080FF, // Np 93
    0xFF006BFF, // Pu 94
    0xFF545CF2, // Am 95
    0xFF785CE3, // Cm 96
    0xFF8A4FE3, // Bk 97
    0xFFA136D4, // Cf 98
    0xFFB31FD4, // Es 99
    0xFFB31FBA, // Fm 100
    0xFFB30DA6, // Md 101
    0xFFBD0D87, // No 102
    0xFFC70066, // Lr 103
    0xFFCC0059, // Rf 104
    0xFFD1004F, // Db 105
    0xFFD90045, // Sg 106
    0xFFE00038, // Bh 107
    0xFFE6002E, // Hs 108
    0xFFEB0026, // Mt 109
};

  public final static int[] argbsCpkRasmol = {
    0x00FF1493 + ( 0 << 24), // Xx 0
    0x00FFFFFF + ( 1 << 24), // H  1
    0x00FFC0CB + ( 2 << 24), // He 2
    0x00B22222 + ( 3 << 24), // Li 3
    0x0000FF00 + ( 5 << 24), // B  5
    0x00C8C8C8 + ( 6 << 24), // C  6
    0x008F8FFF + ( 7 << 24), // N  7
    0x00F00000 + ( 8 << 24), // O  8
    0x00DAA520 + ( 9 << 24), // F  9
    0x000000FF + (11 << 24), // Na 11
    0x00228B22 + (12 << 24), // Mg 12
    0x00808090 + (13 << 24), // Al 13
    0x00DAA520 + (14 << 24), // Si 14
    0x00FFA500 + (15 << 24), // P  15
    0x00FFC832 + (16 << 24), // S  16
    0x0000FF00 + (17 << 24), // Cl 17
    0x00808090 + (20 << 24), // Ca 20
    0x00808090 + (22 << 24), // Ti 22
    0x00808090 + (24 << 24), // Cr 24
    0x00808090 + (25 << 24), // Mn 25
    0x00FFA500 + (26 << 24), // Fe 26
    0x00A52A2A + (28 << 24), // Ni 28
    0x00A52A2A + (29 << 24), // Cu 29
    0x00A52A2A + (30 << 24), // Zn 30
    0x00A52A2A + (35 << 24), // Br 35
    0x00808090 + (47 << 24), // Ag 47
    0x00A020F0 + (53 << 24), // I  53
    0x00FFA500 + (56 << 24), // Ba 56
    0x00DAA520 + (79 << 24), // Au 79
  };

  static {
    // if the length of these tables is all the same then the
    // java compiler should eliminate all of this code.
    if ((elementSymbols.length != elementNames.length) ||
        (elementSymbols.length != vanderwaalsMars.length) ||
        (elementSymbols.length != covalentMars.length) ||
        (elementSymbols.length != argbsCpk.length)) {
      Logger.error("ERROR!!! Element table length mismatch:" +
                         "\n elementSymbols.length=" + elementSymbols.length +
                         "\n elementNames.length=" + elementNames.length +
                         "\n vanderwaalsMars.length=" + vanderwaalsMars.length+
                         "\n covalentMars.length=" +
                         covalentMars.length +
                         "\n argbsCpk.length=" + argbsCpk.length);
    }
  }

  /**
   * Default table of PdbStructure colors
   */
  public final static byte PROTEIN_STRUCTURE_NONE = 0;
  public final static byte PROTEIN_STRUCTURE_TURN = 1;
  public final static byte PROTEIN_STRUCTURE_SHEET = 2;
  public final static byte PROTEIN_STRUCTURE_HELIX = 3;
  public final static byte PROTEIN_STRUCTURE_DNA = 4;
  public final static byte PROTEIN_STRUCTURE_RNA = 5;

  /****************************************************************
   * In DRuMS, RasMol, and Chime, quoting from
   * http://www.umass.edu/microbio/rasmol/rascolor.htm
   *
   *The RasMol structure color scheme colors the molecule by
   *protein secondary structure.
   *
   *Structure                   Decimal RGB    Hex RGB
   *Alpha helices  red-magenta  [255,0,128]    FF 00 80  *
   *Beta strands   yellow       [255,200,0]    FF C8 00  *
   *
   *Turns          pale blue    [96,128,255]   60 80 FF
   *Other          white        [255,255,255]  FF FF FF
   *
   **Values given in the 1994 RasMol 2.5 Quick Reference Card ([240,0,128]
   *and [255,255,0]) are not correct for RasMol 2.6-beta-2a.
   *This correction was made above on Dec 5, 1998.
   ****************************************************************/
  public final static int[] argbsStructure = {
    0xFFFFFFFF, // PROTEIN_STRUCTURE_NONE
    0xFF6080FF, // PROTEIN_STRUCTURE_TURN
    0xFFFFC800, // PROTEIN_STRUCTURE_SHEET
    0xFFFF0080, // PROTEIN_STRUCTURE_HELIX
    0xFFAE00FE, // PROTEIN_STRUCTURE_DNA
    0xFFFD0162, // PROTEIN_STRUCTURE_RNA
  };

  public final static int[] argbsAmino = {
    0xFFBEA06E, // default tan
    // note that these are the rasmol colors and names, not xwindows
    0xFFC8C8C8, // darkGrey   ALA
    0xFF145AFF, // blue       ARG
    0xFF00DCDC, // cyan       ASN
    0xFFE60A0A, // brightRed  ASP
    0xFFE6E600, // yellow     CYS
    0xFF00DCDC, // cyan       GLN
    0xFFE60A0A, // brightRed  GLU
    0xFFEBEBEB, // lightGrey  GLY
    0xFF8282D2, // paleBlue   HIS
    0xFF0F820F, // green      ILE
    0xFF0F820F, // green      LEU
    0xFF145AFF, // blue       LYS
    0xFFE6E600, // yellow     MET
    0xFF3232AA, // midBlue    PHE
    0xFFDC9682, // mauve      PRO
    0xFFFA9600, // orange     SER
    0xFFFA9600, // orange     THR
    0xFFB45AB4, // purple     TRP
    0xFF3232AA, // midBlue    TYR
    0xFF0F820F, // green      VAL

    0xFFFF69B4, // pick a new color ASP/ASN ambiguous
    0xFFFF69B4, // pick a new color GLU/GLN ambiguous
  };

  // hmmm ... what is shapely backbone? seems interesting
  public final static int argbShapelyBackbone = 0xFFB8B8B8;
  public final static int argbShapelySpecial =  0xFF5E005E;
  public final static int argbShapelyDefault =  0xFFFF00FF;
  public final static int[] argbsShapely = {
    0xFFFF00FF, // default
    // these are rasmol values, not xwindows colors
    0xFF8CFF8C, // ALA
    0xFF00007C, // ARG
    0xFFFF7C70, // ASN
    0xFFA00042, // ASP
    0xFFFFFF70, // CYS
    0xFFFF4C4C, // GLN
    0xFF660000, // GLU
    0xFFFFFFFF, // GLY
    0xFF7070FF, // HIS
    0xFF004C00, // ILE
    0xFF455E45, // LEU
    0xFF4747B8, // LYS
    0xFFB8A042, // MET
    0xFF534C52, // PHE
    0xFF525252, // PRO
    0xFFFF7042, // SER
    0xFFB84C00, // THR
    0xFF4F4600, // TRP
    0xFF8C704C, // TYR
    0xFFFF8CFF, // VAL

    0xFFFF00FF, // ASX ASP/ASN ambiguous
    0xFFFF00FF, // GLX GLU/GLN ambiguous
    0xFFFF00FF, // UNK unknown -- 23

    0xFFA0A0FF, // A
    0xFFA0A0FF, // +A

    0xFFFF7070, // G
    0xFFFF7070, // +G

    0xFF80FFFF, // I miguel made up this color
    0xFF80FFFF, // +I
    
    0xFFFF8C4B, // C
    0xFFFF8C4B, // +C

    0xFFA0FFA0, // T
    0xFFA0FFA0, // +T

    0xFFFF8080, // U miguel made up this color
    0xFFFF8080, // +U
  };

  /**
   * colors used for chains
   *
   */

  /****************************************************************
   * some pastel colors
   * 
   * C0D0FF - pastel blue
   * B0FFB0 - pastel green
   * B0FFFF - pastel cyan
   * FFC0C8 - pink
   * FFC0FF - pastel magenta
   * FFFF80 - pastel yellow
   * FFDEAD - navajowhite
   * FFD070 - pastel gold

   * FF9898 - light coral
   * B4E444 - light yellow-green
   * C0C000 - light olive
   * FF8060 - light tomato
   * 00FF7F - springgreen
   * 
cpk on; select atomno>100; label %i; color chain; select selected & hetero; cpk off
   ****************************************************************/

  public final static int[] argbsChainAtom = {
    // ' '->0 'A'->1, 'B'->2
    0xFFffffff, // ' ' & '0' white
    //
    0xFFC0D0FF, // skyblue
    0xFFB0FFB0, // pastel green
    0xFFFFC0C8, // pink
    0xFFFFFF80, // pastel yellow
    0xFFFFC0FF, // pastel magenta
    0xFFB0F0F0, // pastel cyan
    0xFFFFD070, // pastel gold
    0xFFF08080, // lightcoral

    0xFFF5DEB3, // wheat
    0xFF00BFFF, // deepskyblue
    0xFFCD5C5C, // indianred
    0xFF66CDAA, // mediumaquamarine
    0xFF9ACD32, // yellowgreen
    0xFFEE82EE, // violet
    0xFF00CED1, // darkturquoise
    0xFF00FF7F, // springgreen
    0xFF3CB371, // mediumseagreen

    0xFF00008B, // darkblue
    0xFFBDB76B, // darkkhaki
    0xFF006400, // darkgreen
    0xFF800000, // maroon
    0xFF808000, // olive
    0xFF800080, // purple
    0xFF008080, // teal
    0xFFB8860B, // darkgoldenrod
    0xFFB22222, // firebrick
  };

  public final static int[] argbsChainHetero = {
    // ' '->0 'A'->1, 'B'->2
    0xFFffffff, // ' ' & '0' white
    //
    0xFFC0D0FF - 0x00303030, // skyblue
    0xFFB0FFB0 - 0x00303018, // pastel green
    0xFFFFC0C8 - 0x00303018, // pink
    0xFFFFFF80 - 0x00303010, // pastel yellow
    0xFFFFC0FF - 0x00303030, // pastel magenta
    0xFFB0F0F0 - 0x00303030, // pastel cyan
    0xFFFFD070 - 0x00303010, // pastel gold
    0xFFF08080 - 0x00303010, // lightcoral

    0xFFF5DEB3 - 0x00303030, // wheat
    0xFF00BFFF - 0x00001830, // deepskyblue
    0xFFCD5C5C - 0x00181010, // indianred
    0xFF66CDAA - 0x00101818, // mediumaquamarine
    0xFF9ACD32 - 0x00101808, // yellowgreen
    0xFFEE82EE - 0x00301030, // violet
    0xFF00CED1 - 0x00001830, // darkturquoise
    0xFF00FF7F - 0x00003010, // springgreen
    0xFF3CB371 - 0x00081810, // mediumseagreen

    0xFF00008B + 0x00000030, // darkblue
    0xFFBDB76B - 0x00181810, // darkkhaki
    0xFF006400 + 0x00003000, // darkgreen
    0xFF800000 + 0x00300000, // maroon
    0xFF808000 + 0x00303000, // olive
    0xFF800080 + 0x00300030, // purple
    0xFF008080 + 0x00003030, // teal
    0xFFB8860B + 0x00303008, // darkgoldenrod
    0xFFB22222 + 0x00101010, // firebrick
  };

  public final static short FORMAL_CHARGE_COLIX_RED =
    (short)elementSymbols.length;
  public final static short FORMAL_CHARGE_COLIX_WHITE =
    (short)(FORMAL_CHARGE_COLIX_RED + 4);
  public final static short FORMAL_CHARGE_COLIX_BLUE =
    (short)(FORMAL_CHARGE_COLIX_WHITE + 7);
  public final static int FORMAL_CHARGE_RANGE_SIZE = 12;

  public final static int[] argbsFormalCharge = {
    0xFFFF0000, // -4
    0xFFFF4040, // -3
    0xFFFF8080, // -2
    0xFFFFC0C0, // -1
    0xFFFFFFFF, // 0
    0xFFD8D8FF, // 1
    0xFFB4B4FF, // 2
    0xFF9090FF, // 3
    0xFF6C6CFF, // 4
    0xFF4848FF, // 5
    0xFF2424FF, // 6
    0xFF0000FF, // 7
  };

  public final static int FORMAL_CHARGE_INDEX_WHITE = 4;
  public final static int FORMAL_CHARGE_INDEX_MAX = argbsFormalCharge.length;

  public final static short PARTIAL_CHARGE_COLIX_RED =
    (short)(FORMAL_CHARGE_COLIX_BLUE + 1);
  public final static short PARTIAL_CHARGE_COLIX_WHITE =
    (short)(PARTIAL_CHARGE_COLIX_RED + 15);
  public final static short PARTIAL_CHARGE_COLIX_BLUE =
    (short)(PARTIAL_CHARGE_COLIX_WHITE + 15);
  public final static int PARTIAL_CHARGE_RANGE_SIZE = 31;

  public final static int[] argbsRwbScale = {
    0xFFFF0000, // red
    0xFFFF1010, //
    0xFFFF2020, //
    0xFFFF3030, //
    0xFFFF4040, //
    0xFFFF5050, //
    0xFFFF6060, //
    0xFFFF7070, //
    0xFFFF8080, //
    0xFFFF9090, //
    0xFFFFA0A0, //
    0xFFFFB0B0, //
    0xFFFFC0C0, //
    0xFFFFD0D0, //
    0xFFFFE0E0, //
    0xFFFFFFFF, // white
    0xFFE0E0FF, //
    0xFFD0D0FF, //
    0xFFC0C0FF, //
    0xFFB0B0FF, //
    0xFFA0A0FF, //
    0xFF9090FF, //
    0xFF8080FF, //
    0xFF7070FF, //
    0xFF6060FF, //
    0xFF5050FF, //
    0xFF4040FF, //
    0xFF3030FF, //
    0xFF2020FF, //
    0xFF1010FF, //
    0xFF0000FF, // blue
  };

  public final static int[] argbsRoygbScale = {
    0xFFFF0000,
    0xFFFF2000,
    0xFFFF4000,
    0xFFFF6000,
    0xFFFF8000,
    0xFFFFA000,
    0xFFFFC000,
    0xFFFFE000,

    0xFFFFF000, // yellow gets compressed, so give it an extra boost

    0xFFFFFF00,
    0xFFF0F000, // yellow gets compressed, so give it a little boost
    0xFFE0FF00,
    0xFFC0FF00,
    0xFFA0FF00,
    0xFF80FF00,
    0xFF60FF00,
    0xFF40FF00,
    0xFF20FF00,

    0xFF00FF00,
    0xFF00FF20,
    0xFF00FF40,
    0xFF00FF60,
    0xFF00FF80,
    0xFF00FFA0,
    0xFF00FFC0,
    0xFF00FFE0,

    0xFF00FFFF,
    0xFF00E0FF,
    0xFF00C0FF,
    0xFF00A0FF,
    0xFF0080FF,
    0xFF0060FF,
    0xFF0040FF,
    0xFF0020FF,

    0xFF0000FF,
  };

  /*
  public final static int[] argbsBlueRedRainbow = {
    0xFF0000FF,
    //0xFF0010FF,
    0xFF0020FF,
    //0xFF0030FF,
    0xFF0040FF,
    //0xFF0050FF,
    0xFF0060FF,
    //0xFF0070FF,
    0xFF0080FF,
    //0xFF0090FF,
    0xFF00A0FF,
    //0xFF00B0FF,
    0xFF00C0FF,
    //0xFF00D0FF,
    0xFF00E0FF,
    //0xFF00F0FF,

    0xFF00FFFF,
    //0xFF00FFF0,
    0xFF00FFE0,
    //0xFF00FFD0,
    0xFF00FFC0,
    //0xFF00FFB0,
    0xFF00FFA0,
    //0xFF00FF90,
    0xFF00FF80,
    //0xFF00FF70,
    0xFF00FF60,
    //0xFF00FF50,
    0xFF00FF40,
    //0xFF00FF30,
    0xFF00FF20,
    //0xFF00FF10,

    0xFF00FF00,
    //0xFF10FF00,
    0xFF20FF00,
    //0xFF30FF00,
    0xFF40FF00,
    //0xFF50FF00,
    0xFF60FF00,
    //0xFF70FF00,
    0xFF80FF00,
    //0xFF90FF00,
    0xFFA0FF00,
    //0xFFB0FF00,
    0xFFC0FF00,
    //0xFFD0FF00,
    0xFFE0FF00,
    //0xFFF0FF00,

    0xFFFFFF00,
    //0xFFFFF000,
    0xFFFFE000,
    //0xFFFFD000,
    0xFFFFC000,
    //0xFFFFB000,
    0xFFFFA000,
    //0xFFFF9000,
    0xFFFF8000,
    //0xFFFF7000,
    0xFFFF6000,
    //0xFFFF5000,
    0xFFFF4000,
    //0xFFFF3000,
    0xFFFF2000,
    //0xFFFF1000,

    0xFFFF0000,
  };
  */

  // positive and negative default colors used for
  // isosurface rendering of .cube files

  public final static int[] argbsIsosurfacePositive = {
    0xFF5020A0,
    0xFF7040C0,
    0xFF9060E0,
    0xFFB080FF,
  };
  
  public final static int[] argbsIsosurfaceNegative = {
    0xFFA02050,
    0xFFC04070,
    0xFFE06090,
    0xFFFF80B0,
  };

    public final static String[] specialAtomNames = {
    
      ////////////////////////////////////////////////////////////////
      // The ordering of these entries can be changed ... BUT ...
      // the offsets must be kept consistent with the ATOMID definitions
      // below.
      //
      // null is entry 0
      // The first 32 entries are reserved for null + 31 'distinguishing atoms'
      // see definitions below. 32 is magical because bits are used in an
      // int to distinguish groups. If we need more then we can go to 64
      // bits by using a long ... but code must change.
      //
      // All entries less than 64 are backbone entries
      // But the number 64 is not magical and could be easily changed
      ////////////////////////////////////////////////////////////////
      null, // 0
      
      // protein backbone
      //
      "N",   //  1 - amino nitrogen
      "CA",  //  2 - alpha carbon
      "C",   //  3 - carbonyl carbon
      null, // used to be carbonyl oxygen, now can be O or O1
  
    // nucleic acid backbone sugar
    //
    "O5'", //  5 - sugar 5' oxygen
    "C5'", //  6 - sugar 5' carbon
    "C4'", //  7 - sugar ring 4' carbon
    "C3'", //  8 - sugar ring 3' carbon
    "O3'", //  9 - sugar 3' oxygen
    "C2'", // 10 - sugar ring 2' carbon
    "C1'", // 11 - sugar ring 1' carbon
    // Phosphorus is not required for a nucleic group because
    // at the terminus it could have H5T or O5T ...
    "P",   // 12 - phosphate phosphorus

    // ... But we need to distinguish phosphorus separately because
    // it could be found in phosphorus-only nucleic polymers

    // reserved for future expansion ... lipids & carbohydrates
    // 9/2006 -- carbohydrates are just handled as group3 codes
    // see below
    null, null, null,       // 13 - 15
    null, null, null, null, // 16 - 19
    null, null, null, null, // 20 - 23
    null, null, null, null, // 24 - 27
    null, null, null, null, // 28 - 31

    
    // anything that could be considered part of the 'backbone'
    // goes in this next group
    
    // protein backbone
    //
    "OXT", // 32 - second carbonyl oxygen, C-terminus only

    // protein backbone hydrogens
    //
    "H",   // 33 - amino hydrogen
    // these appear on the N-terminus end of 1ALE & 1LCD
    "1H",  // 34 - N-terminus hydrogen
    "2H",  // 35 - second N-terminus Hydrogen
    "3H",  // 36 - third N-terminus Hydrogen
    "HA",  // 37 - H on alpha carbon
    "1HA", // 38 - H on alpha carbon in Gly only
    "2HA", // 39 - 1ALE calls the two GLY hdrogens 1HA & 2HA

    "O",   // 40 - carbonyl oxygen
    "O1",  // 41 - carbonyl oxygen in some protein residues (4THN)
    null,  // 42
    null,  // 43
    null,  // 44

    // Terminal nuclic acid
    "H5T", // 45 - 5' terminus hydrogen which replaces P + O1P + O2P
    "O5T", // 46 - 5' terminus oxygen which replaces P + O1P + O2P
    "O1P", // 47 - first equivalent oxygen on phosphorus of phosphate
    "O2P", // 48 - second equivalent oxygen on phosphorus of phosphate
    "O4'", // 49 - sugar ring 4' oxygen ... not present in +T ... maybe others
    "O2'", // 50 - sugar 2' oxygen, unique to RNA

    // nucleic acid backbone hydrogens
    //
    "1H5'", // 51 - first  equivalent H on sugar 5' carbon
    "2H5'", // 52 - second  equivalent H on sugar 5' carbon 
    "H4'",  // 53 - H on sugar ring 4' carbon
    "H3'",  // 54 - H on sugar ring 3' carbon
    "1H2'", // 55 - first equivalent H on sugar ring 2' carbon
    "2H2'", // 56 - second equivalent H on sugar ring 2' carbon
    "2HO'", // 57 - H on sugar 2' oxygen, unique to RNA 
    "H1'",  // 58 - H on sugar ring 1' carbon 
    //
    "H3T",  // 59 - 3' terminus hydrogen
    //
    null,   // 60
    null,   // 61
    null,   // 62
    null,   // 63
   
    // everything before this (1 - 63, but not 0) is backbone

    // nucleic acid bases
    //
    "N1",   // 64
    "C2",   // 65
    "N3",   // 66
    "C4",   // 67
    "C5",   // 68
    "C6",   // 69 -- currently defined as the nucleotide wing
            // this determines the vector for the sheet
            // could be changed if necessary

    // pyrimidine O2
    //
    "O2",   // 70

    // purine stuff
    //
    "N7",   // 71
    "C8",   // 72
    "N9",   // 73

    // nucleic acid base ring functional groups
    //
    "N4",  // 74 - base ring N4, unique to C
    "N2",  // 75 - base amino N2, unique to G
    "N6",  // 76 - base amino N6, unique to A
    "C5M", // 77 - base methyl carbon, unique to T

    "O6",  // 78 - base carbonyl O6, only in G and I
    "O4",  // 79 - base carbonyl O4, only in T and U
    "S4",  // 80 - base thiol sulfur, unique to thio-U

  };

  public final static int ATOMID_MAX = specialAtomNames.length;

  // this form is used for counting groups in Frame
  private final static String allCarbohydrates = 
    ",[AFL],[AGC],[AHR],[ARA],[ARB],[BDF],[BDR],[BGC],[BMA]" +
    ",[FCA],[FCB],[FRU],[FUC],[FUL],[GAL],[GLA],[GLB],[GLC]" +
    ",[GUP],[LXC],[MAN],[RAA],[RAM],[RIB],[RIP],[XYP],[XYS]" +
    ",[CBI],[CT3],[CTR],[CTT],[LAT],[MAB],[MAL],[MLR],[MTT]" +
    ",[SUC],[TRE],[ASF],[GCU],[MTL],[NAG],[NAM],[RHA],[SOR]" +
    ",[XYL]";// from Eric Martz

  /**
   * @param group3 a potential group3 name
   * @return whether this is a carbohydrate from the list
   */
  public final static boolean checkCarbohydrate(String group3) {
    if (group3 == null)
      return false;
    String str = "[" + group3.toUpperCase() + "]";
    return (allCarbohydrates.indexOf(str) >= 0);
  }

  ////////////////////////////////////////////////////////////////
  // currently, ATOMIDs must be >= 0 && <= 127
  // if we need more then we can go to 255 by:
  //  1. applying 0xFF mask ... as in atom.specialAtomID & 0xFF;
  //  2. change the interesting atoms table to be shorts
  //     so that we can store negative numbers
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // keep this table in order to make it easier to maintain
  ////////////////////////////////////////////////////////////////

  // atomID 0 => nothing special, just an ordinary atom
  public final static byte ATOMID_AMINO_NITROGEN  = 1;
  public final static byte ATOMID_ALPHA_CARBON    = 2;
  public final static byte ATOMID_CARBONYL_CARBON = 3;
  public final static byte ATOMID_O5_PRIME        = 5;
  public final static byte ATOMID_C5_PRIME        = 6;
  public final static byte ATOMID_C3_PRIME        = 8;
  public final static byte ATOMID_O3_PRIME        = 9;
  public final static byte ATOMID_NUCLEIC_PHOSPHORUS = 12;
  public final static byte ATOMID_TERMINATING_OXT = 32;
  public final static byte ATOMID_CARBONYL_OXYGEN = 40;
  public final static byte ATOMID_O1              = 41;
  public final static byte ATOMID_H5T_TERMINUS    = 45;
  public final static byte ATOMID_O5T_TERMINUS    = 46;
  public final static byte ATOMID_RNA_O2PRIME     = 50;
  public final static byte ATOMID_H3T_TERMINUS    = 59;
  public final static byte ATOMID_N1 = 64;
  public final static byte ATOMID_C2 = 65;
  public final static byte ATOMID_N3 = 66;
  public final static byte ATOMID_C4 = 67;
  public final static byte ATOMID_C5 = 68;
  public final static byte ATOMID_C6 = 69;
  public final static byte ATOMID_O2 = 70;
  public final static byte ATOMID_N7 = 71;
  public final static byte ATOMID_C8 = 72;
  public final static byte ATOMID_N9 = 73;
  public final static byte ATOMID_N4 = 74;
  public final static byte ATOMID_N2 = 75;
  public final static byte ATOMID_N6 = 76;
  public final static byte ATOMID_C5M= 77;
  public final static byte ATOMID_O6 = 78;
  public final static byte ATOMID_O4 = 79;
  public final static byte ATOMID_S4 = 80;

  // this is currently defined as C6
  public final static byte ATOMID_NUCLEIC_WING = 69;

  // this is entries 1 through 3 ... 3 bits ... N, CA, C
  public final static int ATOMID_PROTEIN_MASK = 0x07 << 1;
  // this is for groups that only contain an alpha carbon
  public final static int ATOMID_ALPHA_ONLY_MASK = 1 << ATOMID_ALPHA_CARBON;
  // this is entries 5 through through 11 ... 7 bits
  public final static int ATOMID_NUCLEIC_MASK = 0x7F << 5;
  // this is for nucleic groups that only contain a phosphorus
  public final static int ATOMID_PHOSPHORUS_ONLY_MASK =
    1 << ATOMID_NUCLEIC_PHOSPHORUS;

  // this is the MAX of the backbone ... everything < MAX is backbone
  public final static int ATOMID_DISTINGUISHING_ATOM_MAX = 32;
  public final static int ATOMID_BACKBONE_MAX = 64;

  ////////////////////////////////////////////////////////////////
  // GROUP_ID related stuff for special groupIDs
  ////////////////////////////////////////////////////////////////
  
  /****************************************************************
   * PDB file format spec says that the 'residue name' must be
   * right-justified. However, Eric Martz says that some files
   * are not. Therefore, we will be 'flexible' in reading the
   * group name ... we will trim() when read in the field.
   * So a 'group3' can now be less than 3 characters long.
   ****************************************************************/

  public final static int GROUPID_PROLINE = 15;
  public final static int GROUPID_PURINE_MIN = 24;
  public final static int GROUPID_PURINE_LAST = 29;
  public final static int GROUPID_PYRIMIDINE_MIN = 30;
  public final static int GROUPID_PYRIMIDINE_LAST = 35;
  public final static int GROUPID_GUANINE = 26;
  public final static int GROUPID_PLUS_GUANINE = 27;
  public final static int GROUPID_GUANINE_1_MIN = 40;
  public final static int GROUPID_GUANINE_1_LAST = 46;
  public final static int GROUPID_GUANINE_2_MIN = 55;
  public final static int GROUPID_GUANINE_2_LAST = 57;
  

  public final static short GROUPID_AMINO_MAX = 23;

  public final static short GROUPID_SHAPELY_MAX = 36;

  public final static String[] predefinedGroup3Names = {
    // taken from PDB spec
    "", //  0 this is the null group
    
    "ALA", // 1
    "ARG",
    "ASN",
    "ASP",
    "CYS",
    "GLN",
    "GLU",
    "GLY",
    "HIS",
    "ILE",
    "LEU",
    "LYS",
    "MET",
    "PHE",
    "PRO", // 15 Proline
    "SER",
    "THR",
    "TRP",
    "TYR",
    "VAL",
    "ASX", // 21 ASP/ASN ambiguous
    "GLX", // 22 GLU/GLN ambiguous
    "UNK", // 23 unknown -- 23

    // if you change these numbers you *must* update
    // the predefined sets in script.Token.java

    "A", // 24 the purines
    "+A",
    "G", // 26
    "+G",
    "I", // 28
    "+I",
    "C", // 30 the pyrimidines
    "+C",
    "T", // 32
    "+T",
    "U", // 34
    "+U",

    "1MA", // 36
    "AMO",
    "5MC",
    "OMC",
    "1MG", // 40
    "2MG",
    "M2G",
    "7MG",
    "G7M",
    "OMG", // 45
    "YG",
    "QUO",
    "H2U",
    "5MU",
    "4SU", // 50
    "PSU",
    
    "AMP",
    "ADP",
    "ATP",
    
    "GMP", // 55
    "GDP",
    "GTP",
    
    "IMP",
    "IDP",
    "ITP", // 60
    
    "CMP",
    "CDP",
    "CTP",
    
    "TMP",
    "TDP", // 65
    "TTP",
    
    "UMP",
    "UDP",
    "UTP", // 69

    // water && solvent
    "HOH", // 70
    "DOD", // 71
    "WAT", // 72
    // ions && solvent
    "PO4", // 73 phosphate ions
    "SO4", // 74 sulphate ions

  };

  static String getGroup3List() {
    StringBuffer s = new StringBuffer();
    //for menu presentation order
    for (int i = 1; i < GROUPID_PURINE_MIN; i++)
      s.append(",[" + (predefinedGroup3Names[i]+"   ").substring(0,3)+"]");
    s.append(",[G  ],[C  ],[A  ],[T  ],[I  ],[U  ],[+G ],[+C ],[+A ],[+T ],[+I ],[+U ]");
    s.append(allCarbohydrates);
    return "" + s;
  }
  
  public final static boolean isHetero(String group3) {
    int pt = group3List.indexOf("[" + (group3 + "   ").substring(0, 3) + "]");
    return (pt < 0 || pt / 6 > GROUPID_PYRIMIDINE_LAST);
  }

  public final static String group3List = getGroup3List();
  public final static int group3Count = group3List.length() / 6;
  
  public final static String[] predefinedGroup1Names = {
    /* rmh
     * 
     * G   Glycine   Gly                   P   Proline   Pro
     * A   Alanine   Ala                   V   Valine    Val
     * L   Leucine   Leu                   I   Isoleucine    Ile
     * M   Methionine    Met               C   Cysteine    Cys
     * F   Phenylalanine   Phe             Y   Tyrosine    Tyr
     * W   Tryptophan    Trp               H   Histidine   His
     * K   Lysine    Lys                   R   Arginine    Arg
     * Q   Glutamine   Gln                 N   Asparagine    Asn
     * E   Glutamic Acid   Glu             D   Aspartic Acid   Asp
     * S   Serine    Ser                   T   Threonine   Thr
     */
    "", //  0 this is the null group
    
    "A", // 1
    "R",
    "N",
    "D",
    "C",
    "Q",
    "E",
    "G",
    "H",
    "I",
    "L",
    "K",
    "M",
    "F",
    "P", // 15 Proline
    "S",
    "T",
    "W",
    "Y",
    "V",
    "A", // 21 ASP/ASN ambiguous
    "G", // 22 GLU/GLN ambiguous
    "?", // 23 unknown -- 23
    "A", // 24 the purines
    "A",
    "G", // 26
    "G",
    "I", // 28
    "I",
    "C", // 30 the pyrimidines
    "C",
    "T", // 32
    "T",
    "U", // 34
    "U",
    };

  ////////////////////////////////////////////////////////////////
  // predefined sets
  ////////////////////////////////////////////////////////////////

  public static String[] predefinedSets = {
    //
    // main isotope
    //
    "@_1H _H & !(_2H,_3H)",
    "@_12C _C & !(_13C,_14C)",
    "@_14N _N & !(_15N)",
    //
    // protein related
    //
    // protein is hardwired
    "@amino _g>0 & _g<=23",
    "@acidic asp,glu",
    "@basic arg,his,lys",
    "@charged acidic,basic",
    "@negative acidic",
    "@positive basic",
    "@neutral amino&!(acidic,basic)",
    "@polar amino&!hydrophobic",

    "@cyclic his,phe,pro,trp,tyr",
    "@acyclic amino&!cyclic",
    "@aliphatic ala,gly,ile,leu,val",
    "@aromatic his,phe,trp,tyr",
    //    "@cystine",

    "@buried ala,cys,ile,leu,met,phe,trp,val",
    "@surface amino&!buried",

    // doc on hydrophobic is inconsistent
    // text description of hydrophobic says this
    //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
    // table says this
    "@hydrophobic ala,gly,ile,leu,met,phe,pro,trp,tyr,val",
    "@ligand hetero & !solvent",
    "@mainchain backbone",
    "@small ala,gly,ser",
    "@medium asn,asp,cys,pro,thr,val",
    "@large arg,glu,gln,his,ile,leu,lys,met,phe,trp,tyr",

    //
    // nucleic acid related

    // nucleic, dna, rna, purine, pyrimidine are hard-wired
    //
    "@c nucleic & within(group,_a=74)", // ATOMID_N4
    "@g nucleic & within(group,_a=75)", // ATOMID_N2
    "@cg c,g",
    "@a nucleic & within(group,_a=76)", // ATOMID_N6
    "@t nucleic & within(group,_a=77)", // ATOMID_C5M
    "@at a,t",
    "@i nucleic & within(group,_a=78) & !g", // ATOMID_O6
    "@u nucleic & within(group,_a=79) & !t", // ATOMID_O4
    "@tu nucleic & within(group,_a=80)", // ATOMID_S4

    //
    // solvent
    //
    "@solvent _g>=70 & _g<=74", // water or ions
    "@hoh water",
    "@water _g>=70 & _g<=72, oxygen & connected(2, hydrogen or deuterium or tritium), (hydrogen or deuterium and tritium) & connected(oxygen & connected(2, hydrogen or deuterium or tritium))",
    "@ions _g=73,_g=74",

    //
    // structure related
    //
    "@alpha _a=2", // rasmol doc says "approximately *.CA" - whatever?
    "@backbone (protein,nucleic) & _a>0 & _a<=63",
    "@sidechain (protein,nucleic) & !backbone",
    "@base nucleic & !backbone",

    "@turn _structure=1",
    "@sheet _structure=2",
    "@helix _structure=3",

    "@bonded _bondedcount>0",
    //    "@hetero", handled specially

  };

  ////////////////////////////////////////////////////////////////
  // font-related
  ////////////////////////////////////////////////////////////////

  public final static String DEFAULT_FONTFACE = "SansSerif";
  public final static String DEFAULT_FONTSTYLE = "Plain";

  public final static int LABEL_MINIMUM_FONTSIZE = 6;
  public final static int LABEL_MAXIMUM_FONTSIZE = 63;
  public final static int LABEL_DEFAULT_FONTSIZE = 13;
  public final static int LABEL_DEFAULT_X_OFFSET = 4;
  public final static int LABEL_DEFAULT_Y_OFFSET = 4;

  public final static int MEASURE_DEFAULT_FONTSIZE = 15;
  public final static int AXES_DEFAULT_FONTSIZE = 14;

  ////////////////////////////////////////////////////////////////
  // do not rearrange/modify these shapes without
  // updating the String[] shapeBaseClasses below &&
  // also creating a token for this shape in Token.java &&
  // also updating Eval.java shapeToks to confirm consistent
  // conversion from tokens to shapes
  ////////////////////////////////////////////////////////////////

  public final static int SHAPE_BALLS      = 0;
  public final static int SHAPE_STICKS     = 1;
  public final static int SHAPE_HSTICKS    = 2;
  public final static int SHAPE_SSSTICKS   = 3;
  public final static int SHAPE_LABELS     = 4;
  public final static int SHAPE_VECTORS    = 5;
  public final static int SHAPE_MEASURES   = 6;
  public final static int SHAPE_DOTS       = 7;
  
  public final static int SHAPE_MIN_SECONDARY  = 8;
  public final static int SHAPE_BACKBONE   = 8;
  public final static int SHAPE_TRACE      = 9;
  public final static int SHAPE_CARTOON    = 10;
  public final static int SHAPE_STRANDS    = 11;
  public final static int SHAPE_MESHRIBBON = 12;
  public final static int SHAPE_RIBBONS    = 13;
  public final static int SHAPE_ROCKETS    = 14;
  public final static int SHAPE_MAX_SECONDARY  = 15;
  
  public final static int SHAPE_STARS      = 15;
  public final static int SHAPE_HALOS      = 16;

  public final static int SHAPE_MIN_SELECTION_INDEPENDENT = 17;
  public final static int SHAPE_AXES       = 17;
  public final static int SHAPE_BBCAGE     = 18;
  public final static int SHAPE_UCCAGE     = 19;
  public final static int SHAPE_FRANK      = 20;
  public final static int SHAPE_ECHO       = 21;
  public final static int SHAPE_HOVER      = 22;
  public final static int SHAPE_POLYHEDRA  = 23;
  public final static int SHAPE_MIN_NAMED_OBJECT = 24;
  public final static int SHAPE_DIPOLES    = 24;
  public final static int SHAPE_MIN_MESH_COLLECTION = 25;
  public final static int SHAPE_PMESH      = 25;
  public final static int SHAPE_MO         = 26;
  public final static int SHAPE_ISOSURFACE = 27;
  public final static int SHAPE_LCAOCARTOON = 28;
  public final static int SHAPE_DRAW       = 29;
  public final static int SHAPE_MAX        = SHAPE_DRAW + 1;
  //handled in Balls.java: (It's ok that this is SHAPE_MAX)
  
  // this atom flag simply associates an atom with the current model
  // but doesn't necessarily mean it is visible

  public final static int ATOM_IN_MODEL    = 1;

  // reserved for future use:
  
  public final static int ATOM_SLABBED     = 2;

  // these atom flags get tainted with scripts and frame changes
  // and must be reset with setModelVisibility() prior to rendering
 
  final static int getShapeVisibilityFlag(int shapeID) {
    return (4 << shapeID);
  }

  //see Jmol 10.2 for programming information --- pruned from 11.0
  //Bob Hanson 7/17/06
  //public final static int SHAPE_PRUEBA     = 28;
  //public final static int SHAPE_SASURFACE  = 29;
  
  // note that these next two arrays *MUST* be in the same sequence 
  // given in SHAPE_*  

  public final static String[] shapeClassBases = {
    "Balls", "Sticks", "Hsticks", "Sssticks",
    "Labels", "Vectors", "Measures", "Dots",
    "Backbone", "Trace", "Cartoon", "Strands", 
    "MeshRibbon", "Ribbons", "Rockets", "Stars", "Halos",
    "Axes", "Bbcage", "Uccage", 
    "Frank", "Echo", "Hover", 
    "Polyhedra", "Dipoles", 
    "Pmesh", "MolecularOrbital", "Isosurface", "LcaoCartoon", "Draw"
    //,"Prueba","Sasurface"
     };

  static {
    if (shapeClassBases.length != SHAPE_MAX) {
      Logger.error("the shapeClassBases array has the wrong length");
      throw new NullPointerException();
    }
  }

  public final static int[] shapeToks = { 
    Token.atom, Token.bonds, Token.hbond, Token.ssbond, 
    Token.label, Token.vector, Token.monitor, Token.dots, 
    Token.backbone, Token.trace, Token.cartoon, Token.strands,
    Token.meshRibbon, Token.ribbon, Token.rocket, Token.star, Token.halo, 
    Token.axes, Token.boundbox, Token.unitcell, 
    Token.frank, Token.echo, Token.hover,
    Token.polyhedra, Token.dipole,
    Token.pmesh, Token.mo, Token.isosurface, Token.lcaocartoon, Token.draw
      //, Token.prueba, Token.sasurface 
      };

  static {
    if (shapeToks.length != SHAPE_MAX) {
      Logger.error("the shapeToks array has the wrong length");
      throw new NullPointerException();
    }
  }

  final static int shapeTokenIndex(int tok) {
    for (int i = shapeToks.length; --i >= 0;)
      if (tok == shapeToks[i])
        return i;
    return -1;

  }
  
  ////////////////////////////////////////////////////////////////
  // Stereo modes
  ////////////////////////////////////////////////////////////////
  public final static int STEREO_NONE     = 0;
  public final static int STEREO_DOUBLE   = 1;
  public final static int STEREO_REDCYAN  = 2;
  public final static int STEREO_REDBLUE  = 3;
  public final static int STEREO_REDGREEN = 4;
  public final static int STEREO_CUSTOM   = 5;

  // all of these things are compile-time constants
  // if they are false then the compiler should take them away
  static {
    if (argbsFormalCharge.length != FORMAL_CHARGE_MAX-FORMAL_CHARGE_MIN+1) {
      Logger.error("formal charge color table length");
      throw new NullPointerException();
    }
    if (shapeClassBases.length != SHAPE_MAX) {
      Logger.error("graphicBaseClasses wrong length");
      throw new NullPointerException();
    }
    if (argbsAmino.length != GROUPID_AMINO_MAX) {
      Logger.error("argbsAmino wrong length");
      throw new NullPointerException();
    }
    if (argbsShapely.length != GROUPID_SHAPELY_MAX) {
      Logger.error("argbsShapely wrong length");
      throw new NullPointerException();
    }
  }
}
