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

import java.awt.Color;
import java.awt.Font;
import java.util.BitSet;

public class LabelManager {

  DisplayControl control;

  public LabelManager(DisplayControl control) {
    this.control = control;
  }

  public byte styleLabel = DisplayControl.NOLABELS;
  public void setStyleLabel(byte styleLabel) {
    this.styleLabel = styleLabel;
  }
  
  public String strFontFace = "Helvetica";
  public void setFontFace(String strFontFace) {
    this.strFontFace = strFontFace;
  }

  public final static int pointsMin = 6;
  public final static int pointsMax = 32;

  Font[] fonts = new Font[pointsMax - pointsMin + 1];

  public Font getFontOfSize(int points) {
    if (points < pointsMin)
      points = pointsMin;
    else if (points > pointsMax)
      points = pointsMax;
    int index = points - pointsMin;
    Font font = fonts[index];
    if (font == null)
      font = fonts[index] = new Font(strFontFace, Font.PLAIN, points);
    return font;
  }

  public Font getLabelFont(int diameter) {
    int points = diameter * 2 / 3;
    if (pointsLabelFontSize != 0)
      points = pointsLabelFontSize;
    return getFontOfSize(points);
  }

  public int pointsLabelFontSize = 0;
  public void setLabelFontSize(int points) {
    this.pointsLabelFontSize = points;
  }

  public String getLabelAtom(byte styleLabel, Atom atom) {
    String label = null;
    switch (styleLabel) {
    case DisplayControl.SYMBOLS:
      label = atom.getSymbol();
      break;
    case DisplayControl.TYPES:
      label = atom.getAtomTypeName();
       break;
    case DisplayControl.NUMBERS:
      // Note that this is incremented by one for display purposes
      label = "" + (atom.getAtomNumber() + 1);
      break;
    }
    return label;
  }

  public String getLabelAtom(String strFormat, Atom atom) {
    if (strFormat == null || strFormat.equals(""))
      return null;
    ProteinProp pprop = atom.getProteinProp();
    String strLabel = "";
    String strExpansion = "";
    int ich = 0;
    int cch = strFormat.length();
    char ch;
    int ichPercent;
    boolean percentFound =false;
    while ((ichPercent = strFormat.indexOf('%', ich)) != -1) {
      strFormat += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ich == cch) {
        --ich; // a percent sign at the end of the string
        break;
      }
      strExpansion = "";
      ch = strFormat.charAt(ich++);
      switch (ch) {
      case 'i':
        strExpansion = "" + atom.getAtomNumber() + 1;
        break;
      case 'a': // FIXME -- mth -- a is not the same as e
      case 'e':
        strExpansion = atom.getSymbol();
        break;
      case 'b': // these two are the same
      case 't':
        if (pprop != null)
          strExpansion = "" + pprop.getTemperature();
        break;
      case 'c': // these two are the same
      case 's':
        if (pprop != null)
          strExpansion = "" + pprop.getChain();
        break;
      case 'm':
        strExpansion = "<X>";
        break;
      case 'n':
        if (pprop != null)
          strExpansion = "" + pprop.getResidue();
        break;
      case 'r':
        if (pprop != null)
          strExpansion = "" + pprop.getResno();
        break;
      default:
        strExpansion = "" + ch;
      }
      strLabel += strExpansion;
    }
    strLabel += strFormat.substring(ich);
    return strLabel;
  }
}
