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

package org.jmol.api;

import java.io.BufferedReader;

/****************************************************************
 * The ModelAdapter interface defines the API used by the JmolViewer to
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
public abstract class ModelAdapter {
  
  public final static byte ORDER_AROMATIC    = (byte)(1 << 2);
  public final static byte ORDER_HBOND       = (byte)(1 << 6);
  public final static byte ORDER_STEREO_NEAR = (byte)((1 << 3) | 1);
  public final static byte ORDER_STEREO_FAR  = (byte)((2 << 3) | 2);

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

  String modelAdapterName;
  public Logger logger;

  public ModelAdapter(String modelAdapterName, Logger logger) {
    this.modelAdapterName = modelAdapterName;
    this.logger = (logger == null ? new Logger() : logger);
  }

  public Object openBufferedReader(String name,
                                   BufferedReader bufferedReader) {
    return openBufferedReader(name, bufferedReader, null);
  }

  public Object openBufferedReader(String name,
                                   BufferedReader bufferedReader,
                                   Logger logger) {
    return null;
  }

  public void finish(Object clientFile) {}

  /**
   * returns the type of this model
   */
  public String getFileTypeName(Object clientFile) { return "unknown"; }

  /**
   * Some file formats contain a formal name of the molecule in the file.
   * If this method returns <code>null</code> then the JmolViewer will
   * automatically supply the file/URL name as a default.
   */
  public String getModelSetName(Object clientFile) { return null; }

  /**
   * We may need the file header.
   * This is currently only used for the script command 'show pdbheader'
   * Other than for pdb files, the client can return <code>null</code>
   */
  public String getModelFileHeader(Object clientFile) { return null; }

  /**
   * The number of molecular models in the file
   */

  public int getModelCount(Object clientFile) { return 1; }

  /**
   * The model number for each model
   */
  public int getModelNumber(Object clientFile, int modelIndex) { return 1; }

  /**
   * The properties for each model
   */
  public String getModelProperties(Object clientFile, int modelIndex) { return null; }

  /**
   * The number of atoms contained in the file.
   * Just return -1 if you don't know (or don't want to figure it out)
   */
  abstract public int getAtomCount(Object clientFile);

  /**
   * This method returns the parameters that define a crystal unitcell
   * the parameters are returned in a float[] in the following order
   * a, b, c, alpha, beta, gamma
   * a, b, c : angstroms
   * alpha, beta, gamma : degrees
   * if there is no unit cell data then return null
   */
  
  public boolean coordinatesAreFractional(Object clientFile) { return false; }

  public float[] getNotionalUnitcell(Object clientFile) { return null; }
  
  public float[] getPdbScaleMatrix(Object clientFile) { return null; }
  
  public float[] getPdbScaleTranslate(Object clientFile) { return null; }

  public String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
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
  public BondIterator getBondIterator(Object clientFile) { return null; }

  /**
   * Returns a StructureIterator or <code>null</code>
   */

  public StructureIterator getStructureIterator(Object clientFile) {
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
    public String getAtomName() { return null; }
    public int getAtomicCharge() { return 0; }
    abstract public float getX();
    abstract public float getY();
    abstract public float getZ();
    public float getVectorX() { return Float.NaN; }
    public float getVectorY() { return Float.NaN; }
    public float getVectorZ() { return Float.NaN; }
    public float getBfactor() { return Float.NaN; }
    public int getOccupancy() { return 100; }
    public boolean getIsHetero() { return false; }
    public int getAtomSerial() { return Integer.MIN_VALUE; }
    public char getChainID() { return (char)0; }
    public String getGroup3() { return null; }
    public int getSequenceNumber() { return Integer.MIN_VALUE; }
    public char getInsertionCode() { return (char)0; }
    public Object getClientAtomReference() { return null; }
  }

  /****************************************************************
   * BondIterator is used to enumerate all the bonds
   ****************************************************************/

  public abstract class BondIterator {
    public abstract boolean hasNext();
    public abstract Object getAtomUid1();
    public abstract Object getAtomUid2();
    public abstract int getOrder();
  }

  /****************************************************************
   * StructureIterator is used to enumerate Structures
   * Helix, Sheet, Turn
   ****************************************************************/

  public abstract class StructureIterator {
    public abstract boolean hasNext();
    public abstract String getStructureType();
    public abstract char getChainID();
    public abstract int getStartSequenceNumber();
    public abstract char getStartInsertionCode();
    public abstract int getEndSequenceNumber();
    public abstract char getEndInsertionCode();
  }

  /****************************************************************
   * Logger class
   ****************************************************************/

  public class Logger { // default logger will log to stdout
    public boolean isLogging() { return true; }
    public void log(String str1) {
      System.out.println(modelAdapterName + ":" + str1);
    }
    public void log(String str1, Object obj1) {
      System.out.println(modelAdapterName + ":" + str1 + ":" + obj1);
    }
    public void log(String str1, Object obj1, Object obj2) {
      System.out.println(modelAdapterName + ":" + str1 + ":"
                         + obj1 + ":" + obj2);
    }
  }
}
