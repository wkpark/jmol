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
package org.openscience.jmol;

import org.openscience.jmol.render.AtomShape;

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;
import org.openscience.jmol.render.AtomColors;

import java.awt.Color;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Vector;

public class ColorManager {

  DisplayControl control;

  ColorManager(DisplayControl control) {
    this.control = control;
    colorProfiles.addElement(AtomColors.getInstance());
    colorProfiles.addElement(new PartialAtomicChargeColors());
  }

  private Vector colorProfiles = new Vector();;

  public byte modeAtomColorProfile = DisplayControl.ATOMTYPE;
  public void setModeAtomColorProfile(byte mode) {
  }

  public int getModeAtomColorProfile() {
    return modeAtomColorProfile;
  }

  public Color colorOutline = Color.black;
  public void setColorOutline(Color c) {
    colorOutline = c;
  }

  public Color colorSelection = Color.orange;
  public Color colorSelectionTransparent;
  public void setColorSelection(Color c) {
    if (colorSelection == null || !colorSelection.equals(c)) {
      colorSelection = c;
      colorSelectionTransparent = null;
    }
  }
  public Color getColorSelection() {
    if (colorSelectionTransparent == null) {
      colorSelectionTransparent = getColorTransparent(colorSelection);
    }
    return colorSelectionTransparent;
  }

  public Color colorRubberband = Color.pink;
  public Color getColorRubberband() {
    return colorRubberband;
  }

  public boolean isBondAtomColor = true;
  public void setIsBondAtomColor(boolean isBondAtomColor) {
    this.isBondAtomColor = isBondAtomColor;
  }

  public Color colorBond = null;
  public void setColorBond(Color c) {
    colorBond = c;
  }

  public Color colorLabel = Color.black;
  public void setColorLabel (Color c) {
    colorLabel = c;
  }

  public Color colorDistance = Color.black;
  public void setColorDistance(Color c) {
    colorDistance = c;
  }

  public Color colorAngle = Color.black;
  public void setColorAngle(Color c) {
    colorAngle = c;
  }

  public Color colorDihedral = Color.black;
  public void setColorDihedral(Color c) {
    colorDihedral = c;
  }

  public Color colorBackground = Color.white;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
  }
  
  // FIXME NEEDSWORK -- arrow vector stuff
  public Color colorVector = Color.black;

  public void setColorVector(Color c) {
    colorVector = c;
  }

  public Color getColorVector() {
    return colorVector;
  }

  public boolean showDarkerOutline = true;
  public void setShowDarkerOutline(boolean showDarkerOutline) {
    this.showDarkerOutline = showDarkerOutline;
  }

  public Color getColorAtom(Atom atom) {
    return getColorAtom(modeAtomColorProfile, atom);
  }

  public Color getColorAtom(byte mode, Atom atom) {
    if (mode > colorProfiles.size()) {
        return Color.white;
    }
    Color color = ((AtomColorer)colorProfiles.elementAt(mode)).getAtomColor(atom);
    if (modeTransparentColors)
      color = getColorTransparent(color);
    return color;
  }

  public Color getColorAtomOutline(byte style, Color color) {
    Color outline =
      (showDarkerOutline || style == DisplayControl.SHADING)
      ? getDarker(color) : colorOutline;
    if (modeTransparentColors)
      outline = getColorTransparent(outline);
    return outline;
  }

  private Hashtable htDarker = new Hashtable();
  public Color getDarker(Color color) {
    Color darker = (Color) htDarker.get(color);
    if (darker == null) {
      darker = color.darker();
      htDarker.put(color, darker);
    }
    return darker;
  }

  private boolean modeTransparentColors = false;
  public void setModeTransparentColors(boolean modeTransparentColors) {
    this.modeTransparentColors = modeTransparentColors;
  }

  private final static int transparency = 0x60;
  private Hashtable htTransparent = new Hashtable();
  public Color getColorTransparent(Color color) {
    Color transparent = (Color) htTransparent.get(color);
    if (transparent == null) {
      if (control.getUseGraphics2D()) {
        int argb = (color.getRGB() & 0x00FFFFFF) | (transparency << 24);
        transparent = new Color (argb, true);
      } else {
        transparent = color;
      }
      htTransparent.put(color, transparent);
    }
    return transparent;
  }

  public void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setColorBackground(getColorFromString(colorName));
  }

  public void setColorForeground(String colorName) {
    // what is this supposed to do?
    // setColorForeground(getColorFromHexString(colorName));
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
    System.out.println("error converting tsring to color:" + strColor);
    return Color.pink;
  }


  public void flushCachedColors() {
    colorSelectionTransparent = null;
    htTransparent.clear();
    htDarker.clear();
  }
}
