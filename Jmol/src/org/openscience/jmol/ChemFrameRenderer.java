
/*
 * Copyright 2001 The Jmol Development Team
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

import java.awt.Graphics;
import java.util.Enumeration;
import java.util.Vector;
import javax.vecmath.Point3f;

/**
 *  Drawing methods for ChemFrame.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ChemFrameRenderer {

  /**
   * Paint this model to a graphics context.  It uses the matrix
   * associated with this model to map from model space to screen
   * space.
   *
   * @param g the Graphics context to paint to
   */
  public synchronized void paint(Graphics g, ChemFrame frame,
      DisplaySettings settings) {

    if (frame.getNumberOfAtoms() <= 0) {
      return;
    }
    boolean drawHydrogen = settings.getShowHydrogens();
    frame.transform();
    
    if (shapes == null || frame.hashCode() != frameHashCode
        || settings.hashCode() != previousSettingsHashCode) {
      frameHashCode = frame.hashCode();
      previousSettingsHashCode = settings.hashCode();
      double maxMagnitude = -1.0;
      double minMagnitude = Double.MAX_VALUE;
      Vector shapesList = new Vector();
      for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
        Atom atom = frame.getAtomAt(i);
        if (settings.getShowAtoms() && (settings.getShowHydrogens()
            || !atom.isHydrogen())) {
          shapesList.addElement(new AtomShape(atom, settings, frame.isAtomPicked(i)));
          shapesList.addElement(new AtomLabelShape(atom, settings));
        }
        if (settings.getShowBonds()) {
          Enumeration bondIter = atom.getBondedAtoms();
          while (bondIter.hasMoreElements()) {
            Atom otherAtom = (Atom) bondIter.nextElement();
            if (settings.getShowHydrogens()
                || (!atom.isHydrogen() && !otherAtom.isHydrogen())) {
              shapesList.addElement(new BondShape(atom, otherAtom, settings));
            }
          }
        }

        if (settings.getShowVectors()) {
          Point3f vector = atom.getVector();
          if (vector != null) {
            double magnitude = vector.distance(zeroPoint);
            if (magnitude > maxMagnitude) {
              maxMagnitude = magnitude;
            }
            if (magnitude < minMagnitude) {
              minMagnitude = magnitude;
            }
          }
        }
      }
      
      if (settings.getShowVectors()) {
        double magnitudeRange = maxMagnitude - minMagnitude;
        for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
          Atom atom = frame.getAtomAt(i);
          if (settings.getShowHydrogens() || !atom.isHydrogen()) {
            shapesList.addElement(new AtomVectorShape(atom, settings, minMagnitude, magnitudeRange));
          }
        }
      }
      
      shapes = new Shape[shapesList.size()];
      Enumeration shapeIter = shapesList.elements();
      for (int i = 0; i < shapes.length && shapeIter.hasMoreElements(); ++i) {
        shapes[i] = (Shape) shapeIter.nextElement();
      }
    }
    shapeSorter.sort(shapes);
    
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].render(g);
    }
    
  }

  int frameHashCode;
  int previousSettingsHashCode;
  
  Shape[] shapes;
  
  HeapSorter shapeSorter = new HeapSorter(new HeapSorter.Comparator() {

    public int compare(Object atom1, Object atom2) {

      Shape a1 = (Shape) atom1;
      Shape a2 = (Shape) atom2;
      if (a1.getZ() < a2.getZ()) {
        return -1;
      } else if (a1.getZ() > a2.getZ()) {
        return 1;
      }
      return 0;
    }
  });

  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
}

