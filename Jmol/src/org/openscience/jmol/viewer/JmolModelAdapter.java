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

import javax.vecmath.Point3d;
import java.io.BufferedReader;
import java.awt.Color;
import java.util.Iterator;

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
   * Whether or not this client implements getAtomColor(clientAtom, colorScheme)
   * If not, then the atomic number is used to look up colors in a
   * default table.
   * The default atom colors table is included in this file for reference.
   * @see #getAtomColor(Object clientAtom, int colorScheme)
   */
  public boolean suppliesAtomColor();


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
    public abstract Point3d getPoint1();
    /**
     * The end of the line or vector. Note that a 'vector' is not really a 'vector'.
     * It is just a line with an arrowhead. Therefore, Point2 is an absolute
     * position in 3d space
     */
    public abstract Point3d getPoint2();
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
   * The number of color schemes, zero based
   */
  public final static int COLORSCHEME_MAX = 2;

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
  public double getVanderwaalsRadius(Object clientAtom);

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
  public double getCovalentRadius(Object clientAtom);

  /**
   * Returns the coordinates of the atom in javax.vecmath.Point3d format.
   * Coordinates are absolute values in Angstroms.
   */
  public Point3d getPoint3d(Object clientAtom);
  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then individual
   * PDB records for individual <code>clientAtom</code>s are returned here.
   * The returned String is the exact string of the ATOM or HETATM from the .pdb
   * file.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  public String getPdbAtomRecord(Object clientAtom);
  /**
   * If suppliesAtomColors() returns false or if
   * getAtomColor(clientAtom, colorScheme) returns null then the atom color
   * is looked up in internal tables maintained by JmolViewer
   * @see #suppliesAtomColor()
   * @see #atomColors
   */
  public Color getAtomColor(Object clientAtom, int colorScheme);

  /**
   * This method gets called when the user deletes an atom.
   * In some cases, the client may want to update its own data structures.
   */
  public void notifyAtomDeleted(Object clientAtom);

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

  public final static Color color190_190_190 = new Color(190, 190, 190);
  public final static Color color165_42_42 = new Color(165, 42, 42);
  public final static Color color255_165_0 = new Color(255, 165, 0);

  /**
   * Default table of CPK atom colors.
   * Used when the client does not implement getAtomColor(clientAtom, colorScheme)
   * I didn't know what color to define for many of the atoms, so I made them pink.
   */
  public Color[] atomColors = {
    Color.pink,             // new Color(255,20,147),  // Xx	0
    new Color(250,235,215), // H	1
    new Color(255,192,203), // He	2
    new Color(178,34,34),   // Li	3
    new Color(34,139,34),   // Be	4
    Color.green,            // B	5
    new Color(180,180,180), //    new Color(112,128,144), // C	6
    new Color(0,191,255),   // N	7
    Color.red,              // O	8
    new Color(218,165,32),  // F	9
    new Color(255,105,180), // Ne	10
    Color.blue,             // Na	11
    new Color(34,139,34),   // Mg	12
    color190_190_190,       // Al	13
    new Color(218,165,32),  // Si	14
    color255_165_0,         // P	15
    Color.yellow,           // S	16
    Color.green,            // Cl	17
    new Color(255,192,203), // Ar	18
    new Color(255,20,147),  // K	19
    new Color(128,128,128), // Ca	20
    color190_190_190,       // Sc	21
    color190_190_190,       // Ti	22
    color190_190_190,       // V	23
    color190_190_190,       // Cr	24
    color190_190_190,       // Mn	25
    color255_165_0,         // Fe	26
    color165_42_42,         // Co	27
    color165_42_42,         // Ni	28
    color165_42_42,         // Cu	29
    color165_42_42,         // Zn	30
    color165_42_42,         // Ga	31
    new Color(85,107,47),   // Ge	32
    new Color(253,245,230), // As	33
    new Color(152,251,152), // Se	34
    color165_42_42,         // Br	35
    new Color(50,205,50),   // Kr	36
    color165_42_42,         // Rb	37
    color190_190_190,       // Sr	38
    color190_190_190,       // Y	39
    color190_190_190,       // Zr	40
    color190_190_190,       // Nb	41
    new Color(255,127,80),  // Mo	42
    color190_190_190,       // Tc	43
    color190_190_190,       // Ru	44
    color190_190_190,       // Rh	45
    color190_190_190,       // Pd	46
    color190_190_190,       // Ag	47
    new Color(255,140,0),   // Cd	48
    color190_190_190,       // In	49
    color190_190_190,       // Sn	50
    color190_190_190,       // Sb	51
    color190_190_190,       // Te	52
    new Color(160,32,240),  // I	53
    new Color(255,105,180), // Xe	54
    color165_42_42,         // Cs	55
    color190_190_190,       // Ba	56
    color190_190_190,       // La	57
    Color.pink,             //  58  Ce
    Color.pink,             //  59  Pr
    Color.pink,             //  60  Nd
    Color.pink,             //  61  Pm
    Color.pink,             //  62  Sm
    Color.pink,             //  63  Eu
    Color.pink,             //  64  Gd
    Color.pink,             //  65  Tb
    Color.pink,             //  66  Dy
    Color.pink,             //  67  Ho
    Color.pink,             //  68  Er
    Color.pink,             //  69  Tm
    Color.pink,             //  70  Yb
    color190_190_190,       // Lu	71
    color190_190_190,       // Hf	72
    color190_190_190,       // Ta	73
    new Color(64,224,208),  // W	74
    color190_190_190,       // Re	75
    color190_190_190,       // Os	76
    color190_190_190,       // Ir	77
    color190_190_190,       // Pt	78
    new Color(255,215,0),   // Au	79
    color190_190_190,       // Hg	80
    color190_190_190,       // Tl	81
    color190_190_190,       // Pb	82
    new Color(255,181,197), // Bi	83
    Color.pink,             //  84  Po
    Color.pink,             //  85  At
    Color.pink,             //  86  Rn
    Color.pink,             //  87  Fr
    Color.pink,             //  88  Ra
    Color.pink,             //  89  Ac
    Color.pink,             //  90  Th
    Color.pink,             //  91  Pa
    Color.pink,             //  92  U
    Color.pink,             //  93  Np
    Color.pink,             //  94  Pu
    Color.pink,             //  95  Am
    Color.pink,             //  96  Cm
    Color.pink,             //  97  Bk
    Color.pink,             //  98  Cf
    Color.pink,             //  99  Es
    Color.pink,             // 100  Fm
    Color.pink,             // 101  Md
    Color.pink,             // 102  No
    Color.pink,             // 103  Lr
    Color.pink,             // 104  Rf
    Color.pink,             // 105  Db
    Color.pink,             // 106  Sg
    Color.pink,             // 107  Bh
    Color.pink,             // 108  Hs
    Color.pink,             // 109  Mt
  };
}
