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
import java.io.BufferedReader;

// client-specific imports
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.CrystalFrame;
import org.openscience.jmol.ChemFile;
import org.openscience.jmol.Atom;

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;


import org.openscience.jmol.io.ReaderFactory;
import org.openscience.jmol.io.ChemFileReader;
import java.io.IOException;

import java.util.Vector;

public class DeprecatedJmolModelAdapter implements JmolModelAdapter {
  AtomColorer[] colorSchemes;

  public DeprecatedJmolModelAdapter() {
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

  public Object openBufferedReader(JmolViewer viewer,
                                   String name, BufferedReader bufferedReader) {
    ChemFile chemFile = null;
    try {
      ChemFileReader chemFileReader = null;
      try {
        chemFileReader = ReaderFactory.createReader(viewer, bufferedReader);
      } catch (IOException ex) {
        return "Error determining input format: " + ex;
      }
      if (chemFileReader == null) {
        return "unrecognized input format";
      }
      chemFile = chemFileReader.read();
    } catch (IOException ex) {
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
    return ((ChemFile)clientFile).getNumberOfFrames();
  }


  /****************************************************************
   * The frame related methods
   ****************************************************************/

  private ChemFrame getChemFrame(Object clientFile, int frameNumber) {
    return ((ChemFile)clientFile).getFrame(frameNumber);
  }

  public int getAtomCount(Object clientFile, int frameNumber) {
    return getChemFrame(clientFile, frameNumber).getAtomCount();
  }

  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    ChemFrame chemFrame = getChemFrame(clientFile, frameNumber);
    return (chemFrame.getAtomCount() > 0 &&
            chemFrame.getJmolAtomAt(0).getPdbRecord() != null);
  }

  public JmolModelAdapter.AtomIterator
    getAtomIterator(Object clientFile, int frameNumber) {
    return new AtomIterator(getChemFrame(clientFile, frameNumber));
  }

  public JmolModelAdapter.BondIterator
    getCovalentBondIterator(Object clientFile, int frameNumber) {
    return new CovalentBondIterator(getChemFrame(clientFile, frameNumber));
  }

  public JmolModelAdapter.BondIterator
    getAssociationBondIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getVectorIterator(Object clientFile, int frameNumber) {
    return new VectorIterator(getChemFrame(clientFile, frameNumber));
  }

  public JmolModelAdapter.LineIterator
    getCrystalCellIterator(Object clientFile, int frameNumber) {
    return new CrystalCellIterator(getChemFrame(clientFile, frameNumber));
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    ChemFrame chemFrame;
    int atomCount, iatom;
    AtomIterator(ChemFrame chemFrame) {
      this.chemFrame = chemFrame;
      atomCount = chemFrame.getAtomCount();
      iatom = 0;
    }
    public boolean hasNext() {
      return iatom < atomCount;
    }
    public Object next() {
      return chemFrame.getAtomAt(iatom++);
    }
  }

  class CovalentBondIterator extends JmolModelAdapter.BondIterator {
    ChemFrame chemFrame;
    AtomIterator iterAtom;
    Atom atom;
    int ibond, bondedCount;
    Atom atom2;
    int order;

    CovalentBondIterator(ChemFrame chemFrame) {
      this.chemFrame = chemFrame;
      iterAtom = new AtomIterator(chemFrame);
      ibond = bondedCount = 0;
    }
    public boolean hasNext() {
      while (ibond == bondedCount) {
        if (!iterAtom.hasNext())
          return false;
        atom = (Atom)iterAtom.next();
        ibond = 0;
        bondedCount = atom.getBondedCount();
      }
      return true;
    }
    public void moveNext() {
      atom2 = atom.getBondedAtom(ibond);
      order = atom.getBondOrder(ibond);
      ++ibond;
    }
    public Object getAtom1() {
      return atom;
    }
    public Object getAtom2() {
      return atom2;
    }
    public int getOrder() {
      return order;
    }
  }

  class VectorIterator extends JmolModelAdapter.LineIterator {
    ChemFrame chemFrame;
    AtomIterator iterAtom;
    Atom atom;
    Point3d point1, point2;

    VectorIterator(ChemFrame chemFrame) {
      this.chemFrame = chemFrame;
      iterAtom = new AtomIterator(chemFrame);
    }
    public boolean hasNext() {
      while (atom == null ||
             !atom.hasVector()) {
        if (! iterAtom.hasNext())
          return false;
        atom = (Atom)iterAtom.next();
      }
      return true;
    }
    public void moveNext() {
      point1 = atom.getPoint3D();
      point2 = new Point3d(atom.getVector());
      point2.scaleAdd(2, point1);
      atom = null;
    }
    public Point3d getPoint1() {
      return point1;
    }
    public Point3d getPoint2() {
      return point2;
    }
  }


  class CrystalCellIterator extends JmolModelAdapter.LineIterator {
    boolean isCrystalFrame;
    CrystalFrame crystalFrame;
    Point3d point1, point2;
    Vector boxEdges;
    int ibox;
      
    CrystalCellIterator(ChemFrame chemFrame) {
      isCrystalFrame = chemFrame instanceof CrystalFrame;
      if (! isCrystalFrame)
        return;
      crystalFrame = (CrystalFrame)chemFrame;
      boxEdges = crystalFrame.getBoxEdges();
    }

    public boolean hasNext() {
      return (boxEdges != null && ibox < boxEdges.size());
    }

    public void moveNext() {
      point1 = (Point3d)boxEdges.elementAt(ibox++);
      point2 = (Point3d)boxEdges.elementAt(ibox++);
    }
    public Point3d getPoint1() {
      return point1;
    }
    public Point3d getPoint2() {
      return point2;
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
    return ((Atom)clientAtom).getPdbRecord();
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
    public Color getAtomColor(org.openscience.cdk.Atom a) {
        Object o = a.getProperty("org.openscience.jmol.color");
        if (o instanceof Color) {
            return (Color)o;
        } else {
          // no color set. return pink - easy to see
          return Color.pink;
        }
    }
  }

  ////////////////////////////////////////////////////////////////
  // notifications
  ////////////////////////////////////////////////////////////////
  public void notifyAtomDeleted(Object clientAtom) {
    // insert CDK code here
  }
}
