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
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;

import java.awt.Color;
import javax.vecmath.Point3d;
import java.io.Reader;

// client-specific imports
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.SetOfMolecules;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.CDKException;

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;

import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.ChemObjectReader;
import java.io.IOException;

public class CdkJmolModelAdapter implements JmolModelAdapter {
  AtomColorer[] colorSchemes;

  public CdkJmolModelAdapter() {
    colorSchemes = new AtomColorer[JmolModelAdapter.COLORSCHEME_MAX];
    colorSchemes[JmolModelAdapter.COLORSCHEME_CPK] =
      new DefaultCdkAtomColors();
    colorSchemes[JmolModelAdapter.COLORSCHEME_CHARGE] =
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
  public boolean suppliesAtomColor() { return true; }
  
  /****************************************************************
   * the file related methods
   ****************************************************************/

  public Object openReader(JmolViewer viewer,
                           String name, Reader reader) {
    ChemFile chemFile = null;
    try {
      ChemObjectReader chemObjectReader = null;
      try {
        chemObjectReader = new ReaderFactory().createReader(reader);
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
    return chemFile;
  }

  public String getModelName(Object clientFile) {
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
    ChemSequence chemSequence = chemFile.getChemSequence(frameNumber);
    ChemModel[] chemModels = chemSequence.getChemModels();
    ChemModel chemModel = chemModels[0];
    SetOfMolecules setOfMolecules = chemModel.getSetOfMolecules();
    Molecule molecule = setOfMolecules.getMolecule(0);
    return molecule;
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

  public double getVanderwaalsRadius(Object clientAtom) {
    return ((Atom)clientAtom).getVanderwaalsRadius();
  }

  public double getCovalentRadius(Object clientAtom) {
    return ((Atom)clientAtom).getCovalentRadius();
  }

  public Point3d getPoint3d(Object clientAtom) {
    return ((Atom)clientAtom).getPoint3D();
  }

  public String getPdbAtomRecord(Object clientAtom){
    return null;
    //    return ((Atom)clientAtom).getPdbRecord();
  }

  public Color getAtomColor(Object clientAtom, int colorScheme) {
    if (colorScheme >= colorSchemes.length ||
        colorSchemes[colorScheme] == null)
      colorScheme = 0;
    return colorSchemes[colorScheme].getAtomColor((Atom)clientAtom);
  }

  class DefaultCdkAtomColors implements AtomColorer {

    /**
     * Returns the color for a certain atom type
     */
    public Color getAtomColor(org.openscience.cdk.Atom atom) {
      return (Color)atom.getProperty("org.openscience.jmol.color");
    }
  }
}
