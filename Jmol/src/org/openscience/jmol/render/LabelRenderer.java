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
import org.openscience.jmol.g25d.Graphics25D;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
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

  Graphics25D g25d;
  Rectangle clip;

  public void setGraphicsContext(Graphics25D g25d, Rectangle clip) {
    this.g25d = g25d;
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
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    int labelHeight = fontMetrics.getAscent();
    g25d.setColor(isLabelAtomColor ? atomShape.colorAtom : colorLabel);
    
    int labelWidth = fontMetrics.stringWidth(strLabel);
    ++labelWidth; // bias rounding to the left;
    labelHeight -= 2; // this should not be necessary, but looks like it is;
    g25d.drawString(strLabel,
                 atomShape.x - labelWidth / 2, atomShape.y + labelHeight / 2);

    // FIXME -- mth -- understand this property stuff
    if (!control.getPropertyStyleString().equals("")) {

      // check to make sure this atom has this property:
      Enumeration propIter = atomShape.atom.getAtomicProperties().elements();
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
          g25d.setFont(font);
          g25d.setColor(colorLabel);
          String s = p.stringValue();
          if (s.length() > 5) {
            s = s.substring(0, 5);
          }
          int k = 2 + (int) (atomShape.diameter/2 / 1.4142136f);
          g25d.drawString(s, atomShape.x + k, atomShape.y - k);
        }
      }
    }
  }

  public void renderStringOffset(String label, Color color, int points,
                                 int x, int y, int xOffset, int yOffset) {
    g25d.setColor(color);
    Font font = control.getFontOfSize(points);
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    int labelHeight = fontMetrics.getAscent();
    labelHeight -= 2; // this should not be necessary, but looks like it is;
    if (yOffset > 0)
      y += yOffset + labelHeight;
    else if (yOffset == 0)
      y += labelHeight / 2;
    else
      y += yOffset;
    if (xOffset > 0)
      x += xOffset;
    else if (xOffset == 0)
      x -= fontMetrics.stringWidth(label) / 2;
    else
      x += xOffset - fontMetrics.stringWidth(label);
    g25d.drawString(label, x, y);
  }

  public void renderStringOutside(String label, Color color, int points,
                                  int x, int y) {
    //    System.out.println("render string outside " + x + "," + y);
    //    g25d.setColor(Color.blue);
    //    g25d.fillRect(x-1, y-1, 3, 3);
    g25d.setColor(color);
    Font font = control.getFontOfSize(points);
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    int labelAscent = fontMetrics.getAscent();
    int labelWidth = fontMetrics.stringWidth(label);
    int xLabelCenter, yLabelCenter;
    int xCenter = control.getBoundingBoxCenterX();
    int yCenter = control.getBoundingBoxCenterY();
    int dx = x - xCenter;
    int dy = y - yCenter;
    if (dx == 0 && dy == 0) {
      xLabelCenter = x;
      yLabelCenter = y;
    } else {
      int dist = (int) Math.sqrt(dx*dx + dy*dy);
      xLabelCenter = xCenter + ((dist + 2 + (labelWidth + 1) / 2) * dx / dist);
      yLabelCenter = yCenter + ((dist + 3 + (labelAscent + 1)/ 2) * dy / dist);
    }
    int xLabelBaseline = xLabelCenter - labelWidth / 2;
    int yLabelBaseline = yLabelCenter + labelAscent / 2;
    g25d.drawString(label, xLabelBaseline, yLabelBaseline);
  }
}

