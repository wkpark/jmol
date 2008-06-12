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
package org.jmol.modelsetbio;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.modelset.Polymer;
import org.jmol.util.BitSetUtil;
//import org.jmol.util.Escape;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

public abstract class BioPolymer extends Polymer {

  Monomer[] monomers;
  public Monomer[] getMonomers() {
    return monomers;
  }

  int monomerCount;
  
  public int getMonomerCount() {
    return monomerCount;
  }

  protected Model model;
  public Model getModel() {
    return model;
  }
    
  BioPolymer(Monomer[] monomers) {
    this.monomers = monomers;
    monomerCount = monomers.length;
    for (int i = monomerCount; --i >= 0; )
      monomers[i].setBioPolymer(this, i);
    model = monomers[0].getModel();
  }
  
  static BioPolymer allocateBioPolymer(Group[] groups, int firstGroupIndex,
                                       boolean checkConnections) {
    Monomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i) {
      Group group = groups[i];
      Monomer current;
      if (!(group instanceof Monomer) 
          || (current = (Monomer)group).bioPolymer != null
          || previous != null && previous.getClass() != current.getClass()
          || checkConnections && !current.isConnectedAfter(previous))
        break;
      previous = current;
      count++;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (Monomer)groups[firstGroupIndex + j];
    if (previous instanceof AminoMonomer)
      return new AminoPolymer(monomers);
    if (previous instanceof AlphaMonomer)
      return new AlphaPolymer(monomers);
    if (previous instanceof NucleicMonomer)  
      return new NucleicPolymer(monomers);
    if (previous instanceof PhosphorusMonomer)
      return new PhosphorusPolymer(monomers);
    if (previous instanceof CarbohydrateMonomer)
      return new CarbohydratePolymer(monomers);
    Logger.error("Polymer.allocatePolymer() ... no matching polymer for monomor " + previous);
    throw new NullPointerException();
  }

  public void clearStructures() {
    for (int i = 0; i < monomerCount; i++)
      monomers[i].setStructure(null);
  }
  
  void removeProteinStructure(int monomerIndex, int count) {
    //System.out.println("biopolymer removeProteinStructure mIndex " + monomerIndex + " count " + count);
    for (int i = 0, pt = monomerIndex; i < count && pt < monomerCount; i++, pt++)
      monomers[pt].setStructure(null);
  }

  public int[] getLeadAtomIndices() {
    if (leadAtomIndices == null) {
      leadAtomIndices = new int[monomerCount];
      for (int i = monomerCount; --i >= 0; )
        leadAtomIndices[i] = monomers[i].getLeadAtomIndex();
    }
    return leadAtomIndices;
  }
  
  int getIndex(char chainID, int seqcode) {
    int i;
    for (i = monomerCount; --i >= 0;)
      if (monomers[i].getChainID() == chainID) {
        //System.out.println("BioPolymer getIndex seqcode monomers[i].seqcode " + chainID + " "
          //  + Group.getSeqcodeString(seqcode) + " "
            //+ Group.getSeqcodeString(monomers[i].getSeqcode()));
        if (monomers[i].getSeqcode() == seqcode)
          break;
      }
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
    recalculateLeadMidpointsAndWingVectors();
    //calculateStructures();
  }
  
  public void recalculateLeadMidpointsAndWingVectors() {    
    leadAtomIndices = null;
    sheetPoints = null;
    getLeadAtomIndices();
    ProteinStructure ps;
    ProteinStructure psLast = null;
    for (int i = 0; i < monomerCount; i++)
        if ((ps = getProteinStructure(i)) != null && ps != psLast) {
          (psLast = ps).resetAxes();
    }
    calcLeadMidpointsAndWingVectors(false);
  }
  
  public Point3f[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors(true);
    return leadMidpoints;
  }

  Point3f[] getLeadPoints() {
    if (leadPoints == null)
      calcLeadMidpointsAndWingVectors(true);
    return leadPoints;
  }

  public Point3f[] getControlPoints(boolean isTraceAlpha, float sheetSmoothing) {
    if (!isTraceAlpha)
      return leadMidpoints;
    else if (sheetSmoothing == 0)
      return leadPoints;
    return getSheetPoints(sheetSmoothing);
  }

  private float sheetSmoothing;
  private Point3f[] getSheetPoints(float sheetSmoothing) {
    if (sheetPoints != null && sheetSmoothing == this.sheetSmoothing)
      return sheetPoints;
    sheetPoints = new Point3f[monomerCount + 1];
    getLeadPoints();
    for (int i = 0; i < monomerCount; i++)
        sheetPoints[i] = new Point3f();
    Vector3f v = new Vector3f();
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSheet()) {
        v.sub(leadMidpoints[i], leadPoints[i]);
        v.scale(sheetSmoothing);
        sheetPoints[i].add(leadPoints[i], v);
      } else {
        sheetPoints[i] = leadPoints[i];
      }
    }
    sheetPoints[monomerCount] = sheetPoints[monomerCount - 1];
    this.sheetSmoothing = sheetSmoothing;
    return sheetPoints;
  }
  
  public final Vector3f[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors(true);
    return wingVectors; // wingVectors might be null ... before autocalc
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
    //if (model.getModelSet().viewer.getTestFlag1()) hasWingPoints = false;
    
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
        Vector3f previousVectorC = null;
        for (int i = 1; i < count; ++i) {
         // perfect for traceAlpha on; reasonably OK for traceAlpha OFF          
          vectorA.sub(leadMidpoints[i], leadPoints[i]);
          vectorB.sub(leadPoints[i], leadMidpoints[i + 1]);
          vectorC.cross(vectorA, vectorB);
          vectorC.normalize();
          if (previousVectorC != null &&
              previousVectorC.angle(vectorC) > Math.PI/2)
            vectorC.scale(-1);
          previousVectorC = wingVectors[i] = new Vector3f(vectorC);
        }
      }
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[count] = wingVectors[count - 1];
      /*
      Point3f pt = leadPoints[11];
      vectorC.set(wingVectors[11]);
      vectorC.add(pt);
      //order of points is mid11 lead11 mid12 lead12
      System.out.println("draw pt" + 11 + "b " + Escape.escape(leadMidpoints[11])  + " color yellow");
      System.out.println("draw pt" + 11 + " " + Escape.escape(leadPoints[11])  + " color red");
      System.out.println("draw pt" + 12 + "b " + Escape.escape(leadMidpoints[12])  + " color blue");
      System.out.println("draw pt" + 12 + " " + Escape.escape(leadPoints[12])  + " color green");
      System.out.println("draw v" + 11 + " arrow " + Escape.escape(pt) + " " + Escape.escape(vectorC));
      System.out.println("draw plane" + 11 + " " + Escape.escape(leadPoints[11]) + " " + Escape.escape(leadMidpoints[11]) + " "+ Escape.escape(leadMidpoints[12]));

      pt = leadMidpoints[11];
      vectorC.set(wingVectors[11]);
      vectorC.add(pt);
      System.out.println("draw v" + 11 + "b arrow " + Escape.escape(pt) + " " + Escape.escape(vectorC));
*/
      
  }

  private final Vector3f unitVectorX = new Vector3f(1, 0, 0);

  public void findNearestAtomIndex(int xMouse, int yMouse,
                            Atom[] closest, short[] mads, int myVisibilityFlag) {
    for (int i = monomerCount; --i >= 0; ) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || model.isAtomHidden(monomers[i].getLeadAtomIndex()))
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
    BitSetUtil.clear(bsSelectedMonomers);
    for (int i = 0; i < monomerCount; i++) {
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
    Vector structureInfo = null;
    ProteinStructure ps;
    ProteinStructure psLast = null;
    int n = 0;
    for (int i = 0; i < monomerCount; i++) {
      if (bs.get(monomers[i].getLeadAtomIndex())) {
        Hashtable monomerInfo = monomers[i].getMyInfo();
        monomerInfo.put("monomerIndex", new Integer(i));
        info.addElement(monomerInfo);
        if ((ps = getProteinStructure(i)) != null && ps != psLast) {
          Hashtable psInfo = new Hashtable();
          (psLast = ps).getInfo(psInfo);
          if (structureInfo == null)
            structureInfo = new Vector();
          psInfo.put("index", new Integer(n++));
          structureInfo.addElement(psInfo);
        }
      }
    }
    if (info.size() > 0) {
      returnInfo.put("sequence", getSequence());
      returnInfo.put("monomers", info);
      if (structureInfo != null)
        returnInfo.put("structures", structureInfo);
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
  
  public ProteinStructure getProteinStructure(int monomerIndex) {
    return monomers[monomerIndex].getProteinStructure();
  }
  
  protected boolean calcPhiPsiAngles() {
    return false;
  }
  
  final public static void getPdbData(BioPolymer p, char ctype, int derivType, BitSet bsAtoms,
                         StringBuffer pdbATOM, StringBuffer pdbCONECT) {
    int atomno = Integer.MIN_VALUE;
    Quaternion qlast = null;
    Quaternion qprev = null;
    Quaternion dq = null;
    Quaternion dqprev = null;
    Quaternion ddq = null;
    float factor = (ctype == 'r' ? 1f : 10f);
    float x = 0, y = 0, z = 0, w = 0;
    //boolean isQuaternion = ("wxyz".indexOf(ctype) >= 0);
    boolean isRamachandran = (ctype == 'r');
    if (isRamachandran && !p.calcPhiPsiAngles())
      return;    
    char qType = (isRamachandran ? 'c' : p.model.getModelSet().viewer.getQuaternionFrame()); 
    for (int m = 0; m < p.monomerCount; m++) {
      Monomer monomer = p.monomers[m];
      if (bsAtoms.get(monomer.getLeadAtomIndex())) {
        Atom a = monomer.getLeadAtom();
        if (isRamachandran) {
          x = monomer.getPhi();
          y = monomer.getPsi();
          z = monomer.getOmega();
          if (z < -90)
            z += 360;
          z -= 180;
          if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
            continue;
          w = a.getPartialCharge();
        } else {
          Quaternion q = monomer.getQuaternion(qType);
          if (q == null) {
            qlast = null;
            atomno = Integer.MIN_VALUE;
            continue;
          }
          if (derivType > 0) {
            if (qprev == null) {
              qprev = q;
              continue;
            }
            // get dq or dq*
            if (ctype == 'e')
              dq = q.mul(qprev.inv()); //NOT -- but gives plane!
            else 
              dq = qprev.inv().mul(q);
            // save this q as q'
            qprev = q;
            
            if (derivType == 2) {
              // SECOND derivative:
              if (dqprev == null) {
                dqprev = dq;            
                continue;
              }
              ddq = dqprev.inv().mul(dq);
              dqprev = dq;
              q = ddq;
            } else {
              // first deriv:
              q = dq;
            }
            
            // save this dq as dq'
            dqprev = dq;            
            if (q.q0 < 0)
              q = q.mul(-1);
          } else if (qlast == null && q.q0 < 0) {
            //initialize with a positive q0
            q = q.mul(-1);
          }
          if (qlast != null && q.dot(qlast) < 0)
            q = q.mul(-1);
          qlast = q;
          switch (ctype) {
          case 's':
          case 'w':
          case 'e':
            x = q.q1;
            y = q.q2;
            z = q.q3;
            w = q.q0;
            if (ctype == 's') {
              String id = "" + monomer.getResno();
              String strV = " VECTOR " + Escape.escape((Point3f)a) + " ";
              int deg = (int) (Math.acos(w) * 360 / Math.PI);
              //this is the angle required to rotate the INITIAL FRAME to this position
              if (deg < 0)
                deg += 360;
              strV = "draw qx" + id + strV + Escape.escape(q.getVector(0)) + " color red"
                  + "\ndraw qy" + id + strV + Escape.escape(q.getVector(1)) + " color green"
                  + "\ndraw qz" + id + strV + Escape.escape(q.getVector(2)) + " color blue"
                  + "\ndraw qa" + id + strV + " {" + (x*2) + "," + (y*2) + "," + (z*2) + "} \">" + deg + "\" color yellow"
                  + "\ndraw qb" + id + strV + " {" + (-x*2) + "," + (-y*2) + "," + (-z*2) + "} \">" + (deg < 0 ? -deg : 360 - deg) + "\" color yellow";
              pdbATOM.append(strV + "\n");
              continue;
            }
            break;
          case 'x':
            x = q.q0;
            y = q.q1;
            z = q.q2;
            w = q.q3;
            break;
          case 'y':
            x = q.q3;
            y = q.q0;
            z = q.q1;
            w = q.q2;
            break;
          case 'z':
            x = q.q2;
            y = q.q3;
            z = q.q0;
            w = q.q1;
            break;
          }
        }
        pdbATOM.append(a.formatLabel("ATOM  %5i %4a%1A%3n %1c%4R%1E   "));
        pdbATOM.append(TextFormat.sprintf("%8.3f%8.3f%8.3f%6.2f                %2s    \n", 
            new String[] { a.getElementSymbol().toUpperCase() }, 
            new float[]{ x * factor, y * factor, z * factor, w * factor }));
        if (atomno != Integer.MIN_VALUE) {
          pdbCONECT.append("CONECT");
          pdbCONECT.append(TextFormat.formatString("%5i", "i", atomno));
          pdbCONECT.append(TextFormat.formatString("%5i", "i", a
              .getAtomNumber()));
          pdbCONECT.append('\n');
        }
        atomno = a.getAtomNumber();
      }
    }
  }
}
