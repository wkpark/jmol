/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import org.openscience.jmol.viewer.pdb.PdbAtom;
import java.util.BitSet;

public class Labels extends Shape {

  public void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("label".equals(propertyName)) {
      String strLabel = (String)value;
      Atom[] atoms = frame.atoms;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          Atom atom = atoms[i];
          atom.setLabel(getLabelAtom(strLabel, atom, i));
        }
    }
  }

  String getLabelAtom(String strFormat, Atom atom, int atomIndex) {
    if (strFormat == null || strFormat.equals(""))
      return null;
    PdbAtom pdbatom = atom.getPdbAtom();
    String strLabel = "";
    int cch = strFormat.length();
    int ich, ichPercent;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1; ) {
      strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ich == cch) {
        --ich; // a percent sign at the end of the string
        break;
      }
      int ch = strFormat.charAt(ich++);
      switch (ch) {
      case 'i':
        strLabel += atom.getAtomno();
        break;
      case 'a':
        strLabel += atom.getAtomTypeName();
        break;
      case 'e':
        strLabel += atom.getAtomicSymbol();
        break;
      case 'x':
        strLabel += atom.getAtomX();
        break;
      case 'y':
        strLabel += atom.getAtomY();
        break;
      case 'z':
        strLabel += atom.getAtomZ();
        break;
      case 'C':
        int charge = atom.getAtomicCharge();
        if (charge > 0)
          strLabel += "" + charge + "+";
        else if (charge < 0)
          strLabel += "" + -charge + "-";
        else
          strLabel += "0";
        break;
      case 'V':
        strLabel += atom.getVanderwaalsRadiusFloat();
        break;
      case 'I':
        strLabel += atom.getBondingRadiusFloat();
        break;
      case 'b': // these two are the same
      case 't':
        strLabel += (atom.getBfactor100() / 100.0);
        break;
      case 'c': // these two are the same
      case 's':
        if (pdbatom != null)
          strLabel += pdbatom.getChainID();
        break;
      case 'M':
        strLabel += "/" + atom.getModelNumber();
        break;
      case 'm':
        strLabel += "<X>";
        break;
      case 'n':
        if (pdbatom != null)
          strLabel += pdbatom.getGroup3();
        break;
      case 'r':
        if (pdbatom != null)
          strLabel += pdbatom.getSeqcodeString();
        break;
      default:
        strLabel += "" + ch;
      }
    }
    strLabel += strFormat.substring(ich);
    if (strLabel.length() == 0)
      strLabel = null;
    return strLabel;
  }
}
