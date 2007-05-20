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

import org.jmol.util.Logger;
import java.util.BitSet;
import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.ColorEncoder;

class ColorManager {

  Viewer viewer;
  Graphics3D g3d;
  int[] argbsCpk;
  int[] altArgbsCpk;

  ColorManager(Viewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    argbsCpk = JmolConstants.argbsCpk;
    altArgbsCpk = new int[JmolConstants.altArgbsCpk.length];
    for (int i = JmolConstants.altArgbsCpk.length; --i >= 0; )
      altArgbsCpk[i] = JmolConstants.altArgbsCpk[i];
  }

  boolean isDefaultColorRasmol;
  boolean getDefaultColorRasmol() {
    return isDefaultColorRasmol;
  }

  void resetElementColors() {
    setDefaultColors("Jmol");
  }
  
  void setDefaultColors(String colorScheme) {
    if (colorScheme.equalsIgnoreCase("Jmol")) {
      isDefaultColorRasmol = false;
      argbsCpk = JmolConstants.argbsCpk;
      //viewer.setStringProperty("backgroundColor", "black");
      //must be a very old idea -- clear dots color with
      //default color reset, but that makes no sense now.
      //viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
      //if colors are reset with dots or labels or anything else
      //defined, they are automatically reset.
    } else if (colorScheme.equalsIgnoreCase("RasMol")) {
      isDefaultColorRasmol = true;
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
      //viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
    } else {
      Logger.error("unrecognized color scheme");
      return;
    }
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)i, argbsCpk[i]);
    for (int i = JmolConstants.altArgbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)(JmolConstants.elementNumberMax + i), altArgbsCpk[i]);
  }

  void copyArgbsCpk() {
    argbsCpk = new int[JmolConstants.argbsCpk.length];
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      argbsCpk[i] = JmolConstants.argbsCpk[i];
    altArgbsCpk = new int[JmolConstants.altArgbsCpk.length];
    for (int i = JmolConstants.altArgbsCpk.length; --i >= 0; )
      altArgbsCpk[i] = JmolConstants.altArgbsCpk[i];
  }

  
  short colixRubberband = Graphics3D.HOTPINK;
  void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : Graphics3D.getColix(argb));
  }

  short colixBackgroundContrast;
  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast =
      ((Graphics3D.calcGreyscaleRgbFromRgb(argb) & 0xFF) < 128
       ? Graphics3D.WHITE : Graphics3D.BLACK);
  }

  /**
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  short getColixBackgroundContrast() {
    //not implemented
    return colixBackgroundContrast;
  }

  short getColixAtomPalette(Atom atom, byte pid) {
    int argb = 0;
    int index;
    short id;
    ModelSet modelSet;
    int modelIndex;
    float lo, hi;
    switch (pid) {
    case JmolConstants.PALETTE_PROPERTY:
      return getPropertyColix(atom.getAtomIndex());
    case JmolConstants.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.jmol);
      break;
    case JmolConstants.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.rasmol);
      break;
    case JmolConstants.PALETTE_NONE:
    case JmolConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      id = atom.getAtomicAndIsotopeNumber();
      if (id < 256)
        return g3d.getChangeableColix(id, argbsCpk[id]);
      id = (short) JmolConstants.altElementIndexFromNumber(id);
      return g3d.getChangeableColix((short) (JmolConstants.elementNumberMax + id),
          altArgbsCpk[id]);
      case JmolConstants.PALETTE_PARTIAL_CHARGE:
        // This code assumes that the range of partial charges is [-1, 1].
        index = ColorEncoder.quantize(atom.getPartialCharge(), -1, 1,
            JmolConstants.PARTIAL_CHARGE_RANGE_SIZE);
        return g3d.getChangeableColix(
            (short) (JmolConstants.PARTIAL_CHARGE_COLIX_RED + index),
            JmolConstants.argbsRwbScale[index]);
      case JmolConstants.PALETTE_FORMAL_CHARGE:
        index = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
        return g3d.getChangeableColix(
            (short) (JmolConstants.FORMAL_CHARGE_COLIX_RED + index),
            JmolConstants.argbsFormalCharge[index]);
      case JmolConstants.PALETTE_TEMP:
      case JmolConstants.PALETTE_FIXEDTEMP:
      if (pid == JmolConstants.PALETTE_TEMP) {
        modelSet = viewer.getModelSet();
        lo = modelSet.getBfactor100Lo();
        hi = modelSet.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      index = ColorEncoder.quantize(atom.getBfactor100(), lo, hi,
          JmolConstants.argbsRwbScale.length);
      index = JmolConstants.argbsRwbScale.length - 1 - index;
      argb = JmolConstants.argbsRwbScale[index];
      break;
    case JmolConstants.PALETTE_SURFACE:
      hi = viewer.getModelSet().getSurfaceDistanceMax();
      index = ColorEncoder.quantize(atom.getSurfaceDistance100(), 0, hi,
          JmolConstants.argbsRwbScale.length);
      //index = JmolConstants.argbsRwbScale.length - 1 - index;
      argb = JmolConstants.argbsRwbScale[index];
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
      break;
    case JmolConstants.PALETTE_AMINO:
      index = atom.getGroupID();
      if (index < 0 || index >= JmolConstants.GROUPID_AMINO_MAX)
        index = 0;
      argb = JmolConstants.argbsAmino[index];
      break;
    case JmolConstants.PALETTE_SHAPELY:
      index = atom.getGroupID();
      if (index < 0 || index >= JmolConstants.GROUPID_SHAPELY_MAX)
        index = 0;
      argb = JmolConstants.argbsShapely[index];
      break;
    case JmolConstants.PALETTE_CHAIN:
      int chain = atom.getChainID() & 0x1F;
      if (chain < 0)
        chain = 0;
      if (chain >= JmolConstants.argbsChainAtom.length)
        chain = chain % JmolConstants.argbsChainAtom.length;
      argb = (atom.isHetero() ? JmolConstants.argbsChainHetero
          : JmolConstants.argbsChainAtom)[chain];
      break;
    case JmolConstants.PALETTE_GROUP:
      // viewer.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      index = ColorEncoder.quantize(atom.getSelectedGroupIndexWithinChain(), 0, atom
          .getSelectedGroupCountWithinChain() - 1,
          JmolConstants.argbsRoygbScale.length);
      //I guess this is "blue to red" for some reason -- also for color xxx monomer
      index = JmolConstants.argbsRoygbScale.length - 1 - index;
      argb = JmolConstants.argbsRoygbScale[index];
      break;
    case JmolConstants.PALETTE_MONOMER:
      // viewer.calcSelectedMonomersCount() must be called first ...
      index = ColorEncoder.quantize(atom.getSelectedMonomerIndexWithinPolymer(), 0, atom
          .getSelectedMonomerCountWithinPolymer() - 1,
          JmolConstants.argbsRoygbScale.length);
      index = JmolConstants.argbsRoygbScale.length - 1 - index;
      argb = JmolConstants.argbsRoygbScale[index];
      break;
    case JmolConstants.PALETTE_MOLECULE:
      modelSet = viewer.getModelSet();
      index = ColorEncoder.quantize(modelSet.getMoleculeIndex(atom.getAtomIndex()), 0, modelSet
          .getMoleculeCountInModel(atom.getModelIndex()) - 1,
          JmolConstants.argbsRoygbScale.length);
      argb = JmolConstants.argbsRoygbScale[index];
      break;
    case JmolConstants.PALETTE_ALTLOC:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      index = ColorEncoder.quantize(modelSet.getAltLocIndexInModel(modelIndex,
          (char) atom.getAlternateLocationID()), 0, modelSet
          .getAltLocCountInModel(modelIndex),
          JmolConstants.argbsRoygbScale.length);
      argb = JmolConstants.argbsRoygbScale[index];
      break;
    case JmolConstants.PALETTE_INSERTION:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      index = ColorEncoder.quantize(modelSet.getInsertionCodeIndexInModel(modelIndex, atom
          .getInsertionCode()), 0, modelSet
          .getInsertionCountInModel(modelIndex),
          JmolConstants.argbsRoygbScale.length);
      argb = JmolConstants.argbsRoygbScale[index];
      break;
    }
    return (argb == 0 ? Graphics3D.HOTPINK : Graphics3D.getColix(argb));
  }

  private float colorHi, colorLo;
  private float[] colorData;
  
  void setCurrentColorRange(float[] data, BitSet bs, String colorScheme) {
    colorData = data;
    setColorScheme(colorScheme);
    colorHi = Float.MIN_VALUE;
    colorLo = Float.MAX_VALUE;
    if (data == null)
      return;
    for (int i = data.length; --i >= 0;)
      if (bs == null || bs.get(i)) {
        float d = data[i];
        if (Float.isNaN(d))
          continue;
        colorHi = Math.max(colorHi, d);
        colorLo = Math.min(colorLo, d);
      }
    setCurrentColorRange(colorLo, colorHi);
  }  

  void setCurrentColorRange(float min, float max) {
    colorHi = max;
    colorLo = min;
    Logger.info("Property color value range: " + colorLo + " to " + colorHi);
  }

  short getPropertyColix(int iAtom) {
    if (colorData == null || iAtom >= colorData.length)
      return Graphics3D.GRAY;
    return getColixFromPalette(colorData[iAtom], colorLo, colorHi);
  }

  int palette = 0;
  
/* didn't work
  private int rgbRed = 0xFFFF0000;
  private int rgbGreen = 0xFF008000;
  private int rgbBlue = 0xFF0000FF;
  void setRgb(int rgorb, int color) {
    switch (rgorb) {
    case 0:
      rgbRed = color;
      break;
    case 1:
      rgbGreen = color;
      break;
    case 2:
      rgbBlue = color;
    }
  }
  
  int getRgb(int rgorb) {
    switch (rgorb) {
    case 0:
      return rgbRed;
    case 1:
      return rgbGreen;
    case 2:
    default:
      return rgbBlue;
    }
  }
  
  int getRgbRed() {
    return rgbRed;
  }

  int getRgbGreen() {
    return rgbGreen;
  }

  int getRgbBlue() {
    return rgbBlue;
  }
*/  
  int setColorScheme(String colorScheme) {
    return palette = ColorEncoder.getColorScheme(colorScheme);
  }
  
  short getColixFromPalette(float val, float lo, float hi) {
    return ColorEncoder.getColorIndexFromPalette(val, lo, hi, palette); //, rgbRed, rgbGreen, rgbBlue);    
  }

  static short getColixHbondType(short order) {
    int argbIndex = ((order & JmolConstants.BOND_HYDROGEN_MASK)
                     >> JmolConstants.BOND_HBOND_SHIFT);
    return Graphics3D.getColix(JmolConstants.argbsHbondType[argbIndex]);
  }

  /*
  void flushCachedColors() {
  }
  */
  
  private static void flushCaches() {
    Graphics3D.flushShadesAndSphereCaches();
  }

  static void setSpecular(boolean specular) {
    if (Graphics3D.getSpecular() == specular)
      return;
    Graphics3D.setSpecular(specular);
    flushCaches();
  }

  static boolean getSpecular() {
    return Graphics3D.getSpecular();
  }

  static void setSpecularPercent(int specularPercent) {
    if (Graphics3D.getSpecularPercent() == specularPercent)
      return;
    Graphics3D.setSpecularPercent(specularPercent);
    flushCaches();
  }
  
  static int getSpecularPercent() {
    return Graphics3D.getSpecularPercent();
  }

  static void setSpecularPower(int specularPower) {
    if (specularPower < 0) {
      if (Graphics3D.getSpecularExponent() == -specularPower)
        return;
      Graphics3D.setSpecularExponent(-specularPower);
    } else {
      if (Graphics3D.getSpecularPower() == specularPower)
        return;
      Graphics3D.setSpecularPower(specularPower);
    }
    flushCaches();
  }
  
  static void setDiffusePercent(int diffusePercent) {
    if (Graphics3D.getDiffusePercent() == diffusePercent)
      return;
    Graphics3D.setDiffusePercent(diffusePercent);
    flushCaches();
  }

  static int getDiffusePercent() {
    return Graphics3D.getDiffusePercent();
  }

  static void setAmbientPercent(int ambientPercent) {
    if (Graphics3D.getAmbientPercent() == ambientPercent)
      return;
    Graphics3D.setAmbientPercent(ambientPercent);
    flushCaches();
  }

  static int getAmbientPercent() {
    return Graphics3D.getAmbientPercent();
  }

  /*
  void setLightsourceZ(float dist) {
    Graphics3D.setLightsourceZ(dist);
    flushCaches();
  }
  */
  
  static int getJmolOrRasmolArgb(int id, int argb) {
    if (argb == Token.jmol) {
      return (id < 256 ? JmolConstants.argbsCpk[id]
          : JmolConstants.altArgbsCpk[JmolConstants
              .altElementIndexFromNumber(id)]);
    }
    if (argb == Token.rasmol) {
      if (id >= 256)
        return JmolConstants.altArgbsCpk[JmolConstants
            .altElementIndexFromNumber(id)];
      argb = JmolConstants.argbsCpk[id];
      for (int i = JmolConstants.argbsCpkRasmol.length; --i >= 0;) {
        int argbRasmol = JmolConstants.argbsCpkRasmol[i];
        int atomNo = argbRasmol >> 24;
        if (atomNo == id) {
          argb = argbRasmol | 0xFF000000;
          break;
        }
      }
      return argb;
    }
    return argb | 0xFF000000;
  }

  void setElementArgb(int id, int argb) {
    if (argb == Token.jmol && argbsCpk == JmolConstants.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(id, argb);
    if (argbsCpk == JmolConstants.argbsCpk)
      copyArgbsCpk();
    if (id < 256) {
      argbsCpk[id] = argb;
      g3d.changeColixArgb((short)id, argb);
      return;
    }
    id = JmolConstants.altElementIndexFromNumber(id);
    altArgbsCpk[JmolConstants.altElementIndexFromNumber(id)] = argb;
    g3d.changeColixArgb((short) (JmolConstants.elementNumberMax + id), argb);
  }
}
