/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-11 13:29:38 -0600 (Mon, 11 Dec 2006) $
 * $Revision: 6442 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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


import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.shape.Closest;
import org.jmol.util.Logger;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;

public class AminoMonomer extends AlphaMonomer {

  // negative values are optional
  final static byte[] interestingAminoAtomIDs = {
    JmolConstants.ATOMID_ALPHA_CARBON,      // 0 CA alpha carbon
    ~JmolConstants.ATOMID_CARBONYL_OXYGEN,   // 1 O wing man
    JmolConstants.ATOMID_AMINO_NITROGEN,    // 2 N
    JmolConstants.ATOMID_CARBONYL_CARBON,   // 3 C  point man
    ~JmolConstants.ATOMID_TERMINATING_OXT,  // 4 OXT
    ~JmolConstants.ATOMID_O1,               // 5 O1
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    byte[] offsets = scanForOffsets(firstAtomIndex, specialAtomIndexes,
                                    interestingAminoAtomIDs);
    if (offsets == null)
      return null;
    if (specialAtomIndexes[JmolConstants.ATOMID_CARBONYL_OXYGEN] < 0) {
      int carbonylOxygenIndex = specialAtomIndexes[JmolConstants.ATOMID_O1];
      Logger.debug("I see someone who does not have a carbonyl oxygen");
      if (carbonylOxygenIndex < 0)
        return null;
      offsets[1] = (byte)(carbonylOxygenIndex - firstAtomIndex);
    }
    if (atoms[firstAtomIndex].isHetero() && !isBondedCorrectly(firstAtomIndex, offsets, atoms)) 
      return null;
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, seqcode,
                       firstAtomIndex, lastAtomIndex, offsets);
    return aminoMonomer;
  }

  private static boolean isBondedCorrectly(int offset1, int offset2,
                                   int firstAtomIndex,
                                   byte[] offsets, Atom[] atoms) {
    int atomIndex1 = firstAtomIndex + (offsets[offset1] & 0xFF);
    int atomIndex2 = firstAtomIndex + (offsets[offset2] & 0xFF);
    if (atomIndex1 >= atomIndex2)
      return false;
    return atoms[atomIndex1].isBonded(atoms[atomIndex2]);
  }

  private static boolean isBondedCorrectly(int firstAtomIndex, byte[] offsets,
                                 Atom[] atoms) {
    return (isBondedCorrectly(2, 0, firstAtomIndex, offsets, atoms) &&
            isBondedCorrectly(0, 3, firstAtomIndex, offsets, atoms) &&
            isBondedCorrectly(3, 1, firstAtomIndex, offsets, atoms));
  }
  
  ////////////////////////////////////////////////////////////////

  private AminoMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffsetIndex(2);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomPointFromOffsetIndex(2);
  }

  Atom getCarbonylCarbonAtom() {
    return getAtomFromOffsetIndex(3);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomPointFromOffsetIndex(3);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }

  Atom getInitiatorAtom() {
    return getNitrogenAtom();
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[4] != -1 ? 4 : 3);
  }

  ////////////////////////////////////////////////////////////////
/*
  public Atom getAtom(byte specialAtomID) {
    return getSpecialAtom(interestingAminoAtomIDs, specialAtomID);
  }
  public Point3f getAtomPoint(byte specialAtomID) {
    return getSpecialAtomPoint(interestingAminoAtomIDs, specialAtomID);
  }
*/

  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AminoMonomer))
      return false;
    AminoMonomer other = (AminoMonomer)possiblyPreviousMonomer;
    return other.getCarbonylCarbonAtom().isBonded(getNitrogenAtom());
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Closest closest,
                            short madBegin, short madEnd) {
    
    Atom competitor = closest.atom;
    Atom nitrogen = getNitrogenAtom();
    short marBegin = (short) (madBegin / 2);
    if (marBegin < 1200)
      marBegin = 1200;
    if (nitrogen.screenZ == 0)
      return;
    int radiusBegin = scaleToScreen(nitrogen.screenZ, marBegin);
    if (radiusBegin < 4)
      radiusBegin = 4;
    Atom ccarbon = getCarbonylCarbonAtom();
    short marEnd = (short) (madEnd / 2);
    if (marEnd < 1200)
      marEnd = 1200;
    int radiusEnd = scaleToScreen(nitrogen.screenZ, marEnd);
    if (radiusEnd < 4)
      radiusEnd = 4;
    Atom alpha = getLeadAtom();
    if (isCursorOnTopOf(alpha, x, y, (radiusBegin + radiusEnd) / 2,
        competitor)
        || isCursorOnTopOf(nitrogen, x, y, radiusBegin, competitor)
        || isCursorOnTopOf(ccarbon, x, y, radiusEnd, competitor))
      closest.atom = alpha;
  }
  
  public Quaternion getQuaternion() {
    /*
     * also NucleicMonomer
     *  
     * see:
     * 
     *  Hanson and Thakur: http://www.cs.indiana.edu/~hanson/  http://www.cs.indiana.edu/~sithakur/
     *  
     *  Albrecht, Hart, Shaw, Dunker: 
     *  
     *   Contact Ribbons: a New Tool for Visualizing Intra- and Intermolecular Interactions in Proteins
     *   Electronic Proceedings for the 1996 Pacific Symposium on Biocomputing
     *   http://psb.stanford.edu/psb-online/proceedings/psb96/albrecht.pdfx
     *   
     *  Kneller and Calligari:
     *  
     *   Efficient characterization of protein secondary structure in terms of screw motion
     *   Acta Cryst. (2006). D62, 302-311    [ doi:10.1107/S0907444905042654 ]
     *   http://scripts.iucr.org/cgi-bin/paper?ol5289
     * 
     *  Wang and Zang:
     *   
     *   Protein secondary structure prediction with Bayesian learning method
     *   http://cat.inist.fr/?aModele=afficheN&cpsidt=15618506
     *
     *  Geetha:
     *  
     *   Distortions in protein helices
     *   International Journal of Biological Macromolecules, Volume 19, Number 2, August 1996 , pp. 81-89(9)
     *   http://www.ingentaconnect.com/content/els/01418130/1996/00000019/00000002/art01106
     *   DOI: 10.1016/0141-8130(96)01106-3
     *    
     *  Kavraki:
     *  
     *   Representing Proteins in Silico and Protein Forward Kinematics
     *   http://cnx.org/content/m11621/latest
     *   
     */
      
    Point3f ptC = getCarbonylCarbonAtomPoint(); 
    Point3f ptCa = getLeadAtomPoint();
    
    
    //vA = ptC - ptCa
    Vector3f vA = new Vector3f(ptC);
    vA.sub(ptCa);
    
    //vB = ptN - ptCa
    Point3f ptN = getNitrogenAtomPoint();
    Vector3f vB = new Vector3f(ptN);
    vB.sub(ptCa);
    
    /* alternative frame:
     
    Point3f ptO = getCarbonylOxygenAtomPoint(); 
    //vA = ptO - ptC
    Vector3f vA = new Vector3f(ptO);
    vA.sub(ptC);
    
    //vB = ptCa - ptC
    Vector3f vB = new Vector3f(ptCa);
    vB.sub(ptC);
  
   */    
    
    return Quaternion.getQuaternionFrame(vA, vB);   
  }
  
}
