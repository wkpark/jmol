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
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Vector;
import javax.vecmath.Point3d;
import org.openscience.jmol.applet.NonJavaSort;

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
  public synchronized void paint(Graphics g, Rectangle rectClip,
                                 DisplayControl control) {
    ChemFrame frame = control.getFrame();
    int numAtoms = frame.getNumberOfAtoms();
    if (numAtoms <= 0) {
      return;
    }
    int hcFrame = frame.hashCode();
    if (shapes == null || // did not do shapes yet
        control.hasStructuralChange() || 
        // FIXME -- these should be part of hasStructuralChange
        hcFrame != previousFrameHashCode || // frame itself is changed
        numAtoms != previousNumberAtoms // #atoms changed (e.g. a delete)
       ) {
      control.resetStructuralChange();
      previousFrameHashCode = hcFrame;
      previousNumberAtoms = numAtoms;
      shapesList.clear();
      transformables.clear();
      transformables.add(frame);
      for (int i = 0; i < numAtoms; ++i) {
        Atom atom = frame.getAtomAt(i);
        shapesList.add(new AtomShape(atom));
      }
      if (control.getShowVectors()) {
        double minAtomVectorMagnitude = frame.getMinAtomVectorMagnitude();
        double atomVectorRange = frame.getAtomVectorRange();
        boolean showHydrogens = control.getShowHydrogens();
        for (int i = 0; i < numAtoms; ++i) {
          Atom atom = frame.getAtomAt(i);
          if (showHydrogens || !atom.isHydrogen()) {
            shapesList.add(new AtomVectorShape(atom, control,
                                               minAtomVectorMagnitude,
                                               atomVectorRange));
          }
        }
      }
      
      if (frame instanceof CrystalFrame) {
        CrystalFrame crystalFrame = (CrystalFrame) frame;
        double[][] rprimd = crystalFrame.getRprimd();
        
        // The three primitives vectors with arrows
        for (int i = 0; i < 3; i++) {
          VectorShape vector = new VectorShape(zeroPoint,
              new Point3d(rprimd[i][0], rprimd[i][1], rprimd[i][2]), false,
                true);
          shapesList.add(vector);
          transformables.add(vector);
        }
        
        // The full primitive cell
        if (true) {
          // Depends on the settings...TODO
          Vector boxEdges = crystalFrame.getBoxEdges();
          for (int i = 0; i < boxEdges.size(); i = i + 2) {
            LineShape line =
              new LineShape((Point3d) boxEdges.elementAt(i),
                            (Point3d) boxEdges.elementAt(i + 1));
            shapesList.add(line);
            transformables.add(line);
          }
        }
      }
      shapes = (Shape[]) shapesList.toArray(new Shape[0]);
    }
    
    Iterator iter = transformables.listIterator();
    control.calcViewTransformMatrix();
    while (iter.hasNext()) {
      Transformable t1 = (Transformable) iter.next();
      t1.transform(control);
    }
    if (control.jvm12orGreater)
      UseJavaSort.sortShapes(shapes);
    else
      NonJavaSort.sortShapes(shapes);
                              
    AtomShape.prepareRendering(g, rectClip, control);
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].render(g, rectClip, control);
    }
  }

  private int previousFrameHashCode;
  private int previousNumberAtoms;

  private Shape[] shapes = null;
  private final ArrayList shapesList = new ArrayList();
  
  private final ArrayList transformables = new ArrayList();
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3d zeroPoint = new Point3d();

}

