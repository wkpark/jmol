/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Enumeration;
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
  public void paint(Graphics g, DisplayControl control) {
    ChemFrame frame = control.getFrame();
    int numAtoms = frame.getAtomCount();
    if (numAtoms <= 0) {
        System.out.println("No atoms to draw");
        return;
    }
    if (shapes == null || control.hasStructuralChange()) {
      control.resetStructuralChange();
      shapesVector.removeAllElements();
      for (int i = 0; i < numAtoms; ++i) {
        Atom atom = (org.openscience.jmol.Atom)frame.getJmolAtomAt(i);
        AtomShape atomShape = atom.getAtomShape();
        if (atomShape == null) {
          // FIXME mth -- atomShapes should be allocated as part of new Atom()
          // but the Atom code does not have a control at that point
          atomShape = new AtomShape(atom, control);
          atom.setAtomShape(atomShape);
          // FIX? elw -- overwrite the CDK/Jmol atom with a Jmol atom *with*
          //             atomShape
          frame.setAtomAt(i, atom);
        }
        shapesVector.addElement(atom.getAtomShape());
      }
      if (control.getShowVectors()) {
        double minAtomVectorMagnitude = frame.getMinAtomVectorMagnitude();
        double atomVectorRange = frame.getAtomVectorRange();
        boolean showHydrogens = control.getShowHydrogens();
        for (int i = 0; i < numAtoms; ++i) {
          Atom atom = (org.openscience.jmol.Atom)frame.getJmolAtomAt(i);
          if (atom.hasVector() && (showHydrogens || !atom.isHydrogen())) {
            shapesVector.addElement(new AtomVectorShape(atom, control,
                                                        minAtomVectorMagnitude,
                                                        atomVectorRange));
          }
        }
      }
      if (control.getModeAxes() != DisplayControl.AXES_NONE) {
        Axes axes = control.getAxes();
        shapesVector.addElement(axes.getOriginShape());
        Shape[] shapes = axes.getAxisShapes();
        for (int i = shapes.length; --i >= 0; )
          shapesVector.addElement(shapes[i]);
      }
      if (control.getShowBoundingBox()) {
        BoundingBox bbox = control.getBoundingBox();
        Shape[] shapes = bbox.getBboxShapes();
        for (int i = shapes.length; --i >= 0; )
          shapesVector.addElement(shapes[i]);
      }

      if (frame instanceof CrystalFrame) {
        CrystalFrame crystalFrame = (CrystalFrame) frame;
        double[][] rprimd = crystalFrame.getRprimd();
        
        // The three primitives vectors with arrows
        for (int i = 0; i < 3; i++) {
          VectorShape vector = new VectorShape(zeroPoint,
              new Point3d(rprimd[i][0], rprimd[i][1], rprimd[i][2]), false,
                true);
          shapesVector.addElement(vector);
        }
        
        // The full primitive cell
        if (true) {
          // Depends on the settings...TODO
          Vector boxEdges = crystalFrame.getBoxEdges();
          for (int i = 0; i < boxEdges.size(); i = i + 2) {
            LineShape line =
              new LineShape((Point3d) boxEdges.elementAt(i),
                            (Point3d) boxEdges.elementAt(i + 1));
            shapesVector.addElement(line);
          }
        }
      }
      shapes = new Shape[shapesVector.size()];
      shapesVector.copyInto(shapes);
    }

    control.calcViewTransformMatrix();
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].transform(control);
    }
    
    if (control.jvm12orGreater) {
      UseJavaSort.sortShapes(shapes);
    } else {
      NonJavaSort.sortShapes(shapes);
    }
                              
    boolean slabEnabled = control.getSlabEnabled();
    int slabValue = control.getSlabValue();
    for (int i = 0; i < shapes.length; ++i) {
      if (slabEnabled) {
        if (shapes[i].z > slabValue)
          continue;
      }
      shapes[i].render(g, control);
    }
  }

  private Shape[] shapes = null;
  private final Vector shapesVector = new Vector();
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3d zeroPoint = new Point3d();

}

