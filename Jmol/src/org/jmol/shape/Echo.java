/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shape;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import org.jmol.g3d.*;

import java.util.BitSet;
import java.util.Enumeration;

import javax.vecmath.Point3f;

public class Echo extends TextShape {

  /*
   * set echo Text.TOP    [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo MIDDLE [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo BOTTOM [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name   [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name  x-position y-position
   * set echo none  to initiate setting default settings
   * 
   */

  private final static String FONTFACE = "Serif";
  private final static int FONTSIZE = 20;
  private final static short COLOR = Graphics3D.RED;
    
  public void initShape() {
    super.initShape();
    setProperty("target", "top", null);
  }

  public void setProperty(String propertyName, Object value, BitSet bsSelected) {

    if (Logger.debugging) {
      Logger.debug("Echo.setProperty(" + propertyName + "," + value + ")");
    }

    if ("script" == propertyName) {
      if (currentText != null)
        currentText.setScript((String) value);
      return;
    }

    if ("scalereference" == propertyName) {
      if (currentText != null) {
        float val = ((Float) value).floatValue();
        currentText.setScalePixelsPerMicron(val == 0 ? 0 : 10000f / val);
      }
      return;
    }
    
    if ("xpos" == propertyName) {
      if (currentText != null)
        currentText.setMovableX(((Integer) value).intValue());
      return;
    }

    if ("ypos" == propertyName) {
      if (currentText != null)
        currentText.setMovableY(((Integer) value).intValue());
      return;
    }

    if ("%xpos" == propertyName) {
      if (currentText != null)
        currentText.setMovableXPercent(((Integer) value).intValue());
      return;
    }

    if ("%ypos" == propertyName) {
      if (currentText != null)
        currentText.setMovableYPercent(((Integer) value).intValue());
      return;
    }
    
    if ("xypos" == propertyName) {
      if (currentText == null)
        return;
      Point3f pt = (Point3f) value;
      if (pt.z == Float.MAX_VALUE) {
        currentText.setMovableX((int) pt.x);        
        currentText.setMovableY((int) pt.y);        
      } else {
        currentText.setMovableXPercent((int) pt.x);        
        currentText.setMovableYPercent((int) pt.y);        
      }
      return;
    }

    if ("xyz" == propertyName) {
      if (currentText != null) {
        currentText.setXYZ((Point3f) value);
        if (viewer.getFontScaling())
          currentText.setScalePixelsPerMicron(viewer.getScalePixelsPerAngstrom() * 10000f);
      }
      return;
    }

    if ("target" == propertyName) {
      String target = ((String) value).intern().toLowerCase();
      if (target != "none" && target != "all") {
        Text text = (Text) texts.get(target);
        if (text == null) {
          int valign = Text.VALIGN_XY;
          int halign = Text.ALIGN_LEFT;
          if ("top" == target) {
            valign = Text.VALIGN_TOP;
            halign = Text.ALIGN_CENTER;
          } else if ("middle" == target) {
            valign = Text.VALIGN_MIDDLE;
            halign = Text.ALIGN_CENTER;
          } else if ("bottom" == target) {
            valign = Text.VALIGN_BOTTOM;
          }
          text = new Text(viewer, g3d, g3d.getFont3D(FONTFACE, FONTSIZE),
              target, COLOR, valign, halign, 0);
          text.setAdjustForWindow(true); // when a box is around it
          texts.put(target, text);
          if (currentFont != null)
            text.setFont(currentFont);
          if (currentColor != null)
            text.setColix(currentColor);
          if (currentBgColor != null)
            text.setBgColix(currentBgColor);
          if (currentTranslucentLevel != 0)
            text.setTranslucent(currentTranslucentLevel, false);
          if (currentBgTranslucentLevel != 0)
            text.setTranslucent(currentBgTranslucentLevel, true);
          
        }
        currentText = text;
        //process super
      }
    }
    super.setProperty(propertyName, value, null);
  }

  public void setVisibilityFlags(BitSet bs) {
    Enumeration e = texts.elements();
    while (e.hasMoreElements()) {
      Text t = (Text)e.nextElement();
      t.setVisibility(t.modelIndex < 0 || bs.get(t.modelIndex));
    }
  }


  public String getShapeState() {
    StringBuffer s = new StringBuffer("\n  set echo off;\n");
    String lastFormat = "";
    Enumeration e = texts.elements();
    while (e.hasMoreElements()) {
      Text t = (Text) e.nextElement();
      s.append(t.getState(true));
      String format = t.getState(false);
      if (format.equals(lastFormat))
        continue;
      lastFormat = format;
      s.append(format);
    }
    return s.toString();
  }

  public Point3f checkObjectClicked(int x, int y, int modifiers, BitSet bsVisible) {
    Enumeration e = texts.elements();
    while (e.hasMoreElements()) {
      Text t = (Text) e.nextElement();
      if (t.checkObjectClicked(x, y, bsVisible)) {
        String s = t.getScript();
        if (s != null)
          viewer.evalStringQuiet(s);
        return (t.xyz == null ? new Point3f() : t.xyz); // may or may not be null
      }
    }
    return null;
  }

  public boolean checkObjectHovered(int x, int y, BitSet bsVisible) {
    Enumeration e = texts.elements();
    boolean haveScripts = false;
    while (e.hasMoreElements()) {
      Text t = (Text) e.nextElement();
      String s = t.getScript();
      if (s != null) {
        haveScripts = true;
        if (t.checkObjectClicked(x, y, bsVisible)) {
          viewer.setCursor(Viewer.CURSOR_HAND);
          return true;
        }
      }
    }
    if (haveScripts)
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
    return false;
  }

}
