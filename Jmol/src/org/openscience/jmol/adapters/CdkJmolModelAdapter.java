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

import org.openscience.cdk.geometry.CrystalGeometryTools;

import org.openscience.cdk.tools.AtomTypeFactory;
import org.openscience.cdk.tools.SetOfMoleculesManipulator;
import org.openscience.cdk.tools.ChemFileManipulator;

import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.ChemObjectReader;
import java.io.IOException;
import java.util.Vector;

public class CdkJmolModelAdapter extends JmolModelAdapter {

  /****************************************************************
   * the file related methods
   ****************************************************************/

  public Object openBufferedReader(JmolViewer viewer, String name,
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
      // create 3D coordinates before returning the object
      CrystalGeometryTools.fractionalToCartesian(crystal);
      System.out.println(crystal.toString());
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

  public float[] getNotionalUnitcell(Object clientFile, int frameNumber) {
    AtomContainer container = getAtomContainer(clientFile, frameNumber);
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
    } else {
        System.err.println("Cannot return notional unit cell params: no Crystal found");
    }
    return null;
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    AtomContainer atomContainer;
    int atomCount, iatom;
    Atom atom;
    AtomIterator(AtomContainer atomContainer) {
      this.atomContainer = atomContainer;
      atomCount = atomContainer.getAtomCount();
      iatom = 0;
    }
    public boolean hasNext() {
      if (iatom >= atomCount)
        return false;
      atom = atomContainer.getAtomAt(iatom++);
      return true;
    }

    public Object getUniqueID() { return atom; }
    public int getAtomicNumber() { return atom.getAtomicNumber(); }
    public String getAtomicSymbol() { return atom.getSymbol(); }
    public float getX() { return (float)atom.getX3D(); }
    public float getY() { return (float)atom.getY3D(); }
    public float getZ() { return (float)atom.getZ3D(); }
    public String getPdbAtomRecord() {
      return (String)atom.getProperty("pdb.record");
    }
    public int getPdbModelNumber() { return 0; }

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

  public int getAtomicCharge(Object clientAtom) {
    return 0;
  }
  
  public String getAtomicSymbol(Object clientAtom) {
    return ((Atom)clientAtom).getSymbol();
  }

  public String getAtomTypeName(Object clientAtom) {
    return ((Atom)clientAtom).getAtomTypeName();
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

  public int getPdbModelNumber(Object clientAtom) {
    return 0;
  }

  public String[] getPdbStructureRecords(Object clientFile, int frameNumber) {
    ChemFile chemFile = (ChemFile)clientFile;
    ChemSequence chemSequence = chemFile.getChemSequence(0);
    ChemModel chemModel = chemSequence.getChemModel(frameNumber);
    Vector structureVector =
      (Vector)chemModel.getProperty("pdb.structure.records");
    if (structureVector == null)
      return null;
    String[] t = new String[structureVector.size()];
    structureVector.copyInto(t);
    return t;
  }

  class CrystalCellIterator extends JmolModelAdapter.LineIterator {
    Crystal crystal;
    Point3d point1, point2;
    int ibox;
      
    CrystalCellIterator(Crystal crystal) {
      this.crystal = crystal;
      ibox=0;
    }

    public boolean hasNext() {
      return (ibox < 3);
    }

    public void moveNext() {
      point1 = new Point3d(0.0, 0.0, 0.0);
      double[] axis = {0.0, 0.0, 0.0};
      if (ibox == 0) {
        axis = crystal.getA();
      } else if (ibox == 1) {
        axis = crystal.getB();
      } else if (ibox == 2) {
        axis = crystal.getC();
      }
      point2 = new Point3d(axis[0], axis[1], axis[2]);
      ibox++;
    }
    public float getPoint1X() {
      return (float)point1.x;
    }
    public float getPoint1Y() {
      return (float)point1.y;
    }
    public float getPoint1Z() {
      return (float)point1.z;
    }
    public float getPoint2X() {
      return (float)point2.x;
    }
    public float getPoint2Y() {
      return (float)point2.y;
    }
    public float getPoint2Z() {
      return (float)point2.z;
    }
  }
}
