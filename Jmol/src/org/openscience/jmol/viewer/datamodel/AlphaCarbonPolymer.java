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

public class AlphaCarbonPolymer extends Polymer {

  AlphaCarbonPolymer(Chain chain, Group[] groups) {
    super(chain, groups);
  }

  public Atom getAlphaCarbonAtom(int groupIndex) {
    return groups[groupIndex].getAlphaCarbonAtom();
  }

  public Point3f getCenterPoint(int groupIndex) {
    return getAlphaCarbonAtom(groupIndex).point3f;
  }

  public Point3f getResidueAlphaCarbonPoint(int groupIndex) {
    return getAlphaCarbonAtom(groupIndex).point3f;
  }

  public void getAlphaCarbonMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == count) {
      --groupIndex;
    } else if (groupIndex > 0) {
      midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
      midPoint.add(groups[groupIndex-1].getAlphaCarbonPoint());
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
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
      getAlphaCarbonMidPoint(groupIndex, midPoint);
      /*
        System.out.println("" + groupIndex + "the alpha carbon midpoint" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    }
  }

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
    int polymerIndexStart, polymerIndexEnd;
    if ((polymerIndexStart = getIndex(startSeqcode)) == -1 ||
        (polymerIndexEnd = getIndex(endSeqcode)) == -1)
      return;
    int structureCount = polymerIndexEnd - polymerIndexStart + 1;
    if (structureCount < 1) {
      System.out.println("structure definition error");
      return;
    }
    AminoStructure aminostructure = null;
    switch(type) {
    case JmolConstants.SECONDARY_STRUCTURE_HELIX:
      aminostructure = new Helix(this, polymerIndexStart, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_SHEET:
      if (this instanceof AminoPolymer)
        aminostructure = new Sheet((AminoPolymer)this,
                                   polymerIndexStart, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_TURN:
      aminostructure = new Turn(this, polymerIndexStart, structureCount);
      break;
    default:
      System.out.println("unrecognized secondary structure type");
      return;
    }
    for (int i = polymerIndexStart; i <= polymerIndexEnd; ++i)
      groups[i].setStructure(aminostructure);
  }

  public boolean isProtein() {
    return true;
  }

  public void calcHydrogenBonds() {
  }
}
