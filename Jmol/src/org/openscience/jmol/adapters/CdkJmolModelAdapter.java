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

package org.openscience.jmol.adapters;

// these are standard and should be needed by all adapters
import org.openscience.jmol.viewer.*;

import java.awt.Color;
import javax.vecmath.Point3d;
import java.io.BufferedReader;

// client-specific imports
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.SetOfMolecules;
import org.openscience.cdk.tools.SetOfMoleculesManipulator;
import org.openscience.cdk.Crystal;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.CDKException;

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;

import org.openscience.cdk.tools.AtomTypeFactory;
import org.openscience.cdk.tools.SetOfMoleculesManipulator;
import org.openscience.cdk.tools.ChemFileManipulator;

import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.ChemObjectReader;
import java.io.IOException;

public class CdkJmolModelAdapter implements JmolModelAdapter {
  AtomColorer[] colorSchemes;

  public CdkJmolModelAdapter() {
    colorSchemes = new AtomColorer[JmolConstants.PALETTE_MAX];
    colorSchemes[JmolConstants.PALETTE_CPK] =
      new DefaultCdkAtomColors();
    colorSchemes[JmolConstants.PALETTE_CHARGE] =
      new PartialAtomicChargeColors();
  }

  /****************************************************************
   * the capabilities
   ****************************************************************/
  public boolean suppliesAtomicNumber() { return true; }
  public boolean suppliesAtomicSymbol() { return true; }
  public boolean suppliesAtomTypeName() { return true; }
  public boolean suppliesVanderwaalsRadius() { return true; }
  public boolean suppliesCovalentRadius() { return true; }
  public boolean suppliesAtomArgb() { return true; }
  
  /****************************************************************
   * the file related methods
   ****************************************************************/

  public Object openBufferedReader(JmolViewer viewer,
                                   String name, BufferedReader bufferedReader) {
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

  public int getModelType(Object clientFile) {
    if (hasPdbRecords(clientFile, 0))
      return JmolConstants.MODEL_TYPE_PDB;
    return JmolConstants.MODEL_TYPE_OTHER;
  }

  public String getModelName(Object clientFile) {
      if (clientFile instanceof ChemFile) {
          Object title = ((ChemFile)clientFile).getProperty(CDKConstants.TITLE);
          if (title != null) {
              System.out.println("Setting model name to title");
              return title.toString();
          } else {
              // try to recurse
              AtomContainer container = getAtomContainer((ChemFile)clientFile, 0);
              if (container != null) {
                  Object moleculeTitle = container.getProperty(CDKConstants.TITLE);
                  if (moleculeTitle != null) {
                      return moleculeTitle.toString();
                  }
              }
          }
      }
      return null;
  }

  public int getFrameCount(Object clientFile) {
    return ((ChemFile)clientFile).getChemSequenceCount();
  }


  /****************************************************************
   * The frame related methods
   ****************************************************************/

  private AtomContainer getAtomContainer(Object clientFile, int frameNumber) {
    ChemFile chemFile = (ChemFile)clientFile;
    ChemSequence chemSequence = chemFile.getChemSequence(0);
    ChemModel[] chemModels = chemSequence.getChemModels();
    ChemModel chemModel = chemModels[frameNumber];
    SetOfMolecules setOfMolecules = chemModel.getSetOfMolecules();
    Crystal crystal = chemModel.getCrystal();
    if (setOfMolecules != null) {
      AtomContainer molecule =
        SetOfMoleculesManipulator.getAllInOneContainer(setOfMolecules);
        return molecule;
    } else if (crystal != null) {
        return crystal;
    } else {
        System.out.println("Cannot display data in model");
        return null;
    }
  }

  public int getAtomCount(Object clientFile, int frameNumber) {
    return getAtomContainer(clientFile, frameNumber).getAtomCount();
  }

  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    AtomContainer atomContainer = getAtomContainer(clientFile, frameNumber);
    return (atomContainer.getAtomCount() > 0 &&
            getPdbAtomRecord(atomContainer.getAtomAt(0)) != null);
  }

  public JmolModelAdapter.AtomIterator
    getAtomIterator(Object clientFile, int frameNumber) {
    return new AtomIterator(getAtomContainer(clientFile, frameNumber));
  }

  public JmolModelAdapter.BondIterator
    getCovalentBondIterator(Object clientFile, int frameNumber) {
    return new CovalentBondIterator(getAtomContainer(clientFile, frameNumber));
  }

  public JmolModelAdapter.BondIterator
    getAssociationBondIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getVectorIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getCrystalCellIterator(Object clientFile, int frameNumber) {
    return null;
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    AtomContainer atomContainer;
    int atomCount, iatom;
    AtomIterator(AtomContainer atomContainer) {
      this.atomContainer = atomContainer;
      atomCount = atomContainer.getAtomCount();
      iatom = 0;
    }
    public boolean hasNext() {
      return iatom < atomCount;
    }
    public Object next() {
      return atomContainer.getAtomAt(iatom++);
    }
  }

  class CovalentBondIterator extends JmolModelAdapter.BondIterator {
    
    AtomContainer atomContainer;
    Bond[] bonds;
    int ibond;
    Bond bond;
    Atom[] bondedAtoms;

    CovalentBondIterator(AtomContainer atomContainer) {
      this.atomContainer = atomContainer;
      bonds = atomContainer.getBonds();
      ibond = 0;
    }
    public boolean hasNext() {
      return (ibond < bonds.length);
    }
    public void moveNext() {
      bond = bonds[ibond++];
      bondedAtoms = bond.getAtoms();
    }
    public Object getAtom1() {
      return (bondedAtoms.length == 2) ? bondedAtoms[0] : null;
    }
    public Object getAtom2() {
      return (bondedAtoms.length == 2) ? bondedAtoms[1] : null;
    }
    public int getOrder() {
      return (int)bond.getOrder();
    }
  }

  /****************************************************************
   * The atom related methods
   ****************************************************************/

  public int getAtomicNumber(Object clientAtom) {
    return ((Atom)clientAtom).getAtomicNumber();
  }
  
  public String getAtomicSymbol(Object clientAtom) {
    return ((Atom)clientAtom).getSymbol();
  }

  public String getAtomTypeName(Object clientAtom) {
    return ((Atom)clientAtom).getAtomTypeName();
  }

  public float getVanderwaalsRadius(Object clientAtom) {
    return (float)((Atom)clientAtom).getVanderwaalsRadius();
  }

  public float getCovalentRadius(Object clientAtom) {
    return (float)((Atom)clientAtom).getCovalentRadius();
  }

  public float getAtomX(Object clientAtom) {
    return (float)((Atom)clientAtom).getX3D();
  }
  public float getAtomY(Object clientAtom) {
    return (float)((Atom)clientAtom).getY3D();
  }
  public float getAtomZ(Object clientAtom) {
    return (float)((Atom)clientAtom).getZ3D();
  }

  public String getPdbAtomRecord(Object clientAtom){
    String pdbRecord = (String)((Atom)clientAtom).getProperty("pdb.record");
    return pdbRecord;
  }

  public String[] getPdbStructureRecords(Object clientFile, int frameNumber) {
    return null;
  }

  public int getAtomArgb(Object clientAtom, int colorScheme) {
    if (colorScheme >= colorSchemes.length ||
        colorSchemes[colorScheme] == null)
      return 0;
    Color color = colorSchemes[colorScheme].getAtomColor((Atom)clientAtom);
    return color == null ? 0 : color.getRGB();
  }

  class DefaultCdkAtomColors implements AtomColorer {

    /**
     * Returns the color for a certain atom type
     */
    public Color getAtomColor(org.openscience.cdk.Atom atom) {
      return (Color)atom.getProperty("org.openscience.jmol.color");
    }
  }

  ////////////////////////////////////////////////////////////////
  // notifications
  ////////////////////////////////////////////////////////////////
  public void notifyAtomDeleted(Object clientAtom) {
    System.out.println("CdkJmolModelAdapter has received notification" +
                       " that an atom has been deleted");
    // insert CDK code here
  }
}
