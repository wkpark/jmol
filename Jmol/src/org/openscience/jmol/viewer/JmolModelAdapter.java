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

import java.io.BufferedReader;

/****************************************************************
 * The JmolModelAdapter interface defines the API used by the JmolViewer to
 * read external files and fetch atom properties necessary for rendering.
 *
 * A client of the JmolViewer implements this interface on top of their existing
 * molecular model representation. The JmolViewer then requests information
 * from the implementation using this API. 
 *
 * Jmol will automatically calculate some atom properties if the client
 * is not capable or does not want to supply them.
 *
 * Note: If you are seeing pink atoms that have lots of bonds, then your methods
 * for getAtomicNumber(clientAtom) or getAtomicSymbol(clientAtom) are probably
 * returning stray values. Therefore, these atoms are getting mapped to
 * element 0 (Xx), which has color pink and a relatively large covalent bonding
 * radius. 
 * @see JmolViewer
 ****************************************************************/
public interface JmolModelAdapter {
  

  /****************************************************************
   * the capabilities
   ****************************************************************/
  /**
   * Whether or not this client wants to implement getAtomicNumber(clientAtom)
   * If not, then the JmolViewer will look up the atomicNumber using
   * getAtomicSymbol(clientAtom).
   * A client which implements getAtomicNumber() may still choose to
   * return the value -1, thereby asking Jmol to map using getAtomicSymbol()
   * @see #getAtomicNumber(Object clientAtom)
   * @see #getAtomicSymbol(Object clientAtom)
   */
  public boolean suppliesAtomicNumber();

  /**
   * Whether or not this client wants to implement getAtomicSymbol(clientAtom)
   * If not, then the JmolViewere will look up the atomicSymbol using
   * getAtomicNumber(clientAtom)
   * A client which implements getAtomicSymbol() may still choose to
   * return the value null, thereby asking Jmol to map to the appropriate
   * atomic symbol by using getAtomicNumber()
   * The default atomic symbol table is included in this file for user reference
   * Note that either getAtomicNumber(clientAtom) or getAtomicSymbol(clientAtom)
   * must be implemented.
   * @see #getAtomicSymbol(Object clientAtom)
   * @see #getAtomicNumber(Object clientAtom)
   */
  public boolean suppliesAtomicSymbol();

  /**
   * Whether or not this client wants to implement getAtomTypeName(clientAtom)
   * If suppliesAtomTypeName() returns false or getAtomTypeName returns nul
   * then the atomicSymbol is substituted.
   */
  public boolean suppliesAtomTypeName();

  /**
   * Whether or not this client implements getVanderwaalsRadius(clientAtom)
   * If not, then the atomic number is used to look up vanderwaals Radius values
   * in a default table. Default values are taken from OpenBabel.
   * The default vanderwaals radius table is included in this file for reference.
   * @see #getVanderwaalsRadius(Object clientAtom)
   */
  public boolean suppliesVanderwaalsRadius();

  /**
   * Whether or not this client implements getCovalentRadius(clientAtom)
   * If not, then the atomic number is used to look up covalent bonding radius
   * values in a default table. Default values are taken from OpenBabel.
   * The default covalent radius table is included in this file for reference.
   * @see #getCovalentRadius(Object clientAtom)
   */
  public boolean suppliesCovalentRadius();

  /**
   * Whether or not this client implements getAtomArgb(clientAtom, colorScheme)
   * If not, then the atomic number is used to look up colors in a
   * default table.
   * The default atom colors table is included in this file for reference.
   * @see #getAtomArgb(Object clientAtom, int colorScheme)
   */
  public boolean suppliesAtomArgb();


  /*****************************************************************
   * file related
   ****************************************************************/
  /**
   * Given the BufferedReader, return an object which represents the file
   * contents. The parameter <code>name</code> is assumed to be the
   * file name or URL which is the source of reader. Note that this 'file'
   * may have been automatically decompressed. Also note that the name
   * may be 'String', representing a string constant. Therefore, few
   * assumptions should be made about the <code>name</code> parameter.
   *
   * The return value is an object which represents a <code>clientFile</code>.
   * This <code>clientFile</code> will be passed back in to other methods.
   * If the return value is <code>instanceof String</code> then it is considered
   * an error condition and the returned String is the error message
   */
  public Object openBufferedReader(JmolViewer viewer, String name,
                                   BufferedReader bufferedReader);

  /**
   * The number of frames in this file. Used for animations, etc.
   */
  public int getFrameCount(Object clientFile);

  /**
   * Some file formats contain a formal name of the molecule in the file.
   * If this method returns <code>null</code> then the JmolViewer will
   * automatically supply the file/URL name as a default.
   */
  public String getModelName(Object clientFile);

  /****************************************************************
   * frame related
   ****************************************************************/
  /**
   * The number of atoms contained in the specified frame.
   */
  public int getAtomCount(Object clientFile, int frameNumber);
  /**
   * Whether or not this frame has records in the .pdb format as specified
   * by the Protein Data Bank.
   * @see <a href='http://www.rcsb.org/pdb'>www.rcsb.org/pdb</a>
   */
  public boolean hasPdbRecords(Object clientFile, int frameNumber);
  /**
   * Returns an AtomIterator used to retrieve all the atoms in the file.
   * This method may not return <code>null</code>.
   * @see AtomIterator
   */
  public AtomIterator getAtomIterator(Object clientFile, int frameNumber);
  /**
   * Returns a BondIterator. If this method returns <code>null</code> and no
   * bonds are defined then the JmolViewer will automatically apply its
   * rebonding code to build covalent bonds between atoms.
   * @see BondIterator
   */
  public BondIterator getCovalentBondIterator(Object clientFile,
                                              int frameNumber);
  /**
   * This method is used to return associations between atoms. Associations are
   * non-covalent bonds between atoms.
   * This method is provided for convenience of implementation. The client may
   * choose to return all associatoins as part of getCovalentBondIterator
   * @see BondIterator
   */
  public BondIterator getAssociationBondIterator(Object clientFile,
                                                 int frameNumber);
  /**
   * This method returns all vectors which are part of the molecule file.
   * Vectors are directional and have an arrowhead.
   * @see LineIterator
   */
  public LineIterator getVectorIterator(Object clientFile, int frameNumber);

  /**
   * This method returns the list of lines which define the crystal cell.
   * The first three lines returned are rendered as vectors.
   * The center of the crystal cell as defined by these lines is assumed
   * to be the default center of rotation for the rendered molecule.
   * @see LineIterator
   */
  public LineIterator getCrystalCellIterator(Object clientFile,
                                             int frameNumber);


  /****************************************************************
   * AtomIterator is used to enumerate all the <code>clientAtom</code>
   * objects in a specified frame. 
   * Note that Java 1.1 does not have java.util.Iterator
   * so we will define our own AtomIterator
   ****************************************************************/
  public abstract class AtomIterator {
    public abstract boolean hasNext();
    public abstract Object next();
  }

  /****************************************************************
   * BondIterator is used to enumerate all the bonds
   ****************************************************************/
  public abstract class BondIterator {
    public abstract boolean hasNext();
    /**
     * Note that the moveNext() method does not return a value
     */
    public abstract void moveNext();
    /**
     * returns the first atom of a bond
     */
    public abstract Object getAtom1();
    /**
     * returns the second atom of a bond
     */
    public abstract Object getAtom2();
    /**
     * returns the order of the bond. Bond order can be 1, 2, 3, AROMATIC,
     * BACKBONE, or HYDROGEN
     */
    public abstract int getOrder();
  }

  /**
   * returns two endpoints which represent the endpoints of a line. For vectors,
   * the arrowhead is at Point2
   */
  public abstract class LineIterator {
    public abstract boolean hasNext();
    /**
     * Note that the next() method does not return a value
     */
    public abstract void moveNext();
    /**
     * The start of the line or vector
     */
    public abstract float getPoint1X();
    public abstract float getPoint1Y();
    public abstract float getPoint1Z();
    /**
     * The end of the line or vector. Note that a 'vector' is not really a 'vector'.
     * It is just a line with an arrowhead. Therefore, Point2 is an absolute
     * position in 3d space
     */
    public abstract float getPoint2X();
    public abstract float getPoint2Y();
    public abstract float getPoint2Z();
  }
  
  /**
   * The CPK color scheme
   */
  public final static int COLORSCHEME_CPK = 0;
  /**
   * A color scheme based upon atomic charge
   */
  public final static int COLORSCHEME_CHARGE = 1;
  /**
   * coloring based upon pdb structure (none, helix, sheet, turn)
   */
  public final static int COLORSCHEME_PDB_STRUCTURE = 2;
  /**
   * coloring based upon pdb amino type
   */
  public final static int COLORSCHEME_PDB_AMINO = 3;
  /**
   * coloring based upon shapely colors
   */
  public final static int COLORSCHEME_PDB_SHAPELY = 4;
  /**
   * each chain gets a different color
   */
  public final static int COLORSCHEME_PDB_CHAIN = 5;

  /**
   * The number of color schemes, zero based
   */
  public final static int COLORSCHEME_MAX = 6;

  /**
   * Returns the atomicNumber of the clientAtom previously returned by
   * an AtomIterator.
   *
   * If suppliesAtomicNumber() returns false or if getAtomicNumber(clientAtom)
   * returns -1 then the JmolViewer will automatically look up the atomicNumber
   * using the String returned by getAtomicSymbol(clientAtom)
   *
   * Note that for a given molecule, either getAtomicNumber(clientAtom) or
   * getAtomicSymbol(clientAtom) must return a value.
   * @see #suppliesAtomicNumber()
   * @see #getAtomIterator(Object clientFile, int frameNumber)
   * @see #getAtomicSymbol(Object clientAtom)
   */
  public int getAtomicNumber(Object clientAtom);

  /**
   * Returns the atomicSymbol of the clientAtom previously returned by
   * an AtomIterator.
   *
   * If suppliesAtomicSymbol() returns false or if getAtomicSymbol(clientAtom)
   * returns null then the JmolViewer will automatically look up the atomicSymbol
   * using the getAtomicNumber(clientAtom)
   *
   * Note that for a given molecule, either getAtomicNumber(clientAtom) or
   * getAtomicSymbol(clientAtom) must return a value.
   */
  public String getAtomicSymbol(Object clientAtom);

  /**
   * This method allows one to return an atom type name (such as alpha carbon)
   * as separate from the atomicSymbol
   *
   * If getAtomTypeName returns null then the JmolViewer will substitute the
   * atomicSymbol
   */
  public String getAtomTypeName(Object clientAtom);

  /**
   * The vanderwaalsRadius is used for spacefill rendering. 
   *
   * If suppliesVanderwallsRadius() returns false or getVanderwaalsRadius(clientAtom)
   * returns 0 then the JmolViewer will lookup the value in its own table.
   * The table of values is taken from OpenBabel.
   * @see #suppliesVanderwaalsRadius()
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   * @see #vanderwaalsRadii
   */
  public float getVanderwaalsRadius(Object clientAtom);

  /**
   * The covalentRadius is used for automatically calculating bonds between
   * atoms when no bonds are specified by the client package. 
   *
   * If suppliesCovalentRadius() returns false or getCovalentRadius(clientAtom)
   * returns 0 then the JmolViewer will lookup the value in its own table.
   * The table of values is taken from OpenBabel.
   * @see #suppliesCovalentRadius()
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   * @see #covalentRadii
   */
  public float getCovalentRadius(Object clientAtom);

  /**
   * Returns the coordinates of the atom.
   * Coordinates are absolute values in Angstroms.
   */
  public float getAtomX(Object clientAtom);
  public float getAtomY(Object clientAtom);
  public float getAtomZ(Object clientAtom);

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then individual
   * PDB records for individual <code>clientAtom</code>s are returned here.
   * The returned String is the exact string of the ATOM or HETATM from the .pdb
   * file.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  public String getPdbAtomRecord(Object clientAtom);
  /**
   * If suppliesAtomArgb() returns false or if
   * getAtomArgb(clientAtom, colorScheme) returns 0 then the atom color
   * is looked up in internal tables maintained by JmolViewer
   * @see #suppliesAtomArgb()
   * @see #atomColors
   */
  public int getAtomArgb(Object clientAtom, int colorScheme);

  /**
   * This method gets called when the user deletes an atom.
   * In some cases, the client may want to update its own data structures.
   */
  public void notifyAtomDeleted(Object clientAtom);

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then structural
   * PDB records are returned here as an array of strings. 
   * The individual strings are exact HELIX, SHEET, and TURN recordsfrom the .pdb file.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  public String[] getPdbStructureRecords(Object clientFile, int frameNumber);

  /*
   ****************************************************************
   * these tables are here so that a client can have easy access to them
   ****************************************************************/
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
    0xFFE0E0E0, // grey88          H   1
    //    0xFFF0FFFF, // azure           H   1
    //    0xFFF0F8FF, // AliceBlue    H   1
    0xFFFFC0CB, // pink           He   2
    0xFFB22222, // firebrick      Li   3
    0xFF228B22, // ForestGreen    Be   4
    0xFF00FF00, // green           B   5
    //    0xFF708090, // SlateGray       C   6
    0xFF404040, // grey25          C   6
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
   * as currently implemented, this array must be of length 16
   */

  public final static int[] argbsPdbChain = {
    // xwindows colors
    // ' '->0 'A'->1, 'B'->2
    0xFF6B8E23, // OliveDrab
    0xFFFF0000, // red
    0xFF00FF00, // lime
    0xFF0000FF, // blue
    0xFFFFFF00, // yellow
    0xFFFF00FF, // magenta
    0xFF00FFFF, // cyan
    0xFFFFA500, // orange
    0xFF87CEEB, // SkyBlue
    0xFF8B0000, // red4
    0xFF006400, // DarkGreen
    0xFF000080, // NavyBlue
    0xFFA020F0, // purple
    0xFF40E0D0, // turquoise
    0xFFFA8072, // salmon
    0xFF00FF7F, // SpringGreen
  };
}
