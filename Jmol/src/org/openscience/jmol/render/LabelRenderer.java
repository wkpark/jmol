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

package org.openscience.jmol.render;

import org.openscience.jmol.*;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;
import java.util.Enumeration;

public class LabelRenderer {

  DisplayControl control;
  public LabelRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics g;
  Rectangle clip;

  public void setGraphicsContext(Graphics g, Rectangle clip) {
    this.g = g;
    this.clip = clip;
    colorLabel = control.getColorLabel();
    isLabelAtomColor = colorLabel == null;
  }

  Color colorLabel;
  boolean isLabelAtomColor;

  public void render(AtomShape atomShape) {
    String strLabel = atomShape.strLabel;
    if (strLabel == null)
      return;
    Font font = control.getLabelFont(atomShape.diameter);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    int labelHeight = fontMetrics.getAscent();
    g.setColor(isLabelAtomColor ? atomShape.colorAtom : colorLabel);
    
    int labelWidth = fontMetrics.stringWidth(strLabel);
    ++labelWidth; // bias rounding to the left;
    labelHeight -= 2; // this should not be necessary, but looks like it is;
    g.drawString(strLabel,
                 atomShape.x - labelWidth / 2, atomShape.y + labelHeight / 2);

    // FIXME -- mth -- understand this property stuff
    if (!control.getPropertyStyleString().equals("")) {

      // check to make sure this atom has this property:
      Enumeration propIter = atomShape.atom.getProperties().elements();
      while (propIter.hasMoreElements()) {
        PhysicalProperty p = (PhysicalProperty) propIter.nextElement();
        if (p.getDescriptor().equals(control.getPropertyStyleString())) {
        
          // OK, we had this property.  Let's draw the value on
          // screen:
          // font = new Font("Helvetica", Font.PLAIN, radius / 2);
          // mth -- the code used to divide the radius by 2
          // but getLabelFont works with diameters
          // so divide the diameter by 2
          font = control.getLabelFont(atomShape.diameter / 2);
          g.setFont(font);
          g.setColor(colorLabel);
          String s = p.stringValue();
          if (s.length() > 5) {
            s = s.substring(0, 5);
          }
          int k = 2 + (int) (atomShape.diameter/2 / 1.4142136f);
          g.drawString(s, atomShape.x + k, atomShape.y - k);
        }
      }
    }
  }

  public void renderStringOffset(String label, Color color, int points,
                                 int x, int y, int xOffset, int yOffset) {
    g.setColor(color);
    Font font = control.getFontOfSize(points);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    int labelHeight = fontMetrics.getAscent();
    if (yOffset > 0)
      y += yOffset + labelHeight;
    else if (yOffset == 0)
      y += labelHeight / 2;
    else
      y += yOffset;
    if (xOffset > 0)
      x += xOffset;
    else if (xOffset == 0)
      x -= fontMetrics.stringWidth(label);
    else
      x += xOffset - fontMetrics.stringWidth(label);
    g.drawString(label, x, y);
  }
}

