/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import org.jmol.api.MepCalculationInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;

class IsoMepReader extends AtomDataReader {

  IsoMepReader(SurfaceGenerator sg) {
    super(sg);
  }
    
  /////// molecular electrostatic potential ///////

  protected void setup() {
    super.setup();
    doAddHydrogens = false;
    getAtoms(params.mep_marginAngstroms, true, false);
    setHeader("MEP", "");
    setRangesAndAddAtoms(params.mep_ptsPerAngstrom, params.mep_gridMax, myAtomCount);    
  }

  protected void generateCube() {
    try {
      MepCalculationInterface m = (MepCalculationInterface) Class.forName(
          JmolConstants.CLASSBASE_QUANTUM + "MepCalculation").newInstance();
      m.calculate(volumeData, bsMySelected, atomData.atomXyz,
          params.theProperty);
    } catch (Exception e) {
      Logger.error("Error in MEP calculation " + e.getMessage());
    }

  }
}
