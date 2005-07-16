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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import java.util.Hashtable;

final public class JmolConstants {

  // for now, just update this by hand
  // perhaps use ant filter later ... but mth doesn't like it :-(
  public final static String copyright = "(C) 2005 The Jmol Development Team";
  public final static String version = "10.00.16";
  public final static String cvsDate = "$Date$";
  public final static String date = cvsDate.substring(7, 23);

  public final static boolean officialRelease = false;

  public final static byte LABEL_NONE     = 0;
  public final static byte LABEL_SYMBOL   = 1;
  public final static byte LABEL_TYPENAME = 2;
  public final static byte LABEL_ATOMNO   = 3;

  public final static short MAR_DELETED = Short.MIN_VALUE;

  public final static byte STYLE_DELETED   =-1;
  public final static byte STYLE_NONE      = 0;
  public final static byte STYLE_WIREFRAME = 1;
  public final static byte STYLE_SHADED    = 2;
    
  public final static byte AXES_NONE = 0;
  public final static byte AXES_UNIT = 1;
  public final static byte AXES_BBOX = 2;

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
  public final static byte MULTIBOND_SMALL =     2;
  public final static byte MULTIBOND_ALWAYS =    3;

  public final static short madMultipleBondSmallMaximum = 500;

  /**
   * picking modes
   */
  public final static int PICKING_OFF =           0;
  public final static int PICKING_IDENT =         1;
  public final static int PICKING_DISTANCE =      2;
  public final static int PICKING_MONITOR =       3;
  public final static int PICKING_ANGLE =         4;
  public final static int PICKING_TORSION =       5;
  public final static int PICKING_LABEL =         6;
  public final static int PICKING_CENTER =        7;
  public final static int PICKING_COORD =         8;
  public final static int PICKING_BOND =          9;
  public final static int PICKING_SELECT_ATOM =  10;
  public final static int PICKING_SELECT_GROUP = 11;
  public final static int PICKING_SELECT_CHAIN = 12;

  public final static String[] pickingModeNames = {
    "off", "ident", "distance", "monitor", "angle", "torsion", "label",
    "center", "coord", "bond", "atom", "group", "chain" };

  /**
   * listing of model types
   */
  public final static int MODEL_TYPE_OTHER = 0;
  public final static int MODEL_TYPE_PDB = 1;
  public final static int MODEL_TYPE_XYZ = 2;

  /**
   * Extended Bond Definition Types
   *
   */
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
  public final static short BOND_PARTIAL01     = (1 << 10);
  public final static short BOND_PARTIAL12     = (1 << 11);

  public final static short BOND_ALL_MASK      = (short)0xFFFF;

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
  public final static String[] elementSymbols = {
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
   * @return elementNumber
   */
  public static byte elementNumberFromSymbol(String elementSymbol) {
    if (htElementMap == null) {
      Hashtable map = new Hashtable();
      for (int elementNumber = elementNumberMax; --elementNumber >= 0; ) {
        String symbol = elementSymbols[elementNumber];
        Integer boxed = new Integer(elementNumber);
        map.put(symbol, boxed);
        if (symbol.length() == 2) {
          symbol =
            "" + symbol.charAt(0) + Character.toUpperCase(symbol.charAt(1));
          map.put(symbol, boxed);
        }
        if (elementNumber == 1) {
          // special case for D = deuterium
          //
          // We can put in a special table for these in the future
          // if there are more 'element symbol aliases'
          map.put("D", boxed);
        }
      }
      htElementMap = map;
    }
    if (elementSymbol == null)
      return 0;
    Integer boxedAtomicNumber = (Integer)htElementMap.get(elementSymbol);
    if (boxedAtomicNumber != null)
      return (byte)boxedAtomicNumber.intValue();
    System.out.println("" + elementSymbol + "' is not a recognized symbol");
    return 0;
  }

  public final static String elementNames[] = {
    "unknown",
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
    "praseodymium",  //59
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

  public final static byte[] alternateElementNumbers = {
    0,
    13,
    16,
    55,
  };

  public final static String[] alternateElementNames = {
    "dummy",
    "aluminium",
    "sulphur",
    "caesium",
  };

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
   * ionic radii are looked up using a pair of parallel arrays
   * the ionicLookupTable contains both the elementNumber
   * and the ionization value, represented as follows:
   *   (elementNumber << 4) + (ionizationValue + 4)
   * if you don't understand this representation, don't worry about
   * the binary shifting and stuff. It is just a sorted list
   * of keys
   *
   * the values are stored in the ionicMars table
   * these two arrays are parallel
   *
   * This data is from
   *  Handbook of Chemistry and Physics. 48th Ed, 1967-8, p. F143
   *  (scanned for Jmol by Phillip Barak, Jan 2004)
   ****************************************************************/

  public final static int FORMAL_CHARGE_MIN = -4;
  public final static int FORMAL_CHARGE_MAX = 7;
  public final static short[] ionicLookupTable = {
    (1 << 4) + (-1 + 4),  // 1,-1,1.54,"H"
    (3 << 4) + (1 + 4),   // 3,1,0.68,"Li"
    (4 << 4) + (1 + 4),   // 4,1,0.44,"Be"
    (4 << 4) + (2 + 4),   // 4,2,0.35,"Be"
    (5 << 4) + (1 + 4),   // 5,1,0.35,"B"
    (5 << 4) + (3 + 4),   // 5,3,0.23,"B"
    (6 << 4) + (-4 + 4),  // 6,-4,2.6,"C"
    (6 << 4) + (4 + 4),   // 6,4,0.16,"C"
    (7 << 4) + (-3 + 4),  // 7,-3,1.71,"N"
    (7 << 4) + (1 + 4),   // 7,1,0.25,"N"
    (7 << 4) + (3 + 4),   // 7,3,0.16,"N"
    (7 << 4) + (5 + 4),   // 7,5,0.13,"N"
    (8 << 4) + (-2 + 4),  // 8,-2,1.32,"O"
    (8 << 4) + (-1 + 4),  // 8,-1,1.76,"O"
    (8 << 4) + (1 + 4),   // 8,1,0.22,"O"
    (8 << 4) + (6 + 4),   // 8,6,0.09,"O"
    (9 << 4) + (-1 + 4),  // 9,-1,1.33,"F"
    (9 << 4) + (7 + 4),   // 9,7,0.08,"F"
    (10 << 4) + (1 + 4),  // 10,1,1.12,"Ne"
    (11 << 4) + (1 + 4),  // 11,1,0.97,"Na"
    (12 << 4) + (1 + 4),  // 12,1,0.82,"Mg"
    (12 << 4) + (2 + 4),  // 12,2,0.66,"Mg"
    (13 << 4) + (3 + 4),  // 13,3,0.51,"Al"
    (14 << 4) + (-4 + 4), // 14,-4,2.71,"Si"
    (14 << 4) + (-1 + 4), // 14,-1,3.84,"Si"
    (14 << 4) + (1 + 4),  // 14,1,0.65,"Si"
    (14 << 4) + (4 + 4),  // 14,4,0.42,"Si"
    (15 << 4) + (-3 + 4), // 15,-3,2.12,"P"
    (15 << 4) + (3 + 4),  // 15,3,0.44,"P"
    (15 << 4) + (5 + 4),  // 15,5,0.35,"P"
    (16 << 4) + (-2 + 4), // 16,-2,1.84,"S"
    (16 << 4) + (2 + 4),  // 16,2,2.19,"S"
    (16 << 4) + (4 + 4),  // 16,4,0.37,"S"
    (16 << 4) + (6 + 4),  // 16,6,0.3,"S"
    (17 << 4) + (-1 + 4), // 17,-1,1.81,"Cl"
    (17 << 4) + (5 + 4),  // 17,5,0.34,"Cl"
    (17 << 4) + (7 + 4),  // 17,7,0.27,"Cl"
    (18 << 4) + (1 + 4),  // 18,1,1.54,"Ar"
    (19 << 4) + (1 + 4),  // 19,1,1.33,"K"
    (20 << 4) + (1 + 4),  // 20,1,1.18,"Ca"
    (20 << 4) + (2 + 4),  // 20,2,0.99,"Ca"
    (21 << 4) + (3 + 4),  // 21,3,0.732,"Sc"
    (22 << 4) + (1 + 4),  // 22,1,0.96,"Ti"
    (22 << 4) + (2 + 4),  // 22,2,0.94,"Ti"
    (22 << 4) + (3 + 4),  // 22,3,0.76,"Ti"
    (22 << 4) + (4 + 4),  // 22,4,0.68,"Ti"
    (23 << 4) + (2 + 4),  // 23,2,0.88,"V"
    (23 << 4) + (3 + 4),  // 23,3,0.74,"V"
    (23 << 4) + (4 + 4),  // 23,4,0.63,"V"
    (23 << 4) + (5 + 4),  // 23,5,0.59,"V"
    (24 << 4) + (1 + 4),  // 24,1,0.81,"Cr"
    (24 << 4) + (2 + 4),  // 24,2,0.89,"Cr"
    (24 << 4) + (3 + 4),  // 24,3,0.63,"Cr"
    (24 << 4) + (6 + 4),  // 24,6,0.52,"Cr"
    (25 << 4) + (2 + 4),  // 25,2,0.8,"Mn"
    (25 << 4) + (3 + 4),  // 25,3,0.66,"Mn"
    (25 << 4) + (4 + 4),  // 25,4,0.6,"Mn"
    (25 << 4) + (7 + 4),  // 25,7,0.46,"Mn"
    (26 << 4) + (2 + 4),  // 26,2,0.74,"Fe"
    (26 << 4) + (3 + 4),  // 26,3,0.64,"Fe"
    (27 << 4) + (2 + 4),  // 27,2,0.72,"Co"
    (27 << 4) + (3 + 4),  // 27,3,0.63,"Co"
    (28 << 4) + (2 + 4),  // 28,2,0.69,"Ni"
    (29 << 4) + (1 + 4),  // 29,1,0.96,"Cu"
    (29 << 4) + (2 + 4),  // 29,2,0.72,"Cu"
    (30 << 4) + (1 + 4),  // 30,1,0.88,"Zn"
    (30 << 4) + (2 + 4),  // 30,2,0.74,"Zn"
    (31 << 4) + (1 + 4),  // 31,1,0.81,"Ga"
    (31 << 4) + (3 + 4),  // 31,3,0.62,"Ga"
    (32 << 4) + (-4 + 4), // 32,-4,2.72,"Ge"
    (32 << 4) + (2 + 4),  // 32,2,0.73,"Ge"
    (32 << 4) + (4 + 4),  // 32,4,0.53,"Ge"
    (33 << 4) + (-3 + 4), // 33,-3,2.22,"As"
    (33 << 4) + (3 + 4),  // 33,3,0.58,"As"
    (33 << 4) + (5 + 4),  // 33,5,0.46,"As"
    (34 << 4) + (-2 + 4), // 34,-2,1.91,"Se"
    (34 << 4) + (-1 + 4), // 34,-1,2.32,"Se"
    (34 << 4) + (1 + 4),  // 34,1,0.66,"Se"
    (34 << 4) + (4 + 4),  // 34,4,0.5,"Se"
    (34 << 4) + (6 + 4),  // 34,6,0.42,"Se"
    (35 << 4) + (-1 + 4), // 35,-1,1.96,"Br"
    (35 << 4) + (5 + 4),  // 35,5,0.47,"Br"
    (35 << 4) + (7 + 4),  // 35,7,0.39,"Br"
    (37 << 4) + (1 + 4),  // 37,1,1.47,"Rb"
    (38 << 4) + (2 + 4),  // 38,2,1.12,"Sr"
    (39 << 4) + (3 + 4),  // 39,3,0.893,"Y"
    (40 << 4) + (1 + 4),  // 40,1,1.09,"Zr"
    (40 << 4) + (4 + 4),  // 40,4,0.79,"Zr"
    (41 << 4) + (1 + 4),  // 41,1,1,"Nb"
    (41 << 4) + (4 + 4),  // 41,4,0.74,"Nb"
    (41 << 4) + (5 + 4),  // 41,5,0.69,"Nb"
    (42 << 4) + (1 + 4),  // 42,1,0.93,"Mo"
    (42 << 4) + (4 + 4),  // 42,4,0.7,"Mo"
    (42 << 4) + (6 + 4),  // 42,6,0.62,"Mo"
    (43 << 4) + (7 + 4),  // 43,7,0.979,"Tc"
    (44 << 4) + (4 + 4),  // 44,4,0.67,"Ru"
    (45 << 4) + (3 + 4),  // 45,3,0.68,"Rh"
    (46 << 4) + (2 + 4),  // 46,2,0.8,"Pd"
    (46 << 4) + (4 + 4),  // 46,4,0.65,"Pd"
    (47 << 4) + (1 + 4),  // 47,1,1.26,"Ag"
    (47 << 4) + (2 + 4),  // 47,2,0.89,"Ag"
    (48 << 4) + (1 + 4),  // 48,1,1.14,"Cd"
    (48 << 4) + (2 + 4),  // 48,2,0.97,"Cd"
    (49 << 4) + (3 + 4),  // 49,3,0.81,"In"
    (50 << 4) + (-4 + 4), // 50,-4,2.94,"Sn"
    (50 << 4) + (-1 + 4), // 50,-1,3.7,"Sn"
    (50 << 4) + (2 + 4),  // 50,2,0.93,"Sn"
    (50 << 4) + (4 + 4),  // 50,4,0.71,"Sn"
    (51 << 4) + (-3 + 4), // 51,-3,2.45,"Sb"
    (51 << 4) + (3 + 4),  // 51,3,0.76,"Sb"
    (51 << 4) + (5 + 4),  // 51,5,0.62,"Sb"
    (52 << 4) + (-2 + 4), // 52,-2,2.11,"Te"
    (52 << 4) + (-1 + 4), // 52,-1,2.5,"Te"
    (52 << 4) + (1 + 4),  // 52,1,0.82,"Te"
    (52 << 4) + (4 + 4),  // 52,4,0.7,"Te"
    (52 << 4) + (6 + 4),  // 52,6,0.56,"Te"
    (53 << 4) + (-1 + 4), // 53,-1,2.2,"I"
    (53 << 4) + (5 + 4),  // 53,5,0.62,"I"
    (53 << 4) + (7 + 4),  // 53,7,0.5,"I"
    (55 << 4) + (1 + 4),  // 55,1,1.67,"Cs"
    (56 << 4) + (1 + 4),  // 56,1,1.53,"Ba"
    (56 << 4) + (2 + 4),  // 56,2,1.34,"Ba"
    (57 << 4) + (1 + 4),  // 57,1,1.39,"La"
    (57 << 4) + (3 + 4),  // 57,3,1.016,"La"
    (58 << 4) + (1 + 4),  // 58,1,1.27,"Ce"
    (58 << 4) + (3 + 4),  // 58,3,1.034,"Ce"
    (58 << 4) + (4 + 4),  // 58,4,0.92,"Ce"
    (59 << 4) + (3 + 4),  // 59,3,1.013,"Pr"
    (59 << 4) + (4 + 4),  // 59,4,0.9,"Pr"
    (60 << 4) + (3 + 4),  // 60,3,0.995,"Nd"
    (61 << 4) + (3 + 4),  // 61,3,0.979,"Pm"
    (62 << 4) + (3 + 4),  // 62,3,0.964,"Sm"
    (63 << 4) + (2 + 4),  // 63,2,1.09,"Eu"
    (63 << 4) + (3 + 4),  // 63,3,0.95,"Eu"
    (64 << 4) + (3 + 4),  // 64,3,0.938,"Gd"
    (65 << 4) + (3 + 4),  // 65,3,0.923,"Tb"
    (65 << 4) + (4 + 4),  // 65,4,0.84,"Tb"
    (66 << 4) + (3 + 4),  // 66,3,0.908,"Dy"
    (67 << 4) + (3 + 4),  // 67,3,0.894,"Ho"
    (68 << 4) + (3 + 4),  // 68,3,0.881,"Er"
    (69 << 4) + (3 + 4),  // 69,3,0.87,"Tm"
    (70 << 4) + (2 + 4),  // 70,2,0.93,"Yb"
    (70 << 4) + (3 + 4),  // 70,3,0.858,"Yb"
    (71 << 4) + (3 + 4),  // 71,3,0.85,"Lu"
    (72 << 4) + (4 + 4),  // 72,4,0.78,"Hf"
    (73 << 4) + (5 + 4),  // 73,5,0.68,"Ta"
    (74 << 4) + (4 + 4),  // 74,4,0.7,"W"
    (74 << 4) + (6 + 4),  // 74,6,0.62,"W"
    (75 << 4) + (4 + 4),  // 75,4,0.72,"Re"
    (75 << 4) + (7 + 4),  // 75,7,0.56,"Re"
    (76 << 4) + (4 + 4),  // 76,4,0.88,"Os"
    (76 << 4) + (6 + 4),  // 76,6,0.69,"Os"
    (77 << 4) + (4 + 4),  // 77,4,0.68,"Ir"
    (78 << 4) + (2 + 4),  // 78,2,0.8,"Pt"
    (78 << 4) + (4 + 4),  // 78,4,0.65,"Pt"
    (79 << 4) + (1 + 4),  // 79,1,1.37,"Au"
    (79 << 4) + (3 + 4),  // 79,3,0.85,"Au"
    (80 << 4) + (1 + 4),  // 80,1,1.27,"Hg"
    (80 << 4) + (2 + 4),  // 80,2,1.1,"Hg"
    (81 << 4) + (1 + 4),  // 81,1,1.47,"Tl"
    (81 << 4) + (3 + 4),  // 81,3,0.95,"Tl"
    (82 << 4) + (2 + 4),  // 82,2,1.2,"Pb"
    (82 << 4) + (4 + 4),  // 82,4,0.84,"Pb"
    (83 << 4) + (1 + 4),  // 83,1,0.98,"Bi"
    (83 << 4) + (3 + 4),  // 83,3,0.96,"Bi"
    (83 << 4) + (5 + 4),  // 83,5,0.74,"Bi"
    (84 << 4) + (6 + 4),  // 84,6,0.67,"Po"
    (85 << 4) + (7 + 4),  // 85,7,0.62,"At"
    (87 << 4) + (1 + 4),  // 87,1,1.8,"Fr"
    (88 << 4) + (2 + 4),  // 88,2,1.43,"Ra a"
    (89 << 4) + (3 + 4),  // 89,3,1.18,"Ac"
    (90 << 4) + (4 + 4),  // 90,4,1.02,"Th"
    (91 << 4) + (3 + 4),  // 91,3,1.13,"Pa"
    (91 << 4) + (4 + 4),  // 91,4,0.98,"Pa"
    (91 << 4) + (5 + 4),  // 91,5,0.89,"Pa"
    (92 << 4) + (4 + 4),  // 92,4,0.97,"U"
    (92 << 4) + (6 + 4),  // 92,6,0.8,"U"
    (93 << 4) + (3 + 4),  // 93,3,1.1,"Np"
    (93 << 4) + (4 + 4),  // 93,4,0.95,"Np"
    (93 << 4) + (7 + 4),  // 93,7,0.71,"Np"
    (94 << 4) + (3 + 4),  // 94,3,1.08,"Pu"
    (94 << 4) + (4 + 4),  // 94,4,0.93,"Pu"
    (95 << 4) + (3 + 4),  // 95,3,1.07,"Am"
    (95 << 4) + (4 + 4),  // 95,4,0.92,"Am"
  };

  public final static short[] ionicMars = {
    1540, // "H",1,-1,1.54,1540
    680,  // "Li",3,1,0.68,680
    440,  // "Be",4,1,0.44,440
    350,  // "Be",4,2,0.35,350
    350,  // "B",5,1,0.35,350
    230,  // "B",5,3,0.23,230
    2600, // "C",6,-4,2.6,2600
    160,  // "C",6,4,0.16,160
    1710, // "N",7,-3,1.71,1710
    250,  // "N",7,1,0.25,250
    160,  // "N",7,3,0.16,160
    130,  // "N",7,5,0.13,130
    1320, // "O",8,-2,1.32,1320
    1760, // "O",8,-1,1.76,1760
    220,  // "O",8,1,0.22,220
    90,   // "O",8,6,0.09,90
    1330, // "F",9,-1,1.33,1330
    80,   // "F",9,7,0.08,80
    1120, // "Ne",10,1,1.12,1120
    970,  // "Na",11,1,0.97,970
    820,  // "Mg",12,1,0.82,820
    660,  // "Mg",12,2,0.66,660
    510,  // "Al",13,3,0.51,510
    2710, // "Si",14,-4,2.71,2710
    3840, // "Si",14,-1,3.84,3840
    650,  // "Si",14,1,0.65,650
    420,  // "Si",14,4,0.42,420
    2120, // "P",15,-3,2.12,2120
    440,  // "P",15,3,0.44,440
    350,  // "P",15,5,0.35,350
    1840, // "S",16,-2,1.84,1840
    2190, // "S",16,2,2.19,2190
    370,  // "S",16,4,0.37,370
    300,  // "S",16,6,0.3,300
    1810, // "Cl",17,-1,1.81,1810
    340,  // "Cl",17,5,0.34,340
    270,  // "Cl",17,7,0.27,270
    1540, // "Ar",18,1,1.54,1540
    1330, // "K",19,1,1.33,1330
    1180, // "Ca",20,1,1.18,1180
    990,  // "Ca",20,2,0.99,990
    732,  // "Sc",21,3,0.732,732
    960,  // "Ti",22,1,0.96,960
    940,  // "Ti",22,2,0.94,940
    760,  // "Ti",22,3,0.76,760
    680,  // "Ti",22,4,0.68,680
    880,  // "V",23,2,0.88,880
    740,  // "V",23,3,0.74,740
    630,  // "V",23,4,0.63,630
    590,  // "V",23,5,0.59,590
    810,  // "Cr",24,1,0.81,810
    890,  // "Cr",24,2,0.89,890
    630,  // "Cr",24,3,0.63,630
    520,  // "Cr",24,6,0.52,520
    800,  // "Mn",25,2,0.8,800
    660,  // "Mn",25,3,0.66,660
    600,  // "Mn",25,4,0.6,600
    460,  // "Mn",25,7,0.46,460
    740,  // "Fe",26,2,0.74,740
    640,  // "Fe",26,3,0.64,640
    720,  // "Co",27,2,0.72,720
    630,  // "Co",27,3,0.63,630
    690,  // "Ni",28,2,0.69,690
    960,  // "Cu",29,1,0.96,960
    720,  // "Cu",29,2,0.72,720
    880,  // "Zn",30,1,0.88,880
    740,  // "Zn",30,2,0.74,740
    810,  // "Ga",31,1,0.81,810
    620,  // "Ga",31,3,0.62,620
    2720, // "Ge",32,-4,2.72,2720
    730,  // "Ge",32,2,0.73,730
    530,  // "Ge",32,4,0.53,530
    2220, // "As",33,-3,2.22,2220
    580,  // "As",33,3,0.58,580
    460,  // "As",33,5,0.46,460
    1910, // "Se",34,-2,1.91,1910
    2320, // "Se",34,-1,2.32,2320
    660,  // "Se",34,1,0.66,660
    500,  // "Se",34,4,0.5,500
    420,  // "Se",34,6,0.42,420
    1960, // "Br",35,-1,1.96,1960
    470,  // "Br",35,5,0.47,470
    390,  // "Br",35,7,0.39,390
    1470, // "Rb",37,1,1.47,1470
    1120, // "Sr",38,2,1.12,1120
    893,  // "Y",39,3,0.893,893
    1090, // "Zr",40,1,1.09,1090
    790,  // "Zr",40,4,0.79,790
    1000, // "Nb",41,1,1,1000
    740,  // "Nb",41,4,0.74,740
    690,  // "Nb",41,5,0.69,690
    930,  // "Mo",42,1,0.93,930
    700,  // "Mo",42,4,0.7,700
    620,  // "Mo",42,6,0.62,620
    979,  // "Tc",43,7,0.979,979
    670,  // "Ru",44,4,0.67,670
    680,  // "Rh",45,3,0.68,680
    800,  // "Pd",46,2,0.8,800
    650,  // "Pd",46,4,0.65,650
    1260, // "Ag",47,1,1.26,1260
    890,  // "Ag",47,2,0.89,890
    1140, // "Cd",48,1,1.14,1140
    970,  // "Cd",48,2,0.97,970
    810,  // "In",49,3,0.81,810
    2940, // "Sn",50,-4,2.94,2940
    3700, // "Sn",50,-1,3.7,3700
    930,  // "Sn",50,2,0.93,930
    710,  // "Sn",50,4,0.71,710
    2450, // "Sb",51,-3,2.45,2450
    760,  // "Sb",51,3,0.76,760
    620,  // "Sb",51,5,0.62,620
    2110, // "Te",52,-2,2.11,2110
    2500, // "Te",52,-1,2.5,2500
    820,  // "Te",52,1,0.82,820
    700,  // "Te",52,4,0.7,700
    560,  // "Te",52,6,0.56,560
    2200, // "I",53,-1,2.2,2200
    620,  // "I",53,5,0.62,620
    500,  // "I",53,7,0.5,500
    1670, // "Cs",55,1,1.67,1670
    1530, // "Ba",56,1,1.53,1530
    1340, // "Ba",56,2,1.34,1340
    1390, // "La",57,1,1.39,1390
    1016, // "La",57,3,1.016,1016
    1270, // "Ce",58,1,1.27,1270
    1034, // "Ce",58,3,1.034,1034
    920,  // "Ce",58,4,0.92,920
    1013, // "Pr",59,3,1.013,1013
    900,  // "Pr",59,4,0.9,900
    995,  // "Nd",60,3,0.995,995
    979,  // "Pm",61,3,0.979,979
    964,  // "Sm",62,3,0.964,964
    1090, // "Eu",63,2,1.09,1090
    950,  // "Eu",63,3,0.95,950
    938,  // "Gd",64,3,0.938,938
    923,  // "Tb",65,3,0.923,923
    840,  // "Tb",65,4,0.84,840
    908,  // "Dy",66,3,0.908,908
    894,  // "Ho",67,3,0.894,894
    881,  // "Er",68,3,0.881,881
    870,  // "Tm",69,3,0.87,870
    930,  // "Yb",70,2,0.93,930
    858,  // "Yb",70,3,0.858,858
    850,  // "Lu",71,3,0.85,850
    780,  // "Hf",72,4,0.78,780
    680,  // "Ta",73,5,0.68,680
    700,  // "W",74,4,0.7,700
    620,  // "W",74,6,0.62,620
    720,  // "Re",75,4,0.72,720
    560,  // "Re",75,7,0.56,560
    880,  // "Os",76,4,0.88,880
    690,  // "Os",76,6,0.69,690
    680,  // "Ir",77,4,0.68,680
    800,  // "Pt",78,2,0.8,800
    650,  // "Pt",78,4,0.65,650
    1370, // "Au",79,1,1.37,1370
    850,  // "Au",79,3,0.85,850
    1270, // "Hg",80,1,1.27,1270
    1100, // "Hg",80,2,1.1,1100
    1470, // "Tl",81,1,1.47,1470
    950,  // "Tl",81,3,0.95,950
    1200, // "Pb",82,2,1.2,1200
    840,  // "Pb",82,4,0.84,840
    980,  // "Bi",83,1,0.98,980
    960,  // "Bi",83,3,0.96,960
    740,  // "Bi",83,5,0.74,740
    670,  // "Po",84,6,0.67,670
    620,  // "At",85,7,0.62,620
    1800, // "Fr",87,1,1.8,1800
    1430, // "Ra a",88,2,1.43,1430
    1180, // "Ac",89,3,1.18,1180
    1020, // "Th",90,4,1.02,1020
    1130, // "Pa",91,3,1.13,1130
    980,  // "Pa",91,4,0.98,980
    890,  // "Pa",91,5,0.89,890
    970,  // "U",92,4,0.97,970
    800,  // "U",92,6,0.8,800
    1100, // "Np",93,3,1.1,1100
    950,  // "Np",93,4,0.95,950
    710,  // "Np",93,7,0.71,710
    1080, // "Pu",94,3,1.08,1080
    930,  // "Pu",94,4,0.93,930
    1070, // "Am",95,3,1.07,1070
    920,  // "Am",95,4,0.92,920
  };

  public static short getBondingMar(int elementNumber, int charge) {
    if (charge != 0) {
      // ionicLookupTable is a sorted table of ionic keys
      // lookup doing a binary search
      // when found, return the corresponding value in ionicMars
      // if not found, just return covalent radius
      short ionic = (short)((elementNumber << 4)+(charge + 4));
      int iMin = 0, iMax = ionicLookupTable.length;
      while (iMin != iMax) {
        int iMid = (iMin + iMax) / 2;
        if (ionic < ionicLookupTable[iMid])
          iMax = iMid;
        else if (ionic > ionicLookupTable[iMid])
          iMin = iMid + 1;
        else
          return ionicMars[iMid];
      }
    }
    return (short)covalentMars[elementNumber];
  }

  public static boolean isValidFormalCharge(int elementNumber, int charge) {
    if (charge == 0)
      return true;
    if (charge < FORMAL_CHARGE_MIN || charge > FORMAL_CHARGE_MAX)
      return false;
    short ionic = (short)((elementNumber << 4)+(charge + 4));
    int iMin = 0, iMax = ionicLookupTable.length;
    while (iMin != iMax) {
      int iMid = (iMin + iMax) / 2;
      if (ionic < ionicLookupTable[iMid])
        iMax = iMid;
      else if (ionic > ionicLookupTable[iMid])
        iMin = iMid + 1;
      else
        return true;
    }
    return false;
  }

  // maximum number of bonds that an atom can have when
  // autoBonding
  // All bonding is done by distances
  // this is only here for truly pathological cases
  public final static int MAXIMUM_AUTO_BOND_COUNT = 20;

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
      System.out.println("ERROR!!! Element table length mismatch:" +
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

  /*
  public final static int[] argbsChainAtom = {
    // ' '->0 'A'->1, 'B'->2
    // protein explorer colors
    0xFFffffff, // ' ' & '0' white - pewhite 0xFFffffff
    //
    0xFF40E0D0, // A & 1 turquoise - pecyan 0xFF00ffff
    0xFFDA70D6, // B & 2 orchid - pepurple 0xFFd020ff
    0xFF00FF00, // C & 3 lime - pegreen 0xFF00ff00
    0xFF6495ED, // D & 4 cornflowerblue - peblue 0xFF6060ff
    0xFFFF69B4, // E & 5 hotpink - peviolet 0xFFff80c0
    0xFFA52A2A, // F & 6 brown - pebrown 0xFFa42028
    0xFFFFC0CB, // G & 7 pink - pepink 0xFFffd8d8
    0xFFFFFF00, // H & 8 yellow - peyellow 0xFFffff00
    0xFF228B22, // I & 9 forestgreen - pedarkgreen 0xFF00c000
    0xFFFFA500, // J orange - peorange 0xFFffb000
    0xFF87CEEB, // K skyblue - pelightblue 0xFFb0b0ff
    0xFF008080, // L teal - pedarkcyan 0xFF00a0a0
    0xFF606060, // M pedarkgray 0xFF606060
    // pick two more colors
    0xFF0000CD, // N mediumblue
    0xFFf6f675, // O yellowtint

    0xFFFF6347, // P tomato
    0xFFC8A880, // Q a darkened tan
    0xFF800080, // R purple
    0xFF808000, // S olive
    0xFFF4A460, // T sandybrown
    0xFF7FFFD4, // U aquamarine
    0xFFB8860B, // V darkgoldenrod
    0xFFF08080, // W lightcoral
    0xFF9ACD32, // X yellowgreen
    0xFF00008B, // Y darkblue
    0xFFF5DEB3, // Z wheat
  };

  public final static int[] argbsChainHetero = {
    // ' '->0 'A'->1, 'B'->2
    0xFFD0D0D0, // ' ' & '0' a light gray
    //
    0xFF20c0b0, // A & 1 a darker turquoise
    0xFFB850b6, // B & 2 a darker orchid
    0xFF00C800, // C & 3 a darker limegreen
    0xFF4070D0, // D & 4 a darker cornflowerblue
    0xFFE04890, // E & 5 a darker hotpink
    0xFF800008, // F & 6 a darker brown
    0xFFD898A0, // G & 7 a darker pink
    0xFFD0D000, // H & 8 a darker yellow
    0xFF006400, // I & 9 darkgreen
    0xFFE08500, // J a darker orange
    0xFF68A8C8, // K a darker skyblue
    0xFF006060, // L a darker teal
    0xFF484848, // M a darker gray
    // pick two more colors
    0xFF0000A0, // N a darker blue
    0xFFC8C858, // O a darker yellow

    0xFFd84838, // P a darker tomato
    0xFFA4845C, // Q a darker tan
    0xFF600060, // R a deeper purple
    0xFF606000, // S a darker olive
    0xFFD88840, // T a darker sandybrown
    0xFF58D8AC, // U a darker aquamarine
    0xFF986600, // V a darker darkgoldenrod
    0xFFD86868, // W a darker lightcoral
    0xFF78A810, // X a darker yellowgreen
    0xFF000060, // Y a darker darkblue
    0xFFD8B898, // Z a darker wheat
  };
  */

  public final static int[] argbsCharge = {
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

  ////////////////////////////////////////////////////////////////
  // predefined sets
  ////////////////////////////////////////////////////////////////

  public static String[] predefinedSets = {
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
    "@surface !buried", // this looks wrong to me -- mth

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
    "@c nucleic & within(group,_a=74)",
    "@g nucleic & within(group,_a=75)",
    "@cg c,g",
    "@a nucleic & within(group,_a=76)",
    "@t nucleic & within(group,_a=77)",
    "@at a,t",
    "@i nucleic & within(group,_a=78) & !g",
    "@u nucleic & within(group,_a=79) & !t",
    "@tu nucleic & within(group,_a=80)",

    //
    // solvent
    //
    "@solvent _g>=70 & _g<=74", // water or ions
    "@hoh water",
    "@water _g>=70 & _g<=72",
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
  // also updating Eval.java to confirm consistent
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
  public final static int SHAPE_BACKBONE   = 8;
  public final static int SHAPE_TRACE      = 9;
  public final static int SHAPE_CARTOON    = 10;
  public final static int SHAPE_STRANDS    = 11;
  public final static int SHAPE_MESHRIBBON = 12;
  public final static int SHAPE_RIBBONS    = 13;
  public final static int SHAPE_ROCKETS    = 14;
  public final static int SHAPE_STARS      = 15;

  public final static int SHAPE_MIN_SELECTION_INDEPENDENT = 16;
  public final static int SHAPE_AXES       = 16;
  public final static int SHAPE_BBCAGE     = 17;
  public final static int SHAPE_UCCAGE     = 18;
  public final static int SHAPE_FRANK      = 19;
  public final static int SHAPE_ECHO       = 20;
  public final static int SHAPE_HOVER      = 21;
  public final static int SHAPE_PMESH      = 22;
  public final static int SHAPE_POLYHEDRA  = 23;
  public final static int SHAPE_SURFACE    = 24;
  public final static int SHAPE_VOLUMETRIC = 25;
  public final static int SHAPE_PRUEBA     = 26;
  public final static int SHAPE_MAX        = 27;

  public final static String[] shapeClassBases = {
    "Balls", "Sticks", "Hsticks", "Sssticks",
    "Labels", "Vectors", "Measures",
    "Dots",
    "Backbone", "Trace",
    "Cartoon",
    "Strands", "MeshRibbon", "Ribbons",
    "Rockets", "Stars",
    "Axes", "Bbcage", "Uccage", "Frank", "Echo", "Hover",
    "Pmesh", "Polyhedra", "Surface", "Volumetric",
    "Prueba"
  };

  // all of these things are compile-time constants
  // if they are false then the compiler should take them away
  static {
    if (ionicLookupTable.length != ionicMars.length) {
      System.out.println("ionic table mismatch!");
      throw new NullPointerException();
    }
    for (int i = ionicLookupTable.length; --i > 0; ) {
      if (ionicLookupTable[i - 1] >= ionicLookupTable[i]) {
        System.out.println("ionicLookupTable not sorted properly");
        throw new NullPointerException();
      }
    }
    if (argbsCharge.length != FORMAL_CHARGE_MAX - FORMAL_CHARGE_MIN + 1) {
      System.out.println("charge color table length");
      throw new NullPointerException();
    }
    if (shapeClassBases.length != SHAPE_MAX) {
      System.out.println("graphicBaseClasses wrong length");
      throw new NullPointerException();
    }
    if (argbsAmino.length != GROUPID_AMINO_MAX) {
      System.out.println("argbsAmino wrong length");
      throw new NullPointerException();
    }
    if (argbsShapely.length != GROUPID_SHAPELY_MAX) {
      System.out.println("argbsShapely wrong length");
      throw new NullPointerException();
    }
  }
}
