/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
import java.awt.image.Kernel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;
import java.util.Enumeration;
import java.util.Hashtable;

import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;

import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

/**
 * Graphical representation of an atom.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomShape extends Shape {

  Atom atom;
  //  private Point3d screenPosition = new Point3d();
  public int diameter;
  public int bondWidth;
  
  public AtomShape(Atom atom) {
    this.atom = atom;
  }

  public String toString() {
    return "Atom shape for " + atom + ": z = " + z;
  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(atom.getPosition());
    x = (int)screen.x;
    y = (int)screen.y;
    z = (int)screen.z;
    diameter = control.screenAtomDiameter(z, atom);
    bondWidth = control.screenBondWidth(z);
  }

  public void render(Graphics g, Rectangle clip, DisplayControl control) {
    if (!control.showHydrogens && atom.isHydrogen()) {
      return;
    }
    if (control.showBonds) {
      renderBonds(g, clip, control);
    }
    if (control.showAtoms && isClipVisible(clip)) {
      control.atomRenderer.render(this);
    }
  }

  public void renderBonds(Graphics g, Rectangle clip, DisplayControl control) {
    Atom[] bondedAtoms = atom.getBondedAtoms();
    if (bondedAtoms == null) {
      return;
    }
    for (int i = 0; i < bondedAtoms.length; ++i) {
      Atom atomOther = bondedAtoms[i];
      AtomShape atomShapeOther = atomOther.getAtomShape();
      int zOther = atomShapeOther.z;
      if ((control.showHydrogens || !atomOther.isHydrogen()) &&
          ((z > zOther) ||
           (z==zOther && atom.getAtomNumber()>atomOther.getAtomNumber())) &&
          isBondClipVisible(clip, x, y, atomShapeOther.x, atomShapeOther.y)) {
        control.bondRenderer.render(this, atomShapeOther,
                                    atom.getBondOrder(atomOther));
      }
    }
  }

  private static Rectangle rectTemp = new Rectangle();
  private boolean isClipVisible(Rectangle clip) {
    int radius = diameter / 2;
    rectTemp.setRect(x - radius, y - radius, diameter, diameter);
    // note that this is not correct if the atom is selected
    // because the halo may be visible while the atom is not
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("isClipVisible -> " + visible);
    System.out.println(" x=" + x + " y=" + y + " diameter=" + diameter);
    visible = true;
    */
    return visible;
  }

  private boolean isBondClipVisible(Rectangle clip,
                                    int x1, int y1, int x2, int y2) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (x1 < x2) {
      xMin = x1;
      width = x2 - x1;
    } else if (x2 < x1) {
      xMin = x2;
      width = x1 - x2;
    } else {
      xMin = x1;
      width = 1;
    }
    if (y1 < y2) {
      yMin = y1;
      height = y2 - y1;
    } else if (y2 < y1) {
      yMin = y2;
      height = y1 - y2;
    } else {
      yMin = y1;
      height = 1;
    }
    // there are some problems with this quick&dirty implementation
    // so I am going to throw in some slop
    xMin -= 5;
    yMin -= 5;
    width += 10;
    height += 10;
    rectTemp.setRect(xMin, yMin, width, height);
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("bond " + x + "," + y + "->" + x2 + "," + y2 +
                       " & " + clip.x + "," + clip.y +
                       " W " + clip.width + " H " + clip.height +
                       "->" + visible);
    visible = true;
    */
    return visible;
  }

}

