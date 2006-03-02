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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

/****************************************************************
 * Mps stands for Model-Chain-Polymer-Shape
 ****************************************************************/
abstract class Mps extends Shape {

  Mmset mmset;

  Mpsmodel[] mpsmodels;

  final void initShape() {
    mmset = frame.mmset;
  }

  void setSize(int size, BitSet bsSelected) {
    short mad = (short) size;
    initialize();
    for (int m = mpsmodels.length; --m >= 0; )
      mpsmodels[m].setMad(mad, bsSelected);
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    if ("color" == propertyName) {
      String palette = null;
      short colix = Graphics3D.getColix(value);
      if (colix == Graphics3D.UNRECOGNIZED)
        palette = (String)value;
      for (int m = mpsmodels.length; --m >= 0; )
        mpsmodels[m].setColix(colix, palette, bs);
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int m = mpsmodels.length; --m >= 0; )
        mpsmodels[m].setTranslucent(isTranslucent, bs);
    }
  }

  abstract Mpspolymer allocateMpspolymer(Polymer polymer);

  void initialize() {
    if (mpsmodels == null) {
      int modelCount = mmset == null ? 0 : mmset.getModelCount();
      Model[] models = mmset.getModels();
      mpsmodels = new Mpsmodel[modelCount];
      for (int i = modelCount; --i >= 0; )
        mpsmodels[i] = new Mpsmodel(models[i]);
    }
  }

  int getMpsmodelCount() {
    return mpsmodels.length;
  }

  Mpsmodel getMpsmodel(int i) {
    return mpsmodels[i];
  }

  void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
    for (int i = mpsmodels.length; --i >= 0; )
      mpsmodels[i].findNearestAtomIndex(xMouse, yMouse, closest);
  }

  void setModelVisibility() {
    for (int i = mpsmodels.length; --i >= 0; )
      mpsmodels[i].setModelVisibility();
  }

  class Mpsmodel {
    Mpspolymer[] mpspolymers;
    int modelIndex;
    int modelVisibilityFlags = 0;
    
    Mpsmodel(Model model) {
      mpspolymers = new Mpspolymer[model.getPolymerCount()];
      this.modelIndex = model.modelIndex;
      for (int i = mpspolymers.length; --i >= 0; )
        mpspolymers[i] = allocateMpspolymer(model.getPolymer(i));
    }
    
    void setMad(short mad, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        Mpspolymer polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setMad(mad, bsSelected);
      }
    }

    void setColix(short colix, String palette, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        Mpspolymer polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setColix(colix, palette, bsSelected);
      }
    }

    void setTranslucent(boolean isTranslucent, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        Mpspolymer polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setTranslucent(isTranslucent, bsSelected);
      }
    }

    int getMpspolymerCount() {
      return mpspolymers.length;
    }

    Mpspolymer getMpspolymer(int i) {
      return mpspolymers[i];
    }

    void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
      for (int i = mpspolymers.length; --i >= 0; )
        mpspolymers[i].findNearestAtomIndex(xMouse, yMouse, closest);
    }

    void setModelVisibility() {
      int displayModelIndex = viewer.getDisplayModelIndex();
      modelVisibilityFlags = 
        (displayModelIndex >= 0 && displayModelIndex != modelIndex
            ? 0 : myVisibilityFlag);
      for (int i = mpspolymers.length; --i >= 0; )
        mpspolymers[i].setModelVisibility();
    }
    
  }

  
  abstract class Mpspolymer {
    Polymer polymer;
    short madOn;
    short madHelixSheet;
    short madTurnRandom;
    short madDnaRna;

    int monomerCount;
    Monomer[] monomers;
    short[] colixes;
    short[] mads;
    
    Point3f[] leadMidpoints;
    Vector3f[] wingVectors;


    Mpspolymer(Polymer polymer, int madOn,
              int madHelixSheet, int madTurnRandom, int madDnaRna) {
      this.polymer = polymer;
      this.madOn = (short)madOn;
      this.madHelixSheet = (short)madHelixSheet;
      this.madTurnRandom = (short)madTurnRandom;
      this.madDnaRna = (short)madDnaRna;

      // FIXME
      // I don't think that polymer can ever be null for this thing
      // so stop checking for null and see if it explodes
      monomerCount = polymer == null ? 0 : polymer.monomerCount;
      if (monomerCount > 0) {
        colixes = new short[monomerCount];
        mads = new short[monomerCount + 1];
        monomers = polymer.monomers;

        leadMidpoints = polymer.getLeadMidpoints();
        wingVectors = polymer.getWingVectors();
      }
    }

    short getMadSpecial(short mad, int groupIndex) {
      switch (mad) {
      case -1: // trace on
        if (madOn >= 0)
          return madOn;
        if (madOn != -2) {
          System.out.println("not supported?");
          return 0;
        }
        // fall into;
      case -2: // trace structure
        switch (monomers[groupIndex].getProteinStructureType()) {
        case JmolConstants.PROTEIN_STRUCTURE_SHEET:
        case JmolConstants.PROTEIN_STRUCTURE_HELIX:
          return madHelixSheet;
        case JmolConstants.PROTEIN_STRUCTURE_DNA:
        case JmolConstants.PROTEIN_STRUCTURE_RNA:
          return madDnaRna;
        default:
          return madTurnRandom;
        }
      case -3: // trace temperature
        {
          if (! hasBfactorRange)
            calcBfactorRange();
          Atom atom = monomers[groupIndex].getLeadAtom();
          int bfactor100 = atom.getBfactor100(); // scaled by 1000
          int scaled = bfactor100 - bfactorMin;
          if (range == 0)
            return (short)0;
          float percentile = scaled / floatRange;
          if (percentile < 0 || percentile > 1)
            System.out.println("Que ha ocurrido? " + percentile);
          return (short)((1750 * percentile) + 250);
        }
      case -4: // trace displacement
        {
          Atom atom = monomers[groupIndex].getLeadAtom();
          return // double it ... we are returning a diameter
            (short)(2 * calcMeanPositionalDisplacement(atom.getBfactor100()));
        }
      }
      System.out.println("unrecognized Mps.getSpecial(" +
                         mad + ")");
      return 0;
    }

    boolean hasBfactorRange = false;
    int bfactorMin, bfactorMax;
    int range;
    float floatRange;

    void calcBfactorRange() {
      bfactorMin = bfactorMax =
        monomers[0].getLeadAtom().getBfactor100();
      for (int i = monomerCount; --i > 0; ) {
        int bfactor =
          monomers[i].getLeadAtom().getBfactor100();
        if (bfactor < bfactorMin)
          bfactorMin = bfactor;
        else if (bfactor > bfactorMax)
          bfactorMax = bfactor;
      }
      range = bfactorMax - bfactorMin;
      floatRange = range;
      System.out.println("bfactor range=" + range);
      hasBfactorRange = true;
    }

    void setMad(short mad, BitSet bsSelected) {
      int[] leadAtomIndices = polymer.getLeadAtomIndices();
      boolean isVisible = (mad != 0);
      for (int i = monomerCount; --i >= 0; ) {
        int leadAtomIndex = leadAtomIndices[i];
        if (bsSelected.get(leadAtomIndex)) { 
          mads[i] = mad >= 0 ? mad : getMadSpecial(mad, i);
          monomers[i].setShapeVisibility(myVisibilityFlag, isVisible);
          frame.atoms[leadAtomIndex].setShapeVisibility(myVisibilityFlag,isVisible);
        }
      }
      if (monomerCount > 1)
        mads[monomerCount] = mads[monomerCount - 1];
    }

    void setColix(short colix, String palette, BitSet bsSelected) {
      int[] atomIndices = polymer.getLeadAtomIndices();
      for (int i = monomerCount; --i >= 0; ) {
        int atomIndex = atomIndices[i];
        if (bsSelected.get(atomIndex))
          colixes[i] =
            ((colix != Graphics3D.UNRECOGNIZED) ? colix :
             viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette));
      }
    }

    void setTranslucent(boolean isTranslucent, BitSet bsSelected) {
      int[] atomIndices = polymer.getLeadAtomIndices();
      for (int i = monomerCount; --i >= 0; ) {
        int atomIndex = atomIndices[i];
        if (bsSelected.get(atomIndex))
          colixes[i] = Graphics3D.setTranslucent(colixes[i], isTranslucent);
      }
    }

    private final static double eightPiSquared100 = 8 * Math.PI * Math.PI * 100;
    /**
     * Calculates the mean positional displacement in milliAngstroms.
     * <p>
     * <a href='http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html'>
     * http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html
     * </a>
     * <code>
     * > -----Original Message-----
     * > From: pdb-l-admin@sdsc.edu [mailto:pdb-l-admin@sdsc.edu] On 
     * > Behalf Of Philipp Heuser
     * > Sent: Thursday, March 27, 2003 6:05 AM
     * > To: pdb-l@sdsc.edu
     * > Subject: pdb-l: temperature factor; occupancy
     * > 
     * > 
     * > Hi all!
     * > 
     * > Does anyone know where to find proper definitions for the 
     * > temperature factors 
     * > and the values for occupancy?
     * > 
     * > Alright I do know, that the atoms with high temperature 
     * > factors are more 
     * > disordered than others, but what does a temperature factor of 
     * > a specific 
     * > value mean exactly.
     * > 
     * > 
     * > Thanks in advance!
     * > 
     * > Philipp
     * > 
     * pdb-l: temperature factor; occupancy
     * Bernhard Rupp br@llnl.gov
     * Thu, 27 Mar 2003 08:01:29 -0800
     * 
     * * Previous message: pdb-l: temperature factor; occupancy
     * * Next message: pdb-l: Structural alignment?
     * * Messages sorted by: [ date ] [ thread ] [ subject ] [ author ]
     * 
     * Isotropic B is defined as 8*pi**2<u**2>.
     * 
     * Meaning: eight pi squared =79
     * 
     * so B=79*mean square displacement (from rest position) of the atom.
     * 
     * as u is in Angstrom, B must be in Angstrom squared.
     * 
     * example: B=79A**2
     * 
     * thus, u=sqrt([79/79]) = 1 A mean positional displacement for atom.
     * 
     * 
     * See also 
     * 
     * http://www-structure.llnl.gov/Xray/comp/comp_scat_fac.htm#Atomic
     * 
     * for more examples.
     * 
     * BR
     *</code>
     *
     * @param bFactor100
     * @return ?
     */
    short calcMeanPositionalDisplacement(int bFactor100) {
      return (short)(Math.sqrt(bFactor100/eightPiSquared100) * 1000);
    }

    void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
      polymer.findNearestAtomIndex(xMouse, yMouse, closest, mads);
    }

    void setModelVisibility() {
      if (wingVectors == null)
        return;
      boolean isNucleicPolymer = polymer instanceof NucleicPolymer;
      if (! isNucleicPolymer)
        return;
      for (int i = monomerCount; --i >= 0; ) {
        if (mads[i] <= 0) 
          continue;
        NucleicMonomer group = (NucleicMonomer) monomers[i];
        group.setModelVisibility();        
      }
    }
  }
}
