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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.datamodel.JmolFrame;
import org.openscience.jmol.viewer.datamodel.MeasurementShape;

import java.util.BitSet;
import java.util.Vector;
import javax.vecmath.Point3d;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class MeasurementManager {

  JmolViewer viewer;
  JmolFrame frame;

  public MeasurementManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void setJmolFrame(JmolFrame frame) {
    this.frame = frame;
    frame.clearMeasurementShapes();
  }

  public void clearMeasurements() {
    if (frame != null)
      frame.clearMeasurementShapes();
  }

  public MeasurementShape[] getMeasurements(int count) {
    int numFound = 0;
    int measurementShapeCount = frame.getMeasurementShapeCount();
    MeasurementShape[] measurementShapes = frame.getMeasurementShapes();
    for (int i = measurementShapeCount; --i >= 0; )
      if (measurementShapes[i].atomIndices.length == count)
        ++numFound;
    MeasurementShape[] foundMeasurements = new MeasurementShape[numFound];
    for (int i = 0, j = 0; i < numFound; ++i, ++j) {
      while (measurementShapes[j].atomIndices.length != count)
        ++j;
      foundMeasurements[i] = measurementShapes[j];
    }
    return foundMeasurements;
  }

  public void defineMeasurement(int count, int[] atomIndices) {
    MeasurementShape[] measurementShapes = frame.getMeasurementShapes();
    for (int i = frame.getMeasurementShapeCount(); --i >= 0; ) {
      if (measurementShapes[i].sameAs(count, atomIndices))
        return;
    }
    frame.addMeasurementShape(new MeasurementShape(viewer, count, atomIndices));
  }

  public boolean deleteMeasurement(MeasurementShape measurementShape) {
    MeasurementShape[] measurementShapes = frame.getMeasurementShapes();
    for (int i = frame.getMeasurementShapeCount(); --i >= 0; ) {
      if (measurementShapes[i] == measurementShape) {
        frame.deleteMeasurementShape(i);
        return true;
      }
    }
    return false;
  }

  public boolean deleteMeasurement(int count, int[] atomIndices) {
    MeasurementShape[] measurementShapes = frame.getMeasurementShapes();
    for (int i = frame.getMeasurementShapeCount(); --i >= 0; ) {
      if (measurementShapes[i].sameAs(count, atomIndices)) {
        frame.deleteMeasurementShape(i);
        return true;
      }
    }
    return false;
  }

  public void deleteMeasurements(int count) {
    MeasurementShape[] measurementShapes = frame.getMeasurementShapes();
    for (int i = frame.getMeasurementShapeCount(); --i >= 0; )
      if (measurementShapes[i].atomIndices.length == count)
        frame.deleteMeasurementShape(i);
  }
}
