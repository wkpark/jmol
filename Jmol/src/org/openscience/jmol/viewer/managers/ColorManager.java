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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import org.openscience.jmol.viewer.datamodel.AtomShape;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.script.Token;

import java.awt.Color;
import java.util.Hashtable;

public class ColorManager {

  JmolViewer viewer;
  JmolModelAdapter modelAdapter;
  boolean suppliesAtomArgb;
  public byte schemeDefault = JmolViewer.ATOMTYPE;

  public ColorManager(JmolViewer viewer, JmolModelAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    suppliesAtomArgb = modelAdapter.suppliesAtomArgb();
    if (JmolModelAdapter.argbsCpk.length != JmolModelAdapter.atomicNumberMax)
      System.out.println("WARNING! argbsCpk.length not consistent");
    if (JmolModelAdapter.argbsPdbAmino.length != Token.RESID_AMINO_MAX)
      System.out.println("WARNING! argbsPdbAmino.length not consistent");
    if (JmolModelAdapter.argbsPdbShapely.length != Token.RESID_DNA_MAX)
      System.out.println("WARNING! argbsPdbShapely.length not consistent");
  }

  public void setSchemeDefault(byte scheme) {
    schemeDefault = scheme;
  }

  public byte getSchemeDefault() {
    return schemeDefault;
  }

  public Color colorSelection = Color.orange;
  public short colixSelection = Colix.ORANGE;

  public void setColorSelection(Color c) {
    colorSelection = c;
    colixSelection = Colix.getColix(c);
  }

  public Color getColorSelection() {
    return colorSelection;
  }

  public short getColixSelection() {
    return colixSelection;
  }

  public Color colorRubberband = Color.pink;
  public short colixRubberband = Colix.PINK;
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
    colixBond = Colix.getColix(c);
  }

  public Color colorLabel = Color.black;
  public short colixLabel = Colix.BLACK;
  public void setColorLabel(Color color) {
    colorLabel = color;
    colixLabel = Colix.getColix(color);
  }

    public short colixDotsConvex = 0;
    public short colixDotsConcave = Colix.GREEN;
    public short colixDotsSaddle = Colix.RED;

    public void setColorDotsConvex(Color color) {
	colixDotsConvex = Colix.getColix(color);
    }
    public void setColorDotsConcave(Color color) {
	colixDotsConcave = Colix.getColix(color);
	if (colixDotsConcave == 0)
	    colixDotsConcave = Colix.GREEN;
    }
    public void setColorDotsSaddle(Color color) {
	colixDotsSaddle = Colix.getColix(color);
	if (colixDotsSaddle == 0)
	    colixDotsSaddle = Colix.RED;
    }

  public Color colorDistance = Color.black;
  public short colixDistance = Colix.BLACK;
  public void setColorDistance(Color c) {
    colorDistance = c;
    colixDistance = Colix.getColix(c);
  }

  public Color colorAngle = Color.black;
  public short colixAngle = Colix.BLACK;
  public void setColorAngle(Color c) {
    colorAngle = c;
    colixAngle = Colix.getColix(c);
  }

  public Color colorDihedral = Color.black;
  public short colixDihedral = Colix.BLACK;
  public void setColorDihedral(Color c) {
    colorDihedral = c;
    colixDihedral = Colix.getColix(c);
  }

  public Color colorBackground = Color.white;
  public short colixBackground = Colix.WHITE;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    colixBackground = Colix.getColix(colorBackground);
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  public Color colorVector = Color.black;
  public short colixVector = Colix.BLACK;
  public void setColorVector(Color c) {
    colorVector = c;
    colixVector = Colix.getColix(c);
  }
  public Color getColorVector() {
    return colorVector;
  }

  public void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setColorBackground(getColorFromString(colorName));
  }

  // official HTML 4.0 color names & values
  private static final Object[] aHtmlColors = {
    "aqua",    Color.cyan,
    "black",   Color.black,
    "blue",    Color.blue,
    "fuchsia", Color.magenta,
    "gray",    Color.gray,
    "green",   new Color(0,   128,   0),
    "lime",    Color.green,
    "maroon",  new Color(128,   0,   0),
    "navy",    new Color(  0,   0, 128),
    "olive",   new Color(128, 128,   0),
    "purple",  new Color(128,   0, 128),
    "red",     Color.red,
    "silver",  Color.lightGray,
    "teal",    new Color(  0, 128, 128),
    "yellow",  Color.yellow,
    "white",   Color.white
  };

  private static final Hashtable mapHtmlColors = new Hashtable();
  static {
    for (int i = aHtmlColors.length; --i >= 0; ) {
      Color co = (Color)aHtmlColors[i];
      String str = (String)aHtmlColors[--i];
      mapHtmlColors.put(str, co);
    }
  }

  public Color getColorFromString(String strColor) {
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
        Color color = (Color)mapHtmlColors.get(strColor.toLowerCase());
        if (color != null)
          return color;
      }
    }
    System.out.println("error converting string to color:" + strColor);
    return Color.pink;
  }

  public short getColixAtom(AtomShape atom) {
    return getColixAtomScheme(atom, schemeDefault);
  }

  public short getColixAtomScheme(AtomShape atom, byte scheme) {
    int argb = 0;
    if (suppliesAtomArgb) {
      argb = modelAdapter.getAtomArgb(atom.clientAtom, scheme);
      if (argb != 0)
        return Colix.getColix(argb);
      System.out.println("JmolModelAdapter.getColorAtom returned null");
    }
    switch (scheme) {
    case JmolModelAdapter.COLORSCHEME_CPK:
      argb = JmolModelAdapter.argbsCpk[atom.atomicNumber];
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_STRUCTURE:
      if (atom.pdbatom != null)
        argb = JmolModelAdapter.argbsPdbStructure[atom.pdbatom.structureType];
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_AMINO:
      if (atom.pdbatom != null) {
        byte resid = atom.pdbatom.getResID();
        if (resid >= 0 && resid < Token.RESID_AMINO_MAX)
          argb = JmolModelAdapter.argbsPdbAmino[resid];
      }
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_SHAPELY:
      if (atom.pdbatom != null) {
        byte resid = atom.pdbatom.getResID();
        if (resid >= 0 && resid < Token.RESID_DNA_MAX)
          argb = JmolModelAdapter.argbsPdbShapely[resid];
        else
          argb = JmolModelAdapter.argbPdbShapelyDefault;
      }
      break;
    case JmolModelAdapter.COLORSCHEME_CHARGE:
      break;
    }
    if (argb == 0)
      return Colix.PINK;
    return Colix.getColix(argb);
  }

  public void flushCachedColors() {
  }
}
