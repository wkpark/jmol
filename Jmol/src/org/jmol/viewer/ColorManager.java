/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

class ColorManager {

  Viewer viewer;
  Graphics3D g3d;
  int[] argbsCpk;

  byte paletteDefault = JmolConstants.PALETTE_NONE_CPK;

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
      int argb = JmolConstants.argbsCpkRasmol[0] | 0xFF000000;
      argbsCpk = new int[JmolConstants.argbsCpk.length];
      for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
        argbsCpk[i] = argb;
      for (int i = JmolConstants.argbsCpkRasmol.length; --i >= 0; ) {
        argb = JmolConstants.argbsCpkRasmol[i];
        int atomNo = argb >> 24;
        argb |= 0xFF000000;
        argbsCpk[atomNo] = argb;
        g3d.changeColixArgb((short)atomNo, argb);
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

  void setPaletteDefault(byte palette) {
    paletteDefault = palette;
  }

  byte getPaletteDefault() {
    return paletteDefault;
  }

  Color colorSelection = Color.orange;
  short colixSelection = Graphics3D.ORANGE;

  void setColorSelection(Color c) {
    colorSelection = c;
    colixSelection = g3d.getColix(c);
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
    colixRubberband = g3d.getColix(color);
  }

  boolean isBondAtomColor = true;
  void setIsBondAtomColor(boolean isBondAtomColor) {
    this.isBondAtomColor = isBondAtomColor;
  }

  Color colorBond = null;
  short colixBond = 0;
  void setColorBond(Color c) {
    colorBond = c;
    colixBond = g3d.getColix(c);
  }

  Color colorHbond = null;
  short colixHbond = 0;
  void setColorHbond(Color c) {
    colorHbond = c;
    colixHbond = g3d.getColix(c);
  }

  Color colorSsbond = null;
  short colixSsbond = 0;
  void setColorSsbond(Color c) {
    colorSsbond = c;
    colixSsbond = g3d.getColix(c);
  }

  Color colorLabel = Color.black;
  short colixLabel = Graphics3D.BLACK;
  void setColorLabel(Color color) {
    colorLabel = color;
    colixLabel = g3d.getColix(color);
  }

  short colixDotsConvex = 0;
  short colixDotsConcave = 0;
  short colixDotsSaddle = 0;

  void setColorDotsConvex(Color color) {
    colixDotsConvex = g3d.getColix(color);
  }
  void setColorDotsConcave(Color color) {
    colixDotsConcave = g3d.getColix(color);
  }
  void setColorDotsSaddle(Color color) {
    colixDotsSaddle = g3d.getColix(color);
  }

  Color colorDistance = Color.white;
  short colixDistance = Graphics3D.WHITE;
  void setColorDistance(Color c) {
    colorDistance = c;
    colixDistance = g3d.getColix(c);
  }

  Color colorAngle = Color.white;
  short colixAngle = Graphics3D.WHITE;
  void setColorAngle(Color c) {
    colorAngle = c;
    colixAngle = g3d.getColix(c);
  }

  Color colorTorsion = Color.white;
  short colixTorsion = Graphics3D.WHITE;
  void setColorTorsion(Color c) {
    colorTorsion = c;
    colixTorsion = g3d.getColix(c);
  }

  void setColorMeasurement(Color c) {
    colorDistance = colorAngle = colorTorsion = c;
    colixDistance = colixAngle = colixTorsion = g3d.getColix(c);
  }

  Color colorBackground = Color.white;
  short colixBackground = Graphics3D.WHITE;
  void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    colixBackground = g3d.getColix(colorBackground);
    g3d.setBackground(colixBackground);
  }

  Color colorAxes = new Color(128, 128, 0);
  short colixAxes = Graphics3D.OLIVE;
  void setColorAxes(Color color) {
    colorAxes = color;
    colixAxes = g3d.getColix(color);
  }

  Color colorAxesText = colorAxes;
  short colixAxesText = Graphics3D.OLIVE;
  void setColorAxesText(Color color) {
    colorAxesText = color;
    colixAxesText = g3d.getColix(color);
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  Color colorVector = Color.black;
  short colixVector = Graphics3D.BLACK;
  void setColorVector(Color c) {
    colorVector = c;
    colixVector = g3d.getColix(c);
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

  short getColixAtomPalette(Atom atom, byte palette) {
    int argb = 0;
    int index;
    switch (palette) {
    case JmolConstants.PALETTE_NONE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      short id = atom.getElementNumber();
      return g3d.getChangableColix(id, argbsCpk[id]);
    case JmolConstants.PALETTE_PARTIALCHARGE:
      /*
        This code assumes that the range of partial charges is
        [-1, 1].
        It also explicitly constructs colors red (negative) and
        blue (positive)
        Using colors other than these would make the shading
        calculations more difficult
      */
      index = quantize(-1, 1, atom.getPartialCharge(),
                       JmolConstants.argbsRwbScale.length);
      argb = JmolConstants.argbsRwbScale[index];
      break;
    case JmolConstants.PALETTE_TEMPERATURE:
    case JmolConstants.PALETTE_FIXEDTEMP:
      float lo,hi;
      if (palette == JmolConstants.PALETTE_TEMPERATURE) {
        Frame frame = viewer.getFrame();
        lo = frame.getBfactor100Lo();
        hi = frame.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      index = quantize(lo, hi, atom.getBfactor100(),
                       JmolConstants.argbsRwbScale.length);
      index = JmolConstants.argbsRwbScale.length - 1 - index;
      argb = JmolConstants.argbsRwbScale[index];
      break;
    case JmolConstants.PALETTE_FORMALCHARGE:
      index = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
      argb = JmolConstants.argbsCharge[index];
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
      break;
    case JmolConstants.PALETTE_AMINO:
      index = atom.getGroupID();
      if (index >= JmolConstants.GROUPID_AMINO_MAX)
        index = 0;
      argb = JmolConstants.argbsAmino[index];
      break;
    case JmolConstants.PALETTE_SHAPELY:
      index = atom.getGroupID();
      if (index >= JmolConstants.GROUPID_SHAPELY_MAX)
        index = 0;
      argb = JmolConstants.argbsShapely[index];
      break;
    case JmolConstants.PALETTE_CHAIN:
      int chain = atom.getChainID() & 0x1F;
      if (chain >= JmolConstants.argbsChainAtom.length)
        chain = chain % JmolConstants.argbsChainAtom.length;
      argb = (atom.isHetero()
              ? JmolConstants.argbsChainHetero
              : JmolConstants.argbsChainAtom)[chain];
      break;
    case JmolConstants.PALETTE_GROUP:
      index = quantize(0,
                       atom.getSelectedGroupCountWithinChain() - 1,
                       atom.getSelectedGroupIndexWithinChain(),
                       JmolConstants.argbsBlueRedRainbow.length);
      argb = JmolConstants.argbsBlueRedRainbow[index];
      break;
    case JmolConstants.PALETTE_MONOMER:
      index = quantize(0,
                       atom.getSelectedMonomerCountWithinPolymer() - 1,
                       atom.getSelectedMonomerIndexWithinPolymer(),
                       JmolConstants.argbsBlueRedRainbow.length);
      argb = JmolConstants.argbsBlueRedRainbow[index];
      break;
    }
    if (argb == 0)
      return Graphics3D.HOTPINK;
    return g3d.getColix(argb);
  }

  int quantize(float lo, float hi, float val, int segmentCount) {
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

  short getColixHbondType(short order) {
    int argbIndex = ((order & JmolConstants.BOND_HYDROGEN_MASK)
                     >> JmolConstants.BOND_HBOND_SHIFT);
    return g3d.getColix(JmolConstants.argbsHbondType[argbIndex]);
  }

  void flushCachedColors() {
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();
  final Vector3f vNormal = new Vector3f();
  final Vector3f vRotated = new Vector3f();

  int calcSurfaceIntensity(Point3f pA, Point3f pB, Point3f pC) {
    vAB.sub(pB, pA);
    vAC.sub(pC, pA);
    vNormal.cross(vAB, vAC);
    viewer.transformVector(vNormal, vRotated);
    int intensity =
      vRotated.z >= 0
      ? calcIntensity(-vRotated.x, -vRotated.y, vRotated.z)
      : calcIntensity(vRotated.x, vRotated.y, -vRotated.z);
    if (intensity > Graphics3D.intensitySpecularSurfaceLimit)
      intensity = Graphics3D.intensitySpecularSurfaceLimit;
    return intensity;
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

  int calcIntensity(float x, float y, float z) {
    return Graphics3D.calcIntensity(x, y, z);
  }
}
