/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.adapter.cdk;

// these are standard and should be needed by all adapters
import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;

// client-specific imports
import org.openscience.cdk.*;
import org.openscience.cdk.config.AtomTypeFactory;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.CrystalGeometryTools;
import org.openscience.cdk.tools.manipulator.*;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.ChemObjectReader;
import java.io.IOException;

public class CdkJmolAdapter extends JmolAdapter {

  public final static String ATOM_SET_INDEX = "org.jmol.adapter.cdk.ATOM_SET_INDEX";
    
  public CdkJmolAdapter(Logger logger) {
    super("CdkJmolAdapter", logger);
  }

  /* **************************************************************
   * the file related methods
   * **************************************************************/

  public Object openBufferedReader(String name,
                                   BufferedReader bufferedReader) {
    ChemFile chemFile = null;
    try {
      ChemObjectReader chemObjectReader = null;
      try {
        chemObjectReader = new ReaderFactory().createReader(bufferedReader);
      } catch (IOException ex) {
        return "Error determining input format: " + ex;
      }
      if (chemObjectReader == null) {
        return "unrecognized input format";
      }
      chemFile = (ChemFile)chemObjectReader.read(new ChemFile());
    } catch (CDKException ex) {
      return "Error reading input:" + ex;
    }
    if (chemFile == null)
      return "unknown error reading file";
    try {
      AtomTypeFactory factory = AtomTypeFactory.getInstance("jmol_atomtypes.txt");
      AtomContainer atomContainer = ChemFileManipulator.getAllInOneContainer(chemFile);
      Atom[] atoms = atomContainer.getAtoms();
      for (int i=0; i<atoms.length; i++) {
        try {
          factory.configure(atoms[i]);
        } catch (CDKException exception) {
          System.out.println("Could not configure atom: " + atoms[i]);
        }
      }
    } catch (ClassNotFoundException exception) {
      // could not configure atoms... what to do?
      System.err.println(exception.toString());
      exception.printStackTrace();
    } catch (IOException exception) {
      // could not configure atoms... what to do?
      System.err.println(exception.toString());
      exception.printStackTrace();
    }
    return chemFile;
  }

  public String getFileTypeName(Object clientFile) {
    AtomContainer atomContainer = getAtomContainer(clientFile);
    if ((atomContainer.getAtomCount() > 0 &&
         atomContainer.getAtomAt(0).getProperty("pdb.record") != null))
      return "pdb";
    return "other";
  }

  public String getAtomSetCollectionName(Object clientFile) {
    if (clientFile instanceof ChemFile) {
      Object title = ((ChemFile)clientFile).getProperty(CDKConstants.TITLE);
      if (title != null) {
        System.out.println("Setting model name to title");
        return title.toString();
      }
      // try to recurse
      AtomContainer container = getAtomContainer(clientFile);
      if (container != null) {
        Object moleculeTitle = container.getProperty(CDKConstants.TITLE);
        if (moleculeTitle != null) {
          return moleculeTitle.toString();
        }
      }
    }
    return null;
  }

  /* **************************************************************
   * The frame related methods
   * **************************************************************/

  public int getAtomSetCount(Object clientFile) {
    ChemFile chemFile = (ChemFile)clientFile;
    ChemSequence chemSequence = chemFile.getChemSequence(0);
    if (chemSequence != null) {
      return chemSequence.getChemModelCount();
    }
    return super.getAtomSetCount(clientFile);
  }
   
  private AtomContainer getAtomContainer(Object clientFile) {
    ChemFile chemFile = (ChemFile)clientFile;
    ChemSequence chemSequence = chemFile.getChemSequence(0);
    ChemModel[] chemModels = chemSequence.getChemModels();
    AtomContainer superMolecule = new AtomContainer();
    for (int i=0; i<chemModels.length; i++) {
        ChemModel chemModel = chemModels[i];
        ChemModelManipulator.setAtomProperties(chemModel, ATOM_SET_INDEX, new Integer(i));
        SetOfMolecules setOfMolecules = chemModel.getSetOfMolecules();
        Crystal crystal = chemModel.getCrystal();
        if (setOfMolecules != null) {
            superMolecule.add(SetOfMoleculesManipulator.getAllInOneContainer(setOfMolecules));
        } else if (crystal != null) {
            // create 3D coordinates before returning the object
            CrystalGeometryTools.fractionalToCartesian(crystal);
            superMolecule.add(crystal);
        } else {
            System.out.println("Cannot display data in model");
            return null;
        }
    }
    return superMolecule;
  }

  public int getEstimatedAtomCount(Object clientFile) {
    return getAtomContainer(clientFile).getAtomCount();
  }

  /*
    this needs to be handled through the StructureIterator

  String[] getPdbStructureRecords(Object clientFile) {
    ChemFile chemFile = (ChemFile)clientFile;
    ChemSequence chemSequence = chemFile.getChemSequence(0);
    ChemModel chemModel = chemSequence.getChemModel(0);
    Vector structureVector =
      (Vector)chemModel.getProperty("pdb.structure.records");
    if (structureVector == null)
      return null;
    String[] t = new String[structureVector.size()];
    structureVector.copyInto(t);
    return t;
  }
  */

  public float[] getNotionalUnitcell(Object clientFile) {
    AtomContainer container = getAtomContainer(clientFile);
    if (container instanceof Crystal) {
        Crystal crystal = (Crystal)container;
        double[] notional = CrystalGeometryTools.cartesianToNotional(
            crystal.getA(), crystal.getB(), crystal.getC()
        );
        float[] fNotional = new float[6];
        for (int i=0; i<6; i++) {
            fNotional[i] = (float)notional[i];
        }
        return fNotional;
    } // else: no crystal thus no unit cell info
    return null;
  }

  public String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
    Object value = ((Atom)clientAtom).getProperty(propertyName);
    return value == null ? null : "" + value;
  }

  public JmolAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator(getAtomContainer(clientFile));
  }

  public JmolAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator(getAtomContainer(clientFile));
  }

  /* ***************************************************************
   * the frame iterators
   * **************************************************************/
  class AtomIterator extends JmolAdapter.AtomIterator {
    AtomContainer atomContainer;
    int atomCount, iatom;
    Atom atom;
    AtomIterator(AtomContainer atomContainer) {
      this.atomContainer = atomContainer;
      atomCount = atomContainer.getAtomCount();
      iatom = 0;
    }
    public boolean hasNext() {
      if (iatom == atomCount)
        return false;
      atom = atomContainer.getAtomAt(iatom++);
      // System.out.println("CDK atom: " + atom);
      return true;
    }

    public Object getUniqueID() { return atom; }
    public int getElementNumber() { return atom.getAtomicNumber(); }
    public String getElementSymbol() { return atom.getSymbol(); }
    public float getX() { return (float)atom.getX3d(); }
    public float getY() { return (float)atom.getY3d(); }
    public float getZ() { return (float)atom.getZ3d(); }
    public String getPdbAtomRecord() {
      return (String)atom.getProperty("pdb.record");
    }
    public String getAtomName() {
        return atom.getAtomTypeName();
    }
    public int getAtomSetIndex() {
        Integer indexInt = (Integer)atom.getProperty(ATOM_SET_INDEX);
        System.out.println("indexInt: " + indexInt);
        if (indexInt != null) {
            return indexInt.intValue();
        }
        return super.getAtomSetIndex();
    }
    public char getChainID() {
        String chainID = (String)atom.getProperty("pdb.chainID");
        // System.out.println("chainID: " + chainID);
        if (chainID != null && chainID.length() > 0) {
            return chainID.charAt(0);
        }
        return super.getChainID();
    }
    public String getGroup3() {
        String resName = (String)atom.getProperty("pdb.resName");
        // System.out.println("resName: " + resName);
        if (resName != null && resName.length() > 0) {
            return resName.trim();
        }
        return super.getGroup3();
    }
    public int getSequenceNumber() {
        //String sequence = (String)atom.getProperty("pdb.resSeq");
        // System.out.println("sequence: " + sequence);
        String sequenceNumber = (String)atom.getProperty("pdb.resSeq");
        if (sequenceNumber != null && sequenceNumber.length() > 0) {
            try {
                int sequenceInt = Integer.parseInt(sequenceNumber);
                // System.out.println("     int: " + sequenceInt);
                return sequenceInt;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return super.getSequenceNumber();
    }
    public char getInsertionCode() {
        String iCode = (String)atom.getProperty("pdb.iCode");
        // System.out.println("iCode: " + iCode);
        if (iCode != null && iCode.length() > 0) {
            return iCode.charAt(0);
        }
        return super.getInsertionCode();
    }
    public Object getClientAtomReference() {
      return atom;
    }
  }

  class BondIterator extends JmolAdapter.BondIterator {
    
    AtomContainer atomContainer;
    Bond[] bonds;
    int ibond;
    Bond bond;
    Atom[] bondedAtoms;

    BondIterator(AtomContainer atomContainer) {
      this.atomContainer = atomContainer;
      bonds = atomContainer.getBonds();
      ibond = 0;
    }
    public boolean hasNext() {
      if (ibond == bonds.length)
        return false;
      bond = bonds[ibond++];
      bondedAtoms = bond.getAtoms();
      return true;
    }
    public Object getAtomUniqueID1() {
      return (bondedAtoms.length == 2) ? bondedAtoms[0] : null;
    }
    public Object getAtomUniqueID2() {
      return (bondedAtoms.length == 2) ? bondedAtoms[1] : null;
    }
    public int getEncodedOrder() {
      return (int)bond.getOrder();
    }
  }

}
