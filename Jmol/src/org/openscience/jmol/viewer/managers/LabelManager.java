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
import org.openscience.jmol.viewer.datamodel.AtomShape;
import org.openscience.jmol.viewer.protein.PdbAtom;

import java.awt.Font;
import java.util.BitSet;

public class LabelManager {

  JmolViewer viewer;

  public LabelManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public byte styleLabel = JmolViewer.NOLABELS;
  public void setStyleLabel(byte styleLabel) {
    this.styleLabel = styleLabel;
  }
  
  public String strFontFace = "SansSerif";
  public void setFontFace(String strFontFace) {
    this.strFontFace = strFontFace;
  }

  public final static int pointsMin = 6;
  public final static int pointsMax = 52;

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
    return getFontOfSize(pointsLabelFontSize);
    /*
    int points = diameter * 3 / 4;
    if (pointsLabelFontSize != 0)
      points = pointsLabelFontSize;
    if (points < 6)
      return null;
    return getFontOfSize(points);
    */
  }

  public int pointsLabelFontSize = 12;
  public void setLabelFontSize(int points) {
    this.pointsLabelFontSize = points <= 0 ? 12 : points;
  }

  public String getLabelAtom(byte styleLabel, AtomShape atomShape,
                             int atomIndex) {
    String label = null;
    switch (styleLabel) {
    case JmolViewer.SYMBOLS:
      label = atomShape.getAtomicSymbol();
      break;
    case JmolViewer.TYPES:
      label = atomShape.getAtomTypeName();
       break;
    case JmolViewer.NUMBERS:
      label = "" + (atomIndex + 1);
      break;
    }
    return label;
  }

  public String getLabelAtom(String strFormat, AtomShape atomShape,
                             int atomIndex) {
    if (strFormat == null || strFormat.equals(""))
      return null;
    PdbAtom pdbatom = atomShape.getPdbAtom();
    String strLabel = "";
    int cch = strFormat.length();
    int ich, ichPercent;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1; ) {
      strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ich == cch) {
        --ich; // a percent sign at the end of the string
        break;
      }
      int ch = strFormat.charAt(ich++);
      switch (ch) {
      case 'i':
        strLabel += (atomIndex + 1);
        break;
      case 'a':
        strLabel += atomShape.getAtomTypeName();
        break;
      case 'e':
        strLabel += atomShape.getAtomicSymbol();
        break;
      case 'b': // these two are the same
      case 't':
        if (pdbatom != null)
          strLabel += pdbatom.getTemperature();
        break;
      case 'c': // these two are the same
      case 's':
        if (pdbatom != null)
          strLabel += pdbatom.getChain();
        break;
      case 'm':
        strLabel += "<X>";
        break;
      case 'n':
        if (pdbatom != null)
          strLabel += pdbatom.getResidue();
        break;
      case 'r':
        if (pdbatom != null)
          strLabel += pdbatom.getResno();
        break;
      default:
        strLabel += ch;
      }
    }
    strLabel += strFormat.substring(ich);
    return strLabel;
  }
}
