/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.managers;

import org.jmol.g3d.*;
import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.script.Token;

import java.awt.Color;
import java.util.Hashtable;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

public class ColorManager {

  JmolViewer viewer;
  Graphics3D g3d;
  int[] argbsCpk;

  public byte paletteDefault = JmolConstants.PALETTE_NONE_CPK;

  public ColorManager(JmolViewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    argbsCpk = JmolConstants.argbsCpk;
  }

  public void setColorScheme(String colorScheme) {
    System.out.println("setting color scheme to:" + colorScheme);
    if (colorScheme.equals("jmol")) {
      argbsCpk = JmolConstants.argbsCpk;
      viewer.setColorBackground(Color.black);
      viewer.setColorMeasurement(Color.white);
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

  public void setPaletteDefault(byte palette) {
    paletteDefault = palette;
  }

  public byte getPaletteDefault() {
    return paletteDefault;
  }

  public Color colorSelection = Color.orange;
  public short colixSelection = Graphics3D.ORANGE;

  public void setColorSelection(Color c) {
    colorSelection = c;
    colixSelection = g3d.getColix(c);
  }

  public Color getColorSelection() {
    return colorSelection;
  }

  public short getColixSelection() {
    return colixSelection;
  }

  public Color colorRubberband = Color.pink;
  public short colixRubberband = Graphics3D.PINK;
  public Color getColorRubberband() {
    return colorRubberband;
  }

  public short getColixRubberband() {
    return colixRubberband;
  }

  public boolean isBondAtomColor = true;
  public void setIsBondAtomColor(boolean isBondAtomColor) {
    this.isBondAtomColor = isBondAtomColor;
  }

  public Color colorBond = null;
  public short colixBond = 0;
  public void setColorBond(Color c) {
    colorBond = c;
    colixBond = g3d.getColix(c);
  }

  public Color colorHbond = null;
  public short colixHbond = 0;
  public void setColorHbond(Color c) {
    colorHbond = c;
    colixHbond = g3d.getColix(c);
  }

  public Color colorSsbond = null;
  public short colixSsbond = 0;
  public void setColorSsbond(Color c) {
    colorSsbond = c;
    colixSsbond = g3d.getColix(c);
  }

  public Color colorLabel = Color.black;
  public short colixLabel = Graphics3D.BLACK;
  public void setColorLabel(Color color) {
    colorLabel = color;
    colixLabel = g3d.getColix(color);
  }

  public short colixDotsConvex = 0;
  public short colixDotsConcave = 0;
  public short colixDotsSaddle = 0;

  public void setColorDotsConvex(Color color) {
    colixDotsConvex = g3d.getColix(color);
  }
  public void setColorDotsConcave(Color color) {
    colixDotsConcave = g3d.getColix(color);
  }
  public void setColorDotsSaddle(Color color) {
    colixDotsSaddle = g3d.getColix(color);
  }

  public Color colorDistance = Color.white;
  public short colixDistance = Graphics3D.WHITE;
  public void setColorDistance(Color c) {
    colorDistance = c;
    colixDistance = g3d.getColix(c);
  }

  public Color colorAngle = Color.white;
  public short colixAngle = Graphics3D.WHITE;
  public void setColorAngle(Color c) {
    colorAngle = c;
    colixAngle = g3d.getColix(c);
  }

  public Color colorTorsion = Color.white;
  public short colixTorsion = Graphics3D.WHITE;
  public void setColorTorsion(Color c) {
    colorTorsion = c;
    colixTorsion = g3d.getColix(c);
  }

  public void setColorMeasurement(Color c) {
    colorDistance = colorAngle = colorTorsion = c;
    colixDistance = colixAngle = colixTorsion = g3d.getColix(c);
  }

  public Color colorBackground = Color.white;
  public short colixBackground = Graphics3D.WHITE;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    colixBackground = g3d.getColix(colorBackground);
    g3d.setBackground(colixBackground);
  }

  public Color colorAxes = new Color(128, 128, 0);
  public short colixAxes = Graphics3D.OLIVE;
  public void setColorAxes(Color color) {
    colorAxes = color;
    colixAxes = g3d.getColix(color);
  }

  public Color colorAxesText = colorAxes;
  public short colixAxesText = Graphics3D.OLIVE;
  public void setColorAxesText(Color color) {
    colorAxesText = color;
    colixAxesText = g3d.getColix(color);
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  public Color colorVector = Color.black;
  public short colixVector = Graphics3D.BLACK;
  public void setColorVector(Color c) {
    colorVector = c;
    colixVector = g3d.getColix(c);
  }
  public Color getColorVector() {
    return colorVector;
  }

  public void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setColorBackground(viewer.getColorFromString(colorName));
  }

  public short getColixAtom(Atom atom) {
    return getColixAtomPalette(atom, paletteDefault);
  }

  public short getColixAtomPalette(Atom atom, byte palette) {
    int argb = 0;
    switch (palette) {
    case JmolConstants.PALETTE_NONE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      short id = atom.elementNumber;
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
      float partialCharge = atom.getPartialCharge();
      if (Float.isNaN(partialCharge) ||
          partialCharge == 0) {
        argb = 0xFFFFFFFF; // white
      } else if (partialCharge < 0) {
        if (partialCharge < -1)
          partialCharge = -1;
        int byteVal = 0xFF - ((int)(255 * -partialCharge) & 0xFF);
        argb = 0xFFFF0000 | (byteVal << 8) | byteVal;
      } else { // partialCharge > 0
        if (partialCharge > 1)
          partialCharge = 1;
        int byteVal = 0xFF - ((int)(255 * partialCharge) & 0xFF);
        argb = 0xFF0000FF | (byteVal << 16) | (byteVal << 8);
      }
      break;
    case JmolConstants.PALETTE_TEMPERATURE:
      Frame frame = viewer.getFrame();
      int bfactor100Lo = frame.getBfactor100Lo();
      int bfactor100Hi = frame.getBfactor100Hi();
      int bfactorRange = bfactor100Hi - bfactor100Lo;
      if (bfactorRange == 0)
        argb = 0xFFFFFFFF;
      else {
        argb = 0xFF800080;
      }
      break;
    case JmolConstants.PALETTE_FORMALCHARGE:
      int i = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
      argb = JmolConstants.argbsCharge[i];
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
      break;
    case JmolConstants.PALETTE_AMINO:
      {
        int index = atom.getGroupID();
        if (index >= JmolConstants.GROUPID_AMINO_MAX)
          index = 0;
        argb = JmolConstants.argbsAmino[index];
        break;
      }
    case JmolConstants.PALETTE_SHAPELY:
      {
        int index = atom.getGroupID();
        if (index >= JmolConstants.GROUPID_AMINO_MAX)
          index = 0;
        argb = JmolConstants.argbsShapely[index];
        break;
      }
    case JmolConstants.PALETTE_CHAIN:
      int chain = atom.getChainID() & 0x1F;
      if (chain >= JmolConstants.argbsChainAtom.length)
        chain = chain % JmolConstants.argbsChainAtom.length;
      argb = (atom.isHetero()
              ? JmolConstants.argbsChainHetero
              : JmolConstants.argbsChainAtom)[chain];
      break;
    }
    if (argb == 0)
      return Graphics3D.HOTPINK;
    return g3d.getColix(argb);
  }

  public short getColixHbondType(short order) {
    int argbIndex = ((order & JmolConstants.BOND_HYDROGEN_MASK)
                     >> JmolConstants.BOND_HBOND_SHIFT);
    return g3d.getColix(JmolConstants.argbsHbondType[argbIndex]);
  }

  public void flushCachedColors() {
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();
  final Vector3f vNormal = new Vector3f();
  final Vector3f vRotated = new Vector3f();

  public int calcSurfaceIntensity(Point3f pA, Point3f pB, Point3f pC) {
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

  public void setSpecular(boolean specular) {
    g3d.setSpecular(specular);
    flushCaches();
  }

  public boolean getSpecular() {
    return g3d.getSpecular();
  }

  public void setSpecularPower(int specularPower) {
    g3d.setSpecularPower(specularPower);
    flushCaches();
  }

  public void setAmbientPercent(int ambientPercent) {
    g3d.setAmbientPercent(ambientPercent);
    flushCaches();
  }

  public void setDiffusePercent(int diffusePercent) {
    g3d.setDiffusePercent(diffusePercent);
    flushCaches();
  }

  public void setSpecularPercent(int specularPercent) {
    g3d.setSpecularPercent(specularPercent);
    flushCaches();
  }

  public void setLightsourceZ(float dist) {
    g3d.setLightsourceZ(dist);
    flushCaches();
  }

  public int calcIntensity(float x, float y, float z) {
    return g3d.calcIntensity(x, y, z);
  }
}
