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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

public class NucleotidePolymer extends Polymer {

  NucleotidePolymer(Chain chain, Group[] groups) {
    super(chain, groups);
  }

  public Atom getNucleotidePhosphorusAtom(int groupIndex) {
    return groups[groupIndex].getNucleotidePhosphorusAtom();
  }

  public Atom getLeadAtom(int groupIndex) {
    return getNucleotidePhosphorusAtom(groupIndex);
  }

  public Point3f getLeadPoint(int groupIndex) {
    return getLeadAtom(groupIndex).point3f;
  }

  public void getStructureMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex < count &&
        groups[groupIndex].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex].aminostructure.
                   getStructureMidPoint(groupIndex));
      /*
        System.out.println("" + groupIndex} + "isHelixOrSheet" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else if (groupIndex > 0 &&
               groups[groupIndex - 1].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex - 1].aminostructure.
                   getStructureMidPoint(groupIndex));
      /*
        System.out.println("" + groupIndex + "previous isHelixOrSheet" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else {
      getLeadMidPoint(groupIndex, midPoint);
      /*
        System.out.println("" + groupIndex + "the alpha carbon midpoint" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    }
  }

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
  }

  public boolean isProtein() {
    return false;
  }

  public void calcHydrogenBonds() {
  }
}
