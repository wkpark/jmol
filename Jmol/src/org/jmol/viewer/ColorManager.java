/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import org.jmol.script.Token;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Elements;
import org.jmol.util.Logger;

import java.util.BitSet;
import java.util.Map;

import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.util.ColorEncoder;

class ColorManager {
  
  /*
   * 
   * warnings here because I'm trying to implement it in a relatively thread-safe way
   * 
   * must get rid of statics in ColorEncoder
   * 
   * 
   * 
   * 
   * 
   */

  private ColorEncoder colorEncoder;
  private Viewer viewer;
  private Graphics3D g3d;
  private int[] argbsCpk;
  private int[] altArgbsCpk;
  private int currentPalette = 0;
  private boolean currentTranslucent = false;
  private float colorHi, colorLo;
  private float[] colorData;  

  float[] getCurrentColorRange() {
    return new float[] { colorLo, colorHi };
  }

  ColorManager(Viewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    colorEncoder = new ColorEncoder();
    argbsCpk = JmolConstants.argbsCpk;
    altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
  }

  void clear() {
    //causes problems? flushCaches();
  }
  
  private boolean isDefaultColorRasmol;
  boolean getDefaultColorRasmol() {
    return isDefaultColorRasmol;
  }

  void resetElementColors() {
    setDefaultColors(false);
  }
  
  void setDefaultColors(boolean isRasmol) {
    if (isRasmol) {
      isDefaultColorRasmol = true;
      argbsCpk = ColorEncoder.getRasmolScale(true);
    } else {
      isDefaultColorRasmol = false;
      argbsCpk = JmolConstants.argbsCpk;
    }
    altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
    colorEncoder.makeColorScheme((isRasmol ? "Rasmol" : "Jmol"), null, true);
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)i, argbsCpk[i]);
    for (int i = JmolConstants.altArgbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)(Elements.elementNumberMax + i), altArgbsCpk[i]);
  }

  short colixRubberband = Graphics3D.HOTPINK;
  void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : Graphics3D.getColix(argb));
  }

  /*
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  short colixBackgroundContrast;
  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast =
      ((Graphics3D.calcGreyscaleRgbFromRgb(argb) & 0xFF) < 128
       ? Graphics3D.WHITE : Graphics3D.BLACK);
  }

  short getColixBondPalette(Bond bond, byte pid) {
    int argb = 0;
    switch (pid) {
    case JmolConstants.PALETTE_ENERGY:
      return colorEncoder.getColorIndexFromPalette(bond.getEnergy(), 
          -2.5f, -0.5f, ColorEncoder.BWR, false);
    }
    return (argb == 0 ? Graphics3D.RED : Graphics3D.getColix(argb));
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
      return getPropertyColix(atom.getIndex());
    case JmolConstants.PALETTE_NONE:
    case JmolConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      id = atom.getAtomicAndIsotopeNumber();
      if (id < Elements.elementNumberMax)
        return g3d.getChangeableColix(id, argbsCpk[id]);
      id = (short) Elements.altElementIndexFromNumber(id);
      return g3d.getChangeableColix(
          (short) (Elements.elementNumberMax + id), altArgbsCpk[id]);
    case JmolConstants.PALETTE_PARTIAL_CHARGE:
      // This code assumes that the range of partial charges is [-1, 1].
      index = ColorEncoder.quantize(atom.getPartialCharge(), 
          -1, 1, JmolConstants.PARTIAL_CHARGE_RANGE_SIZE);
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
      return colorEncoder.getColorIndexFromPalette(atom.getBfactor100(), 
          lo, hi, ColorEncoder.BWR, false);
    case JmolConstants.PALETTE_STRAIGHTNESS:
      return colorEncoder.getColorIndexFromPalette(atom.getGroupParameter(Token.straightness), 
          -1, 1, ColorEncoder.BWR, false);
    case JmolConstants.PALETTE_SURFACE:
      hi = viewer.getSurfaceDistanceMax();
      return colorEncoder.getColorIndexFromPalette(atom.getSurfaceDistance100(), 
          0, hi, ColorEncoder.BWR, false);
    case JmolConstants.PALETTE_AMINO:
      return colorEncoder.getColorIndexFromPalette(atom
          .getGroupID(), 0, 0, ColorEncoder.AMINO, false);
    case JmolConstants.PALETTE_SHAPELY:
      return colorEncoder.getColorIndexFromPalette(atom
          .getGroupID(), 0, 0, ColorEncoder.SHAPELY, false);
    case JmolConstants.PALETTE_GROUP:
      // viewer.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      return colorEncoder.getColorIndexFromPalette(
          atom.getSelectedGroupIndexWithinChain(), 0,
          atom.getSelectedGroupCountWithinChain() - 1,
          ColorEncoder.BGYOR, false);
    case JmolConstants.PALETTE_POLYMER:
      Model m = viewer.getModelSet().getModels()[atom.modelIndex];
      return colorEncoder.getColorIndexFromPalette(
          atom.getPolymerIndexInModel(), 
          0, m.getBioPolymerCount() - 1,
          ColorEncoder.BGYOR, false);
    case JmolConstants.PALETTE_MONOMER:
      // viewer.calcSelectedMonomersCount() must be called first ...
      return colorEncoder.getColorIndexFromPalette(
          atom.getSelectedMonomerIndexWithinPolymer(), 
          0, atom.getSelectedMonomerCountWithinPolymer() - 1,
          ColorEncoder.BGYOR, false);
    case JmolConstants.PALETTE_MOLECULE:
      modelSet = viewer.getModelSet();
      return colorEncoder.getColorIndexFromPalette(
          modelSet.getMoleculeIndex(atom.getIndex()), 
          0, modelSet.getMoleculeCountInModel(atom.getModelIndex()) - 1, 
          ColorEncoder.ROYGB, false);
    case JmolConstants.PALETTE_ALTLOC:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return colorEncoder.getColorIndexFromPalette(
          modelSet.getAltLocIndexInModel(modelIndex,
          atom.getAlternateLocationID()), 
          0, modelSet.getAltLocCountInModel(modelIndex),
          ColorEncoder.ROYGB, false);
    case JmolConstants.PALETTE_INSERTION:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return colorEncoder.getColorIndexFromPalette(
          modelSet.getInsertionCodeIndexInModel(
          modelIndex, atom.getInsertionCode()), 
          0, modelSet.getInsertionCountInModel(modelIndex),
          ColorEncoder.ROYGB, false);
    case JmolConstants.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.jmol);
      break;
    case JmolConstants.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.rasmol);
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType() + 1];
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
    }
    return (argb == 0 ? Graphics3D.HOTPINK : Graphics3D.getColix(argb));
  }

  private short getPropertyColix(int iAtom) {
    if (colorData == null || iAtom >= colorData.length)
      return Graphics3D.GRAY;
    return getColixForPropertyValue(colorData[iAtom]);    
  }

  private int getJmolOrRasmolArgb(int id, int argb) {
    switch (argb) {
    case Token.jmol:
      if (id >= Elements.elementNumberMax)
        break;
      return colorEncoder.getArgbFromPalette(id, 0, 0, ColorEncoder.JMOL);
    case Token.rasmol:
      if (id >= Elements.elementNumberMax)
        break;
      return colorEncoder.getArgbFromPalette(id, 0, 0, ColorEncoder.RASMOL);
    default:
      return argb;
    }
    return JmolConstants.altArgbsCpk[Elements
        .altElementIndexFromNumber(id)];
  }

  void setElementArgb(int id, int argb) {
    if (argb == Token.jmol && argbsCpk == JmolConstants.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(id, argb);
    if (argbsCpk == JmolConstants.argbsCpk) {
      argbsCpk = ArrayUtil.arrayCopy(JmolConstants.argbsCpk, 0, -1, false);
      altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
    }
    if (id < Elements.elementNumberMax) {
      argbsCpk[id] = argb;
      g3d.changeColixArgb((short)id, argb);
      return;
    }
    id = Elements.altElementIndexFromNumber(id);
    altArgbsCpk[id] = argb;
    g3d.changeColixArgb((short) (Elements.elementNumberMax + id), argb);
  }
  
  void setCurrentColorRange(float[] data, BitSet bs, String colorScheme) {
    colorData = data;
    currentPalette = colorEncoder.getColorScheme(colorScheme, true, false);
    colorHi = Float.MIN_VALUE;
    colorLo = Float.MAX_VALUE;
    if (data == null)
      return;
    boolean isAll = (bs == null);
    float d;
    int i0 = (isAll ? data.length - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      if (Float.isNaN(d = data[i]))
        continue;
      colorHi = Math.max(colorHi, d);
      colorLo = Math.min(colorLo, d);
    }
    setCurrentColorRange(colorLo, colorHi);
  }  

  void setCurrentColorRange(float min, float max) {
    colorLo = min;
    colorHi = max;
    Logger.info("ColorManager: color \"" + colorEncoder.getColorSchemeName(currentPalette) + "\" range " + colorLo + " " + colorHi);
  }

  int setColorScheme(String colorScheme, boolean isTranslucent, boolean isOverloaded) {
    currentTranslucent = isTranslucent;
    currentPalette = colorEncoder.getColorScheme(colorScheme, true, isOverloaded);
    setCurrentColorRange(colorLo, colorHi);
    return currentPalette;
  }

  String getState(StringBuffer sfunc) {
    StringBuffer s = new StringBuffer();
    int n = 0;
    for (Map.Entry<String, int[]> entry : colorEncoder.schemes.entrySet()) {
      String name = entry.getKey();
      if (name.length() > 0 & n++ >= 0) 
        s.append("color \"" + name + "=" + colorEncoder.getColorSchemeList(entry.getValue()) + "\";\n");
    }
    
    //String colors = getColorSchemeList(getColorSchemeArray(USER));
    //if (colors.length() > 0)
      //s.append("userColorScheme = " + colors + ";\n");
    if (n > 0 && sfunc != null)
      sfunc.append("\n  _setColorState\n");
    return (n > 0 && sfunc != null ? "function _setColorState() {\n" 
        + s.append("}\n\n").toString() : s.toString());
  }
  
  void setUserScale(int[] scale) {
    colorEncoder.setUserScale(scale);
  }
  
  String getColorSchemeList(String colorScheme, boolean ifDefault) {
    // isosurface sets ifDefault FALSE so that any default schemes are returned
    int iPt = (ifDefault && (colorScheme == null || colorScheme.length() == 0) ? 
        currentPalette : ColorEncoder.getColorScheme(colorScheme, true, false));
    return (ifDefault || iPt < 0 ? ColorEncoder.getColorSchemeList(colorEncoder.getColorSchemeArray(iPt)) : colorScheme);
  }
  
  short getColixForPropertyValue(float val) {
    return (colorLo < colorHi ? 
        colorEncoder.getColorIndexFromPalette(val, colorLo, colorHi, currentPalette, currentTranslucent)
        :colorEncoder.getColorIndexFromPalette(-val, -colorLo, -colorHi, currentPalette, currentTranslucent));    
  }
}
