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
 * A client of the JmolViewer implements this interface on top of their
 * existing molecular model representation. The JmolViewer then requests
 * information from the implementation using this API. 
 *
 * Jmol will automatically calculate some atom properties if the client
 * is not capable or does not want to supply them.
 *
 * Note: If you are seeing pink atoms that have lots of bonds, then your
 * methods for getAtomicNumber(clientAtom) or getAtomicSymbol(clientAtom)
 * are probably returning stray values. Therefore, these atoms are getting
 * mapped to element 0 (Xx), which has color pink and a relatively large
 * covalent bonding radius. 
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
   * The default atomic symbol table is included in this file for user
   * reference
   * Note that either getAtomicNumber(clientAtom) or
   * getAtomicSymbol(clientAtom) must be implemented.
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
   * If not, then the atomic number is used to look up vanderwaals Radius
   * values in a default table. Default values are taken from OpenBabel.
   * The default vanderwaals radius table is included in this file for
   * reference.
   * @see #getVanderwaalsRadius(Object clientAtom)
   */
  public boolean suppliesVanderwaalsRadius();

  /**
   * Whether or not this client implements getBondingRadius(clientAtom)
   * If not, then the atomic number is used to look up covalent bonding radius
   * values in a default table. Default values are taken from OpenBabel.
   * The default covalent radius table is included in this file for reference.
   * @see #getBondingRadius(Object clientAtom)
   */
  public boolean suppliesBondingRadius();

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
   * If the return value is <code>instanceof String</code> then it is
   * considered an error condition and the returned String is the error
   * message. 
   */
  public Object openBufferedReader(JmolViewer viewer, String name,
                                   BufferedReader bufferedReader);

  /**
   * returns the type of this model
   */
  public int getModelType(Object clientFile);

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
  public final static int ORDER_AROMATIC = 1 << 2;
  public final static int ORDER_HBOND = 1 << 6;

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
     * returns the order of the bond. Bond order can be 1, 2, 3,
     * or a value from the extended bond definition table
     */
    public abstract int getOrder();
  }

  /**
   * returns two endpoints which represent the endpoints of a line.
   * For vectors, the arrowhead is at Point2
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
     * The end of the line or vector. Note that a 'vector' is not really
     * a 'vector'. It is just a line with an arrowhead. Therefore, Point2
     * is an absolute position in 3d space
     */
    public abstract float getPoint2X();
    public abstract float getPoint2Y();
    public abstract float getPoint2Z();
  }
  
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
   * Returns the atomic charge of the clientAtom previously returned by
   * an AtomIterator.
   *
   * The client must implement this ... although it can always return 0
   */
  public int getAtomicCharge(Object clientAtom);

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
   * If suppliesVanderwallsRadius() returns false or
   * getVanderwaalsRadiusMilliAngstroms(clientAtom)
   * returns 0 then the JmolViewer will lookup the value in its own table.
   * The table of values is taken from OpenBabel.
   * @see #suppliesVanderwaalsRadius()
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   * @see #vanderwaalsRadii
   */
  public int getVanderwaalsRadiusMilliAngstroms(Object clientAtom);

  /**
   * The bondingRadius is used for automatically calculating bonds between
   * atoms when no bonds are specified by the client package. 
   *
   * If suppliesBondingRadius() returns false
   * or getBondingRadiusMilliAngstroms(clientAtom)
   * returns 0 then the JmolViewer will lookup the value in its own table.
   * The table of values is taken from OpenBabel.
   * @see #suppliesBondingRadius()
   * @see <a href='http://openbabel.sourceforge.net'>openbabel.sourceforge.net</a>
   */
  public int getBondingRadiusMilliAngstroms(Object clientAtom);

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
   * If hasPdbRecords(clientFile, frameNumber) returns true then the model
   * number for individual <code>clientAtom</code>s are returned here.
   * This is necessary because the model number is not part of the pdbRecord.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  public int getPdbModelNumber(Object clientAtom);

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

}
