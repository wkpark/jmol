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
import java.util.BitSet;

public class Trace extends Mcps {

  boolean hasTemperatureRange = false;
  int temperatureMin, temperatureMax;
  int range;
  float floatRange;

  Mcps.Chain allocateMcpsChain(PdbPolymer polymer) {
    return new Chain(polymer);
  }

  class Chain extends Mcps.Chain {
    Chain(PdbPolymer polymer) {
      super(polymer);
    }

    short getMadSpecial(short mad, int groupIndex) {
      switch (mad) {
      case -1: // trace on
        return (short)600;
      case -2: // trace structure
        int structureType = polymerGroups[groupIndex].getStructureType();
        if (structureType == JmolConstants.SECONDARY_STRUCTURE_SHEET ||
            structureType == JmolConstants.SECONDARY_STRUCTURE_HELIX)
          return (short)1500;
        return (short)500;
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
      System.out.println("unrecognized parameter to Trace.getMadDefault(" +
                         mad + ")");
      return 0;
    }

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
  }
}

