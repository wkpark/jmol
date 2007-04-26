/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-24 08:15:07 -0500 (Tue, 24 Apr 2007) $
 * $Revision: 7479 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.shapebio;

import org.jmol.modelframe.Group;
import org.jmol.modelframe.Polymer;
import org.jmol.shape.Closest;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;


public abstract class BioPolymer extends Polymer {

  Monomer[] monomers;
  int monomerCount;
  
  //int structureCount;
  
  BioPolymer(Monomer[] monomers) {
    this.monomers = monomers;
    this.monomerCount = monomers.length;
    for (int i = monomerCount; --i >= 0; )
      monomers[i].setBioPolymer(this, i);
    model = monomers[0].getModel();
    model.addBioPolymer((Polymer)this);
  }
  
  static BioPolymer allocateBioPolymer(Group[] groups, int firstGroupIndex) {
    Monomer[] monomers;
    monomers = getAminoMonomers(groups, firstGroupIndex);
    if (monomers != null) {
      return new AminoPolymer(monomers);
    }
    monomers = getAlphaMonomers(groups, firstGroupIndex);
    if (monomers != null) {
      return new AlphaPolymer(monomers);
    }
    monomers = getNucleicMonomers(groups, firstGroupIndex);
    if (monomers != null) {
      return new NucleicPolymer(monomers);
    }
    monomers = getPhosphorusMonomers(groups, firstGroupIndex);
    if (monomers != null) {
      return new PhosphorusPolymer(monomers);
    }
    monomers = getCarbohydrateMonomers(groups, firstGroupIndex);
    if (monomers != null) {
      return new CarbohydratePolymer(monomers);
    }
    Logger.error("Polymer.allocatePolymer() ... why am I here?");
    throw new NullPointerException();
  }

  private static Monomer[] getAlphaMonomers(Group[] groups, int firstGroupIndex) {
    AlphaMonomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i, ++count) {
      Group group = groups[i];
      if (! (group instanceof AlphaMonomer))
        break;
      AlphaMonomer current = (AlphaMonomer)group;
      if (current.bioPolymer != null)
        break;
      if (! current.isConnectedAfter(previous))
        break;
      previous = current;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (AlphaMonomer)groups[firstGroupIndex + j];
    return monomers;
  }

  private static Monomer[] getAminoMonomers(Group[] groups, int firstGroupIndex) {
    AminoMonomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i, ++count) {
      Group group = groups[i];
      if (! (group instanceof AminoMonomer))
        break;
      AminoMonomer current = (AminoMonomer)group;
      if (current.bioPolymer != null)
        break;
      if (! current.isConnectedAfter(previous))
        break;
      previous = current;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (AminoMonomer)groups[firstGroupIndex + j];
    return monomers;
  }

  private static Monomer[] getCarbohydrateMonomers(Group[] groups, int firstGroupIndex) {
    //CarbohydrateMonomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i, ++count) {
      Group group = groups[i];
      if (! (group instanceof CarbohydrateMonomer))
        break;
      CarbohydrateMonomer current = (CarbohydrateMonomer)group;
      if (current.bioPolymer != null)
        break;
      //ignoring how these are connected for now
      //if (current.isConnectedAfter(previous))
        //break;
      //previous = current;
    }
    
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (CarbohydrateMonomer)groups[firstGroupIndex + j];
    return monomers;
  }

  private static Monomer[] getPhosphorusMonomers(Group[] groups, int firstGroupIndex) {
    PhosphorusMonomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i, ++count) {
      Group group = groups[i];
      if (! (group instanceof PhosphorusMonomer))
        break;
      PhosphorusMonomer current = (PhosphorusMonomer)group;
      if (current.bioPolymer != null)
        break;
      if (! current.isConnectedAfter(previous))
        break;
      previous = current;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (PhosphorusMonomer)groups[firstGroupIndex + j];
    return monomers;
  }

  private static Monomer[] getNucleicMonomers(Group[] groups, int firstGroupIndex) {
    NucleicMonomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i, ++count) {
      Group group = groups[i];
      if (! (group instanceof NucleicMonomer))
        break;
      NucleicMonomer current = (NucleicMonomer)group;
      if (current.bioPolymer != null)
        break;
      if (! current.isConnectedAfter(previous))
        break;
      previous = current;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (NucleicMonomer)groups[firstGroupIndex + j];
    return monomers;
  }

  int[] getLeadAtomIndices() {
    if (leadAtomIndices == null) {
      leadAtomIndices = new int[monomerCount];
      for (int i = monomerCount; --i >= 0; )
        leadAtomIndices[i] = monomers[i].getLeadAtomIndex();
    }
    return leadAtomIndices;
  }
  
  int getIndex(char chainID, int seqcode) {
    int i;
    for (i = monomerCount; --i >= 0; )
      if (monomers[i].getSeqcode() == seqcode &&
          monomers[i].getChainID() == chainID)
        break;
    return i;
  }

  final Point3f getLeadPoint(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtomPoint();
  }

  final Point3f getInitiatorPoint() {
    return monomers[0].getInitiatorAtom();
  }

  final Point3f getTerminatorPoint() {
    return monomers[monomerCount - 1].getTerminatorAtom();
  }
/*
  public final Atom getLeadAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }
*/
  void getLeadMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == monomerCount) {
      --groupIndex;
    } else if (groupIndex > 0) {
      midPoint.set(getLeadPoint(groupIndex));
      midPoint.add(getLeadPoint(groupIndex - 1));
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(getLeadPoint(groupIndex));
  }
  
  void getLeadPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == monomerCount)
      --groupIndex;
    midPoint.set(getLeadPoint(groupIndex));
  }
  
  boolean hasWingPoints() { return false; }

  // this might change in the future ... if we calculate a wing point
  // without an atom for an AlphaPolymer
  final Point3f getWingPoint(int polymerIndex) {
    return monomers[polymerIndex].getWingAtomPoint();
  }
  
  final Point3f getPointPoint(int polymerIndex) {
    return monomers[polymerIndex].getPointAtomPoint();
  }
  
  public void addSecondaryStructure(byte type,
                             char startChainID, int startSeqcode,
                             char endChainID, int endSeqcode) {
  }

  public void calculateStructures() { }

  public void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    // subclasses should override if they know how to calculate hbonds
  }
  
  public void setConformation(BitSet bsSelected, int nAltLocsInModel) {
    for (int i = monomerCount; --i >= 0; )
      monomers[i].updateOffsetsForAlternativeLocations(bsSelected, nAltLocsInModel);
    leadAtomIndices = null;
    getLeadAtomIndices();
    calcLeadMidpointsAndWingVectors(false);
    //calculateStructures();
  }
  
  public Point3f[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadMidpoints;
  }

  Point3f[] getLeadPoints() {
    if (leadPoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadPoints;
  }

  Point3f[] getControlPoints(boolean isTraceAlpha, float sheetSmoothing) {
    if (!isTraceAlpha)
      return leadMidpoints;
    else if (sheetSmoothing == 0)
      return leadPoints;
    return getTempPoints(sheetSmoothing);
  }

  private float sheetSmoothing;
  private Point3f[] getTempPoints(float sheetSmoothing) {
    if (tempPoints != null && sheetSmoothing == this.sheetSmoothing)
      return tempPoints;
    tempPoints = new Point3f[monomerCount + 1];
    getLeadPoints();
    for (int i = 0; i < monomerCount; i++)
        tempPoints[i] = new Point3f();
    Vector3f v = new Vector3f();
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSheet()) {
        v.sub(leadMidpoints[i], leadPoints[i]);
        v.scale(sheetSmoothing);
        tempPoints[i].add(leadPoints[i], v);
      } else {
        tempPoints[i] = leadPoints[i];
      }
    }
    tempPoints[monomerCount] = tempPoints[monomerCount - 1];
    this.sheetSmoothing = sheetSmoothing;
    return tempPoints;
  }
  
  final Vector3f[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors();
    return wingVectors; // wingVectors might be null ... before autocalc
  }

  private final void calcLeadMidpointsAndWingVectors() {
    calcLeadMidpointsAndWingVectors(true);
  }

  private final void calcLeadMidpointsAndWingVectors(boolean getNewPoints) {
    int count = monomerCount;
    if (leadMidpoints == null || getNewPoints) {
      leadMidpoints = new Point3f[count + 1];
      leadPoints = new Point3f[count + 1];
      wingVectors = new Vector3f[count + 1];
      sheetSmoothing = Float.MIN_VALUE;
    }
    boolean hasWingPoints = hasWingPoints();
    
    Vector3f vectorA = new Vector3f();
    Vector3f vectorB = new Vector3f();
    Vector3f vectorC = new Vector3f();
    Vector3f vectorD = new Vector3f();
    
    Point3f leadPointPrev, leadPoint;
    leadMidpoints[0] = getInitiatorPoint();
    leadPoints[0] = leadPoint = getLeadPoint(0);
    Vector3f previousVectorD = null;
    //proteins:
    //       C        O (wing)
    //        \       |
    //         CA--N--C        O (wing)
    //      (lead)     \       |    
    //                  CA--N--C 
    //               (lead)     \
    //                           CA--N
    //                        (lead)
    // mon#    2         1        0
    for (int i = 1; i < count; ++i) {
      leadPointPrev = leadPoint;
      leadPoints[i] = leadPoint = getLeadPoint(i);
      Point3f midpoint = new Point3f(leadPoint);
      midpoint.add(leadPointPrev);
      midpoint.scale(0.5f);
      leadMidpoints[i] = midpoint;
      if (hasWingPoints) {
        vectorA.sub(leadPoint, leadPointPrev);
        vectorB.sub(leadPointPrev, getWingPoint(i - 1));
        vectorC.cross(vectorA, vectorB);
        vectorD.cross(vectorA, vectorC);
        vectorD.normalize();
        if (previousVectorD != null &&
            previousVectorD.angle(vectorD) > Math.PI/2)
          vectorD.scale(-1);
        previousVectorD = wingVectors[i] = new Vector3f(vectorD);
      }
    }
    leadPoints[count] = leadMidpoints[count] = getTerminatorPoint();
    if (!hasWingPoints) {
      if (count < 3) {
        wingVectors[1] = unitVectorX;
      } else {
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
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[count] = wingVectors[count - 1];

    /*
    for (int i = 0; i < wingVectors.length; ++i) {
      if (wingVectors[i] == null) {
        Logger.debug("que? wingVectors[" + i + "] == null?");
        Logger.debug("hasWingPoints=" + hasWingPoints +
                           " wingVectors.length=" + wingVectors.length +
                           " count=" + count);
                      
      }
      else if (Float.isNaN(wingVectors[i].x)) {
        Logger.debug("wingVectors[" + i + "]=" + wingVectors[i]);
      }
    }
    */
  }

  private final Vector3f unitVectorX = new Vector3f(1, 0, 0);

  void findNearestAtomIndex(int xMouse, int yMouse,
                            Closest closest, short[] mads, int myVisibilityFlag) {
    for (int i = monomerCount; --i >= 0; ) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || this.model.isAtomHidden(monomers[i].getLeadAtomIndex()))
        continue;  
      if (mads[i] > 0 || mads[i + 1] > 0)
        monomers[i].findNearestAtomIndex(xMouse, yMouse, closest,
                                         mads[i], mads[i + 1]);
    }
  }

  private int selectedMonomerCount;

  int getSelectedMonomerCount() {
    return selectedMonomerCount;
  }
  
  BitSet bsSelectedMonomers;

  public void calcSelectedMonomersCount(BitSet bsSelected) {
    selectedMonomerCount = 0;
    if (bsSelectedMonomers == null)
      bsSelectedMonomers = new BitSet();
    else
      BitSetUtil.clear(bsSelectedMonomers);
    for (int i = monomerCount; --i >= 0; ) {
      if (monomers[i].isSelected(bsSelected)) {
        ++selectedMonomerCount;
        bsSelectedMonomers.set(i);
      }
    }
  }

  boolean isMonomerSelected(int i) {
    return (i >= 0 && bsSelectedMonomers.get(i));
  }
  
  public int getPolymerPointsAndVectors(int last, BitSet bs, Vector vList,
                                        boolean isTraceAlpha,
                                        float sheetSmoothing) {
    Point3f[] points = getControlPoints(isTraceAlpha, sheetSmoothing);
    Vector3f[] vectors = getWingVectors();
    int count = monomerCount;
    for (int j = 0; j < count; j++)
      if (bs.get(monomers[j].getLeadAtomIndex())) {
        vList.addElement(new Point3f[] { points[j], new Point3f(vectors[j]) });
        last = j;
      } else if (last != Integer.MAX_VALUE - 1) {
        vList.addElement(new Point3f[] { points[j], new Point3f(vectors[j]) });
        last = Integer.MAX_VALUE - 1;
      }
    if (last + 1 < count)
      vList.addElement(new Point3f[] { points[last + 1],
          new Point3f(vectors[last + 1]) });
    return last;
  }
  
  public String getSequence() {
    char[] buf = new char[monomerCount];
    for (int i = 0; i < monomerCount; i++)
      buf[i] = monomers[i].getGroup1();
    return String.valueOf(buf);
  }

  public Hashtable getPolymerInfo(BitSet bs) {
    Hashtable returnInfo = new Hashtable();
    Vector info = new Vector();
    for (int i = 0; i < monomerCount; i++) {
      if (bs.get(monomers[i].getLeadAtomIndex())) {
        Hashtable monomerInfo = monomers[i].getMyInfo();
        monomerInfo.put("monomerIndex", new Integer(i));
        info.addElement(monomerInfo);
      }
    }
    if (info.size() > 0) {
      returnInfo.put("sequence", getSequence());
      returnInfo.put("monomers", info);
    }
    return returnInfo;
  }
  
  public void getPolymerSequenceAtoms(int iModel, int iPolymer, int group1,
                                      int nGroups, BitSet bsInclude,
                                      BitSet bsResult) {
    int max = group1 + nGroups;
    for (int i = group1; i < monomerCount && i < max; i++)
       monomers[i].getMonomerSequenceAtoms(bsInclude, bsResult);
  }


}
