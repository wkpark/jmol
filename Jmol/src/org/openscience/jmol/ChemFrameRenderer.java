
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
package org.openscience.jmol;

import java.awt.Graphics;
import java.util.Enumeration;
import java.util.Vector;
import javax.vecmath.Matrix4d;
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
      DisplaySettings settings, Matrix4d matrix) {

    if (frame.getNumberOfAtoms() <= 0) {
      return;
    }

    // mth 2002 oct 27
    // if logTimes==true then show recalc/repaint times to the console
    long timePaint = 0;
    long time = 0;
    boolean logTimes = false;

    if (logTimes) {
      timePaint = System.currentTimeMillis();
    }

    boolean drawHydrogen = settings.getShowHydrogens();

    if (shapes == null || frame.hashCode() != frameHashCode
        || settings.hashCode() != previousSettingsHashCode) {
      if (logTimes) {
        time = System.currentTimeMillis();
      }
      frameHashCode = frame.hashCode();
      previousSettingsHashCode = settings.hashCode();
      transformables.clear();
      transformables.addElement(frame);
      double maxMagnitude = -1.0;
      double minMagnitude = Double.MAX_VALUE;
      Vector shapesList = new Vector();
      for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
        Atom atom = frame.getAtomAt(i);
        if (settings.getShowAtoms() && (settings.getShowHydrogens()
            || !atom.isHydrogen())) {
          shapesList.addElement(new AtomShape(atom, settings));
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
      
      if (frame instanceof CrystalFrame) {
        CrystalFrame crystalFrame = (CrystalFrame) frame;
        float[][] rprimd = crystalFrame.getRprimd();
        
        // The three primitives vectors with arrows
        for (int i = 0; i < 3; i++) {
          VectorShape vector = new VectorShape(settings, zeroPoint,
              new Point3f(rprimd[i][0], rprimd[i][1], rprimd[i][2]), false,
                true);
          shapesList.addElement(vector);
          transformables.addElement(vector);
        }
        
        // The full primitive cell
        if (true) {
          // Depends on the settings...TODO
          Vector boxEdges = crystalFrame.getBoxEdges();
          for (int i = 0; i < boxEdges.size(); i = i + 2) {
            LineShape line = new LineShape(settings,
                (Point3f) boxEdges.elementAt(i),
                  (Point3f) boxEdges.elementAt(i + 1));
            shapesList.addElement(line);
            transformables.addElement(line);
          }
        }
      }
      
      shapes = new Shape[shapesList.size()];
      Enumeration shapeIter = shapesList.elements();
      for (int i = 0; i < shapes.length && shapeIter.hasMoreElements(); ++i) {
        shapes[i] = (Shape) shapeIter.nextElement();
      }
      if (logTimes) {
        time = System.currentTimeMillis() - time;
        System.out.println("redo shapes time:" + time);
      }
    }
    
    if (logTimes) {
      time = System.currentTimeMillis();
    }
    Enumeration iter = transformables.elements();
    while (iter.hasMoreElements()) {
      Transformable t1 = (Transformable) iter.nextElement();
      t1.transform(matrix);
    }
    if (logTimes) {
      time = System.currentTimeMillis() - time;
      System.out.println("transform time:" + time);
    }

    if (logTimes) {
      time = System.currentTimeMillis();
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
    if (logTimes) {
      time = System.currentTimeMillis() - time;
      System.out.println("sort time:" + time);
      time = System.currentTimeMillis();
    }
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].render(g);
    }
    if (logTimes) {
      time = System.currentTimeMillis() - time;
      System.out.println("render time:" + time);

      timePaint = System.currentTimeMillis() - timePaint;
      System.out.println("ChemFrameRenderer.paint() : " + timePaint);
    }
  }

  int frameHashCode;
  int previousSettingsHashCode;
  
  Shape[] shapes;
  
  Vector transformables = new Vector();
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
}

