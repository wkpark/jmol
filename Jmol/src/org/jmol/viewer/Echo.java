/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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

import java.awt.FontMetrics;
import java.util.BitSet;

class Echo extends Shape {

  private final static int LEFT = 0;
  private final static int CENTER = 1;
  private final static int RIGHT = 2;

  private final static int TOP = 0;
  private final static int BOTTOM = 1;
  private final static int MIDDLE = 2;

  private final static String FONTFACE = "Serif";
  private final static int FONTSIZE = 20;
  private final static short COLOR = Graphics3D.RED;

  Text topText;
  Text middleText;
  Text bottomText;
  Text currentText;
  
  void initShape() {
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    /*
    System.out.println("Echo.setProperty(" + propertyName + "," + value + ")");
    */

    if ("color" == propertyName) {
      if (currentText != null)
        currentText.colix = g3d.getColix(value);
      return;
    }

    if ("bgcolor" == propertyName) {
      if (currentText != null)
        currentText.bgcolix = value == null ? (short)0 : g3d.getColix(value);
      return;
    }

    if ("font" == propertyName) {
      if (currentText != null) {
        currentText.font3d = (Font3D)value;
        currentText.recalc();
      }
      return;
    }
    
    if ("echo" == propertyName) {
      if (currentText != null) {
        currentText.text = (String)value;
        currentText.recalc();
      }
    }

    if ("off" == propertyName) {
      currentText = null;
      if (topText != null) topText.text = null;
      if (middleText != null) middleText.text = null;
      if (bottomText != null) bottomText.text = null;
    }
    
    if ("target" == propertyName) {
      String target = ((String)value).intern();
      if ("top" == target) {
       if (topText == null)
          topText = new Text(TOP, CENTER,
                             g3d.getFont3D(FONTFACE, FONTSIZE), COLOR);
        currentText = topText;
        return;
      }
    
      if ("middle" == target) {
       if (middleText == null)
          middleText = new Text(MIDDLE, CENTER,
                                g3d.getFont3D(FONTFACE, FONTSIZE), COLOR);
        currentText = middleText;
        return;
      }
      
      if ("bottom" == target) {
       if (bottomText == null)
          bottomText = new Text(BOTTOM, LEFT,
                                g3d.getFont3D(FONTFACE, FONTSIZE), COLOR);
        currentText = bottomText;
        return;
      }

      if ("none" == target) {
       currentText = null;
        return;
      }
      System.out.println("unrecognized target:" + target);
      return;
    }

    if ("align" == propertyName) {
      if (currentText == null)
        return;
      String align = ((String)value).intern();
      if ("left" == align) {
       currentText.align = LEFT;
        return;
      }
      
      if ("center" == align) {
       currentText.align = CENTER;
        return;
      }
      
      if ("right" == align) {
       currentText.align = RIGHT;
        return;
      }
      System.out.println("unrecognized align:" + align);
      return;
    }
  }

  class Text {
    String text;
    int align;
    int valign;
    Font3D font3d;
    short colix;
    short bgcolix;
    
    int width;
    int ascent;
    int descent;

    Text(int valign, int align, Font3D font3d, short colix) {
      this.align = align;
      this.valign = valign;
      this.font3d = font3d;
      this.colix = colix;
    }

    void recalc() {
      if (text == null || text.length() == 0) {
        text = null;
        return;
      }
      FontMetrics fm = font3d.fontMetrics;
      width = fm.stringWidth(text);
      descent = fm.getDescent();
      ascent = fm.getAscent();
    }

    void render(Graphics3D g3d) {
      if (text == null)
        return;
      int x = g3d.getRenderWidth() - width - 1;
      if (align == CENTER)
        x /= 2;
      else if (align == LEFT)
        x = 0;

      int y;
      if (valign == TOP)
        y = ascent;
      else if (valign == MIDDLE) // baseline is at the middle
        y = g3d.getRenderHeight() / 2;
      else
        y = g3d.getRenderHeight() - descent - 1;

      g3d.setFont(font3d);
      g3d.drawString(text, colix, x, y, 0);
    }
  }
}

