
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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.pdb.PdbAtom;
import org.openscience.jmol.viewer.script.Token;

import java.awt.Color;
import java.util.Hashtable;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

public class ColorManager {

  JmolViewer viewer;
  Graphics3D g3d;
  int[] argbsCpk;

  public byte paletteDefault = JmolConstants.PALETTE_CPK;

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
      viewer.setColorDots(null);
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
      viewer.setColorDots(null);
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

  private final static short colixDotsConcaveDefault = Graphics3D.GREEN;
  private final static short colixDotsSaddleDefault = Graphics3D.BLUE;

  public short colixDotsConvex = 0;
  public short colixDotsConcave = colixDotsConcaveDefault;
  public short colixDotsSaddle = colixDotsSaddleDefault;

  public void setColorDotsConvex(Color color) {
    colixDotsConvex = g3d.getColix(color);
  }
  public void setColorDotsConcave(Color color) {
    colixDotsConcave = g3d.getColix(color);
    if (colixDotsConcave == 0)
      colixDotsConcave = colixDotsConcaveDefault;
  }
  public void setColorDotsSaddle(Color color) {
    colixDotsSaddle = g3d.getColix(color);
    if (colixDotsSaddle == 0)
      colixDotsSaddle = colixDotsSaddleDefault;
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
      setColorBackground(getColorFromString(colorName));
  }

  private static final Hashtable mapJavaScriptColors = new Hashtable();
  static {
    for (int i = JmolConstants.colorNames.length; --i >= 0; )
      mapJavaScriptColors.put(JmolConstants.colorNames[i],
                              new Color(JmolConstants.colorArgbs[i]));
  }

  public static int getArgbFromString(String strColor) {
    /*
    System.out.println("ColorManager.getArgbFromString(" + strColor + ")");
    */
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return (0xFF000000 |
                  (red & 0xFF) << 16 |
                  (grn & 0xFF) << 8  |
                  (blu & 0xFF));
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color.getRGB();
      }
    }
    return 0;
  }

  public static Color getColorFromString(String strColor) {
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return new Color(red, grn, blu);
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color;
      }
    }
    System.out.println("error converting string to color:" + strColor);
    return Color.pink;
  }

  public short getColixAtom(Atom atom) {
    return getColixAtomPalette(atom, paletteDefault);
  }

  public short getColixAtomPalette(Atom atom, byte palette) {
    int argb = 0;
    PdbAtom pdbatom = atom.pdbAtom;
    switch (palette) {
    case JmolConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      short id = atom.elementNumber;
      return g3d.getChangableColix(id, argbsCpk[id]);
    case JmolConstants.PALETTE_CHARGE:
      int i = atom.getAtomicCharge() - JmolConstants.CHARGE_MIN;
      argb = JmolConstants.argbsCharge[i];
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      if (pdbatom != null)
        argb = JmolConstants.
          argbsPdbStructure[pdbatom.getSecondaryStructureType()];
      break;
    case JmolConstants.PALETTE_AMINO:
      if (pdbatom != null) {
        int groupID = pdbatom.getGroupID();
        argb = ((groupID < JmolConstants.argbsPdbAmino.length)
                ? JmolConstants.argbsPdbAmino[groupID]
                : JmolConstants.argbPdbAminoDefault);
      }
      break;
    case JmolConstants.PALETTE_SHAPELY:
      if (pdbatom != null) {
        int groupID = pdbatom.getGroupID();
        argb = ((groupID < JmolConstants.argbsPdbShapely.length)
                ? JmolConstants.argbsPdbShapely[groupID]
                : JmolConstants.argbPdbShapelyDefault);
      }
      break;
    case JmolConstants.PALETTE_CHAIN:
      if (pdbatom != null) {
        int chain = pdbatom.getChainID() & 0x1F;
        if (chain >= JmolConstants.argbsPdbChainAtom.length)
          chain = chain % JmolConstants.argbsPdbChainAtom.length;
        argb = (atom.isHetero()
                ? JmolConstants.argbsPdbChainHetero
                : JmolConstants.argbsPdbChainAtom)[chain];
      }
      break;
    }
    if (argb == 0)
      return Graphics3D.HOTPINK;
    return g3d.getColix(argb);
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
