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
 * methods for getElementNumber(clientAtom) or getElementSymbol(clientAtom)
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
  public Object openBufferedReader(JmolViewer viewer, String name,
                                   BufferedReader bufferedReader) {
    return null;
  }

  public void finish(Object clientFile) {}

  /**
   * returns the type of this model
   public final static int MODEL_TYPE_OTHER = 0;
   public final static int MODEL_TYPE_PDB = 1;
   public final static int MODEL_TYPE_XYZ = 2;
   */
  public int getModelType(Object clientFile) { return 0; }

  /**
   * Some file formats contain a formal name of the molecule in the file.
   * If this method returns <code>null</code> then the JmolViewer will
   * automatically supply the file/URL name as a default.
   */
  public String getModelName(Object clientFile) { return null; }

  /**
   * We may need the file header.
   * This is currently only used for the script command 'show pdbheader'
   * Other than for pdb files, the client can return <code>null</code>
   */
  public String getModelHeader(Object clientFile) { return null; }

  /**
   * The number of atoms contained in the file.
   * Just return -1 if you don't know (or don't want to figure it out)
   */
  abstract public int getAtomCount(Object clientFile);
  /**
   * Whether or not this file has records in the .pdb format as specified
   * by the Protein Data Bank.
   * @see <a href='http://www.rcsb.org/pdb'>www.rcsb.org/pdb</a>
   */
  public boolean hasPdbRecords(Object clientFile) {
    return false;
  }

  /**
   * If hasPdbRecords(clientFile, frameNumber) returns true then structural
   * PDB records are returned here as an array of strings. 
   * The individual strings are exact HELIX, SHEET, and TURN recordsfrom the .pdb file.
   * @see #hasPdbRecords(Object clientFile)
   */
  public String[] getPdbStructureRecords(Object clientFile) {
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
  public float[] getNotionalUnitcell(Object clientFile) {
    return null;
  }

  public float[] getPdbScaleMatrix(Object clientFile) {
    return null;
  }

  public float[] getPdbScaleTranslate(Object clientFile) {
    return null;
  }

  /**
   * Returns an AtomIterator used to retrieve all the atoms in the file.
   * This method may not return <code>null</code>.
   * @see AtomIterator
   */
  abstract public AtomIterator getAtomIterator(Object clientFile);
  /**
   * Returns a BondIterator. If this method returns <code>null</code> and no
   * bonds are defined then the JmolViewer will automatically apply its
   * rebonding code to build bonds between atoms.
   * @see BondIterator
   */
  public BondIterator getBondIterator(Object clientFile) {
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
    public int getModelNumber(){ return 1; }
    abstract public Object getUniqueID();
    public int getElementNumber() { return -1; }
    public String getElementSymbol() { return null; }
    public String getAtomTypeName() { return null; }
    public int getAtomicCharge() { return 0; }
    abstract public float getX();
    abstract public float getY();
    abstract public float getZ();
    public float getVectorX() { return Float.NaN; }
    public float getVectorY() { return Float.NaN; }
    public float getVectorZ() { return Float.NaN; }
    public float getBfactor() { return Float.NaN; }
    public String getPdbAtomRecord() { return null; }
  }

  /****************************************************************
   * BondIterator is used to enumerate all the bonds
   ****************************************************************/
  public final static int ORDER_AROMATIC = 1 << 2;
  public final static int ORDER_HBOND = 1 << 6;

  public abstract class BondIterator {
    public abstract boolean hasNext();
    public abstract Object getAtomUid1();
    public abstract Object getAtomUid2();
    public abstract int getOrder();
  }
}
