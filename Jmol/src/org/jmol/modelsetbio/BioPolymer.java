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
import org.jmol.util.Quaternion;
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
    for (int i = 0; i < monomerCount; i++) {
      if ((ps = getProteinStructure(i)) != null && ps != psLast)
        (psLast = ps).resetAxes();
      monomers[i].nhChecked = false;
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
          || !monomers[i].getLeadAtom().isVisible())
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
  
  final private static String[] qColor = { "yellow", "orange", "purple" };
  
  final public static void getPdbData(BioPolymer p, char ctype, char qtype,
                                      int derivType, boolean isDraw,
                                      BitSet bsAtoms, StringBuffer pdbATOM,
                                      StringBuffer pdbCONECT, BitSet bsSelected) {
    int atomno = Integer.MIN_VALUE;
    Quaternion qlast = null;
    Quaternion qprev = null;
    Quaternion dq = null;
    Quaternion dqprev = null;
    Quaternion ddq = null;
    float factor = (ctype == 'R' ? 1f : 10f);
    float x = 0, y = 0, z = 0, w = 0;
    //boolean isQuaternion = ("wxyz".indexOf(ctype) >= 0);
    boolean isRamachandran = (ctype == 'R');
    if (isRamachandran && !p.calcPhiPsiAngles())
      return;
    /*
     * A quaternion visualization involves assigning a frame to each amino
     * acid residue or nucleic acid base. This frame is an orthonormal x-y-z
     * axis system, which can be defined any number of ways. 
     * 
     *  'c'  C-alpha, as defined by Andy Hanson, U. of Indiana (unpublished results)
     *  
     *    X: CA-C (carbonyl carbon)
     *    Z: (CA-C) x (CA-N)
     *    Y: Z x X
     *    
     *  'p'  Peptide plane as defined by Bob Hanson, St. Olaf College (unpublished results)
     *  
     *    X: CA-
     *
     * 
     * 
     * 
     * quaternion types:
     * 
     * w, x, y, z : which of the q-terms to expunge in order to display
     * the other three. 
     * 
     * a : absolute (standard) derivative
     * r : relative (commuted) derivative
     * 
     */

    Atom aprev = null;
    String strExtra = "";
    boolean isAmino = (p instanceof AminoPolymer);
    boolean isRelativeAlias = (ctype == 'r');
    String prefix = (derivType > 0 ? "dq" + (derivType == 2 ? "2" : "") : "q");
    float psiLast = Float.NaN;
    if (derivType == 2 && isRelativeAlias)
      ctype = 'w';
    Quaternion q;
    for (int m = 0; m < p.monomerCount; m++) {
      Monomer monomer = p.monomers[m];
      if (bsAtoms == null || bsAtoms.get(monomer.getLeadAtomIndex())) {
        Atom a = monomer.getLeadAtom();
        char cid = monomer.getChainID();
        String id = "" + monomer.getResno() + (cid == '\0' ? "" : "" + cid);
        if (isRamachandran) {
          x = monomer.getPhi();
          y = monomer.getPsi();
          z = monomer.getOmega();
          if (z < -90)
            z += 360;
          z -= 180; // center on 0
          if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
            continue;
          
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getAtomIndex()))
              continue;
          // draw arrow arc {3.N} {3.ca} {3.C} {131 -131 0.5} "phi -131"
          // draw arrow arc {3.CA} {3.C} {3.N} {0 133 0.5} "psi 133"
          // as looked DOWN the bond, with {pt1} in the back, using
          // standard dihedral/Jmol definitions for anticlockwise positive angles
            AminoMonomer aa = (AminoMonomer)monomer;
            pdbATOM.append("draw phi" + id + " arrow arc scale 0.5 ") 
                .append(Escape.escape(aa.getNitrogenAtomPoint()))
                .append(Escape.escape((Point3f)a)) 
                .append(Escape.escape(aa.getCarbonylCarbonAtomPoint()))
                .append("{" + (-x) + " " + x + " 0.2} \"phi = " + (int) x + "\"")
                .append(" color ").append(qColor[0]).append('\n');
            pdbATOM.append("draw psi" + id + " arrow arc scale 0.5 ") 
                .append(Escape.escape((Point3f)a)) 
                .append(Escape.escape(aa.getCarbonylCarbonAtomPoint()))
                .append(Escape.escape(aa.getNitrogenAtomPoint()))
                .append("{0 " + y + " 0.2} \"psi = " + (int) y + "\"")
                .append(" color ").append(qColor[1]).append('\n');
            continue;
          }

          w = a.getPartialCharge();
          float phiNext = (m == p.monomerCount - 1 ? Float.NaN
              : p.monomers[m + 1].getPhi());
          float angle = y + phiNext - psiLast - x;//psi[i] + phi[i+1] - psi[i-1] - phi[i]
          psiLast = y;
          if (Float.isNaN(angle)) {
            strExtra = "";
            if (qtype == 'r')
              continue;
          } else {
            q = new Quaternion(new Point3f(1, 0, 0), angle);
            strExtra = TextFormat.sprintf("%10.6f%10.6f%10.6f%10.6f  %6.2f",
                new Object[] { new float[] { q.q0, q.q1, q.q2, q.q3,
                    q.getTheta() } });
            if (qtype == 'r')
              z = angle;
          }
        } else {
          cid = monomer.getLeadAtom().getAlternateLocationID();
          if (cid != '\0')
            id += cid;
          q = monomer.getQuaternion(qtype);
          if (q == null) {
            qlast = null;
            atomno = Integer.MIN_VALUE;
            continue;
          }
          if (derivType > 0) {
            if (aprev == null) {
              qprev = q;
              aprev = a;
              continue;
            }
            // get dq or dq*
            if (isRelativeAlias) {
              // ctype = 'r';
              // relative quaterion derivative = Q(i_inverse) * Q(i+1) 
              //  R = Q(i_inverse) * Q(i+1) * (0, v) * Q(i+1_inverse) * Q(i)
              // used for aligning all standard amino acids along X axis 
              // in the second derivative and in an ellipse in the first derivative
              dq = qprev.inv().mul(q);
            } else {
              // ctype = 'a' or 'w'
              // the standard difference dq
              // absolute quaterion derivative = Q(i+1) * Q(i_inverse) 
              //  R = Q(i+1) * Q(i_inverse) * (0, v) * Q(i) * Q(i+1_inverse)
              // used for definition of the local helical axis
              dq = q.mul(qprev.inv());
            }
            // save this q as q'
            qprev = q;
            if (derivType == 2) {
              // SECOND derivative:
              if (dqprev == null) {
                dqprev = dq;
                aprev = a;
                continue;
              }
              // standard second deriv.
              ddq = dq.mul(dqprev.inv());
              dqprev = dq;
              q = ddq;
            } else {
              // first deriv:
              q = dq;
            }
            if (derivType == 1 && aprev != null && qlast != null
                && ctype == 'w')
              aprev.getGroup().setStraightness(getStraightness(id, qlast, q));

            // and assign a to aprev so that the proper 
            // residue gets reported.
            // without these next three lines, the first
            // residues of helices and sheets are missed;
            // with these lines, the last residues of 
            // helices and sheets are missed
            // (in terms of assigning structure from quaternions).
            Atom atemp = a;
            a = aprev;
            aprev = atemp;
            // save this dq as dq'
            dqprev = dq;
          }
          qlast = q;
          switch (ctype) {
          default:
            x = q.q1;
            y = q.q2;
            z = q.q3;
            w = q.q0;
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
          Point3f ptCenter = (isAmino ? ((AminoMonomer) monomer).getQuaternionFrameCenter(qtype)
              : p instanceof NucleicPolymer ? 
                  ((NucleicMonomer) monomer).getQuaternionFrameCenter(qtype) 
              : new Point3f());

          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getAtomIndex()))
              continue;
            String strV = " VECTOR " + Escape.escape(ptCenter) + " ";
            int deg = (int) (Math.acos(w) * 360 / Math.PI);
            if (derivType == 0) {
              pdbATOM.append(
                  "draw " + prefix + "x" + id + strV
                      + Escape.escape(q.getVector(0))).append(" color red\n")
                  .append(
                      "draw " + prefix + "y" + id + strV
                          + Escape.escape(q.getVector(1))).append(
                      " color green\n").append(
                      "draw " + prefix + "z" + id + strV
                          + Escape.escape(q.getVector(2))).append(
                      " color blue\n");
              if (qtype == 'n' && isAmino) {
                Point3f ptH = ((AminoMonomer) monomer).getNitrogenHydrogenPoint();
                if (ptH != null)
                  pdbATOM.append(
                      "draw " + prefix + "nh" + id + " width 0.1 " + Escape.escape(ptH) + "\n");
              }
            }
            pdbATOM.append(
                "draw " + prefix + "a" + id + strV + " {" + (x * 2) + ","
                    + (y * 2) + "," + (z * 2) + "}" + " \">" + deg + "\"")
                .append(" color ").append(qColor[derivType]).append('\n');
            continue;
          }
          strExtra = TextFormat.sprintf(
              "%10.6f%10.6f%10.6f%10.6f  %6.2f  %10.5p %10.5p %10.5p",
              new Object[] {
                  new float[] { q.q0, q.q1, q.q2, q.q3, q.getTheta() },
                  new Point3f[] { ptCenter } });
          if (qtype == 'n' && isAmino)
            strExtra += TextFormat.sprintf("  %10.5p %10.5p %10.5p",
                new Object[] { new Point3f[] { ((AminoMonomer) a.getGroup())
                    .getNitrogenHydrogenPoint() } });
        }
        if (pdbATOM == null)
          continue;
        pdbATOM.append(a.formatLabel("ATOM  %5i %4a%1A%3n %1c%4R%1E   "));
        pdbATOM.append(TextFormat.sprintf(
            "%8.2f%8.2f%8.2f      %6.3f          %2s    %s\n", new String[] {
                a.getElementSymbol().toUpperCase(), strExtra }, new float[] {
                x * factor, y * factor, z * factor, w * factor }));
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
  
  private static float getStraightness(String id, Quaternion dqprev, Quaternion dq) {
    //System.out.println(id + " " + dqprev.getTheta() + " " + dq.getTheta());
    float f = (float) (Math.acos(dqprev.getNormal().dot(dq.getNormal()))/ Math.PI);
    return 1 - 2 * f;
  }
}
