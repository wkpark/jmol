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

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;
import org.openscience.jmol.render.AtomColors;

import java.awt.Color;
import java.util.Hashtable;

public class ColorManager {

  DisplayControl control;

  ColorManager(DisplayControl control) {
    this.control = control;
  }

  private AtomColorer colorProfile = AtomColors.getInstance();
  public int modeAtomColorProfile = DisplayControl.ATOMTYPE;
  public void setModeAtomColorProfile(int mode) {
    if (modeAtomColorProfile != mode) {
      modeAtomColorProfile = mode;
      if (mode == DisplayControl.ATOMTYPE)
        colorProfile = AtomColors.getInstance();
      else
        colorProfile = new PartialAtomicChargeColors();
    }
  }

  public int getModeAtomColorProfile() {
    return modeAtomColorProfile;
  }

  public Color colorOutline = Color.black;
  public void setColorOutline(Color c) {
    colorOutline = c;
  }
  public Color getColorOutline() {
    return colorOutline;
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
      colorSelectionTransparent = 
        control.getUseGraphics2D() ?
        getColorTransparent(colorSelection) :
        colorSelection;
    }
    return colorSelectionTransparent;
  }

  public Color colorRubberband = Color.pink;
  public Color getColorRubberband() {
    return colorRubberband;
  }

  public Color colorText = Color.black;
  public void setColorText(Color c) {
    colorText = c;
  }
  public Color getColorText() {
    return colorText;
  }

  public Color colorDistance = Color.black;
  public void setColorDistance(Color c) {
    colorDistance = c;
  }
  public Color getColorDistance() {
    return colorDistance;
  }

  public Color colorAngle = Color.black;
  public void setColorAngle(Color c) {
    colorAngle = c;
  }
  public Color getColorAngle() {
    return colorAngle;
  }

  public Color colorDihedral = Color.black;
  public void setColorDihedral(Color c) {
    colorDihedral = c;
  }
  public Color getColorDihedral() {
    return colorDihedral;
  }

  public Color colorBackground = Color.white;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
  }
  public Color getColorBackground() {
    return colorBackground;
  }
  
  // FIXME NEEDSWORK -- arrow vector stuff
  public Color colorVector = Color.black;

  public void setColorVector(Color c) {
    colorVector = c;
  }

  public Color getColorVector() {
    return colorVector;
  }

  public Color getColorAtom(Atom atom) {
    Color color = colorProfile.getAtomColor(atom);
    if (modeTransparentColors)
      color = getColorTransparent(color);
    return color;
  }

  public Color getColorAtomOutline(Color color) {
    Color outline =
      (control.getShowDarkerOutline() ||
       control.getModeAtomDraw() == DisplayControl.SHADING)
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
      int argb = (color.getRGB() & 0x00FFFFFF) | (transparency << 24);
      transparent = new Color (argb, true);
      htTransparent.put(color, transparent);
    }
    return transparent;
  }

  public void setColorBackground(String colorName) {
    setColorBackground(getColorFromHexString(colorName));
  }

  public void setColorForeground(String colorName) {
    // what is this supposed to do?
    // setColorForeground(getColorFromHexString(colorName));
  }

  public Color getColorFromHexString(String hexColor) {
    if ((hexColor != null) &&
        (hexColor.length() == 7) ||
        (hexColor.charAt(0) == '#')) {
      try {
        int red = Integer.parseInt(hexColor.substring(1, 3), 16);
        int grn = Integer.parseInt(hexColor.substring(3, 5), 16);
        int blu = Integer.parseInt(hexColor.substring(5, 7), 16);
        return new Color(red, grn, blu);
      } catch (NumberFormatException e) {
      }
    }
    System.out.println("error converting hex string to color:" + hexColor);
    return Color.white;
  }

  public void flushCachedColors() {
    colorSelectionTransparent = null;
    htTransparent.clear();
    htDarker.clear();
  }
}
