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
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.pdb.*;
import java.awt.Color;
import java.util.BitSet;

/****************************************************************
 * Mcps stands for Model-Chain-Polymer-Shape
 ****************************************************************/
abstract public class Mcps extends Shape {

  PdbFile pdbFile;

  Model[] models;

  final public void initShape() {
    pdbFile = frame.pdbFile;
  }

  public void setSize(int size, BitSet bsSelected) {
    short mad = (short) size;
    initialize();
    for (int m = models.length; --m >= 0; )
      models[m].setMad(mad, bsSelected);
  }
  
  public void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    byte palette = 0;
    short colix = 0;
    if (propertyName.equals("colorScheme")) {
      if (value == null)
        return;
      palette = viewer.getPalette((String)value);
    } else if (propertyName.equals("color")) {
      palette = JmolConstants.PALETTE_COLOR;
      colix = viewer.getColix((Color)value);
    } else {
      return;
    }
    for (int m = models.length; --m >= 0; )
      models[m].setColix(palette, colix, bs);
  }

  abstract Chain allocateMcpsChain(PdbPolymer polymer);

  void initialize() {
    if (models == null) {
      int modelCount = pdbFile == null ? 0 : pdbFile.getModelCount();
      models = new Model[modelCount];
      for (int i = modelCount; --i >= 0; )
        models[i] = new Model(pdbFile.getModel(i));
    }
  }

  int getModelCount() {
    return models.length;
  }

  Model getMcpsModel(int i) {
    return models[i];
  }

  class Model {
    Chain[] chains;
    int modelNumber;
    
    Model(PdbModel model) {
      chains = new Chain[model.getChainCount()];
      this.modelNumber = model.getModelNumber();
      for (int i = chains.length; --i >= 0; )
        chains[i] = allocateMcpsChain(model.getChain(i).getPolymer());
    }
    
    public void setMad(short mad, BitSet bsSelected) {
      for (int i = chains.length; --i >= 0; ) {
        Chain chain = chains[i];
        if (chain.polymerCount > 0)
          chain.setMad(mad, bsSelected);
      }
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      for (int i = chains.length; --i >= 0; ) {
        Chain chain = chains[i];
        if (chain.polymerCount > 0)
          chain.setColix(palette, colix, bsSelected);
      }
    }

    int getChainCount() {
      return chains.length;
    }

    Chain getMcpsChain(int i) {
      return chains[i];
    }
  }

  abstract class Chain {
    PdbPolymer polymer;
    short madOn;
    short madHelixSheet;
    short madTurnRandom;

    int polymerCount;
    PdbGroup[] polymerGroups;
    short[] colixes;
    short[] mads;
    
    Chain(PdbPolymer polymer, int madOn, int madHelixSheet, int madTurnRandom) {
      this.polymer = polymer;
      this.madOn = (short)madOn;
      this.madHelixSheet = (short)madHelixSheet;
      this.madTurnRandom = (short)madTurnRandom;

      polymerCount = polymer.getCount();
      if (polymerCount > 0) {
        colixes = new short[polymerCount];
        mads = new short[polymerCount + 1];
        polymerGroups = polymer.getGroups();
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
        int structureType = polymerGroups[groupIndex].getStructureType();
        if (structureType == JmolConstants.SECONDARY_STRUCTURE_SHEET ||
            structureType == JmolConstants.SECONDARY_STRUCTURE_HELIX)
          return madHelixSheet;
        return madTurnRandom;
      case -3: // trace temperature
        if (! hasTemperatureRange)
          calcTemperatureRange();
        Atom atom = polymerGroups[groupIndex].getAlphaCarbonAtom();
        PdbAtom pdbAtom = atom.getPdbAtom();
        int temperature = pdbAtom.getTemperature(); // scaled by 1000
        int scaled = temperature - temperatureMin;
        if (range == 0)
          return (short)0;
        float percentile = scaled / floatRange;
        if (percentile < 0 || percentile > 1)
          System.out.println("Que ha ocurrido? " + percentile);
        return (short)((1500 * percentile) + 500);
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
        polymerGroups[0].getAlphaCarbonAtom().getTemperature();
      for (int i = polymerCount; --i > 0; ) {
        int temperature = polymerGroups[i].getAlphaCarbonAtom().getTemperature();
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

    public void setMad(short mad, BitSet bsSelected) {
      int[] atomIndices = polymer.getAtomIndices();
      for (int i = polymerCount; --i >= 0; ) {
        if (bsSelected.get(atomIndices[i]))
          mads[i] = mad >= 0 ? mad : getMadSpecial(mad, i);
      }
      if (polymerCount > 1)
        mads[polymerCount] = mads[polymerCount - 1];
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      int[] atomIndices = polymer.getAtomIndices();
      for (int i = polymerCount; --i >= 0; ) {
        int atomIndex = atomIndices[i];
        if (bsSelected.get(atomIndex))
          colixes[i] =
            palette > JmolConstants.PALETTE_CPK
            ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette)
            : colix;
      }
    }
  }
}

