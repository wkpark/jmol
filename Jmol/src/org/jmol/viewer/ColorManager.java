/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import org.jmol.g3d.*;

import java.awt.Color;

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
      viewer.setColorBackground(Color.black);
      viewer.setColorMeasurement(null);
      viewer.setColorLabel(Color.white);
      viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, null);
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
      viewer.setColorBackground(Color.black);
      viewer.setColorMeasurement(Color.white);
      viewer.setColorLabel(null);
      viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, null);
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

  String paletteDefault = "cpk";

  void setPaletteDefault(String palette) {
    paletteDefault = palette.intern();
  }

  /*
  byte getPaletteDefault() {
    return paletteDefault;
  }
  */

  final static Color colorSelectionDefault = Graphics3D.COLOR_GOLD;
  final static short colixSelectionDefault = Graphics3D.GOLD;

  Color colorSelection = colorSelectionDefault;
  short colixSelection = colixSelectionDefault;

  void setColorSelection(Color c) {
    if (c == null)
      c = colorSelectionDefault;
    colorSelection = c;
    colixSelection = Graphics3D.getColix(c);
  }

  Color getColorSelection() {
    return colorSelection;
  }

  short getColixSelection() {
    return colixSelection;
  }

  Color colorRubberband = Color.pink;
  short colixRubberband = Graphics3D.HOTPINK;
  Color getColorRubberband() {
    return colorRubberband;
  }

  short getColixRubberband() {
    return colixRubberband;
  }

  void setColorRubberband(Color color) {
    if (color == null)
      color = Color.pink;
    colorRubberband = color;
    colixRubberband = Graphics3D.getColix(color);
  }

  boolean isBondAtomColor = true;
  void setIsBondAtomColor(boolean isBondAtomColor) {
    this.isBondAtomColor = isBondAtomColor;
  }

  Color colorBond = null;
  short colixBond = 0;
  void setColorBond(Color c) {
    colorBond = c;
    colixBond = Graphics3D.getColix(c);
  }

  Color colorHbond = null;
  short colixHbond = 0;
  void setColorHbond(Color c) {
    colorHbond = c;
    colixHbond = Graphics3D.getColix(c);
  }

  Color colorSsbond = null;
  short colixSsbond = 0;
  void setColorSsbond(Color c) {
    colorSsbond = c;
    colixSsbond = Graphics3D.getColix(c);
  }

  Color colorLabel = Color.black;
  short colixLabel = Graphics3D.BLACK;
  void setColorLabel(Color color) {
    colorLabel = color;
    colixLabel = Graphics3D.getColix(color);
  }

  short colixDotsConvex = 0;
  short colixDotsConcave = 0;
  short colixDotsSaddle = 0;

  void setColorDotsConvex(Color color) {
    colixDotsConvex = Graphics3D.getColix(color);
  }
  void setColorDotsConcave(Color color) {
    colixDotsConcave = Graphics3D.getColix(color);
  }
  void setColorDotsSaddle(Color color) {
    colixDotsSaddle = Graphics3D.getColix(color);
  }

  Color colorDistance = Color.white;
  short colixDistance = Graphics3D.WHITE;
  void setColorDistance(Color c) {
    colorDistance = c;
    colixDistance = Graphics3D.getColix(c);
  }

  Color colorAngle = Color.white;
  short colixAngle = Graphics3D.WHITE;
  void setColorAngle(Color c) {
    colorAngle = c;
    colixAngle = Graphics3D.getColix(c);
  }

  Color colorTorsion = Color.white;
  short colixTorsion = Graphics3D.WHITE;
  void setColorTorsion(Color c) {
    colorTorsion = c;
    colixTorsion = Graphics3D.getColix(c);
  }

  void setColorMeasurement(Color c) {
    colorDistance = colorAngle = colorTorsion = c;
    colixDistance = colixAngle = colixTorsion = Graphics3D.getColix(c);
  }

  Color colorBackground = Color.white;
  short colixBackground = Graphics3D.WHITE;
  void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    colixBackground = Graphics3D.getColix(colorBackground);
    g3d.setBackground(colixBackground);
  }

  Color colorAxes = new Color(128, 128, 0);
  short colixAxes = Graphics3D.OLIVE;
  void setColorAxes(Color color) {
    colorAxes = color;
    colixAxes = Graphics3D.getColix(color);
  }

  Color colorAxesText = colorAxes;
  short colixAxesText = Graphics3D.OLIVE;
  void setColorAxesText(Color color) {
    colorAxesText = color;
    colixAxesText = Graphics3D.getColix(color);
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  Color colorVector = Color.black;
  short colixVector = Graphics3D.BLACK;
  void setColorVector(Color c) {
    colorVector = c;
    colixVector = Graphics3D.getColix(c);
  }
  Color getColorVector() {
    return colorVector;
  }

  void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setColorBackground(viewer.getColorFromString(colorName));
  }

  short getColixAtom(Atom atom) {
    return getColixAtomPalette(atom, paletteDefault);
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

  void setElementColor(int elementNumber, Color color) {
    int argb;
    if (color == null) {
      if (argbsCpk == JmolConstants.argbsCpk)
        return;
      argb = JmolConstants.argbsCpk[elementNumber];
    } else
      argb = color.getRGB() | 0xFF000000;
    if (argbsCpk == JmolConstants.argbsCpk)
      copyArgbsCpk();
    argbsCpk[elementNumber] = argb;
    g3d.changeColixArgb((short)elementNumber, argb);
  }
}
