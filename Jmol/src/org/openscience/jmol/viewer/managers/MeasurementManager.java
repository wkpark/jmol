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
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Measurement;

import java.util.BitSet;
import java.util.Vector;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class MeasurementManager {

  JmolViewer viewer;
  Frame frame;

  public MeasurementManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void setFrame(Frame frame) {
    this.frame = frame;
    if (frame != null)
      frame.clearMeasurements();
  }

  public void clearMeasurements() {
    if (frame != null)
      frame.clearMeasurements();
  }

  public int getMeasurementCount() {
    return (frame == null) ? 0 : frame.getMeasurementCount();
  }

  public int[] getMeasurementIndices(int measurementIndex) {
    return frame.getMeasurements()[measurementIndex].atomIndices;
  }

  public String getMeasurementString(int measurementIndex) {
    return frame.getMeasurements()[measurementIndex].strMeasurement;
  }

  public Measurement[] getMeasurements(int count) {
    int numFound = 0;
    int measurementCount = frame.getMeasurementCount();
    Measurement[] measurements = frame.getMeasurements();
    for (int i = measurementCount; --i >= 0; )
      if (measurements[i].atomIndices.length == count)
        ++numFound;
    Measurement[] foundMeasurements = new Measurement[numFound];
    for (int i = 0, j = 0; i < numFound; ++i, ++j) {
      while (measurements[j].atomIndices.length != count)
        ++j;
      foundMeasurements[i] = measurements[j];
    }
    return foundMeasurements;
  }

  public void defineMeasurement(int count, int[] atomIndices) {
    Measurement[] measurements = frame.getMeasurements();
    for (int i = frame.getMeasurementCount(); --i >= 0; ) {
      if (measurements[i].sameAs(count, atomIndices))
        return;
    }
    frame.addMeasurement(count, atomIndices);
  }

  public boolean deleteMeasurement(int measurementIndex) {
    if (measurementIndex < 0 ||
        measurementIndex >= frame.getMeasurementCount())
      return false;
    frame.deleteMeasurement(measurementIndex);
    return true;
  }

  public boolean deleteMeasurement(Measurement measurement) {
    Measurement[] measurements = frame.getMeasurements();
    for (int i = frame.getMeasurementCount(); --i >= 0; ) {
      if (measurements[i] == measurement) {
        frame.deleteMeasurement(i);
        return true;
      }
    }
    return false;
  }

  public boolean deleteMeasurement(int count, int[] atomIndices) {
    Measurement[] measurements = frame.getMeasurements();
    for (int i = frame.getMeasurementCount(); --i >= 0; ) {
      if (measurements[i].sameAs(count, atomIndices)) {
        frame.deleteMeasurement(i);
        return true;
      }
    }
    return false;
  }

  public void deleteMeasurements(int count) {
    Measurement[] measurements = frame.getMeasurements();
    for (int i = frame.getMeasurementCount(); --i >= 0; )
      if (measurements[i].atomIndices.length == count)
        frame.deleteMeasurement(i);
  }

  public boolean deleteMeasurementsReferencing(int atomIndexDeleted) {
    boolean measurementDeleted = false;
    Measurement[] measurements = frame.getMeasurements();
    for (int i = frame.getMeasurementCount(); --i >= 0; ) {
      int[] atomIndices = measurements[i].atomIndices;
      for (int j = atomIndices.length; --j >= 0; )
        if (atomIndices[j] == atomIndexDeleted) {
          deleteMeasurement(i);
          measurementDeleted = true;
          break;
        }
    }
    return measurementDeleted;
  }
}
