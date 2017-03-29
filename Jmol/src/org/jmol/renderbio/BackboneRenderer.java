/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-17 10:45:52 -0600 (Fri, 17 Nov 2006) $
 * $Revision: 6250 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.renderbio;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;
import org.jmol.util.GData;

public class BackboneRenderer extends BioShapeRenderer {

  private boolean isDataFrame;

  @Override
  protected void renderBioShape(BioShape bioShape) {
    boolean checkPass2 = (!isExport && !vwr.gdata.isPass2);
    boolean showBlocks = vwr.getBoolean(T.backboneblocks);
    boolean showSteps = !showBlocks && vwr.getBoolean(T.backbonesteps)
        && bioShape.bioPolymer.isNucleic();
    float blockHeight = (showBlocks ? vwr.getFloat(T.backboneblockheight) : 0);
    isDataFrame = vwr.ms.isJmolDataFrameForModel(bioShape.modelIndex);
    int n = monomerCount;
    Atom[] atoms = ms.at;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      Atom atomA = atoms[leadAtomIndices[i]];
      short cA = colixes[i];
      mad = mads[i];
      int i1 = (i + 1) % n;
      drawSegment(atomA, atoms[leadAtomIndices[i1]], cA, colixes[i1], 100,
          checkPass2);
      if (showSteps) {
        NucleicMonomer g = (NucleicMonomer) monomers[i];
        Lst<BasePair> bps = g.getBasePairs();
        if (bps != null) {
          for (int j = bps.size(); --j >= 0;) {
            int iAtom = bps.get(j).getPartnerAtom(g);
            if (iAtom > i)
              drawSegment(atomA, atoms[iAtom], cA, cA, 1000, checkPass2);
          }
        }
      } else if (showBlocks && atomA.nBackbonesDisplayed > 0
          && monomers[i].isNucleicMonomer()) {
        cA = C.getColixInherited(cA, atomA.colixAtom);
        if (checkPass2 && !setBioColix(cA))
          continue;
        NucleicMonomer g = (NucleicMonomer) monomers[i];

        if (scrBox == null) {
          scrBox = new P3[8];
          for (int j = 0; j < 8; j++)
            scrBox[j] = new P3();
        }
        P3[] oxyz = g.getDSSRFrame(vwr);
        P3[] box = g.dssrBox;
        float lastHeight = g.dssrBoxHeight;
        boolean isPurine = g.isPurine();
        if (box == null || lastHeight != blockHeight) {
          g.dssrBoxHeight = blockHeight;
          if (box == null) {
          box = new P3[8];
          for (int j = 8; --j >= 0;)
            box[j] = new P3();
          g.dssrBox = box;
          }
          SymmetryInterface uc = vwr.getSymTemp().getUnitCell(oxyz, false, null);
          uc.toFractional(oxyz[0], true);
          uc.setOffsetPt(P3.new3(oxyz[0].x - 2.25f, oxyz[0].y + 5f, oxyz[0].z
              - blockHeight / 2));
          float x = 4.5f;
          float y = (isPurine ? -4.5f : -3f);
          float z = blockHeight;
          uc.toCartesian(box[0] = P3.new3(0, 0, 0), false);
          uc.toCartesian(box[1] = P3.new3(x, 0, 0), false);
          uc.toCartesian(box[2] = P3.new3(x, y, 0), false);
          uc.toCartesian(box[3] = P3.new3(0, y, 0), false);
          uc.toCartesian(box[4] = P3.new3(0, 0, z), false);
          uc.toCartesian(box[5] = P3.new3(x, 0, z), false);
          uc.toCartesian(box[6] = P3.new3(x, y, z), false);
          uc.toCartesian(box[7] = P3.new3(0, y, z), false);
        }
        for (int j = 0; j < 8; j++)
          vwr.tm.transformPt3f(box[j], scrBox[j]);      
        for (int j = 0; j < 36;)
          g3d.fillTriangle3f(scrBox[triangles[j++]], scrBox[triangles[j++]], scrBox[triangles[j++]], false);
        Atom atomB = g.getC1P();
        Atom atomC = g.getN0();
        if (atomB != null && atomC != null) {
          drawSegmentAB(atomA, atomB, cA, cA, 1000);
          drawSegmentAB(atomB, atomC, cA, cA, 1000);
        }
      }
    }
  }
  
  private P3[] scrBox;
  private final int[] triangles = new int[] {
     1, 0, 3, 1, 3, 2, 
     0, 4, 7, 0, 7, 3,
     4, 5, 6, 4, 6, 7,
     5, 1, 2, 5, 2, 6,
     2, 3, 7, 2, 7, 6,
     0, 1, 5, 0, 5, 4
  };

  private void drawSegment(Atom atomA, Atom atomB, short colixA, short colixB,
                           float max, boolean checkPass2) {
    if (atomA.nBackbonesDisplayed == 0 || atomB.nBackbonesDisplayed == 0
        || ms.isAtomHidden(atomB.i) || !isDataFrame
        && max < 1000 && atomA.distanceSquared(atomB) > max)
      return;
    colixA = C.getColixInherited(colixA, atomA.colixAtom);
    colixB = C.getColixInherited(colixB, atomB.colixAtom);
    if (checkPass2 && !setBioColix(colixA) && !setBioColix(colixB))
      return;
    drawSegmentAB(atomA, atomB, colixA, colixB, max);
  }

  private void drawSegmentAB(Atom atomA, Atom atomB, short colixA, short colixB, float max) {
    int xA = atomA.sX, yA = atomA.sY, zA = atomA.sZ;
    int xB = atomB.sX, yB = atomB.sY, zB = atomB.sZ;
    int mad = this.mad;
    if (max == 1000)
      mad = mad >> 1;
    if (mad < 0) {
      g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    } else {
      int width = (int) (isExport ? mad : vwr.tm.scaleToScreen((zA + zB) / 2,
          mad));
      g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_SPHERICAL, width, xA,
          yA, zA, xB, yB, zB);
    }
  }  
}
