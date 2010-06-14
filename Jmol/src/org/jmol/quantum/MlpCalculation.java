/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.quantum;

import java.util.BitSet;

import org.jmol.modelset.Atom;
import org.jmol.util.Logger;

/*
 * loosely derived from pyMLP.py  http://code.google.com/p/pymlp/
 * see:
 * 
 * 
Broto P., Moreau G., Vandycke C. - 
Molecular structures: Perception, autocorrelation descriptor and sar studies.
System of atomic contributions for the calculation of the n-octanol/water 
partition coefficients, Eu. J. Med. Chem. 1984, 19.1, 71-78

Laguerre M., Saux M., Dubost J.P., Carpy A. -
MLPP: A program for the calculation of molecular lipophilicity potential in
proteins, Pharm. Sci. 1997, 3.5-6, 217-222
 
 
 * 
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 */
public class MlpCalculation extends MepCalculation {

  public MlpCalculation() {
    rangeBohr = 8; //bohr; about 4 Angstroms
    distanceMode = E_MINUS_D;
  }  
  
  public void fillPotentials(Atom[] atoms, float[] potentials, 
                             BitSet bsAromatic, BitSet bsCarbonyl) {
    for (int i = 0; i < atoms.length; i++) {
      float f;
      String name = atoms[i].getAtomType();
      switch (atoms[i].getElementNumber()) {
      case 6:
        //       0 2 4 6 8 10 13 16 19
        switch ("C CA".indexOf(name)) {
        case 0:
          f = -0.54f;
          break;
        case 2:
          f = 0.02f;
          break;
        default:
          f = (bsAromatic.get(i) ? 0.31f : bsCarbonyl.get(i) ? -0.54f : 0.45f); 
        }
        break;
      case 7:
        switch ("N NZNH1NH2ND2NE2".indexOf(name)) {
        case 0:
          f = -0.44f;
          break;
        case 2:
          f = -1.08f;
          break;
        case 4:
        case 10:
        case 11:
          f = -0.11f;
          break;
        case 7:
          f = -0.83f;
          break;
        default:
          f = (bsAromatic.get(i) ? -0.6f : bsCarbonyl.get(i) ? -0.44f : 
            -1.0f);
        }
        break;
      case 8:
        switch ("O OHOD1OD2OE1OE2OGOG1".indexOf(name)) {
        case 0:
        case 4:
        case 10:
          f = -0.68f;
          break;
        case 2:
          f = -0.17f;
          break;
        case 7:
        case 13:
          f = 0.53f;
          break;
        case 15:
        case 17:
          f = -1.0f;
        default:
          f = (bsCarbonyl.get(i) ? -0.9f : -0.17f);
        }
        break;
      case 16:
        f = -0.3f;
        break;
      default:
        f = 0;
      }
      if (Logger.debugging)
        Logger.info(atoms[i].getInfo() + " " + f);
      potentials[i] = f;
    }
  }

}
