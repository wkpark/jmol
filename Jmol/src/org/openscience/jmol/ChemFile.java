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
package org.openscience.jmol;

import org.openscience.jmol.DisplayControl;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class constitutes a sequence of ChemFrames.
 **/
public class ChemFile {

  DisplayControl control;
  private ChemFrame[] frames = new ChemFrame[0];
  //  private boolean bondsEnabled = true;
  private Vector propertyList = new Vector();

  /**
   * Very simple class that should be subclassed for each different
   * kind of file that can be read by Jmol.
   */
  public ChemFile(DisplayControl control) {
    this.control = control;
  }

  public ChemFile(DisplayControl control, boolean bondsEnabled) {
    this.control = control;
    //    this.bondsEnabled = bondsEnabled;
  }

  /**
   * returns a ChemFrame from a sequence of ChemFrames that make up
   * this ChemFile
   *
   * @see ChemFrame
   * @param whichframe which frame to return
   */
  public ChemFrame getFrame(int whichframe) {
    return frames[whichframe];
  }

  /**
   * Adds a frame to this file.
   *
   * @param frame the frame to be added
   */
  public void addFrame(ChemFrame frame) {
    ChemFrame[] framesNew = new ChemFrame[frames.length + 1];
    System.arraycopy(frames, 0, framesNew, 0, frames.length);
    framesNew[frames.length] = frame;
    frames = framesNew;
  }

  /**
   * Set the frame at the given index.
   *
   * @param frame the new frame
   * @param whichframe the index of the frame
   */
  public void setFrame(ChemFrame frame, int whichframe) {
    frames[whichframe] = frame;
  }

  /**
   * Returns the number of frames in this file.
   */
  public int getNumberOfFrames() {
    return frames.length;
  }

  /**
   * Returns the array of all the frames
   */
  /*
  public ChemFrame[] getFrames() {
    return frames;
  }
  */

  /**
   * Returns a list of descriptions for physical properties
   * contained by this file.
   */
  /*
  public Vector getPropertyList() {
    return propertyList;
  }
  */

  /**
   * Adds a property description to the property list.
   *
   * @param prop the property description
   */
  /*
  public void addProperty(String prop) {
    if (propertyList.indexOf(prop) < 0) {
      propertyList.addElement(prop);
    }
  }
  */

  /**
   * Returns a list of the names of atom properties on frames in this file.
   */
  public Vector getAtomPropertyList() {

    Vector descriptions = new Vector();
    for (int iframe = 0; iframe < frames.length; ++iframe) {
      ChemFrame frame = frames[iframe];
      if (frame.getAtomCount() > 0) {
        Enumeration properties =
          frame.getJmolAtomAt(0).getAtomicProperties().elements();
        while (properties.hasMoreElements()) {
          PhysicalProperty property =
            (PhysicalProperty) properties.nextElement();
          if (!descriptions.contains(property.getDescriptor())) {
            descriptions.addElement(property.getDescriptor());
          }
        }
      }
    }
    return descriptions;
  }

  /**
   * Returns a list of the names of frame properties in this file.
   */
  public Vector getFramePropertyList() {

    Vector descriptions = new Vector();
    for (int iframe = 0; iframe < frames.length; ++iframe) {
      ChemFrame frame = frames[iframe];
      Enumeration properties = frame.getFrameProperties().elements();
      while (properties.hasMoreElements()) {
        PhysicalProperty property =
          (PhysicalProperty) properties.nextElement();
        if (!descriptions.contains(property.getDescriptor())) {
          descriptions.addElement(property.getDescriptor());
        }
      }
    }
    return descriptions;
  }
}

