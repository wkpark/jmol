/*
 * Copyright 2002 The Jmol Development Team
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
import java.awt.Graphics;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Vector;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import java.util.Arrays;
import java.util.Comparator;

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
      DisplaySettings settings, Matrix4f matrix) {

    int numAtoms = frame.getNumberOfAtoms();
    if (numAtoms <= 0) {
      return;
    }
    int hcFrame = frame.hashCode();
    int hcSettings = settings.hashCode();
    if (shapes == null || // did not do shapes yet
        hcFrame != previousFrameHashCode || // frame itself is changed
        hcSettings != previousSettingsHashCode || // settings have changed
        numAtoms != previousNumberAtoms // #atoms changed (e.g. a delete)
       ) {
      previousFrameHashCode = hcFrame;
      previousSettingsHashCode = hcSettings;
      previousNumberAtoms = numAtoms;
      shapesList.clear();
      transformables.clear();
      transformables.add(frame);
      double maxMagnitude = -1.0;
      double minMagnitude = Double.MAX_VALUE;
      boolean showHydrogens = settings.getShowHydrogens();
      boolean showVectors = settings.getShowVectors();
    
      for (int i = 0; i < numAtoms; ++i) {
        Atom atom = frame.getAtomAt(i);
        shapesList.add(new AtomShape(atom));
        if (showVectors) {
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
      
      if (showVectors) {
        double magnitudeRange = maxMagnitude - minMagnitude;
        for (int i = 0; i < numAtoms; ++i) {
          Atom atom = frame.getAtomAt(i);
          if (showHydrogens || !atom.isHydrogen()) {
            shapesList.add(new AtomVectorShape(atom, settings,
                                               minMagnitude, magnitudeRange));
          }
        }
      }
      
      if (frame instanceof CrystalFrame) {
        CrystalFrame crystalFrame = (CrystalFrame) frame;
        float[][] rprimd = crystalFrame.getRprimd();
        
        // The three primitives vectors with arrows
        for (int i = 0; i < 3; i++) {
          VectorShape vector = new VectorShape(settings, zeroPoint,
              new Point3f(rprimd[i][0], rprimd[i][1], rprimd[i][2]), false,
                true);
          shapesList.add(vector);
          transformables.add(vector);
        }
        
        // The full primitive cell
        if (true) {
          // Depends on the settings...TODO
          Vector boxEdges = crystalFrame.getBoxEdges();
          for (int i = 0; i < boxEdges.size(); i = i + 2) {
            LineShape line = new LineShape(settings,
                (Point3f) boxEdges.elementAt(i),
                  (Point3f) boxEdges.elementAt(i + 1));
            shapesList.add(line);
            transformables.add(line);
          }
        }
      }
      shapes = (Shape[]) shapesList.toArray(new Shape[0]);
    }
    
    Iterator iter = transformables.listIterator();
    while (iter.hasNext()) {
      Transformable t1 = (Transformable) iter.next();
      t1.transform(matrix, settings);
    }
    Arrays.sort(shapes,
                new Comparator() {
                  public int compare(Object shape1, Object shape2) {
                    int z1 = ((Shape) shape1).getZ();
                    int z2 = ((Shape) shape2).getZ();
                    if (z1 < z2)
                      return -1;
                    if (z1 == z2)
                      return 0;
                    return 1;
                  }
                }
                );
    AtomShape.prepareRendering(g, settings);
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].render(g);
    }

  }

  private int previousFrameHashCode;
  private int previousSettingsHashCode;
  private int previousNumberAtoms;

  private Shape[] shapes = null;
  private final ArrayList shapesList = new ArrayList();
  
  private final ArrayList transformables = new ArrayList();
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
}

