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

abstract public class Polymer {

  Chain chain;
  Group[] groups;
  int count;

  private int[] atomIndices;

  static Polymer allocatePolymer(Chain chain) {
    Group[] polymerGroups;
    polymerGroups = getAminoGroups(chain);
    if (polymerGroups != null)
      return new AminoPolymer(chain, polymerGroups);
    polymerGroups = getAlphaCarbonGroups(chain);
    if (polymerGroups != null)
      return new AlphaCarbonPolymer(chain, polymerGroups);
    return null;
  }

  static Group[] getAminoGroups(Chain chain) {
    Group[] chainGroups = chain.groups;
    int firstNonMainchain = 0;
    int count = 0;
    for (int i = 0; i < chain.groupCount; ++i ) {
      Group group = chainGroups[i];
      if (! group.hasFullMainchain())
        continue;
      ++count;
      if (firstNonMainchain == i)
        ++firstNonMainchain;
    }
    if (count < 2)
      return null;
    Group[] groups = new Group[count];
    for (int i = 0, j = 0; i < chain.groupCount; ++i) {
      Group group = chainGroups[i];
      if (! group.hasFullMainchain())
        continue;
      groups[j++] = group;
    }
    System.out.println("is an AminoPolymer");
    return groups;
  }

  static Group[] getAlphaCarbonGroups(Chain chain) {
    Group[] chainGroups = chain.groups;
    int firstNonMainchain = 0;
    int count = 0;
    for (int i = 0; i < chain.groupCount; ++i ) {
      Group group = chainGroups[i];
      if (! group.hasAlphaCarbon())
        continue;
      ++count;
      if (firstNonMainchain == i)
        ++firstNonMainchain;
    }
    if (count < 2)
      return null;
    Group[] groups = new Group[count];
    for (int i = 0, j = 0; i < chain.groupCount; ++i) {
      Group group = chainGroups[i];
      if (! group.hasAlphaCarbon())
        continue;
      groups[j++] = group;
    }
    System.out.println("is an AlphaCarbonPolymer");
    return groups;
  }

  Polymer(Chain chain, Group[] groups) {
    this.chain = chain;
    this.groups = groups;
    this.count = groups.length;
    for (int i = count; --i >= 0; )
      groups[i].setPolymer(this);
  }

  public int getCount() {
    return count;
  }

  public Group[] getGroups() {
    return groups;
  }

  public int[] getLeadAtomIndices() {
    if (atomIndices == null) {
      atomIndices = new int[count];
      for (int i = count; --i >= 0; )
        atomIndices[i] = groups[i].getLeadAtomIndex();
    }
    return atomIndices;
  }

  public int getIndex(int seqcode) {
    int i;
    for (i = count; --i >= 0; )
      if (groups[i].seqcode == seqcode)
        break;
    return i;
  }

  abstract void addSecondaryStructure(byte type,
                                      int startSeqcode, int endSeqcode);
  abstract boolean isProtein();

  abstract void calcHydrogenBonds();
}
