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
public abstract class JmolModelAdapter {
  

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
  abstract public Object openBufferedReader(JmolViewer viewer, String name,
                                   BufferedReader bufferedReader);

  /**
   * returns the type of this model
   public final static int MODEL_TYPE_OTHER = 0;
   public final static int MODEL_TYPE_PDB = 1;
   public final static int MODEL_TYPE_XYZ = 2;
   */
  abstract public int getModelType(Object clientFile);

  /**
   * The number of frames in this file. Used for animations, etc.
   */
  public int getFrameCount(Object clientFile) {
    return 1;
  }

  /**
   * Some file formats contain a formal name of the molecule in the file.
   * If this method returns <code>null</code> then the JmolViewer will
   * automatically supply the file/URL name as a default.
   */
  public String getModelName(Object clientFile) {
    return null;
  }

  /**
   * We may need the file header.
   * This is currently only used for the script command 'show pdbheader'
   * Other than for pdb files, the client can return <code>null</code>
   */
  public String getModelHeader(Object clientFile) {
    return null;
  }

  /****************************************************************
   * frame related
   ****************************************************************/
  /**
   * The number of atoms contained in the specified frame.
   */
  abstract public int getAtomCount(Object clientFile, int frameNumber);
  /**
   * Whether or not this frame has records in the .pdb format as specified
   * by the Protein Data Bank.
   * @see <a href='http://www.rcsb.org/pdb'>www.rcsb.org/pdb</a>
   */
  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    return false;
  }
  /**
   * Returns an AtomIterator used to retrieve all the atoms in the file.
   * This method may not return <code>null</code>.
   * @see AtomIterator
   */
  abstract public AtomIterator getAtomIterator(Object clientFile,
                                               int frameNumber);
  /**
   * Returns a BondIterator. If this method returns <code>null</code> and no
   * bonds are defined then the JmolViewer will automatically apply its
   * rebonding code to build covalent bonds between atoms.
   * @see BondIterator
   */
  public BondIterator getCovalentBondIterator(Object clientFile,
                                              int frameNumber) {
    return null;
  }
  /**
   * This method is used to return associations between atoms. Associations are
   * non-covalent bonds between atoms.
   * This method is provided for convenience of implementation. The client may
   * choose to return all associatoins as part of getCovalentBondIterator
   * @see BondIterator
   */
  public BondIterator getAssociationBondIterator(Object clientFile,
                                                 int frameNumber) {
    return null;
  }
  /**
   * This method returns all vectors which are part of the molecule file.
   * Vectors are directional and have an arrowhead.
   * @see LineIterator
   */
  public LineIterator getVectorIterator(Object clientFile, int frameNumber) {
    return null;
  }

  /**
   * This method returns the parameters that define a crystal unitcell
   * the parameters are returned in a float[] in the following order
   * a, b, c, alpha, beta, gamma
   * a, b, c : angstroms
   * alpha, beta, gamma : degrees
   * if there is no unit cell data then return null
   */
  public float[] getNotionalUnitcell(Object clientFile, int frameNumber) {
    return null;
  }

  public float[] getPdbScaleMatrix(Object clientFile, int frameNumber) {
    return null;
  }

  public float[] getPdbScaleTranslate(Object clientFile, int frameNumber) {
    return null;
  }

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
  abstract public int getAtomicNumber(Object clientAtom);

  /**
   * Returns the atomic charge of the clientAtom previously returned by
   * an AtomIterator.
   *
   * The client must implement this ... although it can always return 0
   */
  abstract public int getAtomicCharge(Object clientAtom);

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
  abstract public String getAtomicSymbol(Object clientAtom);

  /**
   * This method allows one to return an atom type name (such as alpha carbon)
   * as separate from the atomicSymbol
   *
   * If getAtomTypeName returns null then the JmolViewer will substitute the
   * atomicSymbol
   */
  abstract public String getAtomTypeName(Object clientAtom);

  /*
   * Returns the coordinates of the atom.
   * Coordinates are absolute values in Angstroms.
   */
  abstract public float getAtomX(Object clientAtom);
  abstract public float getAtomY(Object clientAtom);
  abstract public float getAtomZ(Object clientAtom);

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then individual
   * PDB records for individual <code>clientAtom</code>s are returned here.
   * The returned String is the exact string of the ATOM or HETATM from the .pdb
   * file.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  abstract public String getPdbAtomRecord(Object clientAtom);

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then the model
   * number for individual <code>clientAtom</code>s are returned here.
   * This is necessary because the model number is not part of the pdbRecord.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  abstract public int getPdbModelNumber(Object clientAtom);

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then structural
   * PDB records are returned here as an array of strings. 
   * The individual strings are exact HELIX, SHEET, and TURN recordsfrom the .pdb file.
   * @see #hasPdbRecords(Object clientFile, int frameNumber)
   */
  abstract public String[] getPdbStructureRecords(Object clientFile, int frameNumber);

}
