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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.*;
import java.awt.Color;
import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/****************************************************************
 * Mcps stands for Model-Chain-Polymer-Shape
 ****************************************************************/
abstract class Mcps extends Shape {

  PdbFile pdbFile;

  Mcpsmodel[] mcpsmodels;

  final void initShape() {
    pdbFile = frame.pdbFile;
  }

  void setSize(int size, BitSet bsSelected) {
    short mad = (short) size;
    initialize();
    for (int m = mcpsmodels.length; --m >= 0; )
      mcpsmodels[m].setMad(mad, bsSelected);
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    byte palette = 0;
    short colix = 0;
    if ("colorScheme" == propertyName) {
      if (value == null)
        return;
      palette = viewer.getPalette((String)value);
    } else if ("color" == propertyName) {
      palette = JmolConstants.PALETTE_COLOR;
      colix = g3d.getColix(value);
    } else {
      return;
    }
    for (int m = mcpsmodels.length; --m >= 0; )
      mcpsmodels[m].setColix(palette, colix, bs);
  }

  abstract Mcpschain allocateMcpschain(Polymer polymer);

  void initialize() {
    if (mcpsmodels == null) {
      int modelCount = pdbFile == null ? 0 : pdbFile.getModelCount();
      PdbModel[] models = pdbFile.getModels();
      mcpsmodels = new Mcpsmodel[modelCount];
      for (int i = modelCount; --i >= 0; )
        mcpsmodels[i] = new Mcpsmodel(models[i]);
    }
  }

  int getMcpsmodelCount() {
    return mcpsmodels.length;
  }

  Mcpsmodel getMcpsmodel(int i) {
    return mcpsmodels[i];
  }

  class Mcpsmodel {
    Mcpschain[] mcpschains;
    int modelIndex;
    
    Mcpsmodel(PdbModel model) {
      mcpschains = new Mcpschain[model.getChainCount()];
      this.modelIndex = model.modelIndex;
      for (int i = mcpschains.length; --i >= 0; )
        mcpschains[i] = allocateMcpschain(model.getChain(i).getPolymer());
    }
    
    void setMad(short mad, BitSet bsSelected) {
      for (int i = mcpschains.length; --i >= 0; ) {
        Mcpschain chain = mcpschains[i];
        if (chain.polymerCount > 0)
          chain.setMad(mad, bsSelected);
      }
    }

    void setColix(byte palette, short colix, BitSet bsSelected) {
      for (int i = mcpschains.length; --i >= 0; ) {
        Mcpschain chain = mcpschains[i];
        if (chain.polymerCount > 0)
          chain.setColix(palette, colix, bsSelected);
      }
    }

    int getMcpschainCount() {
      return mcpschains.length;
    }

    Mcpschain getMcpschain(int i) {
      return mcpschains[i];
    }
  }

  abstract class Mcpschain {
    Polymer polymer;
    short madOn;
    short madHelixSheet;
    short madTurnRandom;
    short madDnaRna;

    int polymerCount;
    Group[] polymerGroups;
    short[] colixes;
    short[] mads;
    
    Point3f[] leadMidpoints;
    Vector3f[] wingVectors;

    Mcpschain(Polymer polymer, int madOn,
              int madHelixSheet, int madTurnRandom, int madDnaRna) {
      this.polymer = polymer;
      this.madOn = (short)madOn;
      this.madHelixSheet = (short)madHelixSheet;
      this.madTurnRandom = (short)madTurnRandom;
      this.madDnaRna = (short)madDnaRna;

      polymerCount = polymer == null ? 0 : polymer.getCount();
      if (polymerCount > 0) {
        colixes = new short[polymerCount];
        mads = new short[polymerCount + 1];
        polymerGroups = polymer.getGroups();

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
        switch (polymerGroups[groupIndex].getStructureType()) {
        case JmolConstants.SECONDARY_STRUCTURE_SHEET:
        case JmolConstants.SECONDARY_STRUCTURE_HELIX:
          return madHelixSheet;
        case JmolConstants.SECONDARY_STRUCTURE_DNA:
        case JmolConstants.SECONDARY_STRUCTURE_RNA:
          return madDnaRna;
        default:
          return madTurnRandom;
        }
      case -3: // trace temperature
        {
          if (! hasTemperatureRange)
            calcTemperatureRange();
          Atom atom = polymerGroups[groupIndex].getLeadAtom();
          int bfactor100 = atom.getBfactor100(); // scaled by 1000
          int scaled = bfactor100 - temperatureMin;
          if (range == 0)
            return (short)0;
          float percentile = scaled / floatRange;
          if (percentile < 0 || percentile > 1)
            System.out.println("Que ha ocurrido? " + percentile);
          return (short)((1750 * percentile) + 250);
        }
      case -4: // trace displacement
        {
          Atom atom = polymerGroups[groupIndex].getLeadAtom();
          return // double it ... we are returning a diameter
            (short)(2 * calcMeanPositionalDisplacement(atom.getBfactor100()));
        }
      }
      System.out.println("unrecognized Mcps.getSpecial(" +
                         mad + ")");
      return 0;
    }

    boolean hasTemperatureRange = false;
    int temperatureMin, temperatureMax;
    int range;
    float floatRange;

    void calcTemperatureRange() {
      temperatureMin = temperatureMax =
        polymerGroups[0].getLeadAtom().getBfactor100();
      for (int i = polymerCount; --i > 0; ) {
        int temperature =
          polymerGroups[i].getLeadAtom().getBfactor100();
        if (temperature < temperatureMin)
          temperatureMin = temperature;
        else if (temperature > temperatureMax)
          temperatureMax = temperature;
      }
      range = temperatureMax - temperatureMin;
      floatRange = range;
      System.out.println("temperature range=" + range);
      hasTemperatureRange = true;
    }

    void setMad(short mad, BitSet bsSelected) {
      int[] atomIndices = polymer.getLeadAtomIndices();
      for (int i = polymerCount; --i >= 0; ) {
        if (bsSelected.get(atomIndices[i]))
          mads[i] = mad >= 0 ? mad : getMadSpecial(mad, i);
      }
      if (polymerCount > 1)
        mads[polymerCount] = mads[polymerCount - 1];
    }

    void setColix(byte palette, short colix, BitSet bsSelected) {
      int[] atomIndices = polymer.getLeadAtomIndices();
      for (int i = polymerCount; --i >= 0; ) {
        int atomIndex = atomIndices[i];
        if (bsSelected.get(atomIndex))
          colixes[i] =
            palette > JmolConstants.PALETTE_CPK
            ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette)
            : colix;
      }
    }

    /****************************************************************
     * the distance that we are returning is the
     * mean positional displacement in milliAngstroms
     * see below
     ****************************************************************/
    private final static double eightPiSquared100 = 8 * Math.PI * Math.PI * 100;
    short calcMeanPositionalDisplacement(int bFactor100) {
      return (short)(Math.sqrt(bFactor100/eightPiSquared100) * 1000);
    }
  }
/****************************************************************
http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html

pdb-l: temperature factor; occupancy
Bernhard Rupp br@llnl.gov
Thu, 27 Mar 2003 08:01:29 -0800

* Previous message: pdb-l: temperature factor; occupancy
* Next message: pdb-l: Structural alignment?
* Messages sorted by: [ date ] [ thread ] [ subject ] [ author ]

Isotropic B is defined as 8*pi**2<u**2>.

Meaning: eight pi squared =79

so B=79*mean square displacement (from rest position) of the atom.

as u is in Angstrom, B must be in Angstrom squared.

example: B=79A**2

thus, u=sqrt([79/79]) = 1 A mean positional displacement for atom.


See also 

http://www-structure.llnl.gov/Xray/comp/comp_scat_fac.htm#Atomic

for more examples.

BR


> -----Original Message-----
> From: pdb-l-admin@sdsc.edu [mailto:pdb-l-admin@sdsc.edu] On 
> Behalf Of Philipp Heuser
> Sent: Thursday, March 27, 2003 6:05 AM
> To: pdb-l@sdsc.edu
> Subject: pdb-l: temperature factor; occupancy
> 
> 
> Hi all!
> 
> Does anyone know where to find proper definitions for the 
> temperature factors 
> and the values for occupancy?
> 
> Alright I do know, that the atoms with high temperature 
> factors are more 
> disordered than others, but what does a temperature factor of 
> a specific 
> value mean exactly.
> 
> 
> Thanks in advance!
> 
> Philipp
> 
> 
> -- 
> *************************************
> Philipp Heuser
> 
> CUBIC - Cologne University Bioinformatics Center
> Institute of Biochemistry       
> University of Cologne                    
> 
> Zuelpicher Str. 47                       
> D-50674 Cologne, GERMANY       
> 
> Phone :  Office +49-221/470-7427 
> Fax:     Office +49-221/470-5092
> *************************************
> 
> TO UNSUBSCRIBE OR CHANGE YOUR SUBSCRIPTION OPTIONS, please 
> see https://lists.sdsc.edu/mailman/listinfo.cgi/pdb-l . 
> 
****************************************************************/

}

