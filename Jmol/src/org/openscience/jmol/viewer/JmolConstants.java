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

  public final static short marMultipleBondSmallMaximum = 128;

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
  public final static byte BOND_COVALENT    = 3;
  public final static byte BOND_STEREO      = (1 << 2);
  public final static byte BOND_STEREO_NEAR = (1 << 2) | 1;
  public final static byte BOND_STEREO_FAR  = (1 << 2) | 2;
  public final static byte BOND_AROMATIC    = (1 << 3) | 1;
  public final static byte BOND_SULFUR      = (1 << 4) | 1;
  public final static byte BOND_HYDROGEN    = (1 << 5);
  public final static byte BOND_BACKBONE    = (1 << 6);

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
   * Default table of vanderwaalsRadii. Used when the client does not choose
   * to implement getVanderwaalsRadius(clientAtom).
   * Used for spacefill rendering of atoms.
   * Values taken from OpenBabel.
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   */
  public final static float[] vanderwaalsRadii = {
    1f,      //   0  Xx big enough to see
    1.2f,    //   1  H
    1.4f,    //   2  He
    1.82f,   //   3  Li
    1.7f,    //   4  Be
    2.08f,   //   5  B
    1.95f,   //   6  C
    1.85f,   //   7  N
    1.7f,    //   8  O
    1.73f,   //   9  F
    1.54f,   //  10  Ne
    2.27f,   //  11  Na
    1.73f,   //  12  Mg
    2.05f,   //  13  Al
    2.1f,    //  14  Si
    2.08f,   //  15  P
    2f,      //  16  S
    1.97f,   //  17  Cl
    1.88f,   //  18  Ar
    2.75f,   //  19  K
    1.973f,  //  20  Ca
    1.7f,    //  21  Sc
    1.7f,    //  22  Ti
    1.7f,    //  23  V
    1.7f,    //  24  Cr
    1.7f,    //  25  Mn
    1.7f,    //  26  Fe
    1.7f,    //  27  Co
    1.63f,   //  28  Ni
    1.4f,    //  29  Cu
    1.39f,   //  30  Zn
    1.87f,   //  31  Ga
    1.7f,    //  32  Ge
    1.85f,   //  33  As
    1.9f,    //  34  Se
    2.1f,    //  35  Br
    2.02f,   //  36  Kr
    1.7f,    //  37  Rb
    1.7f,    //  38  Sr
    1.7f,    //  39  Y
    1.7f,    //  40  Zr
    1.7f,    //  41  Nb
    1.7f,    //  42  Mo
    1.7f,    //  43  Tc
    1.7f,    //  44  Ru
    1.7f,    //  45  Rh
    1.63f,   //  46  Pd
    1.72f,   //  47  Ag
    1.58f,   //  48  Cd
    1.93f,   //  49  In
    2.17f,   //  50  Sn
    2.2f,    //  51  Sb
    2.06f,   //  52  Te
    2.15f,   //  53  I
    2.16f,   //  54  Xe
    1.7f,    //  55  Cs
    1.7f,    //  56  Ba
    1.7f,    //  57  La
    1.7f,    //  58  Ce
    1.7f,    //  59  Pr
    1.7f,    //  60  Nd
    1.7f,    //  61  Pm
    1.7f,    //  62  Sm
    1.7f,    //  63  Eu
    1.7f,    //  64  Gd
    1.7f,    //  65  Tb
    1.7f,    //  66  Dy
    1.7f,    //  67  Ho
    1.7f,    //  68  Er
    1.7f,    //  69  Tm
    1.7f,    //  70  Yb
    1.7f,    //  71  Lu
    1.7f,    //  72  Hf
    1.7f,    //  73  Ta
    1.7f,    //  74  W
    1.7f,    //  75  Re
    1.7f,    //  76  Os
    1.7f,    //  77  Ir
    1.72f,   //  78  Pt
    1.66f,   //  79  Au
    1.55f,   //  80  Hg
    1.96f,   //  81  Tl
    2.02f,   //  82  Pb
    1.7f,    //  83  Bi
    1.7f,    //  84  Po
    1.7f,    //  85  At
    1.7f,    //  86  Rn
    1.7f,    //  87  Fr
    1.7f,    //  88  Ra
    1.7f,    //  89  Ac
    1.7f,    //  90  Th
    1.7f,    //  91  Pa
    1.86f,   //  92  U
    1.7f,    //  93  Np
    1.7f,    //  94  Pu
    1.7f,    //  95  Am
    1.7f,    //  96  Cm
    1.7f,    //  97  Bk
    1.7f,    //  98  Cf
    1.7f,    //  99  Es
    1.7f,    // 100  Fm
    1.7f,    // 101  Md
    1.7f,    // 102  No
    1.7f,    // 103  Lr
    1.7f,    // 104  Rf
    1.7f,    // 105  Db
    1.7f,    // 106  Sg
    1.7f,    // 107  Bh
    1.7f,    // 108  Hs
    1.7f,    // 109  Mt
  };

  /**
   * Default table of covalentRadii. Used when the client does not choose
   * to implement getCovalentRadius(clientAtom).
   * Used for bonding atoms when the client does not supply bonds. 
   * Values taken from OpenBabel.
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   */
  public final static float[] covalentRadii = {
    2f,      //   0  Xx big enough to cause trouble
    0.23f,   //   1  H
    0.93f,   //   2  He
    0.68f,   //   3  Li
    0.35f,   //   4  Be
    0.83f,   //   5  B
    0.68f,   //   6  C
    0.68f,   //   7  N
    0.68f,   //   8  O
    0.64f,   //   9  F
    1.12f,   //  10  Ne
    0.97f,   //  11  Na
    1.1f,    //  12  Mg
    1.35f,   //  13  Al
    1.2f,    //  14  Si
    0.75f,   //  15  P
    1.02f,   //  16  S
    0.99f,   //  17  Cl
    1.57f,   //  18  Ar
    1.33f,   //  19  K
    0.99f,   //  20  Ca
    1.44f,   //  21  Sc
    1.47f,   //  22  Ti
    1.33f,   //  23  V
    1.35f,   //  24  Cr
    1.35f,   //  25  Mn
    1.34f,   //  26  Fe
    1.33f,   //  27  Co
    1.5f,    //  28  Ni
    1.52f,   //  29  Cu
    1.45f,   //  30  Zn
    1.22f,   //  31  Ga
    1.17f,   //  32  Ge
    1.21f,   //  33  As
    1.22f,   //  34  Se
    1.21f,   //  35  Br
    1.91f,   //  36  Kr
    1.47f,   //  37  Rb
    1.12f,   //  38  Sr
    1.78f,   //  39  Y
    1.56f,   //  40  Zr
    1.48f,   //  41  Nb
    1.47f,   //  42  Mo
    1.35f,   //  43  Tc
    1.4f,    //  44  Ru
    1.45f,   //  45  Rh
    1.5f,    //  46  Pd
    1.59f,   //  47  Ag
    1.69f,   //  48  Cd
    1.63f,   //  49  In
    1.46f,   //  50  Sn
    1.46f,   //  51  Sb
    1.47f,   //  52  Te
    1.4f,    //  53  I
    1.98f,   //  54  Xe
    1.67f,   //  55  Cs
    1.34f,   //  56  Ba
    1.87f,   //  57  La
    1.83f,   //  58  Ce
    1.82f,   //  59  Pr
    1.81f,   //  60  Nd
    1.8f,    //  61  Pm
    1.8f,    //  62  Sm
    1.99f,   //  63  Eu
    1.79f,   //  64  Gd
    1.76f,   //  65  Tb
    1.75f,   //  66  Dy
    1.74f,   //  67  Ho
    1.73f,   //  68  Er
    1.72f,   //  69  Tm
    1.94f,   //  70  Yb
    1.72f,   //  71  Lu
    1.57f,   //  72  Hf
    1.43f,   //  73  Ta
    1.37f,   //  74  W
    1.35f,   //  75  Re
    1.37f,   //  76  Os
    1.32f,   //  77  Ir
    1.5f,    //  78  Pt
    1.5f,    //  79  Au
    1.7f,    //  80  Hg
    1.55f,   //  81  Tl
    1.54f,   //  82  Pb
    1.54f,   //  83  Bi
    1.68f,   //  84  Po
    1.7f,    //  85  At
    2.4f,    //  86  Rn
    2f,      //  87  Fr
    1.9f,    //  88  Ra
    1.88f,   //  89  Ac
    1.79f,   //  90  Th
    1.61f,   //  91  Pa
    1.58f,   //  92  U
    1.55f,   //  93  Np
    1.53f,   //  94  Pu
    1.51f,   //  95  Am
    1.5f,    //  96  Cm
    1.5f,    //  97  Bk
    1.5f,    //  98  Cf
    1.5f,    //  99  Es
    1.5f,    // 100  Fm
    1.5f,    // 101  Md
    1.5f,    // 102  No
    1.5f,    // 103  Lr
    1.6f,    // 104  Rf
    1.6f,    // 105  Db
    1.6f,    // 106  Sg
    1.6f,    // 107  Bh
    1.6f,    // 108  Hs
    1.6f,    // 109  Mt
  };

  /**
   * Default table of CPK atom colors.
   * Used when the client does not implement getAtomColor(clientAtom, colorScheme)
   * I didn't know what color to define for many of the atoms, so I made them pink.
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
   * Used when the client does not implement
   *    getAtomColor(clientAtom, colorScheme)
   * I didn't know what color to define for many of the atoms,
   * so I made them pink.
   */
  public final static int[] argbsPdbStructure = {
    0xFFFFFFFF, // white       STRUCTURE_NONE
    0xFFEE00EE, // magenta2    STRUCTURE_HELIX
    0xFFFFFF00, // yellow      STRUCTURE_SHEET
    0xFF4876FF, // RoyalBlue1  STRUCTURE_TURN
  };

  public final static int[] argbsPdbAmino = {
    // note that these are the rasmol colors and names, not xwindows
    0xFFC8C8C8, // darkGrey   "ALA", /* 8.4% */
    0xFFEBEBEB, // lightGrey  "GLY", /* 8.3% */
    0xFF0F820F, // green      "LEU", /* 8.0% */
    0xFFFA9600, // orange     "SER", /* 7.5% */
    0xFF0F820F, // green      "VAL", /* 7.1% */
    0xFFFA9600, // orange     "THR", /* 6.4% */
    0xFF145AFF, // blue       "LYS", /* 5.8% */
    0xFFE60A0A, // brightRed  "ASP", /* 5.5% */
    0xFF0F820F, // green      "ILE", /* 5.2% */
    0xFF00DCDC, // cyan       "ASN", /* 4.9% */
    0xFFE60A0A, // brightRed  "GLU", /* 4.9% */
    0xFFDC9682, // mauve      "PRO", /* 4.4% */
    0xFF145AFF, // blue       "ARG", /* 3.8% */
    0xFF3232AA, // midBlue    "PHE", /* 3.7% */
    0xFF00DCDC, // cyan       "GLN", /* 3.5% */
    0xFF3232AA, // midBlue    "TYR", /* 3.5% */
    0xFF8282D2, // paleBlue   "HIS", /* 2.3% */
    0xFFE6E600, // yellow     "CYS", /* 2.0% */
    0xFFE6E600, // yellow     "MET", /* 1.8% */
    0xFFB45AB4, // purple     "TRP", /* 1.4% */

    0xFFBEA06E, // tan "ASX"
    0xFFBEA06E, // tan "GLX"
    0xFFBEA06E, // tan "PCA"
    0xFFBEA06E, // tan "HYP",
  };

  public final static int argbPdbShapelyBackbone = 0xFFB8B8B8;
  public final static int argbPdbShapelySpecial =  0xFF5E005E;
  public final static int argbPdbShapelyDefault =  0xFFFF00FF;
  public final static int[] argbsPdbShapely = {
    // these are rasmol values, not xwindows colors
    0xFF8CFF8C, // "ALA", /* 8.4% */
    0xFFFFFFFF, // "GLY", /* 8.3% */
    0xFF455E45, // "LEU", /* 8.0% */
    0xFFFF7042, // "SER", /* 7.5% */
    0xFFFF8CFF, // "VAL", /* 7.1% */
    0xFFB84C00, // "THR", /* 6.4% */
    0xFF4747B8, // "LYS", /* 5.8% */
    0xFFA00042, // "ASP", /* 5.5% */
    0xFF004C00, // "ILE", /* 5.2% */
    0xFFFF7C70, // "ASN", /* 4.9% */
    0xFF660000, // "GLU", /* 4.9% */
    0xFF525252, // "PRO", /* 4.4% */
    0xFF00007C, // "ARG", /* 3.8% */
    0xFF534C52, // "PHE", /* 3.7% */
    0xFFFF4C4C, // "GLN", /* 3.5% */
    0xFF8C704C, // "TYR", /* 3.5% */
    0xFF7070FF, // "HIS", /* 2.3% */
    0xFFFFFF70, // "CYS", /* 2.0% */
    0xFFB8A042, // "MET", /* 1.8% */
    0xFF4F4600, // "TRP", /* 1.4% */

    0xFFFF00FF, // "ASX"
    0xFFFF00FF, // "GLX"
    0xFFFF00FF, // "PCA"
    0xFFFF00FF, // "HYP",

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

}
