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

  // these arrays will be one longer than the polymerCount
  // we probably should have better names for these things
  // holds center points between alpha carbons or sugar phosphoruses
  Point3f[] leadMidpoints;
  // holds the vector that runs across the 'ribbon'
  Vector3f[] wingVectors;

  static Polymer allocatePolymer(Chain chain) {
    Group[] polymerGroups;
    polymerGroups = getAminoGroups(chain);
    if (polymerGroups != null)
      return new AminoPolymer(chain, polymerGroups);
    polymerGroups = getAlphaCarbonGroups(chain);
    if (polymerGroups != null)
      return new AlphaCarbonPolymer(chain, polymerGroups);
    polymerGroups = getNucleotideGroups(chain);
    if (polymerGroups != null)
      return new NucleotidePolymer(chain, polymerGroups);
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

  static Group[] getNucleotideGroups(Chain chain) {
    Group[] chainGroups = chain.groups;
    int firstNonNucleotide = 0;
    int count = 0;
    for (int i = 0; i < chain.groupCount; ++i ) {
      Group group = chainGroups[i];
      if (! group.hasNucleotidePhosphorus())
        continue;
      ++count;
    }
    if (count < 2)
      return null;
    Group[] groups = new Group[count];
    for (int i = 0, j = 0; i < chain.groupCount; ++i) {
      Group group = chainGroups[i];
      if (! group.hasNucleotidePhosphorus())
        continue;
      groups[j++] = group;
    }
    System.out.println("is a NucleotidePolymer");
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

  abstract public Point3f getLeadPoint(int polymerIndex);

  abstract public Atom getLeadAtom(int polymerIndex);

  public void getLeadMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == count) {
      --groupIndex;
    } else if (groupIndex > 0) {
      midPoint.set(getLeadPoint(groupIndex));
      midPoint.add(getLeadPoint(groupIndex - 1));
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(getLeadPoint(groupIndex));
  }

  boolean hasWingPoints() { return false; }

  Point3f getWingPoint(int polymerIndex) { return null; }

  abstract void addSecondaryStructure(byte type,
                                      int startSeqcode, int endSeqcode);
  abstract boolean isProtein();

  abstract void calcHydrogenBonds();

  Point3f[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadMidpoints;
  }

  Vector3f[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors();
    return wingVectors; // wingVectors might be null ... before autocalc
  }

  void calcLeadMidpointsAndWingVectors() {
    int count = this.count;
    leadMidpoints = new Point3f[count + 1];
    wingVectors = new Vector3f[count + 1];
    boolean hasWingPoints = hasWingPoints();
    hasWingPoints = false;
    
    Vector3f vectorA = new Vector3f();
    Vector3f vectorB = new Vector3f();
    Vector3f vectorC = new Vector3f();
    Vector3f vectorD = new Vector3f();
    
    Point3f leadPointPrev, leadPoint;
    leadMidpoints[0] = leadPointPrev = leadPoint = getLeadPoint(0);
    Vector3f previousVectorD = null;
    for (int i = 1; i < count; ++i) {
      leadPointPrev = leadPoint;
      leadPoint = getLeadPoint(i);
      Point3f midpoint = new Point3f(leadPoint);
      midpoint.add(leadPointPrev);
      midpoint.scale(0.5f);
      leadMidpoints[i] = midpoint;
      if (hasWingPoints) {
        vectorA.sub(leadPoint, leadPointPrev);
        vectorB.sub(getWingPoint(i - 1), leadPointPrev);
        vectorC.cross(vectorA, vectorB);
        vectorD.cross(vectorC, vectorA);
        vectorD.normalize();
        if (previousVectorD != null &&
            previousVectorD.angle(vectorD) > Math.PI/2)
          vectorD.scale(-1);
        previousVectorD = wingVectors[i] = new Vector3f(vectorD);
      }
    }
    leadMidpoints[count] = getLeadPoint(count - 1);
    if (! hasWingPoints) {
      // auto-calculate wing vectors based upon lead atom positions only
      // seems to work like a charm! :-)
      Point3f next, current, prev;
      prev = leadMidpoints[0];
      current = leadMidpoints[1];
      Vector3f previousVectorC = null;
      for (int i = 1; i < count; ++i) {
        next = leadMidpoints[i + 1];
        vectorA.sub(prev, current);
        vectorB.sub(next, current);
        vectorC.cross(vectorA, vectorB);
        vectorC.normalize();
        if (previousVectorC != null &&
            previousVectorC.angle(vectorC) > Math.PI/2)
          vectorC.scale(-1);
        previousVectorC = wingVectors[i] = new Vector3f(vectorC);
        prev = current;
        current = next;
      }
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[count] = wingVectors[count - 1];
    }
  }
}
