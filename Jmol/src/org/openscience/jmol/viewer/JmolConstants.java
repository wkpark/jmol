/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer;

final public class JmolConstants {

  // for now, just update this by hand
  // perhaps use ant filter later ... but mth doesn't like it :-(
  public static String version="10pre4a";
  public final static String copyright="(C) 2004 The Jmol Development Team";

  public final static byte LABEL_NONE     = 0;
  public final static byte LABEL_SYMBOL   = 1;
  public final static byte LABEL_TYPENAME = 2;
  public final static byte LABEL_ATOMNO   = 3;

  public final static byte STYLE_DELETED   =-1;
  public final static byte STYLE_NONE      = 0;
  public final static byte STYLE_WIREFRAME = 1;
  public final static byte STYLE_SHADED    = 2;
    
  public final static byte PALETTE_COLOR      =-1;
  public final static byte PALETTE_CPK        = 0;
  public final static byte PALETTE_CHARGE     = 1;
  public final static byte PALETTE_STRUCTURE  = 2;
  public final static byte PALETTE_AMINO      = 3;
  public final static byte PALETTE_SHAPELY    = 4;
  public final static byte PALETTE_CHAIN      = 5;
  public final static byte PALETTE_MAX        = 6;

  public final static byte AXES_NONE = 0;
  public final static byte AXES_UNIT = 1;
  public final static byte AXES_BBOX = 2;

  public static final int MOUSE_ROTATE = 0;
  public static final int MOUSE_ZOOM = 1;
  public static final int MOUSE_XLATE = 2;
  public static final int MOUSE_PICK = 3;
  public static final int MOUSE_DELETE = 4;
  public static final int MOUSE_MEASURE = 5;
  public static final int MOUSE_DEFORM = 6; // mth -- what is this?
  public static final int MOUSE_ROTATE_Z = 7;
  public static final int MOUSE_SLAB_PLANE = 8;
  public static final int MOUSE_POPUP_MENU = 9;

  public final static byte MULTIBOND_NEVER =     0;
  public final static byte MULTIBOND_WIREFRAME = 1;
  public final static byte MULTIBOND_SMALL =     2;
  public final static byte MULTIBOND_ALWAYS =    3;

  public final static short marMultipleBondSmallMaximum = 250;

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
  public final static byte BOND_COVALENT      = 3;
  public final static byte BOND_AROMATIC_MASK = (1 << 2);
  public final static byte BOND_AROMATIC      = (1 << 2) | 1;
  public final static byte BOND_STEREO_MASK   = (3 << 3);
  public final static byte BOND_STEREO_NEAR   = (1 << 3) | 1;
  public final static byte BOND_STEREO_FAR    = (2 << 3) | 2;
  public final static byte BOND_SULFUR_MASK   = (1 << 5);
  public final static byte BOND_HYDROGEN      = (1 << 6);
  public final static byte BOND_ALL_MASK      = (byte)0xFF;

  /**
   * The default atomicSymbols. Presumably the only entry which may cause
   * confusion is element 0, whose symbol we have defined as "Xx". 
   */
  public final static String[] atomicSymbols = {
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
   * one larger than the last atomicNumber, same as atomicSymbols.length
   */
  public final static int atomicNumberMax = atomicSymbols.length;

  /**
   * Default table of van der Waals Radii.
   * values are stored as MAR -- Milli Angstrom Radius
   * Used when the client does not choose
   * to implement getVanderwaalsRadius(clientAtom).
   * Used for spacefill rendering of atoms.
   * Values taken from OpenBabel.
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
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
    1700, //  72  H00
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
   * Default table of covalent Radii.
   * stored as a short mar ... Milli Angstrom Radius
   * Used when the client does not choose
   * to implement getCovalentRadius(clientAtom).
   * Used for bonding atoms when the client does not supply bonds. 
   * Values taken from OpenBabel.
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   */
  public final static short[] covalentMars = {
    2000, //   0  Xx big enough to bring attention to itself
    230, //   1  H
    930, //   2  He
    680, //   3  Li
    350, //   4  Be
    830, //   5  B
    680, //   6  C
    680, //   7  N
    680, //   8  O
    640, //   9  F
    1120, //  10  Ne
    970, //  11  Na
    1100, //  12  Mg
    1350, //  13  Al
    1200, //  14  Si
    750, //  15  P
    1020, //  16  S
    990, //  17  Cl
    1570, //  18  Ar
    1330, //  19  K
    990, //  20  Ca
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

  /**
   * Default table of CPK atom colors.
   * Used when the client does not implement
   * getAtomColor(clientAtom, colorScheme)
   * I didn't know what color to define for many of the atoms,
   * so I made them HotPink.
   */
  public final static int[] argbsCpk = {
    0xFFFF69B4, // HotPink        Xx   0
    //    0xFFFFFAF0, // floralwhite 0xFFFFFAF0
    0xFFE0E0E0, // grey88          H   1
    //    0xFFF0FFFF, // azure           H   1
    //    0xFFF0F8FF, // AliceBlue    H   1
    0xFFFFC0CB, // pink           He   2
    0xFFB22222, // firebrick      Li   3
    0xFF228B22, // ForestGreen    Be   4
    0xFF00FF00, // green           B   5
    0xFF708090, // SlateGray       C   6
    //0xFF545454, // grey33          C   6
    0xFF00BFFF, // DeepSkyBlue     N   7
    0xFFEE0000, // red2            O   8
    0xFFDAA520, // goldenrod       F   9 change me
    0xFFFF69B4, // HotPink        Ne  10 change me
    0xFF0000FF, // blue           Na  11
    0xFF228B22, // ForestGreen    Mg  12
    0xFFBFBFBF, // grey75         Al  13
    0xFFDAA520, // goldenrod      Si  14
    0xFFFFA500, // orange          P  15
    0xFFFFFF00, // yellow          S  16
    0xFF00FF00, // green          Cl  17
    0xFFFFC0CB, // HotPink        Ar  18 change me
    0xFFFF1493, // DeepPink        K  19
    0xFF7F7F7F, // grey50         Ca  20
    0xFFBFBFBF, // grey75         Sc  21
    0xFFBFBFBF, // grey75         Ti  22
    0xFFBFBFBF, // grey75          V  23
    0xFFBFBFBF, // grey75         Cr  24
    0xFFBFBFBF, // grey75         Mn  25
    0xFFFFA500, // orange         Fe  26
    0xFFA52A2A, // brown          Co  27
    0xFFA52A2A, // brown          Ni  28
    0xFFA52A2A, // brown          Cu  29
    0xFFA52A2A, // brown          Zn  30
    0xFFA52A2A, // brown          Ga  31
    0xFF556B2F, // DarkOliveGreen Ge  32
    0xFFFDF5E6, // OldLace        As  33
    0xFF98FB98, // PaleGreen      Se  34
    0xFFA52A2A, // brown          Br  35
    0xFF32CD32, // LimeGreen      Kr  36
    0xFFA52A2A, // brown          Rb  37
    0xFFBFBFBF, // grey75         Sr  38
    0xFFBFBFBF, // grey75         Y	  9
    0xFFBFBFBF, // grey75         Zr  40
    0xFFBFBFBF, // grey75         Nb  41
    0xFFFF7F50, // coral          Mo  42
    0xFFBFBFBF, // grey75         Tc  43
    0xFFBFBFBF, // grey75         Ru  44
    0xFFBFBFBF, // grey75         Rh  45
    0xFFBFBFBF, // grey75         Pd  46
    0xFFBFBFBF, // grey75         Ag  47
    0xFFFF8C00, // DarkOrange     Cd  48
    0xFFBFBFBF, // grey75         In  49
    0xFFBFBFBF, // grey75         Sn  50
    0xFFBFBFBF, // grey75         Sb  51
    0xFFBFBFBF, // grey75         Te  52
    0xFFA020F0, // purple          I  53
    0xFFFF69B4, // HotPink        Xe  54
    0xFFA52A2A, // brown          Cs  55
    0xFFBFBFBF, // grey75         Ba  56
    0xFFBFBFBF, // grey75         La  57
    0xFFFF6575, // ??         Ce  58
    0xFFFF6575, //            Pr  59
    0xFFFF6575, //            Nd  60
    0xFFFF6575, //            Pm  61
    0xFFFF6575, //            Sm  62
    0xFFFF6575, //            Eu  63
    0xFFFF6575, //            Gd  64
    0xFFFF6575, //            Tb  65
    0xFFFF6575, //            Dy  66
    0xFFFF6575, //            Ho  67
    0xFFFF6575, //            Er  68
    0xFFFF6575, //            Tm  69
    0xFFFF6575, //            Yb  70
    0xFFBFBFBF, // grey75         Lu  71
    0xFFBFBFBF, // grey75         Hf  72
    0xFFBFBFBF, // grey75         Ta  73
    0xFF40E0D0, // turquoise       W  74
    0xFFBFBFBF, // grey75         Re  75
    0xFFBFBFBF, // grey75         Os  76
    0xFFBFBFBF, // grey75         Ir  77
    0xFFBFBFBF, // grey75         Pt  78
    0xFFFFD700, // gold           Au  79
    0xFFBFBFBF, // grey75         Hg  80
    0xFFBFBFBF, // grey75         Tl  81
    0xFFBFBFBF, // grey75         Pb  82
    0xFFFFB5C5, // pink1          Bi  83
    0xFFFF6575, //            Po  84
    0xFFFF6575, //            At  85
    0xFFFF6575, //            Rn  86
    0xFFFF6575, //            Fr  87
    0xFFFF6575, //            Ra  88
    0xFFFF6575, //            Ac  89
    0xFFFF6575, //            Th  90
    0xFFFF6575, //            Pa  91
    0xFFFF6575, //             U  92
    0xFFFF6575, //            Np  93
    0xFFFF6575, //            Pu  94
    0xFFFF6575, //            Am  95
    0xFFFF6575, //            Cm  96
    0xFFFF6575, //            Bk  97
    0xFFFF6575, //            Cf  98
    0xFFFF6575, //            Es  99
    0xFFFF6575, //            Fm 100
    0xFFFF6575, //            Md 101
    0xFFFF6575, //            No 102
    0xFFFF6575, //            Lr 103
    0xFFFF6575, //            Rf 104
    0xFFFF6575, //            Db 105
    0xFFFF6575, //            Sg 106
    0xFFFF6575, //            Bh 107
    0xFFFF6575, //            Hs 108
    0xFFFF6575, //            Mt 109
  };

  /**
   * Default table of PdbStructure colors
   */
  public final static byte SECONDARY_STRUCTURE_NONE = 0;
  public final static byte SECONDARY_STRUCTURE_TURN = 1;
  public final static byte SECONDARY_STRUCTURE_SHEET = 2;
  public final static byte SECONDARY_STRUCTURE_HELIX = 3;

  public final static int[] argbsPdbStructure = {
    0xFFFFFFFF, // white       STRUCTURE_NONE
    0xFF4876FF, // RoyalBlue1  STRUCTURE_TURN
    0xFFFFFF00, // yellow      STRUCTURE_SHEET
    0xFFEE00EE, // magenta2    STRUCTURE_HELIX
  };

  public final static int argbPdbAminoDefault =  0xFFBEA06E; // tan
  public final static int[] argbsPdbAmino = {
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

  };

  public final static int argbPdbShapelyBackbone = 0xFFB8B8B8;
  public final static int argbPdbShapelySpecial =  0xFF5E005E;
  public final static int argbPdbShapelyDefault =  0xFFFF00FF;
  public final static int[] argbsPdbShapely = {
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

    0xFFFF00FF, // ASX
    0xFFFF00FF, // GLX
    0xFFFF00FF, // UNK

    0xFFA0A0FF, // A
    0xFFFF8C4B, // C
    0xFFFF7070, // G
    0xFFA0FFA0, // T
  };

  /**
   * colors used for chains
   * as currently implemented, this array must be of length 8
   */

  public final static int[] argbsPdbChainAtom = {
    // xwindows colors
    // ' '->0 'A'->1, 'B'->2
    0xFFFFFAF0, // floralwhite 0xFFFFFAF0
    0xFF0000FF, // blue 0xFF0000FF
    0xFF00FFFF, // cyan 0xFF00FFFF
    0xFF008000, // green 0xFF008000
    0xFFFFFF00, // yellow 0xFFFFFF00
    // these need to be changed
    0xFFFF0000, // red 0xFFFF0000
    0xFFFF00FF, // magenta 0xFFFF00FF
    0xFFFFA500, // orange 0xFFFFA500
  };

  public final static int[] argbsPdbChainHetero = {
    // xwindows colors
    // ' '->0 'A'->1, 'B'->2
    0xFFFF0000, // red
    0xFF1E90FF, // dodgerblue
    0xFF00FA9A, // mediumspringgreen
    0xFF6B8E23, // olivedrab
    0xFFFF8C00, // darkorange
    // these need to be changed
    0xFFFF00FF, // magenta 0xFFFF00FF
    0xFFFFA500, // orange 0xFFFFA500
    0xFF87CEEB, // SkyBlue 0xFF87CEEB
  };

  // 140 JavaScript color names
  // includes 16 official HTML 4.0 color names & values
  // plus a few extra rasmol names

  public final static String[] colorNames = {
    "aliceblue",            // F0F8FF
    "antiquewhite",         // FAEBD7
    "aqua",                 // 00FFFF
    "aquamarine",           // 7FFFD4
    "azure",                // F0FFFF
    "beige",                // F5F5DC
    "bisque",               // FFE4C4
    "black",                // 000000
    "blanchedalmond",       // FFEBCD
    "blue",                 // 0000FF
    "blueviolet",           // 8A2BE2
    "brown",                // A52A2A
    "burlywood",            // DEB887
    "cadetblue",            // 5F9EA0
    "chartreuse",           // 7FFF00
    "chocolate",            // D2691E
    "coral",                // FF7F50
    "cornflowerblue",       // 6495ED
    "cornsilk",             // FFF8DC
    "crimson",              // DC143C
    "cyan",                 // 00FFFF
    "darkblue",             // 00008B
    "darkcyan",             // 008B8B
    "darkgoldenrod",        // B8860B
    "darkgray",             // A9A9A9
    "darkgreen",            // 006400
    "darkkhaki",            // BDB76B
    "darkmagenta",          // 8B008B
    "darkolivegreen",       // 556B2F
    "darkorange",           // FF8C00
    "darkorchid",           // 9932CC
    "darkred",              // 8B0000
    "darksalmon",           // E9967A
    "darkseagreen",         // 8FBC8F
    "darkslateblue",        // 483D8B
    "darkslategray",        // 2F4F4F
    "darkturquoise",        // 00CED1
    "darkviolet",           // 9400D3
    "deeppink",             // FF1493
    "deepskyblue",          // 00BFFF
    "dimgray",              // 696969
    "dodgerblue",           // 1E90FF
    "firebrick",            // B22222
    "floralwhite",          // FFFAF0
    "forestgreen",          // 228B22
    "fuchsia",              // FF00FF
    "gainsboro",            // DCDCDC
    "ghostwhite",           // F8F8FF
    "gold",                 // FFD700
    "goldenrod",            // DAA520
    "gray",                 // 808080
    "green",                // 008000
    "greenyellow",          // ADFF2F
    "honeydew",             // F0FFF0
    "hotpink",              // FF69B4
    "indianred",            // CD5C5C
    "indigo",               // 4B0082
    "ivory",                // FFFFF0
    "khaki",                // F0E68C
    "lavender",             // E6E6FA
    "lavenderblush",        // FFF0F5
    "lawngreen",            // 7CFC00
    "lemonchiffon",         // FFFACD
    "lightblue",            // ADD8E6
    "lightcoral",           // F08080
    "lightcyan",            // E0FFFF
    "lightgoldenrodyellow", // FAFAD2
    "lightgreen",           // 90EE90
    "lightgrey",            // D3D3D3
    "lightpink",            // FFB6C1
    "lightsalmon",          // FFA07A
    "lightseagreen",        // 20B2AA
    "lightskyblue",         // 87CEFA
    "lightslategray",       // 778899
    "lightsteelblue",       // B0C4DE
    "lightyellow",          // FFFFE0
    "lime",                 // 00FF00
    "limegreen",            // 32CD32
    "linen",                // FAF0E6
    "magenta",              // FF00FF
    "maroon",               // 800000
    "mediumaquamarine",     // 66CDAA
    "mediumblue",           // 0000CD
    "mediumorchid",         // BA55D3
    "mediumpurple",         // 9370DB
    "mediumseagreen",       // 3CB371
    "mediumslateblue",      // 7B68EE
    "mediumspringgreen",    // 00FA9A
    "mediumturquoise",      // 48D1CC
    "mediumvioletred",      // C71585
    "midnightblue",         // 191970
    "mintcream",            // F5FFFA
    "mistyrose",            // FFE4E1
    "moccasin",             // FFE4B5
    "navajowhite",          // FFDEAD
    "navy",                 // 000080
    "oldlace",              // FDF5E6
    "olive",                // 808000
    "olivedrab",            // 6B8E23
    "orange",               // FFA500
    "orangered",            // FF4500
    "orchid",               // DA70D6
    "palegoldenrod",        // EEE8AA
    "palegreen",            // 98FB98
    "paleturquoise",        // AFEEEE
    "palevioletred",        // DB7093
    "papayawhip",           // FFEFD5
    "peachpuff",            // FFDAB9
    "peru",                 // CD853F
    "pink",                 // FFC0CB
    "plum",                 // DDA0DD
    "powderblue",           // B0E0E6
    "purple",               // 800080
    "red",                  // FF0000
    "rosybrown",            // BC8F8F
    "royalblue",            // 4169E1
    "saddlebrown",          // 8B4513
    "salmon",               // FA8072
    "sandybrown",           // F4A460
    "seagreen",             // 2E8B57
    "seashell",             // FFF5EE
    "sienna",               // A0522D
    "silver",               // C0C0C0
    "skyblue",              // 87CEEB
    "slateblue",            // 6A5ACD
    "slategray",            // 708090
    "snow",                 // FFFAFA
    "springgreen",          // 00FF7F
    "steelblue",            // 4682B4
    "tan",                  // D2B48C
    "teal",                 // 008080
    "thistle",              // D8BFD8
    "tomato",               // FF6347
    "turquoise",            // 40E0D0
    "violet",               // EE82EE
    "wheat",                // F5DEB3
    "white",                // FFFFFF
    "whitesmoke",           // F5F5F5
    "yellow",               // FFFF00
    "yellowgreen",          // 9ACD32
    // plus a few rasmol names/values
    "bluetint",             // AFD7FF
    "greenblue",            // 2E8B57
    "greentint",            // 98FFB3
    "grey",                 // 808080
    "pinktint",             // FFABBB
    "redorange",            // FF4500
    "yellowtint",           // F6F675
  };

  public final static int[] colorArgbs = {
    0xFFF0F8FF, // aliceblue
    0xFFFAEBD7, // antiquewhite
    0xFF00FFFF, // aqua
    0xFF7FFFD4, // aquamarine
    0xFFF0FFFF, // azure
    0xFFF5F5DC, // beige
    0xFFFFE4C4, // bisque
    0xFF000000, // black
    0xFFFFEBCD, // blanchedalmond
    0xFF0000FF, // blue
    0xFF8A2BE2, // blueviolet
    0xFFA52A2A, // brown
    0xFFDEB887, // burlywood
    0xFF5F9EA0, // cadetblue
    0xFF7FFF00, // chartreuse
    0xFFD2691E, // chocolate
    0xFFFF7F50, // coral
    0xFF6495ED, // cornflowerblue
    0xFFFFF8DC, // cornsilk
    0xFFDC143C, // crimson
    0xFF00FFFF, // cyan
    0xFF00008B, // darkblue
    0xFF008B8B, // darkcyan
    0xFFB8860B, // darkgoldenrod
    0xFFA9A9A9, // darkgray
    0xFF006400, // darkgreen
    0xFFBDB76B, // darkkhaki
    0xFF8B008B, // darkmagenta
    0xFF556B2F, // darkolivegreen
    0xFFFF8C00, // darkorange
    0xFF9932CC, // darkorchid
    0xFF8B0000, // darkred
    0xFFE9967A, // darksalmon
    0xFF8FBC8F, // darkseagreen
    0xFF483D8B, // darkslateblue
    0xFF2F4F4F, // darkslategray
    0xFF00CED1, // darkturquoise
    0xFF9400D3, // darkviolet
    0xFFFF1493, // deeppink
    0xFF00BFFF, // deepskyblue
    0xFF696969, // dimgray
    0xFF1E90FF, // dodgerblue
    0xFFB22222, // firebrick
    0xFFFFFAF0, // floralwhite
    0xFF228B22, // forestgreen
    0xFFFF00FF, // fuchsia
    0xFFDCDCDC, // gainsboro
    0xFFF8F8FF, // ghostwhite
    0xFFFFD700, // gold
    0xFFDAA520, // goldenrod
    0xFF808080, // gray
    0xFF008000, // green
    0xFFADFF2F, // greenyellow
    0xFFF0FFF0, // honeydew
    0xFFFF69B4, // hotpink
    0xFFCD5C5C, // indianred
    0xFF4B0082, // indigo
    0xFFFFFFF0, // ivory
    0xFFF0E68C, // khaki
    0xFFE6E6FA, // lavender
    0xFFFFF0F5, // lavenderblush
    0xFF7CFC00, // lawngreen
    0xFFFFFACD, // lemonchiffon
    0xFFADD8E6, // lightblue
    0xFFF08080, // lightcoral
    0xFFE0FFFF, // lightcyan
    0xFFFAFAD2, // lightgoldenrodyellow
    0xFF90EE90, // lightgreen
    0xFFD3D3D3, // lightgrey
    0xFFFFB6C1, // lightpink
    0xFFFFA07A, // lightsalmon
    0xFF20B2AA, // lightseagreen
    0xFF87CEFA, // lightskyblue
    0xFF778899, // lightslategray
    0xFFB0C4DE, // lightsteelblue
    0xFFFFFFE0, // lightyellow
    0xFF00FF00, // lime
    0xFF32CD32, // limegreen
    0xFFFAF0E6, // linen
    0xFFFF00FF, // magenta
    0xFF800000, // maroon
    0xFF66CDAA, // mediumaquamarine
    0xFF0000CD, // mediumblue
    0xFFBA55D3, // mediumorchid
    0xFF9370DB, // mediumpurple
    0xFF3CB371, // mediumseagreen
    0xFF7B68EE, // mediumslateblue
    0xFF00FA9A, // mediumspringgreen
    0xFF48D1CC, // mediumturquoise
    0xFFC71585, // mediumvioletred
    0xFF191970, // midnightblue
    0xFFF5FFFA, // mintcream
    0xFFFFE4E1, // mistyrose
    0xFFFFE4B5, // moccasin
    0xFFFFDEAD, // navajowhite
    0xFF000080, // navy
    0xFFFDF5E6, // oldlace
    0xFF808000, // olive
    0xFF6B8E23, // olivedrab
    0xFFFFA500, // orange
    0xFFFF4500, // orangered
    0xFFDA70D6, // orchid
    0xFFEEE8AA, // palegoldenrod
    0xFF98FB98, // palegreen
    0xFFAFEEEE, // paleturquoise
    0xFFDB7093, // palevioletred
    0xFFFFEFD5, // papayawhip
    0xFFFFDAB9, // peachpuff
    0xFFCD853F, // peru
    0xFFFFC0CB, // pink
    0xFFDDA0DD, // plum
    0xFFB0E0E6, // powderblue
    0xFF800080, // purple
    0xFFFF0000, // red
    0xFFBC8F8F, // rosybrown
    0xFF4169E1, // royalblue
    0xFF8B4513, // saddlebrown
    0xFFFA8072, // salmon
    0xFFF4A460, // sandybrown
    0xFF2E8B57, // seagreen
    0xFFFFF5EE, // seashell
    0xFFA0522D, // sienna
    0xFFC0C0C0, // silver
    0xFF87CEEB, // skyblue
    0xFF6A5ACD, // slateblue
    0xFF708090, // slategray
    0xFFFFFAFA, // snow
    0xFF00FF7F, // springgreen
    0xFF4682B4, // steelblue
    0xFFD2B48C, // tan
    0xFF008080, // teal
    0xFFD8BFD8, // thistle
    0xFFFF6347, // tomato
    0xFF40E0D0, // turquoise
    0xFFEE82EE, // violet
    0xFFF5DEB3, // wheat
    0xFFFFFFFF, // white
    0xFFF5F5F5, // whitesmoke
    0xFFFFFF00, // yellow
    0xFF9ACD32, // yellowgreen
    // plus a few rasmol names/values
    0xFFAFD7FF, // bluetint
    0xFF2E8B57, // greenblue
    0xFF98FFB3, // greentint
    0xFF808080, // grey
    0xFFFFABBB, // pinktint
    0xFFFF4500, // redorange
    0xFFF6F675, // yellowtint
  };

  public final static short ATOMID_MAINCHAIN_MAX = 4;
  // some pdbfiles do not have sidechain atoms labeled properly
  // we call these MAINCHAIN_IMPOSTERS
  // the residue will accept the first atom with the proper name
  // others will get their atomid changed
  public final static short ATOMID_MAINCHAIN_IMPOSTERS = 4;

  public final static String[] predefinedAtomNames4 = {
    " N  ",  // 0
    " CA ",
    " C  ",
    " O  ", // 3
    " N  ", // imposter N
    " C  ", // imposter CA
    " C  ", // imposter C
    " O  ", // imposter O
    
    " P  ",
    " O1P",
    " O2P", // 10
    " O3P",
    " O5*",
    " O4*",
    " O3*",
    " O2*", // 15
    " C5*",
    " C4*",
    " C3*",
    " C2*",
    " C1*", // 20
    "1H5*",
    "2H5*",
    " H4*",
    " H3*",
    "1H2*", // 25
    "2H2*",
    " H1*",
    " H5T",
    " H3T", // 29
  };
  
  public final static short RESID_AMINO_MAX = 22;

  public final static String[] predefinedGroup3Names = {
    // taken from PDB spec
    "ALA", // 0
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
    "PRO",
    "SER",
    "THR",
    "TRP",
    "TYR",
    "VAL",
    "ASX", // 20 ASP/ASN ambiguous
    "GLX", // 21 GLU/GLN ambiguous
    "UNK", // 22 unknown -- 22

    // if you change these numbers you *must* update
    // the predefined sets in script.Token.java

    "  A", // 23
    " +A",
    "  G", // 25
    " +G",
    "  I", // 27
    " +I",
    "  C", // 29
    " +C",
    "  T", // 31
    " +T",
    "  U", // 33
    " +U",

    "1MA", // 35
    "AMO",
    "5MC",
    "OMC",
    "1MG",
    "2MG", // 40
    "M2G",
    "7MG",
    "G7M",
    "OMG",
    " YG", // 45
    "QUO",
    "H2U",
    "5MU",
    "4SU",
    "PSU", // 50
    
    "AMP",
    "ADP",
    "ATP",
    
    "GMP",
    "GDP", // 55
    "GTP",
    
    "IMP",
    "IDP",
    "ITP",
    
    "CMP", // 60
    "CDP",
    "CTP",
    
    "TMP",
    "TDP",
    "TTP", // 65
    
    "UMP",
    "UDP",
    "UTP", // 68
   
    
    // water && solvent
    "HOH", // 69
    "DOD", // 70
    // ions && solvent
    "PO4", // 71 phosphate ions
    "SO4", // 72 sulphate ions 
    
  };

  public final static int GRAPHIC_BACKBONE = 0;
  public final static int GRAPHIC_TRACE    = 1;
  public final static int GRAPHIC_AXES     = 2;
  public final static int GRAPHIC_BBOX     = 3;
  public final static int GRAPHIC_CARTOON  = 4;
  public final static int GRAPHIC_STRANDS  = 5;
  public final static int GRAPHIC_MAX = 6;

  public final static String[] graphicClassBases = {
    "Backbone", "Trace", "Axes", "Bbox", "Cartoon", "Strands"
  };
}
