/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.viewer;

import org.jmol.g3d.*;

class ColorManager {

  Viewer viewer;
  Graphics3D g3d;

  int[] argbsCpk;

  ColorManager(Viewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    argbsCpk = JmolConstants.argbsCpk;
  }

  void setDefaultColors(String colorScheme) {
    System.out.println("setting color scheme to:" + colorScheme);
    if (colorScheme.equals("jmol")) {
      argbsCpk = JmolConstants.argbsCpk;
      viewer.setColorBackground("black");
      viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
    } else if (colorScheme.equals("rasmol")) {
      copyArgbsCpk();
      int argb = JmolConstants.argbsCpkRasmol[0] | 0xFF000000;
      for (int i = argbsCpk.length; --i >= 0; )
        argbsCpk[i] = argb;
      for (int i = JmolConstants.argbsCpkRasmol.length; --i >= 0; ) {
        argb = JmolConstants.argbsCpkRasmol[i];
        int atomNo = argb >> 24;
        argb |= 0xFF000000;
        argbsCpk[atomNo] = argb;
      }
      viewer.setColorBackground("black");
      viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
    } else {
      System.out.println("unrecognized color scheme");
      return;
    }
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)i, argbsCpk[i]);
  }

  void copyArgbsCpk() {
    argbsCpk = new int[JmolConstants.argbsCpk.length];
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      argbsCpk[i] = JmolConstants.argbsCpk[i];
  }

  final static short colixSelectionDefault = Graphics3D.GOLD;

  short colixSelection = colixSelectionDefault;
  void setSelectionArgb(int argb) {
    colixSelection =
      (argb == 0 ? colixSelectionDefault : Graphics3D.getColix(argb));
  }

  short getColixSelection() {
    return colixSelection;
  }

  short colixRubberband = Graphics3D.HOTPINK;
  void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : Graphics3D.getColix(argb));
  }

  int argbBackground;
  short colixBackgroundContrast;
  void setBackgroundArgb(int argb) {
    argbBackground = argb;
    g3d.setBackgroundArgb(argb);
    colixBackgroundContrast =
      ((Graphics3D.calcGreyscaleRgbFromRgb(argb) & 0xFF) < 128
       ? Graphics3D.WHITE : Graphics3D.BLACK);
  }

  void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setBackgroundArgb(Graphics3D.getArgbFromString(colorName));
  }


  /**
   * black or white, whichever contrasts more with the current background
   *
   * @return black or white colix value
   */
  public short getColixBackgroundContrast() {
    return colixBackgroundContrast;
  }

  short getColixAtom(Atom atom) {
    return getColixAtomPalette(atom, "cpk");
  }

  short getColixAtomPalette(Atom atom, String palette) {
    int argb = 0;
    int index;
    if ("cpk" == palette) {
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      short id = atom.getElementNumber();
      return g3d.getChangableColix(id, argbsCpk[id]);
    }
    if ("partialcharge" == palette) {
      // This code assumes that the range of partial charges is [-1, 1].
      index = quantize(atom.getPartialCharge(), -1, 1,
                       JmolConstants.PARTIAL_CHARGE_RANGE_SIZE);
      return
        g3d.getChangableColix((short)(JmolConstants.PARTIAL_CHARGE_COLIX_RED +
                                      index),
                              JmolConstants.argbsRwbScale[index]);
    } else if ("formalcharge" == palette) {
      index = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
      return
        g3d.getChangableColix((short)(JmolConstants.FORMAL_CHARGE_COLIX_RED +
                                      index),
                              JmolConstants.argbsFormalCharge[index]);
    } else if ("temperature" == palette ||
               "fixedtemperature" == palette) {
      float lo,hi;
      if ("temperature" == palette) {
        Frame frame = viewer.getFrame();
        lo = frame.getBfactor100Lo();
        hi = frame.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      index = quantize(atom.getBfactor100(), lo, hi, 
                       JmolConstants.argbsRwbScale.length);
      index = JmolConstants.argbsRwbScale.length - 1 - index;
      argb = JmolConstants.argbsRwbScale[index];
    } else if ("structure" == palette) {
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
    } else if ("amino" == palette) {
      index = atom.getGroupID();
      if (index >= JmolConstants.GROUPID_AMINO_MAX)
        index = 0;
      argb = JmolConstants.argbsAmino[index];
    } else if ("shapely" == palette) {
      index = atom.getGroupID();
      if (index >= JmolConstants.GROUPID_SHAPELY_MAX)
        index = 0;
      argb = JmolConstants.argbsShapely[index];
    } else if ("chain" == palette) {
      int chain = atom.getChainID() & 0x1F;
      if (chain >= JmolConstants.argbsChainAtom.length)
        chain = chain % JmolConstants.argbsChainAtom.length;
      argb = (atom.isHetero()
              ? JmolConstants.argbsChainHetero
              : JmolConstants.argbsChainAtom)[chain];
    } else if ("group" == palette) {
      // viewer.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      index = quantize(atom.getSelectedGroupIndexWithinChain(),
                       0,
                       atom.getSelectedGroupCountWithinChain() - 1,
                       JmolConstants.argbsRoygbScale.length);
      index = JmolConstants.argbsRoygbScale.length - 1 - index;
      argb = JmolConstants.argbsRoygbScale[index];
    } else if ("monomer" == palette) {
      // viewer.calcSelectedMonomersCount() must be called first ...
      index = quantize(atom.getSelectedMonomerIndexWithinPolymer(),
                       0,
                       atom.getSelectedMonomerCountWithinPolymer() - 1,
                       JmolConstants.argbsRoygbScale.length);
      index = JmolConstants.argbsRoygbScale.length - 1 - index;
      argb = JmolConstants.argbsRoygbScale[index];
    } else {
      System.out.println("ColorManager.getColixAtomPalette:" +
                         " unrecognized color palette:" + palette);
      return Graphics3D.HOTPINK;
    }
    // FIXME I think that we should assert that argb != 0 here
    if (argb == 0)
      return Graphics3D.HOTPINK;
    return Graphics3D.getColix(argb);
  }

  int quantize(float val, float lo, float hi, int segmentCount) {
    float range = hi - lo;
    if (range <= 0 || Float.isNaN(val))
      return segmentCount / 2;
    float t = val - lo;
    if (t <= 0)
      return 0;
    float quanta = range / segmentCount;
    int q = (int)(t / quanta + 0.5f);
    if (q >= segmentCount)
      q = segmentCount - 1;
    return q;
  }

  short getColixFromPalette(float val, float lo, float hi, String palette) {
    if (palette == "rwb") {
      int index = quantize(val, lo, hi, JmolConstants.argbsRwbScale.length);
      return Graphics3D.getColix(JmolConstants.argbsRwbScale[index]);
    }
    if (palette == "roygb") {
      int index = quantize(val, lo, hi, JmolConstants.argbsRoygbScale.length);
      return Graphics3D.getColix(JmolConstants.argbsRoygbScale[index]);
    }
    return Graphics3D.HOTPINK;
  }

  short getColixHbondType(short order) {
    int argbIndex = ((order & JmolConstants.BOND_HYDROGEN_MASK)
                     >> JmolConstants.BOND_HBOND_SHIFT);
    return Graphics3D.getColix(JmolConstants.argbsHbondType[argbIndex]);
  }

  void flushCachedColors() {
  }

  private void flushCaches() {
    g3d.flushShadesAndImageCaches();
    viewer.refresh();
  }

  void setSpecular(boolean specular) {
    g3d.setSpecular(specular);
    flushCaches();
  }

  boolean getSpecular() {
    return g3d.getSpecular();
  }

  void setSpecularPower(int specularPower) {
    g3d.setSpecularPower(specularPower);
    flushCaches();
  }

  void setAmbientPercent(int ambientPercent) {
    g3d.setAmbientPercent(ambientPercent);
    flushCaches();
  }

  void setDiffusePercent(int diffusePercent) {
    g3d.setDiffusePercent(diffusePercent);
    flushCaches();
  }

  void setSpecularPercent(int specularPercent) {
    g3d.setSpecularPercent(specularPercent);
    flushCaches();
  }

  void setLightsourceZ(float dist) {
    g3d.setLightsourceZ(dist);
    flushCaches();
  }

  void setElementArgb(int elementNumber, int argb) {
    if (argb == 0) {
      if (argbsCpk == JmolConstants.argbsCpk)
        return;
      argb = JmolConstants.argbsCpk[elementNumber];
    } else
      argb |= 0xFF000000;
    if (argbsCpk == JmolConstants.argbsCpk)
      copyArgbsCpk();
    argbsCpk[elementNumber] = argb;
    g3d.changeColixArgb((short)elementNumber, argb);
  }
}
